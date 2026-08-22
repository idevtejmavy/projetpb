package ci.progbandama.mobile.tools

import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import com.blankj.utilcode.util.LogUtils

/**
 * Remplacement atomique d'une table lors de la mise a jour de la base locale.
 *
 * La chaine de telechargement procede partout de la meme facon : purger la table, puis reinserer
 * ce que le serveur vient de renvoyer. Les deux operations etaient distinctes, si bien qu'entre
 * les deux la table etait vide. Une coupure a cet instant - fermeture de l'application, batterie,
 * exception au milieu de la boucle d'insertion - laissait la table amputee ou vide, sans aucun
 * moyen de le savoir : l'agent retrouvait ses listes deroulantes vides et ne pouvait plus saisir
 * hors ligne jusqu'a la mise a jour suivante.
 *
 * Le correctif D3 avait deja repousse la purge apres la reception de la reponse, ce qui traite le
 * cas du serveur injoignable. Il reste la fenetre entre la purge et la fin des insertions, que
 * seule une transaction ferme : ou la table est entierement remplacee, ou elle n'a pas bouge.
 *
 * Lot 1, ligne 1.6 - securisation de la mise a jour de la base locale.
 */
object MajLocale {

    /**
     * Remplace le contenu d'une table en une seule transaction.
     *
     * @param elements ce que le serveur a renvoye. null = reponse non exploitable : rien n'est
     *        purge, la table locale reste telle quelle. Une liste vide, elle, est une reponse
     *        valide et vide bien la table.
     * @param purge suppression a appliquer. Selon la table, deleteAll() pour une liste de
     *        reference, deleteSynced() la ou des saisies locales non transmises cohabitent.
     * @param inserer insertion d'un element.
     * @return vrai si le remplacement a bien eu lieu.
     */
    fun <T> remplacer(
        base: ProgBandRoomDatabase?,
        libelle: String,
        elements: List<T>?,
        purge: () -> Unit,
        inserer: (T) -> Unit
    ): Boolean {
        if (elements == null) {
            LogUtils.d("Mise a jour $libelle : reponse non exploitable, table conservee")
            return false
        }
        return enBloc(base, libelle) {
            purge()
            elements.forEach { inserer(it) }
        }
    }

    /**
     * Meme garantie, pour un remplacement qui ne se decompose pas en une boucle par element -
     * une insertion groupee, par exemple.
     *
     * L'appelant a deja verifie que la reponse du serveur etait exploitable : ce qui est passe
     * ici sera execute, ou pas du tout.
     */
    fun enBloc(base: ProgBandRoomDatabase?, libelle: String, remplacement: () -> Unit): Boolean {
        if (base == null) {
            LogUtils.e("Mise a jour $libelle : base indisponible, remplacement abandonne")
            return false
        }
        try {
            base.runInTransaction(remplacement)
        } catch (ex: Exception) {
            // Room a annule la transaction : la table est restee dans son etat d'avant la purge.
            // L'exception est ensuite relancee, car c'est la chaine de telechargement qui decide
            // quoi en faire - elle affiche un message et propose de recommencer la mise a jour.
            // L'absorber ici ferait passer pour reussie une etape qui ne l'est pas.
            LogUtils.e("Mise a jour $libelle : remplacement annule, ancien contenu conserve - ${ex.message}")
            throw ex
        }
        return true
    }
}
