package ci.progbandama.mobile

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ci.progbandama.mobile.tools.SaisiePfte
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Affichage conditionnel des formulaires PFTE - lot 1, brique reutilisee par les trois modules.
 *
 * Deux defauts constates sur le terrain, tous deux silencieux :
 *
 *  - un bloc ferme puis rouvert restait grise : ses champs avaient ete desactives a la fermeture
 *    et personne ne les reactivait. Le cas se produit des qu'un choix multiple est modifie deux
 *    fois, ce qui est la situation courante sur "Autre, preciser" ;
 *  - une liste deroulante d'un bloc masque conservait son choix. Une liste EST un ViewGroup, le
 *    parcours recursif la traitait comme un conteneur et ne la remettait jamais a zero : la
 *    valeur devenue sans objet partait au serveur.
 *
 * Les vues sont construites en code plutot qu'inflatees : c'est la logique de bascule qui est
 * testee, pas une mise en page, et cela evite d'embarquer un theme applicatif dans le test.
 */
@RunWith(AndroidJUnit4::class)
class SaisiePfteBasculerTest {

    private lateinit var contexte: Context
    private lateinit var bloc: LinearLayout
    private lateinit var champ: EditText
    private lateinit var liste: Spinner
    private lateinit var champImbrique: EditText

    @Before
    fun preparer() {
        contexte = ContextThemeWrapper(
            InstrumentationRegistry.getInstrumentation().context,
            android.R.style.Theme_Material_Light
        )

        champ = EditText(contexte)
        liste = Spinner(contexte).apply {
            adapter = ArrayAdapter(
                contexte, android.R.layout.simple_spinner_item,
                listOf("Faite un choix !", "Autre, préciser", "Sensibilisation")
            )
        }
        champImbrique = EditText(contexte)

        // Un sous-conteneur : les blocs reels en comportent, chaque champ etant empile avec son
        // libelle.
        val sousBloc = LinearLayout(contexte).apply { addView(champImbrique) }

        bloc = LinearLayout(contexte).apply {
            orientation = LinearLayout.VERTICAL
            addView(champ)
            addView(liste)
            addView(sousBloc)
        }
    }

    // ------------------------------------------------------------------ fermeture

    @Test
    fun fermer_un_bloc_le_masque_et_vide_ses_champs() {
        champ.setText("Cantine scolaire")
        champImbrique.setText("Autre chose")

        SaisiePfte.basculer(bloc, false)

        assertEquals(View.GONE, bloc.visibility)
        assertEquals("", champ.text.toString())
        assertEquals("Un champ imbrique doit etre vide lui aussi", "", champImbrique.text.toString())
    }

    @Test
    fun fermer_un_bloc_remet_ses_listes_sur_la_ligne_d_invite() {
        liste.setSelection(2)
        assertEquals("Preparation", 2, liste.selectedItemPosition)

        SaisiePfte.basculer(bloc, false)

        assertEquals(
            "Une liste masquee doit revenir a la ligne d'invite, sinon son choix part au serveur",
            0, liste.selectedItemPosition
        )
    }

    @Test
    fun fermer_un_bloc_desactive_ses_champs_et_ses_listes() {
        SaisiePfte.basculer(bloc, false)

        assertFalse(champ.isEnabled)
        assertFalse(liste.isEnabled)
        assertFalse(champImbrique.isEnabled)
    }

    // ------------------------------------------------------------------ reouverture

    @Test
    fun rouvrir_un_bloc_ferme_rend_ses_champs_saisissables() {
        // Le defaut signale : apres une fermeture, la reouverture laissait le bloc grise.
        SaisiePfte.basculer(bloc, false)
        SaisiePfte.basculer(bloc, true)

        assertEquals(View.VISIBLE, bloc.visibility)
        assertTrue("Le champ doit redevenir saisissable", champ.isEnabled)
        assertTrue("Y compris un champ imbrique", champImbrique.isEnabled)
        assertTrue("Et les listes", liste.isEnabled)
    }

    @Test
    fun une_saisie_faite_apres_reouverture_est_bien_conservee() {
        SaisiePfte.basculer(bloc, false)
        SaisiePfte.basculer(bloc, true)

        champ.setText("Autre mesure precisee")

        assertEquals("Autre mesure precisee", champ.text.toString())
    }

    @Test
    fun plusieurs_allers_retours_ne_degradent_rien() {
        // Une regle conditionnelle se declenche a chaque changement de selection : le bloc peut
        // etre ferme et rouvert des dizaines de fois pendant une seule enquete.
        repeat(5) {
            SaisiePfte.basculer(bloc, false)
            SaisiePfte.basculer(bloc, true)
        }

        assertTrue(champ.isEnabled)
        assertTrue(liste.isEnabled)
        assertEquals(View.VISIBLE, bloc.visibility)
    }

    @Test
    fun rouvrir_ne_restitue_pas_ce_que_la_fermeture_avait_efface() {
        champ.setText("Valeur devenue sans objet")

        SaisiePfte.basculer(bloc, false)
        SaisiePfte.basculer(bloc, true)

        assertEquals(
            "La valeur a bien ete abandonnee : c'est le but de la fermeture",
            "", champ.text.toString()
        )
    }

    // ------------------------------------------------------------------ robustesse

    @Test
    fun basculer_un_bloc_absent_ne_plante_pas() {
        SaisiePfte.basculer(null, true)
        SaisiePfte.basculer(null, false)
    }

    @Test
    fun une_liste_sans_adaptateur_ne_plante_pas() {
        val vide = LinearLayout(contexte).apply { addView(Spinner(contexte)) }
        SaisiePfte.basculer(vide, false)
        assertEquals(View.GONE, vide.visibility)
    }
}
