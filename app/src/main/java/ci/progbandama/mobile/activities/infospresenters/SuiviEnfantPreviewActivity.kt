package ci.progbandama.mobile.activities.infospresenters

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ci.progbandama.mobile.R
import ci.progbandama.mobile.activities.forms.SuiviEnfantActivity
import ci.progbandama.mobile.adapters.PreviewItemAdapter
import ci.progbandama.mobile.databinding.ActivityRecapPfteBinding
import ci.progbandama.mobile.models.SuiviEnfantModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.Commons
import ci.progbandama.mobile.tools.RecapSuiviEnfant
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Recapitulatif d'un Suivi Enfant Travailleur avant enregistrement - lot 5.
 *
 * Meme role et meme mise en page que pour les deux autres modules : dernier point ou l'agent peut
 * corriger, le serveur n'exposant aucun endpoint de relecture.
 *
 * Le suivi est ecrit en base depuis cet ecran, et le brouillon clos ensuite - le clore avant
 * perdrait la saisie si l'enregistrement echouait.
 */
class SuiviEnfantPreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecapPfteBinding
    private var uidBrouillon = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecapPfteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // La mise en page est partagee par les modules PFTE : le titre est pose ici.
        binding.labelTitleMenuAction.setText(R.string.recapitulatif_suivi_enfant)
        binding.clickSaveEnquetePreview.setText(R.string.enregistrer_le_suivi)
        binding.clickCloseBtn.setOnClickListener { finish() }

        uidBrouillon = intent?.getIntExtra(EXTRA_BROUILLON, 0) ?: 0
        val suivi: SuiviEnfantModel? = intent?.getParcelableExtra(EXTRA_SUIVI)

        if (suivi == null) {
            LogUtils.e("Recapitulatif ouvert sans suivi : retour au formulaire")
            finish()
            return
        }

        afficher(suivi)
        binding.clickSaveEnquetePreview.setOnClickListener { confirmer(suivi) }
    }

    private fun afficher(suivi: SuiviEnfantModel) {
        val lignes = mutableListOf<Map<String, String>>()
        RecapSuiviEnfant.lignes(suivi).forEach { ligne ->
            Commons.addItemsToList(getString(ligne.libelle), ligne.valeur, lignes)
        }

        binding.recyclerInfoPrev.adapter = PreviewItemAdapter(lignes)
        binding.recyclerInfoPrev.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun confirmer(suivi: SuiviEnfantModel) {
        Commons.showMessage(
            message = getString(R.string.confirmer_enregistrement_suivi),
            context = this,
            finished = false,
            callback = {
                try {
                    ProgBandRoomDatabase.getDatabase(this)?.suiviEnfantDao()?.insert(suivi)
                    // Le brouillon n'est clos qu'une fois le suivi en base : l'inverse le
                    // perdrait si l'enregistrement echouait.
                    BrouillonPfte.cloturer(this, uidBrouillon)

                    Commons.showMessage(
                        message = getString(R.string.suivi_enregistre),
                        context = this,
                        finished = true,
                        callback = {
                            // Le formulaire est ferme derriere nous : y revenir rouvrirait une
                            // saisie deja enregistree, que l'agent transmettrait deux fois.
                            ActivityUtils.finishActivity(SuiviEnfantActivity::class.java)
                        },
                        positive = getString(R.string.ok)
                    )
                } catch (ex: Exception) {
                    LogUtils.e("Enregistrement du suivi enfant : ${ex.message}")
                    FirebaseCrashlytics.getInstance().recordException(ex)
                    Commons.showMessage(
                        message = getString(R.string.suivi_non_enregistre),
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
        const val EXTRA_SUIVI = "suivi_enfant"
        const val EXTRA_BROUILLON = "brouillon_uid"
    }
}
