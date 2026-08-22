package ci.progbandama.mobile

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ci.progbandama.mobile.models.InspectionDTO
import ci.progbandama.mobile.models.LocaliteModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Correctifs D1 et D2 - la purge de la base locale ne doit jamais detruire une saisie
 * qui n'a pas encore ete transmise au serveur.
 *
 * Test instrumente sur base Room en memoire : aucune donnee reelle de l'appareil n'est touchee.
 */
@RunWith(AndroidJUnit4::class)
class PurgeSelectiveTest {

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

    // ------------------------------------------------------------------ localites

    private fun localite(nom: String, transmise: Boolean) = LocaliteModel(
        id = 0,
        nom = nom,
        sectionId = "1",
        uid = 0,
        isSynced = transmise,
        agentId = agent,
        origin = if (transmise) "remote" else "local"
    )

    @Test
    fun deleteSynced_conserve_les_localites_non_transmises() {
        val dao = base.localiteDoa()
        dao.insert(localite("Localite transmise", transmise = true))
        dao.insert(localite("Saisie hors ligne", transmise = false))
        assertEquals("Preparation", 2, dao.getAll(agent).size)

        dao.deleteSynced()

        val restantes = dao.getAll(agent)
        assertEquals("Seule la saisie non transmise doit subsister", 1, restantes.size)
        assertEquals("Saisie hors ligne", restantes.first().nom)
        assertTrue("Elle doit rester marquee non transmise", restantes.first().isSynced == false)
    }

    @Test
    fun deleteAll_supprime_tout_y_compris_le_non_transmis() {
        val dao = base.localiteDoa()
        dao.insert(localite("Localite transmise", transmise = true))
        dao.insert(localite("Saisie hors ligne", transmise = false))

        dao.deleteAll()

        assertEquals(
            "deleteAll reste destructif : c'est pourquoi il ne doit plus etre utilise " +
                    "sur les tables recevant des saisies locales",
            0, dao.getAll(agent).size
        )
    }

    @Test
    fun deleteSynced_ne_fait_rien_si_tout_est_en_attente() {
        val dao = base.localiteDoa()
        dao.insert(localite("Saisie A", transmise = false))
        dao.insert(localite("Saisie B", transmise = false))

        dao.deleteSynced()

        assertEquals(2, dao.getAll(agent).size)
    }

    // ------------------------------------------------------------------ inspections

    private fun inspection(transmise: Boolean) = InspectionDTO(
        uid = 0,
        isSynced = transmise,
        origin = if (transmise) "remote" else "local",
        agentId = agent,
        userid = agent.toInt()
    )

    @Test
    fun deleteSynced_conserve_les_inspections_non_transmises() {
        val dao = base.inspectionDao()
        dao.insert(inspection(transmise = true))
        dao.insert(inspection(transmise = false))
        assertEquals("Preparation", 1, dao.getUnSyncedAll(agent).size)

        dao.deleteSynced()

        assertEquals(
            "L'inspection saisie hors ligne doit survivre a la mise a jour",
            1, dao.getUnSyncedAll(agent).size
        )
    }
}
