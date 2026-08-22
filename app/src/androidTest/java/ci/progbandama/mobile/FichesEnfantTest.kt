package ci.progbandama.mobile

import android.content.Context
import android.view.ContextThemeWrapper
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ci.progbandama.mobile.tools.FichesEnfant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import ci.progbandama.mobile.test.R as TestR

/**
 * Mecanisme de fiches enfant repetables - lot 1, ligne 1.4.
 *
 * Cette brique n'a aucun precedent dans l'application et trois modules vont en dependre :
 * elle est verifiee avant d'etre utilisee, plutot qu'a travers eux.
 */
@RunWith(AndroidJUnit4::class)
class FichesEnfantTest {

    private lateinit var contexte: Context
    private lateinit var conteneur: LinearLayout

    /** Modele de test, reduit a ce que le controleur manipule. */
    private data class FicheTest(val nom: String)

    @Before
    fun preparer() {
        // Contexte du paquet de TEST, et non de l'application sous test.
        //
        // La fiche de test vit dans les ressources androidTest : son identifiant appartient
        // a la table de ressources du paquet de test. L'inflater avec le contexte de
        // l'application resolvait le meme identifiant dans une autre table, et inflatait
        // donc une ressource sans rapport - d'ou une InflateException sur un animateur
        // Material. Le theme de plateforme evite en outre toute dependance au theme applicatif.
        contexte = ContextThemeWrapper(
            InstrumentationRegistry.getInstrumentation().context,
            android.R.style.Theme_Material_Light
        )
        conteneur = LinearLayout(contexte).apply { orientation = LinearLayout.VERTICAL }
    }

    private fun controleur(suivi: ((Int) -> Unit)? = null) = FichesEnfant<FicheTest>(
        contexte = contexte,
        conteneur = conteneur,
        layoutFiche = TestR.layout.fiche_enfant_test,
        idNumero = TestR.id.labelNumeroTest,
        idBoutonRetirer = TestR.id.btnRetirerTest,
        surCreation = { _, _ -> },
        lecture = { vue, _ ->
            val nom = vue.findViewById<TextView>(TestR.id.champNomTest).text.toString()
            if (nom.isBlank()) null else FicheTest(nom)
        },
        ecriture = { vue, modele ->
            vue.findViewById<TextView>(TestR.id.champNomTest).setText(modele.nom)
        },
        surChangementNombre = suivi
    )

    private fun saisir(index: Int, nom: String) {
        conteneur.getChildAt(index).findViewById<TextView>(TestR.id.champNomTest).setText(nom)
    }

    private fun numero(index: Int): String =
        conteneur.getChildAt(index).findViewById<TextView>(TestR.id.labelNumeroTest).text.toString()

    // ------------------------------------------------------------------ ajout et retrait

    @Test
    fun ajouter_empile_les_fiches_et_les_numerote() {
        val c = controleur()
        c.ajouter(); c.ajouter(); c.ajouter()

        assertEquals(3, c.taille())
        assertEquals("Enfant n°1", numero(0))
        assertEquals("Enfant n°2", numero(1))
        assertEquals("Enfant n°3", numero(2))
    }

    @Test
    fun retirer_une_fiche_du_milieu_renumerote_les_suivantes() {
        val c = controleur()
        c.ajouter(); c.ajouter(); c.ajouter()
        saisir(0, "Premier"); saisir(1, "Deuxieme"); saisir(2, "Troisieme")

        c.retirer(conteneur.getChildAt(1))

        assertEquals(2, c.taille())
        assertEquals("Enfant n°1", numero(0))
        assertEquals("Enfant n°2", numero(1))
        assertEquals(
            "La saisie des fiches conservees ne doit pas bouger",
            listOf("Premier", "Troisieme"), c.collecter().map { it.nom }
        )
    }

    @Test
    fun le_bouton_de_retrait_de_la_fiche_la_supprime() {
        val c = controleur()
        c.ajouter(); c.ajouter()

        conteneur.getChildAt(0).findViewById<TextView>(TestR.id.btnRetirerTest).performClick()

        assertEquals(1, c.taille())
    }

    // ------------------------------------------------------------------ plafond

    @Test
    fun le_plafond_bloque_les_ajouts_au_dela_de_l_effectif_declare() {
        val c = controleur()
        c.definirPlafond(2)

        assertNotNull(c.ajouter())
        assertNotNull(c.ajouter())
        assertNull("La troisieme fiche doit etre refusee", c.ajouter())
        assertEquals(2, c.taille())
    }

    @Test
    fun sans_plafond_les_ajouts_sont_libres() {
        val c = controleur()
        c.definirPlafond(null)
        repeat(6) { c.ajouter() }
        assertEquals(6, c.taille())
    }

    // ------------------------------------------------------------------ generation automatique

    @Test
    fun synchroniser_cree_le_nombre_de_fiches_demande() {
        val c = controleur()
        c.synchroniserDepuisNombre(4)
        assertEquals(4, c.taille())
        assertEquals("Enfant n°4", numero(3))
    }

    @Test
    fun synchroniser_a_la_baisse_retire_par_la_fin_et_preserve_les_saisies() {
        val c = controleur()
        c.synchroniserDepuisNombre(3)
        saisir(0, "Premier"); saisir(1, "Deuxieme"); saisir(2, "Troisieme")

        c.synchroniserDepuisNombre(2)

        assertEquals(2, c.taille())
        assertEquals(
            "Les fiches conservees sont les premieres, avec leur saisie",
            listOf("Premier", "Deuxieme"), c.collecter().map { it.nom }
        )
    }

    @Test
    fun synchroniser_a_zero_vide_le_conteneur() {
        val c = controleur()
        c.synchroniserDepuisNombre(3)
        c.synchroniserDepuisNombre(0)
        assertEquals(0, c.taille())
    }

    @Test
    fun synchroniser_avec_un_nombre_negatif_ne_plante_pas() {
        val c = controleur()
        c.synchroniserDepuisNombre(2)
        c.synchroniserDepuisNombre(-5)
        assertEquals(0, c.taille())
    }

    // ------------------------------------------------------------------ collecte et reprise

    @Test
    fun collecter_ignore_les_fiches_vides() {
        val c = controleur()
        c.ajouter(); c.ajouter(); c.ajouter()
        saisir(0, "Renseignee")
        // les fiches 1 et 2 restent vides : la fonction de lecture renvoie null

        assertEquals(listOf("Renseignee"), c.collecter().map { it.nom })
    }

    @Test
    fun restaurer_recree_les_fiches_et_leur_contenu() {
        val c = controleur()
        c.restaurer(listOf(FicheTest("Aya"), FicheTest("Koffi"), FicheTest("Seydou")))

        assertEquals(3, c.taille())
        assertEquals(listOf("Aya", "Koffi", "Seydou"), c.collecter().map { it.nom })
        assertEquals("Enfant n°3", numero(2))
    }

    @Test
    fun restaurer_passe_outre_le_plafond_puis_le_retablit() {
        val c = controleur()
        c.definirPlafond(2)

        // Un brouillon peut contenir plus de fiches que l'effectif actuellement declare :
        // les perdre a la relecture serait pire que depasser le plafond.
        c.restaurer(listOf(FicheTest("A"), FicheTest("B"), FicheTest("C"), FicheTest("D")))
        assertEquals(4, c.taille())

        assertNull("Le plafond redevient actif apres la restauration", c.ajouter())
    }

    @Test
    fun restaurer_une_liste_vide_vide_le_conteneur() {
        val c = controleur()
        c.ajouter(); c.ajouter()
        c.restaurer(emptyList())
        assertEquals(0, c.taille())
    }

    // ------------------------------------------------------------------ suivi de l'effectif

    @Test
    fun l_effectif_est_signale_a_chaque_variation() {
        val effectifs = mutableListOf<Int>()
        val c = controleur(suivi = { effectifs.add(it) })

        c.ajouter()
        c.ajouter()
        c.retirer(conteneur.getChildAt(0))

        assertEquals(
            "Le dernier effectif signale doit correspondre au nombre de fiches",
            c.taille(), effectifs.last()
        )
    }
}
