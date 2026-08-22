package ci.progbandama.mobile

import ci.progbandama.mobile.models.EnfantMenageModel
import ci.progbandama.mobile.models.EnqueteMenageModel
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.ControlesEnqueteMenage
import ci.progbandama.mobile.tools.VocabulairesPfte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Controles de saisie de l'Enquete Menage - lot 3, ligne 3.6.
 *
 * L'endpoint mobile n'applique aucune validation : une enquete incomplete est acceptee en base
 * sans le moindre signalement. Ces controles sont le seul filet, et une erreur ici ne se voit pas
 * - elle laisse simplement passer une enquete inexploitable. D'ou ces tests.
 *
 * Les trois chemins d'arret anticipe sont verifies un par un : ce sont eux qui distinguent une
 * enquete legitimement courte d'une enquete bâclée.
 */
class ControlesEnqueteMenageTest {

    private fun libelles(manques: List<ControlesEnqueteMenage.Manque>) = manques.map { it.libelle }

    // ------------------------------------------------------------------ jeux d'essai

    /** Identification complete : le tronc commun de tous les chemins. */
    private fun identifiee() = EnqueteMenageModel(
        sectionId = "1",
        localiteId = "12",
        producteurId = "203",
        latitude = "5.3599",
        longitude = "-4.0083",
        estProducteurRepondant = "Oui",
        nomRepondant = "KOUAME YAO"
    )

    private fun enfantComplet() = EnfantMenageModel(
        nom = "KOFFI AYA",
        dateNaissance = "12-03-2015",
        sexe = "F",
        lienParente = "Fils/Fille du producteur/rice",
        situationScolaire = VocabulairesPfte.SCOLARISE,
        extraitNaissance = "Oui",
        situationsPfte = mutableListOf("Port de charges lourdes"),
        mesuresEnfant = mutableListOf("Sensibilisation de l'enfant"),
        mesuresMenage = mutableListOf("Sensibilisation du menage"),
        mesuresCommunaute = mutableListOf("Sensibilisation de la communaute")
    )

    /** Enquete menee jusqu'au bout, avec un enfant et sa sensibilisation. */
    private fun complete() = identifiee().apply {
        producteurDisponible = "Oui"
        raisonIndisponibilite = VocabulairesPfte.SANS_OBJET
        consentement = "Oui"
        situationMatrimoniale = "Vit en couple"
        nombreAdultes = "2"
        nombreEnfants0a4 = "1"
        nombreEnfants5a17 = "3"
        aEnfantACharge = "Oui"
        nombreEnfantsACharge = "3"
        nombreEnfantsEnquetes = "3"
        themesSensibilisationStringify = BrouillonPfte.serialiserListe(listOf("Travail des enfants"))
        outilsSensibilisationStringify = BrouillonPfte.serialiserListe(listOf("Boite a images"))
        telephoneProducteurSensibilisation = "0700000000"
        photoSensibilisation = "data:image/jpeg;base64,AAAA"
    }

    // ------------------------------------------------------------------ cas nominal

    @Test
    fun une_enquete_complete_ne_signale_rien() {
        val manques = ControlesEnqueteMenage.manquants(complete(), listOf(enfantComplet()))
        assertTrue("Rien ne devrait manquer, or : $manques", manques.isEmpty())
    }

    // ------------------------------------------------------------------ identification

    @Test
    fun l_identification_est_exigee_sur_tous_les_chemins() {
        val manques = libelles(ControlesEnqueteMenage.manquants(EnqueteMenageModel(), emptyList()))

        assertTrue(manques.contains(R.string.section))
        assertTrue(manques.contains(R.string.localite))
        assertTrue(manques.contains(R.string.producteur))
        assertTrue("La position GPS fait partie de l'identification", manques.contains(R.string.position_gps))
    }

    @Test
    fun un_identifiant_a_zero_vaut_une_absence_de_choix() {
        // Une liste jamais ouverte laisse un identifiant a zero, pas une chaine vide.
        val enquete = identifiee().apply { producteurId = "0" }
        assertTrue(libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))
            .contains(R.string.producteur))
    }

    @Test
    fun un_repondant_autre_que_le_producteur_exige_son_nom_et_son_titre() {
        val enquete = identifiee().apply {
            estProducteurRepondant = "Non"
            nomRepondant = ""
            producteurDisponible = "Oui"
            consentement = "Non"
        }
        val manques = libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))

        assertTrue(manques.contains(R.string.nom_du_repondant))
        assertTrue(manques.contains(R.string.titre_du_repondant))
    }

    @Test
    fun le_titre_n_est_pas_exige_quand_le_producteur_repond_lui_meme() {
        val enquete = identifiee().apply {
            producteurDisponible = "Oui"
            consentement = "Non"
        }
        assertFalse(libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))
            .contains(R.string.titre_du_repondant))
    }

    // ------------------------------------------------------------------ premier arret : indisponible

    @Test
    fun un_producteur_indisponible_arrete_l_enquete_sans_exiger_la_suite() {
        val enquete = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = "Décédé(e)"
        }
        val manques = libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))

        assertTrue("Une enquete arretee sur une indisponibilite est complete, or : $manques",
            manques.isEmpty())
    }

    @Test
    fun une_indisponibilite_sans_motif_est_refusee() {
        val enquete = identifiee().apply { producteurDisponible = "Non" }
        assertEquals(listOf(R.string.raison_de_l_indisponibilite),
            libelles(ControlesEnqueteMenage.manquants(enquete, emptyList())))
    }

    @Test
    fun une_absence_temporaire_exige_une_date_de_replanification() {
        val enquete = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.ABSENCE_TEMPORAIRE
        }
        assertTrue(libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))
            .contains(R.string.date_de_replanification))

        enquete.datePlanification = "30-09-2026"
        assertTrue(ControlesEnqueteMenage.manquants(enquete, emptyList()).isEmpty())
    }

    @Test
    fun un_refus_exige_au_moins_une_raison() {
        val enquete = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.REFUS
        }
        assertTrue(libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))
            .contains(R.string.raison_s_du_refus))

        enquete.raisonRefusStringify = BrouillonPfte.serialiserListe(listOf("Prime non payée"))
        assertTrue(ControlesEnqueteMenage.manquants(enquete, emptyList()).isEmpty())
    }

    @Test
    fun une_date_de_replanification_n_est_pas_exigee_pour_un_refus() {
        val enquete = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.REFUS
            raisonRefusStringify = BrouillonPfte.serialiserListe(listOf("Prime non payée"))
        }
        assertFalse(libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))
            .contains(R.string.date_de_replanification))
    }

    // ------------------------------------------------------------------ second arret : non consentement

    @Test
    fun un_refus_de_consentement_arrete_l_enquete() {
        val enquete = identifiee().apply {
            producteurDisponible = "Oui"
            consentement = "Non"
        }
        assertTrue(ControlesEnqueteMenage.manquants(enquete, emptyList()).isEmpty())
    }

    @Test
    fun un_producteur_disponible_sans_reponse_de_consentement_est_refuse() {
        val enquete = identifiee().apply { producteurDisponible = "Oui" }
        assertEquals(listOf(R.string.consentement_du_producteur),
            libelles(ControlesEnqueteMenage.manquants(enquete, emptyList())))
    }

    // ------------------------------------------------------------------ troisieme arret : pas d'enfant

    @Test
    fun un_menage_sans_enfant_a_charge_n_exige_ni_fiche_ni_sensibilisation() {
        val enquete = complete().apply {
            aEnfantACharge = "Non"
            nombreEnfantsACharge = ""
            themesSensibilisationStringify = ""
            outilsSensibilisationStringify = ""
            telephoneProducteurSensibilisation = ""
            photoSensibilisation = ""
        }
        assertTrue(ControlesEnqueteMenage.manquants(enquete, emptyList()).isEmpty())
    }

    @Test
    fun les_caracteristiques_du_menage_sont_exigees_des_le_consentement() {
        val enquete = identifiee().apply {
            producteurDisponible = "Oui"
            consentement = "Oui"
        }
        val manques = libelles(ControlesEnqueteMenage.manquants(enquete, emptyList()))

        assertTrue(manques.contains(R.string.situation_matrimoniale))
        assertTrue(manques.contains(R.string.nombre_d_adultes))
        assertTrue(manques.contains(R.string.nombre_d_enfants_0_4))
        assertTrue(manques.contains(R.string.nombre_d_enfants_5_17))
        assertTrue(manques.contains(R.string.y_a_t_il_des_enfants_a_charge))
    }

    // ------------------------------------------------------------------ fiches enfant

    @Test
    fun des_enfants_a_charge_sans_aucune_fiche_sont_refuses() {
        assertTrue(libelles(ControlesEnqueteMenage.manquants(complete(), emptyList()))
            .contains(R.string.au_moins_une_fiche_enfant))
    }

    @Test
    fun une_fiche_incomplete_est_signalee_avec_son_numero() {
        val manques = ControlesEnqueteMenage.manquants(
            complete(), listOf(enfantComplet(), EnfantMenageModel(nom = "SEYDOU"))
        )

        val secondEnfant = manques.filter { it.enfant == 2 }
        assertTrue("La seconde fiche doit etre signalee", secondEnfant.isNotEmpty())
        assertTrue("La premiere fiche est complete",
            manques.none { it.enfant == 1 })
        assertTrue(secondEnfant.map { it.libelle }.contains(R.string.date_de_naissance))
    }

    @Test
    fun le_serveur_exige_une_situation_et_une_mesure_de_chaque_niveau() {
        val enfant = enfantComplet().apply {
            situationsPfte = mutableListOf()
            mesuresEnfant = mutableListOf()
            mesuresMenage = mutableListOf()
            mesuresCommunaute = mutableListOf()
        }
        val manques = libelles(ControlesEnqueteMenage.manquants(complete(), listOf(enfant)))

        assertTrue(manques.contains(R.string.situations_pfte_constatees))
        assertTrue(manques.contains(R.string.mesures_au_niveau_de_l_enfant))
        assertTrue(manques.contains(R.string.mesures_au_niveau_du_menage))
        assertTrue(manques.contains(R.string.mesures_au_niveau_de_la_communaute))
    }

    // ------------------------------------------------------------------ sensibilisation

    @Test
    fun la_sensibilisation_est_exigee_des_lors_qu_il_y_a_des_enfants_a_charge() {
        val enquete = complete().apply {
            themesSensibilisationStringify = ""
            outilsSensibilisationStringify = ""
            telephoneProducteurSensibilisation = ""
            photoSensibilisation = ""
        }
        val manques = libelles(ControlesEnqueteMenage.manquants(enquete, listOf(enfantComplet())))

        assertTrue(manques.contains(R.string.themes_abordes))
        assertTrue(manques.contains(R.string.outils_utilises))
        assertTrue(manques.contains(R.string.telephone_du_producteur))
        assertTrue(manques.contains(R.string.photo_de_la_sensibilisation))
    }

    @Test
    fun une_liste_vide_serialisee_ne_compte_pas_comme_un_choix() {
        // "[]" n'est pas une chaine vide : sans relecture, elle passerait pour renseignee.
        val enquete = complete().apply {
            themesSensibilisationStringify = BrouillonPfte.serialiserListe(emptyList<String>())
        }
        assertTrue(libelles(ControlesEnqueteMenage.manquants(enquete, listOf(enfantComplet())))
            .contains(R.string.themes_abordes))
    }

    // ------------------------------------------------------------------ ordre de restitution

    @Test
    fun les_manques_suivent_l_ordre_du_formulaire() {
        val manques = libelles(ControlesEnqueteMenage.manquants(EnqueteMenageModel(), emptyList()))

        assertEquals(
            "L'agent doit les retrouver de haut en bas",
            listOf(R.string.section, R.string.localite, R.string.producteur, R.string.position_gps),
            manques.take(4)
        )
    }
}
