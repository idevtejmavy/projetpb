package ci.progbandama.mobile.tools

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import ci.progbandama.mobile.activities.forms.views.MultiSelectSpinner
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import ci.progbandama.mobile.repositories.datas.CommonData
import com.blankj.utilcode.util.GsonUtils
import com.blankj.utilcode.util.LogUtils
import com.google.gson.reflect.TypeToken

/**
 * Composants de saisie adosses aux listes de reference PFTE.
 *
 * Les trois modules du programme reutilisent les memes listes : dix choix multiples pour
 * l'Enquete Menage, huit pour la Visite Plantation, dix pour le Suivi Enfant Travailleur.
 * Sans ce point d'entree commun, chacun aurait ete cable a la main, avec sa propre lecture
 * de la base, sa propre restauration de selection et sa propre serialisation.
 *
 * S'appuie sur Commons.setupItemMultiSelection, deja utilise par les formulaires existants,
 * plutot que de redevelopper la gestion du composant.
 *
 * Lot 1 - socle commun aux modules PFTE.
 */
object SaisiePfte {

    /** Separateur entre le niveau et la classe dans le libelle affiche. */
    private const val SEPARATEUR_GROUPE = " · "

    /** Termes declenchant l'affichage d'un champ "Autre, preciser", tels que definis cote web. */
    private val DECLENCHEURS_AUTRE = listOf("Autre", "préciser")

    private fun dao(context: Context) =
        ProgBandRoomDatabase.getDatabase(context.applicationContext)?.refPfteDao()

    /** Libelles d'une liste de reference, dans l'ordre d'affichage. */
    fun valeurs(context: Context, table: String): MutableList<String> = try {
        dao(context)?.nomsPourTable(table) ?: mutableListOf()
    } catch (ex: Exception) {
        LogUtils.e("Lecture du referentiel $table : ${ex.message}"); mutableListOf()
    }

    /** Vrai si la liste a bien ete telechargee : permet d'alerter avant d'ouvrir un formulaire. */
    fun estDisponible(context: Context, table: String): Boolean =
        try { (dao(context)?.compter(table) ?: 0) > 0 } catch (ex: Exception) { false }

    /** Listes de reference manquantes parmi celles attendues par les modules PFTE. */
    fun referentielsManquants(context: Context): List<String> =
        ReferentielsPfte.TABLES.filterNot { estDisponible(context, it) }

    /**
     * Branche un choix multiple sur une liste de reference.
     *
     * @param selectionCourante valeurs deja retenues, pour la reprise d'un brouillon.
     * @param surChangement recoit la selection a chaque validation.
     */
    fun listeMultiple(
        activite: Activity,
        composant: MultiSelectSpinner,
        table: String,
        titre: String,
        selectionCourante: MutableList<String> = mutableListOf(),
        surChangement: ((MutableList<String>) -> Unit)
    ) {
        val libelles = valeurs(activite, table)
        if (libelles.isEmpty()) {
            LogUtils.e("Referentiel $table vide : liste non renseignee. Synchronisation requise.")
        }
        Commons.setupItemMultiSelection(
            context = activite,
            selectItemMulti = composant,
            title = titre,
            itemList = libelles.map { CommonData(nom = it) },
            currentList = selectionCourante,
            onValueChanged = surChangement
        )
    }

    // ------------------------------------------------------------------ classes et niveaux

    /**
     * Classes prefixees de leur niveau d'etude : "Primaire · CP1".
     *
     * Le back-office regroupe les classes par niveau dans des optgroups, que la liste
     * deroulante Android ne sait pas rendre. Le prefixe restitue ce regroupement sans changer
     * la valeur transmise, qui reste le seul nom de la classe (voir [classeDepuisLibelle]).
     */
    fun classesAffichage(context: Context): MutableList<String> {
        val base = dao(context) ?: return mutableListOf()
        return try {
            val niveaux = base.pourTable(ReferentielsPfte.NIVEAUX_ETUDE).associate { it.idRef to it.nom }
            base.pourTable(ReferentielsPfte.CLASSES)
                .map { classe ->
                    val niveau = niveaux[classe.parentId]
                    if (niveau.isNullOrBlank()) classe.nom else "$niveau$SEPARATEUR_GROUPE${classe.nom}"
                }
                .toMutableList()
        } catch (ex: Exception) {
            LogUtils.e("Lecture des classes : ${ex.message}")
            mutableListOf()
        }
    }

    /** Valeur a transmettre au serveur a partir du libelle affiche. */
    fun classeDepuisLibelle(libelle: String?): String =
        libelle?.substringAfterLast(SEPARATEUR_GROUPE)?.trim().orEmpty()

    /** Libelle affiche correspondant a une classe enregistree, pour la reprise d'un brouillon. */
    fun libellePourClasse(context: Context, classe: String?): String? {
        if (classe.isNullOrBlank()) return null
        return classesAffichage(context).firstOrNull { classeDepuisLibelle(it) == classe } ?: classe
    }

    /**
     * Vocabulaire des actions de remediation - lot 5.
     *
     * Il n'existe aucune table pour cette liste : le back-office la construit a la volee, par
     * fusion dedoublonnee des trois listes de mesures. Le mobile fait de meme plutot que de
     * telecharger une liste de plus, et reste ainsi aligne sur le serveur si l'une des trois
     * evolue.
     *
     * L'ordre suit celui des trois listes, enfant puis menage puis communaute : c'est celui du
     * formulaire web, et l'agent y retrouve ses reperes.
     */
    fun actionsRemediation(context: Context): MutableList<String> {
        val fusion = LinkedHashSet<String>()
        listOf(
            ReferentielsPfte.MESURES_ENFANT,
            ReferentielsPfte.MESURES_MENAGE,
            ReferentielsPfte.MESURES_COMMUNAUTE
        ).forEach { table -> fusion.addAll(valeurs(context, table)) }
        return fusion.toMutableList()
    }

    // ------------------------------------------------------------------ regle "Autre, preciser"

    /**
     * Vrai si la selection impose de saisir une precision.
     *
     * Reprend la regle du back-office : un libelle contenant "Autre" ou "préciser" ouvre le
     * champ de precision associe. Elle sert six fois par fiche enfant.
     */
    fun exigePrecision(vararg selections: List<String>?): Boolean =
        selections.filterNotNull().flatten()
            .any { libelle -> DECLENCHEURS_AUTRE.any { libelle.contains(it, ignoreCase = true) } }

    // ------------------------------------------------------------------ affichage conditionnel

    /**
     * Affiche ou masque un bloc conditionnel.
     *
     * Reproduit le toggle() du back-office, dans les deux sens :
     *
     *  - a la fermeture, masquer ne suffit pas. Le bloc est aussi VIDE et desactive, sinon une
     *    valeur rendue sans objet par un autre choix partirait quand meme au serveur ;
     *  - a la reouverture, il ne suffit pas non plus de rendre le bloc visible : il faut
     *    REACTIVER ses champs, que la fermeture precedente avait desactives.
     *
     * Ce second point manquait. Un bloc ferme puis rouvert s'affichait grise, et l'agent ne
     * pouvait plus y saisir - le cas se produisait des qu'un choix multiple etait modifie deux
     * fois, ce qui est la situation courante sur "Autre, preciser".
     *
     * C'est la meme regle que celle appliquee par le binding par reflexion, qui ne collecte que
     * les conteneurs visibles : les deux mecanismes restent ainsi coherents.
     */
    fun basculer(bloc: View?, afficher: Boolean) {
        if (bloc == null) return
        bloc.visibility = if (afficher) View.VISIBLE else View.GONE
        if (afficher) reactiverChamps(bloc) else viderChamps(bloc)
    }

    /**
     * Vide et desactive recursivement les champs d'un bloc masque.
     *
     * L'ordre des cas compte, et c'est un piege : une liste deroulante EST un ViewGroup -
     * MultiSelectSpinner herite d'AppCompatSpinner, donc de Spinner, d'AdapterView et de
     * ViewGroup. Tester ViewGroup en premier faisait descendre dans les vues internes du
     * composant sans jamais remettre la liste a zero : le choix d'un bloc masque survivait et
     * partait au serveur. Les listes sont donc reconnues AVANT les conteneurs.
     */
    private fun viderChamps(vue: View) {
        when (vue) {
            is MultiSelectSpinner -> {
                vue.setSelection(intArrayOf())
                vue.isEnabled = false
            }
            is Spinner -> {
                // La premiere ligne est celle d'invite posee par Commons.setListenerForSpinner :
                // y revenir equivaut a n'avoir rien choisi.
                if (vue.adapter != null && vue.adapter.count > 0) vue.setSelection(0)
                vue.isEnabled = false
            }
            is EditText -> {
                vue.setText("")
                vue.isEnabled = false
            }
            is ViewGroup -> for (i in 0 until vue.childCount) viderChamps(vue.getChildAt(i))
        }
    }

    /**
     * Reactive les champs d'un bloc reaffiche.
     *
     * Meme piege que ci-dessus : une liste deroulante doit etre reactivee elle-meme, et non
     * parcourue comme un conteneur.
     */
    fun reactiverChamps(vue: View?) {
        if (vue == null) return
        vue.isEnabled = true
        if (vue is Spinner) return
        if (vue is ViewGroup) {
            for (i in 0 until vue.childCount) reactiverChamps(vue.getChildAt(i))
        }
    }

    // ------------------------------------------------------------------ serialisation

    /** Serialise une selection pour la colonne Stringify du modele. */
    fun serialiser(selection: List<String>?): String = GsonUtils.toJson(selection ?: emptyList<String>())

    /** Relit une colonne Stringify. Tolere une valeur absente ou illisible. */
    fun deserialiser(stringify: String?): MutableList<String> {
        if (stringify.isNullOrBlank()) return mutableListOf()
        return try {
            GsonUtils.fromJson<MutableList<String>>(
                stringify, object : TypeToken<MutableList<String>>() {}.type
            ) ?: mutableListOf()
        } catch (ex: Exception) {
            LogUtils.e("Relecture d'une selection : ${ex.message}"); mutableListOf()
        }
    }
}
