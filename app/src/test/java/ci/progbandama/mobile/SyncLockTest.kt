package ci.progbandama.mobile

import ci.progbandama.mobile.tools.SyncLock
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Correctif D4 - exclusion mutuelle entre l'envoi des donnees et la mise a jour locale.
 *
 * Test unitaire pur (aucun appareil requis) : SyncLock ne depend pas du framework Android.
 */
class SyncLockTest {

    /** SyncLock est un singleton : on repart d'un etat neutre avant chaque test. */
    @Before
    fun reinitialiser() {
        SyncLock.terminerEnvoi()
        SyncLock.terminerMiseAJour()
    }

    @Test
    fun `au repos les deux flux peuvent demarrer`() {
        assertFalse(SyncLock.estEnvoiEnCours)
        assertFalse(SyncLock.estMiseAJourEnCours)
        assertTrue(SyncLock.debuterEnvoi())
    }

    @Test
    fun `un envoi en cours empeche la mise a jour`() {
        assertTrue(SyncLock.debuterEnvoi())
        assertFalse(
            "La mise a jour ne doit pas pouvoir demarrer pendant un envoi",
            SyncLock.debuterMiseAJour()
        )
        assertFalse(SyncLock.estMiseAJourEnCours)
    }

    @Test
    fun `une mise a jour en cours empeche l envoi`() {
        assertTrue(SyncLock.debuterMiseAJour())
        assertFalse(
            "L'envoi ne doit pas pouvoir demarrer pendant une mise a jour",
            SyncLock.debuterEnvoi()
        )
        assertFalse(SyncLock.estEnvoiEnCours)
    }

    @Test
    fun `un second envoi est refuse tant que le premier n est pas termine`() {
        assertTrue(SyncLock.debuterEnvoi())
        assertFalse(SyncLock.debuterEnvoi())
    }

    @Test
    fun `la mise a jour devient possible une fois l envoi termine`() {
        assertTrue(SyncLock.debuterEnvoi())
        assertFalse(SyncLock.debuterMiseAJour())

        SyncLock.terminerEnvoi()

        assertFalse(SyncLock.estEnvoiEnCours)
        assertTrue(SyncLock.debuterMiseAJour())
    }

    @Test
    fun `l envoi devient possible une fois la mise a jour terminee`() {
        assertTrue(SyncLock.debuterMiseAJour())
        assertFalse(SyncLock.debuterEnvoi())

        SyncLock.terminerMiseAJour()

        assertFalse(SyncLock.estMiseAJourEnCours)
        assertTrue(SyncLock.debuterEnvoi())
    }

    @Test
    fun `liberer un verrou non pris est sans effet`() {
        SyncLock.terminerEnvoi()
        SyncLock.terminerMiseAJour()
        assertFalse(SyncLock.estEnvoiEnCours)
        assertFalse(SyncLock.estMiseAJourEnCours)
        assertTrue(SyncLock.debuterEnvoi())
    }

    /**
     * Scenario reel : le service d'envoi demarre automatiquement au retour sur le tableau de
     * bord, l'agent lance la mise a jour dans la foulee, puis le service se termine.
     */
    @Test
    fun `scenario nominal envoi puis mise a jour`() {
        assertTrue("Le service prend le verrou", SyncLock.debuterEnvoi())
        assertFalse("L'agent lance la mise a jour : elle doit patienter", SyncLock.debuterMiseAJour())

        SyncLock.terminerEnvoi()

        assertTrue("Le service a fini : la mise a jour demarre", SyncLock.debuterMiseAJour())
        assertFalse("Aucun envoi ne peut plus demarrer", SyncLock.debuterEnvoi())

        SyncLock.terminerMiseAJour()
        assertTrue(SyncLock.debuterEnvoi())
    }
}
