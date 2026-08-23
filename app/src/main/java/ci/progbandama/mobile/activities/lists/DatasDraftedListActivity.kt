package ci.progbandama.mobile.activities.lists

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import ci.progbandama.mobile.R
import ci.progbandama.mobile.adapters.DataDraftedAdapter
import ci.progbandama.mobile.databinding.ActivityDatasDraftedListBinding
import ci.progbandama.mobile.models.DataDraftedModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.Constants
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils


@SuppressLint("All")
class DatasDraftedListActivity : AppCompatActivity(R.layout.activity_datas_drafted_list) {


    var draftedDatasList: MutableList<DataDraftedModel>? = mutableListOf()
    var fromGlobalMenu = ""

    private lateinit var binding: ActivityDatasDraftedListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDatasDraftedListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        intent?.let {
            fromGlobalMenu = it.getStringExtra("fromMenu").toString()

            val replaceID = fromGlobalMenu
                .replace("parcelles", "suivi_parcelle")
                .replace("formation_visiteur", "visiteur_formation")
                .replace("livraison_magcentral", "suivi_livraison_central")
                .replace("ssrteclmrs", "ssrte")
                // Le menu menage couvre deux types de brouillon : celui de l'ancien formulaire
                // et celui de l'Enquete Menage PFTE. La requete filtre par LIKE, un motif les
                // rassemble donc tous les deux, et chaque brouillon se rouvre dans le formulaire
                // qui l'a produit. Aucun autre type ne contient "menage".
                .replace("menage", "%menage%")
                .lowercase()
            LogUtils.d(fromGlobalMenu, replaceID)
            val draftedDataList = ProgBandRoomDatabase.getDatabase(this)?.draftedDatasDao()?.getAllByType(
                    SPUtils.getInstance().getInt(Constants.AGENT_ID).toString(),
                    replaceID
                )

            LogUtils.d(draftedDataList)

            val draftedDatasAdapter = DataDraftedAdapter(
                this,
                draftedDataList
            )

            binding.recyclerDraftedList.adapter = draftedDatasAdapter
            binding.recyclerDraftedList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

            draftedDatasAdapter.notifyDataSetChanged()

            draftedDataList?.let { draftsList ->
                if (draftsList.isEmpty()) {
                    binding.recyclerDraftedList.visibility = View.GONE
                    binding.linearEmptyContainerDraftsList.visibility = View.VISIBLE
                } else {
                    binding.recyclerDraftedList.visibility = View.VISIBLE
                    binding.linearEmptyContainerDraftsList.visibility = View.GONE
                }
            }
        }

        binding.clickCloseBtn.setOnClickListener {
            finish()
        }
    }
}
