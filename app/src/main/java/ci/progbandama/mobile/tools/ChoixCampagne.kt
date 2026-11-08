package ci.progbandama.mobile.tools

import android.content.Context
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.CampagneModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import com.toptoche.searchablespinnerlibrary.SearchableSpinner

/**
 * Choix de la campagne, en tete des formulaires qui la transmettent.
 *
 * Le meme cablage etait recopie dans chaque activite - et commente dans trois d'entre elles. Il est
 * pose ici une fois : la liste vient de la table locale, alimentee par getcampagne a chaque
 * configuration.
 *
 * Depuis le 8 novembre 2026, getcampagne renvoie les campagnes actives de la cooperative de
 * l'agent : la table locale en porte autant, la plus recente en tete, et le choix est donc reel.
 * Une cooperative qui n'en declare qu'une retrouve le cas precedent - un seul choix, deja
 * positionne.
 */
object ChoixCampagne {

    /**
     * Renseigne un selecteur et retourne la campagne pre-positionnee, ou null si l'appareil n'en
     * connait aucune.
     *
     * @param idInitial campagne a re-selectionner - reprise d'un brouillon ou d'une correction.
     *                  Absente de la liste, on retombe sur la premiere.
     * @param onChoix   appele des la pre-selection, puis a chaque changement. Recoit null quand
     *                  aucune campagne n'est connue, pour que l'appelant vide ses propres champs.
     */
    fun installer(
        contexte: Context,
        selecteur: SearchableSpinner?,
        idInitial: String? = null,
        onChoix: (CampagneModel?) -> Unit
    ): CampagneModel? {
        val selection = selecteur ?: return null
        val campagnes = lire(contexte)

        selection.setTitle(contexte.getString(R.string.choisir_la_campagne))

        if (campagnes.isEmpty()) {
            // Aucune campagne connue : l'appareil n'a jamais ete configure, ou le serveur n'en
            // declare aucune d'active. Le selecteur reste inerte plutot que d'ouvrir une liste vide.
            selection.adapter = ArrayAdapter(
                contexte, android.R.layout.simple_dropdown_item_1line,
                mutableListOf(contexte.getString(R.string.aucune_campagne_connue))
            )
            selection.isEnabled = false
            onChoix(null)
            return null
        }

        selection.isEnabled = true
        selection.adapter =
            ArrayAdapter(contexte, android.R.layout.simple_dropdown_item_1line, campagnes)

        val depart = campagnes.indexOfFirst { it.id.toString() == idInitial }.takeIf { it >= 0 } ?: 0

        selection.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, vue: View?, position: Int, id: Long) {
                onChoix(campagnes.getOrNull(position))
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // La pose de l'adaptateur declenche deja une selection sur la position 0 : on ne notifie
        // explicitement que lorsque la reprise designe une autre ligne.
        selection.setSelection(depart)
        onChoix(campagnes[depart])

        return campagnes[depart]
    }

    /** Campagne active connue de l'appareil, sans passer par un selecteur. */
    fun active(contexte: Context): CampagneModel? = lire(contexte).firstOrNull()

    private fun lire(contexte: Context): MutableList<CampagneModel> = try {
        ProgBandRoomDatabase.getDatabase(contexte.applicationContext)?.campagneDao()?.getAll()
            ?: mutableListOf()
    } catch (ex: Exception) {
        // Une base non encore migree ne doit pas empecher l'ouverture du formulaire.
        mutableListOf()
    }
}
