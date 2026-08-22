package ci.progbandama.mobile.tools

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.LayoutRes
import com.blankj.utilcode.util.LogUtils

/**
 * Fiches enfant repetables d'un formulaire PFTE.
 *
 * Aucun formulaire de l'application n'avait de sous-formulaire repetable : ce mecanisme est
 * la seule brique du socle sans precedent interne.
 *
 * Les fiches sont empilees dans un LinearLayout, et non dans un RecyclerView. Ce choix est
 * volontaire :
 *
 *  - le recyclage detruit et reconstruit les vues au defilement, ce qui perd l'etat de saisie
 *    des blocs sortis de l'ecran ;
 *  - le binding par reflexion de Commons.getAllTitleAndValueViews() ne parcourt que la
 *    hierarchie attachee : les fiches recyclees lui echapperaient ;
 *  - les regles d'affichage conditionnel sont propres a chaque fiche, un adaptateur devrait
 *    les rejouer a chaque liaison.
 *
 * Le volume le justifie : une fiche par enfant du menage, en pratique moins de dix.
 * C'est aussi ce que fait le back-office, qui empile ses blocs dans un conteneur simple.
 *
 * Deux modes d'alimentation, selon le module :
 *  - ajout manuel plafonne par un effectif declare (Enquete Menage) ;
 *  - generation automatique a partir d'un effectif (Visite Plantation), via
 *    [synchroniserDepuisNombre].
 *
 * Lot 1 - socle commun aux modules PFTE.
 *
 * @param surCreation cable les listes de reference et les regles conditionnelles d'une fiche.
 * @param lecture construit le modele a partir des vues d'une fiche ; null pour l'ignorer.
 * @param ecriture reporte un modele dans les vues d'une fiche.
 */
class FichesEnfant<T>(
    private val contexte: Context,
    private val conteneur: LinearLayout,
    @LayoutRes private val layoutFiche: Int,
    private val idNumero: Int,
    private val idBoutonRetirer: Int,
    private val surCreation: (vue: View, index: Int) -> Unit,
    private val lecture: (vue: View, index: Int) -> T?,
    private val ecriture: (vue: View, modele: T) -> Unit,
    private val libelleNumero: (Int) -> String = { "Enfant n°$it" },
    private val surChangementNombre: ((Int) -> Unit)? = null
) {

    /** Effectif maximal autorise. null = sans plafond. */
    private var plafond: Int? = null

    fun taille(): Int = conteneur.childCount

    fun definirPlafond(effectif: Int?) {
        plafond = effectif
        surChangementNombre?.invoke(taille())
    }

    fun plafondAtteint(): Boolean {
        val max = plafond ?: return false
        return taille() >= max
    }

    /** Vues des fiches, dans l'ordre d'affichage. */
    fun fiches(): List<View> = (0 until conteneur.childCount).map { conteneur.getChildAt(it) }

    /**
     * Ajoute une fiche vierge.
     * @return la vue creee, ou null si le plafond est atteint.
     */
    fun ajouter(): View? {
        if (plafondAtteint()) {
            LogUtils.d("Fiche enfant non ajoutee : plafond de ${plafond} atteint")
            return null
        }
        val vue = LayoutInflater.from(contexte).inflate(layoutFiche, conteneur, false)
        conteneur.addView(vue)

        vue.findViewById<View>(idBoutonRetirer)?.setOnClickListener { retirer(vue) }

        return try {
            surCreation(vue, conteneur.childCount - 1)
            renumeroter()
            vue
        } catch (ex: Exception) {
            // Une fiche a moitie cablee vaut moins que pas de fiche du tout.
            conteneur.removeView(vue)
            renumeroter()
            LogUtils.e("Creation d'une fiche enfant : ${ex.message}")
            null
        }
    }

    fun retirer(vue: View) {
        conteneur.removeView(vue)
        renumeroter()
    }

    fun viderTout() {
        conteneur.removeAllViews()
        renumeroter()
    }

    /**
     * Aligne le nombre de fiches sur un effectif declare.
     *
     * Mode de la Visite Plantation, ou l'agent ne cree pas les fiches lui-meme : leur nombre
     * decoule du nombre d'enfants trouves. Les fiches en trop sont retirees par la fin, pour
     * ne pas perdre les saisies deja faites sur les premieres.
     */
    fun synchroniserDepuisNombre(cible: Int) {
        val voulu = if (cible < 0) 0 else cible
        while (taille() < voulu) {
            if (ajouter() == null) break
        }
        while (taille() > voulu) {
            conteneur.removeViewAt(conteneur.childCount - 1)
        }
        renumeroter()
    }

    /** Renumerote les fiches et signale le nouvel effectif. */
    fun renumeroter() {
        fiches().forEachIndexed { index, vue ->
            vue.findViewById<TextView>(idNumero)?.text = libelleNumero(index + 1)
        }
        surChangementNombre?.invoke(taille())
    }

    /**
     * Construit la liste des modeles a partir des vues.
     *
     * La collecte est explicite parce que le binding par reflexion ne descend pas dans les
     * fiches : chaque module fournit sa fonction de lecture.
     */
    fun collecter(): MutableList<T> {
        val modeles = mutableListOf<T>()
        fiches().forEachIndexed { index, vue ->
            try {
                lecture(vue, index)?.let { modeles.add(it) }
            } catch (ex: Exception) {
                LogUtils.e("Lecture de la fiche enfant ${index + 1} : ${ex.message}")
            }
        }
        return modeles
    }

    /**
     * Recree les fiches a partir de modeles enregistres.
     *
     * Utilise a la reprise d'un brouillon. Le plafond est temporairement leve : un brouillon
     * peut contenir plus de fiches que l'effectif actuellement declare, et perdre des saisies
     * a la relecture serait pire que depasser le plafond.
     */
    fun restaurer(modeles: List<T>?) {
        viderTout()
        if (modeles.isNullOrEmpty()) return

        val plafondInitial = plafond
        plafond = null
        try {
            modeles.forEach { modele ->
                val vue = ajouter() ?: return@forEach
                try {
                    ecriture(vue, modele)
                } catch (ex: Exception) {
                    LogUtils.e("Restauration d'une fiche enfant : ${ex.message}")
                }
            }
        } finally {
            plafond = plafondInitial
            renumeroter()
        }
    }
}
