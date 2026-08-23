package ci.progbandama.mobile.tools

import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.SuiviEnfantModel
import com.google.gson.reflect.TypeToken

/**
 * Controles de saisie du Suivi Enfant Travailleur - lot 5.
 *
 * Comme l'Enquete Menage, l'endpoint mobile de ce module ne valide rien : ces controles sont le
 * seul filet avant l'enregistrement.
 *
 * Ils sont plus simples que ceux des deux autres modules : ce formulaire n'a ni filtre ni
 * consentement, donc aucun chemin d'arret anticipe. Tout ce qu'il demande est exige, a trois
 * conditions pres - le motif d'absence, les precisions "Autre", et les raisons de non-scolarisation.
 *
 * Une exception a la regle du back-office est assumee ici, et documentee : voir autreMesure.
 */
object ControlesSuiviEnfant {

    /**
     * Information manquante.
     *
     * @param libelle identifiant de la chaine decrivant le champ.
     */
    data class Manque(val libelle: Int)

    private fun vide(valeur: String?) = valeur.isNullOrBlank()

    private fun liste(stringify: String?): List<String> =
        BrouillonPfte.deserialiserListe(stringify, object : TypeToken<MutableList<String>>() {})

    fun manquants(suivi: SuiviEnfantModel): List<Manque> {
        val manques = mutableListOf<Manque>()
        fun exiger(condition: Boolean, libelle: Int) { if (condition) manques.add(Manque(libelle)) }

        // ------------------------------------------------------------ identification
        exiger(vide(suivi.enfantId) || suivi.enfantId == "0", R.string.enfant_suivi)
        exiger(vide(suivi.dateEnquete), R.string.date_de_la_visite_de_suivi)

        // ------------------------------------------------------------ suivi depuis la derniere visite
        exiger(liste(suivi.actionsRemediationStringify).isEmpty(), R.string.actions_de_remediation)

        // ------------------------------------------------------------ reevaluation
        exiger(vide(suivi.nom), R.string.nom)
        exiger(vide(suivi.dateNaissance), R.string.date_de_naissance)
        exiger(vide(suivi.sexe), R.string.sexe)

        exiger(vide(suivi.lienParente), R.string.lien_de_parente)
        exiger(suivi.lienParente == AUTRE && vide(suivi.autreLienParente),
            R.string.autre_lien_de_parente)

        exiger(vide(suivi.raisonNeVitPasParents), R.string.raison_ne_vit_pas_avec_ses_parents)
        exiger(suivi.raisonNeVitPasParents == AUTRES && vide(suivi.autreRaisonNeVitPasParents),
            R.string.autre_raison)

        exiger(vide(suivi.situationScolaire), R.string.situation_scolaire)
        val scolarise = suivi.situationScolaire == VocabulairesPfte.SCOLARISE
        val descolarise = suivi.situationScolaire == VocabulairesPfte.DESCOLARISE
        val jamaisScolarise = suivi.situationScolaire == VocabulairesPfte.JAMAIS_SCOLARISE

        exiger((scolarise || descolarise) && vide(suivi.niveauScolaire), R.string.classe)

        val raisonsNonScolarisation = liste(suivi.raisonNonScolarisationStringify)
        exiger((descolarise || jamaisScolarise) && raisonsNonScolarisation.isEmpty(),
            R.string.raisons_de_non_scolarisation)
        exiger(SaisiePfte.exigePrecision(raisonsNonScolarisation) &&
                vide(suivi.autreRaisonNonScolarisation), R.string.autre_raison_de_non_scolarisation)

        exiger(vide(suivi.extraitNaissance), R.string.dispose_d_un_extrait_de_naissance)
        exiger(suivi.extraitNaissance == NON && liste(suivi.raisonPasExtraitStringify).isEmpty(),
            R.string.raisons_absence_extrait)

        // ------------------------------------------------------------ presence et temps de travail
        exiger(vide(suivi.presentDisponible), R.string.present_et_disponible)
        // Le motif n'est demande que si l'enfant etait absent. Le code "0" vaut "Non" : c'est une
        // reponse, pas une absence de saisie.
        exiger(suivi.presentDisponible == VocabulairesPfte.ABSENT && vide(suivi.raisonAbsent),
            R.string.raison_de_l_absence)

        exiger(vide(suivi.heuresTravailSemaine), R.string.heures_de_travail_par_semaine)
        exiger(vide(suivi.joursTravail), R.string.jours_travailles_par_semaine)
        exiger(vide(suivi.heuresTravailJournee), R.string.heures_journee_la_plus_chargee)

        // ------------------------------------------------------------ situations et mesures
        val situations = liste(suivi.situationsPfteStringify)
        exiger(situations.isEmpty(), R.string.situations_pfte_constatees)

        // Une situation constatee appelle une raison, sauf si la seule retenue est "Aucune".
        val exigeRaison = situations.isNotEmpty() &&
                situations.any { !it.contains("Aucune", ignoreCase = true) }
        val raisonsTravail = liste(suivi.raisonTravailAbusStringify)
        exiger(exigeRaison && raisonsTravail.isEmpty(), R.string.raisons_du_travail_ou_de_l_abus)
        exiger(SaisiePfte.exigePrecision(raisonsTravail) && vide(suivi.autreRaisonTravailAbus),
            R.string.autre_raison)

        val mesuresEnfant = liste(suivi.mesuresEnfantStringify)
        val mesuresMenage = liste(suivi.mesuresMenageStringify)
        val mesuresCommunaute = liste(suivi.mesuresCommunauteStringify)
        exiger(mesuresEnfant.isEmpty(), R.string.mesures_au_niveau_de_l_enfant)
        exiger(mesuresMenage.isEmpty(), R.string.mesures_au_niveau_du_menage)
        exiger(mesuresCommunaute.isEmpty(), R.string.mesures_au_niveau_de_la_communaute)

        // ECART ASSUME avec le back-office.
        //
        // Ses regles marquent autreMesure comme obligatoire sans condition, alors que son propre
        // formulaire n'affiche le champ que si l'une des trois listes de mesures contient un
        // libelle "Autre" ou "preciser" - et que son toggle() vide les champs masques. Un
        // enregistrement web ordinaire ne poste donc pas ce champ, et la regle ne peut etre
        // satisfaite : elle est contredite par le controle conditionnel qui la suit dans le meme
        // controleur. Le mobile applique la regle conditionnelle, la seule qui soit tenable.
        exiger(
            SaisiePfte.exigePrecision(mesuresEnfant, mesuresMenage, mesuresCommunaute) &&
                    vide(suivi.autreMesure),
            R.string.autre_mesure
        )

        // ------------------------------------------------------------ sensibilisation
        //
        // Toujours exigee dans ce module : elle n'est conditionnee par rien, contrairement a
        // l'Enquete Menage ou elle depend de la presence d'enfants a charge.
        val themes = liste(suivi.themesSensibilisationStringify)
        exiger(themes.isEmpty(), R.string.themes_abordes)
        exiger(SaisiePfte.exigePrecision(themes) && vide(suivi.autreThemeSensibilisation),
            R.string.autre_theme)
        exiger(liste(suivi.outilsSensibilisationStringify).isEmpty(), R.string.outils_utilises)
        exiger(vide(suivi.nombreHommesSensibilises), R.string.nombre_d_hommes_sensibilises)
        exiger(vide(suivi.nombreFemmesSensibilisees), R.string.nombre_de_femmes_sensibilisees)
        exiger(vide(suivi.nombreGarconsSensibilises), R.string.nombre_de_garcons_sensibilises)
        exiger(vide(suivi.nombreFillesSensibilisees), R.string.nombre_de_filles_sensibilisees)
        exiger(vide(suivi.telephoneProducteurSensibilisation), R.string.telephone_du_producteur)
        exiger(vide(suivi.photoSensibilisation), R.string.photo_de_la_sensibilisation)

        return manques
    }

    private const val NON = "Non"
    private const val AUTRE = "Autre"
    private const val AUTRES = "Autres"
}
