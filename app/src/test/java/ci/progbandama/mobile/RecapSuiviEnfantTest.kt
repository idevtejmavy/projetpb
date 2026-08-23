package ci.progbandama.mobile

import ci.progbandama.mobile.models.SuiviEnfantModel
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.RecapSuiviEnfant
import ci.progbandama.mobile.tools.VocabulairesPfte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recapitulatif du Suivi Enfant Travailleur - lot 5.
 *
 * Dernier point ou l'agent peut corriger : le serveur n'expose aucun endpoint de relecture.
 *
 * L'enjeu propre a ce module est la relecture des QUATRE valeurs a codes. Le modele porte "1",
 * "4", "3" ; relues telles quelles, elles n'apprendraient rien a l'agent - et la presence est le
 * cas le plus trompeur, ses libelles etant Oui et Non comme partout ailleurs.
 */
class RecapSuiviEnfantTest {

    private fun libelles(lignes: List<RecapSuiviEnfant.Ligne>) = lignes.map { it.libelle }

    private fun valeur(lignes: List<RecapSuiviEnfant.Ligne>, libelle: Int) =
        lignes.firstOrNull { it.libelle == libelle }?.valeur

    private fun complet() = SuiviEnfantModel(
        enfantId = "412",
        dateEnquete = "23-08-2026",
        nomEnqueteur = "TCHIMOU",
        nom = "KOFFI AYA",
        dateNaissance = "12-03-2015",
        sexe = "F",
        lienParente = "Fils/Fille du producteur/rice",
        raisonNeVitPasParents = "N/A",
        situationScolaire = VocabulairesPfte.SCOLARISE,
        niveauScolaire = "CM2",
        extraitNaissance = "Oui",
        presentDisponible = "1",
        heuresTravailSemaine = "10",
        joursTravail = "3",
        heuresTravailJournee = "4",
        nombreHommesSensibilises = "4",
        nombreFemmesSensibilisees = "6",
        nombreGarconsSensibilises = "2",
        nombreFillesSensibilisees = "3",
        telephoneProducteurSensibilisation = "0700000000",
        photoSensibilisation = "data:image/jpeg;base64,AAAA",
        actionsRemediationStringify = BrouillonPfte.serialiserListe(
            listOf("Sensibilisation", "Kit scolaire")
        ),
        situationsPfteStringify = BrouillonPfte.serialiserListe(listOf("Port de charges lourdes")),
        mesuresEnfantStringify = BrouillonPfte.serialiserListe(listOf("Kit scolaire")),
        themesSensibilisationStringify = BrouillonPfte.serialiserListe(listOf("Défrichage")),
        outilsSensibilisationStringify = BrouillonPfte.serialiserListe(listOf("Affiche")),
        enfantNom = "KOFFI AYA (S2AM-BD-014-EN12.1)",
        producteurNom = "AMANI KOFFI THIERRY",
        localiteNom = "MOROFE_JUIN 2026"
    )

    // ------------------------------------------------------------------ identification

    @Test
    fun l_enfant_suivi_ouvre_le_recapitulatif() {
        val lignes = RecapSuiviEnfant.lignes(complet())

        assertEquals(
            listOf(R.string.enfant_suivi, R.string.producteur, R.string.localite),
            libelles(lignes).take(3)
        )
        assertEquals("KOFFI AYA (S2AM-BD-014-EN12.1)", valeur(lignes, R.string.enfant_suivi))
    }

    @Test
    fun le_rattachement_est_relu_depuis_ce_qui_a_ete_retenu_a_la_saisie() {
        // Le mobile ne relit pas le serveur : producteur et localite viennent du cache local.
        val lignes = RecapSuiviEnfant.lignes(complet())

        assertEquals("AMANI KOFFI THIERRY", valeur(lignes, R.string.producteur))
        assertEquals("MOROFE_JUIN 2026", valeur(lignes, R.string.localite))
    }

    @Test
    fun les_actions_de_remediation_sont_listees_une_par_ligne() {
        assertEquals(
            "Sensibilisation\nKit scolaire",
            valeur(RecapSuiviEnfant.lignes(complet()), R.string.actions_de_remediation)
        )
    }

    // ------------------------------------------------------------------ les quatre codes

    @Test
    fun la_presence_est_relue_en_clair() {
        // Le cas le plus trompeur : les libelles sont Oui et Non comme partout, mais le modele
        // porte "1" et "0".
        assertEquals("Oui", valeur(RecapSuiviEnfant.lignes(complet()), R.string.present_et_disponible))

        val absent = complet().apply { presentDisponible = VocabulairesPfte.ABSENT }
        assertEquals("Non", valeur(RecapSuiviEnfant.lignes(absent), R.string.present_et_disponible))
    }

    @Test
    fun le_motif_d_absence_est_relu_en_clair() {
        val absent = complet().apply {
            presentDisponible = VocabulairesPfte.ABSENT
            raisonAbsent = "4"
        }
        assertEquals("Vacances scolaires",
            valeur(RecapSuiviEnfant.lignes(absent), R.string.raison_de_l_absence))
    }

    @Test
    fun les_indicateurs_de_temps_de_travail_sont_relus_en_clair() {
        val lignes = RecapSuiviEnfant.lignes(complet())

        assertEquals("10", valeur(lignes, R.string.heures_de_travail_par_semaine))
        assertEquals("Sur 3 jours", valeur(lignes, R.string.jours_travailles_par_semaine))
        assertEquals("4h", valeur(lignes, R.string.heures_journee_la_plus_chargee))
    }

    @Test
    fun le_code_zero_est_relu_par_son_libelle_complet() {
        val suivi = complet().apply {
            joursTravail = "0"
            heuresTravailJournee = "0"
        }
        val lignes = RecapSuiviEnfant.lignes(suivi)

        assertEquals("L'enfant n'a fait aucune tâche",
            valeur(lignes, R.string.jours_travailles_par_semaine))
        assertEquals("L'enfant n'a fait aucune tâche",
            valeur(lignes, R.string.heures_journee_la_plus_chargee))
    }

    @Test
    fun un_code_inconnu_n_affiche_pas_de_ligne_plutot_qu_une_ligne_fausse() {
        val suivi = complet().apply { raisonAbsent = "42" }
        assertFalse(libelles(RecapSuiviEnfant.lignes(suivi)).contains(R.string.raison_de_l_absence))
    }

    @Test
    fun un_enfant_present_n_affiche_pas_de_motif_d_absence() {
        assertFalse(libelles(RecapSuiviEnfant.lignes(complet()))
            .contains(R.string.raison_de_l_absence))
    }

    // ------------------------------------------------------------------ sensibilisation

    @Test
    fun le_total_des_personnes_sensibilisees_est_recalcule() {
        // Le serveur le recalcule et le mobile ne le transmet pas : le montrer permet de reperer
        // une saisie aberrante avant l'envoi.
        assertEquals("15",
            valeur(RecapSuiviEnfant.lignes(complet()), R.string.total_des_personnes_sensibilisees))
    }

    @Test
    fun un_effectif_non_numerique_ne_fait_pas_echouer_le_total() {
        val suivi = complet().apply { nombreHommesSensibilises = "" }
        assertEquals("11",
            valeur(RecapSuiviEnfant.lignes(suivi), R.string.total_des_personnes_sensibilisees))
    }

    @Test
    fun la_photo_est_signalee_sans_etre_restituee() {
        assertEquals("Photo prise",
            valeur(RecapSuiviEnfant.lignes(complet()), R.string.photo_de_la_sensibilisation))
    }

    @Test
    fun l_absence_de_photo_ne_produit_aucune_ligne() {
        val suivi = complet().apply { photoSensibilisation = "" }
        assertFalse(libelles(RecapSuiviEnfant.lignes(suivi))
            .contains(R.string.photo_de_la_sensibilisation))
    }

    // ------------------------------------------------------------------ omissions

    @Test
    fun une_information_non_renseignee_n_apparait_pas() {
        val suivi = complet().apply { autreMesure = "" }
        assertFalse(libelles(RecapSuiviEnfant.lignes(suivi)).contains(R.string.autre_mesure))
    }

    @Test
    fun une_liste_vide_n_affiche_pas_de_ligne() {
        val suivi = complet().apply {
            situationsPfteStringify = "[]"
            mesuresEnfantStringify = "[]"
        }
        val lignes = libelles(RecapSuiviEnfant.lignes(suivi))

        assertFalse(lignes.contains(R.string.situations_pfte_constatees))
        assertFalse(lignes.contains(R.string.mesures_au_niveau_de_l_enfant))
    }

    @Test
    fun la_classe_est_relue_telle_qu_elle_sera_transmise() {
        assertEquals("CM2", valeur(RecapSuiviEnfant.lignes(complet()), R.string.classe))
    }
}
