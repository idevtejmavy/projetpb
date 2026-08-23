package ci.progbandama.mobile

import ci.progbandama.mobile.models.EnfantPlantationModel
import ci.progbandama.mobile.models.VisitePlantationModel
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.ControlesVisitePlantation
import ci.progbandama.mobile.tools.VocabulairesPfte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Controles de saisie de la Visite Plantation - lot 4.
 *
 * Ce module est le seul des trois dont l'endpoint valide ce qu'il recoit : une visite incomplete
 * revient en 422, et l'agent ne la corrige qu'en la reprenant depuis la file d'attente, souvent
 * des jours plus tard et loin de la plantation. Ces controles lui evitent le voyage.
 *
 * Deux comportements contre-intuitifs sont verrouilles ici parce qu'un correctif futur pourrait
 * les casser sans que personne ne le voie :
 *  - une visite SANS aucun enfant est valable, c'est un resultat de depistage ;
 *  - les indicateurs de temps de travail sont exiges des qu'une fiche existe.
 */
class ControlesVisitePlantationTest {

    private fun libelles(manques: List<ControlesVisitePlantation.Manque>) = manques.map { it.libelle }

    // ------------------------------------------------------------------ jeux d'essai

    private fun identifiee() = VisitePlantationModel(
        sectionId = "131",
        localiteId = "212",
        producteurId = "12094",
        jourVisite = "Vendredi",
        latitude = "6.8341",
        longitude = "-5.2766",
        estProducteurRepondant = "Oui"
    )

    private fun enfantComplet() = EnfantPlantationModel(
        nom = "KOUASSI AMENAN",
        dateNaissance = "14-06-2013",
        sexe = "F",
        lienParente = "Fils/Fille du producteur/rice",
        raisonNeVitPasParents = "N/A",
        situationScolaire = VocabulairesPfte.SCOLARISE,
        niveauScolaire = "CM1",
        extraitNaissance = "Oui",
        heuresTravailSemaine = "12",
        joursTravail = "3",
        heuresTravailJournee = "4",
        situationsPfte = mutableListOf("Port de charges lourdes"),
        mesuresEnfant = mutableListOf("Sensibilisation"),
        mesuresMenage = mutableListOf("Sensibilisation"),
        mesuresCommunaute = mutableListOf("Sensibilisation")
    )

    /** Visite menee jusqu'au bout, avec un enfant. */
    private fun complete() = identifiee().apply {
        producteurDisponible = "Oui"
        raisonIndisponibilite = VocabulairesPfte.SANS_OBJET
        consentement = "Oui"
        superficiePlantation = "4.5"
        nombreManoeuvresPermanents = "2"
        manoeuvresPermanentsMoins18 = "Non"
        nombreManoeuvresJournaliers = "5"
        manoeuvresJournaliersMoins18 = "Oui"
        nombreEnfants0a4 = "1"
        nombreEnfants5a17 = "1"
        nombrePersonnesTrouvees = "9"
    }

    // ------------------------------------------------------------------ cas nominal

    @Test
    fun une_visite_complete_ne_signale_rien() {
        val manques = ControlesVisitePlantation.manquants(complete(), listOf(enfantComplet()))
        assertTrue("Rien ne devrait manquer, or : $manques", manques.isEmpty())
    }

    // ------------------------------------------------------------------ identification

    @Test
    fun l_identification_est_exigee_sur_tous_les_chemins() {
        val manques = libelles(
            ControlesVisitePlantation.manquants(VisitePlantationModel(), emptyList())
        )

        assertTrue(manques.contains(R.string.section))
        assertTrue(manques.contains(R.string.localite))
        assertTrue(manques.contains(R.string.producteur))
        assertTrue(manques.contains(R.string.position_gps))
    }

    @Test
    fun le_jour_de_visite_est_exige_meme_si_l_enquete_s_arrete() {
        // Propre a ce module, et exige par le back-office : il ne depend d'aucun chemin.
        val visite = identifiee().apply {
            jourVisite = ""
            producteurDisponible = "Non"
            raisonIndisponibilite = "Décédé(e)"
        }
        assertTrue(libelles(ControlesVisitePlantation.manquants(visite, emptyList()))
            .contains(R.string.jour_de_la_visite))
    }

    @Test
    fun un_repondant_autre_que_le_producteur_exige_son_nom_et_son_titre() {
        val visite = identifiee().apply {
            estProducteurRepondant = "Non"
            producteurDisponible = "Oui"
            consentement = "Non"
        }
        val manques = libelles(ControlesVisitePlantation.manquants(visite, emptyList()))

        assertTrue(manques.contains(R.string.nom_du_repondant))
        assertTrue(manques.contains(R.string.titre_du_repondant))
    }

    // ------------------------------------------------------------------ premier arret : indisponible

    @Test
    fun un_producteur_indisponible_arrete_la_visite_sans_exiger_la_suite() {
        val visite = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = "N'est plus membre de la coopérative"
        }
        assertTrue(ControlesVisitePlantation.manquants(visite, emptyList()).isEmpty())
    }

    @Test
    fun une_absence_temporaire_exige_une_date_de_replanification() {
        val visite = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.ABSENCE_TEMPORAIRE
        }
        assertTrue(libelles(ControlesVisitePlantation.manquants(visite, emptyList()))
            .contains(R.string.date_de_replanification))

        visite.datePlanification = "08-09-2026"
        assertTrue(ControlesVisitePlantation.manquants(visite, emptyList()).isEmpty())
    }

    @Test
    fun un_refus_exige_au_moins_une_raison() {
        val visite = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.REFUS
        }
        assertTrue(libelles(ControlesVisitePlantation.manquants(visite, emptyList()))
            .contains(R.string.raison_s_du_refus))

        visite.raisonRefusStringify = BrouillonPfte.serialiserListe(listOf("Prime non payée"))
        assertTrue(ControlesVisitePlantation.manquants(visite, emptyList()).isEmpty())
    }

    // ------------------------------------------------------------------ second arret : non consentement

    @Test
    fun un_refus_de_consentement_arrete_la_visite() {
        val visite = identifiee().apply {
            producteurDisponible = "Oui"
            consentement = "Non"
        }
        assertTrue(ControlesVisitePlantation.manquants(visite, emptyList()).isEmpty())
    }

    // ------------------------------------------------------------------ caracteristiques

    @Test
    fun les_caracteristiques_de_la_plantation_sont_exigees_des_le_consentement() {
        val visite = identifiee().apply {
            producteurDisponible = "Oui"
            consentement = "Oui"
        }
        val manques = libelles(ControlesVisitePlantation.manquants(visite, emptyList()))

        assertTrue(manques.contains(R.string.superficie_de_la_plantation))
        assertTrue(manques.contains(R.string.nombre_de_manoeuvres_permanents))
        assertTrue(manques.contains(R.string.manoeuvres_permanents_moins_18))
        assertTrue(manques.contains(R.string.nombre_de_manoeuvres_journaliers))
        assertTrue(manques.contains(R.string.manoeuvres_journaliers_moins_18))
        assertTrue(manques.contains(R.string.nombre_d_enfants_0_4_trouves))
        assertTrue(manques.contains(R.string.nombre_d_enfants_5_17_trouves))
        assertTrue(manques.contains(R.string.nombre_de_personnes_trouvees))
    }

    // ------------------------------------------------------------------ le cas contre-intuitif

    @Test
    fun une_visite_sans_aucun_enfant_est_valable() {
        // "Aucun enfant trouve" est un resultat de depistage a part entiere : le back-office lui
        // attribue ce statut. Exiger une fiche serait un contresens.
        val visite = complete().apply { nombreEnfants5a17 = "0" }

        assertTrue(
            "Une visite sans enfant doit pouvoir etre enregistree",
            ControlesVisitePlantation.manquants(visite, emptyList()).isEmpty()
        )
    }

    @Test
    fun des_enfants_declares_mais_aucune_fiche_renseignee_sont_signales() {
        // Une fiche generee puis laissee vide n'est pas collectee : l'effectif et le nombre de
        // fiches divergent alors, et c'est ce que le serveur comparera.
        val visite = complete().apply { nombreEnfants5a17 = "3" }

        assertTrue(libelles(ControlesVisitePlantation.manquants(visite, emptyList()))
            .contains(R.string.fiches_enfant_incompletes))
    }

    @Test
    fun une_fiche_manquante_sur_plusieurs_declarees_est_signalee() {
        val visite = complete().apply { nombreEnfants5a17 = "2" }

        assertTrue(libelles(ControlesVisitePlantation.manquants(visite, listOf(enfantComplet())))
            .contains(R.string.fiches_enfant_incompletes))
    }

    @Test
    fun autant_de_fiches_que_declarees_ne_signale_rien() {
        val visite = complete().apply { nombreEnfants5a17 = "2" }
        val manques = ControlesVisitePlantation.manquants(
            visite, listOf(enfantComplet(), enfantComplet())
        )
        assertTrue("Rien ne devrait manquer, or : $manques", manques.isEmpty())
    }

    // ------------------------------------------------------------------ fiches enfant

    @Test
    fun une_fiche_incomplete_est_signalee_avec_son_numero() {
        val visite = complete().apply { nombreEnfants5a17 = "2" }
        val manques = ControlesVisitePlantation.manquants(
            visite, listOf(enfantComplet(), EnfantPlantationModel(nom = "SEYDOU"))
        )

        assertTrue("La premiere fiche est complete", manques.none { it.enfant == 1 })
        val seconde = manques.filter { it.enfant == 2 }.map { it.libelle }
        assertTrue(seconde.contains(R.string.date_de_naissance))
        assertTrue(seconde.contains(R.string.sexe))
    }

    @Test
    fun les_trois_indicateurs_de_temps_de_travail_sont_exiges() {
        // Coeur du depistage : une fiche sans eux n'apprend rien sur la situation de l'enfant.
        val enfant = enfantComplet().apply {
            heuresTravailSemaine = ""
            joursTravail = ""
            heuresTravailJournee = ""
        }
        val manques = libelles(ControlesVisitePlantation.manquants(complete(), listOf(enfant)))

        assertTrue(manques.contains(R.string.heures_de_travail_par_semaine))
        assertTrue(manques.contains(R.string.jours_travailles_par_semaine))
        assertTrue(manques.contains(R.string.heures_journee_la_plus_chargee))
    }

    @Test
    fun le_code_zero_est_une_reponse_valable_pour_le_temps_de_travail() {
        // "L'enfant n'a fait aucune tache" porte le code "0" : une chaine non vide, donc une
        // reponse. La confondre avec une absence de saisie bloquerait une fiche legitime.
        val enfant = enfantComplet().apply {
            heuresTravailSemaine = "0"
            joursTravail = "0"
            heuresTravailJournee = "0"
        }
        assertTrue(ControlesVisitePlantation.manquants(complete(), listOf(enfant)).isEmpty())
    }

    @Test
    fun le_serveur_exige_une_situation_et_une_mesure_de_chaque_niveau() {
        val enfant = enfantComplet().apply {
            situationsPfte = mutableListOf()
            mesuresEnfant = mutableListOf()
            mesuresMenage = mutableListOf()
            mesuresCommunaute = mutableListOf()
        }
        val manques = libelles(ControlesVisitePlantation.manquants(complete(), listOf(enfant)))

        assertTrue(manques.contains(R.string.situations_pfte_constatees))
        assertTrue(manques.contains(R.string.mesures_au_niveau_de_l_enfant))
        assertTrue(manques.contains(R.string.mesures_au_niveau_du_menage))
        assertTrue(manques.contains(R.string.mesures_au_niveau_de_la_communaute))
    }

    @Test
    fun aucune_fiche_n_est_controlee_quand_la_visite_s_est_arretee() {
        // Des fiches laissees par une saisie anterieure ne doivent pas bloquer une visite
        // legitimement close sur une indisponibilite.
        val visite = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = "Décédé(e)"
        }
        val incomplete = EnfantPlantationModel(nom = "RESTE D'UNE SAISIE")

        assertTrue(ControlesVisitePlantation.manquants(visite, listOf(incomplete)).isEmpty())
    }

    // ------------------------------------------------------------------ ordre de restitution

    @Test
    fun les_manques_suivent_l_ordre_du_formulaire() {
        val manques = libelles(
            ControlesVisitePlantation.manquants(VisitePlantationModel(), emptyList())
        )

        assertEquals(
            "L'agent doit les retrouver de haut en bas",
            listOf(R.string.section, R.string.localite, R.string.producteur,
                R.string.jour_de_la_visite, R.string.position_gps),
            manques.take(5)
        )
    }

    @Test
    fun un_identifiant_a_zero_vaut_une_absence_de_choix() {
        val visite = identifiee().apply { producteurId = "0" }
        assertTrue(libelles(ControlesVisitePlantation.manquants(visite, emptyList()))
            .contains(R.string.producteur))
    }

    @Test
    fun une_liste_vide_serialisee_ne_compte_pas_comme_un_choix() {
        val visite = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.REFUS
            raisonRefusStringify = BrouillonPfte.serialiserListe(emptyList<String>())
        }
        assertFalse(ControlesVisitePlantation.manquants(visite, emptyList()).isEmpty())
    }
}
