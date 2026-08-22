package ci.progbandama.mobile.tools

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Verrou partage entre les deux flux de donnees de l'application.
 *
 *  - ENVOI       : SynchronisationIntentService, declenche automatiquement au retour sur le
 *                  tableau de bord, au demarrage de l'appareil et par alarme periodique.
 *  - MISE A JOUR : ConfigurationActivity, declenchee manuellement par l'agent.
 *
 * Les deux manipulent les memes tables Room. Sans exclusion mutuelle, le service peut lire une
 * ligne pendant que la mise a jour vide la table, puis appeler syncData() sur une ligne qui
 * n'existe plus : la donnee est alors perdue ET comptee comme transmise.
 *
 * Correctif D4 du lot "Fiabilisation de la mise a jour des donnees".
 */
object SyncLock {

    private val envoiEnCours = AtomicBoolean(false)
    private val majEnCours = AtomicBoolean(false)

    val estEnvoiEnCours: Boolean get() = envoiEnCours.get()
    val estMiseAJourEnCours: Boolean get() = majEnCours.get()

    /**
     * Prend le verrou pour l'envoi.
     * @return false si une mise a jour est en cours ou si un envoi tourne deja :
     *         l'appelant doit renoncer, l'envoi etant de toute facon replanifie.
     */
    @Synchronized
    fun debuterEnvoi(): Boolean {
        if (majEnCours.get()) return false
        return envoiEnCours.compareAndSet(false, true)
    }

    @Synchronized
    fun terminerEnvoi() {
        envoiEnCours.set(false)
    }

    /**
     * Prend le verrou pour la mise a jour.
     * @return false si un envoi est en cours : l'appelant doit patienter et reessayer.
     */
    @Synchronized
    fun debuterMiseAJour(): Boolean {
        if (envoiEnCours.get()) return false
        return majEnCours.compareAndSet(false, true)
    }

    @Synchronized
    fun terminerMiseAJour() {
        majEnCours.set(false)
    }
}
