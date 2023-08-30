package ci.sicadevd.mobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ci.sicadevd.mobile.R
import ci.sicadevd.mobile.adapters.FormationAdapter.FormationHolder
import ci.sicadevd.mobile.models.FormationModel
import kotlinx.android.synthetic.main.formation_items_list.view.*


class FormationAdapter(private var formations: List<FormationModel>?) : RecyclerView.Adapter<FormationHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormationHolder {
        return FormationHolder(LayoutInflater.from(parent.context).inflate(R.layout.formation_items_list, parent, false))
    }


    override fun onBindViewHolder(holder: FormationHolder, position: Int) {
        val formationModel = formations!![position]

        holder.formationThemeLabel.text = formationModel.themeStringify
        holder.formationDate.text = formationModel.dateFormation
        holder.formationLieu.text = formationModel.lieuFormationsId

        if (formationModel.isSynced) holder.imgSyncedFormation.setImageResource(R.drawable.ic_sync_donz)
        else holder.imgSyncedFormation.setImageResource(R.drawable.ic_sync_error)
    }



    override fun getItemCount() = formations?.size ?: 0


    class FormationHolder(formationView: View) : RecyclerView.ViewHolder(formationView) {
        val formationThemeLabel = formationView.labelThemeFormations
        val formationDate = formationView.labelDateFormations
        val formationLieu = formationView.labelLieuFormations
        val imgSyncedFormation = formationView.imgSyncedDoneFormations
    }
}
