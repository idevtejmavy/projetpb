package ci.progbandama.mobile.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ci.progbandama.mobile.tools.Constants
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize

/**
 * Enfant deja recense, pouvant faire l'objet d'un suivi - lot 5.
 *
 * Ce module ne part pas d'un producteur mais d'un enfant existant, issu d'une Enquete Menage
 * anterieure. La liste vient de POST api/getenfantsmenage et doit etre disponible HORS LIGNE : un
 * agent qui ouvre le formulaire en plantation n'a pas de reseau pour aller la chercher.
 *
 * Elle est donc mise en cache localement, rafraichie a chaque mise a jour des donnees, au meme
 * titre que les producteurs ou les localites.
 *
 * Les treize champs conserves ici sont exactement ceux que le formulaire pre-remplit a la
 * selection de l'enfant. Ils restent modifiables : c'est tout le principe d'une reevaluation, on
 * repart de la situation connue pour constater ce qui a change.
 */
@Parcelize
@Entity(
    tableName = Constants.TABLE_ENFANT_SUIVABLE,
    indices = [Index(value = ["idServeur"], unique = true)]
)
data class EnfantSuivableModel(

    @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    /** Identifiant de l'enfant cote serveur : c'est lui que le formulaire transmet. */
    var idServeur: Int = 0,
    var codeEnfant: String? = "",

    // ---------------------------------------------------------------- ce qui est pre-rempli
    var nom: String? = "",
    var dateNaissance: String? = "",
    var sexe: String? = "",
    var lienParente: String? = "",
    var autreLienParente: String? = "",
    var raisonNeVitPasParents: String? = "",
    var autreRaisonNeVitPasParents: String? = "",
    var situationScolaire: String? = "",
    var niveauScolaire: String? = "",
    var autreRaisonNonScolarisation: String? = "",
    var extraitNaissance: String? = "",
    var autreRaisonTravailAbus: String? = "",
    var autreMesure: String? = "",

    // ---------------------------------------------------------------- rattachement, pour l'affichage
    var producteurNom: String? = "",
    var localiteNom: String? = "",

    var agentId: String? = ""

) : Parcelable {

    /** Libelle propose dans la liste deroulante : le code seul ne dit rien a l'agent. */
    fun libelle(): String {
        val identite = nom.orEmpty().trim()
        val code = codeEnfant.orEmpty().trim()
        return when {
            code.isBlank() -> identite
            identite.isBlank() -> code
            else -> "$identite ($code)"
        }
    }
}

/**
 * Reponse de POST api/getenfantsmenage.
 *
 * Modele de transport, distinct de l'entite : le serveur renvoie l'enfant avec son enquete
 * imbriquee, elle-meme portant le producteur et la localite. Faire porter cette forme a une table
 * Room aurait impose des relations la ou trois libelles suffisent.
 */
data class EnfantSuivableDto(
    @SerializedName("id") @Expose var id: Int = 0,
    @SerializedName("codeEnfant") @Expose var codeEnfant: String? = "",
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
    @SerializedName("menage") @Expose var menage: MenageDeLEnfant? = null
) {

    /** Enquete d'origine, dont on ne retient que le producteur et la localite. */
    data class MenageDeLEnfant(
        @SerializedName("producteur") @Expose var producteur: ProducteurDeLEnfant? = null,
        @SerializedName("localite") @Expose var localite: LocaliteDeLEnfant? = null
    )

    data class ProducteurDeLEnfant(
        @SerializedName("nom") @Expose var nom: String? = "",
        @SerializedName("prenoms") @Expose var prenoms: String? = ""
    )

    data class LocaliteDeLEnfant(
        @SerializedName("nom") @Expose var nom: String? = ""
    )

    fun versModele(agentId: String) = EnfantSuivableModel(
        uid = 0,
        idServeur = id,
        codeEnfant = codeEnfant,
        nom = nom,
        dateNaissance = dateNaissance,
        sexe = sexe,
        lienParente = lienParente,
        autreLienParente = autreLienParente,
        raisonNeVitPasParents = raisonNeVitPasParents,
        autreRaisonNeVitPasParents = autreRaisonNeVitPasParents,
        situationScolaire = situationScolaire,
        niveauScolaire = niveauScolaire,
        autreRaisonNonScolarisation = autreRaisonNonScolarisation,
        extraitNaissance = extraitNaissance,
        autreRaisonTravailAbus = autreRaisonTravailAbus,
        autreMesure = autreMesure,
        producteurNom = menage?.producteur?.let {
            "${it.nom.orEmpty()} ${it.prenoms.orEmpty()}".trim()
        }.orEmpty(),
        localiteNom = menage?.localite?.nom.orEmpty(),
        agentId = agentId
    )
}
