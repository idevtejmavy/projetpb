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

    // ------------------------------------------------------------------ Visite Plantation

    /** Jour de la semaine ou la visite a eu lieu. Propre a ce module. */
    val JOURS_VISITE = listOf(
        "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    )

    /** Reponse commune aux deux indicateurs quand l'enfant n'a rien fait. */
    private const val AUCUNE_TACHE = "L'enfant n'a fait aucune tâche"

    /**
     * Indicateurs de temps de travail : le serveur enregistre un CODE, pas un libelle.
     *
     * Les deux listes sont donc des couples code / libelle. L'agent voit le libelle, le serveur
     * recoit le code - envoyer le libelle donnerait une valeur que le back-office ne sait pas
     * relire, et l'indicateur central du depistage serait perdu sans que rien ne le signale.
     */
    val JOURS_TRAVAIL: List<Pair<String, String>> =
        listOf("0" to AUCUNE_TACHE) +
                (1..7).map { it.toString() to if (it == 1) "Sur 1 jour" else "Sur $it jours" }

    val HEURES_JOURNEE: List<Pair<String, String>> =
        listOf("0" to AUCUNE_TACHE) + (1..24).map { it.toString() to "${it}h" }

    /** Libelles a proposer dans une liste deroulante. */
    fun libelles(couples: List<Pair<String, String>>): List<String> = couples.map { it.second }

    /** Code a transmettre a partir du libelle choisi. */
    fun codePour(couples: List<Pair<String, String>>, libelle: String?): String =
        couples.firstOrNull { it.second == libelle }?.first.orEmpty()

    /** Libelle a reafficher a partir d'un code enregistre, pour la reprise d'un brouillon. */
    fun libellePourCode(couples: List<Pair<String, String>>, code: String?): String? =
        couples.firstOrNull { it.first == code }?.second

    // ------------------------------------------------------------------ Suivi Enfant Travailleur

    /**
     * Presence de l'enfant lors de la visite. Codes "0" et "1", et non "Non" et "Oui".
     *
     * Le piege est ici plus vicieux qu'ailleurs : les libelles affiches sont Oui et Non, comme
     * pour les autres questions du formulaire, mais ceux-la seuls sont transmis tels quels tandis
     * que celui-ci doit partir en code. Une confusion ne se verrait qu'en base.
     */
    val PRESENT_DISPONIBLE: List<Pair<String, String>> = listOf("1" to "Oui", "0" to "Non")

    /** Motif d'absence de l'enfant. Codes 0 a 8, releves sur le formulaire web. */
    val RAISONS_ABSENCE: List<Pair<String, String>> = listOf(
        "0" to "Départ des parents",
        "1" to "L'enfant ne vit pas dans la localité",
        "2" to "L'enfant est décédé",
        "3" to "L'enfant est scolarisé loin de chez lui (temporaire)",
        "4" to "Vacances scolaires",
        "5" to "Sorties scolaires",
        "6" to "L'enfant est à la ferme",
        "7" to "Autre (temporaire)",
        "8" to "Autre (permanent)"
    )

    /** Code de la reponse "Non" a la presence : ouvre le motif d'absence. */
    const val ABSENT = "0"
}
