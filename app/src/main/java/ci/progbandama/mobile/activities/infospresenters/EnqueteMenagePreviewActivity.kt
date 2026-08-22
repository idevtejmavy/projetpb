package ci.progbandama.mobile.activities.infospresenters

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import ci.progbandama.mobile.R
import ci.progbandama.mobile.activities.forms.EnqueteMenageActivity
import ci.progbandama.mobile.adapters.PreviewItemAdapter
import ci.progbandama.mobile.databinding.ActivityEnqueteMenagePreviewBinding
import ci.progbandama.mobile.models.EnfantMenageModel
import ci.progbandama.mobile.models.EnqueteMenageModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.Commons
import ci.progbandama.mobile.tools.RecapEnqueteMenage
import com.blankj.utilcode.util.ActivityUtils
import com.blankj.utilcode.util.LogUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.reflect.TypeToken

/**
 * Recapitulatif d'une Enquete Menage avant enregistrement - lot 3, ligne 3.6.
 *
 * Dernier point ou l'agent peut encore corriger : le serveur n'expose aucun endpoint de relecture
 * pour ce module, une enquete transmise n'est plus modifiable depuis le telephone. L'ecran montre
 * donc ce qui sera reellement envoye, reconstitue depuis le modele et non depuis les vues.
 *
 * C'est aussi ici que l'enquete est ecrite en base, et le brouillon clos - dans cet ordre : le
 * clore avant perdrait la saisie si l'enregistrement echouait.
 */
class EnqueteMenagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnqueteMenagePreviewBinding
    private var enquete: EnqueteMenageModel? = null
    private var uidBrouillon = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnqueteMenagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.clickCloseBtn.setOnClickListener { finish() }

        enquete = intent?.getParcelableExtra(EXTRA_ENQUETE)
        uidBrouillon = intent?.getIntExtra(EXTRA_BROUILLON, 0) ?: 0

        val aRelire = enquete
        if (aRelire == null) {
            LogUtils.e("Recapitulatif ouvert sans enquete : retour au formulaire")
            finish()
            return
        }

        afficher(aRelire)
        binding.clickSaveEnquetePreview.setOnClickListener { confirmer(aRelire) }
    }

    private fun afficher(enquete: EnqueteMenageModel) {
        val enfants: List<EnfantMenageModel> = BrouillonPfte.deserialiserListe(
            enquete.enfantsStringify, object : TypeToken<MutableList<EnfantMenageModel>>() {}
        )

        val lignes = mutableListOf<Map<String, String>>()
        RecapEnqueteMenage.lignes(enquete, enfants).forEach { ligne ->
            // Le numero prefixe le libelle : sans lui, soixante lignes de fiches enfant se
            // ressemblent toutes et l'agent ne sait plus de quel enfant il relit la situation.
            val prefixe = ligne.enfant?.let { getString(R.string.enfant_n) + "$it · " }.orEmpty()
            Commons.addItemsToList(prefixe + getString(ligne.libelle), ligne.valeur, lignes)
        }

        binding.recyclerInfoPrev.adapter = PreviewItemAdapter(lignes)
        binding.recyclerInfoPrev.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun confirmer(enquete: EnqueteMenageModel) {
        Commons.showMessage(
            message = getString(R.string.confirmer_enregistrement_enquete),
            context = this,
            finished = false,
            callback = {
                try {
                    ProgBandRoomDatabase.getDatabase(this)?.enqueteMenageDao()?.insert(enquete)
                    // Le brouillon n'est clos qu'une fois l'enquete en base : l'inverse la
                    // perdrait si l'enregistrement echouait.
                    BrouillonPfte.cloturer(this, uidBrouillon)

                    Commons.showMessage(
                        message = getString(R.string.enquete_enregistree),
                        context = this,
                        finished = true,
                        callback = {
                            // Le formulaire est ferme derriere nous : y revenir rouvrirait une
                            // saisie deja enregistree, et l'agent la transmettrait deux fois.
                            ActivityUtils.finishActivity(EnqueteMenageActivity::class.java)
                        },
                        positive = getString(R.string.ok)
                    )
                } catch (ex: Exception) {
                    LogUtils.e("Enregistrement de l'enquete menage : ${ex.message}")
                    FirebaseCrashlytics.getInstance().recordException(ex)
                    Commons.showMessage(
                        message = getString(R.string.enquete_non_enregistree),
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
        const val EXTRA_ENQUETE = "enquete_menage"
        const val EXTRA_BROUILLON = "brouillon_uid"
    }
}
