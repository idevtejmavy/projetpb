package ci.progbandama.mobile.tools

import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.EnfantMenageModel
import ci.progbandama.mobile.models.EnqueteMenageModel
import com.google.gson.reflect.TypeToken

/**
 * Controles de saisie de l'Enquete Menage - lot 3.
 *
 * L'endpoint mobile n'applique aucune validation : une enquete incomplete est acceptee en base
 * sans le moindre signalement. Ces controles sont donc le seul filet, et ils decident de ce qui
 * part depuis le terrain.
 *
 * Ils sont contextuels : ce qui est exige depend du chemin suivi dans le formulaire. Une enquete
 * arretee sur une indisponibilite n'a pas a porter de caracteristiques du menage, et l'exiger
 * empecherait l'agent d'enregistrer un motif d'absence parfaitement valable.
 *
 * Ils vivent ici, et non dans l'ecran, pour etre verifiables sans appareil : c'est la partie du
 * module ou une erreur ne se voit pas - elle laisse simplement passer une enquete incomplete.
 */
object ControlesEnqueteMenage {

    /**
     * Information manquante.
     *
     * @param libelle identifiant de la chaine decrivant le champ.
     * @param enfant numero de la fiche concernee, a partir de 1 ; null pour l'entete.
     */
    data class Manque(val libelle: Int, val enfant: Int? = null)

    private fun vide(valeur: String?) = valeur.isNullOrBlank()

    /**
     * Relit une colonne de liste.
     *
     * Passe par la serialisation des brouillons, et non par GsonUtils : ces controles doivent
     * pouvoir tourner hors appareil, et l'outil du projet s'appuie sur le contexte applicatif.
     */
    private fun liste(stringify: String?): List<String> =
        BrouillonPfte.deserialiserListe(stringify, object : TypeToken<MutableList<String>>() {})

    /**
     * @return la liste des informations manquantes, vide si l'enquete peut etre enregistree.
     *         L'ordre suit celui du formulaire, pour que l'agent les retrouve de haut en bas.
     */
    fun manquants(enquete: EnqueteMenageModel, enfants: List<EnfantMenageModel>): List<Manque> {
        val manques = mutableListOf<Manque>()
        fun exiger(condition: Boolean, libelle: Int) { if (condition) manques.add(Manque(libelle)) }

        // ------------------------------------------------------------ identification
        exiger(vide(enquete.sectionId) || enquete.sectionId == "0", R.string.section)
        exiger(vide(enquete.localiteId) || enquete.localiteId == "0", R.string.localite)
        exiger(vide(enquete.producteurId) || enquete.producteurId == "0", R.string.producteur)
        exiger(vide(enquete.latitude), R.string.position_gps)

        // ------------------------------------------------------------ repondant
        exiger(vide(enquete.estProducteurRepondant), R.string.le_producteur_est_il_le_repondant)
        if (enquete.estProducteurRepondant == NON) {
            exiger(vide(enquete.nomRepondant), R.string.nom_du_repondant)
            exiger(vide(enquete.titreRepondant), R.string.titre_du_repondant)
        }

        // ------------------------------------------------------------ premier arret : indisponible
        exiger(vide(enquete.producteurDisponible), R.string.le_producteur_est_il_disponible)
        if (enquete.producteurDisponible == NON) {
            val raison = enquete.raisonIndisponibilite
            exiger(vide(raison), R.string.raison_de_l_indisponibilite)
            exiger(
                raison == VocabulairesPfte.ABSENCE_TEMPORAIRE && vide(enquete.datePlanification),
                R.string.date_de_replanification
            )
            exiger(
                raison == VocabulairesPfte.REFUS &&
                        liste(enquete.raisonRefusStringify).isEmpty(),
                R.string.raison_s_du_refus
            )
            // L'enquete s'arrete ici, avec son motif : rien d'autre n'est exige.
            return manques
        }

        // ------------------------------------------------------------ second arret : non consentement
        exiger(vide(enquete.consentement), R.string.consentement_du_producteur)
        if (enquete.consentement != OUI) return manques

        // ------------------------------------------------------------ caracteristiques du menage
        exiger(vide(enquete.situationMatrimoniale), R.string.situation_matrimoniale)
        exiger(vide(enquete.nombreAdultes), R.string.nombre_d_adultes)
        exiger(vide(enquete.nombreEnfants0a4), R.string.nombre_d_enfants_0_4)
        exiger(vide(enquete.nombreEnfants5a17), R.string.nombre_d_enfants_5_17)
        exiger(vide(enquete.aEnfantACharge), R.string.y_a_t_il_des_enfants_a_charge)

        // ------------------------------------------------------------ troisieme arret : pas d'enfant
        if (enquete.aEnfantACharge != OUI) return manques

        exiger(vide(enquete.nombreEnfantsACharge), R.string.nombre_d_enfants_a_charge)

        // ------------------------------------------------------------ fiches enfant
        if (enfants.isEmpty()) {
            manques.add(Manque(R.string.au_moins_une_fiche_enfant))
        }
        enfants.forEachIndexed { index, enfant ->
            val numero = index + 1
            fun exigerEnfant(condition: Boolean, libelle: Int) {
                if (condition) manques.add(Manque(libelle, numero))
            }
            exigerEnfant(vide(enfant.nom), R.string.nom)
            exigerEnfant(vide(enfant.dateNaissance), R.string.date_de_naissance)
            exigerEnfant(vide(enfant.sexe), R.string.sexe)
            exigerEnfant(vide(enfant.lienParente), R.string.lien_de_parente)
            exigerEnfant(vide(enfant.situationScolaire), R.string.situation_scolaire)
            exigerEnfant(vide(enfant.extraitNaissance), R.string.dispose_d_un_extrait_de_naissance)

            // Exigences propres au serveur : au moins une situation constatee, et au moins une
            // mesure a chacun des trois niveaux.
            exigerEnfant(enfant.situationsPfte.isEmpty(), R.string.situations_pfte_constatees)
            exigerEnfant(enfant.mesuresEnfant.isEmpty(), R.string.mesures_au_niveau_de_l_enfant)
            exigerEnfant(enfant.mesuresMenage.isEmpty(), R.string.mesures_au_niveau_du_menage)
            exigerEnfant(enfant.mesuresCommunaute.isEmpty(), R.string.mesures_au_niveau_de_la_communaute)
        }

        // ------------------------------------------------------------ sensibilisation
        exiger(
            liste(enquete.themesSensibilisationStringify).isEmpty(),
            R.string.themes_abordes
        )
        exiger(
            liste(enquete.outilsSensibilisationStringify).isEmpty(),
            R.string.outils_utilises
        )
        exiger(vide(enquete.telephoneProducteurSensibilisation), R.string.telephone_du_producteur)
        exiger(vide(enquete.photoSensibilisation), R.string.photo_de_la_sensibilisation)

        return manques
    }

    private const val OUI = "Oui"
    private const val NON = "Non"
}
