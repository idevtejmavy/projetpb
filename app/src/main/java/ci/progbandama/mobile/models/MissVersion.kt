package ci.progbandama.mobile.models

import com.google.gson.annotations.SerializedName

/**
 * Fichier de reference lu a chaque ouverture du tableau de bord.
 *
 * Il porte deux choses : le controle de version, et la liste des formulaires a afficher. Les deux
 * se pilotent donc sans livrer une nouvelle version de l'application.
 *
 * Attention - Gson construit cet objet SANS passer par le constructeur : les valeurs par defaut
 * declarees ici ne sont jamais appliquees. Toute cle absente du fichier arrive a null, et doit
 * etre traitee comme telle par les appelants.
 */
data class MissVersion(
    val latestVersion: String? = null,
    val latestVersionCode: String? = null,
    val ignoreVersionCode: MutableList<Int>? = arrayListOf(),
    val url: String? = null,
    val releaseNotes: MutableList<String>? = arrayListOf(),
    @SerializedName("forms") val formulaires: MutableList<FormulaireAffiche>? = arrayListOf(),
)

/**
 * Visibilite d'un formulaire dans les tuiles du tableau de bord et dans le menu lateral.
 *
 * @param type  discriminant du formulaire, identique a celui de FeatureModel - PRODUCTEUR,
 *              VISITE_PLANTATION, etc. C'est la seule cle qui compte.
 * @param label intitule lisible, present pour que le fichier reste comprehensible a la lecture.
 *              Il n'est pas utilise par l'application.
 * @param show  false retire le formulaire des deux endroits. Absent, il est considere visible :
 *              un fichier incomplet ne doit jamais faire disparaitre le travail d'un agent.
 */
data class FormulaireAffiche(
    val type: String? = null,
    val label: String? = null,
    val show: Boolean? = null,
)
