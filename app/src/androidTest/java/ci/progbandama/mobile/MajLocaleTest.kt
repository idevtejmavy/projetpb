package ci.progbandama.mobile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ci.progbandama.mobile.models.LocaliteModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.tools.MajLocale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Correctif E5 - le remplacement d'une table lors de la mise a jour doit etre tout ou rien.
 *
 * Le correctif D3 avait deja repousse la purge apres la reception de la reponse du serveur.
 * Restait la fenetre entre la purge et la fin des insertions : une coupure a cet instant laissait
 * la table vide ou amputee, et l'agent sans ses listes pour saisir hors ligne.
 *
 * Test instrumente sur base Room en memoire : aucune donnee reelle de l'appareil n'est touchee.
 */
@RunWith(AndroidJUnit4::class)
class MajLocaleTest {

    private lateinit var base: ProgBandRoomDatabase
    private val agent = "1"

    @Before
    fun creerBaseEnMemoire() {
        val contexte = ApplicationProvider.getApplicationContext<Context>()
        base = Room.inMemoryDatabaseBuilder(contexte, ProgBandRoomDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun fermerBase() {
        base.close()
    }

    private fun localite(nom: String, transmise: Boolean = true) = LocaliteModel(
        id = 0,
        nom = nom,
        sectionId = "1",
        uid = 0,
        isSynced = transmise,
        agentId = agent,
        origin = if (transmise) "remote" else "local"
    )

    private fun noms(): List<String> = base.localiteDoa().getAll(agent).map { it.nom.orEmpty() }

    // ------------------------------------------------------------------ cas nominal

    @Test
    fun le_remplacement_substitue_bien_l_ancien_contenu() {
        val dao = base.localiteDoa()
        dao.insert(localite("Ancienne"))

        val remplace = MajLocale.remplacer(
            base = base,
            libelle = "localites",
            elements = listOf(localite("Nouvelle 1"), localite("Nouvelle 2")),
            purge = { dao.deleteSynced() }
        ) { dao.insert(it) }

        assertTrue(remplace)
        assertEquals(listOf("Nouvelle 1", "Nouvelle 2"), noms().sorted())
    }

    @Test
    fun une_reponse_vide_mais_valide_vide_bien_la_table() {
        val dao = base.localiteDoa()
        dao.insert(localite("Ancienne"))

        // Une liste vide n'est pas une panne : le serveur dit que la table ne contient rien.
        MajLocale.remplacer(
            base = base,
            libelle = "localites",
            elements = emptyList<LocaliteModel>(),
            purge = { dao.deleteSynced() }
        ) { dao.insert(it) }

        assertTrue(noms().isEmpty())
    }

    // ------------------------------------------------------------------ la fenetre fermee

    @Test
    fun une_coupure_pendant_les_insertions_laisse_la_table_intacte() {
        val dao = base.localiteDoa()
        dao.insert(localite("Ancienne 1"))
        dao.insert(localite("Ancienne 2"))

        var insertions = 0
        var releve: Exception? = null
        try {
            MajLocale.remplacer(
                base = base,
                libelle = "localites",
                elements = listOf(localite("Nouvelle 1"), localite("Nouvelle 2"), localite("Nouvelle 3")),
                purge = { dao.deleteSynced() }
            ) {
                // Coupure au milieu de la reinsertion : c'est le pire moment, la table est purgee
                // et partiellement remplie.
                if (insertions == 2) throw IllegalStateException("coupure simulee")
                dao.insert(it)
                insertions++
            }
        } catch (ex: Exception) {
            releve = ex
        }

        assertEquals(
            "L'ancien contenu doit etre integralement restaure",
            listOf("Ancienne 1", "Ancienne 2"), noms().sorted()
        )
        assertTrue(
            "L'exception doit remonter a la chaine de telechargement, qui seule sait la signaler",
            releve is IllegalStateException
        )
    }

    @Test
    fun une_coupure_pendant_la_purge_laisse_la_table_intacte() {
        val dao = base.localiteDoa()
        dao.insert(localite("Ancienne"))

        try {
            MajLocale.remplacer(
                base = base,
                libelle = "localites",
                elements = listOf(localite("Nouvelle")),
                purge = {
                    dao.deleteSynced()
                    throw IllegalStateException("coupure pendant la purge")
                }
            ) { dao.insert(it) }
        } catch (ignore: Exception) {
        }

        assertEquals(listOf("Ancienne"), noms())
    }

    // ------------------------------------------------------------------ reponse inexploitable

    @Test
    fun une_reponse_nulle_ne_purge_rien() {
        val dao = base.localiteDoa()
        dao.insert(localite("Ancienne"))

        // Serveur injoignable ou reponse illisible : surtout ne rien detruire.
        val remplace = MajLocale.remplacer<LocaliteModel>(
            base = base,
            libelle = "localites",
            elements = null,
            purge = { dao.deleteSynced() }
        ) { dao.insert(it) }

        assertFalse(remplace)
        assertEquals(listOf("Ancienne"), noms())
    }

    @Test
    fun sans_base_ouverte_rien_n_est_tente() {
        var purgeAppelee = false

        val remplace = MajLocale.remplacer(
            base = null,
            libelle = "localites",
            elements = listOf(localite("Nouvelle")),
            purge = { purgeAppelee = true }
        ) { }

        assertFalse(remplace)
        assertFalse("La purge ne doit meme pas etre tentee", purgeAppelee)
    }

    // ------------------------------------------------------------------ insertion groupee

    @Test
    fun le_remplacement_en_bloc_est_annule_en_entier() {
        val dao = base.localiteDoa()
        dao.insert(localite("Ancienne"))

        try {
            MajLocale.enBloc(base, "referentiel classes") {
                dao.deleteSynced()
                dao.insert(localite("Nouvelle"))
                throw IllegalStateException("coupure simulee")
            }
        } catch (ignore: Exception) {
        }

        assertEquals(listOf("Ancienne"), noms())
    }
}
