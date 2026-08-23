package ci.progbandama.mobile

import ci.progbandama.mobile.models.EnfantPlantationModel
import ci.progbandama.mobile.models.VisitePlantationModel
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.RecapVisitePlantation
import ci.progbandama.mobile.tools.VocabulairesPfte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recapitulatif de la Visite Plantation - lot 4.
 *
 * Dernier point ou l'agent peut corriger : le serveur n'expose aucun endpoint de relecture, une
 * visite transmise n'est plus modifiable depuis le telephone.
 *
 * Deux exigences propres a ce module sont verrouillees ici :
 *  - les indicateurs de temps de travail sont RELUS EN CLAIR, alors que le modele porte des codes.
 *    Relire "3" n'apprendrait rien a l'agent ;
 *  - l'absence d'enfant est DITE, et non laissee en blanc : c'est un resultat de depistage, pas
 *    une saisie oubliee.
 */
class RecapVisitePlantationTest {

    private fun libelles(lignes: List<RecapVisitePlantation.Ligne>) = lignes.map { it.libelle }

    private fun valeur(lignes: List<RecapVisitePlantation.Ligne>, libelle: Int, enfant: Int? = null) =
        lignes.firstOrNull { it.libelle == libelle && it.enfant == enfant }?.valeur

    private fun identifiee() = VisitePlantationModel(
        sectionNom = "YAKRO JUIN_2026",
        localiteNom = "MOROFE_JUIN 2026",
        producteurNom = "AMANI KOFFI THIERRY",
        dateEnquete = "21-08-2026",
        jourVisite = "Vendredi",
        nomEnqueteur = "TCHIMOU",
        latitude = "6.8341",
        longitude = "-5.2766",
        precisionGps = "5.2",
        estProducteurRepondant = "Oui"
    )

    private fun enfant() = EnfantPlantationModel(
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
        situationsPfte = mutableListOf("Port de charges lourdes", "Travail de nuit"),
        raisonTravailAbus = mutableListOf("Pour aider les parents"),
        mesuresEnfant = mutableListOf("Sensibilisation"),
        mesuresMenage = mutableListOf("Sensibilisation"),
        mesuresCommunaute = mutableListOf("Sensibilisation")
    )

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

    // ------------------------------------------------------------------ identification

    @Test
    fun l_identification_ouvre_le_recapitulatif() {
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(enfant()))

        assertEquals(
            listOf(R.string.section, R.string.localite, R.string.producteur),
            libelles(lignes).take(3)
        )
        assertEquals("MOROFE_JUIN 2026", valeur(lignes, R.string.localite))
    }

    @Test
    fun le_jour_de_visite_figure_au_recapitulatif() {
        // Propre a ce module, et exige par le back-office : l'agent doit pouvoir le relire.
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(enfant()))
        assertEquals("Vendredi", valeur(lignes, R.string.jour_de_la_visite))
    }

    @Test
    fun la_position_est_donnee_avec_sa_precision() {
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(enfant()))
        assertEquals("6.8341 , -5.2766 (± 5.2 m)", valeur(lignes, R.string.position_gps))
    }

    @Test
    fun une_information_non_renseignee_n_apparait_pas() {
        val visite = complete().apply { nomRepondant = "" }
        assertFalse(libelles(RecapVisitePlantation.lignes(visite, listOf(enfant())))
            .contains(R.string.nom_du_repondant))
    }

    // ------------------------------------------------------------------ arrets anticipes

    @Test
    fun une_visite_arretee_sur_une_indisponibilite_s_arrete_aussi_au_recapitulatif() {
        val visite = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.REFUS
            raisonRefusStringify = BrouillonPfte.serialiserListe(
                listOf("Prime non payée", "Autre raison")
            )
            autreRaisonRefus = "Visite deja faite"
            // Restes d'une saisie anterieure : ils ne doivent pas etre relus.
            superficiePlantation = "4.5"
            nombreManoeuvresPermanents = "2"
        }
        val lignes = RecapVisitePlantation.lignes(visite, listOf(enfant()))

        assertEquals("Prime non payée\nAutre raison", valeur(lignes, R.string.raison_s_du_refus))
        assertFalse("Une visite arretee n'a pas de caracteristiques de plantation",
            libelles(lignes).contains(R.string.superficie_de_la_plantation))
        assertTrue("Ni de fiche enfant", lignes.none { it.enfant != null })
    }

    @Test
    fun un_refus_de_consentement_arrete_le_recapitulatif() {
        val visite = complete().apply { consentement = "Non" }
        val lignes = RecapVisitePlantation.lignes(visite, listOf(enfant()))

        assertTrue(libelles(lignes).contains(R.string.consentement_du_producteur))
        assertFalse(libelles(lignes).contains(R.string.superficie_de_la_plantation))
    }

    // ------------------------------------------------------------------ le cas contre-intuitif

    @Test
    fun l_absence_d_enfant_est_dite_et_non_laissee_en_blanc() {
        val lignes = RecapVisitePlantation.lignes(complete().apply { nombreEnfants5a17 = "0" }, emptyList())

        assertTrue("La ligne doit exister", libelles(lignes).contains(R.string.enfants_trouves))
        assertEquals("Aucun enfant trouvé sur la plantation",
            valeur(lignes, R.string.enfants_trouves))
    }

    @Test
    fun les_caracteristiques_restent_relues_meme_sans_enfant() {
        // La visite reste une visite : ce qui a ete constate sur la plantation doit etre relu.
        val lignes = RecapVisitePlantation.lignes(complete(), emptyList())

        assertEquals("4.5", valeur(lignes, R.string.superficie_de_la_plantation))
        assertEquals("9", valeur(lignes, R.string.nombre_de_personnes_trouvees))
    }

    // ------------------------------------------------------------------ temps de travail

    @Test
    fun les_indicateurs_de_temps_de_travail_sont_relus_en_clair() {
        // Le modele porte des codes - "3" et "4" - que le serveur enregistre tels quels. Les
        // relire ainsi n'apprendrait rien a l'agent.
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(enfant()))

        assertEquals("12", valeur(lignes, R.string.heures_de_travail_par_semaine, enfant = 1))
        assertEquals("Sur 3 jours", valeur(lignes, R.string.jours_travailles_par_semaine, enfant = 1))
        assertEquals("4h", valeur(lignes, R.string.heures_journee_la_plus_chargee, enfant = 1))
    }

    @Test
    fun le_code_zero_est_relu_par_son_libelle_complet() {
        val depouille = enfant().apply {
            joursTravail = "0"
            heuresTravailJournee = "0"
        }
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(depouille))

        assertEquals("L'enfant n'a fait aucune tâche",
            valeur(lignes, R.string.jours_travailles_par_semaine, enfant = 1))
        assertEquals("L'enfant n'a fait aucune tâche",
            valeur(lignes, R.string.heures_journee_la_plus_chargee, enfant = 1))
    }

    @Test
    fun un_code_inconnu_n_affiche_pas_de_ligne_plutot_qu_une_ligne_fausse() {
        val douteux = enfant().apply { joursTravail = "99" }
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(douteux))

        assertFalse(lignes.any { it.libelle == R.string.jours_travailles_par_semaine })
    }

    // ------------------------------------------------------------------ fiches enfant

    @Test
    fun chaque_fiche_est_rattachee_a_son_numero() {
        val lignes = RecapVisitePlantation.lignes(
            complete(),
            listOf(enfant(), enfant().apply { nom = "KOUASSI KOFFI"; sexe = "M" })
        )

        assertEquals("KOUASSI AMENAN", valeur(lignes, R.string.nom, enfant = 1))
        assertEquals("KOUASSI KOFFI", valeur(lignes, R.string.nom, enfant = 2))
        assertEquals("M", valeur(lignes, R.string.sexe, enfant = 2))
    }

    @Test
    fun les_choix_multiples_d_une_fiche_sont_listes_un_par_ligne() {
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(enfant()))

        assertEquals(
            "Port de charges lourdes\nTravail de nuit",
            valeur(lignes, R.string.situations_pfte_constatees, enfant = 1)
        )
    }

    @Test
    fun la_classe_est_relue_telle_qu_elle_sera_transmise() {
        // Le prefixe de niveau sert a l'affichage du formulaire ; seul le nom de la classe part.
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(enfant()))
        assertEquals("CM1", valeur(lignes, R.string.classe, enfant = 1))
    }

    @Test
    fun une_fiche_sans_choix_multiple_n_affiche_pas_de_ligne_vide() {
        val depouille = enfant().apply {
            situationsPfte = mutableListOf()
            raisonTravailAbus = mutableListOf()
        }
        val lignes = RecapVisitePlantation.lignes(complete(), listOf(depouille))

        assertFalse(lignes.any { it.libelle == R.string.situations_pfte_constatees })
        assertFalse(lignes.any { it.libelle == R.string.raisons_du_travail_ou_de_l_abus })
    }
}
