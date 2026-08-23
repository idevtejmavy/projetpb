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
 * Visite Plantation (PFTE) - lot 4.
 *
 * Memes conventions que l'Enquete Menage : noms alignes sur POST api/apivisiteplantation, avec les
 * trois ecarts imposes par le controleur mobile - section_id, localite_id, producteur_id.
 *
 * Champs calcules par le serveur, donc absents ici : raisonInterview et typeEnquete (constantes),
 * sexeProducteur et codeProducteur (recopies du producteur), nomRepondant quand le producteur est
 * lui-meme le repondant, nombreEnfantsTrouves (compte des fiches), codeEnfant et statutFin.
 *
 * ATTENTION - quatre champs que l'API mobile ne lit pas encore.
 *
 * jourVisite, et cote fiche enfant heuresTravailSemaine, joursTravail et heuresTravailJournee,
 * existent en base et sont exiges par le controleur web, mais ApivisiteplantationController ne les
 * lit pas : ils seront ignores a l'envoi tant que le back-office n'aura pas ete complete. Ils sont
 * neanmoins collectes et transmis - les indicateurs de temps de travail sont le coeur du depistage
 * PFTE, et la saisie ne doit pas etre a refaire le jour ou le serveur les acceptera.
 */
@Parcelize
@Entity(tableName = Constants.TABLE_VISITE_PLANTATION, indices = [Index(value = ["uid"], unique = true)])
data class VisitePlantationModel(

    @SerializedName("uid") @Expose @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    @SerializedName("id") @Expose var id: Int = 0,
    @SerializedName("userid") @Expose var userid: Int? = 0,

    // ---------------------------------------------------------------- identification
    @SerializedName("section_id") @Expose var sectionId: String? = "",
    @SerializedName("localite_id") @Expose var localiteId: String? = "",
    @SerializedName("producteur_id") @Expose var producteurId: String? = "",
    @SerializedName("dateEnquete") @Expose var dateEnquete: String? = "",
    /** Propre a ce module : le jour de la semaine ou la visite a eu lieu. */
    @SerializedName("jourVisite") @Expose var jourVisite: String? = "",
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

    // ---------------------------------------------------------------- caracteristiques de la plantation
    @SerializedName("superficiePlantation") @Expose var superficiePlantation: String? = "",
    @SerializedName("nombreManoeuvresPermanents") @Expose var nombreManoeuvresPermanents: String? = "",
    @SerializedName("manoeuvresPermanentsMoins18") @Expose var manoeuvresPermanentsMoins18: String? = "",
    @SerializedName("nombreManoeuvresJournaliers") @Expose var nombreManoeuvresJournaliers: String? = "",
    @SerializedName("manoeuvresJournaliersMoins18") @Expose var manoeuvresJournaliersMoins18: String? = "",
    @SerializedName("nombreEnfants0a4") @Expose var nombreEnfants0a4: String? = "",
    /** Pilote la generation des fiches : l'agent ne les ajoute pas une a une. */
    @SerializedName("nombreEnfants5a17") @Expose var nombreEnfants5a17: String? = "",
    @SerializedName("nombrePersonnesTrouvees") @Expose var nombrePersonnesTrouvees: String? = "",

    // ---------------------------------------------------------------- soumission
    @SerializedName("etatSoumission") @Expose var etatSoumission: String? = "Soumis",

    // ---------------------------------------------------------------- listes portees en colonne
    var raisonRefusStringify: String? = "",
    /** Fiches enfant serialisees. Meme dispositif que l'Enquete Menage : pas de seconde table. */
    var enfantsStringify: String? = "",

    // ---------------------------------------------------------------- suivi local
    var isSynced: Boolean = false,
    var origin: String? = "local",
    var agentId: String? = "",
    var sectionNom: String? = "",
    var localiteNom: String? = "",
    var producteurNom: String? = ""

) : Parcelable {

    @Ignore @SerializedName("raisonRefus") @Expose(serialize = true, deserialize = false)
    var raisonRefus: MutableList<String>? = mutableListOf()

    @Ignore @SerializedName("enfants") @Expose(serialize = true, deserialize = false)
    var enfants: MutableList<EnfantPlantationModel>? = mutableListOf()
}

/**
 * Enfant trouve dans la plantation.
 *
 * Vingt-trois champs, contre vingt-cinq pour l'Enquete Menage : pas de presentDisponible ni de
 * raisonAbsent, mais trois indicateurs de temps de travail que l'Enquete Menage n'a pas.
 *
 * joursTravail et heuresTravailJournee portent des codes numeriques - 0 a 7 et 0 a 24 - et non des
 * libelles : c'est ce que le back-office enregistre.
 */
@Parcelize
data class EnfantPlantationModel(

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

    // Temps de travail : le coeur du depistage. Voir la note d'entete du modele principal.
    @SerializedName("heuresTravailSemaine") @Expose var heuresTravailSemaine: String? = "",
    @SerializedName("joursTravail") @Expose var joursTravail: String? = "",
    @SerializedName("heuresTravailJournee") @Expose var heuresTravailJournee: String? = "",

    @SerializedName("autreRaisonTravailAbus") @Expose var autreRaisonTravailAbus: String? = "",
    @SerializedName("autreMesure") @Expose var autreMesure: String? = "",

    @SerializedName("raisonNonScolarisation") @Expose var raisonNonScolarisation: MutableList<String> = mutableListOf(),
    @SerializedName("raisonPasExtrait") @Expose var raisonPasExtrait: MutableList<String> = mutableListOf(),
    @SerializedName("situationsPfte") @Expose var situationsPfte: MutableList<String> = mutableListOf(),
    @SerializedName("raisonTravailAbus") @Expose var raisonTravailAbus: MutableList<String> = mutableListOf(),
    @SerializedName("mesuresEnfant") @Expose var mesuresEnfant: MutableList<String> = mutableListOf(),
    @SerializedName("mesuresMenage") @Expose var mesuresMenage: MutableList<String> = mutableListOf(),
    @SerializedName("mesuresCommunaute") @Expose var mesuresCommunaute: MutableList<String> = mutableListOf()

) : Parcelable
