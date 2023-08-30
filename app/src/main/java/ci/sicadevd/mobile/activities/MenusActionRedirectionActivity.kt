package ci.sicadevd.mobile.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import ci.sicadevd.mobile.R
import ci.sicadevd.mobile.activities.forms.*
import ci.sicadevd.mobile.activities.lists.*
import ci.sicadevd.mobile.repositories.databases.SicadevdRoomDatabase
import ci.sicadevd.mobile.repositories.databases.daos.*
import ci.sicadevd.mobile.tools.Commons
import ci.sicadevd.mobile.tools.Constants
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.SPUtils
import kotlinx.android.synthetic.main.activity_menus_action_redirection.*

class MenusActionRedirectionActivity : AppCompatActivity(R.layout.activity_menus_action_redirection) {


    var from: String? = ""
    var formationDao: FormationDao? = null
    var localiteDao: LocaliteDao? = null
    var livraisonDao: LivraisonDao? = null
    var parcelleDao: ParcelleDao? = null
    var suiviParcelleDao: SuiviParcelleDao? = null
    var producteurDao: ProducteurDao? = null
    var producteurMenageDao: ProducteurMenageDao? = null
    var sicadevdRoomDatabase: SicadevdRoomDatabase? = null


    fun refreshDatasDraft() {
        labelSeeDraftsCountenuAction.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                from ?: ""
            ).toString()
    }


    fun refreshDatas(fromData: String) {
        when (fromData.uppercase()) {
            "LOCALITE" -> {
                labelUnsyncDatasCountCountenuAction.text = localiteDao?.getUnSyncedAll(
                    agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )?.size.toString()
            }
            "PRODUCTEUR" -> {
                labelUnsyncDatasCountCountenuAction.text = producteurDao?.getUnSyncedAll(
                    agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )?.size.toString()
            }
            "INFOS_PRODUCTEUR" -> {
                labelUnsyncDatasCountCountenuAction.text = SicadevdRoomDatabase.getDatabase(this)?.infosProducteurDao()
                    ?.getUnSyncedAll(
                        SPUtils.getInstance().getInt(Constants.AGENT_ID).toString()
                    )?.size.toString()
            }
            "MENAGE" -> {
                labelUnsyncDatasCountCountenuAction.text = producteurMenageDao?.getUnSyncedAll(
                    agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )?.size.toString()
            }
            "PARCELLE" -> {
                labelUnsyncDatasCountCountenuAction.text = parcelleDao?.getUnSyncedAll(
                    agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )?.size.toString()
            }
            "SUIVI_PARCELLE" -> {
                labelUnsyncDatasCountCountenuAction.text = suiviParcelleDao?.getUnSyncedAll(
                    agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )?.size.toString()
            }
            "INSPECTION" -> {
                labelUnsyncDatasCountCountenuAction.text = SicadevdRoomDatabase.getDatabase(this)?.inspectionDao()
                    ?.getUnSyncedAll(
                        SPUtils.getInstance().getInt(Constants.AGENT_ID).toString()
                    )?.size.toString()
            }
            "SSRTE" -> {
                labelUnsyncDatasCountCountenuAction.text = SicadevdRoomDatabase.getDatabase(this)?.enqueteSsrtDao()
                    ?.getUnSyncedAll()?.size.toString()
            }
            "FORMATION" -> {
                labelUnsyncDatasCountCountenuAction.text = formationDao?.getUnSyncedAll(
                    agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )?.size.toString()
            }
            "ESTIMATION" -> {
                labelUnsyncDatasCountCountenuAction.text = SicadevdRoomDatabase.getDatabase(this)?.estimationDao()
                    ?.getUnSyncedAll()?.size.toString()
            }
            "SUIVI_APPLICATION" -> {
                labelUnsyncDatasCountCountenuAction.text = SicadevdRoomDatabase.getDatabase(this)?.suiviApplicationDao()
                    ?.getUnSyncedAll()?.size.toString()
            }
            "LIVRAISON" -> {
                labelUnsyncDatasCountCountenuAction.text = livraisonDao?.getUnSyncedAll(
                    agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
                )?.size.toString()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        refreshDatasDraft()
        refreshDatas(from ?: "")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            sicadevdRoomDatabase = SicadevdRoomDatabase.getDatabase(this);
            producteurDao = sicadevdRoomDatabase?.producteurDoa()
            parcelleDao = sicadevdRoomDatabase?.parcelleDao();
            localiteDao = sicadevdRoomDatabase?.localiteDoa();
            producteurMenageDao = sicadevdRoomDatabase?.producteurMenageDoa()
            suiviParcelleDao = sicadevdRoomDatabase?.suiviParcelleDao()
            formationDao = sicadevdRoomDatabase?.formationDao()
            livraisonDao = sicadevdRoomDatabase?.livraisonDao()

            from = it.getStringExtra("from")
            labelTitleMenuAction.text = labelTitleMenuAction.text.toString().plus(from?.uppercase()?.replace("_", " "))
            buildInterfaceAccordingAction(from ?: "")
        }

        clickCloseMenuAction.setOnClickListener {
            finish()
        }

//        goToHomeAction.setOnClickListener {
//            ActivityUtils.startActivity(DashboardAgentActivity::class.java)
//        }

        linearUpdateContentMenuAction.setOnClickListener {
            redirectMenu(from ?: "", "UPDATE")
        }

        linearAddContentMenuAction.setOnClickListener {
            redirectMenu(from ?: "", "ADD")
        }

        linearSeeDraftsMenuAction.setOnClickListener {
            redirectMenu(fromMenu = from ?: "", "DRAFTS")
        }

        linearUnsyncDatasMenuAction.setOnClickListener {
            redirectMenu(from ?: "", "DATAS")
        }



    }


    fun buildInterfaceAccordingAction(fromMenu: String) {
        when (fromMenu.uppercase()) {
            "INSPECTION",
            "SSRTE",
            "LOCALITE",
            "INFOS_PRODUCTEUR",
            "MENAGE",
            "SUIVI_PARCELLE",
            "FORMATION",
            "CALCUL_ESTIMATION",
            "SUIVI_APPLICATION",
            "LIVRAISON" -> {
                linearUpdateContentMenuAction.visibility = View.GONE
            }

        }
    }


    fun redirectMenu(fromMenu: String, actionMenu: String) {
        when (actionMenu.uppercase()) {
            "ADD" -> {
                when (fromMenu.uppercase()) {
                    "LOCALITE" -> Commons.showMessage("Cette fonctionnalité est désactivé", this, finished = true, callback = {}, positive = "OKAY", deconnec = false, showNo = false) // ActivityUtils.startActivity(LocaliteActivity::class.java)
                    "PRODUCTEUR" -> ActivityUtils.startActivity(ProducteurActivity::class.java)
                    "INFOS_PRODUCTEUR" -> ActivityUtils.startActivity(UniteAgricoleProducteurActivity::class.java)
                    "MENAGE" -> ActivityUtils.startActivity(ProducteurMenageActivity::class.java)
                    "PARCELLE" -> ActivityUtils.startActivity(ParcelleActivity::class.java)
                    "SUIVI_PARCELLE" -> ActivityUtils.startActivity(SuiviParcelleActivity::class.java)
                    "INSPECTION" -> ActivityUtils.startActivity(InspectionActivity::class.java)
                    "SSRTE" -> ActivityUtils.startActivity(SsrtClmsActivity::class.java)
                    "FORMATION" -> ActivityUtils.startActivity(FormationActivity::class.java)
                    "CALCUL_ESTIMATION" -> ActivityUtils.startActivity(CalculEstimationActivity::class.java)
                    "SUIVI_APPLICATION" -> ActivityUtils.startActivity(SuiviApplicationActivity::class.java)
                    "LIVRAISON" -> ActivityUtils.startActivity(LivraisonActivity::class.java)
                }
            }

            "UPDATE" -> {
                val intentUpdateContent = Intent(this, UpdateContentsListActivity::class.java)
                intentUpdateContent.putExtra("fromContent", fromMenu)
                ActivityUtils.startActivity(intentUpdateContent)
            }

            "DRAFTS" -> {
                val intentDraft = Intent(this, DatasDraftedListActivity::class.java)
                intentDraft.putExtra("fromMenu", fromMenu)
                ActivityUtils.startActivity(intentDraft)
            }

            "DATAS" -> {
                when (fromMenu.uppercase()) {
                    "LOCALITE" -> Commons.showMessage("Cette fonctionnalité est désactivé", this, finished = true, callback = {}, positive = "OKAY", deconnec = false, showNo = false) //ActivityUtils.startActivity(LocalitesListActivity::class.java)
                    "PRODUCTEUR" -> ActivityUtils.startActivity(ProducteursListActivity::class.java)
                    //"INFOS_PRODUCTEUR" -> ActivityUtils.startActivity(UniteAgricoleProducteurActivity::class.java)
                    "MENAGE" -> ActivityUtils.startActivity(MenageresListActivity::class.java)
                    "PARCELLE" -> ActivityUtils.startActivity(ParcellesListActivity::class.java)
                    "SUIVI_PARCELLE" -> ActivityUtils.startActivity(SuiviPacellesListActivity::class.java)
                    //"INSPECTION" -> ActivityUtils.startActivity(EvaluationActivity::class.java)
                    //"SSRTE" -> ActivityUtils.startActivity(SsrtClmsActivity::class.java)
                    "FORMATION" -> ActivityUtils.startActivity(FormationsListActivity::class.java)
                    //"CALCUL_ESTIMATION" -> ActivityUtils.startActivity(CalculEstimationActivity::class.java)
                    //"SUIVI_APPLICATION" -> ActivityUtils.startActivity(SuiviApplicationActivity::class.java)
                    "LIVRAISON" -> ActivityUtils.startActivity(LivraisonsListActivity::class.java)
                }
            }
        }


    }

}
