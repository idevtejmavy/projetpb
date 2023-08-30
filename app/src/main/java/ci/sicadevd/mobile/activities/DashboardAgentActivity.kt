package ci.sicadevd.mobile.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ci.sicadevd.mobile.R
import ci.sicadevd.mobile.activities.lists.DatasDraftedListActivity
import ci.sicadevd.mobile.broadcasts.LoopAlarmReceiver
import ci.sicadevd.mobile.models.AgentModel
import ci.sicadevd.mobile.repositories.databases.SicadevdRoomDatabase
import ci.sicadevd.mobile.repositories.databases.daos.*
import ci.sicadevd.mobile.services.GpsService
import ci.sicadevd.mobile.tools.Commons
import ci.sicadevd.mobile.tools.Constants
import com.blankj.utilcode.constant.TimeConstants
import com.blankj.utilcode.util.*
import com.google.android.material.navigation.NavigationView
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_dashboard_agent.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.net.UnknownHostException


/**
 * Created by didierboka.developer on 18/12/2021
 * mail for work:   (didierboka.developer@gmail.com)
 */


class DashboardAgentActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener {


    var sicadevdRoomDatabase: SicadevdRoomDatabase? = null
    var agentDao: AgentDao? = null;
    var formationDao: FormationDao? = null;
    var localiteDao: LocaliteDao? = null;
    var livraisonDao: LivraisonDao? = null;
    var parcelleDao: ParcelleDao? = null;
    var suiviParcelleDao: SuiviParcelleDao? = null;
    var agentLogged: AgentModel? = null
    var producteurDao: ProducteurDao? = null
    var producteurMenageDao: ProducteurMenageDao? = null
    val TAG = "DashboardAgentActivity.kt"
    var networkFlag = true


    fun bindDatas(agentModel: AgentModel?) {
        labelUserDashboard.text = agentModel?.firstname.toString().plus(" ".plus(agentModel?.lastname.toString()))
    }


    fun refreshDatas() {
        labelProducteurCount.text = producteurDao?.getUnSyncedAll(
            agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
        )?.size.toString()
        /*labelLocaliteCount.text = localiteDao?.getUnSyncedAll(
            agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
        )?.size.toString()*/
        labelMenageCount.text = producteurMenageDao?.getUnSyncedAll(
            agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
        )?.size.toString()
        labelParcelleCount.text = parcelleDao?.getUnSyncedAll(
            agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
        )?.size.toString()
        labelSuiviParcelleCount.text = suiviParcelleDao?.getUnSyncedAll(
            agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
        )?.size.toString()
        labelFormationCount.text = formationDao?.getUnSyncedAll(
            agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
        )?.size.toString()
        labelLivraisonCount.text = livraisonDao?.getUnSyncedAll(
            agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0).toString()
        )?.size.toString()
        labelEstimationCount.text = SicadevdRoomDatabase.getDatabase(this)?.estimationDao()
            ?.getUnSyncedAll()?.size.toString()
        labelApplicateurCount.text = SicadevdRoomDatabase.getDatabase(this)?.suiviApplicationDao()
            ?.getUnSyncedAll()?.size.toString()
        labelSSRTCount.text = SicadevdRoomDatabase.getDatabase(this)?.enqueteSsrtDao()
            ?.getUnSyncedAll()?.size.toString()
        labelEvaluation.text = SicadevdRoomDatabase.getDatabase(this)?.inspectionDao()
            ?.getUnSyncedAll(
                SPUtils.getInstance().getInt(Constants.AGENT_ID).toString()
            )?.size.toString()
        labelUniteAgricole.text = SicadevdRoomDatabase.getDatabase(this)?.infosProducteurDao()
            ?.getUnSyncedAll(
                SPUtils.getInstance().getInt(Constants.AGENT_ID).toString()
            )?.size.toString()

        labelProducteurDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "producteur"
            )?.toString()
        labelEstimationraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "calcul_estimation"
            )?.toString()
        /*labelLocaliteDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "localite"
            )?.toString()*/
        labelUniteAgricoleDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "infos_producteur"
            )?.toString()
        labelMenageDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "menage"
            )?.toString()
        labelParcelleDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "parcelle"
            )?.toString()
        labelSuiviParcelleDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "suivi_parcelle"
            )?.toString()
        labelFormationDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "formation"
            )?.toString()
        labelApplicateurDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "suivi_application"
            )?.toString()
        labelLivraisonDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "livraison"
            )?.toString()
        labelSSRTDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "ssrte"
            )?.toString()
        labelEvaluationDraftCount.text =
            SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.countByType(
                agentID = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                type = "inspection"
            )?.toString()
    }


    // Setup a recurring alarm every half hour
    fun scheduleAlarm() {
        // Construct an intent that will execute the AlarmReceiver
        val intent = Intent(applicationContext, LoopAlarmReceiver::class.java)
        // Create a PendingIntent to be triggered when the alarm goes off
        val pIntent = PendingIntent.getBroadcast(
            this,
            LoopAlarmReceiver.REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        // Setup periodic alarm every every half hour from this point onwards
        val firstMillis = System.currentTimeMillis() // alarm is set right away
        val alarm = this.getSystemService(ALARM_SERVICE) as AlarmManager
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
        // alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis, AlarmManager.INTERVAL_FIFTEEN_MINUTES / 3, pIntent);
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstMillis,
            (TimeConstants.MIN).toLong(),
            pIntent
        )
    }


    fun askLocationPermission() {
        val permissionLocation = Manifest.permission.ACCESS_FINE_LOCATION
        val grant = ContextCompat.checkSelfPermission(this, permissionLocation)

        if (grant != PackageManager.PERMISSION_GRANTED) {
            val permissionList = arrayOfNulls<String>(1)
            permissionList[0] = permissionLocation
            ActivityCompat.requestPermissions(this, permissionList, 2021)
        } else {
            try {
                val intentGpsService = Intent(this, GpsService::class.java)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    this.startForegroundService(intentGpsService)
                } else {
                    this.startService(intentGpsService)
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            2021 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if ((ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED)
                    ) {
                        SPUtils.getInstance().put("permission_asked", true)
                        try {

                            val intentGpsService = Intent(this@DashboardAgentActivity, GpsService::class.java)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                this.startForegroundService(intentGpsService)
                            } else {
                                this.startService(intentGpsService)
                            }
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                } else {
                    SPUtils.getInstance().put("permission_asked", false)
                }
                return
            }
        }
    }


    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)

        MainScope().launch {
            // checkNetworkAvailablility()
        }
    }


    @SuppressLint("MissingPermission")
    suspend fun checkNetworkAvailablility() {

        CoroutineScope(IO).launch {
            try {
                networkFlag = NetworkUtils.isAvailable()
            } catch (ex: UnknownHostException) {
                networkFlag = false
                LogUtils.e("Internet error !")
            }

            if (networkFlag) {
                MainScope().launch {
                    Commons.synchronisation("all",  this@DashboardAgentActivity)
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        refreshDatas()

        MainScope().launch {
            checkNetworkAvailablility()
        }
        askLocationPermission()
    }


    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // 4 - Handle Navigation Item Click

        when (item.itemId) {
            R.id.addDashboardMenu -> {

            }
            R.id.updateDashboardMenu -> {

            }
            R.id.draftsDashboardMenu -> {
                val intentDraftsList = Intent(this, DatasDraftedListActivity::class.java)
                startActivity(intentDraftsList)
            }
            else -> {

            }
        }

        // drawerDashboard.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // finally change the color
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        sicadevdRoomDatabase = SicadevdRoomDatabase.getDatabase(this)
        agentDao = sicadevdRoomDatabase?.agentDoa();
        producteurDao = sicadevdRoomDatabase?.producteurDoa();
        parcelleDao = sicadevdRoomDatabase?.parcelleDao();
        localiteDao = sicadevdRoomDatabase?.localiteDoa();
        producteurMenageDao = sicadevdRoomDatabase?.producteurMenageDoa()
        suiviParcelleDao = sicadevdRoomDatabase?.suiviParcelleDao()
        formationDao = sicadevdRoomDatabase?.formationDao()
        livraisonDao = sicadevdRoomDatabase?.livraisonDao()

        agentLogged = agentDao?.getAgent(SPUtils.getInstance().getInt(Constants.AGENT_ID, 3))

        setContentView(R.layout.activity_dashboard_agent)
        bindDatas(agentModel = agentLogged)

        // .setNavigationItemSelectedListener(this)

        imgProfileDashboard.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Deconnexion ?")
            builder.setCancelable(false)

            builder.setPositiveButton("Oui") { dialog, _ ->
                dialog.dismiss()
                this.finish()

                SicadevdRoomDatabase.getDatabase(this)?.agentDoa()
                    ?.logoutAgent(false, SPUtils.getInstance().getInt(Constants.AGENT_ID, 0))
                ActivityUtils.startActivity(SplashActivity::class.java)
            }

            builder.setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        imgBackDashboard.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Voulez-vous quitter ?")
            builder.setCancelable(false)

            builder.setPositiveButton("Oui") { dialog, _ ->
                dialog.dismiss()
                this.finish()
            }

            builder.setNegativeButton("Non") { dialog, _ ->
                dialog.dismiss()
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        /*linearLocalite.setOnClickListener {
            Commons.showMessage("Cette fonctionnalité est désactivé", this, finished = true, callback = {}, positive = "OKAY", deconnec = false, showNo = false)
            return@setOnClickListener;
            //  val intentLocalite = Intent(this, MenusActionRedirectionActivity::class.java)
            //  intentLocalite.putExtra("from", "localite")
            //  ActivityUtils.startActivity(intentLocalite)
        }*/

        imgMenuDashboard.setOnClickListener {
            // drawerDashboard.openDrawer(GravityCompat.START)
        }

        linearSync.setOnClickListener {
            var message = "Mettre à jour la base de données... ?"

            if (!networkFlag) {
                message = "Vous n'etes pas connecté à internet pour effectuer cette action !"
            }

            val builder = AlertDialog.Builder(this)
            builder.setMessage(message)
            builder.setCancelable(false)

            if (networkFlag) {
                builder.setPositiveButton("Oui") { dialog, _ ->
                    dialog.dismiss()
                    this.finish()

                    val intentConfiguration =
                        Intent(this@DashboardAgentActivity, ConfigurationActivity::class.java)
                    intentConfiguration.putExtra(
                        Constants.AGENT_ID,
                        SPUtils.getInstance().getInt(Constants.AGENT_ID, 0)
                    )
                    ActivityUtils.startActivity(intentConfiguration)
                }

                builder.setNegativeButton("Non") { dialog, _ ->
                    dialog.dismiss()
                }
            } else {
                builder.setPositiveButton("OK") { dialog, _ ->
                    dialog.dismiss()
                }
            }

            val dialog: AlertDialog = builder.create()
            dialog.show()
        }

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) !== PackageManager.PERMISSION_GRANTED
            || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ),
                1010
            )
        }

        val menuToken = object : TypeToken<MutableList<String>>() {}.type
        val roles: MutableList<String> =
            GsonUtils.fromJson(SPUtils.getInstance().getString("menu"), menuToken)

        setDataClickListener()
        val listOrderItem = arrayListOf<Int>()
        roles.map {
            when (it.uppercase()) {
                "LOCALITES","LOCALITE" -> {
                    // linearLocalite.visibility = View.VISIBLE
                }
                "PRODUCTEURS","PRODUCTEUR" -> {
                    listOrderItem.add(1)
                    listOrderItem.add(2)
                    //setClickListenForFeature(1);
                    //setClickListenForFeature(2);
                    linealProducteur.visibility = View.VISIBLE
                    linearUniteAgricole.visibility = View.VISIBLE

                }
                "PARCELLE" -> {
                    listOrderItem.add(4)
                    //setClickListenForFeature(4);
                    linealParcel.visibility = View.VISIBLE
                }
                "MENAGES","MENAGE" -> {
                    listOrderItem.add(3)
                    //setClickListenForFeature(3);
                    linealMenage.visibility = View.VISIBLE
                }
                "SUIVIAPPLICATIONS","APPLICATION" -> {
                    listOrderItem.add(10)
                    //setClickListenForFeature(10);
                    linealSuiviApplictions.visibility = View.VISIBLE
                }
                "EVALUATIONS","EVALUATION" -> {
                    listOrderItem.add(6)
                    //setClickListenForFeature(6);
                    linearEvaluation.visibility = View.VISIBLE
                }
                "ESTIMATIONS","ESTIMATION" -> {
                    listOrderItem.add(9)
                    //setClickListenForFeature(9);
                    linearCalculEstimation.visibility = View.VISIBLE
                }
                "SUIVIPARCELLES","PARCELLES" -> {
                    listOrderItem.add(5)
                    //setClickListenForFeature(5);
                    linealSuiviParcelle.visibility = View.VISIBLE
                }
                "SUIVIFORMATION","FORMATION" -> {
                    listOrderItem.add(8)
                    //setClickListenForFeature(8);
                    linearFormation.visibility = View.VISIBLE
                }
                "SSRTECLMRS","SSRTECLMR" -> {
                    listOrderItem.add(7)
                    //setClickListenForFeature(7);
                    linearSSRT.visibility = View.VISIBLE
                }
                "LIVRAISONS","LIVRAISON" -> {
                    listOrderItem.add(11)
                    //setClickListenForFeature(11);
                    linearLivraison.visibility = View.VISIBLE
                }
            }
        }

        if(
            listOrderItem.containsAll(listOf(1,2,3,4))
            || listOrderItem.containsAll(listOf(1,2,3,4,5,6))
            || listOrderItem.size > 6
        ) {
            gridLayoutOfDashboard.columnCount = 2
            gridLayoutOfDashboard.requestLayout()
        }
        //  WorkManager.getInstance(this).enqueue()
        // scheduleAlarm()


        /*for (i in 1..10) {
            FakeLocaliteDatas.saveLocalite(i, this)
        }*/
    }

    private fun setDataClickListener() {
        linealProducteur.let {
            it.setOnClickListener {
                val intentLocalite = Intent(this, MenusActionRedirectionActivity::class.java)
                intentLocalite.putExtra("from", "producteur")
                ActivityUtils.startActivity(intentLocalite)
            }

        }
        linealParcel.let {
            it.setOnClickListener {
                val intentParcelle = Intent(this, MenusActionRedirectionActivity::class.java)
                intentParcelle.putExtra("from", "parcelle")
                ActivityUtils.startActivity(intentParcelle)
            }

        }

        linealSuiviParcelle.let{
            it.setOnClickListener {
                val intentSuiviParcelle = Intent(this, MenusActionRedirectionActivity::class.java)
                intentSuiviParcelle.putExtra("from", "suivi_parcelle")
                ActivityUtils.startActivity(intentSuiviParcelle)
            }

        }

        linealMenage.let {
            it.setOnClickListener {
                val intentMenage = Intent(this, MenusActionRedirectionActivity::class.java)
                intentMenage.putExtra("from", "menage")
                ActivityUtils.startActivity(intentMenage)
            }

        }

        linearEvaluation.let{
            it.setOnClickListener {
                val intentInspection = Intent(this, MenusActionRedirectionActivity::class.java)
                intentInspection.putExtra("from", "inspection")
                ActivityUtils.startActivity(intentInspection)
            }

        }

        // Merci de faire les test de la base de données
        linearUniteAgricole.let{
            it.setOnClickListener {
                val intentInfosProducteur =
                    Intent(this, MenusActionRedirectionActivity::class.java)
                intentInfosProducteur.putExtra("from", "infos_producteur")
                ActivityUtils.startActivity(intentInfosProducteur)
            }

        }
        linearFormation.let{
            it.setOnClickListener {
                val intentFormation = Intent(this, MenusActionRedirectionActivity::class.java)
                intentFormation.putExtra("from", "formation")
                ActivityUtils.startActivity(intentFormation)
            }

        }

        linearLivraison.let{
            it.setOnClickListener {
                val intentLivraison = Intent(this, MenusActionRedirectionActivity::class.java)
                intentLivraison.putExtra("from", "livraison")
                ActivityUtils.startActivity(intentLivraison)
            }

        }
        linearCalculEstimation.let{
            it.setOnClickListener {
                val intentCalculEstimation =
                    Intent(this, MenusActionRedirectionActivity::class.java)
                intentCalculEstimation.putExtra("from", "calcul_estimation")
                ActivityUtils.startActivity(intentCalculEstimation)
            }

        }

        linealSuiviApplictions.let{
            it.setOnClickListener {
                val intentSuiviApplicatio =
                    Intent(this, MenusActionRedirectionActivity::class.java)
                intentSuiviApplicatio.putExtra("from", "suivi_application")
                ActivityUtils.startActivity(intentSuiviApplicatio)
            }

        }
        linearSSRT.let{
            it.setOnClickListener {
                val intentSsrt = Intent(this, MenusActionRedirectionActivity::class.java)
                intentSsrt.putExtra("from", "ssrte")
                ActivityUtils.startActivity(intentSsrt)
            }

        }
    }

    private fun setClickListenForFeature(position: Int) {

        when(position){

        }
    }

    private fun getPlaceToViewInGrid(it: LinearLayout?) {
//        (it?.layoutParams as GridLayout.LayoutParams).columnSpec = GridLayout.spec(
//            GridLayout.UNDEFINED,GridLayout.FILL, 1f
//        )
    }
}
