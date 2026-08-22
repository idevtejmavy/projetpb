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
 * Enquete Menage (PFTE) - lot 3.
 *
 * Les noms exposes reprennent exactement ceux attendus par POST api/apienquetemenage. Trois seuls
 * ecarts avec le formulaire web, imposes par le controleur mobile : section_id, localite_id et
 * producteur_id la ou le web poste section, localite et producteur.
 *
 * Les champs calcules par le serveur ne figurent pas ici et ne doivent pas etre envoyes :
 * raisonInterview et typeEnquete (constantes), sexeProducteur et codeProducteur (recopies du
 * producteur), totalPersonnesMenage et totalPersonnesSensibilisees (sommes), codeEnfant, et
 * statutFin (arbre de decision serveur). Les recalculer cote mobile n'apporterait rien et
 * ouvrirait un risque de divergence.
 *
 * Les valeurs numeriques sont portees en texte, comme partout ailleurs dans l'application : c'est
 * ce que produit le binding par reflexion depuis les champs de saisie, et le serveur les recoit
 * dans des colonnes entieres sans difficulte.
 */
@Parcelize
@Entity(tableName = Constants.TABLE_ENQUETE_MENAGE, indices = [Index(value = ["uid"], unique = true)])
data class EnqueteMenageModel(

    @SerializedName("uid") @Expose @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    @SerializedName("id") @Expose var id: Int = 0,
    @SerializedName("userid") @Expose var userid: Int? = 0,

    // ---------------------------------------------------------------- identification
    @SerializedName("section_id") @Expose var sectionId: String? = "",
    @SerializedName("localite_id") @Expose var localiteId: String? = "",
    @SerializedName("producteur_id") @Expose var producteurId: String? = "",
    @SerializedName("dateEnquete") @Expose var dateEnquete: String? = "",
    @SerializedName("nomEnqueteur") @Expose var nomEnqueteur: String? = "",
    @SerializedName("latitude") @Expose var latitude: String? = "",
    @SerializedName("longitude") @Expose var longitude: String? = "",
    @SerializedName("altitude") @Expose var altitude: String? = "",
    @SerializedName("precisionGps") @Expose var precisionGps: String? = "",

    // ---------------------------------------------------------------- filtre et consentement
    @SerializedName("estProducteurRepondant") @Expose var estProducteurRepondant: String? = "",
    @SerializedName("nomRepondant") @Expose var nomRepondant: String? = "",
    @SerializedName("titreRepondant") @Expose var titreRepondant: String? = "",
    @SerializedName("producteurDisponible") @Expose var producteurDisponible: String? = "",
    @SerializedName("raisonIndisponibilite") @Expose var raisonIndisponibilite: String? = "",
    @SerializedName("datePlanification") @Expose var datePlanification: String? = "",
    @SerializedName("autreRaisonRefus") @Expose var autreRaisonRefus: String? = "",
    @SerializedName("consentement") @Expose var consentement: String? = "",

    // ---------------------------------------------------------------- caracteristiques du menage
    @SerializedName("situationMatrimoniale") @Expose var situationMatrimoniale: String? = "",
    @SerializedName("nombreAdultes") @Expose var nombreAdultes: String? = "",
    @SerializedName("nombreEnfants0a4") @Expose var nombreEnfants0a4: String? = "",
    @SerializedName("nombreEnfants5a17") @Expose var nombreEnfants5a17: String? = "",
    @SerializedName("aEnfantACharge") @Expose var aEnfantACharge: String? = "",
    @SerializedName("nombreEnfantsACharge") @Expose var nombreEnfantsACharge: String? = "",
    @SerializedName("nombreEnfantsEnquetes") @Expose var nombreEnfantsEnquetes: String? = "",

    // ---------------------------------------------------------------- sensibilisation de proximite
    @SerializedName("autreThemeSensibilisation") @Expose var autreThemeSensibilisation: String? = "",
    @SerializedName("nombreHommesSensibilises") @Expose var nombreHommesSensibilises: String? = "",
    @SerializedName("nombreFemmesSensibilisees") @Expose var nombreFemmesSensibilisees: String? = "",
    @SerializedName("nombreGarconsSensibilises") @Expose var nombreGarconsSensibilises: String? = "",
    @SerializedName("nombreFillesSensibilisees") @Expose var nombreFillesSensibilisees: String? = "",
    @SerializedName("telephoneProducteurSensibilisation") @Expose var telephoneProducteurSensibilisation: String? = "",
    /** Image encodee en base64 : le controleur mobile n'accepte pas le multipart. */
    @SerializedName("photoSensibilisation") @Expose var photoSensibilisation: String? = "",

    // ---------------------------------------------------------------- soumission
    /** "Brouillon" ou "Soumis". Toute autre valeur est normalisee en "Soumis" par le serveur. */
    @SerializedName("etatSoumission") @Expose var etatSoumission: String? = "Soumis",

    // ---------------------------------------------------------------- listes portees en colonne
    //
    // Meme dispositif que le reste de l'application : la liste vit dans une colonne texte, et un
    // champ @Ignore expose la vraie liste au moment de l'envoi. Ces colonnes ne sont pas exposees :
    // le serveur attend les tableaux, pas leur forme serialisee.
    var raisonRefusStringify: String? = "",
    var themesSensibilisationStringify: String? = "",
    var outilsSensibilisationStringify: String? = "",
    /**
     * Fiches enfant serialisees.
     *
     * Les enfants ne sont pas une table a part. Le serveur remplace integralement ses enfants a
     * chaque enregistrement - jamais un delta - et le mobile n'a aucun endpoint pour les relire :
     * une seconde table n'apporterait qu'une jointure a maintenir et un risque d'orphelins. Ils
     * suivent donc le meme dispositif que les autres listes.
     */
    var enfantsStringify: String? = "",

    // ---------------------------------------------------------------- suivi local
    var isSynced: Boolean = false,
    var origin: String? = "local",
    var agentId: String? = "",
    /** Libelles retenus pour l'affichage des listes : le mobile ne relit pas le serveur. */
    var sectionNom: String? = "",
    var localiteNom: String? = "",
    var producteurNom: String? = ""

) : Parcelable {

    @Ignore @SerializedName("raisonRefus") @Expose(serialize = true, deserialize = false)
    var raisonRefus: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("themesSensibilisation") @Expose(serialize = true, deserialize = false)
    var themesSensibilisation: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("outilsSensibilisation") @Expose(serialize = true, deserialize = false)
    var outilsSensibilisation: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("enfants") @Expose(serialize = true, deserialize = false)
    var enfants: MutableList<EnfantMenageModel>? = mutableListOf()
}

/**
 * Fiche enfant d'une Enquete Menage.
 *
 * Pas une entite Room : voir EnqueteMenageModel.enfantsStringify. Tous les champs sont exposes,
 * cette fiche partant au serveur imbriquee dans le tableau enfants du formulaire.
 *
 * codeEnfant est genere par le serveur - {codeProdapp}-EN{idEnquete}.{numero} - et n'est donc pas
 * porte ici.
 */
@Parcelize
data class EnfantMenageModel(

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

    @SerializedName("autreRaisonTravailAbus") @Expose var autreRaisonTravailAbus: String? = "",
    @SerializedName("autreMesure") @Expose var autreMesure: String? = "",

    // Les listes sont ici de vraies listes : la fiche n'etant pas une table, aucune colonne texte
    // n'est necessaire et le nom expose est directement celui attendu par le serveur.
    @SerializedName("raisonNonScolarisation") @Expose var raisonNonScolarisation: MutableList<String> = mutableListOf(),
    @SerializedName("raisonPasExtrait") @Expose var raisonPasExtrait: MutableList<String> = mutableListOf(),
    @SerializedName("situationsPfte") @Expose var situationsPfte: MutableList<String> = mutableListOf(),
    @SerializedName("raisonTravailAbus") @Expose var raisonTravailAbus: MutableList<String> = mutableListOf(),
    @SerializedName("mesuresEnfant") @Expose var mesuresEnfant: MutableList<String> = mutableListOf(),
    @SerializedName("mesuresMenage") @Expose var mesuresMenage: MutableList<String> = mutableListOf(),
    @SerializedName("mesuresCommunaute") @Expose var mesuresCommunaute: MutableList<String> = mutableListOf()

) : Parcelable
