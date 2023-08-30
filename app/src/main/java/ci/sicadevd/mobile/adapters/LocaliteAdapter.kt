package ci.sicadevd.mobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ci.sicadevd.mobile.R
import ci.sicadevd.mobile.adapters.LocaliteAdapter.LocaliteHolder
import ci.sicadevd.mobile.interfaces.RecyclerItemListener
import ci.sicadevd.mobile.models.LocaliteModel
import kotlinx.android.synthetic.main.localite_items_list.view.*
import kotlinx.android.synthetic.main.localite_items_list.view.imgSynced

class LocaliteAdapter(private var localites: List<LocaliteModel>?) : RecyclerView.Adapter<LocaliteHolder>() {


    lateinit var localiteListener: RecyclerItemListener<LocaliteModel>


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocaliteHolder {
        return LocaliteHolder(LayoutInflater.from(parent.context).inflate(R.layout.localite_items_list, parent, false))
    }


    override fun onBindViewHolder(holder: LocaliteHolder, position: Int) {
        val localiteModel = localites!![position]

        holder.localiteNomLabel.text = localiteModel.nom
        holder.localiteTypeLabel.text = localiteModel.type

        if (localiteModel.isSynced) holder.imgSyncedStatus.setImageResource(R.drawable.ic_sync_donz)
        else holder.imgSyncedStatus.setImageResource(R.drawable.ic_sync_error)
    }


    override fun getItemCount() = localites?.size ?: 0


    class LocaliteHolder(localiteView: View) : RecyclerView.ViewHolder(localiteView) {
        val localiteNomLabel = localiteView.labelLocaliteNom
        val localiteTypeLabel = localiteView.labelLocaliteType
        val imgSyncedStatus = localiteView.imgSynced
    }
}