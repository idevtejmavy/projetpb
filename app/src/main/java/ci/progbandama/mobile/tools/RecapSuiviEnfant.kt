package ci.progbandama.mobile.tools

import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.SuiviEnfantModel
import com.google.gson.reflect.TypeToken

/**
 * Recapitulatif d'un Suivi Enfant Travailleur avant enregistrement - lot 5.
 *
 * Memes principes que pour les deux autres modules : construit depuis le modele, et non depuis les
 * vues ; les informations non renseignees n'apparaissent pas.
 *
 * Deux points propres a ce module :
 *  - quatre valeurs sont relues EN CLAIR alors que le modele porte des codes - presence, motif
 *    d'absence, jours travailles et heures de la journee la plus chargee ;
 *  - il n'y a pas de chemin d'arret : ce formulaire n'a ni filtre ni consentement, le
 *    recapitulatif se lit donc toujours en entier.
 */
object RecapSuiviEnfant {

    /**
     * Une ligne du recapitulatif.
     *
     * @param libelle identifiant de la chaine decrivant l'information.
     * @param valeur ce qui sera transmis, deja mis en forme.
     */
    data class Ligne(val libelle: Int, val valeur: String)

    private fun liste(stringify: String?): List<String> =
        BrouillonPfte.deserialiserListe(stringify, object : TypeToken<MutableList<String>>() {})

    fun lignes(suivi: SuiviEnfantModel): List<Ligne> {
        val lignes = mutableListOf<Ligne>()

        fun poser(libelle: Int, valeur: String?) {
            if (!valeur.isNullOrBlank()) lignes.add(Ligne(libelle, valeur.trim()))
        }

        fun poserListe(libelle: Int, valeurs: List<String>) {
            poser(libelle, valeurs.joinToString("\n"))
        }

        // ------------------------------------------------------------ identification
        poser(R.string.enfant_suivi, suivi.enfantNom)
        poser(R.string.producteur, suivi.producteurNom)
        poser(R.string.localite, suivi.localiteNom)
        poser(R.string.date_de_la_visite_de_suivi, suivi.dateEnquete)
        poser(R.string.nom_de_l_enqueteur, suivi.nomEnqueteur)

        // ------------------------------------------------------------ suivi depuis la derniere visite
        poserListe(R.string.actions_de_remediation, liste(suivi.actionsRemediationStringify))

        // ------------------------------------------------------------ reevaluation
        poser(R.string.nom, suivi.nom)
        poser(R.string.date_de_naissance, suivi.dateNaissance)
        poser(R.string.sexe, suivi.sexe)
        poser(R.string.lien_de_parente, suivi.lienParente)
        poser(R.string.autre_lien_de_parente, suivi.autreLienParente)
        poser(R.string.raison_ne_vit_pas_avec_ses_parents, suivi.raisonNeVitPasParents)
        poser(R.string.autre_raison, suivi.autreRaisonNeVitPasParents)
        poser(R.string.situation_scolaire, suivi.situationScolaire)
        poser(R.string.classe, suivi.niveauScolaire)
        poserListe(R.string.raisons_de_non_scolarisation, liste(suivi.raisonNonScolarisationStringify))
        poser(R.string.autre_raison_de_non_scolarisation, suivi.autreRaisonNonScolarisation)
        poser(R.string.dispose_d_un_extrait_de_naissance, suivi.extraitNaissance)
        poserListe(R.string.raisons_absence_extrait, liste(suivi.raisonPasExtraitStringify))

        // Les quatre valeurs a codes sont relues en clair : relire "0" ou "3" n'apprendrait rien.
        poser(R.string.present_et_disponible,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.PRESENT_DISPONIBLE, suivi.presentDisponible))
        poser(R.string.raison_de_l_absence,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.RAISONS_ABSENCE, suivi.raisonAbsent))
        poser(R.string.heures_de_travail_par_semaine, suivi.heuresTravailSemaine)
        poser(R.string.jours_travailles_par_semaine,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.JOURS_TRAVAIL, suivi.joursTravail))
        poser(R.string.heures_journee_la_plus_chargee,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.HEURES_JOURNEE, suivi.heuresTravailJournee))

        poserListe(R.string.situations_pfte_constatees, liste(suivi.situationsPfteStringify))
        poserListe(R.string.raisons_du_travail_ou_de_l_abus, liste(suivi.raisonTravailAbusStringify))
        poser(R.string.autre_raison, suivi.autreRaisonTravailAbus)
        poserListe(R.string.mesures_au_niveau_de_l_enfant, liste(suivi.mesuresEnfantStringify))
        poserListe(R.string.mesures_au_niveau_du_menage, liste(suivi.mesuresMenageStringify))
        poserListe(R.string.mesures_au_niveau_de_la_communaute, liste(suivi.mesuresCommunauteStringify))
        poser(R.string.autre_mesure, suivi.autreMesure)

        // ------------------------------------------------------------ sensibilisation
        poserListe(R.string.themes_abordes, liste(suivi.themesSensibilisationStringify))
        poser(R.string.autre_theme, suivi.autreThemeSensibilisation)
        poserListe(R.string.outils_utilises, liste(suivi.outilsSensibilisationStringify))
        poser(R.string.nombre_d_hommes_sensibilises, suivi.nombreHommesSensibilises)
        poser(R.string.nombre_de_femmes_sensibilisees, suivi.nombreFemmesSensibilisees)
        poser(R.string.nombre_de_garcons_sensibilises, suivi.nombreGarconsSensibilises)
        poser(R.string.nombre_de_filles_sensibilisees, suivi.nombreFillesSensibilisees)
        poser(R.string.total_des_personnes_sensibilisees, totalSensibilises(suivi))
        poser(R.string.telephone_du_producteur, suivi.telephoneProducteurSensibilisation)
        poser(
            R.string.photo_de_la_sensibilisation,
            if (suivi.photoSensibilisation.isNullOrBlank()) "" else PHOTO_PRISE
        )

        return lignes
    }

    private fun entier(valeur: String?) = valeur?.trim()?.toIntOrNull() ?: 0

    /**
     * Total affiche a titre indicatif : le serveur le recalcule et le mobile ne le transmet pas.
     * Le montrer permet a l'agent de reperer une saisie aberrante avant l'envoi.
     */
    private fun totalSensibilises(suivi: SuiviEnfantModel): String =
        (entier(suivi.nombreHommesSensibilises) + entier(suivi.nombreFemmesSensibilisees) +
                entier(suivi.nombreGarconsSensibilises) + entier(suivi.nombreFillesSensibilisees))
            .toString()

    private const val PHOTO_PRISE = "Photo prise"
}
