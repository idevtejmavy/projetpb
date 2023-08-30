package ci.sicadevd.mobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import ci.sicadevd.mobile.R
import ci.sicadevd.mobile.adapters.CultureProducteurAdapter.CultureProducteurHolder
import ci.sicadevd.mobile.interfaces.RecyclerItemListener
import ci.sicadevd.mobile.models.CultureProducteurModel
import kotlinx.android.synthetic.main.culture_items_list.view.*

class CultureProducteurAdapter(private var producteurCultures: List<CultureProducteurModel>?) : RecyclerView.Adapter<CultureProducteurHolder>() {


    lateinit var cultureProducteurListener: RecyclerItemListener<CultureProducteurModel>


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CultureProducteurHolder {
        return CultureProducteurHolder(LayoutInflater.from(parent.context).inflate(R.layout.culture_items_list, parent, false))
    }


    override fun onBindViewHolder(holder: CultureProducteurHolder, position: Int) {
        val cultureProducteurModel = producteurCultures!![position]
        holder.cultureLabel.text = cultureProducteurModel.label
        holder.cultureSuperficie.text = cultureProducteurModel.superficie.toString()

    }


    override fun getItemCount() = producteurCultures?.size ?: 0


    class CultureProducteurHolder(var cultureProducteurView: View) : RecyclerView.ViewHolder(cultureProducteurView) {
        val cultureLabel = cultureProducteurView.labelCultureItem
        val cultureSuperficie = cultureProducteurView.superficieCultureItem
    }
}
