# -*- coding: utf-8 -*-
"""
Genere les deux mises en page du module Visite Plantation (lot 4).

Meme demarche que pour l'Enquete Menage, et meme gabarit : voir gabarit_pfte.py. Seules changent
les sections propres a ce module - le jour de visite, les caracteristiques de la plantation, et les
trois indicateurs de temps de travail de la fiche enfant.
"""
import io
import os

from gabarit_pfte import (RES, alerte, banniere, bloc, bouton, carte, champ, champ_date, edit,
                          espace, ind, label, spinner)


# ---------------------------------------------------------------- ecran principal

def ecran():
    n = 4  # niveau d'indentation a l'interieur d'une carte

    identification = (
        champ("Section", spinner("selectSectionVp", "sectionId", n), n) +
        champ("Localite", spinner("selectLocaliteVp", "localiteId", n), n) +
        champ("Producteur", spinner("selectProducteurVp", "producteurId", n), n) +
        champ("Sexe du producteur", edit("editSexeProducteurVp", None, n, hint="-", lecture_seule=True), n) +
        champ("Code producteur", edit("editCodeProducteurVp", None, n, hint="-", lecture_seule=True), n) +
        champ("Date de la visite", champ_date("editDateEnqueteVp", "dateEnquete", n), n) +
        champ("Jour de la visite", spinner("selectJourVisiteVp", "jourVisite", n), n) +
        champ("Nom de l'enqueteur", edit("editNomEnqueteurVp", "nomEnqueteur", n, hint="KOUAME", lecture_seule=True), n) +
        champ("Latitude", edit("editLatitudeVp", "latitude", n, hint="-", lecture_seule=True), n) +
        champ("Longitude", edit("editLongitudeVp", "longitude", n, hint="-", lecture_seule=True), n) +
        champ("Altitude", edit("editAltitudeVp", "altitude", n, hint="-", lecture_seule=True), n) +
        champ("Precision GPS", edit("editPrecisionGpsVp", "precisionGps", n, hint="-", lecture_seule=True), n) +
        bouton("btnCapturerPositionVp", "Capturer la position", n)
    )

    # Le nom et le titre du repondant sont ici dans un meme bloc, contrairement a l'Enquete Menage :
    # quand le producteur repond lui-meme, le back-office recalcule son nom et n'attend rien.
    filtre = (
        champ("Le producteur est-il le repondant ?", spinner("selectEstRepondantVp", "estProducteurRepondant", n), n) +
        bloc("blocRepondantVp",
             champ("Nom du repondant", edit("editNomRepondantVp", "nomRepondant", n + 1, hint="KOFFI YAO"), n + 1) +
             champ("Titre du repondant", spinner("selectTitreRepondantVp", "titreRepondant", n + 1), n + 1), n) + '\n' +
        champ("Le producteur est-il disponible ?", spinner("selectProducteurDisponibleVp", "producteurDisponible", n), n) +
        bloc("blocIndisponibleVp",
             champ("Raison de l'indisponibilite", spinner("selectRaisonIndisponibiliteVp", "raisonIndisponibilite", n + 1), n + 1) +
             bloc("blocDatePlanificationVp",
                  champ("Date de replanification", champ_date("editDatePlanificationVp", "datePlanification", n + 2), n + 2), n + 1) + '\n' +
             bloc("blocRaisonRefusVp",
                  champ("Raison(s) du refus", spinner("selectRaisonRefusVp", None, n + 2, multi=True), n + 2), n + 1) + '\n' +
             bloc("blocAutreRaisonRefusVp",
                  champ("Autre raison du refus", edit("editAutreRaisonRefusVp", "autreRaisonRefus", n + 2, hint="Preciser"), n + 2), n + 1),
             n) + '\n' +
        bloc("blocConsentementVp",
             champ("Consentement du producteur", spinner("selectConsentementVp", "consentement", n + 1), n + 1), n) + '\n' +
        alerte("labelFinEnqueteVp",
               "La visite s'arrete ici. Enregistrez pour conserver le motif.", n)
    )

    plantation = (
        champ("Superficie de la plantation (ha)",
              edit("editSuperficieVp", "superficiePlantation", n, hint="0", numerique=True), n) +
        champ("Nombre de manoeuvres permanents",
              edit("editManoeuvresPermanentsVp", "nombreManoeuvresPermanents", n, hint="0", numerique=True), n) +
        champ("Des manoeuvres permanents ont-ils moins de 18 ans ?",
              spinner("selectPermanentsMoins18Vp", "manoeuvresPermanentsMoins18", n), n) +
        champ("Nombre de manoeuvres journaliers",
              edit("editManoeuvresJournaliersVp", "nombreManoeuvresJournaliers", n, hint="0", numerique=True), n) +
        champ("Des manoeuvres journaliers ont-ils moins de 18 ans ?",
              spinner("selectJournaliersMoins18Vp", "manoeuvresJournaliersMoins18", n), n) +
        champ("Nombre d'enfants de 0 a 4 ans trouves",
              edit("editNombreEnfants0a4Vp", "nombreEnfants0a4", n, hint="0", numerique=True), n) +
        champ("Nombre d'enfants de 5 a 17 ans trouves",
              edit("editNombreEnfants5a17Vp", "nombreEnfants5a17", n, hint="0", numerique=True), n) +
        champ("Nombre total de personnes trouvees",
              edit("editNombrePersonnesVp", "nombrePersonnesTrouvees", n, hint="0", numerique=True), n)
    )

    # Aucun bouton d'ajout : le nombre de fiches suit le nombre d'enfants de 5 a 17 ans trouves.
    # Le back-office a bien un bouton, mais il est code en display:none et jamais reaffiche.
    enfants = (
        label("Une fiche est creee par enfant de 5 a 17 ans declare ci-dessus.", n) + '\n' +
        espace(n) + '\n' +
        ind(n) + '<LinearLayout\n' +
        ind(n + 1) + 'android:id="@+id/conteneurFichesEnfantVp"\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:orientation="vertical" />\n\n'
    )

    corps = (
        banniere(3, "@string/information_visite_plantation", "labelInformationVp") +
        carte("cardIdentificationVp", "IDENTIFICATION", identification, 3) +
        carte("cardFiltreVp", "FILTRE ET CONSENTEMENT", filtre, 3) +
        ind(3) + '<LinearLayout\n' +
        ind(4) + 'android:id="@+id/blocSuiteFormulaireVp"\n' +
        ind(4) + 'android:layout_width="match_parent"\n' +
        ind(4) + 'android:layout_height="wrap_content"\n' +
        ind(4) + 'android:orientation="vertical"\n' +
        ind(4) + 'android:visibility="gone">\n\n' +
        carte("cardPlantationVp", "CARACTERISTIQUES DE LA PLANTATION", plantation, 4) +
        carte("cardEnfantsVp", "ENFANT(S) TROUVE(S) DANS LA PLANTATION", enfants, 4, visible=False) +
        ind(3) + '</LinearLayout>\n\n' +
        ind(3) + '<LinearLayout\n' +
        ind(4) + 'android:layout_width="match_parent"\n' +
        ind(4) + 'android:layout_height="wrap_content"\n' +
        ind(4) + 'android:layout_marginStart="@dimen/_8sdp"\n' +
        ind(4) + 'android:layout_marginEnd="@dimen/_8sdp"\n' +
        ind(4) + 'android:layout_marginBottom="@dimen/_16sdp"\n' +
        ind(4) + 'android:orientation="vertical">\n\n' +
        bouton("btnEnregistrerVp", "Enregistrer la visite", 4) +
        bouton("btnBrouillonVp", "Mettre au brouillon", 4, couleur="@color/progband_gris") +
        ind(3) + '</LinearLayout>\n\n'
    )

    entete = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!--\n'
        '     Visite Plantation (PFTE) - lot 4.\n'
        '\n'
        '     Memes conventions que l\'Enquete Menage : blocs conditionnels masques a l\'ouverture,\n'
        '     android:tag portant le nom de la propriete du modele.\n'
        '\n'
        '     Une difference tient a la place : ce module n\'a pas de bouton "Ajouter un enfant". Le\n'
        '     nombre de fiches suit le nombre d\'enfants de 5 a 17 ans trouves, comme sur le web ou le\n'
        '     bouton existe mais reste masque en permanence.\n'
        '-->\n'
        '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:app="http://schemas.android.com/apk/res-auto"\n'
        '    xmlns:tools="http://schemas.android.com/tools"\n'
        '    android:id="@+id/layout_visite_plantation"\n'
        '    android:layout_width="match_parent"\n'
        '    android:layout_height="match_parent"\n'
        '    android:orientation="vertical"\n'
        '    tools:context=".activities.forms.VisitePlantationActivity">\n\n'
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
        '            android:text="VISITE PLANTATION"\n'
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
        '        android:id="@+id/scrollVisitePlantation"\n'
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
        ind(n) + '<androidx.appcompat.widget.AppCompatTextView\n' +
        ind(n + 1) + 'android:id="@+id/labelNumeroEnfantVp"\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="wrap_content"\n' +
        ind(n + 1) + 'android:text="Enfant n1"\n' +
        ind(n + 1) + 'android:textColor="@color/text_color_white"\n' +
        ind(n + 1) + 'android:textSize="@dimen/page_title_dim14"\n' +
        ind(n + 1) + 'android:textStyle="bold" />\n\n' +
        espace(n) + '\n'
    )

    corps = (
        entete_fiche +
        champ("Nom et prenoms", edit("editNomEnfantVp", None, n, hint="KOFFI AYA"), n) +
        champ("Date de naissance", champ_date("editDateNaissanceEnfantVp", None, n), n) +
        champ("Sexe", spinner("selectSexeEnfantVp", None, n), n) +
        champ("Lien de parente", spinner("selectLienParenteEnfantVp", None, n), n) +
        bloc("blocAutreLienParenteEnfantVp",
             champ("Autre lien de parente", edit("editAutreLienParenteEnfantVp", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Raison pour laquelle l'enfant ne vit pas avec ses parents",
              spinner("selectRaisonNeVitPasParentsEnfantVp", None, n), n) +
        bloc("blocAutreRaisonNeVitPasParentsEnfantVp",
             champ("Autre raison", edit("editAutreRaisonNeVitPasParentsEnfantVp", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Situation scolaire", spinner("selectSituationScolaireEnfantVp", None, n), n) +
        bloc("blocNiveauScolaireEnfantVp",
             champ("Classe", spinner("selectNiveauScolaireEnfantVp", None, n + 1), n + 1), n) + '\n' +
        bloc("blocRaisonNonScolarisationEnfantVp",
             champ("Raison(s) de non-scolarisation", spinner("selectRaisonNonScolarisationEnfantVp", None, n + 1, multi=True), n + 1), n) + '\n' +
        bloc("blocAutreRaisonNonScolarisationEnfantVp",
             champ("Autre raison de non-scolarisation", edit("editAutreRaisonNonScolarisationEnfantVp", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Dispose d'un extrait de naissance ?", spinner("selectExtraitNaissanceEnfantVp", None, n), n) +
        bloc("blocRaisonPasExtraitEnfantVp",
             champ("Raison(s) de l'absence d'extrait", spinner("selectRaisonPasExtraitEnfantVp", None, n + 1, multi=True), n + 1), n) + '\n' +
        # Les trois indicateurs de temps de travail, propres a ce module.
        champ("Heures de travail par semaine",
              edit("editHeuresTravailSemaineEnfantVp", None, n, hint="0", numerique=True), n) +
        champ("Nombre de jours travailles par semaine", spinner("selectJoursTravailEnfantVp", None, n), n) +
        champ("Heures travaillees la journee la plus chargee",
              spinner("selectHeuresJourneeEnfantVp", None, n), n) +
        champ("Situation(s) PFTE constatee(s)", spinner("selectSituationsPfteEnfantVp", None, n, multi=True), n) +
        bloc("blocRaisonTravailAbusEnfantVp",
             champ("Raison(s) du travail ou de l'abus", spinner("selectRaisonTravailAbusEnfantVp", None, n + 1, multi=True), n + 1), n) + '\n' +
        bloc("blocAutreRaisonTravailAbusEnfantVp",
             champ("Autre raison", edit("editAutreRaisonTravailAbusEnfantVp", None, n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Mesure(s) au niveau de l'enfant", spinner("selectMesuresEnfantEnfantVp", None, n, multi=True), n) +
        champ("Mesure(s) au niveau du menage", spinner("selectMesuresMenageEnfantVp", None, n, multi=True), n) +
        champ("Mesure(s) au niveau de la communaute", spinner("selectMesuresCommunauteEnfantVp", None, n, multi=True), n) +
        bloc("blocAutreMesureEnfantVp",
             champ("Autre mesure", edit("editAutreMesureEnfantVp", None, n + 1, hint="Preciser"), n + 1), n) + '\n'
    )

    return (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!--\n'
        '     Fiche enfant d\'une Visite Plantation - lot 4.\n'
        '\n'
        '     Gabarit repete par FichesEnfant, une fois par enfant de 5 a 17 ans trouve. Aucun bouton\n'
        '     de retrait : le nombre de fiches est pilote par l\'effectif declare, retirer une fiche\n'
        '     isolee le contredirait.\n'
        '\n'
        '     Trois champs absents de l\'Enquete Menage figurent ici : les indicateurs de temps de\n'
        '     travail, coeur du depistage. Voir la note de VisitePlantationModel sur leur sort actuel\n'
        '     cote serveur.\n'
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


for nom, contenu in (("activity_visite_plantation.xml", ecran()),
                     ("fiche_enfant_plantation.xml", fiche())):
    chemin = os.path.join(RES, nom)
    io.open(chemin, "w", encoding="utf-8", newline="\n").write(contenu)
    print("%s : %d lignes" % (nom, contenu.count("\n")))
