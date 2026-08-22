package ci.progbandama.mobile.tools

/**
 * Vocabulaires fixes des formulaires PFTE.
 *
 * Ces listes ne sont pas en base : le back-office les ecrit en dur dans ses controleurs, et
 * l'endpoint getlistedatas ne les expose donc pas. Elles sont reprises ici telles quelles, releve
 * fait sur le formulaire web le 20/08/2026.
 *
 * Les libelles sont la valeur transmise au serveur, qui stocke du texte et non un identifiant :
 * une reformulation, meme cosmetique, casserait le rapprochement avec les enquetes saisies sur le
 * web. Ils ne doivent pas etre retouches sans le meme changement cote back-office.
 *
 * Lot 1 - socle commun aux modules PFTE.
 */
object VocabulairesPfte {

    val OUI_NON = listOf("Oui", "Non")

    val SEXE_ENFANT = listOf("M", "F")

    /** Reponse possible quand le producteur n'est pas lui-meme le repondant. */
    val TITRES_REPONDANT = listOf(
        "Gérant(e)_fils/fille",
        "Gérant(e)_conjoint(e)",
        "Gérant(e)_frère/sœur",
        "Gérant(e)_neveu/nièce",
        "Gérant(e)_cousin/cousine",
        "Gérant(e)_employé(e)"
    )

    const val ABSENCE_TEMPORAIRE = "Temporairement absent"
    const val REFUS = "Refus"

    val RAISONS_INDISPONIBILITE = listOf(
        ABSENCE_TEMPORAIRE,
        REFUS,
        "N'est plus résident dans la localité",
        "N'est plus membre de la coopérative",
        "Décédé(e)"
    )

    val RAISONS_REFUS = listOf(
        "Promesse de la coopérative non tenue",
        "Prime non payée",
        "Aucune réponse du producteur/rice",
        "Autre raison"
    )

    val SITUATIONS_MATRIMONIALES = listOf(
        "Célibataire",
        "Vit en couple",
        "Divorcé / Séparé",
        "Veuf(ve)"
    )

    val LIENS_PARENTE = listOf(
        "Fils/Fille du producteur/rice",
        "Nièce/Neveu du producteur/rice",
        "Fils/Fille du travailleur",
        "Nièce/Neveu du travailleur",
        "Petite-sœur/Petit-frère",
        "Autre"
    )

    val RAISONS_NE_VIT_PAS_PARENTS = listOf(
        "Parents décédés",
        "Parent(s) ne peut/vent pas s'occuper de l'enfant",
        "Abandon",
        "Raisons scolaires",
        "Confié / donné à quelqu'un",
        "Travaille ici",
        "Est en contact avec son père et/ou sa mère",
        "Pour gagner l'argent",
        "Ne sait pas",
        "Autres",
        "N/A"
    )

    const val SCOLARISE = "Scolarisé"
    const val DESCOLARISE = "Déscolarisé"
    const val JAMAIS_SCOLARISE = "Jamais scolarisé"

    val SITUATIONS_SCOLAIRES = listOf(SCOLARISE, DESCOLARISE, JAMAIS_SCOLARISE)

    /**
     * Valeur portee par raisonIndisponibilite quand le producteur est disponible.
     *
     * Le serveur la force de son cote ; le mobile l'ecrit aussi, pour que l'enquete conservee sur
     * l'appareil dise la meme chose que celle enregistree en base.
     */
    const val SANS_OBJET = "N/A"
}
