package ci.progbandama.mobile.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ci.progbandama.mobile.R
import ci.progbandama.mobile.activities.forms.*
import ci.progbandama.mobile.activities.lists.DatasDraftedListActivity
import ci.progbandama.mobile.databinding.MenagereItemsListBinding
import ci.progbandama.mobile.repositories.datas.CommonData
import ci.progbandama.mobile.tools.Commons
import ci.progbandama.mobile.tools.Constants
import ci.progbandama.mobile.tools.RepriseEnAttente
import android.app.Activity
import android.content.Intent
import android.widget.Toast
import com.blankj.utilcode.util.SPUtils


/**
 * Created by Didier BOKA, email: didierboka.developer@gmail.com
 * on 18/04/2022.
 **/

@SuppressLint("All")
class DataSendingAdapter(val context: Context, var draftedList: MutableList<CommonData>?): RecyclerView.Adapter<DataSendingAdapter.DataSyncedHolder>() {


    class DataSyncedHolder(viewDataDrafted: MenagereItemsListBinding) : RecyclerView.ViewHolder(viewDataDrafted.root) {

        val item_title = viewDataDrafted.itemTitle
        val labelQuartierMenagere = viewDataDrafted.labelQuartierMenagere
        val item2_title = viewDataDrafted.item2Title
        val labelProducteurNomMenagere = viewDataDrafted.labelProducteurNomMenagere
        val imgSyncedStatus = viewDataDrafted.imgSyncedMenage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataSyncedHolder {
        return DataSyncedHolder(
            MenagereItemsListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//            LayoutInflater.from(context).inflate(R.layout.menagere_items_list, parent, false)
        )
    }


    override fun onBindViewHolder(holder: DataSyncedHolder, position: Int) {
        val draftedData = draftedList!![position]

        if(draftedData == null) return

        holder.item_title.text = draftedData.listOfValue?.get(0).toString()
        holder.labelQuartierMenagere.text = draftedData.listOfValue?.get(1).toString()
        if(draftedData.listOfValue?.size!! > 2){
            holder.item2_title.text = draftedData.listOfValue?.get(2).toString()
            holder.labelProducteurNomMenagere.text = draftedData.listOfValue?.get(3).toString()
        }

        if (draftedData.listOfValue?.size!! >= 5) {
            if( draftedData.listOfValue?.get(4).isNullOrEmpty() == false ) holder.imgSyncedStatus.setImageResource(R.drawable.ic_sync_donz)
        }
        else holder.imgSyncedStatus.setImageResource(R.drawable.ic_sync_error)

        // Correction d'une donnee en attente d'envoi : elle est ramenee vers les brouillons,
        // ou le circuit de modification existe deja pour tous les modules.
        holder.itemView.setOnClickListener {
            val module = draftedData.value?.uppercase()
            if (!RepriseEnAttente.estSupporte(module)) {
                Toast.makeText(context, "Cette donnée ne peut pas être reprise", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Commons.showMessage(
                message = "Reprendre cette saisie pour la corriger ?" +
                        "\n\n" +
                        "Elle quitte la file d'envoi et repart dans vos brouillons. " +
                        "Elle ne sera transmise qu'après un nouvel enregistrement.",
                context = context,
                finished = false,
                showNo = true,
                positive = "Reprendre",
                callback = {
                    val ok = RepriseEnAttente.reprendre(
                        context,
                        module!!,
                        draftedData.id ?: 0,
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
//draftedData.value?.uppercase()

//        modifyIcColor(context, holder.imageTypeDraft, R.color.black)

    }

    override fun getItemCount() = draftedList?.size ?: 0

}
