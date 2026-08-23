# -*- coding: utf-8 -*-
"""
Genere la mise en page du module Suivi Enfant Travailleur (lot 5).

Meme gabarit que les deux autres modules : voir gabarit_pfte.py. Un seul fichier ici, sans fiche
repetable - l'enfant suivi EST le sujet du formulaire.

Ce module n'a ni filtre ni consentement, et aucun champ de geolocalisation : il porte sur une
personne deja recensee, pas sur une visite de terrain a un producteur.
"""
import io
import os

from gabarit_pfte import (RES, banniere, bloc, bouton, carte, champ, champ_date, edit, espace,
                          ind, label, spinner)


def ecran():
    n = 4

    # L'enfant commande tout : sa selection pre-remplit quatorze champs de la reevaluation.
    identification = (
        champ("Enfant suivi", spinner("selectEnfantSet", "enfantId", n), n) +
        champ("Producteur", edit("editProducteurSet", None, n, hint="-", lecture_seule=True), n) +
        champ("Localite", edit("editLocaliteSet", None, n, hint="-", lecture_seule=True), n) +
        champ("Date de la visite", champ_date("editDateEnqueteSet", "dateEnquete", n), n) +
        champ("Nom de l'enqueteur",
              edit("editNomEnqueteurSet", "nomEnqueteur", n, hint="KOUAME", lecture_seule=True), n)
    )

    # Le vocabulaire n'est pas une table : c'est la fusion dedoublonnee des trois listes de
    # mesures. Voir SaisiePfte.actionsRemediation().
    remediation = (
        label("Actions menees depuis la derniere visite de suivi.", n) + '\n' +
        espace(n) + '\n' +
        champ("Action(s) de remediation", spinner("selectActionsRemediationSet", None, n, multi=True), n)
    )

    reevaluation = (
        label("Ces informations sont reprises de la derniere enquete. Corrigez ce qui a change.", n) + '\n' +
        espace(n) + '\n' +
        champ("Nom et prenoms", edit("editNomSet", "nom", n, hint="KOFFI AYA"), n) +
        champ("Date de naissance", champ_date("editDateNaissanceSet", "dateNaissance", n), n) +
        champ("Sexe", spinner("selectSexeSet", "sexe", n), n) +
        champ("Lien de parente", spinner("selectLienParenteSet", "lienParente", n), n) +
        bloc("blocAutreLienParenteSet",
             champ("Autre lien de parente",
                   edit("editAutreLienParenteSet", "autreLienParente", n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Raison pour laquelle l'enfant ne vit pas avec ses parents",
              spinner("selectRaisonNeVitPasParentsSet", "raisonNeVitPasParents", n), n) +
        bloc("blocAutreRaisonNeVitPasParentsSet",
             champ("Autre raison",
                   edit("editAutreRaisonNeVitPasParentsSet", "autreRaisonNeVitPasParents", n + 1,
                        hint="Preciser"), n + 1), n) + '\n' +
        champ("Situation scolaire", spinner("selectSituationScolaireSet", "situationScolaire", n), n) +
        bloc("blocNiveauScolaireSet",
             champ("Classe", spinner("selectNiveauScolaireSet", "niveauScolaire", n + 1), n + 1), n) + '\n' +
        bloc("blocRaisonNonScolarisationSet",
             champ("Raison(s) de non-scolarisation",
                   spinner("selectRaisonNonScolarisationSet", None, n + 1, multi=True), n + 1), n) + '\n' +
        bloc("blocAutreRaisonNonScolarisationSet",
             champ("Autre raison de non-scolarisation",
                   edit("editAutreRaisonNonScolarisationSet", "autreRaisonNonScolarisation", n + 1,
                        hint="Preciser"), n + 1), n) + '\n' +
        champ("Dispose d'un extrait de naissance ?",
              spinner("selectExtraitNaissanceSet", "extraitNaissance", n), n) +
        bloc("blocRaisonPasExtraitSet",
             champ("Raison(s) de l'absence d'extrait",
                   spinner("selectRaisonPasExtraitSet", None, n + 1, multi=True), n + 1), n) + '\n' +
        # Presence et motif d'absence : propres a ce module.
        champ("L'enfant etait-il present et disponible ?",
              spinner("selectPresentDisponibleSet", None, n), n) +
        bloc("blocRaisonAbsentSet",
             champ("Raison de l'absence", spinner("selectRaisonAbsentSet", None, n + 1), n + 1), n) + '\n' +
        champ("Heures de travail par semaine",
              edit("editHeuresTravailSemaineSet", "heuresTravailSemaine", n, hint="0", numerique=True), n) +
        champ("Nombre de jours travailles par semaine",
              spinner("selectJoursTravailSet", None, n), n) +
        champ("Heures travaillees la journee la plus chargee",
              spinner("selectHeuresJourneeSet", None, n), n) +
        champ("Situation(s) PFTE constatee(s)", spinner("selectSituationsPfteSet", None, n, multi=True), n) +
        bloc("blocRaisonTravailAbusSet",
             champ("Raison(s) du travail ou de l'abus",
                   spinner("selectRaisonTravailAbusSet", None, n + 1, multi=True), n + 1), n) + '\n' +
        bloc("blocAutreRaisonTravailAbusSet",
             champ("Autre raison",
                   edit("editAutreRaisonTravailAbusSet", "autreRaisonTravailAbus", n + 1,
                        hint="Preciser"), n + 1), n) + '\n' +
        champ("Mesure(s) au niveau de l'enfant", spinner("selectMesuresEnfantSet", None, n, multi=True), n) +
        champ("Mesure(s) au niveau du menage", spinner("selectMesuresMenageSet", None, n, multi=True), n) +
        champ("Mesure(s) au niveau de la communaute",
              spinner("selectMesuresCommunauteSet", None, n, multi=True), n) +
        bloc("blocAutreMesureSet",
             champ("Autre mesure", edit("editAutreMesureSet", "autreMesure", n + 1, hint="Preciser"), n + 1), n) + '\n'
    )

    # Toujours affichee, contrairement a l'Enquete Menage ou elle depend des enfants a charge.
    sensibilisation = (
        champ("Themes abordes", spinner("selectThemesSensibilisationSet", None, n, multi=True), n) +
        bloc("blocAutreThemeSet",
             champ("Autre theme",
                   edit("editAutreThemeSet", "autreThemeSensibilisation", n + 1, hint="Preciser"), n + 1), n) + '\n' +
        champ("Outils utilises", spinner("selectOutilsSensibilisationSet", None, n, multi=True), n) +
        champ("Nombre d'hommes sensibilises",
              edit("editNombreHommesSet", "nombreHommesSensibilises", n, hint="0", numerique=True), n) +
        champ("Nombre de femmes sensibilisees",
              edit("editNombreFemmesSet", "nombreFemmesSensibilisees", n, hint="0", numerique=True), n) +
        champ("Nombre de garcons sensibilises",
              edit("editNombreGarconsSet", "nombreGarconsSensibilises", n, hint="0", numerique=True), n) +
        champ("Nombre de filles sensibilisees",
              edit("editNombreFillesSet", "nombreFillesSensibilisees", n, hint="0", numerique=True), n) +
        champ("Total des personnes sensibilisees",
              edit("editTotalSensibiliseesSet", None, n, hint="0", lecture_seule=True), n) +
        champ("Telephone du producteur",
              edit("editTelephoneSensibilisationSet", "telephoneProducteurSensibilisation", n,
                   hint="0700000000"), n) +
        label("Photo de la sensibilisation", n) + '\n' +
        ind(n) + '<androidx.appcompat.widget.AppCompatImageView\n' +
        ind(n + 1) + 'android:id="@+id/imagePhotoSensibilisationSet"\n' +
        ind(n + 1) + 'android:layout_width="match_parent"\n' +
        ind(n + 1) + 'android:layout_height="@dimen/_120sdp"\n' +
        ind(n + 1) + 'android:background="@drawable/rounded_background"\n' +
        ind(n + 1) + 'android:padding="@dimen/_4sdp"\n' +
        ind(n + 1) + 'android:scaleType="centerInside" />\n\n' +
        bouton("btnPrendrePhotoSet", "Ajouter la photo", n)
    )

    corps = (
        banniere(3, "@string/information_suivi_enfant", "labelInformationSet") +
        carte("cardIdentificationSet", "IDENTIFICATION", identification, 3) +
        carte("cardRemediationSet", "SUIVI DEPUIS LA DERNIERE VISITE", remediation, 3) +
        carte("cardReevaluationSet", "REEVALUATION DE LA SITUATION DE L'ENFANT", reevaluation, 3) +
        carte("cardSensibilisationSet", "SENSIBILISATION DE PROXIMITE", sensibilisation, 3) +
        ind(3) + '<LinearLayout\n' +
        ind(4) + 'android:layout_width="match_parent"\n' +
        ind(4) + 'android:layout_height="wrap_content"\n' +
        ind(4) + 'android:layout_marginStart="@dimen/_8sdp"\n' +
        ind(4) + 'android:layout_marginEnd="@dimen/_8sdp"\n' +
        ind(4) + 'android:layout_marginBottom="@dimen/_16sdp"\n' +
        ind(4) + 'android:orientation="vertical">\n\n' +
        bouton("btnEnregistrerSet", "Enregistrer le suivi", 4) +
        bouton("btnBrouillonSet", "Mettre au brouillon", 4, couleur="@color/progband_gris") +
        ind(3) + '</LinearLayout>\n\n'
    )

    entete = (
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<!--\n'
        '     Suivi Enfant Travailleur (PFTE) - lot 5.\n'
        '\n'
        '     Memes conventions que les deux autres modules : blocs conditionnels masques a\n'
        '     l\'ouverture, android:tag portant le nom de la propriete du modele.\n'
        '\n'
        '     Trois differences : aucun champ de geolocalisation - le suivi porte sur une personne,\n'
        '     pas sur un lieu ; aucune fiche repetable - l\'enfant suivi est le sujet du formulaire ;\n'
        '     et la sensibilisation est toujours affichee, sans condition.\n'
        '-->\n'
        '<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    xmlns:app="http://schemas.android.com/apk/res-auto"\n'
        '    xmlns:tools="http://schemas.android.com/tools"\n'
        '    android:id="@+id/layout_suivi_enfant"\n'
        '    android:layout_width="match_parent"\n'
        '    android:layout_height="match_parent"\n'
        '    android:orientation="vertical"\n'
        '    tools:context=".activities.forms.SuiviEnfantActivity">\n\n'
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
        '            android:text="SUIVI ENFANT TRAVAILLEUR"\n'
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
        '        android:id="@+id/scrollSuiviEnfant"\n'
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


chemin = os.path.join(RES, "activity_suivi_enfant.xml")
contenu = ecran()
io.open(chemin, "w", encoding="utf-8", newline="\n").write(contenu)
print("activity_suivi_enfant.xml : %d lignes" % contenu.count("\n"))
