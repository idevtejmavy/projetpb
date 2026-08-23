package ci.progbandama.mobile.activities.forms

import android.app.DatePickerDialog
import android.content.Intent
import android.provider.Settings
import android.location.LocationManager
import android.content.pm.PackageManager
import android.content.Context
import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ci.progbandama.mobile.R
import ci.progbandama.mobile.activities.forms.views.MultiSelectSpinner
import ci.progbandama.mobile.activities.infospresenters.EnqueteMenagePreviewActivity
import ci.progbandama.mobile.databinding.ActivityEnqueteMenageBinding
import ci.progbandama.mobile.models.DataDraftedModel
import ci.progbandama.mobile.models.EnfantMenageModel
import ci.progbandama.mobile.models.EnqueteMenageModel
import ci.progbandama.mobile.models.ProducteurModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.repositories.datas.CommonData
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.Commons
import ci.progbandama.mobile.tools.Constants
import ci.progbandama.mobile.tools.ControlesEnqueteMenage
import ci.progbandama.mobile.tools.FichesEnfant
import ci.progbandama.mobile.tools.ReferentielsPfte
import ci.progbandama.mobile.tools.SaisiePfte
import ci.progbandama.mobile.tools.VocabulairesPfte
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.google.gson.reflect.TypeToken
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.format
import id.zelory.compressor.constraint.quality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.util.Calendar

/**
 * Enquete Menage (PFTE) - lot 3.
 *
 * Le formulaire le plus dense de l'application : une cinquantaine de champs a l'entete, autant de
 * fiches enfant que le menage en compte, et une vingtaine de regles d'affichage. Trois d'entre
 * elles arretent l'enquete avant la fin - producteur indisponible, refus, non consentement - et
 * l'enquete doit alors partir quand meme, avec son motif : c'est cette information que le
 * back-office attend pour calculer son statut de fin.
 *
 * Deux regles gouvernent tout le reste :
 *
 *  - masquer un bloc ne suffit pas, il faut aussi le vider et le desactiver. C'est ce que fait le
 *    toggle() du back-office, et c'est ce que fait SaisiePfte.basculer(). Sans cela une valeur
 *    rendue sans objet par un autre choix partirait quand meme au serveur ;
 *  - les fiches enfant sont lues et ecrites explicitement. Le binding par reflexion ne descend pas
 *    dans les sous-formulaires, et le composant de choix multiple ne restitue pas sa selection :
 *    chaque fiche porte donc son propre etat, attache par setTag(R.id.tagSelectionsFiche, ...).
 */
class EnqueteMenageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnqueteMenageBinding
    private lateinit var fichesEnfant: FichesEnfant<EnfantMenageModel>

    private val sectionRetenue = CommonData()
    private val localiteRetenue = CommonData()
    private val producteurRetenu = CommonData()

    private var producteursListe: MutableList<ProducteurModel> = mutableListOf()
    private var brouillonOuvert: DataDraftedModel? = null

    private var sexeProducteur = ""
    private var codeProducteur = ""
    private var photoBase64 = ""
    private var fichierPhoto: File? = null

    // Selections multiples de l'entete. Le composant ne restitue pas sa selection : on la retient.
    private val raisonsRefusRetenues = mutableListOf<String>()
    private val themesRetenus = mutableListOf<String>()
    private val outilsRetenus = mutableListOf<String>()

    /** Etat des choix multiples d'une fiche enfant, attache a la vue de la fiche. */
    private class SelectionsFiche {
        val raisonNonScolarisation = mutableListOf<String>()
        val raisonPasExtrait = mutableListOf<String>()
        val situationsPfte = mutableListOf<String>()
        val raisonTravailAbus = mutableListOf<String>()
        val mesuresEnfant = mutableListOf<String>()
        val mesuresMenage = mutableListOf<String>()
        val mesuresCommunaute = mutableListOf<String>()
    }

    private fun selections(vue: View): SelectionsFiche =
        vue.getTag(R.id.tagSelectionsFiche) as SelectionsFiche

    private val agentId: Int get() = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0)

    // ------------------------------------------------------------------ cycle de vie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnqueteMenageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!referentielsPrets()) return

        preparerFichesEnfant()
        preparerIdentification()
        preparerFiltre()
        preparerMenage()
        preparerSensibilisation()
        preparerBoutons()

        binding.editDateEnqueteMenage.setText(
            DateTime.now().toString(DateTimeFormat.forPattern("dd-MM-yyyy"))
        )
        binding.editNomEnqueteurMenage.setText(SPUtils.getInstance().getString(Constants.AGENT_NAME, ""))

        rouvrirBrouillonEventuel()
    }

    /**
     * Refuse d'ouvrir le formulaire si les listes de reference manquent.
     *
     * Sans elles, la moitie des listes deroulantes serait vide et l'agent remplirait une enquete
     * que le serveur refuserait. Mieux vaut le dire tout de suite.
     */
    private fun referentielsPrets(): Boolean {
        val manquants = SaisiePfte.referentielsManquants(this)
        if (manquants.isEmpty()) return true

        LogUtils.e("Enquete Menage : referentiels manquants -> $manquants")
        Commons.showMessage(
            message = getString(R.string.referentiels_pfte_manquants),
            context = this,
            finished = true,
            callback = { finish() },
            positive = getString(R.string.ok)
        )
        return false
    }

    // ------------------------------------------------------------------ identification

    private fun preparerIdentification() {
        choisirSection()

        binding.editDateEnqueteMenage.setOnClickListener { choisirDate(binding.editDateEnqueteMenage) }
        binding.btnCapturerPositionMenage.setOnClickListener { capturerPosition() }
    }

    private fun choisirSection(sectionCourante: String? = null, localiteCourante: String? = null,
                               producteurCourant: String? = null) {
        val sections = ProgBandRoomDatabase.getDatabase(applicationContext)?.sectionsDao()
            ?.getAll(agentID = agentId.toString()) ?: mutableListOf()

        val libelleCourant = sections.firstOrNull { it.id.toString() == sectionCourante }?.libelle

        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.choix_de_la_section),
            message = getString(R.string.la_liste_des_sections_semble_vide_veuillez_proc_der_la_synchronisation_des_donn_es_svp),
            isEmpty = sections.isEmpty(),
            currentVal = libelleCourant,
            spinner = binding.selectSectionMenage,
            listIem = sections.map { it.libelle },
            onChanged = { position ->
                val section = sections[position]
                sectionRetenue.nom = section.libelle.orEmpty()
                sectionRetenue.id = section.id ?: 0
                choisirLocalite(sectionRetenue.id ?: 0, localiteCourante, producteurCourant)
            },
            onSelected = { _, _ -> }
        )
    }

    private fun choisirLocalite(sectionId: Int, localiteCourante: String? = null,
                                producteurCourant: String? = null) {
        val localites = ProgBandRoomDatabase.getDatabase(applicationContext)?.localiteDoa()
            ?.getLocaliteBySection(sectionId) ?: mutableListOf()

        val libelleCourant = localites.firstOrNull { it.id.toString() == localiteCourante }?.nom

        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.choix_de_la_localit),
            message = getString(R.string.la_liste_des_localit_s_semble_vide_veuillez_proc_der_la_synchronisation_des_donn_es_svp),
            isEmpty = localites.isEmpty(),
            currentVal = libelleCourant,
            spinner = binding.selectLocaliteMenage,
            listIem = localites.map { it.nom },
            onChanged = { position ->
                val localite = localites[position]
                localiteRetenue.nom = localite.nom.orEmpty()
                localiteRetenue.id = localite.id ?: 0
                choisirProducteur(localiteRetenue.id ?: 0, producteurCourant)
            },
            onSelected = { _, _ -> }
        )
    }

    private fun choisirProducteur(localiteId: Int, producteurCourant: String? = null) {
        producteursListe = ProgBandRoomDatabase.getDatabase(applicationContext)?.producteurDoa()
            ?.getProducteursByLocalite(localite = localiteId.toString()) ?: mutableListOf()

        fun libelle(p: ProducteurModel) = "${p.nom.orEmpty()} ${p.prenoms.orEmpty()}".trim()

        // Un producteur saisi hors ligne n'a pas encore d'identifiant serveur : c'est son uid
        // local qui l'identifie, comme partout ailleurs dans l'application.
        val libelleCourant = producteursListe.firstOrNull {
            val cle = if (it.id == 0) it.uid.toString() else it.id.toString()
            cle == producteurCourant
        }?.let { libelle(it) }

        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.choix_du_producteur),
            message = getString(R.string.la_liste_des_producteurs_semble_vide_veuillez_proc_der_la_synchronisation_des_donn_es_svp),
            isEmpty = producteursListe.isEmpty(),
            currentVal = libelleCourant,
            spinner = binding.selectProducteurMenage,
            listIem = producteursListe.map { libelle(it) },
            onChanged = { position ->
                val producteur = producteursListe[position]
                producteurRetenu.nom = libelle(producteur)
                producteurRetenu.id =
                    if (producteur.isSynced == true) producteur.id ?: 0 else producteur.uid

                // Calcul C4 : le sexe et le code viennent du producteur, l'agent ne les saisit pas.
                sexeProducteur = producteur.sexeProducteur.orEmpty()
                codeProducteur = producteur.codeProdApp.orEmpty().ifBlank { producteur.codeProd.orEmpty() }
                binding.editSexeProducteurMenage.setText(sexeProducteur)
                binding.editCodeProducteurMenage.setText(codeProducteur)

                // Regle R1 : si le producteur est le repondant, son nom est repris tel quel.
                appliquerRepondant()
            },
            onSelected = { _, _ -> }
        )
    }

    /**
     * Releve la position de l'enquete.
     *
     * Trois situations distinctes, et une seule appelle a patienter :
     *
     *  - l'autorisation de localisation n'a pas ete accordee : on la redemande ;
     *  - le service de localisation de l'appareil est eteint : on propose de l'activer et on
     *    conduit l'agent a l'ecran de reglages, en reprenant la capture a son retour ;
     *  - le service tourne mais aucune position n'est encore fixee : la seule reponse est
     *    d'attendre quelques instants a l'exterieur.
     *
     * Les confondre en un unique message d'indisponibilite laissait l'agent sans rien a faire
     * dans les deux premiers cas, qui sont les plus frequents.
     */
    private fun capturerPosition() {
        if (!autorisationLocalisationAccordee()) {
            demanderAutorisationLocalisation()
            return
        }
        if (!localisationActivee()) {
            proposerActivationLocalisation()
            return
        }

        val prefs = SPUtils.getInstance()
        val latitude = prefs.getString(Constants.PREFS_COMMON_LAT, "")
        val longitude = prefs.getString(Constants.PREFS_COMMON_LNG, "")

        // "0.0" est ce que le service ecrit tant qu'il n'a rien capte : ce n'est pas une position.
        if (latitude.isNullOrBlank() || longitude.isNullOrBlank() ||
            latitude == "0.0" || longitude == "0.0") {
            Commons.showMessage(
                message = getString(R.string.position_gps_pas_encore_fixee),
                context = this, finished = false, callback = { },
                positive = getString(R.string.ok)
            )
            return
        }

        binding.editLatitudeMenage.setText(latitude)
        binding.editLongitudeMenage.setText(longitude)
        binding.editAltitudeMenage.setText(prefs.getString(Constants.PREFS_COMMON_ALT, ""))
        binding.editPrecisionGpsMenage.setText(prefs.getString(Constants.PREFS_COMMON_PRECISION, ""))
    }

    private fun autorisationLocalisationAccordee(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    private fun demanderAutorisationLocalisation() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            DEMANDE_AUTORISATION_POSITION
        )
    }

    /**
     * Vrai si l'appareil peut fournir une position.
     *
     * Le fournisseur reseau est accepte au meme titre que le GPS : en zone couverte il donne une
     * position plus vite, et l'enquete releve un lieu d'habitation, pas une limite de parcelle.
     */
    private fun localisationActivee(): Boolean = try {
        val gestionnaire = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        gestionnaire.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                gestionnaire.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    } catch (ex: Exception) {
        LogUtils.e("Etat du service de localisation : ${ex.message}")
        false
    }

    private fun proposerActivationLocalisation() {
        Commons.showMessage(
            message = getString(R.string.localisation_desactivee_proposer_activation),
            context = this,
            finished = false,
            callback = {
                try {
                    startActivityForResult(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                        DEMANDE_ACTIVATION_POSITION
                    )
                } catch (ex: Exception) {
                    // Certains constructeurs n'exposent pas cet ecran : le dire vaut mieux que
                    // laisser le bouton sans effet.
                    LogUtils.e("Ouverture des reglages de localisation : ${ex.message}")
                    Commons.showMessage(
                        message = getString(R.string.reglages_localisation_introuvables),
                        context = this, finished = false, callback = { },
                        positive = getString(R.string.ok)
                    )
                }
            },
            positive = getString(R.string.activer),
            deconnec = false,
            showNo = true
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != DEMANDE_AUTORISATION_POSITION) return

        // L'agent a accorde l'autorisation en reponse a son propre clic : on poursuit ce qu'il
        // avait demande, plutot que de le laisser rappuyer sur le bouton.
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            capturerPosition()
        }
    }

    // ------------------------------------------------------------------ filtre et consentement

    private fun preparerFiltre() {
        listeSimple(binding.selectEstRepondantMenage, getString(R.string.le_producteur_est_il_le_repondant),
            VocabulairesPfte.OUI_NON) { appliquerRepondant() }

        listeSimple(binding.selectTitreRepondantMenage, getString(R.string.titre_du_repondant),
            VocabulairesPfte.TITRES_REPONDANT) { }

        listeSimple(binding.selectProducteurDisponibleMenage, getString(R.string.le_producteur_est_il_disponible),
            VocabulairesPfte.OUI_NON) { appliquerDisponibilite() }

        listeSimple(binding.selectRaisonIndisponibiliteMenage, getString(R.string.raison_de_l_indisponibilite),
            VocabulairesPfte.RAISONS_INDISPONIBILITE) { appliquerRaisonIndisponibilite() }

        listeSimple(binding.selectConsentementMenage, getString(R.string.consentement_du_producteur),
            VocabulairesPfte.OUI_NON) { appliquerConsentement() }

        Commons.setupItemMultiSelection(
            context = this,
            selectItemMulti = binding.selectRaisonRefusMenage,
            title = getString(R.string.raison_s_du_refus),
            itemList = VocabulairesPfte.RAISONS_REFUS.map { CommonData(nom = it) },
            currentList = raisonsRefusRetenues
        ) { choix ->
            raisonsRefusRetenues.clear()
            raisonsRefusRetenues.addAll(choix)
            // Regle R5 : "Autre raison" ouvre le champ de precision.
            SaisiePfte.basculer(binding.blocAutreRaisonRefusMenage, SaisiePfte.exigePrecision(choix))
        }

        binding.editDatePlanificationMenage.setOnClickListener {
            choisirDate(binding.editDatePlanificationMenage, futureAutorisee = true)
        }
    }

    /** Regle R1. */
    private fun appliquerRepondant() {
        val estRepondant = valeur(binding.selectEstRepondantMenage) == "Oui"

        SaisiePfte.basculer(binding.blocTitreRepondantMenage, !estRepondant)
        if (estRepondant) {
            binding.editNomRepondantMenage.setText(producteurRetenu.nom.orEmpty())
            binding.editNomRepondantMenage.isEnabled = false
        } else {
            binding.editNomRepondantMenage.isEnabled = true
        }
    }

    /** Regle R2 et R6. */
    private fun appliquerDisponibilite() {
        val reponse = valeur(binding.selectProducteurDisponibleMenage)
        val disponible = reponse == "Oui"

        SaisiePfte.basculer(binding.blocIndisponibleMenage, !disponible)
        SaisiePfte.basculer(binding.blocConsentementMenage, disponible)

        // Producteur indisponible : l'enquete s'arrete, mais elle part quand meme avec son motif.
        binding.labelFinEnqueteMenage.visibility = if (!disponible && reponse.isNotBlank()) View.VISIBLE else View.GONE
        if (!disponible) masquerSuite()
        appliquerConsentement()
    }

    /** Regles R3 et R4. */
    private fun appliquerRaisonIndisponibilite() {
        val raison = valeur(binding.selectRaisonIndisponibiliteMenage)

        SaisiePfte.basculer(binding.blocDatePlanificationMenage, raison == VocabulairesPfte.ABSENCE_TEMPORAIRE)
        SaisiePfte.basculer(binding.blocRaisonRefusMenage, raison == VocabulairesPfte.REFUS)

        if (raison != VocabulairesPfte.REFUS) {
            raisonsRefusRetenues.clear()
            SaisiePfte.basculer(binding.blocAutreRaisonRefusMenage, false)
        }
    }

    /** Regle R7. */
    private fun appliquerConsentement() {
        if (valeur(binding.selectProducteurDisponibleMenage) != "Oui") return

        val reponse = valeur(binding.selectConsentementMenage)
        val consent = reponse == "Oui"

        binding.labelFinEnqueteMenage.visibility = if (!consent && reponse.isNotBlank()) View.VISIBLE else View.GONE
        if (consent) {
            SaisiePfte.basculer(binding.blocSuiteFormulaireMenage, true)
            appliquerEnfantsACharge()
        } else {
            masquerSuite()
        }
    }

    /**
     * Referme tout le corps du formulaire.
     *
     * Les champs sont vides autant que masques : une enquete arretee ne doit rien porter de ce qui
     * aurait pu etre saisi avant que l'agent ne revienne en arriere.
     */
    private fun masquerSuite() {
        SaisiePfte.basculer(binding.blocSuiteFormulaireMenage, false)
        fichesEnfant.viderTout()
    }

    // ------------------------------------------------------------------ caracteristiques du menage

    private fun preparerMenage() {
        listeSimple(binding.selectSituationMatrimonialeMenage, getString(R.string.situation_matrimoniale),
            VocabulairesPfte.SITUATIONS_MATRIMONIALES) { }

        listeSimple(binding.selectAEnfantAChargeMenage, getString(R.string.y_a_t_il_des_enfants_a_charge),
            VocabulairesPfte.OUI_NON) { appliquerEnfantsACharge() }

        // Calcul C1 : le total des personnes du menage suit les trois effectifs.
        listOf(binding.editNombreAdultesMenage, binding.editNombreEnfants0a4Menage,
            binding.editNombreEnfants5a17Menage).forEach { champ ->
            champ.addTextChangedListener(surSaisie { calculerTotalMenage() })
        }

        // Calcul C2 : le nombre d'enfants a enqueter suit le nombre d'enfants de 5 a 17 ans, et
        // plafonne les fiches. Regle R8 : a zero, il n'y a plus d'enfant a charge du tout.
        binding.editNombreEnfants5a17Menage.addTextChangedListener(surSaisie { appliquerNombreEnfants() })

        binding.btnAjouterEnfantMenage.setOnClickListener { ajouterFicheEnfant() }
    }

    private fun calculerTotalMenage() {
        val total = entier(binding.editNombreAdultesMenage) +
                entier(binding.editNombreEnfants0a4Menage) +
                entier(binding.editNombreEnfants5a17Menage)
        binding.editTotalPersonnesMenage.setText(total.toString())
    }

    /** Regle R8 et calcul C2. */
    private fun appliquerNombreEnfants() {
        val enfants5a17 = entier(binding.editNombreEnfants5a17Menage)
        fichesEnfant.definirPlafond(enfants5a17)

        if (enfants5a17 == 0) {
            forcerValeur(binding.selectAEnfantAChargeMenage, "Non")
            binding.editNombreEnfantsAChargeMenage.setText("0")
            fichesEnfant.viderTout()
        }
        appliquerEnfantsACharge()
    }

    /** Regle R9. */
    private fun appliquerEnfantsACharge() {
        val aEnfantACharge = valeur(binding.selectAEnfantAChargeMenage) == "Oui" &&
                entier(binding.editNombreEnfants5a17Menage) > 0

        // Une seule primitive pour les trois blocs : elle vide a la fermeture et reactive a la
        // reouverture, ce que des affectations de visibilite ne feraient ni l'un ni l'autre.
        SaisiePfte.basculer(binding.blocNombreEnfantsAChargeMenage, aEnfantACharge)
        SaisiePfte.basculer(binding.cardEnfantsMenage, aEnfantACharge)
        SaisiePfte.basculer(binding.cardSensibilisationMenage, aEnfantACharge)

        if (!aEnfantACharge) {
            // Les selections ne vivent pas dans les vues : les vider a la main.
            themesRetenus.clear()
            outilsRetenus.clear()
            photoBase64 = ""
            binding.imagePhotoSensibilisationMenage.setImageDrawable(null)
            fichesEnfant.viderTout()
        }
        mettreAJourBoutonAjout()
    }

    // ------------------------------------------------------------------ sensibilisation

    private fun preparerSensibilisation() {
        SaisiePfte.listeMultiple(
            activite = this,
            composant = binding.selectThemesSensibilisationMenage,
            table = ReferentielsPfte.THEMES_SENSIBILISATION,
            titre = getString(R.string.themes_abordes),
            selectionCourante = themesRetenus
        ) { choix ->
            themesRetenus.clear()
            themesRetenus.addAll(choix)
            // Regle R10 : un theme "Autres themes" ouvre le champ de precision.
            SaisiePfte.basculer(binding.blocAutreThemeMenage, SaisiePfte.exigePrecision(choix))
        }

        SaisiePfte.listeMultiple(
            activite = this,
            composant = binding.selectOutilsSensibilisationMenage,
            table = ReferentielsPfte.OUTILS_SENSIBILISATION,
            titre = getString(R.string.outils_utilises),
            selectionCourante = outilsRetenus
        ) { choix ->
            outilsRetenus.clear()
            outilsRetenus.addAll(choix)
        }

        // Calcul C3 : le total des personnes sensibilisees suit les quatre effectifs.
        listOf(binding.editNombreHommesMenage, binding.editNombreFemmesMenage,
            binding.editNombreGarconsMenage, binding.editNombreFillesMenage).forEach { champ ->
            champ.addTextChangedListener(surSaisie { calculerTotalSensibilises() })
        }

        binding.btnPrendrePhotoMenage.setOnClickListener { choisirSourcePhoto() }
    }

    private fun calculerTotalSensibilises() {
        val total = entier(binding.editNombreHommesMenage) + entier(binding.editNombreFemmesMenage) +
                entier(binding.editNombreGarconsMenage) + entier(binding.editNombreFillesMenage)
        binding.editTotalSensibiliseesMenage.setText(total.toString())
    }

    // ------------------------------------------------------------------ fiches enfant

    private fun preparerFichesEnfant() {
        fichesEnfant = FichesEnfant(
            contexte = this,
            conteneur = binding.conteneurFichesEnfantMenage,
            layoutFiche = R.layout.fiche_enfant_menage,
            idNumero = R.id.labelNumeroEnfant,
            idBoutonRetirer = R.id.btnRetirerEnfant,
            surCreation = { vue, _ -> cablerFiche(vue) },
            lecture = { vue, _ -> lireFiche(vue) },
            ecriture = { vue, modele -> ecrireFiche(vue, modele) },
            surChangementNombre = { mettreAJourBoutonAjout() }
        )
    }

    private fun ajouterFicheEnfant() {
        if (fichesEnfant.ajouter() == null) {
            Commons.showMessage(
                message = getString(R.string.plafond_fiches_enfant_atteint),
                context = this, finished = false, callback = { },
                positive = getString(R.string.ok)
            )
        }
    }

    /** Regle R11 : on ne peut pas enqueter plus d'enfants qu'il n'y en a de 5 a 17 ans. */
    private fun mettreAJourBoutonAjout() {
        binding.btnAjouterEnfantMenage.isEnabled = !fichesEnfant.plafondAtteint()
    }

    /** Branche les listes et les dix regles d'une fiche. */
    private fun cablerFiche(vue: View) {
        // Reprend l'etat deja pose s'il existe : la restauration d'un brouillon remplit les
        // selections AVANT de rebrancher les listes, et en creer un neuf ici les perdrait toutes.
        val etat = (vue.getTag(R.id.tagSelectionsFiche) as? SelectionsFiche)
            ?: SelectionsFiche().also { vue.setTag(R.id.tagSelectionsFiche, it) }

        val dateNaissance = vue.findViewById<AppCompatEditText>(R.id.editDateNaissanceEnfant)
        dateNaissance.setOnClickListener { choisirDate(dateNaissance) }

        listeSimple(spinnerDe(vue, R.id.selectSexeEnfant), getString(R.string.sexe),
            VocabulairesPfte.SEXE_ENFANT) { }

        // Regle F1.
        listeSimple(spinnerDe(vue, R.id.selectLienParenteEnfant), getString(R.string.lien_de_parente),
            VocabulairesPfte.LIENS_PARENTE) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreLienParenteEnfant), choix == "Autre")
        }

        // Regle F2.
        listeSimple(spinnerDe(vue, R.id.selectRaisonNeVitPasParentsEnfant),
            getString(R.string.raison_ne_vit_pas_avec_ses_parents),
            VocabulairesPfte.RAISONS_NE_VIT_PAS_PARENTS) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNeVitPasParentsEnfant), choix == "Autres")
        }

        // Regles F3 et F4.
        listeSimple(spinnerDe(vue, R.id.selectSituationScolaireEnfant), getString(R.string.situation_scolaire),
            VocabulairesPfte.SITUATIONS_SCOLAIRES) { choix ->
            SaisiePfte.basculer(
                vue.findViewById(R.id.blocNiveauScolaireEnfant),
                choix == VocabulairesPfte.SCOLARISE || choix == VocabulairesPfte.DESCOLARISE
            )
            SaisiePfte.basculer(
                vue.findViewById(R.id.blocRaisonNonScolarisationEnfant),
                choix == VocabulairesPfte.DESCOLARISE || choix == VocabulairesPfte.JAMAIS_SCOLARISE
            )
            if (choix == VocabulairesPfte.SCOLARISE) {
                etat.raisonNonScolarisation.clear()
                SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNonScolarisationEnfant), false)
            }
        }

        // Les classes portent leur niveau en prefixe : la liste deroulante Android ne sait pas
        // rendre les groupes du back-office.
        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.classe),
            message = getString(R.string.referentiels_pfte_manquants),
            isEmpty = false,
            spinner = spinnerDe(vue, R.id.selectNiveauScolaireEnfant),
            listIem = SaisiePfte.classesAffichage(this),
            onChanged = { }, onSelected = { _, _ -> }
        )

        // Regle F6.
        listeSimple(spinnerDe(vue, R.id.selectExtraitNaissanceEnfant),
            getString(R.string.dispose_d_un_extrait_de_naissance), VocabulairesPfte.OUI_NON) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocRaisonPasExtraitEnfant), choix == "Non")
            if (choix != "Non") etat.raisonPasExtrait.clear()
        }

        // Regle F5.
        listeMultipleFiche(vue, R.id.selectRaisonNonScolarisationEnfant,
            ReferentielsPfte.RAISONS_NON_SCOLARISATION, getString(R.string.raisons_de_non_scolarisation),
            etat.raisonNonScolarisation) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNonScolarisationEnfant),
                SaisiePfte.exigePrecision(choix))
        }

        listeMultipleFiche(vue, R.id.selectRaisonPasExtraitEnfant,
            ReferentielsPfte.RAISONS_PAS_EXTRAIT, getString(R.string.raisons_absence_extrait),
            etat.raisonPasExtrait) { }

        // Regle F7.
        listeMultipleFiche(vue, R.id.selectSituationsPfteEnfant,
            ReferentielsPfte.SITUATIONS_PFTE, getString(R.string.situations_pfte_constatees),
            etat.situationsPfte) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocRaisonTravailAbusEnfant), exigeRaisonTravail(choix))
            if (!exigeRaisonTravail(choix)) etat.raisonTravailAbus.clear()
        }

        // Regle F8.
        listeMultipleFiche(vue, R.id.selectRaisonTravailAbusEnfant,
            ReferentielsPfte.RAISONS_TRAVAIL_ABUS, getString(R.string.raisons_du_travail_ou_de_l_abus),
            etat.raisonTravailAbus) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonTravailAbusEnfant),
                SaisiePfte.exigePrecision(choix))
        }

        // Regle F9 : le champ de precision est partage par les trois listes de mesures.
        val surMesures = { _: MutableList<String> ->
            SaisiePfte.basculer(
                vue.findViewById(R.id.blocAutreMesureEnfant),
                SaisiePfte.exigePrecision(etat.mesuresEnfant, etat.mesuresMenage, etat.mesuresCommunaute)
            )
        }
        listeMultipleFiche(vue, R.id.selectMesuresEnfantEnfant, ReferentielsPfte.MESURES_ENFANT,
            getString(R.string.mesures_au_niveau_de_l_enfant), etat.mesuresEnfant, surMesures)
        listeMultipleFiche(vue, R.id.selectMesuresMenageEnfant, ReferentielsPfte.MESURES_MENAGE,
            getString(R.string.mesures_au_niveau_du_menage), etat.mesuresMenage, surMesures)
        listeMultipleFiche(vue, R.id.selectMesuresCommunauteEnfant, ReferentielsPfte.MESURES_COMMUNAUTE,
            getString(R.string.mesures_au_niveau_de_la_communaute), etat.mesuresCommunaute, surMesures)
    }

    /**
     * Regle F7 : une situation constatee appelle une raison, sauf si la seule retenue est "Aucune".
     */
    private fun exigeRaisonTravail(situations: List<String>): Boolean =
        situations.isNotEmpty() && situations.any { !it.contains("Aucune", ignoreCase = true) }

    private fun lireFiche(vue: View): EnfantMenageModel? {
        val nom = texte(vue, R.id.editNomEnfant)
        val naissance = texte(vue, R.id.editDateNaissanceEnfant)
        // Une fiche laissee entierement vide n'est pas un enfant : elle est simplement ignoree.
        if (nom.isBlank() && naissance.isBlank()) return null

        val etat = selections(vue)
        return EnfantMenageModel(
            nom = nom,
            dateNaissance = naissance,
            sexe = valeur(spinnerDe(vue, R.id.selectSexeEnfant)),
            lienParente = valeur(spinnerDe(vue, R.id.selectLienParenteEnfant)),
            autreLienParente = texte(vue, R.id.editAutreLienParenteEnfant),
            raisonNeVitPasParents = valeur(spinnerDe(vue, R.id.selectRaisonNeVitPasParentsEnfant)),
            autreRaisonNeVitPasParents = texte(vue, R.id.editAutreRaisonNeVitPasParentsEnfant),
            situationScolaire = valeur(spinnerDe(vue, R.id.selectSituationScolaireEnfant)),
            // Le prefixe de niveau sert a l'affichage : le serveur attend le seul nom de la classe.
            niveauScolaire = SaisiePfte.classeDepuisLibelle(valeur(spinnerDe(vue, R.id.selectNiveauScolaireEnfant))),
            autreRaisonNonScolarisation = texte(vue, R.id.editAutreRaisonNonScolarisationEnfant),
            extraitNaissance = valeur(spinnerDe(vue, R.id.selectExtraitNaissanceEnfant)),
            autreRaisonTravailAbus = texte(vue, R.id.editAutreRaisonTravailAbusEnfant),
            autreMesure = texte(vue, R.id.editAutreMesureEnfant),
            raisonNonScolarisation = etat.raisonNonScolarisation.toMutableList(),
            raisonPasExtrait = etat.raisonPasExtrait.toMutableList(),
            situationsPfte = etat.situationsPfte.toMutableList(),
            raisonTravailAbus = etat.raisonTravailAbus.toMutableList(),
            mesuresEnfant = etat.mesuresEnfant.toMutableList(),
            mesuresMenage = etat.mesuresMenage.toMutableList(),
            mesuresCommunaute = etat.mesuresCommunaute.toMutableList()
        )
    }

    /**
     * Reporte une fiche enregistree dans ses vues.
     *
     * L'ordre compte. Les selections multiples sont posees d'abord, puis les listes sont
     * rebranchees pour que le composant les affiche - rebrancher ensuite remettrait les listes
     * simples a zero. Les regles propres aux choix multiples sont enfin rejouees a la main :
     * seules celles des listes simples se declenchent d'elles-memes au repositionnement.
     */
    private fun ecrireFiche(vue: View, modele: EnfantMenageModel) {
        val etat = selections(vue)
        etat.raisonNonScolarisation.apply { clear(); addAll(modele.raisonNonScolarisation) }
        etat.raisonPasExtrait.apply { clear(); addAll(modele.raisonPasExtrait) }
        etat.situationsPfte.apply { clear(); addAll(modele.situationsPfte) }
        etat.raisonTravailAbus.apply { clear(); addAll(modele.raisonTravailAbus) }
        etat.mesuresEnfant.apply { clear(); addAll(modele.mesuresEnfant) }
        etat.mesuresMenage.apply { clear(); addAll(modele.mesuresMenage) }
        etat.mesuresCommunaute.apply { clear(); addAll(modele.mesuresCommunaute) }

        cablerFiche(vue)

        vue.findViewById<AppCompatEditText>(R.id.editNomEnfant).setText(modele.nom)
        vue.findViewById<AppCompatEditText>(R.id.editDateNaissanceEnfant).setText(modele.dateNaissance)
        vue.findViewById<AppCompatEditText>(R.id.editAutreLienParenteEnfant).setText(modele.autreLienParente)
        vue.findViewById<AppCompatEditText>(R.id.editAutreRaisonNeVitPasParentsEnfant)
            .setText(modele.autreRaisonNeVitPasParents)
        vue.findViewById<AppCompatEditText>(R.id.editAutreRaisonNonScolarisationEnfant)
            .setText(modele.autreRaisonNonScolarisation)
        vue.findViewById<AppCompatEditText>(R.id.editAutreRaisonTravailAbusEnfant)
            .setText(modele.autreRaisonTravailAbus)
        vue.findViewById<AppCompatEditText>(R.id.editAutreMesureEnfant).setText(modele.autreMesure)

        // Repositionner une liste simple declenche son ecouteur, donc sa regle d'affichage.
        forcerValeur(spinnerDe(vue, R.id.selectSexeEnfant), modele.sexe)
        forcerValeur(spinnerDe(vue, R.id.selectLienParenteEnfant), modele.lienParente)
        forcerValeur(spinnerDe(vue, R.id.selectRaisonNeVitPasParentsEnfant), modele.raisonNeVitPasParents)
        forcerValeur(spinnerDe(vue, R.id.selectSituationScolaireEnfant), modele.situationScolaire)
        forcerValeur(spinnerDe(vue, R.id.selectNiveauScolaireEnfant),
            SaisiePfte.libellePourClasse(this, modele.niveauScolaire))
        forcerValeur(spinnerDe(vue, R.id.selectExtraitNaissanceEnfant), modele.extraitNaissance)

        rejouerReglesMultiples(vue, etat)
    }

    /** Regles F5, F7, F8 et F9, que le repositionnement d'un choix multiple ne declenche pas. */
    private fun rejouerReglesMultiples(vue: View, etat: SelectionsFiche) {
        SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNonScolarisationEnfant),
            SaisiePfte.exigePrecision(etat.raisonNonScolarisation))
        SaisiePfte.basculer(vue.findViewById(R.id.blocRaisonTravailAbusEnfant),
            exigeRaisonTravail(etat.situationsPfte))
        SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonTravailAbusEnfant),
            SaisiePfte.exigePrecision(etat.raisonTravailAbus))
        SaisiePfte.basculer(vue.findViewById(R.id.blocAutreMesureEnfant),
            SaisiePfte.exigePrecision(etat.mesuresEnfant, etat.mesuresMenage, etat.mesuresCommunaute))
    }

    // ------------------------------------------------------------------ enregistrement

    private fun preparerBoutons() {
        binding.clickCloseBtn.setOnClickListener { finish() }
        binding.imageDraftBtn.setOnClickListener { mettreAuBrouillon() }
        binding.btnBrouillonMenage.setOnClickListener { mettreAuBrouillon() }
        binding.btnEnregistrerMenage.setOnClickListener { enregistrer() }
    }

    /**
     * Construit l'enquete a partir de l'ecran.
     *
     * @param controler false pour un brouillon : une saisie incomplete doit pouvoir etre mise de
     *        cote, c'est meme sa raison d'etre. Le back-office applique la meme validation aux
     *        deux, ce qui vide en partie le sens de son propre bouton brouillon ; le mobile ne
     *        reproduit pas ce defaut, le brouillon restant purement local.
     */
    private fun construireEnquete(): EnqueteMenageModel? {
        val disponible = valeur(binding.selectProducteurDisponibleMenage) == "Oui"
        val enfants = fichesEnfant.collecter()

        return EnqueteMenageModel(
            uid = 0,
            userid = agentId,
            sectionId = sectionRetenue.id?.toString().orEmpty(),
            localiteId = localiteRetenue.id?.toString().orEmpty(),
            producteurId = producteurRetenu.id?.toString().orEmpty(),
            dateEnquete = texte(binding.editDateEnqueteMenage),
            nomEnqueteur = texte(binding.editNomEnqueteurMenage),
            latitude = texte(binding.editLatitudeMenage),
            longitude = texte(binding.editLongitudeMenage),
            altitude = texte(binding.editAltitudeMenage),
            precisionGps = texte(binding.editPrecisionGpsMenage),

            estProducteurRepondant = valeur(binding.selectEstRepondantMenage),
            nomRepondant = texte(binding.editNomRepondantMenage),
            titreRepondant = valeur(binding.selectTitreRepondantMenage),
            producteurDisponible = valeur(binding.selectProducteurDisponibleMenage),
            // Le serveur force cette valeur quand le producteur est disponible : on l'ecrit aussi,
            // pour que l'enquete conservee sur l'appareil dise la meme chose que celle en base.
            raisonIndisponibilite = if (disponible) VocabulairesPfte.SANS_OBJET
            else valeur(binding.selectRaisonIndisponibiliteMenage),
            datePlanification = texte(binding.editDatePlanificationMenage),
            autreRaisonRefus = texte(binding.editAutreRaisonRefusMenage),
            consentement = valeur(binding.selectConsentementMenage),

            situationMatrimoniale = valeur(binding.selectSituationMatrimonialeMenage),
            nombreAdultes = texte(binding.editNombreAdultesMenage),
            nombreEnfants0a4 = texte(binding.editNombreEnfants0a4Menage),
            nombreEnfants5a17 = texte(binding.editNombreEnfants5a17Menage),
            aEnfantACharge = valeur(binding.selectAEnfantAChargeMenage),
            nombreEnfantsACharge = texte(binding.editNombreEnfantsAChargeMenage),
            // Calcul C2 : plafond des fiches, recopie du nombre d'enfants de 5 a 17 ans.
            nombreEnfantsEnquetes = texte(binding.editNombreEnfants5a17Menage),

            autreThemeSensibilisation = texte(binding.editAutreThemeMenage),
            nombreHommesSensibilises = texte(binding.editNombreHommesMenage),
            nombreFemmesSensibilisees = texte(binding.editNombreFemmesMenage),
            nombreGarconsSensibilises = texte(binding.editNombreGarconsMenage),
            nombreFillesSensibilisees = texte(binding.editNombreFillesMenage),
            telephoneProducteurSensibilisation = texte(binding.editTelephoneSensibilisationMenage),
            photoSensibilisation = photoBase64,

            etatSoumission = "Soumis",

            raisonRefusStringify = BrouillonPfte.serialiserListe(raisonsRefusRetenues),
            themesSensibilisationStringify = BrouillonPfte.serialiserListe(themesRetenus),
            outilsSensibilisationStringify = BrouillonPfte.serialiserListe(outilsRetenus),
            enfantsStringify = BrouillonPfte.serialiserListe(enfants),

            isSynced = false,
            origin = "local",
            agentId = agentId.toString(),
            sectionNom = sectionRetenue.nom.orEmpty(),
            localiteNom = localiteRetenue.nom.orEmpty(),
            producteurNom = producteurRetenu.nom.orEmpty()
        )
    }

    /**
     * Affiche les informations manquantes.
     *
     * @return vrai si l'enquete est incomplete, auquel cas rien n'est enregistre.
     */
    private fun signalerManques(enquete: EnqueteMenageModel): Boolean {
        val manques = ControlesEnqueteMenage.manquants(enquete, fichesEnfant.collecter())
        if (manques.isEmpty()) return false

        val details = manques.joinToString("\n") { manque ->
            val prefixe = manque.enfant?.let { getString(R.string.enfant_n) + "$it : " }.orEmpty()
            "- $prefixe" + getString(manque.libelle)
        }
        Commons.showMessage(
            message = getString(R.string.champs_obligatoires_manquants) + "\n\n" + details,
            context = this, finished = false, callback = { },
            positive = getString(R.string.ok)
        )
        return true
    }

    /**
     * Ouvre le recapitulatif.
     *
     * L'enquete n'est pas ecrite ici : elle l'est apres relecture, dans le recapitulatif. C'est
     * le dernier point ou l'agent peut corriger - le serveur n'offrant aucun endpoint de
     * relecture, une enquete transmise n'est plus modifiable depuis le telephone.
     */
    private fun enregistrer() {
        val enquete = construireEnquete() ?: return
        if (signalerManques(enquete)) return

        startActivity(
            Intent(this, EnqueteMenagePreviewActivity::class.java)
                .putExtra(EnqueteMenagePreviewActivity.EXTRA_ENQUETE, enquete)
                .putExtra(EnqueteMenagePreviewActivity.EXTRA_BROUILLON, brouillonOuvert?.uid ?: 0)
        )
    }

    private fun mettreAuBrouillon() {
        val enquete = construireEnquete() ?: return
        enquete.etatSoumission = "Brouillon"

        BrouillonPfte.enregistrer(
            activite = this,
            type = BrouillonPfte.TYPE_ENQUETE_MENAGE,
            modele = enquete,
            uidExistant = brouillonOuvert?.uid ?: 0,
            boutonBrouillon = binding.imageDraftBtn
        ) { finish() }
    }

    // ------------------------------------------------------------------ reprise d'un brouillon

    private fun rouvrirBrouillonEventuel() {
        val uid = intent.getIntExtra(EXTRA_BROUILLON, 0)
        if (uid == 0) return

        brouillonOuvert = BrouillonPfte.parId(this, uid)
        val enquete = BrouillonPfte.lire(brouillonOuvert, EnqueteMenageModel::class.java)
        if (enquete == null) {
            LogUtils.e("Brouillon d'enquete menage $uid illisible")
            return
        }
        restaurer(enquete)
    }

    private fun restaurer(enquete: EnqueteMenageModel) {
        // La cascade est rejouee depuis la section : chaque niveau alimente le suivant.
        choisirSection(enquete.sectionId, enquete.localiteId, enquete.producteurId)

        binding.editDateEnqueteMenage.setText(enquete.dateEnquete)
        binding.editNomEnqueteurMenage.setText(enquete.nomEnqueteur)
        binding.editLatitudeMenage.setText(enquete.latitude)
        binding.editLongitudeMenage.setText(enquete.longitude)
        binding.editAltitudeMenage.setText(enquete.altitude)
        binding.editPrecisionGpsMenage.setText(enquete.precisionGps)

        binding.editNomRepondantMenage.setText(enquete.nomRepondant)
        binding.editDatePlanificationMenage.setText(enquete.datePlanification)
        binding.editAutreRaisonRefusMenage.setText(enquete.autreRaisonRefus)

        binding.editNombreAdultesMenage.setText(enquete.nombreAdultes)
        binding.editNombreEnfants0a4Menage.setText(enquete.nombreEnfants0a4)
        binding.editNombreEnfants5a17Menage.setText(enquete.nombreEnfants5a17)
        binding.editNombreEnfantsAChargeMenage.setText(enquete.nombreEnfantsACharge)

        binding.editAutreThemeMenage.setText(enquete.autreThemeSensibilisation)
        binding.editNombreHommesMenage.setText(enquete.nombreHommesSensibilises)
        binding.editNombreFemmesMenage.setText(enquete.nombreFemmesSensibilisees)
        binding.editNombreGarconsMenage.setText(enquete.nombreGarconsSensibilises)
        binding.editNombreFillesMenage.setText(enquete.nombreFillesSensibilisees)
        binding.editTelephoneSensibilisationMenage.setText(enquete.telephoneProducteurSensibilisation)

        raisonsRefusRetenues.apply { clear(); addAll(SaisiePfte.deserialiser(enquete.raisonRefusStringify)) }
        themesRetenus.apply { clear(); addAll(SaisiePfte.deserialiser(enquete.themesSensibilisationStringify)) }
        outilsRetenus.apply { clear(); addAll(SaisiePfte.deserialiser(enquete.outilsSensibilisationStringify)) }

        photoBase64 = enquete.photoSensibilisation.orEmpty()
        afficherPhoto()

        // Les listes sont rebranchees pour afficher la selection restauree.
        preparerFiltre()
        preparerSensibilisation()

        // L'ordre compte : repositionner le consentement rejoue la regle des enfants a charge, qui
        // vide la sensibilisation tant qu'aucun enfant n'est declare. Ces deux listes sont donc
        // remises en place AVANT, sans quoi la restauration effacerait ce qu'elle vient de poser.
        forcerValeur(binding.selectSituationMatrimonialeMenage, enquete.situationMatrimoniale)
        forcerValeur(binding.selectAEnfantAChargeMenage, enquete.aEnfantACharge)

        forcerValeur(binding.selectEstRepondantMenage, enquete.estProducteurRepondant)
        forcerValeur(binding.selectTitreRepondantMenage, enquete.titreRepondant)
        forcerValeur(binding.selectProducteurDisponibleMenage, enquete.producteurDisponible)
        forcerValeur(binding.selectRaisonIndisponibiliteMenage, enquete.raisonIndisponibilite)
        forcerValeur(binding.selectConsentementMenage, enquete.consentement)

        // Regles R5 et R10 : un choix multiple repositionne ne declenche pas son ecouteur.
        SaisiePfte.basculer(binding.blocAutreRaisonRefusMenage,
            SaisiePfte.exigePrecision(raisonsRefusRetenues))
        SaisiePfte.basculer(binding.blocAutreThemeMenage, SaisiePfte.exigePrecision(themesRetenus))
        binding.editAutreRaisonRefusMenage.setText(enquete.autreRaisonRefus)
        binding.editAutreThemeMenage.setText(enquete.autreThemeSensibilisation)

        fichesEnfant.restaurer(
            BrouillonPfte.deserialiserListe(
                enquete.enfantsStringify, object : TypeToken<MutableList<EnfantMenageModel>>() {}
            )
        )

        calculerTotalMenage()
        calculerTotalSensibilises()
        appliquerRepondant()
        appliquerDisponibilite()
        appliquerRaisonIndisponibilite()
    }

    // ------------------------------------------------------------------ photo

    /**
     * Propose les deux sources possibles pour la photo de sensibilisation.
     *
     * Sur le terrain la photo est parfois deja prise - avant l'ouverture du formulaire, ou par un
     * collegue. N'offrir que l'appareil photo obligeait alors a la reprendre.
     */
    private fun choisirSourcePhoto() {
        val sources = arrayOf(
            getString(R.string.prendre_une_photo),
            getString(R.string.choisir_dans_la_galerie)
        )
        AlertDialog.Builder(this, R.style.DialogTheme)
            .setTitle(R.string.photo_de_la_sensibilisation)
            .setItems(sources) { dialogue, choix ->
                dialogue.dismiss()
                if (choix == 0) prendrePhoto() else ouvrirGalerie()
            }
            .setNegativeButton(R.string.non) { dialogue, _ -> dialogue.dismiss() }
            .show()
    }

    private fun prendrePhoto() {
        try {
            // Meme emplacement et meme autorite que les autres formulaires : file_paths.xml
            // n'expose que le dossier externe de l'application.
            val dossier = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val fichier = File.createTempFile("sensibilisation_", ".jpg", dossier)
            fichierPhoto = fichier

            val uri: Uri = FileProvider.getUriForFile(this, AUTORITE_FICHIERS, fichier)
            val intentPhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                .putExtra(MediaStore.EXTRA_OUTPUT, uri)
            startActivityForResult(intentPhoto, DEMANDE_PHOTO)
        } catch (ex: Exception) {
            LogUtils.e("Ouverture de l'appareil photo : ${ex.message}")
            signalerPhotoImpossible()
        }
    }

    /**
     * Ouvre le selecteur d'images du systeme.
     *
     * ACTION_GET_CONTENT plutot que l'acces direct a la galerie : le systeme accorde lui-meme
     * l'acces au fichier choisi, sans autorisation de lecture du stockage a demander a l'agent,
     * et le procede vaut sur toutes les versions d'Android que l'application supporte.
     */
    private fun ouvrirGalerie() {
        try {
            val intentGalerie = Intent(Intent.ACTION_GET_CONTENT)
                .setType("image/*")
                .addCategory(Intent.CATEGORY_OPENABLE)
            startActivityForResult(intentGalerie, DEMANDE_GALERIE)
        } catch (ex: Exception) {
            LogUtils.e("Ouverture de la galerie : ${ex.message}")
            signalerPhotoImpossible()
        }
    }

    private fun signalerPhotoImpossible() {
        Commons.showMessage(
            message = getString(R.string.photo_impossible),
            context = this, finished = false, callback = { },
            positive = getString(R.string.ok)
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Retour de l'ecran de reglages : on reprend la capture que l'agent avait demandee.
        // Le code de retour n'y est pas renseigne, c'est l'etat du service qui fait foi.
        if (requestCode == DEMANDE_ACTIVATION_POSITION) {
            capturerPosition()
            return
        }

        if (resultCode != RESULT_OK) return

        when (requestCode) {
            DEMANDE_PHOTO -> fichierPhoto?.let { traiterPhoto(it) }
            DEMANDE_GALERIE -> data?.data?.let { source ->
                val copie = copierDepuisGalerie(source)
                if (copie == null) signalerPhotoImpossible() else traiterPhoto(copie)
            }
        }
    }

    /**
     * Recopie l'image choisie dans le dossier de l'application.
     *
     * L'acces accorde par le systeme sur l'image d'origine ne survit pas a la fermeture de
     * l'ecran : sans copie, la photo deviendrait illisible avant meme d'etre encodee. Le
     * compresseur, lui, travaille sur un fichier.
     */
    private fun copierDepuisGalerie(source: Uri): File? = try {
        val dossier = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val destination = File.createTempFile("sensibilisation_", ".jpg", dossier)
        contentResolver.openInputStream(source)?.use { entree ->
            destination.outputStream().use { sortie -> entree.copyTo(sortie) }
        }
        fichierPhoto = destination
        destination
    } catch (ex: Exception) {
        LogUtils.e("Copie de l'image choisie : ${ex.message}")
        null
    }

    /**
     * Compresse puis encode la photo, quelle que soit sa provenance.
     *
     * L'API mobile attend du base64 - elle n'accepte pas le multipart - et le serveur refuse
     * au-dela de deux megaoctets. Une photo brute de telephone les depasse une fois encodee.
     */
    private fun traiterPhoto(fichier: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val compresse = Compressor.compress(this@EnqueteMenageActivity, fichier) {
                    quality(70)
                    format(Bitmap.CompressFormat.JPEG)
                }
                val encode = Commons.encodeFileToBase64Binary(compresse).orEmpty()
                runOnUiThread {
                    photoBase64 = encode
                    afficherPhoto()
                }
            } catch (ex: Exception) {
                LogUtils.e("Compression de la photo : ${ex.message}")
                runOnUiThread { signalerPhotoImpossible() }
            }
        }
    }

    private fun afficherPhoto() {
        val fichier = fichierPhoto
        if (fichier != null && fichier.exists()) {
            binding.imagePhotoSensibilisationMenage
                .setImageBitmap(BitmapFactory.decodeFile(fichier.absolutePath))
        }
    }

    // ------------------------------------------------------------------ petits outils

    private fun spinnerDe(vue: View, id: Int): android.widget.Spinner = vue.findViewById(id)

    private fun texte(champ: AppCompatEditText): String = champ.text?.toString()?.trim().orEmpty()

    private fun texte(vue: View, id: Int): String =
        vue.findViewById<AppCompatEditText>(id).text?.toString()?.trim().orEmpty()

    private fun entier(champ: AppCompatEditText): Int = texte(champ).toIntOrNull() ?: 0

    /**
     * Valeur retenue dans une liste deroulante, ou chaine vide si aucune.
     *
     * Commons.setListenerForSpinner insere toujours une ligne d'invite en tete. Sans ce controle
     * de position, une liste jamais ouverte renverrait ce libelle comme s'il s'agissait d'une
     * reponse - il serait compte comme renseigne par les controles de saisie, et partirait tel
     * quel au serveur. Il sert aussi de valeur vide : SaisiePfte.basculer() ramene les listes des
     * blocs masques sur cette premiere ligne.
     */
    private fun valeur(spinner: android.widget.Spinner): String =
        if (spinner.selectedItemPosition <= 0) ""
        else spinner.selectedItem?.toString()?.trim().orEmpty()

    /** Positionne une liste deroulante sur un libelle, pour la reprise d'un brouillon. */
    private fun forcerValeur(spinner: android.widget.Spinner, libelle: String?) {
        if (libelle.isNullOrBlank()) return
        val adaptateur = spinner.adapter ?: return
        for (i in 0 until adaptateur.count) {
            if (adaptateur.getItem(i)?.toString() == libelle) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun listeSimple(spinner: android.widget.Spinner, titre: String, valeurs: List<String>,
                            surChoix: (String) -> Unit) {
        Commons.setListenerForSpinner(
            this,
            title = titre,
            isEmpty = valeurs.isEmpty(),
            spinner = spinner,
            listIem = valeurs,
            onChanged = { position -> surChoix(valeurs[position]) },
            onSelected = { _, _ -> }
        )
    }

    private fun listeMultipleFiche(vue: View, id: Int, table: String, titre: String,
                                   selection: MutableList<String>,
                                   surChoix: (MutableList<String>) -> Unit) {
        SaisiePfte.listeMultiple(
            activite = this,
            composant = vue.findViewById<MultiSelectSpinner>(id),
            table = table,
            titre = titre,
            selectionCourante = selection
        ) { choix ->
            selection.clear()
            selection.addAll(choix)
            surChoix(choix)
        }
    }

    private fun choisirDate(champ: AppCompatEditText, futureAutorisee: Boolean = false) {
        val calendrier = Calendar.getInstance()
        val selecteur = DatePickerDialog(
            this,
            { _, annee, mois, jour ->
                champ.setText(Commons.convertDate("$jour-${mois + 1}-$annee", false))
            },
            calendrier.get(Calendar.YEAR), calendrier.get(Calendar.MONTH),
            calendrier.get(Calendar.DAY_OF_MONTH)
        )
        // Une replanification se situe dans le futur ; toutes les autres dates sont passees.
        if (!futureAutorisee) selecteur.datePicker.maxDate = DateTime.now().millis
        else selecteur.datePicker.minDate = DateTime.now().millis
        selecteur.show()
    }

    private fun surSaisie(action: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) = action()
    }

    companion object {
        const val EXTRA_BROUILLON = "brouillon_uid"
        private const val DEMANDE_PHOTO = 4201
        private const val DEMANDE_GALERIE = 4204
        private const val DEMANDE_ACTIVATION_POSITION = 4202
        private const val DEMANDE_AUTORISATION_POSITION = 4203
        private const val AUTORITE_FICHIERS = "ci.progbandama.mobile.fileprovider"
    }
}
