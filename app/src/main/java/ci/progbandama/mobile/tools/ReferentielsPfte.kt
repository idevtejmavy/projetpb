package ci.progbandama.mobile.tools

import ci.progbandama.mobile.models.RefPfteModel

/**
 * Catalogue des listes de reference du programme PFTE.
 *
 * Lot 1 - socle commun aux modules Enquete Menage, Visite Plantation et
 * Suivi Enfant Travailleur, qui partagent tous ces referentiels.
 */
object ReferentielsPfte {

    // Noms des tables serveur, tels qu'attendus par l'endpoint getlistedatas.
    const val RAISONS_NON_SCOLARISATION = "raisons_non_scolarisations"
    const val RAISONS_PAS_EXTRAIT = "raisons_pas_extraits"
    const val SITUATIONS_PFTE = "situations_pftes"
    const val RAISONS_TRAVAIL_ABUS = "raisons_travail_abus"
    const val MESURES_ENFANT = "mesures_enfants"
    const val MESURES_MENAGE = "mesures_menages"
    const val MESURES_COMMUNAUTE = "mesures_communautes"
    const val THEMES_SENSIBILISATION = "sensibilisation_themes"
    const val OUTILS_SENSIBILISATION = "sensibilisation_outils"
    const val NIVEAUX_ETUDE = "niveaux_etudes"
    const val CLASSES = "classes"

    /** Les onze listes a telecharger, dans l'ordre de leur apparition dans les formulaires. */
    val TABLES = listOf(
        NIVEAUX_ETUDE, CLASSES,
        RAISONS_NON_SCOLARISATION, RAISONS_PAS_EXTRAIT,
        SITUATIONS_PFTE, RAISONS_TRAVAIL_ABUS,
        MESURES_ENFANT, MESURES_MENAGE, MESURES_COMMUNAUTE,
        THEMES_SENSIBILISATION, OUTILS_SENSIBILISATION
    )

    /** Effectifs attendus, releves sur le serveur le 20/08/2026. Sert de controle de coherence. */
    val EFFECTIFS_ATTENDUS = mapOf(
        NIVEAUX_ETUDE to 6, CLASSES to 22,
        RAISONS_NON_SCOLARISATION to 13, RAISONS_PAS_EXTRAIT to 7,
        SITUATIONS_PFTE to 29, RAISONS_TRAVAIL_ABUS to 7,
        MESURES_ENFANT to 13, MESURES_MENAGE to 10, MESURES_COMMUNAUTE to 13,
        THEMES_SENSIBILISATION to 16, OUTILS_SENSIBILISATION to 8
    )

    /**
     * Rattachement d'une classe a son niveau d'etude.
     *
     * L'endpoint getlistedatas ne renvoie que l'identifiant et le nom : le champ
     * niveaux_etude_id de la table classes n'est pas expose, alors que le back-office s'en
     * sert pour regrouper les classes par niveau dans sa liste deroulante. A defaut de
     * pouvoir etendre l'endpoint, le rattachement est etabli ici a partir des identifiants
     * serveur, qui suivent strictement la progression scolaire (1 a 22).
     *
     * A remplacer par la donnee serveur si getlistedatas venait a exposer ce champ.
     */
    fun niveauPourClasse(idClasse: Int): Int = when (idClasse) {
        in 1..3 -> 1     // Petite, Moyenne, Grande Section -> Maternelle
        in 4..9 -> 2     // CP1 a CM2                       -> Primaire
        in 10..13 -> 3   // 6eme a 3eme                     -> 1er Cycle
        in 14..16 -> 4   // 2nd, 1ere, Tle                  -> 2nd Cycle
        in 17..19 -> 5   // Bac+1 a Bac+3                   -> Superieure
        else -> 6        // Passerelle, coranique, professionnel -> Autre
    }

    /** Construit une valeur de reference a partir de ce que renvoie le serveur. */
    fun versModele(tableRef: String, idRef: Int, nom: String, agentId: String) = RefPfteModel(
        uid = 0,
        tableRef = tableRef,
        idRef = idRef,
        nom = nom,
        parentId = if (tableRef == CLASSES) niveauPourClasse(idRef) else 0,
        ordre = idRef,
        agentId = agentId
    )
}
