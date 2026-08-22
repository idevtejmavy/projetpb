package ci.progbandama.mobile

import ci.progbandama.mobile.tools.BrouillonPfte
import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Relecture des brouillons PFTE - lot 1, ligne 1.5.
 *
 * Le point verifie ici est le seul qui puisse faire perdre une saisie sans la moindre trace :
 * une liste ecrite dans le brouillon puis relue vide, sans erreur ni message. Le test compare
 * les deux reglages de Gson sur le meme contenu pour rendre l'ecart visible.
 */
class BrouillonPfteTest {

    /**
     * Modele reduit reproduisant la forme des modeles de l'application : une colonne texte
     * portant la liste, doublee d'un champ expose en ecriture seule pour l'API.
     */
    private data class FicheEnfantTest(
        var nom: String = "",
        var age: Int = 0,
        var travauxStringify: String = "",
        // Meme forme que EnqueteSsrtModel.travauxLegers : part au serveur, ne se relit pas.
        @Expose(serialize = true, deserialize = false) var travaux: MutableList<String> = mutableListOf()
    )

    /** Reglage d'ApiClient, reconstruit ici pour ne pas demarrer la couche reseau en test JVM. */
    private val gsonApi = GsonBuilder().excludeFieldsWithoutExposeAnnotation().setLenient().create()

    private fun fiche() = FicheEnfantTest(
        nom = "Aya",
        age = 12,
        travauxStringify = """["Desherbage","Portage de charges"]""",
        travaux = mutableListOf("Desherbage", "Portage de charges")
    )

    // ------------------------------------------------------------------ le piege evite

    @Test
    fun le_gson_de_l_api_perd_les_listes_a_la_relecture() {
        val relu = gsonApi.fromJson(gsonApi.toJson(fiche()), FicheEnfantTest::class.java)

        assertTrue(
            "deserialize = false : la liste repart vide, c'est ce que ce reglage ne peut pas faire",
            relu.travaux.isEmpty()
        )
    }

    @Test
    fun le_gson_des_brouillons_relit_les_listes_a_l_identique() {
        val relu = BrouillonPfte.gson.fromJson(
            BrouillonPfte.gson.toJson(fiche()), FicheEnfantTest::class.java
        )

        assertEquals(listOf("Desherbage", "Portage de charges"), relu.travaux)
    }

    @Test
    fun le_gson_des_brouillons_conserve_aussi_les_champs_simples() {
        val relu = BrouillonPfte.gson.fromJson(
            BrouillonPfte.gson.toJson(fiche()), FicheEnfantTest::class.java
        )

        assertEquals("Aya", relu.nom)
        assertEquals(12, relu.age)
        assertEquals("""["Desherbage","Portage de charges"]""", relu.travauxStringify)
    }

    // ------------------------------------------------------------------ listes de fiches

    @Test
    fun une_liste_de_fiches_se_relit_dans_le_meme_ordre() {
        val fiches = listOf(
            FicheEnfantTest(nom = "Aya", age = 12),
            FicheEnfantTest(nom = "Koffi", age = 9),
            FicheEnfantTest(nom = "Seydou", age = 15)
        )

        val relu = BrouillonPfte.deserialiserListe(
            BrouillonPfte.serialiserListe(fiches),
            object : TypeToken<MutableList<FicheEnfantTest>>() {}
        )

        assertEquals(listOf("Aya", "Koffi", "Seydou"), relu.map { it.nom })
        assertEquals(listOf(12, 9, 15), relu.map { it.age })
    }

    @Test
    fun une_liste_de_fiches_conserve_les_listes_internes() {
        val relu = BrouillonPfte.deserialiserListe(
            BrouillonPfte.serialiserListe(listOf(fiche())),
            object : TypeToken<MutableList<FicheEnfantTest>>() {}
        )

        assertEquals(1, relu.size)
        assertEquals(listOf("Desherbage", "Portage de charges"), relu.first().travaux)
    }

    @Test
    fun serialiser_une_liste_absente_donne_un_tableau_vide() {
        assertEquals("[]", BrouillonPfte.serialiserListe<String>(null))
    }

    @Test
    fun relire_une_colonne_vide_ou_illisible_ne_plante_pas() {
        val type = object : TypeToken<MutableList<FicheEnfantTest>>() {}

        assertTrue(BrouillonPfte.deserialiserListe(null, type).isEmpty())
        assertTrue(BrouillonPfte.deserialiserListe("", type).isEmpty())
        assertTrue(BrouillonPfte.deserialiserListe("   ", type).isEmpty())
        assertTrue(BrouillonPfte.deserialiserListe("{ceci n'est pas du json", type).isEmpty())
    }

    // ------------------------------------------------------------------ contenu de brouillon

    @Test
    fun lire_un_brouillon_absent_ou_vide_renvoie_null() {
        assertNull(BrouillonPfte.lire(null, FicheEnfantTest::class.java))
    }

    @Test
    fun les_types_de_brouillon_des_trois_modules_sont_distincts() {
        val types = listOf(
            BrouillonPfte.TYPE_ENQUETE_MENAGE,
            BrouillonPfte.TYPE_VISITE_PLANTATION,
            BrouillonPfte.TYPE_SUIVI_ENFANT
        )

        assertEquals(
            "Un type partage melangerait les brouillons de deux modules dans la meme liste",
            types.size, types.toSet().size
        )
        assertTrue("Aucun type ne doit etre vide", types.none { it.isBlank() })
    }
}
