package ci.progbandama.mobile.tools

import android.content.Context
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import com.blankj.utilcode.util.LogUtils

/**
 * Inventaire des donnees saisies localement et non encore transmises au serveur.
 *
 * Source unique pour l'ecran de mise a jour (ConfigurationActivity) et pour l'indicateur
 * du tableau de bord (DashboardAgentActivity), afin de ne pas dupliquer une troisieme fois
 * la liste des 17 types de donnees synchronisables.
 *
 * Correctif E6 du lot "Fiabilisation de la mise a jour des donnees".
 */
object DonneesEnAttente {

    /**
     * @return le nombre de lignes non transmises par type de donnee.
     *         Les types sans donnee en attente sont omis. L'ordre suit celui de la chaine
     *         d'envoi de SynchronisationIntentService.
     */
    fun compter(context: Context, agentId: Int): LinkedHashMap<String, Int> {
        val compte = linkedMapOf<String, Int>()
        val base = ProgBandRoomDatabase.getDatabase(context.applicationContext) ?: return compte
        val idTexte = agentId.toString()

        fun ajouter(libelle: String, taille: () -> Int) {
            try {
                val n = taille()
                if (n > 0) compte[libelle] = n
            } catch (ex: Exception) {
                LogUtils.e("Comptage $libelle : ${ex.message}")
            }
        }

        ajouter("Localites") { base.localiteDoa().getUnSyncedAll(idTexte).size }
        ajouter("Producteurs") { base.producteurDoa().getUnSyncedAll(idTexte).size }
        ajouter("Menages") { base.producteurMenageDoa().getUnSyncedAll(idTexte).size }
        ajouter("Parcelles") { base.parcelleDao().getUnSyncedAll(idTexte).size }
        ajouter("Suivis de parcelle") { base.suiviParcelleDao().getUnSyncedAll(idTexte).size }
        ajouter("Formations") { base.formationDao().getUnSyncedAll(idTexte).size }
        ajouter("Visiteurs de formation") { base.visiteurFormationDao().getUnSyncedAll(agentId).size }
        ajouter("Evaluations besoins arbres") { base.evaluationArbreDao().getUnSyncedAll(agentId).size }
        ajouter("Distributions d'arbres") { base.distributionArbreDao().getUnSyncedAll(idTexte).size }
        ajouter("Post-planting") { base.postplantingDao().getUnSyncedAll(idTexte).size }
        ajouter("Livraisons") { base.livraisonDao().getUnSyncedAll(idTexte).size }
        ajouter("Livraisons magasin central") { base.livraisonCentralDao().getUnSyncedAll(idTexte).size }
        ajouter("Estimations") { base.estimationDao().getUnSyncedAll().size }
        ajouter("Applications phytosanitaires") { base.suiviApplicationDao().getUnSyncedAll().size }
        ajouter("Enquetes SSRT/CLMRS") { base.enqueteSsrtDao().getUnSyncedAll().size }
        ajouter("Inspections") { base.inspectionDao().getUnSyncedAll(idTexte).size }
        ajouter("Infos producteur") { base.infosProducteurDao().getUnSyncedAll(idTexte).size }
        ajouter("Enquetes Menage") { base.enqueteMenageDao().getUnSyncedAll().size }
        ajouter("Visites de plantation") { base.visitePlantationDao().getUnSyncedAll().size }
        ajouter("Suivis enfant travailleur") { base.suiviEnfantDao().getUnSyncedAll().size }

        return compte
    }

    /** Nombre total de lignes en attente, tous types confondus. */
    fun total(context: Context, agentId: Int): Int = compter(context, agentId).values.sum()

    /**
     * Restitution lisible, une ligne par type : "Producteurs : 3".
     *
     * @param limite nombre maximal de lignes detaillees. Au-dela, les types restants sont
     *        regroupes sur une derniere ligne, pour ne jamais depasser la hauteur allouee
     *        a l'affichage (17 types sont interroges).
     */
    fun resume(compte: Map<String, Int>, limite: Int = 0): String {
        val lignes = compte.entries.map { "${it.key} : ${it.value}" }
        if (limite <= 0 || lignes.size <= limite) return lignes.joinToString(separator = "\n")

        val visibles = lignes.take(limite - 1)
        val reste = compte.entries.drop(limite - 1)
        val total = reste.sumOf { it.value }
        return (visibles + "et ${reste.size} autre(s) type(s) : $total").joinToString(separator = "\n")
    }

    /** Restitution compacte sur une ligne : "Producteurs (3), Parcelles (1)". */
    fun resumeCompact(compte: Map<String, Int>): String =
        compte.entries.joinToString(separator = ", ") { "${it.key} (${it.value})" }
}
