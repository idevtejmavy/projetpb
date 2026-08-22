package ci.progbandama.mobile.tools

import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.EnfantMenageModel
import ci.progbandama.mobile.models.EnqueteMenageModel
import com.google.gson.reflect.TypeToken

/**
 * Recapitulatif d'une Enquete Menage avant enregistrement - lot 3, ligne 3.6.
 *
 * Le recapitulatif des autres formulaires est bati par le binding par reflexion de
 * Commons.getAllTitleAndValueViews(), qui parcourt les vues visibles. Il ne convient pas ici :
 * il ne descend pas dans les fiches enfant, qui representent l'essentiel de l'enquete, et il
 * restituerait les libelles de l'ecran plutot que ce qui part reellement au serveur.
 *
 * Le recapitulatif est donc construit a partir du modele, c'est-a-dire de ce qui sera enregistre.
 * L'agent relit ce qu'il envoie, pas ce qu'il croit avoir saisi - c'est tout l'interet de l'ecran
 * pour une enquete qu'il ne pourra plus corriger une fois transmise, le serveur n'offrant aucun
 * endpoint de relecture.
 *
 * Les sections sans objet sont omises : une enquete arretee sur une indisponibilite n'affiche pas
 * de caracteristiques du menage, comme le formulaire n'en demandait pas.
 */
object RecapEnqueteMenage {

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

    fun lignes(enquete: EnqueteMenageModel, enfants: List<EnfantMenageModel>): List<Ligne> {
        val lignes = mutableListOf<Ligne>()

        fun poser(libelle: Int, valeur: String?, enfant: Int? = null) {
            // Une information non renseignee n'apparait pas : la faire figurer vide donnerait a
            // relire une ligne qui ne dit rien, et allongerait un ecran deja long.
            if (!valeur.isNullOrBlank()) lignes.add(Ligne(libelle, valeur.trim(), enfant))
        }

        fun poserListe(libelle: Int, valeurs: List<String>, enfant: Int? = null) {
            poser(libelle, valeurs.joinToString("\n"), enfant)
        }

        // ------------------------------------------------------------ identification
        poser(R.string.section, enquete.sectionNom)
        poser(R.string.localite, enquete.localiteNom)
        poser(R.string.producteur, enquete.producteurNom)
        poser(R.string.date_de_l_enquete, enquete.dateEnquete)
        poser(R.string.nom_de_l_enqueteur, enquete.nomEnqueteur)
        poser(R.string.position_gps, positionLisible(enquete))

        // ------------------------------------------------------------ repondant
        poser(R.string.le_producteur_est_il_le_repondant, enquete.estProducteurRepondant)
        poser(R.string.nom_du_repondant, enquete.nomRepondant)
        poser(R.string.titre_du_repondant, enquete.titreRepondant)

        // ------------------------------------------------------------ premier arret
        poser(R.string.le_producteur_est_il_disponible, enquete.producteurDisponible)
        if (enquete.producteurDisponible == NON) {
            poser(R.string.raison_de_l_indisponibilite, enquete.raisonIndisponibilite)
            poser(R.string.date_de_replanification, enquete.datePlanification)
            poserListe(R.string.raison_s_du_refus, liste(enquete.raisonRefusStringify))
            poser(R.string.autre_raison_du_refus, enquete.autreRaisonRefus)
            return lignes
        }

        // ------------------------------------------------------------ second arret
        poser(R.string.consentement_du_producteur, enquete.consentement)
        if (enquete.consentement != OUI) return lignes

        // ------------------------------------------------------------ caracteristiques du menage
        poser(R.string.situation_matrimoniale, enquete.situationMatrimoniale)
        poser(R.string.nombre_d_adultes, enquete.nombreAdultes)
        poser(R.string.nombre_d_enfants_0_4, enquete.nombreEnfants0a4)
        poser(R.string.nombre_d_enfants_5_17, enquete.nombreEnfants5a17)
        poser(R.string.total_des_personnes_du_menage, totalMenage(enquete))
        poser(R.string.y_a_t_il_des_enfants_a_charge, enquete.aEnfantACharge)

        // ------------------------------------------------------------ troisieme arret
        if (enquete.aEnfantACharge != OUI) return lignes
        poser(R.string.nombre_d_enfants_a_charge, enquete.nombreEnfantsACharge)

        // ------------------------------------------------------------ fiches enfant
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
            poserListe(R.string.situations_pfte_constatees, enfant.situationsPfte, n)
            poserListe(R.string.raisons_du_travail_ou_de_l_abus, enfant.raisonTravailAbus, n)
            poser(R.string.autre_raison, enfant.autreRaisonTravailAbus, n)
            poserListe(R.string.mesures_au_niveau_de_l_enfant, enfant.mesuresEnfant, n)
            poserListe(R.string.mesures_au_niveau_du_menage, enfant.mesuresMenage, n)
            poserListe(R.string.mesures_au_niveau_de_la_communaute, enfant.mesuresCommunaute, n)
            poser(R.string.autre_mesure, enfant.autreMesure, n)
        }

        // ------------------------------------------------------------ sensibilisation
        poserListe(R.string.themes_abordes, liste(enquete.themesSensibilisationStringify))
        poser(R.string.autre_theme, enquete.autreThemeSensibilisation)
        poserListe(R.string.outils_utilises, liste(enquete.outilsSensibilisationStringify))
        poser(R.string.nombre_d_hommes_sensibilises, enquete.nombreHommesSensibilises)
        poser(R.string.nombre_de_femmes_sensibilisees, enquete.nombreFemmesSensibilisees)
        poser(R.string.nombre_de_garcons_sensibilises, enquete.nombreGarconsSensibilises)
        poser(R.string.nombre_de_filles_sensibilisees, enquete.nombreFillesSensibilisees)
        poser(R.string.total_des_personnes_sensibilisees, totalSensibilises(enquete))
        poser(R.string.telephone_du_producteur, enquete.telephoneProducteurSensibilisation)
        poser(
            R.string.photo_de_la_sensibilisation,
            if (enquete.photoSensibilisation.isNullOrBlank()) "" else PHOTO_PRISE
        )

        return lignes
    }

    /**
     * Position mise en forme.
     *
     * Latitude et longitude sur une ligne, precision a la suite : lues separement elles ne disent
     * rien a l'agent, et c'est la precision qui lui permet de juger si la mesure vaut quelque chose.
     */
    private fun positionLisible(enquete: EnqueteMenageModel): String {
        if (enquete.latitude.isNullOrBlank() || enquete.longitude.isNullOrBlank()) return ""
        val base = "${enquete.latitude} , ${enquete.longitude}"
        val precision = enquete.precisionGps
        return if (precision.isNullOrBlank()) base else "$base (± $precision m)"
    }

    private fun entier(valeur: String?) = valeur?.trim()?.toIntOrNull() ?: 0

    /**
     * Totaux affiches a titre indicatif.
     *
     * Le serveur les recalcule a partir des effectifs : ils ne sont pas transmis. Les montrer
     * permet a l'agent de reperer une saisie aberrante avant l'envoi.
     */
    private fun totalMenage(enquete: EnqueteMenageModel): String =
        (entier(enquete.nombreAdultes) + entier(enquete.nombreEnfants0a4) +
                entier(enquete.nombreEnfants5a17)).toString()

    private fun totalSensibilises(enquete: EnqueteMenageModel): String =
        (entier(enquete.nombreHommesSensibilises) + entier(enquete.nombreFemmesSensibilisees) +
                entier(enquete.nombreGarconsSensibilises) + entier(enquete.nombreFillesSensibilisees))
            .toString()

    private const val OUI = "Oui"
    private const val NON = "Non"
    private const val PHOTO_PRISE = "Photo prise"
}
