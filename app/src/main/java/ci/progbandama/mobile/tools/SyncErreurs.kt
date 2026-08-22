package ci.progbandama.mobile.tools

import ci.progbandama.mobile.models.SyncErreurModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import org.joda.time.DateTime

/**
 * Motif du dernier refus du serveur, par module.
 *
 * Renseigne automatiquement par l'intercepteur reseau d'ApiClient : aucune des dix-huit
 * fonctions d'envoi de SynchronisationIntentService n'a eu besoin d'etre modifiee.
 * L'intercepteur reconnait le module a partir du chemin de l'appel.
 */
object SyncErreurs {

    /** Chemin de l'endpoint d'envoi -> code fonctionnel du module. */
    private val MODULE_PAR_CHEMIN = mapOf(
        "apiproducteur" to "PRODUCTEUR",
        "apimenage" to "MENAGE",
        "apiparcelle" to "PARCELLE",
        "apisuiviparcelle" to "PARCELLES",
        "apilocalite" to "LOCALITE",
        "apisuiviformation" to "FORMATION",
        "apivisiteur" to "FORMATION_VISITEUR",
        "apiestimation" to "ESTIMATION",
        "apiapplication" to "APPLICATION",
        "apievaluation" to "INSPECTION",
        "updateinspection" to "INSPECTION",
        "apissrteclrms" to "SSRTECLMRS",
        "apiagroevaluation" to "AGRO_EVALUATION",
        "apidistribution" to "AGRO_DISTRIBUTION",
        "postplanting" to "POSTPLANTING",
        "apilivraisonmagasinsection" to "LIVRAISON",
        "apilivraisonmagasincentral" to "LIVRAISON_MAGCENTRAL",
        "apiinfosproducteur" to "INFOS_PRODUCTEUR",
        "apienquetemenage" to "ENQUETE_MENAGE"
    )

    fun modulePourChemin(chemin: String): String? =
        MODULE_PAR_CHEMIN.entries.firstOrNull { chemin.endsWith(it.key, ignoreCase = true) }?.value

    /**
     * Message lisible par un agent de terrain.
     *
     * Le corps renvoye est volontairement ecarte : il est soit vide (erreur fatale PHP),
     * soit une page HTML, soit un JSON de validation trop technique. Seul le 422 apporte
     * une information exploitable, et on la resume.
     */
    /** Codes internes, en dehors de la plage HTTP, pour les echecs sans reponse du serveur. */
    const val CODE_SANS_RESEAU = 0
    const val CODE_RESEAU_LENT = -1

    private fun messageLisible(code: Int, corps: String?): String = when {
        code == CODE_SANS_RESEAU -> "En attente de connexion au réseau internet"
        code == CODE_RESEAU_LENT -> "Réseau trop lent : envoi interrompu, nouvelle tentative plus tard"
        code == 401 || code == 403 -> "Envoi refusé : session expirée, reconnectez-vous"
        code == 404 -> "Envoi refusé : service introuvable sur le serveur"
        code == 422 -> "Envoi refusé : données incomplètes ou invalides" +
                (corps?.let { resumeValidation(it) } ?: "")
        code in 500..599 -> "Envoi refusé : erreur $code du serveur. " +
                "La saisie est conservée, réessayez plus tard ou signalez-le."
        else -> "Envoi refusé : erreur $code du serveur"
    }

    /**
     * Vrai lorsque l'echec traduit une absence de reseau : DNS non resolu, hote injoignable,
     * aucune route. Ce sont les signatures rencontrees quand le telephone est hors couverture.
     */
    private fun estAbsenceDeReseau(panne: String?): Boolean {
        if (panne.isNullOrBlank()) return false
        return listOf(
            "UnknownHostException", "ConnectException", "NoRouteToHost",
            "Unable to resolve host", "No address associated"
        ).any { panne.contains(it, ignoreCase = true) }
    }

    /** Extrait les noms de champs d'une reponse de validation Laravel, sans le detail technique. */
    private fun resumeValidation(corps: String): String {
        val champs = Regex("\"([a-zA-Z_]+)\"\\s*:\\s*\\[").findAll(corps)
            .map { it.groupValues[1] }
            .filter { it !in setOf("details", "errors", "error", "message") }
            .distinct().take(4).toList()
        return if (champs.isEmpty()) "" else " (" + champs.joinToString(", ") + ")"
    }

    /** Enregistre le refus. Appele par l'intercepteur, hors du fil principal. */
    fun enregistrer(chemin: String, code: Int, corps: String?, panne: String? = null) {
        val module = modulePourChemin(chemin) ?: return
        val dao = ProgBandRoomDatabase.INSTANCE?.syncErreurDao() ?: return
        // La nature de la panne est traduite en code des l'enregistrement : le libelle,
        // lui, est reconstruit a l'affichage (voir messagePourModule).
        val codeReel = if (code != 0) code
        else if (estAbsenceDeReseau(panne)) CODE_SANS_RESEAU else CODE_RESEAU_LENT
        try {
            dao.insert(
                SyncErreurModel(
                    uid = 0,
                    module = module,
                    code = codeReel,
                    message = messageLisible(codeReel, corps),
                    heure = DateTime.now().toString("HH:mm"),
                    agentId = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )
            )
        } catch (ex: Exception) {
            LogUtils.e("Enregistrement du refus $module : ${ex.message}")
        }
    }

    /** Efface le refus d'un module : appele des qu'un envoi de ce module aboutit. */
    fun effacer(chemin: String) {
        val module = modulePourChemin(chemin) ?: return
        try {
            ProgBandRoomDatabase.INSTANCE?.syncErreurDao()?.effacerModule(module)
        } catch (ex: Exception) {
            LogUtils.e("Effacement du refus $module : ${ex.message}")
        }
    }

    /** Message a afficher sous les elements en attente du module, ou null si le dernier envoi a abouti. */
    fun messagePourModule(module: String?): String? {
        if (module.isNullOrBlank()) return null
        return try {
            val erreur = ProgBandRoomDatabase.INSTANCE?.syncErreurDao()?.pourModule(
                module.uppercase(),
                SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
            ) ?: return null
            // Reconstruit a partir du code plutot que repris tel quel : une evolution de
            // formulation s'applique ainsi aux refus deja enregistres. Seul le 422 conserve
            // son message, les noms de champs invalides provenant du corps de la reponse.
            val texte = if (erreur.code == 422) erreur.message else messageLisible(erreur.code, null)
            "$texte (${erreur.heure})"
        } catch (ex: Exception) {
            LogUtils.e(ex.message); null
        }
    }
}
