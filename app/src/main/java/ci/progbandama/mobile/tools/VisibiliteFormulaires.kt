package ci.progbandama.mobile.tools

import ci.progbandama.mobile.models.FormulaireAffiche
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.google.gson.reflect.TypeToken

/**
 * Formulaires a afficher dans les tuiles du tableau de bord et dans le menu lateral.
 *
 * La liste vient du fichier de reference, releve a chaque ouverture du tableau de bord. Elle est
 * conservee sur l'appareil : le menu obeit donc aussi hors ligne, et des le demarrage - sans
 * attendre le retour du reseau.
 *
 * REGLE DE PRUDENCE : tout ce qui n'est pas explicitement marque show=false est AFFICHE. Un fichier
 * incomplet, injoignable ou mal forme laisse donc l'application telle quelle, plutot que de faire
 * disparaitre les formulaires d'un agent en pleine collecte.
 */
object VisibiliteFormulaires {

    /** Types masques, tels que retenus au dernier releve. */
    private fun masques(): Set<String> = try {
        val brut = SPUtils.getInstance().getString(Constants.FORMULAIRES_MASQUES, "")
        if (brut.isNullOrBlank()) emptySet()
        else GsonUtils.fromJson<MutableList<String>>(
            brut, object : TypeToken<MutableList<String>>() {}.type
        ).orEmpty().toSet()
    } catch (ex: Exception) {
        trace("Lecture des formulaires masques : ${ex.message}")
        emptySet()
    }

    /** Le formulaire doit-il apparaitre ? */
    fun estVisible(type: String?): Boolean =
        type == null || !masques().contains(type.uppercase())

    /**
     * Types que le fichier autorise a s'afficher SANS droit declare cote site.
     *
     * Le site ne declare aucun droit pour les deux questionnaires PFTE ajoutes aux lots 4 et 5 :
     * sans cette liste, ils n'apparaissent qu'en build debug. Elle disparaitra d'elle-meme le jour
     * ou ces droits existeront - il suffira de retirer la propriete du fichier.
     */
    fun sansDroit(): Set<String> = try {
        val brut = SPUtils.getInstance().getString(Constants.FORMULAIRES_SANS_DROIT, "")
        if (brut.isNullOrBlank()) emptySet()
        else GsonUtils.fromJson<MutableList<String>>(
            brut, object : TypeToken<MutableList<String>>() {}.type
        ).orEmpty().toSet()
    } catch (ex: Exception) {
        trace("Lecture des formulaires sans droit : ${ex.message}")
        emptySet()
    }

    /**
     * Retient la liste relevee dans le fichier de reference.
     *
     * @return true si la selection a CHANGE, pour que l'appelant reconstruise le menu. Une liste
     *         nulle - cle absente du fichier - ne touche a rien : on conserve le dernier etat connu
     *         plutot que de tout reafficher sur un fichier tronque.
     */
    fun enregistrer(formulaires: MutableList<FormulaireAffiche>?): Boolean {
        if (formulaires == null) return false

        val nouveaux = formulaires
            .filter { it.show == false && !it.type.isNullOrBlank() }
            .map { it.type!!.uppercase() }
            .toSortedSet()

        // Un formulaire masque ne peut evidemment pas etre affiche sans droit : le masquage prime.
        val affranchis = formulaires
            .filter { it.sansDroit == true && it.show != false && !it.type.isNullOrBlank() }
            .map { it.type!!.uppercase() }
            .toSortedSet()

        if (nouveaux == masques().toSortedSet() && affranchis == sansDroit().toSortedSet()) return false

        return try {
            SPUtils.getInstance().put(Constants.FORMULAIRES_MASQUES, GsonUtils.toJson(nouveaux.toList()))
            SPUtils.getInstance().put(Constants.FORMULAIRES_SANS_DROIT, GsonUtils.toJson(affranchis.toList()))
            trace("Formulaires masques : ${if (nouveaux.isEmpty()) "aucun" else nouveaux.joinToString()}")
            trace("Formulaires sans droit : ${if (affranchis.isEmpty()) "aucun" else affranchis.joinToString()}")
            true
        } catch (ex: Exception) {
            trace("Enregistrement de la visibilite des formulaires : ${ex.message}")
            false
        }
    }

    /**
     * Journalise sans jamais lever : tous les appels de ce fichier sont dans des blocs catch, et
     * une levee du journal y remplacerait l'erreur d'origine par une autre.
     */
    private fun trace(message: String) {
        try {
            LogUtils.e(message)
        } catch (ignore: Throwable) {
            println(message)
        }
    }
}
