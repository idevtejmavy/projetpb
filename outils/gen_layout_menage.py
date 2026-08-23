# -*- coding: utf-8 -*-
"""
Genere les deux mises en page du module Enquete Menage (lot 3).

Ecrit plutot que recopie a la main : le formulaire compte une cinquantaine de champs et une
vingtaine de blocs conditionnels, tous batis sur le meme gabarit que les formulaires existants.
Un generateur garantit que ce gabarit est identique partout, ce qu'une saisie manuelle de
3 000 lignes de XML ne garantit pas.
"""
import io, os

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


# ---------------------------------------------------------------- ecran principal

def ecran():
    n = 4  # niveau d'indentation a l'interieur d'une carte

    identification = (
        champ("Section", spinner("selectSectionMenage", "sectionId", n), n) +
        champ("Localite", spinner("selectLocaliteMenage", "localiteId", n), n) +
        champ("Producteur", spinner("selectProducteurMenage", "producteurId", n), n) +
        champ("Sexe du producteur", edit("editSexeProducteurMenage", None, n, hint="-", lecture_seule=True), n) +
        champ("Code producteur", edit("editCodeProducteurMenage", None, n, hint="-", lecture_seule=True), n) +
        champ("Date de l'enquete", champ_date("editDateEnqueteMenage", "dateEnquete", n), n) +
        champ("Nom de l'enqueteur", edit("editNomEnqueteurMenage", "nomEnqueteur", n, hint="KOUAME", lecture_seule=True), n) +
        champ("Latitude", edit("editLatitudeMenage", "latitude", n, hint="-", lecture_seule=True), n) +
        champ("Longitude", edit("editLongitudeMenage", "longitude", n, hint="-", lecture_seule=True), n) +
        champ("Altitude", edit("editAltitudeMenage", "altitude", n, hint="-", lecture_seule=True), n) +
        champ("Precision GPS", edit("editPrecisionGpsMenage", "precisionGps", n, hint="-", lecture_seule=True), n) +
        bouton("btnCapturerPositionMenage", "Capturer la position", n)
    )

    filtre = (
        champ("Le producteur est-il le repondant ?", spinner("selectEstRepondantMenage", "estProducteurRepondant", n), n) +
        champ("Nom du repondant", edit("editNomRepondantMenage", "nomRepondant", n, hint="KOFFI YAO"), n) +
        bloc("blocTitreRepondantMenage",
             champ("Titre du repondant", spinner("selectTitreRepondantMenage", "titreRepondant", n + 1), n + 1), n) + '\n' +
        champ("Le producteur est-il disponible ?", spinner("selectProducteurDisponibleMenage", "producteurDisponible", n), n) +
        bloc("blocIndisponibleMenage",
             champ("Raison de l'indisponibilite", spinner("selectRaisonIndisponibiliteMenage", "raisonIndisponibilite", n + 1), n + 1) +
             bloc("blocDatePlanificationMenage",
                  champ("Date de replanification", champ_date("editDatePlanificationMenage", "datePlanification", n + 2), n + 2), n + 1) + '\n' +
             bloc("blocRaisonRefusMenage",
                  champ("Raison(s) du refus", spinner("selectRaisonRefusMenage", None, n + 2, multi=True), n + 2), n + 1) + '\n' +
             bloc("blocAutreRaisonRefusMenage",
                  champ("Autre raison du refus", edit("editAutreRaisonRefusMenage", "autreRaisonRefus", n + 2, hint="Preciser"), n + 2), n + 1),
             n) + '\n' +
        bloc("blocConsentementMenage",
             champ("Consentement du producteur", spinner("selectConsentementMenage", "consentement", n + 1), n + 1), n) + '\n' +
        alerte("labelFinEnqueteMenage",
               "L'enquete s'arrete ici. Enregistrez pour conserver le motif.", n)
    )

    menage = (
        champ("Situation matrimoniale", spinner("selectSituationMatrimonialeMenage", "situationMatrimoniale", n), n) +
        champ("Nombre d'adultes", edit("editNombreAdultesMenage", "nombreAdultes", n, hint="0", numerique=True), n) +
        champ("Nombre d'enfants de 0 a 4 ans", edit("editNombreEnfants0a4Menage", "nombreEnfants0a4", n, hint="0", numerique=True), n) +
        champ("Nombre d'enfants de 5 a 17 ans", edit("editNombreEnfants5a17Menage", "nombreEnfants5a17", n, hint="0", numerique=True), n) +
        champ("Total des personnes du menage", edit("editTotalPersonnesMenage", None, n, hint="0", lecture_seule=True), n) +
        champ("Y a-t-il des enfants a charge ?", spinner("selectAEnfantAChargeMenage", "aEnfantACharge", n), n) +
        bloc("blocNombreEnfantsAChargeMenage",
             champ("Nombre d'enfants a charge", edit("editNombreEnfantsAChargeMenage", "nombreEnfantsACharge", n + 1, hint="0", numerique=True), n + 1), n)
    )

    enfants = (
        ind(n) + '<LinearLayout\n' +
        ind(n + 1) + 'android:id="@+id/conteneurFichesEnfantMenage"\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:orientation="vertical" />\n\n' +
        bouton("btnAjouterEnfantMenage", "Ajouter un enfant", n)
    )

    sensibilisation = (
        champ("Themes abordes", spinner("selectThemesSensibilisationMenage", None, n, multi=True), n) +
        bloc("blocAutreThemeMenage",
             champ("Autre theme", edit("editAutreThemeMenage", "autreThemeSensibilisation", n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Outils utilises", spinner("selectOutilsSensibilisationMenage", None, n, multi=True), n) +
        champ("Nombre d'hommes sensibilises", edit("editNombreHommesMenage", "nombreHommesSensibilises", n, hint="0", numerique=True), n) +
        champ("Nombre de femmes sensibilisees", edit("editNombreFemmesMenage", "nombreFemmesSensibilisees", n, hint="0", numerique=True), n) +
        champ("Nombre de garcons sensibilises", edit("editNombreGarconsMenage", "nombreGarconsSensibilises", n, hint="0", numerique=True), n) +
        champ("Nombre de filles sensibilisees", edit("editNombreFillesMenage", "nombreFillesSensibilisees", n, hint="0", numerique=True), n) +
        champ("Total des personnes sensibilisees", edit("editTotalSensibiliseesMenage", None, n, hint="0", lecture_seule=True), n) +
        champ("Telephone du producteur", edit("editTelephoneSensibilisationMenage", "telephoneProducteurSensibilisation", n, hint="0700000000"), n) +
        label("Photo de la sensibilisation", n) + '\n' +
        ind(n) + '<androidx.appcompat.widget.AppCompatImageView\n' +
        ind(n + 1) + 'android:id="@+id/imagePhotoSensibilisationMenage"\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="@dimen/_120sdp"\n' +
        ind(n + 1) + 'android:background="@drawable/rounded_background"\n' +
        ind(n + 1) + 'android:padding="@dimen/_4sdp"\n' +
        ind(n + 1) + 'android:scaleType="centerInside" />\n\n' +
        bouton("btnPrendrePhotoMenage", "Ajouter la photo", n)
    )

    corps = (
        carte("cardIdentificationMenage", "IDENTIFICATION", identification, 3) +
        carte("cardFiltreMenage", "FILTRE ET CONSENTEMENT", filtre, 3) +
        ind(3) + '<LinearLayout\n' +
        ind(4) + 'android:id="@+id/blocSuiteFormulaireMenage"\n' +
        ind(4) + 'android:layout_width="match_parent"\n' +
        ind(4) + 'android:layout_height="wrap_content"\n' +
        ind(4) + 'android:orientation="vertical"\n' +
        ind(4) + 'android:visibility="gone">\n\n' +
        carte("cardMenageMenage", "CARACTERISTIQUES DU MENAGE", menage, 4) +
        carte("cardEnfantsMenage", "ENFANT(S) DU MENAGE", enfants, 4, visible=False) +
        carte("cardSensibilisationMenage", "SENSIBILISATION DE PROXIMITE", sensibilisation, 4, visible=False) +
        ind(3) + '</LinearLayout>\n\n' +
        ind(3) + '<LinearLayout\n' +
        ind(4) + 'android:layout_width="match_parent"\n' +
        ind(4) + 'android:layout_height="wrap_content"\n' +
        ind(4) + 'android:layout_marginStart="@dimen/_8sdp"\n' +
        ind(4) + 'android:layout_marginEnd="@dimen/_8sdp"\n' +
        ind(4) + 'android:layout_marginBottom="@dimen/_16sdp"\n' +
        ind(4) + 'android:orientation="vertical">\n\n' +
        bouton("btnEnregistrerMenage", "Enregistrer l'enquete", 4) +
        bouton("btnBrouillonMenage", "Mettre au brouillon", 4, couleur="@color/progband_gris") +
        ind(3) + '</LinearLayout>\n\n'
    )

    entete = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!--\n'
        '     Enquete Menage (PFTE) - lot 3.\n'
        '\n'
        '     Les blocs conditionnels sont masques a l\'ouverture : c\'est une regle de SaisiePfte qui\n'
        '     les fait apparaitre. Masquer ne suffit pas, le bloc est aussi vide et desactive, comme le\n'
        '     fait le back-office - sinon une valeur rendue sans objet par un autre choix partirait\n'
        '     quand meme au serveur.\n'
        '\n'
        '     Les android:tag portent le nom de la propriete du modele : c\'est ce que lit le binding\n'
        '     par reflexion de Commons.getAllTitleAndValueViews() pour l\'ecran de recapitulatif.\n'
        '-->\n'
        '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:app="http://schemas.android.com/apk/res-auto"\n'
        '    xmlns:tools="http://schemas.android.com/tools"\n'
        '    android:id="@+id/layout_enquete_menage"\n'
        '    android:layout_width="match_parent"\n'
        '    android:layout_height="match_parent"\n'
        '    android:orientation="vertical"\n'
        '    tools:context=".activities.forms.EnqueteMenageActivity">\n\n'
        '    <RelativeLayout\n'
        '        android:id="@+id/linealToolbar"\n'
        '        android:layout_width="match_parent"\n'
        '        android:layout_height="@dimen/_70sdp"\n'
        '        android:background="@color/progband_gris">\n\n'
        '        <androidx.appcompat.widget.AppCompatImageView\n'
        '            android:id="@+id/clickCloseBtn"\n'
        '            android:layout_width="@dimen/page_icon_head_dim35"\n'
        '            android:layout_height="@dimen/page_icon_head_dim35"\n'
        '            android:layout_alignParentStart="true"\n'
        '            android:layout_centerVertical="true"\n'
        '            app:srcCompat="@drawable/ic_back_black" />\n\n'
        '        <androidx.appcompat.widget.AppCompatTextView\n'
        '            android:id="@+id/labelTitleMenuAction"\n'
        '            android:layout_width="wrap_content"\n'
        '            android:layout_height="wrap_content"\n'
        '            android:layout_centerInParent="true"\n'
        '            android:text="ENQUETE MENAGE"\n'
        '            android:textColor="@color/text_color_white"\n'
        '            android:textSize="@dimen/page_title_dim14"\n'
        '            android:textStyle="bold" />\n\n'
        '        <androidx.appcompat.widget.AppCompatImageView\n'
        '            android:id="@+id/imageDraftBtn"\n'
        '            android:layout_width="@dimen/page_icon_head_dim35"\n'
        '            android:layout_height="@dimen/page_icon_head_dim35"\n'
        '            android:layout_alignParentEnd="true"\n'
        '            android:layout_centerVertical="true"\n'
        '            android:padding="@dimen/_7sdp"\n'
        '            android:src="@drawable/baseline_send_and_archive_white" />\n\n'
        '    </RelativeLayout>\n\n'
        '    <ScrollView\n'
        '        android:id="@+id/scrollEnqueteMenage"\n'
        '        android:layout_width="match_parent"\n'
        '        android:layout_height="0dp"\n'
        '        android:layout_weight="1"\n'
        '        android:fillViewport="true">\n\n'
        '        <LinearLayout\n'
        '            android:layout_width="match_parent"\n'
        '            android:layout_height="wrap_content"\n'
        '            android:orientation="vertical"\n'
        '            android:paddingTop="@dimen/_10sdp">\n\n'
    )

    pied = ('        </LinearLayout>\n\n'
            '    </ScrollView>\n\n'
            '</LinearLayout>\n')

    return entete + corps + pied


# ---------------------------------------------------------------- fiche enfant

def fiche():
    n = 2

    entete_fiche = (
        ind(n) + '<LinearLayout\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:gravity="center_vertical"\n' +
        ind(n + 1) + 'android:orientation="horizontal">\n\n' +
        ind(n + 1) + '<androidx.appcompat.widget.AppCompatTextView\n' +
        ind(n + 2) + 'android:id="@+id/labelNumeroEnfant"\n' +
        ind(n + 2) + 'android:layout_width="0dp"\n' +
        ind(n + 2) + 'android:layout_height="wrap_content"\n' +
        ind(n + 2) + 'android:layout_weight="1"\n' +
        ind(n + 2) + 'android:text="Enfant n1"\n' +
        ind(n + 2) + 'android:textColor="@color/text_color_white"\n' +
        ind(n + 2) + 'android:textSize="@dimen/page_title_dim14"\n' +
        ind(n + 2) + 'android:textStyle="bold" />\n\n' +
        ind(n + 1) + '<androidx.appcompat.widget.AppCompatImageView\n' +
        ind(n + 2) + 'android:id="@+id/btnRetirerEnfant"\n' +
        ind(n + 2) + 'android:layout_width="@dimen/_28sdp"\n' +
        ind(n + 2) + 'android:layout_height="@dimen/_28sdp"\n' +
        ind(n + 2) + 'android:clickable="true"\n' +
        ind(n + 2) + 'android:focusable="true"\n' +
        ind(n + 2) + 'android:padding="@dimen/_4sdp"\n' +
        ind(n + 2) + 'app:srcCompat="@drawable/baseline_delete_red_forever_24" />\n\n' +
        ind(n) + '</LinearLayout>\n\n' +
        espace(n) + '\n'
    )

    corps = (
        entete_fiche +
        champ("Nom et prenoms", edit("editNomEnfant", None, n, hint="KOFFI AYA"), n) +
        champ("Date de naissance", champ_date("editDateNaissanceEnfant", None, n), n) +
        champ("Sexe", spinner("selectSexeEnfant", None, n), n) +
        champ("Lien de parente", spinner("selectLienParenteEnfant", None, n), n) +
        bloc("blocAutreLienParenteEnfant",
             champ("Autre lien de parente", edit("editAutreLienParenteEnfant", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Raison pour laquelle l'enfant ne vit pas avec ses parents",
              spinner("selectRaisonNeVitPasParentsEnfant", None, n), n) +
        bloc("blocAutreRaisonNeVitPasParentsEnfant",
             champ("Autre raison", edit("editAutreRaisonNeVitPasParentsEnfant", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Situation scolaire", spinner("selectSituationScolaireEnfant", None, n), n) +
        bloc("blocNiveauScolaireEnfant",
             champ("Classe", spinner("selectNiveauScolaireEnfant", None, n + 1), n + 1), n) + '\n' +
        bloc("blocRaisonNonScolarisationEnfant",
             champ("Raison(s) de non-scolarisation", spinner("selectRaisonNonScolarisationEnfant", None, n + 1, multi=True), n + 1), n) + '\n' +
        bloc("blocAutreRaisonNonScolarisationEnfant",
             champ("Autre raison de non-scolarisation", edit("editAutreRaisonNonScolarisationEnfant", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Dispose d'un extrait de naissance ?", spinner("selectExtraitNaissanceEnfant", None, n), n) +
        bloc("blocRaisonPasExtraitEnfant",
             champ("Raison(s) de l'absence d'extrait", spinner("selectRaisonPasExtraitEnfant", None, n + 1, multi=True), n + 1), n) + '\n' +
        champ("Situation(s) PFTE constatee(s)", spinner("selectSituationsPfteEnfant", None, n, multi=True), n) +
        bloc("blocRaisonTravailAbusEnfant",
             champ("Raison(s) du travail ou de l'abus", spinner("selectRaisonTravailAbusEnfant", None, n + 1, multi=True), n + 1), n) + '\n' +
        bloc("blocAutreRaisonTravailAbusEnfant",
             champ("Autre raison", edit("editAutreRaisonTravailAbusEnfant", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Mesure(s) au niveau de l'enfant", spinner("selectMesuresEnfantEnfant", None, n, multi=True), n) +
        champ("Mesure(s) au niveau du menage", spinner("selectMesuresMenageEnfant", None, n, multi=True), n) +
        champ("Mesure(s) au niveau de la communaute", spinner("selectMesuresCommunauteEnfant", None, n, multi=True), n) +
        bloc("blocAutreMesureEnfant",
             champ("Autre mesure", edit("editAutreMesureEnfant", None, n + 1, hint="Preciser"), n + 1), n) + '\n'
    )

    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!--\n'
        '     Fiche enfant d\'une Enquete Menage - lot 3.\n'
        '\n'
        '     Gabarit repete par FichesEnfant, une fois par enfant enquete. Aucun android:tag ici :\n'
        '     les fiches ne passent pas par le binding par reflexion, qui ne descend pas dans les\n'
        '     sous-formulaires. Chaque fiche est lue et ecrite explicitement par le formulaire.\n'
        '\n'
        '     Les blocs conditionnels sont masques a l\'ouverture et propres a CHAQUE fiche : deux\n'
        '     enfants du meme menage n\'ont pas les memes champs ouverts.\n'
        '-->\n'
        '<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:app="http://schemas.android.com/apk/res-auto"\n'
        '    android:layout_width="match_parent"\n'
        '    android:layout_height="wrap_content"\n'
        '    android:layout_marginBottom="@dimen/_10sdp"\n'
        '    app:cardBackgroundColor="@color/progband_gris"\n'
        '    app:cardCornerRadius="@dimen/_8sdp">\n\n'
        '    <LinearLayout\n'
        '        android:layout_width="match_parent"\n'
        '        android:layout_height="wrap_content"\n'
        '        android:orientation="vertical"\n'
        '        android:padding="@dimen/_10sdp">\n\n'
        + corps +
        '    </LinearLayout>\n\n'
        '</androidx.cardview.widget.CardView>\n'
    )


for nom, contenu in (("activity_enquete_menage.xml", ecran()),
                     ("fiche_enfant_menage.xml", fiche())):
    chemin = os.path.join(RES, nom)
    io.open(chemin, "w", encoding="utf-8", newline="\n").write(contenu)
    print("%s : %d lignes" % (nom, contenu.count("\n")))
