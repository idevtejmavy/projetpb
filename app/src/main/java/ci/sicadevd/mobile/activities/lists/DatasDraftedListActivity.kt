package ci.sicadevd.mobile.activities.lists

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import ci.sicadevd.mobile.R
import ci.sicadevd.mobile.adapters.DataDraftedAdapter
import ci.sicadevd.mobile.models.DataDraftedModel
import ci.sicadevd.mobile.repositories.databases.SicadevdRoomDatabase
import ci.sicadevd.mobile.tools.Constants
import com.blankj.utilcode.util.SPUtils
import kotlinx.android.synthetic.main.activity_datas_drafted_list.*


@SuppressLint("All")
class DatasDraftedListActivity : AppCompatActivity(R.layout.activity_datas_drafted_list) {


    var draftedDatasList: MutableList<DataDraftedModel>? = mutableListOf()
    var fromGlobalMenu = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.let {
            fromGlobalMenu = it.getStringExtra("fromMenu").toString()

            val draftedDataList = SicadevdRoomDatabase.getDatabase(this)?.draftedDatasDao()?.getAllByType(
                    SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                    fromGlobalMenu
                )

            val draftedDatasAdapter = DataDraftedAdapter(
                this,
                draftedDataList
            )

            recyclerDraftedList.adapter = draftedDatasAdapter
            recyclerDraftedList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            draftedDatasAdapter.notifyDataSetChanged()

            draftedDataList?.let { draftsList ->
                if (draftsList.isEmpty()) {
                    recyclerDraftedList.visibility = View.GONE
                    linearEmptyContainerDraftsList.visibility = View.VISIBLE
                } else {
                    recyclerDraftedList.visibility = View.VISIBLE
                    linearEmptyContainerDraftsList.visibility = View.GONE
                }
            }
        }

        clickCloseBtn.setOnClickListener {
            finish()
        }
    }
}
