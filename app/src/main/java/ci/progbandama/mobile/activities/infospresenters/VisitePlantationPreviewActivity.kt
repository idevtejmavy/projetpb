package ci.progbandama.mobile.activities.infospresenters

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ci.progbandama.mobile.R
import ci.progbandama.mobile.activities.forms.VisitePlantationActivity
import ci.progbandama.mobile.adapters.PreviewItemAdapter
import ci.progbandama.mobile.databinding.ActivityRecapPfteBinding
import ci.progbandama.mobile.models.EnfantPlantationModel
import ci.progbandama.mobile.models.VisitePlantationModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.Commons
import ci.progbandama.mobile.tools.RecapVisitePlantation
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken

/**
 * Recapitulatif d'une Visite Plantation avant enregistrement - lot 4.
 *
 * Meme role que celui de l'Enquete Menage, et meme mise en page : dernier point ou l'agent peut
 * corriger, le serveur n'exposant aucun endpoint de relecture pour ce module.
 *
 * La visite est ecrite en base depuis cet ecran, et le brouillon clos ensuite - le clore avant
 * perdrait la saisie si l'enregistrement echouait.
 */
class VisitePlantationPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecapPfteBinding
    private var uidBrouillon = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecapPfteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // La mise en page est partagee par les modules PFTE : le titre est pose ici.
        binding.labelTitleMenuAction.setText(R.string.recapitulatif_visite_plantation)
        binding.clickSaveEnquetePreview.setText(R.string.enregistrer_la_visite)
        binding.clickCloseBtn.setOnClickListener { finish() }

        uidBrouillon = intent?.getIntExtra(EXTRA_BROUILLON, 0) ?: 0
        val visite: VisitePlantationModel? = intent?.getParcelableExtra(EXTRA_VISITE)

        if (visite == null) {
            LogUtils.e("Recapitulatif ouvert sans visite : retour au formulaire")
            finish()
            return
        }

        afficher(visite)
        binding.clickSaveEnquetePreview.setOnClickListener { confirmer(visite) }
    }

    private fun afficher(visite: VisitePlantationModel) {
        val enfants: List<EnfantPlantationModel> = BrouillonPfte.deserialiserListe(
            visite.enfantsStringify, object : TypeToken<MutableList<EnfantPlantationModel>>() {}
        )

        val lignes = mutableListOf<Map<String, String>>()
        RecapVisitePlantation.lignes(visite, enfants).forEach { ligne ->
            // Le numero prefixe le libelle : sans lui, les lignes de fiches se ressemblent toutes.
            val prefixe = ligne.enfant?.let { getString(R.string.enfant_n) + "$it · " }.orEmpty()
            Commons.addItemsToList(prefixe + getString(ligne.libelle), ligne.valeur, lignes)
        }

        binding.recyclerInfoPrev.adapter = PreviewItemAdapter(lignes)
        binding.recyclerInfoPrev.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun confirmer(visite: VisitePlantationModel) {
        Commons.showMessage(
            message = getString(R.string.confirmer_enregistrement_visite),
            context = this,
            finished = false,
            callback = {
                try {
                    ProgBandRoomDatabase.getDatabase(this)?.visitePlantationDao()?.insert(visite)
                    // Le brouillon n'est clos qu'une fois la visite en base : l'inverse la
                    // perdrait si l'enregistrement echouait.
                    BrouillonPfte.cloturer(this, uidBrouillon)

                    Commons.showMessage(
                        message = getString(R.string.visite_enregistree),
                        context = this,
                        finished = true,
                        callback = {
                            // Le formulaire est ferme derriere nous : y revenir rouvrirait une
                            // saisie deja enregistree, que l'agent transmettrait deux fois.
                            ActivityUtils.finishActivity(VisitePlantationActivity::class.java)
                        },
                        positive = getString(R.string.ok)
                    )
                } catch (ex: Exception) {
                    LogUtils.e("Enregistrement de la visite plantation : ${ex.message}")
                    FirebaseCrashlytics.getInstance().recordException(ex)
                    Commons.showMessage(
                        message = getString(R.string.visite_non_enregistree),
                        context = this, finished = false, callback = { },
                        positive = getString(R.string.ok)
                    )
                }
            },
            positive = getString(R.string.oui),
            deconnec = false,
            showNo = true
        )
    }

    companion object {
        const val EXTRA_VISITE = "visite_plantation"
        const val EXTRA_BROUILLON = "brouillon_uid"
    }
}
