# -*- coding: utf-8 -*-
"""
Gabarit commun des mises en page PFTE.

Les trois modules - Enquete Menage, Visite Plantation, Suivi Enfant Travailleur - dessinent leurs
ecrans de la meme facon : meme cadre arrondi, meme espacement, memes composants de saisie. Ces
fonctions sont donc partagees, et non recopiees : une retouche de gabarit doit valoir pour les
trois, sinon les formulaires divergent au premier ajustement.

Chaque generateur de module importe ce fichier et n'ecrit que ce qui lui est propre : ses sections,
ses champs et ses regles.
"""
RES = r"E:\Projets\PROJET_MOBILE_PROGRAMME_BANDAMA\ccbm\app\src\main\res\layout"

I = "    "


def ind(n):
    return I * n


def label(texte, n, blanc=True):
    couleur = '\n' + ind(n + 1) + 'android:textColor="@color/text_color_white"' if blanc else ''
    return (
        ind(n) + '<androidx.appcompat.widget.AppCompatTextView\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:paddingLeft="@dimen/_12sdp"\n' +
        ind(n + 1) + 'android:text="%s"%s\n' % (texte, couleur) +
        ind(n + 1) + 'android:textStyle="bold" />\n'
    )


def espace(n):
    return (ind(n) + '<Space\n' +
            ind(n + 1) + 'android:layout_width="@dimen/_5sdp"\n' +
            ind(n + 1) + 'android:layout_height="@dimen/_10sdp" />\n')


def spinner(idv, tag, n, multi=False):
    classe = ('ci.progbandama.mobile.activities.forms.views.MultiSelectSpinner' if multi
              else 'com.toptoche.searchablespinnerlibrary.SearchableSpinner')
    tagattr = (ind(n + 2) + 'android:tag="%s"\n' % tag) if tag else ''
    return (
        ind(n) + '<LinearLayout\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:background="@drawable/rounded_background">\n\n' +
        espace(n + 1) + '\n' +
        ind(n + 1) + '<%s\n' % classe +
        tagattr +
        ind(n + 2) + 'android:id="@+id/%s"\n' % idv +
        ind(n + 2) + 'android:layout_width="match_parent"\n' +
        ind(n + 2) + 'android:layout_height="@dimen/_37sdp"\n' +
        ind(n + 2) + 'android:spinnerMode="dialog" />\n\n' +
        ind(n) + '</LinearLayout>\n'
    )


def edit(idv, tag, n, hint="", numerique=False, lecture_seule=False):
    tagattr = (ind(n + 1) + 'android:tag="%s"\n' % tag) if tag else ''
    saisie = 'number' if numerique else 'textCapCharacters'
    verrou = ''
    if lecture_seule:
        verrou = (ind(n + 1) + 'android:focusable="false"\n' +
                  ind(n + 1) + 'android:clickable="false"\n')
    return (
        ind(n) + '<androidx.appcompat.widget.AppCompatEditText\n' +
        tagattr +
        ind(n + 1) + 'android:id="@+id/%s"\n' % idv +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="@dimen/_37sdp"\n' +
        ind(n + 1) + 'android:background="@drawable/rounded_background"\n' +
        ind(n + 1) + 'android:ellipsize="end"\n' +
        ind(n + 1) + 'android:hint="%s"\n' % hint +
        ind(n + 1) + 'android:imeOptions="actionDone"\n' +
        ind(n + 1) + 'android:inputType="%s"\n' % saisie +
        verrou +
        ind(n + 1) + 'android:maxLines="1"\n' +
        ind(n + 1) + 'android:paddingStart="@dimen/_10sdp"\n' +
        ind(n + 1) + 'android:paddingEnd="@dimen/_10sdp"\n' +
        ind(n + 1) + 'android:textSize="@dimen/page_input_content_dim13" />\n'
    )


def champ_date(idv, tag, n, hint="JJ-MM-AAAA"):
    """Champ de date : non saisissable, ouvert par un selecteur, comme les formulaires existants."""
    return edit(idv, tag, n, hint=hint, lecture_seule=True)


def bloc(idv, contenu, n, visible=False):
    """Conteneur conditionnel. Masque par defaut : c'est une regle qui le fera apparaitre."""
    return (
        ind(n) + '<LinearLayout\n' +
        ind(n + 1) + 'android:id="@+id/%s"\n' % idv +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:orientation="vertical"\n' +
        ind(n + 1) + 'android:visibility="%s">\n\n' % ('visible' if visible else 'gone') +
        contenu +
        ind(n) + '</LinearLayout>\n'
    )


def groupe(contenu, n):
    """Simple empilement vertical, sans identifiant : sert a tenir un libelle et son champ."""
    return (
        ind(n) + '<LinearLayout\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:orientation="vertical">\n\n' +
        contenu +
        ind(n) + '</LinearLayout>\n'
    )


def champ(titre, corps, n):
    return groupe(label(titre, n + 1) + '\n' + corps, n) + '\n' + espace(n) + '\n'


def carte(idv, titre, contenu, n, visible=True):
    entete = (
        ind(n + 2) + '<androidx.appcompat.widget.AppCompatTextView\n' +
        ind(n + 3) + 'android:layout_width="match_parent"\n' +
        ind(n + 3) + 'android:layout_height="wrap_content"\n' +
        ind(n + 3) + 'android:paddingBottom="@dimen/_8sdp"\n' +
        ind(n + 3) + 'android:text="%s"\n' % titre +
        ind(n + 3) + 'android:textColor="@color/text_color_white"\n' +
        ind(n + 3) + 'android:textSize="@dimen/page_title_dim14"\n' +
        ind(n + 3) + 'android:textStyle="bold" />\n\n'
    )
    return (
        ind(n) + '<androidx.cardview.widget.CardView\n' +
        ind(n + 1) + 'android:id="@+id/%s"\n' % idv +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:layout_marginStart="@dimen/_8sdp"\n' +
        ind(n + 1) + 'android:layout_marginEnd="@dimen/_8sdp"\n' +
        ind(n + 1) + 'android:layout_marginBottom="@dimen/_10sdp"\n' +
        ind(n + 1) + 'android:visibility="%s"\n' % ('visible' if visible else 'gone') +
        ind(n + 1) + 'app:cardBackgroundColor="@color/progband_gris"\n' +
        ind(n + 1) + 'app:cardCornerRadius="@dimen/_8sdp">\n\n' +
        ind(n + 1) + '<LinearLayout\n' +
        ind(n + 2) + 'android:layout_width="match_parent"\n' +
        ind(n + 2) + 'android:layout_height="wrap_content"\n' +
        ind(n + 2) + 'android:orientation="vertical"\n' +
        ind(n + 2) + 'android:padding="@dimen/_10sdp">\n\n' +
        entete + contenu +
        ind(n + 1) + '</LinearLayout>\n\n' +
        ind(n) + '</androidx.cardview.widget.CardView>\n\n'
    )


def banniere(n, texte="@string/information_enquete_menage", idv="labelInformationMenage"):
    """
    Bandeau d'information en tete de formulaire.

    Rappelle ce que l'enquete depiste et dans quel ordre la remplir : l'ordre n'est pas libre,
    le filtre et le consentement commandent l'ouverture de tout le reste.
    """
    return (
        ind(n) + '<LinearLayout\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:layout_marginStart="@dimen/_8sdp"\n' +
        ind(n + 1) + 'android:layout_marginEnd="@dimen/_8sdp"\n' +
        ind(n + 1) + 'android:layout_marginBottom="@dimen/_10sdp"\n' +
        ind(n + 1) + 'android:background="@drawable/banniere_information"\n' +
        ind(n + 1) + 'android:orientation="vertical"\n' +
        ind(n + 1) + 'android:padding="@dimen/_10sdp">\n\n' +
        ind(n + 1) + '<androidx.appcompat.widget.AppCompatTextView\n' +
        ind(n + 2) + 'android:layout_width="match_parent"\n' +
        ind(n + 2) + 'android:layout_height="wrap_content"\n' +
        ind(n + 2) + 'android:text="@string/important_titre"\n' +
        ind(n + 2) + 'android:textSize="@dimen/page_title_dim14"\n' +
        ind(n + 2) + 'android:textStyle="bold" />\n\n' +
        ind(n + 1) + '<androidx.appcompat.widget.AppCompatTextView\n' +
        ind(n + 2) + 'android:id="@+id/%s"\n' % idv +
        ind(n + 2) + 'android:layout_width="match_parent"\n' +
        ind(n + 2) + 'android:layout_height="wrap_content"\n' +
        ind(n + 2) + 'android:layout_marginTop="@dimen/_4sdp"\n' +
        ind(n + 2) + 'android:text="%s"\n' % texte +
        ind(n + 2) + 'android:textSize="@dimen/page_input_title_dim11" />\n\n' +
        ind(n) + '</LinearLayout>\n\n'
    )


def bouton(idv, texte, n, couleur="@color/bggreen"):
    """Meme composant Material que les boutons des formulaires existants."""
    return (
        ind(n) + '<com.google.android.material.button.MaterialButton\n' +
        ind(n + 1) + 'android:id="@+id/%s"\n' % idv +
        ind(n + 1) + 'style="@style/OptionsButton_Active"\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="@dimen/_40sdp"\n' +
        ind(n + 1) + 'android:layout_marginTop="@dimen/_6sdp"\n' +
        ind(n + 1) + 'android:backgroundTint="%s"\n' % couleur +
        ind(n + 1) + 'android:elevation="0dp"\n' +
        ind(n + 1) + 'android:text="%s"\n' % texte +
        ind(n + 1) + 'android:textColor="@color/text_color_white"\n' +
        ind(n + 1) + 'android:textSize="@dimen/page_button_text_dim13"\n' +
        ind(n + 1) + 'app:cornerRadius="@dimen/_37sdp" />\n\n'
    )

def alerte(idv, texte, n):
    return (
        ind(n) + '<androidx.appcompat.widget.AppCompatTextView\n' +
        ind(n + 1) + 'android:id="@+id/%s"\n' % idv +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:background="@drawable/rounded_background"\n' +
        ind(n + 1) + 'android:padding="@dimen/_10sdp"\n' +
        ind(n + 1) + 'android:text="%s"\n' % texte +
        ind(n + 1) + 'android:textStyle="bold"\n' +
        ind(n + 1) + 'android:visibility="gone" />\n\n'
    )
