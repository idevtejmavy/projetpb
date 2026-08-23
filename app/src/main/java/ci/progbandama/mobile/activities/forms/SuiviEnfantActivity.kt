package ci.progbandama.mobile.activities.forms

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.FileProvider
import ci.progbandama.mobile.R
import ci.progbandama.mobile.activities.infospresenters.SuiviEnfantPreviewActivity
import ci.progbandama.mobile.databinding.ActivitySuiviEnfantBinding
import ci.progbandama.mobile.models.DataDraftedModel
import ci.progbandama.mobile.models.EnfantSuivableModel
import ci.progbandama.mobile.models.SuiviEnfantModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.repositories.datas.CommonData
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.Commons
import ci.progbandama.mobile.tools.Constants
import ci.progbandama.mobile.tools.ControlesSuiviEnfant
import ci.progbandama.mobile.tools.ReferentielsPfte
import ci.progbandama.mobile.tools.SaisiePfte
import ci.progbandama.mobile.tools.VocabulairesPfte
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
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
 * Suivi Enfant Travailleur (PFTE) - lot 5.
 *
 * Dernier module du programme, et le plus different des deux autres :
 *
 *  - il part d'un ENFANT deja recense, choisi dans une liste mise en cache lors de la derniere
 *    mise a jour. Sa selection pre-remplit quatorze champs, tous modifiables : c'est le principe
 *    d'une reevaluation, on repart de la situation connue pour constater ce qui a change ;
 *  - il n'a ni filtre ni consentement, donc aucun chemin d'arret anticipe ;
 *  - il n'a aucun champ de geolocalisation : le suivi porte sur une personne, pas sur un lieu ;
 *  - sa sensibilisation est toujours exigee, sans condition.
 *
 * Quatre valeurs partent en CODES et non en libelles - presence, motif d'absence, jours travailles
 * et heures de la journee la plus chargee. Voir VocabulairesPfte.
 */
class SuiviEnfantActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySuiviEnfantBinding

    private var enfantsSuivables: MutableList<EnfantSuivableModel> = mutableListOf()
    private var enfantRetenu: EnfantSuivableModel? = null
    private var brouillonOuvert: DataDraftedModel? = null

    private var photoBase64 = ""
    private var fichierPhoto: File? = null

    // Les composants de choix multiple ne restituent pas leur selection : on la retient.
    private val actionsRemediation = mutableListOf<String>()
    private val raisonNonScolarisation = mutableListOf<String>()
    private val raisonPasExtrait = mutableListOf<String>()
    private val situationsPfte = mutableListOf<String>()
    private val raisonTravailAbus = mutableListOf<String>()
    private val mesuresEnfant = mutableListOf<String>()
    private val mesuresMenage = mutableListOf<String>()
    private val mesuresCommunaute = mutableListOf<String>()
    private val themes = mutableListOf<String>()
    private val outils = mutableListOf<String>()

    private val agentId: Int get() = SPUtils.getInstance().getInt(Constants.AGENT_ID, 0)

    // ------------------------------------------------------------------ cycle de vie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySuiviEnfantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!referentielsPrets()) return
        if (!enfantsDisponibles()) return

        preparerIdentification()
        preparerRemediation()
        preparerReevaluation()
        preparerSensibilisation()
        preparerBoutons()

        binding.editDateEnqueteSet.setText(
            DateTime.now().toString(DateTimeFormat.forPattern("dd-MM-yyyy"))
        )
        binding.editNomEnqueteurSet.setText(SPUtils.getInstance().getString(Constants.AGENT_NAME, ""))

        rouvrirBrouillonEventuel()
    }

    private fun referentielsPrets(): Boolean {
        val manquants = SaisiePfte.referentielsManquants(this)
        if (manquants.isEmpty()) return true

        LogUtils.e("Suivi Enfant : referentiels manquants -> $manquants")
        Commons.showMessage(
            message = getString(R.string.referentiels_pfte_manquants),
            context = this, finished = true, callback = { finish() },
            positive = getString(R.string.ok)
        )
        return false
    }

    /**
     * Refuse d'ouvrir le formulaire si aucun enfant n'est en cache.
     *
     * Ce module ne cree pas d'enfant : il en suit un deja recense. Sans liste, la premiere liste
     * deroulante serait vide et l'agent ne pourrait rien saisir - autant le dire tout de suite, et
     * lui indiquer d'ou vient la liste.
     */
    private fun enfantsDisponibles(): Boolean {
        enfantsSuivables = ProgBandRoomDatabase.getDatabase(applicationContext)?.enfantSuivableDao()
            ?.getAll(agentId.toString()) ?: mutableListOf()

        if (enfantsSuivables.isNotEmpty()) return true

        Commons.showMessage(
            message = getString(R.string.aucun_enfant_suivable),
            context = this, finished = true, callback = { finish() },
            positive = getString(R.string.ok)
        )
        return false
    }

    // ------------------------------------------------------------------ identification

    private fun preparerIdentification() {
        choisirEnfant()
        binding.editDateEnqueteSet.setOnClickListener { choisirDate(binding.editDateEnqueteSet) }
    }

    private fun choisirEnfant(idCourant: String? = null) {
        val libelles = enfantsSuivables.map { it.libelle() }

        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.enfant_suivi),
            message = getString(R.string.aucun_enfant_suivable),
            isEmpty = enfantsSuivables.isEmpty(),
            currentVal = enfantsSuivables.firstOrNull { it.idServeur.toString() == idCourant }?.libelle(),
            spinner = binding.selectEnfantSet,
            listIem = libelles,
            onChanged = { position -> retenirEnfant(enfantsSuivables[position]) },
            onSelected = { _, _ -> }
        )
    }

    /**
     * Reporte la situation connue de l'enfant dans le formulaire.
     *
     * Les quatorze champs restent modifiables : la visite sert precisement a constater ce qui a
     * change depuis la derniere enquete.
     */
    private fun retenirEnfant(enfant: EnfantSuivableModel) {
        enfantRetenu = enfant

        binding.editProducteurSet.setText(enfant.producteurNom)
        binding.editLocaliteSet.setText(enfant.localiteNom)

        binding.editNomSet.setText(enfant.nom)
        binding.editDateNaissanceSet.setText(enfant.dateNaissance)
        binding.editAutreLienParenteSet.setText(enfant.autreLienParente)
        binding.editAutreRaisonNeVitPasParentsSet.setText(enfant.autreRaisonNeVitPasParents)
        binding.editAutreRaisonNonScolarisationSet.setText(enfant.autreRaisonNonScolarisation)
        binding.editAutreRaisonTravailAbusSet.setText(enfant.autreRaisonTravailAbus)
        binding.editAutreMesureSet.setText(enfant.autreMesure)

        // Repositionner une liste declenche son ecouteur, donc sa regle d'affichage.
        forcerValeur(binding.selectSexeSet, enfant.sexe)
        forcerValeur(binding.selectLienParenteSet, enfant.lienParente)
        forcerValeur(binding.selectRaisonNeVitPasParentsSet, enfant.raisonNeVitPasParents)
        forcerValeur(binding.selectSituationScolaireSet, enfant.situationScolaire)
        forcerValeur(binding.selectNiveauScolaireSet,
            SaisiePfte.libellePourClasse(this, enfant.niveauScolaire))
        forcerValeur(binding.selectExtraitNaissanceSet, enfant.extraitNaissance)
    }

    // ------------------------------------------------------------------ suivi depuis la derniere visite

    private fun preparerRemediation() {
        // Le vocabulaire n'est pas une table : c'est la fusion des trois listes de mesures, comme
        // le fait le back-office.
        Commons.setupItemMultiSelection(
            context = this,
            selectItemMulti = binding.selectActionsRemediationSet,
            title = getString(R.string.actions_de_remediation),
            itemList = SaisiePfte.actionsRemediation(this).map { CommonData(nom = it) },
            currentList = actionsRemediation
        ) { choix ->
            actionsRemediation.clear()
            actionsRemediation.addAll(choix)
        }
    }

    // ------------------------------------------------------------------ reevaluation

    private fun preparerReevaluation() {
        binding.editDateNaissanceSet.setOnClickListener { choisirDate(binding.editDateNaissanceSet) }

        listeSimple(binding.selectSexeSet, getString(R.string.sexe), VocabulairesPfte.SEXE_ENFANT) { }

        listeSimple(binding.selectLienParenteSet, getString(R.string.lien_de_parente),
            VocabulairesPfte.LIENS_PARENTE) { choix ->
            SaisiePfte.basculer(binding.blocAutreLienParenteSet, choix == "Autre")
        }

        listeSimple(binding.selectRaisonNeVitPasParentsSet,
            getString(R.string.raison_ne_vit_pas_avec_ses_parents),
            VocabulairesPfte.RAISONS_NE_VIT_PAS_PARENTS) { choix ->
            SaisiePfte.basculer(binding.blocAutreRaisonNeVitPasParentsSet, choix == "Autres")
        }

        listeSimple(binding.selectSituationScolaireSet, getString(R.string.situation_scolaire),
            VocabulairesPfte.SITUATIONS_SCOLAIRES) { choix ->
            SaisiePfte.basculer(
                binding.blocNiveauScolaireSet,
                choix == VocabulairesPfte.SCOLARISE || choix == VocabulairesPfte.DESCOLARISE
            )
            SaisiePfte.basculer(
                binding.blocRaisonNonScolarisationSet,
                choix == VocabulairesPfte.DESCOLARISE || choix == VocabulairesPfte.JAMAIS_SCOLARISE
            )
            if (choix == VocabulairesPfte.SCOLARISE) {
                raisonNonScolarisation.clear()
                SaisiePfte.basculer(binding.blocAutreRaisonNonScolarisationSet, false)
            }
        }

        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.classe),
            message = getString(R.string.referentiels_pfte_manquants),
            isEmpty = false,
            spinner = binding.selectNiveauScolaireSet,
            listIem = SaisiePfte.classesAffichage(this),
            onChanged = { }, onSelected = { _, _ -> }
        )

        listeSimple(binding.selectExtraitNaissanceSet,
            getString(R.string.dispose_d_un_extrait_de_naissance), VocabulairesPfte.OUI_NON) { choix ->
            SaisiePfte.basculer(binding.blocRaisonPasExtraitSet, choix == "Non")
            if (choix != "Non") raisonPasExtrait.clear()
        }

        // Presence et motif d'absence : deux listes a codes.
        listeSimple(binding.selectPresentDisponibleSet, getString(R.string.present_et_disponible),
            VocabulairesPfte.libelles(VocabulairesPfte.PRESENT_DISPONIBLE)) { choix ->
            val code = VocabulairesPfte.codePour(VocabulairesPfte.PRESENT_DISPONIBLE, choix)
            SaisiePfte.basculer(binding.blocRaisonAbsentSet, code == VocabulairesPfte.ABSENT)
        }
        listeSimple(binding.selectRaisonAbsentSet, getString(R.string.raison_de_l_absence),
            VocabulairesPfte.libelles(VocabulairesPfte.RAISONS_ABSENCE)) { }

        listeSimple(binding.selectJoursTravailSet, getString(R.string.jours_travailles_par_semaine),
            VocabulairesPfte.libelles(VocabulairesPfte.JOURS_TRAVAIL)) { }
        listeSimple(binding.selectHeuresJourneeSet, getString(R.string.heures_journee_la_plus_chargee),
            VocabulairesPfte.libelles(VocabulairesPfte.HEURES_JOURNEE)) { }

        listeMultiple(binding.selectRaisonNonScolarisationSet,
            ReferentielsPfte.RAISONS_NON_SCOLARISATION, getString(R.string.raisons_de_non_scolarisation),
            raisonNonScolarisation) { choix ->
            SaisiePfte.basculer(binding.blocAutreRaisonNonScolarisationSet,
                SaisiePfte.exigePrecision(choix))
        }

        listeMultiple(binding.selectRaisonPasExtraitSet, ReferentielsPfte.RAISONS_PAS_EXTRAIT,
            getString(R.string.raisons_absence_extrait), raisonPasExtrait) { }

        listeMultiple(binding.selectSituationsPfteSet, ReferentielsPfte.SITUATIONS_PFTE,
            getString(R.string.situations_pfte_constatees), situationsPfte) { choix ->
            SaisiePfte.basculer(binding.blocRaisonTravailAbusSet, exigeRaisonTravail(choix))
            if (!exigeRaisonTravail(choix)) raisonTravailAbus.clear()
        }

        listeMultiple(binding.selectRaisonTravailAbusSet, ReferentielsPfte.RAISONS_TRAVAIL_ABUS,
            getString(R.string.raisons_du_travail_ou_de_l_abus), raisonTravailAbus) { choix ->
            SaisiePfte.basculer(binding.blocAutreRaisonTravailAbusSet, SaisiePfte.exigePrecision(choix))
        }

        // Le champ de precision est partage par les trois listes de mesures.
        val surMesures = { _: MutableList<String> ->
            SaisiePfte.basculer(
                binding.blocAutreMesureSet,
                SaisiePfte.exigePrecision(mesuresEnfant, mesuresMenage, mesuresCommunaute)
            )
        }
        listeMultiple(binding.selectMesuresEnfantSet, ReferentielsPfte.MESURES_ENFANT,
            getString(R.string.mesures_au_niveau_de_l_enfant), mesuresEnfant, surMesures)
        listeMultiple(binding.selectMesuresMenageSet, ReferentielsPfte.MESURES_MENAGE,
            getString(R.string.mesures_au_niveau_du_menage), mesuresMenage, surMesures)
        listeMultiple(binding.selectMesuresCommunauteSet, ReferentielsPfte.MESURES_COMMUNAUTE,
            getString(R.string.mesures_au_niveau_de_la_communaute), mesuresCommunaute, surMesures)
    }

    /** Une situation constatee appelle une raison, sauf si la seule retenue est "Aucune". */
    private fun exigeRaisonTravail(situations: List<String>): Boolean =
        situations.isNotEmpty() && situations.any { !it.contains("Aucune", ignoreCase = true) }

    // ------------------------------------------------------------------ sensibilisation

    private fun preparerSensibilisation() {
        listeMultiple(binding.selectThemesSensibilisationSet, ReferentielsPfte.THEMES_SENSIBILISATION,
            getString(R.string.themes_abordes), themes) { choix ->
            SaisiePfte.basculer(binding.blocAutreThemeSet, SaisiePfte.exigePrecision(choix))
        }

        listeMultiple(binding.selectOutilsSensibilisationSet, ReferentielsPfte.OUTILS_SENSIBILISATION,
            getString(R.string.outils_utilises), outils) { }

        listOf(binding.editNombreHommesSet, binding.editNombreFemmesSet,
            binding.editNombreGarconsSet, binding.editNombreFillesSet).forEach { champ ->
            champ.addTextChangedListener(surSaisie { calculerTotalSensibilises() })
        }

        binding.btnPrendrePhotoSet.setOnClickListener { choisirSourcePhoto() }
    }

    private fun calculerTotalSensibilises() {
        val total = entier(binding.editNombreHommesSet) + entier(binding.editNombreFemmesSet) +
                entier(binding.editNombreGarconsSet) + entier(binding.editNombreFillesSet)
        binding.editTotalSensibiliseesSet.setText(total.toString())
    }

    // ------------------------------------------------------------------ photo

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
            val dossier = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val fichier = File.createTempFile("suivi_enfant_", ".jpg", dossier)
            fichierPhoto = fichier

            val uri: Uri = FileProvider.getUriForFile(this, AUTORITE_FICHIERS, fichier)
            startActivityForResult(
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri),
                DEMANDE_PHOTO
            )
        } catch (ex: Exception) {
            LogUtils.e("Ouverture de l'appareil photo : ${ex.message}")
            signalerPhotoImpossible()
        }
    }

    /**
     * ACTION_GET_CONTENT plutot qu'un acces direct a la galerie : le systeme accorde lui-meme
     * l'acces au fichier choisi, sans autorisation de lecture du stockage a demander a l'agent.
     */
    private fun ouvrirGalerie() {
        try {
            startActivityForResult(
                Intent(Intent.ACTION_GET_CONTENT).setType("image/*")
                    .addCategory(Intent.CATEGORY_OPENABLE),
                DEMANDE_GALERIE
            )
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
     * l'ecran : sans copie, la photo deviendrait illisible avant meme d'etre encodee.
     */
    private fun copierDepuisGalerie(source: Uri): File? = try {
        val dossier = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val destination = File.createTempFile("suivi_enfant_", ".jpg", dossier)
        contentResolver.openInputStream(source)?.use { entree ->
            destination.outputStream().use { sortie -> entree.copyTo(sortie) }
        }
        fichierPhoto = destination
        destination
    } catch (ex: Exception) {
        LogUtils.e("Copie de l'image choisie : ${ex.message}")
        null
    }

    /** L'API mobile attend du base64, et le serveur refuse au-dela de deux megaoctets. */
    private fun traiterPhoto(fichier: File) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val compresse = Compressor.compress(this@SuiviEnfantActivity, fichier) {
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
            binding.imagePhotoSensibilisationSet
                .setImageBitmap(BitmapFactory.decodeFile(fichier.absolutePath))
        }
    }

    // ------------------------------------------------------------------ enregistrement

    private fun preparerBoutons() {
        binding.clickCloseBtn.setOnClickListener { finish() }
        binding.imageDraftBtn.setOnClickListener { mettreAuBrouillon() }
        binding.btnBrouillonSet.setOnClickListener { mettreAuBrouillon() }
        binding.btnEnregistrerSet.setOnClickListener { enregistrer() }
    }

    private fun construireSuivi(): SuiviEnfantModel = SuiviEnfantModel(
        uid = 0,
        userid = agentId,
        enfantId = enfantRetenu?.idServeur?.toString().orEmpty(),
        dateEnquete = texte(binding.editDateEnqueteSet),
        nomEnqueteur = texte(binding.editNomEnqueteurSet),

        nom = texte(binding.editNomSet),
        dateNaissance = texte(binding.editDateNaissanceSet),
        sexe = valeur(binding.selectSexeSet),
        lienParente = valeur(binding.selectLienParenteSet),
        autreLienParente = texte(binding.editAutreLienParenteSet),
        raisonNeVitPasParents = valeur(binding.selectRaisonNeVitPasParentsSet),
        autreRaisonNeVitPasParents = texte(binding.editAutreRaisonNeVitPasParentsSet),
        situationScolaire = valeur(binding.selectSituationScolaireSet),
        // Le prefixe de niveau sert a l'affichage : le serveur attend le seul nom de la classe.
        niveauScolaire = SaisiePfte.classeDepuisLibelle(valeur(binding.selectNiveauScolaireSet)),
        autreRaisonNonScolarisation = texte(binding.editAutreRaisonNonScolarisationSet),
        extraitNaissance = valeur(binding.selectExtraitNaissanceSet),

        // Codes et non libelles : c'est ce que le back-office enregistre.
        presentDisponible = VocabulairesPfte.codePour(
            VocabulairesPfte.PRESENT_DISPONIBLE, valeur(binding.selectPresentDisponibleSet)),
        raisonAbsent = VocabulairesPfte.codePour(
            VocabulairesPfte.RAISONS_ABSENCE, valeur(binding.selectRaisonAbsentSet)),
        heuresTravailSemaine = texte(binding.editHeuresTravailSemaineSet),
        joursTravail = VocabulairesPfte.codePour(
            VocabulairesPfte.JOURS_TRAVAIL, valeur(binding.selectJoursTravailSet)),
        heuresTravailJournee = VocabulairesPfte.codePour(
            VocabulairesPfte.HEURES_JOURNEE, valeur(binding.selectHeuresJourneeSet)),

        autreRaisonTravailAbus = texte(binding.editAutreRaisonTravailAbusSet),
        autreMesure = texte(binding.editAutreMesureSet),

        autreThemeSensibilisation = texte(binding.editAutreThemeSet),
        nombreHommesSensibilises = texte(binding.editNombreHommesSet),
        nombreFemmesSensibilisees = texte(binding.editNombreFemmesSet),
        nombreGarconsSensibilises = texte(binding.editNombreGarconsSet),
        nombreFillesSensibilisees = texte(binding.editNombreFillesSet),
        telephoneProducteurSensibilisation = texte(binding.editTelephoneSensibilisationSet),
        photoSensibilisation = photoBase64,

        etatSoumission = "Soumis",

        actionsRemediationStringify = BrouillonPfte.serialiserListe(actionsRemediation),
        raisonNonScolarisationStringify = BrouillonPfte.serialiserListe(raisonNonScolarisation),
        raisonPasExtraitStringify = BrouillonPfte.serialiserListe(raisonPasExtrait),
        situationsPfteStringify = BrouillonPfte.serialiserListe(situationsPfte),
        raisonTravailAbusStringify = BrouillonPfte.serialiserListe(raisonTravailAbus),
        mesuresEnfantStringify = BrouillonPfte.serialiserListe(mesuresEnfant),
        mesuresMenageStringify = BrouillonPfte.serialiserListe(mesuresMenage),
        mesuresCommunauteStringify = BrouillonPfte.serialiserListe(mesuresCommunaute),
        themesSensibilisationStringify = BrouillonPfte.serialiserListe(themes),
        outilsSensibilisationStringify = BrouillonPfte.serialiserListe(outils),

        isSynced = false,
        origin = "local",
        agentId = agentId.toString(),
        enfantNom = enfantRetenu?.libelle().orEmpty(),
        producteurNom = enfantRetenu?.producteurNom.orEmpty(),
        localiteNom = enfantRetenu?.localiteNom.orEmpty()
    )

    /** Affiche les informations manquantes. @return vrai si le suivi est incomplet. */
    private fun signalerManques(suivi: SuiviEnfantModel): Boolean {
        val manques = ControlesSuiviEnfant.manquants(suivi)
        if (manques.isEmpty()) return false

        Commons.showMessage(
            message = getString(R.string.champs_obligatoires_manquants) + "\n\n" +
                    manques.joinToString("\n") { "- " + getString(it.libelle) },
            context = this, finished = false, callback = { },
            positive = getString(R.string.ok)
        )
        return true
    }

    /**
     * Ouvre le recapitulatif.
     *
     * Le suivi n'est pas ecrit ici : il l'est apres relecture, dernier point ou l'agent peut
     * corriger - le serveur n'offrant aucun endpoint de relecture.
     */
    private fun enregistrer() {
        val suivi = construireSuivi()
        if (signalerManques(suivi)) return

        startActivity(
            Intent(this, SuiviEnfantPreviewActivity::class.java)
                .putExtra(SuiviEnfantPreviewActivity.EXTRA_SUIVI, suivi)
                .putExtra(SuiviEnfantPreviewActivity.EXTRA_BROUILLON, brouillonOuvert?.uid ?: 0)
        )
    }

    private fun mettreAuBrouillon() {
        val suivi = construireSuivi()
        suivi.etatSoumission = "Brouillon"

        BrouillonPfte.enregistrer(
            activite = this,
            type = BrouillonPfte.TYPE_SUIVI_ENFANT,
            modele = suivi,
            uidExistant = brouillonOuvert?.uid ?: 0,
            boutonBrouillon = binding.imageDraftBtn
        ) { finish() }
    }

    // ------------------------------------------------------------------ reprise d'un brouillon

    private fun rouvrirBrouillonEventuel() {
        val uid = intent.getIntExtra(EXTRA_BROUILLON, 0)
        if (uid == 0) return

        brouillonOuvert = BrouillonPfte.parId(this, uid)
        val suivi = BrouillonPfte.lire(brouillonOuvert, SuiviEnfantModel::class.java)
        if (suivi == null) {
            LogUtils.e("Brouillon de suivi enfant $uid illisible")
            return
        }
        restaurer(suivi)
    }

    private fun restaurer(suivi: SuiviEnfantModel) {
        // Les selections sont posees AVANT le rebranchement des listes, faute de quoi le composant
        // ne les afficherait pas.
        actionsRemediation.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.actionsRemediationStringify)) }
        raisonNonScolarisation.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.raisonNonScolarisationStringify)) }
        raisonPasExtrait.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.raisonPasExtraitStringify)) }
        situationsPfte.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.situationsPfteStringify)) }
        raisonTravailAbus.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.raisonTravailAbusStringify)) }
        mesuresEnfant.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.mesuresEnfantStringify)) }
        mesuresMenage.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.mesuresMenageStringify)) }
        mesuresCommunaute.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.mesuresCommunauteStringify)) }
        themes.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.themesSensibilisationStringify)) }
        outils.apply { clear(); addAll(SaisiePfte.deserialiser(suivi.outilsSensibilisationStringify)) }

        preparerRemediation()
        preparerReevaluation()
        preparerSensibilisation()

        // L'enfant est repose en premier : sa selection pre-remplit, et ce qui suit corrige.
        choisirEnfant(suivi.enfantId)
        forcerValeur(binding.selectEnfantSet,
            enfantsSuivables.firstOrNull { it.idServeur.toString() == suivi.enfantId }?.libelle())

        binding.editDateEnqueteSet.setText(suivi.dateEnquete)
        binding.editNomEnqueteurSet.setText(suivi.nomEnqueteur)
        binding.editNomSet.setText(suivi.nom)
        binding.editDateNaissanceSet.setText(suivi.dateNaissance)
        binding.editAutreLienParenteSet.setText(suivi.autreLienParente)
        binding.editAutreRaisonNeVitPasParentsSet.setText(suivi.autreRaisonNeVitPasParents)
        binding.editAutreRaisonNonScolarisationSet.setText(suivi.autreRaisonNonScolarisation)
        binding.editHeuresTravailSemaineSet.setText(suivi.heuresTravailSemaine)
        binding.editAutreRaisonTravailAbusSet.setText(suivi.autreRaisonTravailAbus)
        binding.editAutreMesureSet.setText(suivi.autreMesure)
        binding.editAutreThemeSet.setText(suivi.autreThemeSensibilisation)
        binding.editNombreHommesSet.setText(suivi.nombreHommesSensibilises)
        binding.editNombreFemmesSet.setText(suivi.nombreFemmesSensibilisees)
        binding.editNombreGarconsSet.setText(suivi.nombreGarconsSensibilises)
        binding.editNombreFillesSet.setText(suivi.nombreFillesSensibilisees)
        binding.editTelephoneSensibilisationSet.setText(suivi.telephoneProducteurSensibilisation)

        photoBase64 = suivi.photoSensibilisation.orEmpty()

        forcerValeur(binding.selectSexeSet, suivi.sexe)
        forcerValeur(binding.selectLienParenteSet, suivi.lienParente)
        forcerValeur(binding.selectRaisonNeVitPasParentsSet, suivi.raisonNeVitPasParents)
        forcerValeur(binding.selectSituationScolaireSet, suivi.situationScolaire)
        forcerValeur(binding.selectNiveauScolaireSet,
            SaisiePfte.libellePourClasse(this, suivi.niveauScolaire))
        forcerValeur(binding.selectExtraitNaissanceSet, suivi.extraitNaissance)
        // Le brouillon porte des codes : on retrouve le libelle a afficher.
        forcerValeur(binding.selectPresentDisponibleSet,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.PRESENT_DISPONIBLE, suivi.presentDisponible))
        forcerValeur(binding.selectRaisonAbsentSet,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.RAISONS_ABSENCE, suivi.raisonAbsent))
        forcerValeur(binding.selectJoursTravailSet,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.JOURS_TRAVAIL, suivi.joursTravail))
        forcerValeur(binding.selectHeuresJourneeSet,
            VocabulairesPfte.libellePourCode(VocabulairesPfte.HEURES_JOURNEE, suivi.heuresTravailJournee))

        // Regles que le repositionnement d'un choix multiple ne declenche pas.
        SaisiePfte.basculer(binding.blocAutreRaisonNonScolarisationSet,
            SaisiePfte.exigePrecision(raisonNonScolarisation))
        SaisiePfte.basculer(binding.blocRaisonTravailAbusSet, exigeRaisonTravail(situationsPfte))
        SaisiePfte.basculer(binding.blocAutreRaisonTravailAbusSet,
            SaisiePfte.exigePrecision(raisonTravailAbus))
        SaisiePfte.basculer(binding.blocAutreMesureSet,
            SaisiePfte.exigePrecision(mesuresEnfant, mesuresMenage, mesuresCommunaute))
        SaisiePfte.basculer(binding.blocAutreThemeSet, SaisiePfte.exigePrecision(themes))

        calculerTotalSensibilises()
    }

    // ------------------------------------------------------------------ petits outils

    private fun texte(champ: AppCompatEditText): String = champ.text?.toString()?.trim().orEmpty()

    private fun entier(champ: AppCompatEditText): Int = texte(champ).toIntOrNull() ?: 0

    /**
     * Valeur retenue dans une liste deroulante, ou chaine vide si aucune.
     *
     * La premiere ligne est toujours une invite : sans ce controle de position, une liste jamais
     * ouverte renverrait ce libelle comme s'il s'agissait d'une reponse.
     */
    private fun valeur(spinner: android.widget.Spinner): String =
        if (spinner.selectedItemPosition <= 0) ""
        else spinner.selectedItem?.toString()?.trim().orEmpty()

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

    private fun listeMultiple(composant: ci.progbandama.mobile.activities.forms.views.MultiSelectSpinner,
                              table: String, titre: String, selection: MutableList<String>,
                              surChoix: (MutableList<String>) -> Unit) {
        SaisiePfte.listeMultiple(
            activite = this,
            composant = composant,
            table = table,
            titre = titre,
            selectionCourante = selection
        ) { choix ->
            selection.clear()
            selection.addAll(choix)
            surChoix(choix)
        }
    }

    private fun choisirDate(champ: AppCompatEditText) {
        val calendrier = Calendar.getInstance()
        val selecteur = DatePickerDialog(
            this,
            { _, annee, mois, jour ->
                champ.setText(Commons.convertDate("$jour-${mois + 1}-$annee", false))
            },
            calendrier.get(Calendar.YEAR), calendrier.get(Calendar.MONTH),
            calendrier.get(Calendar.DAY_OF_MONTH)
        )
        // Ni la naissance ni la visite ne se situent dans le futur.
        selecteur.datePicker.maxDate = DateTime.now().millis
        selecteur.show()
    }

    private fun surSaisie(action: () -> Unit) = object : android.text.TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: android.text.Editable?) = action()
    }

    companion object {
        const val EXTRA_BROUILLON = "brouillon_uid"
        private const val DEMANDE_PHOTO = 4401
        private const val DEMANDE_GALERIE = 4402
        private const val AUTORITE_FICHIERS = "ci.progbandama.mobile.fileprovider"
    }
}
