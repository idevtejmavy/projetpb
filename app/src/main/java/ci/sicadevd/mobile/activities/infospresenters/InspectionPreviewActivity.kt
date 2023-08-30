package ci.sicadevd.mobile.activities.infospresenters


import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import ci.sicadevd.mobile.R
import ci.sicadevd.mobile.activities.forms.InspectionActivity
import ci.sicadevd.mobile.adapters.QuestionnairePreviewAdapter
import ci.sicadevd.mobile.interfaces.SectionCallback
import ci.sicadevd.mobile.itemviews.RecyclerItemDecoration
import ci.sicadevd.mobile.models.InspectionDTO
import ci.sicadevd.mobile.models.QuestionResponseModel
import ci.sicadevd.mobile.repositories.databases.SicadevdRoomDatabase
import ci.sicadevd.mobile.tools.Commons
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.activity_inspection_preview.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.lang.Exception


class InspectionPreviewActivity : AppCompatActivity(), SectionCallback {


    private var cQuestionnairesMap: MutableList<HashMap<String, String>>? = mutableListOf()
    private var cQuestionnaires: MutableList<QuestionResponseModel>? = mutableListOf()
    lateinit var questionnaireAdapter: QuestionnairePreviewAdapter


    suspend fun fetchQuestionnaires(inspectionDTO: InspectionDTO?) {
        MainScope().launch {
            try {
                val mQuestionsPreviewToken =
                    object : TypeToken<MutableList<QuestionResponseModel>>() {}.type
                cQuestionnaires =
                    GsonUtils.fromJson(inspectionDTO?.reponseStringify, mQuestionsPreviewToken)

                questionnaireAdapter =
                    QuestionnairePreviewAdapter(this@InspectionPreviewActivity, cQuestionnaires!!)
                recyclerQuestionnairesInspectionPreview.layoutManager = LinearLayoutManager(
                    this@InspectionPreviewActivity,
                    LinearLayoutManager.VERTICAL,
                    false
                )
                recyclerQuestionnairesInspectionPreview.adapter = questionnaireAdapter

                val recyclerDecoration = RecyclerItemDecoration(
                    this@InspectionPreviewActivity,
                    40,
                    true,
                    this@InspectionPreviewActivity
                )
                recyclerQuestionnairesInspectionPreview.addItemDecoration(recyclerDecoration)
            } catch (ex: Exception) {
                LogUtils.e(ex.message)
                FirebaseCrashlytics.getInstance().recordException(ex)
            }
        }
    }


    override fun isSectionHeader(position: Int): Boolean {
        return position == 0 || cQuestionnaires!![position].isTitle!!
    }


    override fun getSectionHeaderName(postion: Int): String {
        try {
            val mQuestionMapped = hashMapOf<String, String>()
            return mQuestionMapped["Title"] ?: "Size"
        } catch (ex: Exception) {
            LogUtils.e(ex.message)
                FirebaseCrashlytics.getInstance().recordException(ex)
        }

        return ""
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inspection_preview)

        intent?.let { intent ->
            try {
                val inspectionDTO: InspectionDTO? = intent.getParcelableExtra("preview")

                inspectionDTO?.let { inspection ->
                    labelProducteurNomInspectionPreview.text = inspection.producteurNomPrenoms
                    labelCampagneInspectionPreview.text = inspection.campagnesLabel

                    CoroutineScope(Dispatchers.Main).launch {
                        fetchQuestionnaires(inspection)
                    }

                    labelDateInspectionPreview.text = inspection.dateEvaluation

                    clickSaveInspectionPreview.setOnClickListener {
                        try {
                            Commons.showMessage(
                                "Etes-vous sûr de vouloir enregistrer ce contenu ?",
                                this,
                                showNo = true,
                                callback = {
                                    SicadevdRoomDatabase.getDatabase(this)?.inspectionDao()
                                        ?.insert(inspection)
                                    Commons.synchronisation(type = "inspection", this)
                                    Commons.showMessage(
                                        "Inpection enregistrée avec succes !",
                                        this,
                                        finished = true,
                                        callback = {})
                                },
                                finished = false
                            )

                            ActivityUtils.finishActivity(InspectionActivity::class.java)
                        } catch (ex: Exception) {
                            Commons.showMessage("Echec enregistreent !", this, callback = {})
                        }
                    }
                }
            } catch (ex: Exception) {
                LogUtils.e(ex.message)
                FirebaseCrashlytics.getInstance().recordException(ex)
            }
        }

        clickCloseBtn.setOnClickListener {
            finish()
        }
    }
}
