package ci.progbandama.mobile.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import ci.progbandama.mobile.tools.Constants
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Suivi Enfant Travailleur (PFTE) - lot 5.
 *
 * Troisieme et dernier module du programme. Il se distingue des deux autres sur trois points :
 *
 *  - il part d'un ENFANT deja recense, non d'un producteur. Un seul ecart de nommage avec le web :
 *    enfant_id la ou le formulaire poste enfant ;
 *  - il n'a AUCUN champ de geolocalisation. Le suivi porte sur une personne, pas sur un lieu ;
 *  - il n'a pas de fiches repetables : l'enfant suivi est le sujet du formulaire.
 *
 * Champs calcules par le serveur, donc absents ici : raisonInterview (constante "Visite de
 * suivi"), et le rattachement producteur / localite, derive de l'enfant.
 *
 * ATTENTION - cinq champs que l'API mobile ne lit pas encore.
 *
 * presentDisponible, raisonAbsent, heuresTravailSemaine, joursTravail et heuresTravailJournee
 * existent en base, sont enregistres par le controleur web et y sont obligatoires, mais
 * ApisuivienfanttravailleurController ne les lit pas. Ils sont neanmoins collectes et transmis :
 * ce sont les indicateurs de temps de travail, le coeur du module, et la saisie ne doit pas etre
 * a refaire le jour ou le serveur les acceptera. Meme reserve que pour la Visite Plantation.
 *
 * Quatre champs portent des CODES et non des libelles - presentDisponible, raisonAbsent,
 * joursTravail et heuresTravailJournee. Voir VocabulairesPfte.
 */
@Parcelize
@Entity(tableName = Constants.TABLE_SUIVI_ENFANT, indices = [Index(value = ["uid"], unique = true)])
data class SuiviEnfantModel(

    @SerializedName("uid") @Expose @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    @SerializedName("id") @Expose var id: Int = 0,
    @SerializedName("userid") @Expose var userid: Int? = 0,

    // ---------------------------------------------------------------- identification
    /** Identifiant serveur de l'enfant suivi. */
    @SerializedName("enfant_id") @Expose var enfantId: String? = "",
    @SerializedName("dateEnquete") @Expose var dateEnquete: String? = "",
    @SerializedName("nomEnqueteur") @Expose var nomEnqueteur: String? = "",

    // ---------------------------------------------------------------- reevaluation de la situation
    @SerializedName("nom") @Expose var nom: String? = "",
    @SerializedName("dateNaissance") @Expose var dateNaissance: String? = "",
    @SerializedName("sexe") @Expose var sexe: String? = "",
    @SerializedName("lienParente") @Expose var lienParente: String? = "",
    @SerializedName("autreLienParente") @Expose var autreLienParente: String? = "",
    @SerializedName("raisonNeVitPasParents") @Expose var raisonNeVitPasParents: String? = "",
    @SerializedName("autreRaisonNeVitPasParents") @Expose var autreRaisonNeVitPasParents: String? = "",
    @SerializedName("situationScolaire") @Expose var situationScolaire: String? = "",
    @SerializedName("niveauScolaire") @Expose var niveauScolaire: String? = "",
    @SerializedName("autreRaisonNonScolarisation") @Expose var autreRaisonNonScolarisation: String? = "",
    @SerializedName("extraitNaissance") @Expose var extraitNaissance: String? = "",

    // Indicateurs de temps de travail : voir la note d'entete.
    @SerializedName("presentDisponible") @Expose var presentDisponible: String? = "",
    @SerializedName("raisonAbsent") @Expose var raisonAbsent: String? = "",
    @SerializedName("heuresTravailSemaine") @Expose var heuresTravailSemaine: String? = "",
    @SerializedName("joursTravail") @Expose var joursTravail: String? = "",
    @SerializedName("heuresTravailJournee") @Expose var heuresTravailJournee: String? = "",

    @SerializedName("autreRaisonTravailAbus") @Expose var autreRaisonTravailAbus: String? = "",
    @SerializedName("autreMesure") @Expose var autreMesure: String? = "",

    // ---------------------------------------------------------------- sensibilisation de proximite
    //
    // Identique a celle de l'Enquete Menage, a une difference pres : elle est toujours affichee,
    // sans etre conditionnee par la presence d'enfants a charge.
    @SerializedName("autreThemeSensibilisation") @Expose var autreThemeSensibilisation: String? = "",
    @SerializedName("nombreHommesSensibilises") @Expose var nombreHommesSensibilises: String? = "",
    @SerializedName("nombreFemmesSensibilisees") @Expose var nombreFemmesSensibilisees: String? = "",
    @SerializedName("nombreGarconsSensibilises") @Expose var nombreGarconsSensibilises: String? = "",
    @SerializedName("nombreFillesSensibilisees") @Expose var nombreFillesSensibilisees: String? = "",
    @SerializedName("telephoneProducteurSensibilisation") @Expose var telephoneProducteurSensibilisation: String? = "",
    /** Image encodee en base64 : le controleur mobile n'accepte pas le multipart. */
    @SerializedName("photoSensibilisation") @Expose var photoSensibilisation: String? = "",

    // ---------------------------------------------------------------- soumission
    @SerializedName("etatSoumission") @Expose var etatSoumission: String? = "Soumis",

    // ---------------------------------------------------------------- listes portees en colonne
    var actionsRemediationStringify: String? = "",
    var raisonNonScolarisationStringify: String? = "",
    var raisonPasExtraitStringify: String? = "",
    var situationsPfteStringify: String? = "",
    var raisonTravailAbusStringify: String? = "",
    var mesuresEnfantStringify: String? = "",
    var mesuresMenageStringify: String? = "",
    var mesuresCommunauteStringify: String? = "",
    var themesSensibilisationStringify: String? = "",
    var outilsSensibilisationStringify: String? = "",

    // ---------------------------------------------------------------- suivi local
    var isSynced: Boolean = false,
    var origin: String? = "local",
    var agentId: String? = "",
    /** Libelles retenus a la saisie : le mobile ne relit pas le serveur. */
    var enfantNom: String? = "",
    var producteurNom: String? = "",
    var localiteNom: String? = ""

) : Parcelable {

    @Ignore @SerializedName("actionsRemediation") @Expose(serialize = true, deserialize = false)
    var actionsRemediation: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("raisonNonScolarisation") @Expose(serialize = true, deserialize = false)
    var raisonNonScolarisation: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("raisonPasExtrait") @Expose(serialize = true, deserialize = false)
    var raisonPasExtrait: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("situationsPfte") @Expose(serialize = true, deserialize = false)
    var situationsPfte: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("raisonTravailAbus") @Expose(serialize = true, deserialize = false)
    var raisonTravailAbus: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("mesuresEnfant") @Expose(serialize = true, deserialize = false)
    var mesuresEnfant: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("mesuresMenage") @Expose(serialize = true, deserialize = false)
    var mesuresMenage: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("mesuresCommunaute") @Expose(serialize = true, deserialize = false)
    var mesuresCommunaute: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("themesSensibilisation") @Expose(serialize = true, deserialize = false)
    var themesSensibilisation: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("outilsSensibilisation") @Expose(serialize = true, deserialize = false)
    var outilsSensibilisation: MutableList<String>? = mutableListOf()
}
