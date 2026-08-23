package ci.progbandama.mobile.tools

import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.EnfantPlantationModel
import ci.progbandama.mobile.models.VisitePlantationModel
import com.google.gson.reflect.TypeToken

/**
 * Recapitulatif d'une Visite Plantation avant enregistrement - lot 4.
 *
 * Memes principes que celui de l'Enquete Menage : construit depuis le modele et non depuis les
 * vues, arrete la ou la visite s'arrete, et n'affiche pas les informations non renseignees.
 *
 * Deux points propres a ce module : les indicateurs de temps de travail sont relus en clair, alors
 * que le modele porte des codes - relire "3" n'apprendrait rien a l'agent - et l'absence d'enfant
 * est dite explicitement plutot que laissee en blanc, "aucun enfant trouve" etant un resultat de
 * depistage et non une saisie oubliee.
 */
object RecapVisitePlantation {

    /**
     * Une ligne du recapitulatif.
     *
     * @param libelle identifiant de la chaine decrivant l'information.
     * @param valeur ce qui sera transmis, deja mis en forme.
     * @param enfant numero de la fiche concernee, a partir de 1 ; null pour l'entete.
     */
    data class Ligne(val libelle: Int, val valeur: String, val enfant: Int? = null)

    private fun liste(stringify: String?): List<String> =
        BrouillonPfte.deserialiserListe(stringify, object : TypeToken<MutableList<String>>() {})

    fun lignes(visite: VisitePlantationModel, enfants: List<EnfantPlantationModel>): List<Ligne> {
        val lignes = mutableListOf<Ligne>()

        fun poser(libelle: Int, valeur: String?, enfant: Int? = null) {
            if (!valeur.isNullOrBlank()) lignes.add(Ligne(libelle, valeur.trim(), enfant))
        }

        fun poserListe(libelle: Int, valeurs: List<String>, enfant: Int? = null) {
            poser(libelle, valeurs.joinToString("\n"), enfant)
        }

        // ------------------------------------------------------------ identification
        poser(R.string.section, visite.sectionNom)
        poser(R.string.localite, visite.localiteNom)
        poser(R.string.producteur, visite.producteurNom)
        poser(R.string.date_de_la_visite, visite.dateEnquete)
        poser(R.string.jour_de_la_visite, visite.jourVisite)
        poser(R.string.nom_de_l_enqueteur, visite.nomEnqueteur)
        poser(R.string.position_gps, positionLisible(visite))

        // ------------------------------------------------------------ repondant
        poser(R.string.le_producteur_est_il_le_repondant, visite.estProducteurRepondant)
        poser(R.string.nom_du_repondant, visite.nomRepondant)
        poser(R.string.titre_du_repondant, visite.titreRepondant)

        // ------------------------------------------------------------ premier arret
        poser(R.string.le_producteur_est_il_disponible, visite.producteurDisponible)
        if (visite.producteurDisponible == NON) {
            poser(R.string.raison_de_l_indisponibilite, visite.raisonIndisponibilite)
            poser(R.string.date_de_replanification, visite.datePlanification)
            poserListe(R.string.raison_s_du_refus, liste(visite.raisonRefusStringify))
            poser(R.string.autre_raison_du_refus, visite.autreRaisonRefus)
            return lignes
        }

        // ------------------------------------------------------------ second arret
        poser(R.string.consentement_du_producteur, visite.consentement)
        if (visite.consentement != OUI) return lignes

        // ------------------------------------------------------------ caracteristiques
        poser(R.string.superficie_de_la_plantation, visite.superficiePlantation)
        poser(R.string.nombre_de_manoeuvres_permanents, visite.nombreManoeuvresPermanents)
        poser(R.string.manoeuvres_permanents_moins_18, visite.manoeuvresPermanentsMoins18)
        poser(R.string.nombre_de_manoeuvres_journaliers, visite.nombreManoeuvresJournaliers)
        poser(R.string.manoeuvres_journaliers_moins_18, visite.manoeuvresJournaliersMoins18)
        poser(R.string.nombre_d_enfants_0_4_trouves, visite.nombreEnfants0a4)
        poser(R.string.nombre_d_enfants_5_17_trouves, visite.nombreEnfants5a17)
        poser(R.string.nombre_de_personnes_trouvees, visite.nombrePersonnesTrouvees)

        // ------------------------------------------------------------ fiches enfant
        //
        // L'absence d'enfant est dite, et non laissee en blanc : c'est un resultat de depistage,
        // pas une saisie oubliee, et l'agent doit pouvoir le verifier avant d'envoyer.
        if (enfants.isEmpty()) {
            lignes.add(Ligne(R.string.enfants_trouves, AUCUN_ENFANT))
            return lignes
        }

        enfants.forEachIndexed { index, enfant ->
            val n = index + 1
            poser(R.string.nom, enfant.nom, n)
            poser(R.string.date_de_naissance, enfant.dateNaissance, n)
            poser(R.string.sexe, enfant.sexe, n)
            poser(R.string.lien_de_parente, enfant.lienParente, n)
            poser(R.string.autre_lien_de_parente, enfant.autreLienParente, n)
            poser(R.string.raison_ne_vit_pas_avec_ses_parents, enfant.raisonNeVitPasParents, n)
            poser(R.string.autre_raison, enfant.autreRaisonNeVitPasParents, n)
            poser(R.string.situation_scolaire, enfant.situationScolaire, n)
            poser(R.string.classe, enfant.niveauScolaire, n)
            poserListe(R.string.raisons_de_non_scolarisation, enfant.raisonNonScolarisation, n)
            poser(R.string.autre_raison_de_non_scolarisation, enfant.autreRaisonNonScolarisation, n)
            poser(R.string.dispose_d_un_extrait_de_naissance, enfant.extraitNaissance, n)
            poserListe(R.string.raisons_absence_extrait, enfant.raisonPasExtrait, n)

            // Les indicateurs de temps de travail sont relus en clair : le modele porte des codes,
            // et relire "3" n'apprendrait rien a l'agent.
            poser(R.string.heures_de_travail_par_semaine, enfant.heuresTravailSemaine, n)
            poser(R.string.jours_travailles_par_semaine,
                VocabulairesPfte.libellePourCode(VocabulairesPfte.JOURS_TRAVAIL, enfant.joursTravail), n)
            poser(R.string.heures_journee_la_plus_chargee,
                VocabulairesPfte.libellePourCode(VocabulairesPfte.HEURES_JOURNEE, enfant.heuresTravailJournee), n)

            poserListe(R.string.situations_pfte_constatees, enfant.situationsPfte, n)
            poserListe(R.string.raisons_du_travail_ou_de_l_abus, enfant.raisonTravailAbus, n)
            poser(R.string.autre_raison, enfant.autreRaisonTravailAbus, n)
            poserListe(R.string.mesures_au_niveau_de_l_enfant, enfant.mesuresEnfant, n)
            poserListe(R.string.mesures_au_niveau_du_menage, enfant.mesuresMenage, n)
            poserListe(R.string.mesures_au_niveau_de_la_communaute, enfant.mesuresCommunaute, n)
            poser(R.string.autre_mesure, enfant.autreMesure, n)
        }

        return lignes
    }

    /**
     * Position mise en forme, avec sa precision : seule elle permet de juger si la mesure vaut
     * quelque chose.
     */
    private fun positionLisible(visite: VisitePlantationModel): String {
        if (visite.latitude.isNullOrBlank() || visite.longitude.isNullOrBlank()) return ""
        val base = "${visite.latitude} , ${visite.longitude}"
        val precision = visite.precisionGps
        return if (precision.isNullOrBlank()) base else "$base (± $precision m)"
    }

    private const val OUI = "Oui"
    private const val NON = "Non"
    private const val AUCUN_ENFANT = "Aucun enfant trouvé sur la plantation"
}
