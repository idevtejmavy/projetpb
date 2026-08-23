package ci.progbandama.mobile

import ci.progbandama.mobile.models.SuiviEnfantModel
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.ControlesSuiviEnfant
import ci.progbandama.mobile.tools.VocabulairesPfte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Controles de saisie du Suivi Enfant Travailleur - lot 5.
 *
 * L'endpoint mobile de ce module ne valide rien : ces controles sont le seul filet avant
 * l'enregistrement.
 *
 * Trois comportements sont verrouilles ici parce qu'un correctif futur pourrait les casser :
 *  - le code "0" de la presence vaut "Non" et ouvre le motif d'absence ; c'est une reponse, pas
 *    une absence de saisie ;
 *  - autreMesure n'est exige que si une liste de mesures le demande, contrairement a la regle du
 *    back-office qui l'exige sans condition et se contredit elle-meme ;
 *  - la sensibilisation est toujours exigee, sans condition, contrairement a l'Enquete Menage.
 */
class ControlesSuiviEnfantTest {

    private fun libelles(manques: List<ControlesSuiviEnfant.Manque>) = manques.map { it.libelle }

    /** Suivi complet, tous chemins renseignes. */
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
        actionsRemediationStringify = BrouillonPfte.serialiserListe(listOf("Sensibilisation")),
        situationsPfteStringify = BrouillonPfte.serialiserListe(listOf("Port de charges lourdes")),
        raisonTravailAbusStringify = BrouillonPfte.serialiserListe(listOf("Pour aider les parents")),
        mesuresEnfantStringify = BrouillonPfte.serialiserListe(listOf("Kit scolaire")),
        mesuresMenageStringify = BrouillonPfte.serialiserListe(listOf("Foyer amélioré")),
        mesuresCommunauteStringify = BrouillonPfte.serialiserListe(listOf("Sensibilisation")),
        themesSensibilisationStringify = BrouillonPfte.serialiserListe(listOf("Défrichage")),
        outilsSensibilisationStringify = BrouillonPfte.serialiserListe(listOf("Affiche"))
    )

    // ------------------------------------------------------------------ cas nominal

    @Test
    fun un_suivi_complet_ne_signale_rien() {
        val manques = ControlesSuiviEnfant.manquants(complet())
        assertTrue("Rien ne devrait manquer, or : $manques", manques.isEmpty())
    }

    // ------------------------------------------------------------------ identification

    @Test
    fun l_enfant_suivi_est_exige() {
        assertTrue(libelles(ControlesSuiviEnfant.manquants(complet().apply { enfantId = "" }))
            .contains(R.string.enfant_suivi))
        assertTrue(libelles(ControlesSuiviEnfant.manquants(complet().apply { enfantId = "0" }))
            .contains(R.string.enfant_suivi))
    }

    @Test
    fun les_actions_de_remediation_sont_exigees() {
        // C'est l'objet meme d'une visite de suivi : ce qui a ete fait depuis la derniere.
        val suivi = complet().apply { actionsRemediationStringify = "[]" }
        assertTrue(libelles(ControlesSuiviEnfant.manquants(suivi))
            .contains(R.string.actions_de_remediation))
    }

    // ------------------------------------------------------------------ presence et absence

    @Test
    fun le_code_zero_de_la_presence_ouvre_le_motif_d_absence() {
        // "0" vaut "Non" : une reponse, pas un champ vide. Le confondre avec une absence de saisie
        // ferait reclamer la presence au lieu du motif.
        val absent = complet().apply { presentDisponible = VocabulairesPfte.ABSENT }
        val manques = libelles(ControlesSuiviEnfant.manquants(absent))

        assertFalse("La presence EST renseignee", manques.contains(R.string.present_et_disponible))
        assertTrue("Le motif devient obligatoire", manques.contains(R.string.raison_de_l_absence))

        absent.raisonAbsent = "4"
        assertTrue(ControlesSuiviEnfant.manquants(absent).isEmpty())
    }

    @Test
    fun un_enfant_present_n_a_pas_a_justifier_d_absence() {
        assertFalse(libelles(ControlesSuiviEnfant.manquants(complet()))
            .contains(R.string.raison_de_l_absence))
    }

    @Test
    fun les_trois_indicateurs_de_temps_de_travail_sont_exiges() {
        val suivi = complet().apply {
            heuresTravailSemaine = ""
            joursTravail = ""
            heuresTravailJournee = ""
        }
        val manques = libelles(ControlesSuiviEnfant.manquants(suivi))

        assertTrue(manques.contains(R.string.heures_de_travail_par_semaine))
        assertTrue(manques.contains(R.string.jours_travailles_par_semaine))
        assertTrue(manques.contains(R.string.heures_journee_la_plus_chargee))
    }

    @Test
    fun le_code_zero_du_temps_de_travail_est_une_reponse_valable() {
        val suivi = complet().apply {
            heuresTravailSemaine = "0"
            joursTravail = "0"
            heuresTravailJournee = "0"
        }
        assertTrue(ControlesSuiviEnfant.manquants(suivi).isEmpty())
    }

    // ------------------------------------------------------------------ scolarite

    @Test
    fun un_enfant_scolarise_doit_avoir_une_classe() {
        val suivi = complet().apply { niveauScolaire = "" }
        assertTrue(libelles(ControlesSuiviEnfant.manquants(suivi)).contains(R.string.classe))
    }

    @Test
    fun un_enfant_jamais_scolarise_doit_avoir_une_raison_mais_pas_de_classe() {
        val suivi = complet().apply {
            situationScolaire = VocabulairesPfte.JAMAIS_SCOLARISE
            niveauScolaire = ""
        }
        val manques = libelles(ControlesSuiviEnfant.manquants(suivi))

        assertFalse("Pas de classe pour un enfant jamais scolarise", manques.contains(R.string.classe))
        assertTrue(manques.contains(R.string.raisons_de_non_scolarisation))
    }

    @Test
    fun un_enfant_descolarise_doit_avoir_classe_et_raison() {
        val suivi = complet().apply {
            situationScolaire = VocabulairesPfte.DESCOLARISE
            niveauScolaire = ""
        }
        val manques = libelles(ControlesSuiviEnfant.manquants(suivi))

        assertTrue(manques.contains(R.string.classe))
        assertTrue(manques.contains(R.string.raisons_de_non_scolarisation))
    }

    @Test
    fun l_absence_d_extrait_appelle_ses_raisons() {
        val suivi = complet().apply { extraitNaissance = "Non" }
        assertTrue(libelles(ControlesSuiviEnfant.manquants(suivi))
            .contains(R.string.raisons_absence_extrait))

        suivi.raisonPasExtraitStringify = BrouillonPfte.serialiserListe(listOf("Ce n'est pas important"))
        assertTrue(ControlesSuiviEnfant.manquants(suivi).isEmpty())
    }

    // ------------------------------------------------------------------ precisions "Autre"

    @Test
    fun un_lien_de_parente_autre_appelle_sa_precision() {
        val suivi = complet().apply { lienParente = "Autre" }
        assertTrue(libelles(ControlesSuiviEnfant.manquants(suivi))
            .contains(R.string.autre_lien_de_parente))

        suivi.autreLienParente = "Voisin"
        assertTrue(ControlesSuiviEnfant.manquants(suivi).isEmpty())
    }

    @Test
    fun autre_mesure_n_est_exigee_que_si_une_liste_le_demande() {
        // ECART ASSUME : le back-office marque ce champ obligatoire sans condition, alors que son
        // propre formulaire ne l'affiche que si une liste contient "Autre" - et qu'il vide les
        // champs masques. La regle est contredite par le controle conditionnel du meme controleur.
        assertFalse(
            "Sans mesure 'Autre', la precision n'a pas lieu d'etre",
            libelles(ControlesSuiviEnfant.manquants(complet())).contains(R.string.autre_mesure)
        )

        val avecAutre = complet().apply {
            mesuresMenageStringify = BrouillonPfte.serialiserListe(listOf("Autre, préciser"))
        }
        assertTrue(libelles(ControlesSuiviEnfant.manquants(avecAutre))
            .contains(R.string.autre_mesure))
    }

    @Test
    fun une_situation_constatee_appelle_une_raison() {
        val suivi = complet().apply { raisonTravailAbusStringify = "[]" }
        assertTrue(libelles(ControlesSuiviEnfant.manquants(suivi))
            .contains(R.string.raisons_du_travail_ou_de_l_abus))
    }

    @Test
    fun la_seule_situation_aucune_n_appelle_pas_de_raison() {
        val suivi = complet().apply {
            situationsPfteStringify = BrouillonPfte.serialiserListe(listOf("Aucune"))
            raisonTravailAbusStringify = "[]"
        }
        assertTrue(ControlesSuiviEnfant.manquants(suivi).isEmpty())
    }

    // ------------------------------------------------------------------ sensibilisation

    @Test
    fun la_sensibilisation_est_toujours_exigee() {
        // Contrairement a l'Enquete Menage, elle n'est conditionnee par rien dans ce module.
        val suivi = complet().apply {
            themesSensibilisationStringify = "[]"
            outilsSensibilisationStringify = "[]"
            nombreHommesSensibilises = ""
            telephoneProducteurSensibilisation = ""
            photoSensibilisation = ""
        }
        val manques = libelles(ControlesSuiviEnfant.manquants(suivi))

        assertTrue(manques.contains(R.string.themes_abordes))
        assertTrue(manques.contains(R.string.outils_utilises))
        assertTrue(manques.contains(R.string.nombre_d_hommes_sensibilises))
        assertTrue(manques.contains(R.string.telephone_du_producteur))
        assertTrue(manques.contains(R.string.photo_de_la_sensibilisation))
    }

    @Test
    fun les_quatre_effectifs_sensibilises_sont_exiges() {
        val suivi = complet().apply {
            nombreHommesSensibilises = ""
            nombreFemmesSensibilisees = ""
            nombreGarconsSensibilises = ""
            nombreFillesSensibilisees = ""
        }
        val manques = libelles(ControlesSuiviEnfant.manquants(suivi))

        assertTrue(manques.contains(R.string.nombre_d_hommes_sensibilises))
        assertTrue(manques.contains(R.string.nombre_de_femmes_sensibilisees))
        assertTrue(manques.contains(R.string.nombre_de_garcons_sensibilises))
        assertTrue(manques.contains(R.string.nombre_de_filles_sensibilisees))
    }

    @Test
    fun un_effectif_a_zero_est_une_reponse_valable() {
        val suivi = complet().apply {
            nombreHommesSensibilises = "0"
            nombreFemmesSensibilisees = "0"
            nombreGarconsSensibilises = "0"
            nombreFillesSensibilisees = "0"
        }
        assertTrue(ControlesSuiviEnfant.manquants(suivi).isEmpty())
    }

    // ------------------------------------------------------------------ ordre de restitution

    @Test
    fun les_manques_suivent_l_ordre_du_formulaire() {
        val manques = libelles(ControlesSuiviEnfant.manquants(SuiviEnfantModel()))

        assertEquals(
            "L'agent doit les retrouver de haut en bas",
            listOf(R.string.enfant_suivi, R.string.date_de_la_visite_de_suivi,
                R.string.actions_de_remediation, R.string.nom),
            manques.take(4)
        )
    }
}
