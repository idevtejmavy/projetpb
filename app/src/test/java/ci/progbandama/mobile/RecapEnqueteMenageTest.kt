package ci.progbandama.mobile

import ci.progbandama.mobile.models.EnfantMenageModel
import ci.progbandama.mobile.models.EnqueteMenageModel
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.RecapEnqueteMenage
import ci.progbandama.mobile.tools.VocabulairesPfte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Recapitulatif avant enregistrement - lot 3, ligne 3.6.
 *
 * C'est le dernier point ou l'agent peut corriger : le serveur n'expose aucun endpoint de
 * relecture pour ce module, une enquete transmise n'est plus modifiable depuis le telephone. Un
 * recapitulatif qui omettrait une information saisie, ou qui en afficherait une devenue sans
 * objet, tromperait donc l'agent sur ce qu'il envoie.
 */
class RecapEnqueteMenageTest {

    private fun libelles(lignes: List<RecapEnqueteMenage.Ligne>) = lignes.map { it.libelle }

    private fun valeur(lignes: List<RecapEnqueteMenage.Ligne>, libelle: Int, enfant: Int? = null) =
        lignes.firstOrNull { it.libelle == libelle && it.enfant == enfant }?.valeur

    private fun identifiee() = EnqueteMenageModel(
        sectionNom = "ABENGOUROU",
        localiteNom = "NIABLE",
        producteurNom = "KOUAME YAO",
        dateEnquete = "22-08-2026",
        nomEnqueteur = "TCHIMOU",
        latitude = "5.3599",
        longitude = "-4.0083",
        precisionGps = "4.5",
        estProducteurRepondant = "Oui",
        nomRepondant = "KOUAME YAO"
    )

    private fun enfant() = EnfantMenageModel(
        nom = "KOFFI AYA",
        dateNaissance = "12-03-2015",
        sexe = "F",
        lienParente = "Fils/Fille du producteur/rice",
        situationScolaire = VocabulairesPfte.SCOLARISE,
        niveauScolaire = "CM2",
        extraitNaissance = "Oui",
        situationsPfte = mutableListOf("Port de charges lourdes", "Travail de nuit"),
        mesuresEnfant = mutableListOf("Sensibilisation de l'enfant"),
        mesuresMenage = mutableListOf("Sensibilisation du menage"),
        mesuresCommunaute = mutableListOf("Sensibilisation de la communaute")
    )

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
        themesSensibilisationStringify = BrouillonPfte.serialiserListe(
            listOf("Travail des enfants", "Scolarisation")
        )
        outilsSensibilisationStringify = BrouillonPfte.serialiserListe(listOf("Boite a images"))
        nombreHommesSensibilises = "4"
        nombreFemmesSensibilisees = "6"
        nombreGarconsSensibilises = "3"
        nombreFillesSensibilisees = "2"
        telephoneProducteurSensibilisation = "0700000000"
        photoSensibilisation = "data:image/jpeg;base64,AAAA"
    }

    // ------------------------------------------------------------------ ce qui est montre

    @Test
    fun l_identification_ouvre_le_recapitulatif() {
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(enfant()))

        assertEquals(
            listOf(R.string.section, R.string.localite, R.string.producteur),
            libelles(lignes).take(3)
        )
        assertEquals("NIABLE", valeur(lignes, R.string.localite))
    }

    @Test
    fun la_position_est_donnee_avec_sa_precision() {
        // Lues separement, latitude et longitude ne disent rien ; c'est la precision qui permet
        // a l'agent de juger si la mesure vaut quelque chose.
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(enfant()))
        assertEquals("5.3599 , -4.0083 (± 4.5 m)", valeur(lignes, R.string.position_gps))
    }

    @Test
    fun une_position_absente_n_apparait_pas() {
        val enquete = complete().apply { latitude = ""; longitude = "" }
        assertFalse(libelles(RecapEnqueteMenage.lignes(enquete, listOf(enfant())))
            .contains(R.string.position_gps))
    }

    @Test
    fun une_information_non_renseignee_n_apparait_pas() {
        // Une ligne vide n'apprend rien et allonge un ecran deja long.
        val enquete = complete().apply { autreThemeSensibilisation = "" }
        assertFalse(libelles(RecapEnqueteMenage.lignes(enquete, listOf(enfant())))
            .contains(R.string.autre_theme))
    }

    // ------------------------------------------------------------------ totaux indicatifs

    @Test
    fun les_totaux_sont_recalcules_pour_relecture() {
        // Le serveur les recalcule et le mobile ne les transmet pas : les montrer permet de
        // reperer une saisie aberrante avant l'envoi.
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(enfant()))

        assertEquals("6", valeur(lignes, R.string.total_des_personnes_du_menage))
        assertEquals("15", valeur(lignes, R.string.total_des_personnes_sensibilisees))
    }

    @Test
    fun un_effectif_non_numerique_ne_fait_pas_echouer_le_total() {
        val enquete = complete().apply { nombreAdultes = "" }
        assertEquals("4", valeur(
            RecapEnqueteMenage.lignes(enquete, listOf(enfant())),
            R.string.total_des_personnes_du_menage
        ))
    }

    // ------------------------------------------------------------------ arrets anticipes

    @Test
    fun une_enquete_arretee_sur_une_indisponibilite_s_arrete_aussi_au_recapitulatif() {
        val enquete = identifiee().apply {
            producteurDisponible = "Non"
            raisonIndisponibilite = VocabulairesPfte.REFUS
            raisonRefusStringify = BrouillonPfte.serialiserListe(
                listOf("Prime non payée", "Autre raison")
            )
            autreRaisonRefus = "Deja enquete cette annee"
            // Restes d'une saisie anterieure : ils ne doivent pas etre relus.
            situationMatrimoniale = "Vit en couple"
            nombreAdultes = "2"
        }
        val lignes = RecapEnqueteMenage.lignes(enquete, listOf(enfant()))

        assertTrue(libelles(lignes).contains(R.string.raison_s_du_refus))
        assertEquals("Prime non payée\nAutre raison", valeur(lignes, R.string.raison_s_du_refus))
        assertFalse("Une enquete arretee n'a pas de caracteristiques de menage",
            libelles(lignes).contains(R.string.situation_matrimoniale))
        assertTrue("Ni de fiche enfant", lignes.none { it.enfant != null })
    }

    @Test
    fun un_refus_de_consentement_arrete_le_recapitulatif() {
        val enquete = complete().apply { consentement = "Non" }
        val lignes = RecapEnqueteMenage.lignes(enquete, listOf(enfant()))

        assertTrue(libelles(lignes).contains(R.string.consentement_du_producteur))
        assertFalse(libelles(lignes).contains(R.string.situation_matrimoniale))
    }

    @Test
    fun un_menage_sans_enfant_a_charge_n_affiche_ni_fiche_ni_sensibilisation() {
        val enquete = complete().apply { aEnfantACharge = "Non" }
        val lignes = RecapEnqueteMenage.lignes(enquete, listOf(enfant()))

        assertTrue(libelles(lignes).contains(R.string.y_a_t_il_des_enfants_a_charge))
        assertTrue(lignes.none { it.enfant != null })
        assertFalse(libelles(lignes).contains(R.string.themes_abordes))
    }

    // ------------------------------------------------------------------ fiches enfant

    @Test
    fun chaque_fiche_est_rattachee_a_son_numero() {
        val lignes = RecapEnqueteMenage.lignes(
            complete(),
            listOf(enfant(), enfant().apply { nom = "SEYDOU BAKARY"; sexe = "M" })
        )

        assertEquals("KOFFI AYA", valeur(lignes, R.string.nom, enfant = 1))
        assertEquals("SEYDOU BAKARY", valeur(lignes, R.string.nom, enfant = 2))
        assertEquals("M", valeur(lignes, R.string.sexe, enfant = 2))
    }

    @Test
    fun les_choix_multiples_d_une_fiche_sont_listes_un_par_ligne() {
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(enfant()))

        assertEquals(
            "Port de charges lourdes\nTravail de nuit",
            valeur(lignes, R.string.situations_pfte_constatees, enfant = 1)
        )
    }

    @Test
    fun la_classe_est_relue_telle_qu_elle_sera_transmise() {
        // Le prefixe de niveau sert a l'affichage du formulaire ; c'est le seul nom de la classe
        // qui part au serveur, et donc lui qu'il faut relire.
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(enfant()))
        assertEquals("CM2", valeur(lignes, R.string.classe, enfant = 1))
    }

    @Test
    fun une_fiche_sans_choix_multiple_n_affiche_pas_de_ligne_vide() {
        val depouille = enfant().apply {
            situationsPfte = mutableListOf()
            mesuresEnfant = mutableListOf()
        }
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(depouille))

        assertFalse(lignes.any { it.libelle == R.string.situations_pfte_constatees })
        assertFalse(lignes.any { it.libelle == R.string.mesures_au_niveau_de_l_enfant })
    }

    // ------------------------------------------------------------------ sensibilisation

    @Test
    fun la_photo_est_signalee_sans_etre_restituee() {
        // Le contenu est une chaine base64 de plusieurs milliers de caracteres : l'afficher
        // rendrait le recapitulatif illisible.
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(enfant()))
        val photo = valeur(lignes, R.string.photo_de_la_sensibilisation)

        assertEquals("Photo prise", photo)
    }

    @Test
    fun l_absence_de_photo_ne_produit_aucune_ligne() {
        val enquete = complete().apply { photoSensibilisation = "" }
        assertFalse(libelles(RecapEnqueteMenage.lignes(enquete, listOf(enfant())))
            .contains(R.string.photo_de_la_sensibilisation))
    }

    @Test
    fun les_themes_sont_relus_depuis_leur_colonne() {
        val lignes = RecapEnqueteMenage.lignes(complete(), listOf(enfant()))
        assertEquals("Travail des enfants\nScolarisation", valeur(lignes, R.string.themes_abordes))
    }
}
