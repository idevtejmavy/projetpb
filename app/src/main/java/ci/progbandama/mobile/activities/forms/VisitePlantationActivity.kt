package ci.progbandama.mobile.activities.forms

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import ci.progbandama.mobile.R
import ci.progbandama.mobile.activities.forms.views.MultiSelectSpinner
import ci.progbandama.mobile.activities.infospresenters.VisitePlantationPreviewActivity
import ci.progbandama.mobile.databinding.ActivityVisitePlantationBinding
import ci.progbandama.mobile.models.DataDraftedModel
import ci.progbandama.mobile.models.EnfantPlantationModel
import ci.progbandama.mobile.models.ProducteurModel
import ci.progbandama.mobile.models.VisitePlantationModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.repositories.datas.CommonData
import ci.progbandama.mobile.tools.BrouillonPfte
import ci.progbandama.mobile.tools.Commons
import ci.progbandama.mobile.tools.Constants
import ci.progbandama.mobile.tools.ControlesVisitePlantation
import ci.progbandama.mobile.tools.FichesEnfant
import ci.progbandama.mobile.tools.ReferentielsPfte
import ci.progbandama.mobile.tools.SaisiePfte
import ci.progbandama.mobile.tools.VocabulairesPfte
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.google.gson.reflect.TypeToken
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.Calendar

/**
 * Visite Plantation (PFTE) - lot 4.
 *
 * Proche de l'Enquete Menage, dont il reprend le socle et les conventions, avec trois differences
 * qui changent la conduite du formulaire :
 *
 *  - les fiches enfant sont GENEREES, pas ajoutees. Leur nombre suit l'effectif d'enfants de 5 a
 *    17 ans trouves sur la plantation, comme sur le web ou le bouton d'ajout existe mais reste
 *    masque en permanence ;
 *  - le nom et le titre du repondant vivent dans un meme bloc, ouvert seulement si le producteur
 *    n'est pas lui-meme le repondant. Sinon le serveur recalcule le nom et n'attend rien ;
 *  - chaque fiche porte trois indicateurs de temps de travail, absents de l'Enquete Menage. Ils
 *    sont enregistres sous forme de CODES et non de libelles - voir VocabulairesPfte.
 *
 * Il n'y a ni section Sensibilisation ni photo dans ce module.
 */
class VisitePlantationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVisitePlantationBinding
    private lateinit var fichesEnfant: FichesEnfant<EnfantPlantationModel>

    private val sectionRetenue = CommonData()
    private val localiteRetenue = CommonData()
    private val producteurRetenu = CommonData()

    private var producteursListe: MutableList<ProducteurModel> = mutableListOf()
    private var brouillonOuvert: DataDraftedModel? = null

    private val raisonsRefusRetenues = mutableListOf<String>()

    /** Etat des choix multiples d'une fiche, attache a sa vue. */
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
        binding = ActivityVisitePlantationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!referentielsPrets()) return

        preparerFichesEnfant()
        preparerIdentification()
        preparerFiltre()
        preparerPlantation()
        preparerBoutons()

        binding.editDateEnqueteVp.setText(
            DateTime.now().toString(DateTimeFormat.forPattern("dd-MM-yyyy"))
        )
        binding.editNomEnqueteurVp.setText(SPUtils.getInstance().getString(Constants.AGENT_NAME, ""))

        rouvrirBrouillonEventuel()
    }

    private fun referentielsPrets(): Boolean {
        val manquants = SaisiePfte.referentielsManquants(this)
        if (manquants.isEmpty()) return true

        LogUtils.e("Visite Plantation : referentiels manquants -> $manquants")
        Commons.showMessage(
            message = getString(R.string.referentiels_pfte_manquants),
            context = this, finished = true, callback = { finish() },
            positive = getString(R.string.ok)
        )
        return false
    }

    // ------------------------------------------------------------------ identification

    private fun preparerIdentification() {
        choisirSection()

        listeSimple(binding.selectJourVisiteVp, getString(R.string.jour_de_la_visite),
            VocabulairesPfte.JOURS_VISITE) { }

        binding.editDateEnqueteVp.setOnClickListener { choisirDate(binding.editDateEnqueteVp) }
        binding.btnCapturerPositionVp.setOnClickListener { capturerPosition() }
    }

    private fun choisirSection(sectionCourante: String? = null, localiteCourante: String? = null,
                               producteurCourant: String? = null) {
        val sections = ProgBandRoomDatabase.getDatabase(applicationContext)?.sectionsDao()
            ?.getAll(agentID = agentId.toString()) ?: mutableListOf()

        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.choix_de_la_section),
            message = getString(R.string.la_liste_des_sections_semble_vide_veuillez_proc_der_la_synchronisation_des_donn_es_svp),
            isEmpty = sections.isEmpty(),
            currentVal = sections.firstOrNull { it.id.toString() == sectionCourante }?.libelle,
            spinner = binding.selectSectionVp,
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

        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.choix_de_la_localit),
            message = getString(R.string.la_liste_des_localit_s_semble_vide_veuillez_proc_der_la_synchronisation_des_donn_es_svp),
            isEmpty = localites.isEmpty(),
            currentVal = localites.firstOrNull { it.id.toString() == localiteCourante }?.nom,
            spinner = binding.selectLocaliteVp,
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
            spinner = binding.selectProducteurVp,
            listIem = producteursListe.map { libelle(it) },
            onChanged = { position ->
                val producteur = producteursListe[position]
                producteurRetenu.nom = libelle(producteur)
                producteurRetenu.id =
                    if (producteur.isSynced == true) producteur.id ?: 0 else producteur.uid

                // Le sexe et le code viennent du producteur : l'agent ne les saisit pas.
                binding.editSexeProducteurVp.setText(producteur.sexeProducteur.orEmpty())
                binding.editCodeProducteurVp.setText(
                    producteur.codeProdApp.orEmpty().ifBlank { producteur.codeProd.orEmpty() }
                )
                appliquerRepondant()
            },
            onSelected = { _, _ -> }
        )
    }

    private fun capturerPosition() {
        if (!autorisationLocalisationAccordee()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                DEMANDE_AUTORISATION_POSITION
            )
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

        binding.editLatitudeVp.setText(latitude)
        binding.editLongitudeVp.setText(longitude)
        binding.editAltitudeVp.setText(prefs.getString(Constants.PREFS_COMMON_ALT, ""))
        binding.editPrecisionGpsVp.setText(prefs.getString(Constants.PREFS_COMMON_PRECISION, ""))
    }

    private fun autorisationLocalisationAccordee(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

    /** Le fournisseur reseau vaut le GPS ici : la visite releve un lieu, pas une limite de parcelle. */
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Le code de retour des reglages n'est pas renseigne : c'est l'etat du service qui fait foi.
        if (requestCode == DEMANDE_ACTIVATION_POSITION) capturerPosition()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != DEMANDE_AUTORISATION_POSITION) return
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            capturerPosition()
        }
    }

    // ------------------------------------------------------------------ filtre et consentement

    private fun preparerFiltre() {
        listeSimple(binding.selectEstRepondantVp, getString(R.string.le_producteur_est_il_le_repondant),
            VocabulairesPfte.OUI_NON) { appliquerRepondant() }

        listeSimple(binding.selectTitreRepondantVp, getString(R.string.titre_du_repondant),
            VocabulairesPfte.TITRES_REPONDANT) { }

        listeSimple(binding.selectProducteurDisponibleVp, getString(R.string.le_producteur_est_il_disponible),
            VocabulairesPfte.OUI_NON) { appliquerDisponibilite() }

        listeSimple(binding.selectRaisonIndisponibiliteVp, getString(R.string.raison_de_l_indisponibilite),
            VocabulairesPfte.RAISONS_INDISPONIBILITE) { appliquerRaisonIndisponibilite() }

        listeSimple(binding.selectConsentementVp, getString(R.string.consentement_du_producteur),
            VocabulairesPfte.OUI_NON) { appliquerConsentement() }

        Commons.setupItemMultiSelection(
            context = this,
            selectItemMulti = binding.selectRaisonRefusVp,
            title = getString(R.string.raison_s_du_refus),
            itemList = VocabulairesPfte.RAISONS_REFUS.map { CommonData(nom = it) },
            currentList = raisonsRefusRetenues
        ) { choix ->
            raisonsRefusRetenues.clear()
            raisonsRefusRetenues.addAll(choix)
            SaisiePfte.basculer(binding.blocAutreRaisonRefusVp, SaisiePfte.exigePrecision(choix))
        }

        binding.editDatePlanificationVp.setOnClickListener {
            choisirDate(binding.editDatePlanificationVp, futureAutorisee = true)
        }
    }

    /**
     * Nom et titre du repondant.
     *
     * Le bloc entier ne s'ouvre que si quelqu'un d'autre repond. Quand le producteur repond
     * lui-meme, le back-office recalcule son nom : ne rien envoyer est donc correct, et le bloc
     * masque est vide comme tous les autres.
     */
    private fun appliquerRepondant() {
        val estRepondant = valeur(binding.selectEstRepondantVp) == "Oui"
        SaisiePfte.basculer(binding.blocRepondantVp, !estRepondant)
    }

    private fun appliquerDisponibilite() {
        val reponse = valeur(binding.selectProducteurDisponibleVp)
        val disponible = reponse == "Oui"

        SaisiePfte.basculer(binding.blocIndisponibleVp, !disponible)
        SaisiePfte.basculer(binding.blocConsentementVp, disponible)

        // Producteur indisponible : la visite s'arrete, mais elle part avec son motif.
        binding.labelFinEnqueteVp.visibility =
            if (!disponible && reponse.isNotBlank()) View.VISIBLE else View.GONE
        if (!disponible) masquerSuite()
        appliquerConsentement()
    }

    private fun appliquerRaisonIndisponibilite() {
        val raison = valeur(binding.selectRaisonIndisponibiliteVp)

        SaisiePfte.basculer(binding.blocDatePlanificationVp, raison == VocabulairesPfte.ABSENCE_TEMPORAIRE)
        SaisiePfte.basculer(binding.blocRaisonRefusVp, raison == VocabulairesPfte.REFUS)

        if (raison != VocabulairesPfte.REFUS) {
            raisonsRefusRetenues.clear()
            SaisiePfte.basculer(binding.blocAutreRaisonRefusVp, false)
        }
    }

    private fun appliquerConsentement() {
        if (valeur(binding.selectProducteurDisponibleVp) != "Oui") return

        val reponse = valeur(binding.selectConsentementVp)
        val consent = reponse == "Oui"

        binding.labelFinEnqueteVp.visibility =
            if (!consent && reponse.isNotBlank()) View.VISIBLE else View.GONE
        if (consent) {
            SaisiePfte.basculer(binding.blocSuiteFormulaireVp, true)
            appliquerNombreEnfants()
        } else {
            masquerSuite()
        }
    }

    /** Referme le corps du formulaire, et vide ce qui aurait ete saisi avant un retour en arriere. */
    private fun masquerSuite() {
        SaisiePfte.basculer(binding.blocSuiteFormulaireVp, false)
        fichesEnfant.viderTout()
    }

    // ------------------------------------------------------------------ caracteristiques de la plantation

    private fun preparerPlantation() {
        listeSimple(binding.selectPermanentsMoins18Vp, getString(R.string.manoeuvres_permanents_moins_18),
            VocabulairesPfte.OUI_NON) { }
        listeSimple(binding.selectJournaliersMoins18Vp, getString(R.string.manoeuvres_journaliers_moins_18),
            VocabulairesPfte.OUI_NON) { }

        // L'effectif d'enfants trouves pilote directement le nombre de fiches.
        binding.editNombreEnfants5a17Vp.addTextChangedListener(surSaisie { appliquerNombreEnfants() })
    }

    /**
     * Aligne les fiches sur l'effectif declare.
     *
     * L'agent ne cree pas les fiches : leur nombre decoule de ce qu'il a trouve sur la plantation.
     * Reduire l'effectif retire les dernieres fiches, jamais les premieres - les saisies deja
     * faites sont ainsi preservees.
     */
    private fun appliquerNombreEnfants() {
        val trouves = entier(binding.editNombreEnfants5a17Vp)
        fichesEnfant.synchroniserDepuisNombre(trouves)
        binding.cardEnfantsVp.visibility = if (trouves > 0) View.VISIBLE else View.GONE
    }

    // ------------------------------------------------------------------ fiches enfant

    private fun preparerFichesEnfant() {
        fichesEnfant = FichesEnfant(
            contexte = this,
            conteneur = binding.conteneurFichesEnfantVp,
            layoutFiche = R.layout.fiche_enfant_plantation,
            idNumero = R.id.labelNumeroEnfantVp,
            // Pas de bouton de retrait : l'effectif declare commande le nombre de fiches,
            // retirer une fiche isolee le contredirait.
            surCreation = { vue, _ -> cablerFiche(vue) },
            lecture = { vue, _ -> lireFiche(vue) },
            ecriture = { vue, modele -> ecrireFiche(vue, modele) }
        )
    }

    /** Branche les listes et les neuf regles d'une fiche. */
    private fun cablerFiche(vue: View) {
        val etat = (vue.getTag(R.id.tagSelectionsFiche) as? SelectionsFiche)
            ?: SelectionsFiche().also { vue.setTag(R.id.tagSelectionsFiche, it) }

        val dateNaissance = vue.findViewById<AppCompatEditText>(R.id.editDateNaissanceEnfantVp)
        dateNaissance.setOnClickListener { choisirDate(dateNaissance) }

        listeSimple(spinnerDe(vue, R.id.selectSexeEnfantVp), getString(R.string.sexe),
            VocabulairesPfte.SEXE_ENFANT) { }

        listeSimple(spinnerDe(vue, R.id.selectLienParenteEnfantVp), getString(R.string.lien_de_parente),
            VocabulairesPfte.LIENS_PARENTE) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreLienParenteEnfantVp), choix == "Autre")
        }

        listeSimple(spinnerDe(vue, R.id.selectRaisonNeVitPasParentsEnfantVp),
            getString(R.string.raison_ne_vit_pas_avec_ses_parents),
            VocabulairesPfte.RAISONS_NE_VIT_PAS_PARENTS) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNeVitPasParentsEnfantVp), choix == "Autres")
        }

        listeSimple(spinnerDe(vue, R.id.selectSituationScolaireEnfantVp), getString(R.string.situation_scolaire),
            VocabulairesPfte.SITUATIONS_SCOLAIRES) { choix ->
            SaisiePfte.basculer(
                vue.findViewById(R.id.blocNiveauScolaireEnfantVp),
                choix == VocabulairesPfte.SCOLARISE || choix == VocabulairesPfte.DESCOLARISE
            )
            SaisiePfte.basculer(
                vue.findViewById(R.id.blocRaisonNonScolarisationEnfantVp),
                choix == VocabulairesPfte.DESCOLARISE || choix == VocabulairesPfte.JAMAIS_SCOLARISE
            )
            if (choix == VocabulairesPfte.SCOLARISE) {
                etat.raisonNonScolarisation.clear()
                SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNonScolarisationEnfantVp), false)
            }
        }

        // Les classes portent leur niveau en prefixe : la liste deroulante ne sait pas grouper.
        Commons.setListenerForSpinner(
            this,
            title = getString(R.string.classe),
            message = getString(R.string.referentiels_pfte_manquants),
            isEmpty = false,
            spinner = spinnerDe(vue, R.id.selectNiveauScolaireEnfantVp),
            listIem = SaisiePfte.classesAffichage(this),
            onChanged = { }, onSelected = { _, _ -> }
        )

        listeSimple(spinnerDe(vue, R.id.selectExtraitNaissanceEnfantVp),
            getString(R.string.dispose_d_un_extrait_de_naissance), VocabulairesPfte.OUI_NON) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocRaisonPasExtraitEnfantVp), choix == "Non")
            if (choix != "Non") etat.raisonPasExtrait.clear()
        }

        // Les deux indicateurs a codes : l'agent voit un libelle, le serveur recoit un chiffre.
        listeSimple(spinnerDe(vue, R.id.selectJoursTravailEnfantVp),
            getString(R.string.jours_travailles_par_semaine),
            VocabulairesPfte.libelles(VocabulairesPfte.JOURS_TRAVAIL)) { }
        listeSimple(spinnerDe(vue, R.id.selectHeuresJourneeEnfantVp),
            getString(R.string.heures_journee_la_plus_chargee),
            VocabulairesPfte.libelles(VocabulairesPfte.HEURES_JOURNEE)) { }

        listeMultipleFiche(vue, R.id.selectRaisonNonScolarisationEnfantVp,
            ReferentielsPfte.RAISONS_NON_SCOLARISATION, getString(R.string.raisons_de_non_scolarisation),
            etat.raisonNonScolarisation) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNonScolarisationEnfantVp),
                SaisiePfte.exigePrecision(choix))
        }

        listeMultipleFiche(vue, R.id.selectRaisonPasExtraitEnfantVp,
            ReferentielsPfte.RAISONS_PAS_EXTRAIT, getString(R.string.raisons_absence_extrait),
            etat.raisonPasExtrait) { }

        listeMultipleFiche(vue, R.id.selectSituationsPfteEnfantVp,
            ReferentielsPfte.SITUATIONS_PFTE, getString(R.string.situations_pfte_constatees),
            etat.situationsPfte) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocRaisonTravailAbusEnfantVp), exigeRaisonTravail(choix))
            if (!exigeRaisonTravail(choix)) etat.raisonTravailAbus.clear()
        }

        listeMultipleFiche(vue, R.id.selectRaisonTravailAbusEnfantVp,
            ReferentielsPfte.RAISONS_TRAVAIL_ABUS, getString(R.string.raisons_du_travail_ou_de_l_abus),
            etat.raisonTravailAbus) { choix ->
            SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonTravailAbusEnfantVp),
                SaisiePfte.exigePrecision(choix))
        }

        // Le champ de precision est partage par les trois listes de mesures.
        val surMesures = { _: MutableList<String> ->
            SaisiePfte.basculer(
                vue.findViewById(R.id.blocAutreMesureEnfantVp),
                SaisiePfte.exigePrecision(etat.mesuresEnfant, etat.mesuresMenage, etat.mesuresCommunaute)
            )
        }
        listeMultipleFiche(vue, R.id.selectMesuresEnfantEnfantVp, ReferentielsPfte.MESURES_ENFANT,
            getString(R.string.mesures_au_niveau_de_l_enfant), etat.mesuresEnfant, surMesures)
        listeMultipleFiche(vue, R.id.selectMesuresMenageEnfantVp, ReferentielsPfte.MESURES_MENAGE,
            getString(R.string.mesures_au_niveau_du_menage), etat.mesuresMenage, surMesures)
        listeMultipleFiche(vue, R.id.selectMesuresCommunauteEnfantVp, ReferentielsPfte.MESURES_COMMUNAUTE,
            getString(R.string.mesures_au_niveau_de_la_communaute), etat.mesuresCommunaute, surMesures)
    }

    /** Une situation constatee appelle une raison, sauf si la seule retenue est "Aucune". */
    private fun exigeRaisonTravail(situations: List<String>): Boolean =
        situations.isNotEmpty() && situations.any { !it.contains("Aucune", ignoreCase = true) }

    private fun lireFiche(vue: View): EnfantPlantationModel? {
        val nom = texte(vue, R.id.editNomEnfantVp)
        val naissance = texte(vue, R.id.editDateNaissanceEnfantVp)
        // Une fiche generee mais laissee vide n'est pas un enfant : elle est ignoree.
        if (nom.isBlank() && naissance.isBlank()) return null

        val etat = selections(vue)
        return EnfantPlantationModel(
            nom = nom,
            dateNaissance = naissance,
            sexe = valeur(spinnerDe(vue, R.id.selectSexeEnfantVp)),
            lienParente = valeur(spinnerDe(vue, R.id.selectLienParenteEnfantVp)),
            autreLienParente = texte(vue, R.id.editAutreLienParenteEnfantVp),
            raisonNeVitPasParents = valeur(spinnerDe(vue, R.id.selectRaisonNeVitPasParentsEnfantVp)),
            autreRaisonNeVitPasParents = texte(vue, R.id.editAutreRaisonNeVitPasParentsEnfantVp),
            situationScolaire = valeur(spinnerDe(vue, R.id.selectSituationScolaireEnfantVp)),
            niveauScolaire = SaisiePfte.classeDepuisLibelle(valeur(spinnerDe(vue, R.id.selectNiveauScolaireEnfantVp))),
            autreRaisonNonScolarisation = texte(vue, R.id.editAutreRaisonNonScolarisationEnfantVp),
            extraitNaissance = valeur(spinnerDe(vue, R.id.selectExtraitNaissanceEnfantVp)),
            heuresTravailSemaine = texte(vue, R.id.editHeuresTravailSemaineEnfantVp),
            // Codes et non libelles : c'est ce que le back-office enregistre.
            joursTravail = VocabulairesPfte.codePour(
                VocabulairesPfte.JOURS_TRAVAIL, valeur(spinnerDe(vue, R.id.selectJoursTravailEnfantVp))),
            heuresTravailJournee = VocabulairesPfte.codePour(
                VocabulairesPfte.HEURES_JOURNEE, valeur(spinnerDe(vue, R.id.selectHeuresJourneeEnfantVp))),
            autreRaisonTravailAbus = texte(vue, R.id.editAutreRaisonTravailAbusEnfantVp),
            autreMesure = texte(vue, R.id.editAutreMesureEnfantVp),
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
     * Meme ordre que pour l'Enquete Menage : selections d'abord, cablage ensuite, listes simples en
     * dernier - rebrancher apres remettrait les listes simples a zero.
     */
    private fun ecrireFiche(vue: View, modele: EnfantPlantationModel) {
        val etat = selections(vue)
        etat.raisonNonScolarisation.apply { clear(); addAll(modele.raisonNonScolarisation) }
        etat.raisonPasExtrait.apply { clear(); addAll(modele.raisonPasExtrait) }
        etat.situationsPfte.apply { clear(); addAll(modele.situationsPfte) }
        etat.raisonTravailAbus.apply { clear(); addAll(modele.raisonTravailAbus) }
        etat.mesuresEnfant.apply { clear(); addAll(modele.mesuresEnfant) }
        etat.mesuresMenage.apply { clear(); addAll(modele.mesuresMenage) }
        etat.mesuresCommunaute.apply { clear(); addAll(modele.mesuresCommunaute) }

        cablerFiche(vue)

        vue.findViewById<AppCompatEditText>(R.id.editNomEnfantVp).setText(modele.nom)
        vue.findViewById<AppCompatEditText>(R.id.editDateNaissanceEnfantVp).setText(modele.dateNaissance)
        vue.findViewById<AppCompatEditText>(R.id.editAutreLienParenteEnfantVp).setText(modele.autreLienParente)
        vue.findViewById<AppCompatEditText>(R.id.editAutreRaisonNeVitPasParentsEnfantVp)
            .setText(modele.autreRaisonNeVitPasParents)
        vue.findViewById<AppCompatEditText>(R.id.editAutreRaisonNonScolarisationEnfantVp)
            .setText(modele.autreRaisonNonScolarisation)
        vue.findViewById<AppCompatEditText>(R.id.editHeuresTravailSemaineEnfantVp)
            .setText(modele.heuresTravailSemaine)
        vue.findViewById<AppCompatEditText>(R.id.editAutreRaisonTravailAbusEnfantVp)
            .setText(modele.autreRaisonTravailAbus)
        vue.findViewById<AppCompatEditText>(R.id.editAutreMesureEnfantVp).setText(modele.autreMesure)

        forcerValeur(spinnerDe(vue, R.id.selectSexeEnfantVp), modele.sexe)
        forcerValeur(spinnerDe(vue, R.id.selectLienParenteEnfantVp), modele.lienParente)
        forcerValeur(spinnerDe(vue, R.id.selectRaisonNeVitPasParentsEnfantVp), modele.raisonNeVitPasParents)
        forcerValeur(spinnerDe(vue, R.id.selectSituationScolaireEnfantVp), modele.situationScolaire)
        forcerValeur(spinnerDe(vue, R.id.selectNiveauScolaireEnfantVp),
            SaisiePfte.libellePourClasse(this, modele.niveauScolaire))
        forcerValeur(spinnerDe(vue, R.id.selectExtraitNaissanceEnfantVp), modele.extraitNaissance)
        // Le brouillon porte des codes : on retrouve le libelle a afficher.
        forcerValeur(spinnerDe(vue, R.id.selectJoursTravailEnfantVp),
            VocabulairesPfte.libellePourCode(VocabulairesPfte.JOURS_TRAVAIL, modele.joursTravail))
        forcerValeur(spinnerDe(vue, R.id.selectHeuresJourneeEnfantVp),
            VocabulairesPfte.libellePourCode(VocabulairesPfte.HEURES_JOURNEE, modele.heuresTravailJournee))

        rejouerReglesMultiples(vue, etat)
    }

    /** Regles que le repositionnement d'un choix multiple ne declenche pas. */
    private fun rejouerReglesMultiples(vue: View, etat: SelectionsFiche) {
        SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonNonScolarisationEnfantVp),
            SaisiePfte.exigePrecision(etat.raisonNonScolarisation))
        SaisiePfte.basculer(vue.findViewById(R.id.blocRaisonTravailAbusEnfantVp),
            exigeRaisonTravail(etat.situationsPfte))
        SaisiePfte.basculer(vue.findViewById(R.id.blocAutreRaisonTravailAbusEnfantVp),
            SaisiePfte.exigePrecision(etat.raisonTravailAbus))
        SaisiePfte.basculer(vue.findViewById(R.id.blocAutreMesureEnfantVp),
            SaisiePfte.exigePrecision(etat.mesuresEnfant, etat.mesuresMenage, etat.mesuresCommunaute))
    }

    // ------------------------------------------------------------------ enregistrement

    private fun preparerBoutons() {
        binding.clickCloseBtn.setOnClickListener { finish() }
        binding.imageDraftBtn.setOnClickListener { mettreAuBrouillon() }
        binding.btnBrouillonVp.setOnClickListener { mettreAuBrouillon() }
        binding.btnEnregistrerVp.setOnClickListener { enregistrer() }
    }

    private fun construireVisite(): VisitePlantationModel {
        val disponible = valeur(binding.selectProducteurDisponibleVp) == "Oui"

        return VisitePlantationModel(
            uid = 0,
            userid = agentId,
            sectionId = sectionRetenue.id?.toString().orEmpty(),
            localiteId = localiteRetenue.id?.toString().orEmpty(),
            producteurId = producteurRetenu.id?.toString().orEmpty(),
            dateEnquete = texte(binding.editDateEnqueteVp),
            jourVisite = valeur(binding.selectJourVisiteVp),
            nomEnqueteur = texte(binding.editNomEnqueteurVp),
            latitude = texte(binding.editLatitudeVp),
            longitude = texte(binding.editLongitudeVp),
            altitude = texte(binding.editAltitudeVp),
            precisionGps = texte(binding.editPrecisionGpsVp),

            estProducteurRepondant = valeur(binding.selectEstRepondantVp),
            nomRepondant = texte(binding.editNomRepondantVp),
            titreRepondant = valeur(binding.selectTitreRepondantVp),
            producteurDisponible = valeur(binding.selectProducteurDisponibleVp),
            // Le serveur force cette valeur quand le producteur est disponible : on l'ecrit aussi,
            // pour que la visite conservee sur l'appareil dise la meme chose que celle en base.
            raisonIndisponibilite = if (disponible) VocabulairesPfte.SANS_OBJET
            else valeur(binding.selectRaisonIndisponibiliteVp),
            datePlanification = texte(binding.editDatePlanificationVp),
            autreRaisonRefus = texte(binding.editAutreRaisonRefusVp),
            consentement = valeur(binding.selectConsentementVp),

            superficiePlantation = texte(binding.editSuperficieVp),
            nombreManoeuvresPermanents = texte(binding.editManoeuvresPermanentsVp),
            manoeuvresPermanentsMoins18 = valeur(binding.selectPermanentsMoins18Vp),
            nombreManoeuvresJournaliers = texte(binding.editManoeuvresJournaliersVp),
            manoeuvresJournaliersMoins18 = valeur(binding.selectJournaliersMoins18Vp),
            nombreEnfants0a4 = texte(binding.editNombreEnfants0a4Vp),
            nombreEnfants5a17 = texte(binding.editNombreEnfants5a17Vp),
            nombrePersonnesTrouvees = texte(binding.editNombrePersonnesVp),

            etatSoumission = "Soumis",

            raisonRefusStringify = BrouillonPfte.serialiserListe(raisonsRefusRetenues),
            enfantsStringify = BrouillonPfte.serialiserListe(fichesEnfant.collecter()),

            isSynced = false,
            origin = "local",
            agentId = agentId.toString(),
            sectionNom = sectionRetenue.nom.orEmpty(),
            localiteNom = localiteRetenue.nom.orEmpty(),
            producteurNom = producteurRetenu.nom.orEmpty()
        )
    }

    /** Affiche les informations manquantes. @return vrai si la visite est incomplete. */
    private fun signalerManques(visite: VisitePlantationModel): Boolean {
        val manques = ControlesVisitePlantation.manquants(visite, fichesEnfant.collecter())
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
     * La visite n'est pas ecrite ici : elle l'est apres relecture. C'est le dernier point ou
     * l'agent peut corriger, le serveur n'offrant aucun endpoint de relecture.
     */
    private fun enregistrer() {
        val visite = construireVisite()
        if (signalerManques(visite)) return

        startActivity(
            Intent(this, VisitePlantationPreviewActivity::class.java)
                .putExtra(VisitePlantationPreviewActivity.EXTRA_VISITE, visite)
                .putExtra(VisitePlantationPreviewActivity.EXTRA_BROUILLON, brouillonOuvert?.uid ?: 0)
        )
    }

    private fun mettreAuBrouillon() {
        val visite = construireVisite()
        visite.etatSoumission = "Brouillon"

        BrouillonPfte.enregistrer(
            activite = this,
            type = BrouillonPfte.TYPE_VISITE_PLANTATION,
            modele = visite,
            uidExistant = brouillonOuvert?.uid ?: 0,
            boutonBrouillon = binding.imageDraftBtn
        ) { finish() }
    }

    // ------------------------------------------------------------------ reprise d'un brouillon

    private fun rouvrirBrouillonEventuel() {
        val uid = intent.getIntExtra(EXTRA_BROUILLON, 0)
        if (uid == 0) return

        brouillonOuvert = BrouillonPfte.parId(this, uid)
        val visite = BrouillonPfte.lire(brouillonOuvert, VisitePlantationModel::class.java)
        if (visite == null) {
            LogUtils.e("Brouillon de visite plantation $uid illisible")
            return
        }
        restaurer(visite)
    }

    private fun restaurer(visite: VisitePlantationModel) {
        choisirSection(visite.sectionId, visite.localiteId, visite.producteurId)

        binding.editDateEnqueteVp.setText(visite.dateEnquete)
        binding.editNomEnqueteurVp.setText(visite.nomEnqueteur)
        binding.editLatitudeVp.setText(visite.latitude)
        binding.editLongitudeVp.setText(visite.longitude)
        binding.editAltitudeVp.setText(visite.altitude)
        binding.editPrecisionGpsVp.setText(visite.precisionGps)

        binding.editNomRepondantVp.setText(visite.nomRepondant)
        binding.editDatePlanificationVp.setText(visite.datePlanification)
        binding.editAutreRaisonRefusVp.setText(visite.autreRaisonRefus)

        binding.editSuperficieVp.setText(visite.superficiePlantation)
        binding.editManoeuvresPermanentsVp.setText(visite.nombreManoeuvresPermanents)
        binding.editManoeuvresJournaliersVp.setText(visite.nombreManoeuvresJournaliers)
        binding.editNombreEnfants0a4Vp.setText(visite.nombreEnfants0a4)
        binding.editNombrePersonnesVp.setText(visite.nombrePersonnesTrouvees)

        raisonsRefusRetenues.apply { clear(); addAll(SaisiePfte.deserialiser(visite.raisonRefusStringify)) }

        // La liste est rebranchee pour afficher la selection restauree.
        preparerFiltre()

        forcerValeur(binding.selectJourVisiteVp, visite.jourVisite)
        forcerValeur(binding.selectPermanentsMoins18Vp, visite.manoeuvresPermanentsMoins18)
        forcerValeur(binding.selectJournaliersMoins18Vp, visite.manoeuvresJournaliersMoins18)

        forcerValeur(binding.selectEstRepondantVp, visite.estProducteurRepondant)
        forcerValeur(binding.selectTitreRepondantVp, visite.titreRepondant)
        forcerValeur(binding.selectProducteurDisponibleVp, visite.producteurDisponible)
        forcerValeur(binding.selectRaisonIndisponibiliteVp, visite.raisonIndisponibilite)
        forcerValeur(binding.selectConsentementVp, visite.consentement)

        SaisiePfte.basculer(binding.blocAutreRaisonRefusVp,
            SaisiePfte.exigePrecision(raisonsRefusRetenues))
        binding.editAutreRaisonRefusVp.setText(visite.autreRaisonRefus)

        // L'effectif est pose en dernier : il declenche la generation des fiches, que la
        // restauration remplit ensuite.
        binding.editNombreEnfants5a17Vp.setText(visite.nombreEnfants5a17)
        fichesEnfant.restaurer(
            BrouillonPfte.deserialiserListe(
                visite.enfantsStringify, object : TypeToken<MutableList<EnfantPlantationModel>>() {}
            )
        )

        appliquerRepondant()
        appliquerDisponibilite()
        appliquerRaisonIndisponibilite()
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
        private const val DEMANDE_ACTIVATION_POSITION = 4302
        private const val DEMANDE_AUTORISATION_POSITION = 4303
    }
}
