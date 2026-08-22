package ci.progbandama.mobile.tools

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import ci.progbandama.mobile.activities.lists.DatasDraftedListActivity
import ci.progbandama.mobile.models.DataDraftedModel
import ci.progbandama.mobile.repositories.apis.ApiClient
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils

/**
 * Reprise d'une donnee en attente d'envoi.
 *
 * Une saisie deja validee mais non encore transmise n'etait pas modifiable : seuls les
 * brouillons l'etaient. Plutot que d'ajouter un chemin de rechargement dans les douze
 * formulaires concernes - trois seulement savent relire un enregistrement local via
 * l'extra "sync_uid" -, on ramene la donnee vers les brouillons :
 *
 *   1. l'enregistrement local est serialise au format brouillon ;
 *   2. il est insere dans drafted_datas avec le type attendu par le formulaire ;
 *   3. la ligne en attente est supprimee, de sorte qu'elle ne parte plus a la
 *      prochaine synchronisation et qu'aucun doublon ne soit cree.
 *
 * L'agent retrouve alors sa saisie dans les brouillons, ou le circuit de correction
 * existe deja et fonctionne pour tous les modules.
 */
object RepriseEnAttente {

    /** Modules dont les donnees en attente peuvent etre reprises. */
    val MODULES_SUPPORTES = setOf(
        // ecran generique "donnees en attente"
        "INFOS_PRODUCTEUR", "PARCELLES", "ESTIMATION", "INSPECTION", "APPLICATION",
        "FORMATION_VISITEUR", "LIVRAISON", "LIVRAISON_MAGCENTRAL", "SSRTECLMRS",
        "AGRO_EVALUATION", "AGRO_DISTRIBUTION", "POSTPLANTING",
        // ecrans de liste dedies (Producteurs, Parcelles, Menages, Formations, Localites)
        "PRODUCTEUR", "PARCELLE", "MENAGE", "FORMATION", "LOCALITE"
    )

    fun estSupporte(module: String?) = module?.uppercase() in MODULES_SUPPORTES

    /**
     * Type de brouillon attendu par le formulaire du module.
     *
     * Les valeurs proviennent des formulaires eux-memes (champ typeDraft a l'enregistrement)
     * et sont volontairement identiques a celles que DatasDraftedListActivity reconstitue.
     */
    private fun typeDraft(module: String): String = when (module.uppercase()) {
        "PARCELLES" -> "suivi_parcelle"
        "FORMATION_VISITEUR" -> "visiteur_formation"
        "LIVRAISON_MAGCENTRAL" -> "suivi_livraison_central"
        "SSRTECLMRS" -> "ssrte"
        else -> module.lowercase()
    }


    /**
     * Point d'entree unique des ecrans de liste : demande confirmation, effectue la reprise,
     * puis amene l'agent sur les brouillons du module.
     *
     * Regroupe ici plutot que duplique dans les huit adaptateurs concernes.
     */
    fun proposerReprise(context: Context, module: String?, uid: Int?) {
        if (!estSupporte(module)) {
            Toast.makeText(context, "Cette donnée ne peut pas être reprise", Toast.LENGTH_SHORT).show()
            return
        }
        if (uid == null || uid == 0) {
            Toast.makeText(context, "Donnée introuvable", Toast.LENGTH_SHORT).show()
            return
        }

        Commons.showMessage(
            message = "Reprendre cette saisie pour la corriger ? Elle quitte la file d'envoi " +
                    "et repart dans vos brouillons ; elle ne sera transmise qu'après un " +
                    "nouvel enregistrement.",
            context = context,
            finished = false,
            showNo = true,
            positive = "Reprendre",
            callback = {
                val ok = reprendre(
                    context,
                    module!!.uppercase(),
                    uid,
                    SPUtils.getInstance().getInt(Constants.AGENT_ID, 0)
                )
                if (ok) {
                    Toast.makeText(context, "Saisie déplacée vers les brouillons", Toast.LENGTH_LONG).show()
                    val versBrouillons = Intent(context, DatasDraftedListActivity::class.java)
                    versBrouillons.putExtra("fromMenu", module.lowercase())
                    context.startActivity(versBrouillons)
                    (context as? Activity)?.finish()
                } else {
                    Toast.makeText(context, "Reprise impossible pour cette donnée", Toast.LENGTH_SHORT).show()
                }
                null
            }
        )
    }

    /**
     * Ramene vers les brouillons la donnee en attente identifiee par [uid].
     *
     * @return true si la reprise a abouti.
     */
    fun reprendre(context: Context, module: String, uid: Int, agentId: Int): Boolean {
        if (uid == 0) return false
        val base = ProgBandRoomDatabase.getDatabase(context.applicationContext) ?: return false
        val idTexte = agentId.toString()

        return try {
            // On retrouve l'enregistrement parmi les donnees en attente du module plutot que
            // d'ajouter un accesseur par uid dans chacun des douze DAO. Les volumes en attente
            // sont faibles par construction : ce sont les saisies pas encore transmises.
            val modele: Any? = when (module.uppercase()) {
                "INFOS_PRODUCTEUR" -> base.infosProducteurDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "PARCELLES" -> base.suiviParcelleDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "ESTIMATION" -> base.estimationDao().getUnSyncedAll().find { it.uid == uid }
                "INSPECTION" -> base.inspectionDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "APPLICATION" -> base.suiviApplicationDao().getUnSyncedAll().find { it.uid == uid }
                "FORMATION_VISITEUR" -> base.visiteurFormationDao().getUnSyncedAll(agentId).find { it.uid == uid }
                "LIVRAISON" -> base.livraisonDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "LIVRAISON_MAGCENTRAL" -> base.livraisonCentralDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "SSRTECLMRS" -> base.enqueteSsrtDao().getUnSyncedAll().find { it.uid == uid }
                "AGRO_EVALUATION" -> base.evaluationArbreDao().getUnSyncedAll(agentId).find { it.uid == uid }
                "AGRO_DISTRIBUTION" -> base.distributionArbreDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "POSTPLANTING" -> base.postplantingDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "PRODUCTEUR" -> base.producteurDoa().getUnSyncedAll(idTexte).find { it.uid == uid }
                // uid declare en Long sur ce modele, contrairement aux autres.
                "PARCELLE" -> base.parcelleDao().getUnSyncedAll(idTexte).find { it.uid.toInt() == uid }
                // uid declare en Long sur ce modele, contrairement aux autres.
                "MENAGE" -> base.producteurMenageDoa().getUnSyncedAll(idTexte).find { it.uid.toInt() == uid }
                "FORMATION" -> base.formationDao().getUnSyncedAll(idTexte).find { it.uid == uid }
                "LOCALITE" -> base.localiteDoa().getUnSyncedAll(idTexte).find { it.uid == uid }
                else -> null
            }

            if (modele == null) {
                LogUtils.e("Reprise impossible : $module uid=$uid introuvable parmi les donnees en attente")
                return false
            }

            // Meme serialisation que celle utilisee a la mise au brouillon par les formulaires.
            base.draftedDatasDao().insert(
                DataDraftedModel(
                    uid = 0,
                    datas = ApiClient.gson.toJson(modele),
                    typeDraft = typeDraft(module),
                    agentId = idTexte
                )
            )

            // La ligne en attente disparait : elle ne sera plus transmise, et la correction
            // repartira du brouillon. Aucun doublon possible.
            when (module.uppercase()) {
                "INFOS_PRODUCTEUR" -> base.infosProducteurDao().deleteByUid(uid)
                "PARCELLES" -> base.suiviParcelleDao().deleteByUid(uid)
                "ESTIMATION" -> base.estimationDao().deleteByUid(uid)
                "INSPECTION" -> base.inspectionDao().deleteByUid(uid)
                "APPLICATION" -> base.suiviApplicationDao().deleteByUid(uid)
                "FORMATION_VISITEUR" -> base.visiteurFormationDao().deleteByUid(uid)
                "LIVRAISON" -> base.livraisonDao().deleteByUid(uid)
                // Ce DAO declare son parametre en String, contrairement aux onze autres.
                "LIVRAISON_MAGCENTRAL" -> base.livraisonCentralDao().deleteByUid(uid.toString())
                "SSRTECLMRS" -> base.enqueteSsrtDao().deleteByUid(uid)
                "AGRO_EVALUATION" -> base.evaluationArbreDao().deleteByUid(uid)
                "AGRO_DISTRIBUTION" -> base.distributionArbreDao().deleteByUid(uid)
                "POSTPLANTING" -> base.postplantingDao().deleteByUid(uid)
                "PRODUCTEUR" -> base.producteurDoa().deleteByUid(uid)
                "PARCELLE" -> base.parcelleDao().deleteByUid(uid)
                "MENAGE" -> base.producteurMenageDoa().deleteByUid(uid)
                "FORMATION" -> base.formationDao().deleteByUid(uid)
                "LOCALITE" -> base.localiteDoa().deleteByUid(uid)
            }

            true
        } catch (ex: Exception) {
            LogUtils.e("Reprise $module uid=$uid : ${ex.message}")
            false
        }
    }
}
