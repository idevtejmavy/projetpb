package ci.progbandama.mobile.tools

import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.EnfantPlantationModel
import ci.progbandama.mobile.models.VisitePlantationModel
import com.google.gson.reflect.TypeToken

/**
 * Controles de saisie de la Visite Plantation - lot 4.
 *
 * Contrairement aux deux autres modules PFTE, l'endpoint mobile de celui-ci VALIDE ce qu'il recoit :
 * une visite incomplete est refusee par un 422. Ces controles ne sont donc pas le seul filet, mais
 * ils evitent a l'agent de partir avec une visite que le serveur rejettera - et qu'il ne pourra
 * corriger qu'en la reprenant depuis la file d'attente, souvent des jours plus tard.
 *
 * Ils reprennent la meme structure contextuelle que l'Enquete Menage : ce qui est exige depend du
 * chemin suivi, et une visite arretee sur une indisponibilite est complete en l'etat.
 *
 * Un point propre a ce module : une visite ou aucun enfant n'a ete trouve est parfaitement valable.
 * Le back-office lui attribue le statut "Aucun enfant trouve", qui est un resultat de depistage a
 * part entiere - exiger au moins une fiche serait un contresens.
 */
object ControlesVisitePlantation {

    /**
     * Information manquante.
     *
     * @param libelle identifiant de la chaine decrivant le champ.
     * @param enfant numero de la fiche concernee, a partir de 1 ; null pour l'entete.
     */
    data class Manque(val libelle: Int, val enfant: Int? = null)

    private fun vide(valeur: String?) = valeur.isNullOrBlank()

    private fun liste(stringify: String?): List<String> =
        BrouillonPfte.deserialiserListe(stringify, object : TypeToken<MutableList<String>>() {})

    fun manquants(visite: VisitePlantationModel, enfants: List<EnfantPlantationModel>): List<Manque> {
        val manques = mutableListOf<Manque>()
        fun exiger(condition: Boolean, libelle: Int) { if (condition) manques.add(Manque(libelle)) }

        // ------------------------------------------------------------ identification
        exiger(vide(visite.sectionId) || visite.sectionId == "0", R.string.section)
        exiger(vide(visite.localiteId) || visite.localiteId == "0", R.string.localite)
        exiger(vide(visite.producteurId) || visite.producteurId == "0", R.string.producteur)
        exiger(vide(visite.jourVisite), R.string.jour_de_la_visite)
        exiger(vide(visite.latitude), R.string.position_gps)

        // ------------------------------------------------------------ repondant
        exiger(vide(visite.estProducteurRepondant), R.string.le_producteur_est_il_le_repondant)
        if (visite.estProducteurRepondant == NON) {
            exiger(vide(visite.nomRepondant), R.string.nom_du_repondant)
            exiger(vide(visite.titreRepondant), R.string.titre_du_repondant)
        }

        // ------------------------------------------------------------ premier arret : indisponible
        exiger(vide(visite.producteurDisponible), R.string.le_producteur_est_il_disponible)
        if (visite.producteurDisponible == NON) {
            val raison = visite.raisonIndisponibilite
            exiger(vide(raison), R.string.raison_de_l_indisponibilite)
            exiger(
                raison == VocabulairesPfte.ABSENCE_TEMPORAIRE && vide(visite.datePlanification),
                R.string.date_de_replanification
            )
            exiger(
                raison == VocabulairesPfte.REFUS && liste(visite.raisonRefusStringify).isEmpty(),
                R.string.raison_s_du_refus
            )
            // La visite s'arrete ici, avec son motif.
            return manques
        }

        // ------------------------------------------------------------ second arret : non consentement
        exiger(vide(visite.consentement), R.string.consentement_du_producteur)
        if (visite.consentement != OUI) return manques

        // ------------------------------------------------------------ caracteristiques de la plantation
        exiger(vide(visite.superficiePlantation), R.string.superficie_de_la_plantation)
        exiger(vide(visite.nombreManoeuvresPermanents), R.string.nombre_de_manoeuvres_permanents)
        exiger(vide(visite.manoeuvresPermanentsMoins18), R.string.manoeuvres_permanents_moins_18)
        exiger(vide(visite.nombreManoeuvresJournaliers), R.string.nombre_de_manoeuvres_journaliers)
        exiger(vide(visite.manoeuvresJournaliersMoins18), R.string.manoeuvres_journaliers_moins_18)
        exiger(vide(visite.nombreEnfants0a4), R.string.nombre_d_enfants_0_4_trouves)
        exiger(vide(visite.nombreEnfants5a17), R.string.nombre_d_enfants_5_17_trouves)
        exiger(vide(visite.nombrePersonnesTrouvees), R.string.nombre_de_personnes_trouvees)

        // ------------------------------------------------------------ fiches enfant
        //
        // Aucune fiche n'est exigee : "Aucun enfant trouve" est un resultat de depistage valable.
        // En revanche, chaque fiche generee doit etre renseignee - une fiche a moitie remplie
        // serait refusee par le serveur, qui valide ce module.
        enfants.forEachIndexed { index, enfant ->
            val numero = index + 1
            fun exigerEnfant(condition: Boolean, libelle: Int) {
                if (condition) manques.add(Manque(libelle, numero))
            }
            exigerEnfant(vide(enfant.nom), R.string.nom)
            exigerEnfant(vide(enfant.dateNaissance), R.string.date_de_naissance)
            exigerEnfant(vide(enfant.sexe), R.string.sexe)
            exigerEnfant(vide(enfant.lienParente), R.string.lien_de_parente)
            exigerEnfant(vide(enfant.raisonNeVitPasParents), R.string.raison_ne_vit_pas_avec_ses_parents)
            exigerEnfant(vide(enfant.situationScolaire), R.string.situation_scolaire)
            exigerEnfant(vide(enfant.extraitNaissance), R.string.dispose_d_un_extrait_de_naissance)

            // Les trois indicateurs de temps de travail : le coeur du depistage.
            exigerEnfant(vide(enfant.heuresTravailSemaine), R.string.heures_de_travail_par_semaine)
            exigerEnfant(vide(enfant.joursTravail), R.string.jours_travailles_par_semaine)
            exigerEnfant(vide(enfant.heuresTravailJournee), R.string.heures_journee_la_plus_chargee)

            exigerEnfant(enfant.situationsPfte.isEmpty(), R.string.situations_pfte_constatees)
            exigerEnfant(enfant.mesuresEnfant.isEmpty(), R.string.mesures_au_niveau_de_l_enfant)
            exigerEnfant(enfant.mesuresMenage.isEmpty(), R.string.mesures_au_niveau_du_menage)
            exigerEnfant(enfant.mesuresCommunaute.isEmpty(), R.string.mesures_au_niveau_de_la_communaute)
        }

        // Une fiche laissee entierement vide n'est pas collectee : l'effectif declare et le nombre
        // de fiches renseignees peuvent donc diverger, et c'est ce que le serveur comparera.
        val declares = visite.nombreEnfants5a17?.trim()?.toIntOrNull() ?: 0
        exiger(declares > 0 && enfants.size < declares, R.string.fiches_enfant_incompletes)

        return manques
    }

    private const val OUI = "Oui"
    private const val NON = "Non"
}
