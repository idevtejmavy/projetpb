package ci.progbandama.mobile.tools

import android.app.Activity
import android.content.Context
import android.view.View
import ci.progbandama.mobile.R
import ci.progbandama.mobile.models.DataDraftedModel
import ci.progbandama.mobile.repositories.databases.ProgBandRoomDatabase
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.SPUtils
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

/**
 * Brouillons des modules PFTE : enregistrement, relecture et cloture.
 *
 * Les trois modules ont le meme cycle hors ligne - saisie partielle, mise au brouillon, reprise
 * integrale, transmission - et le meme enchainement d'ecrans a chaque etape. Ce point d'entree
 * commun evite de le recopier trois fois.
 *
 * ATTENTION - pourquoi un Gson distinct de celui d'ApiClient.
 *
 * ApiClient.gson est regle sur excludeFieldsWithoutExposeAnnotation(), et les modeles portent
 * leurs listes sous la forme :
 *
 *     @Ignore @Expose(serialize = true, deserialize = false) var travauxLegers: MutableList<String>?
 *
 * Ces champs partent bien au serveur mais ne se relisent PAS : deserialize = false. Ecrire un
 * brouillon avec ApiClient.gson puis le relire perdrait donc silencieusement toute liste a choix
 * multiple - et, pour les modules PFTE, les fiches enfant elles-memes. Le brouillon ne quitte
 * jamais l'appareil : il n'a aucune raison d'obeir aux regles d'exposition de l'API, il doit
 * seulement se relire a l'identique. D'ou un Gson sans exclusion.
 *
 * Cette regle ne vaut que pour les modules PFTE : les formulaires existants continuent d'ecrire
 * leurs brouillons avec ApiClient.gson et contournent la limite en stockant leurs listes dans une
 * vraie colonne Stringify.
 *
 * Lot 1 - socle commun aux modules PFTE.
 */
object BrouillonPfte {

    /** Types de brouillon des trois modules. Valeurs alignees sur celles de RepriseEnAttente. */
    const val TYPE_ENQUETE_MENAGE = "enquete_menage_pfte"
    const val TYPE_VISITE_PLANTATION = "visite_plantation_pfte"
    const val TYPE_SUIVI_ENFANT = "suivi_enfant_travailleur"

    /** Voir la note d'entete : relecture fidele, sans les regles d'exposition de l'API. */
    val gson: Gson = GsonBuilder().setLenient().create()

    /**
     * Trace une anomalie sans jamais echouer a son tour.
     *
     * LogUtils s'appuie sur le contexte applicatif et leve si celui-ci n'est pas initialise.
     * Comme tous les appels au journal de ce fichier sont dans des blocs catch, une telle levee
     * remplacerait l'erreur d'origine par une autre - et, dans le cas de l'enregistrement, ferait
     * echouer une mise au brouillon qui avait reussi. Le role de cet objet est de ne pas perdre
     * la saisie de l'agent : il ne peut pas tomber en rendant compte d'un incident.
     */
    private fun trace(message: String) {
        try {
            LogUtils.e(message)
        } catch (ignore: Throwable) {
            println(message)
        }
    }

    fun agent(): String = SPUtils.getInstance().getInt(Constants.AGENT_ID).toString()

    fun dao(context: Context) =
        ProgBandRoomDatabase.getDatabase(context.applicationContext)?.draftedDatasDao()

    // ------------------------------------------------------------------ ecriture

    /**
     * Met un formulaire au brouillon, apres confirmation de l'agent.
     *
     * @param uidExistant uid du brouillon rouvert, 0 pour un nouveau. Le reutiliser evite
     *        d'empiler un brouillon de plus a chaque enregistrement d'une meme saisie.
     * @param boutonBrouillon icone a animer en retour, comme le font les formulaires existants.
     */
    fun <T> enregistrer(
        activite: Activity,
        type: String,
        modele: T,
        uidExistant: Int = 0,
        boutonBrouillon: View? = null,
        surEnregistrement: (() -> Unit)? = null
    ) {
        Commons.showMessage(
            message = activite.getString(
                R.string.voulez_vous_vraiment_mettre_ce_contenu_au_brouillon_afin_de_reprendre_ulterieurement
            ),
            context = activite,
            finished = false,
            callback = {
                if (ecrire(activite, type, modele, uidExistant)) {
                    Commons.showMessage(
                        message = activite.getString(R.string.contenu_ajout_aux_brouillons),
                        context = activite,
                        finished = true,
                        callback = {
                            Commons.playDraftSound(activite)
                            boutonBrouillon?.startAnimation(Commons.loadShakeAnimation(activite))
                            surEnregistrement?.invoke()
                        },
                        positive = activite.getString(R.string.ok),
                        deconnec = false,
                        showNo = false
                    )
                } else {
                    // L'agent doit savoir que sa saisie n'est PAS a l'abri : sur ce message il
                    // reste dans le formulaire, ce que ne ferait pas un ecran de succes.
                    Commons.showMessage(
                        message = activite.getString(R.string.brouillon_non_enregistre),
                        context = activite,
                        finished = false,
                        callback = { },
                        positive = activite.getString(R.string.ok),
                        deconnec = false,
                        showNo = false
                    )
                }
            },
            positive = activite.getString(R.string.oui),
            deconnec = false,
            showNo = true
        )
    }

    /**
     * Ecrit le brouillon sans passer par les ecrans de confirmation.
     *
     * Sert a la reprise d'une donnee en attente d'envoi, qui repasse par le brouillon sans
     * intervention de l'agent.
     *
     * @return vrai si le brouillon est bien en base.
     */
    fun <T> ecrire(context: Context, type: String, modele: T, uidExistant: Int = 0): Boolean = try {
        dao(context)?.insert(
            DataDraftedModel(
                uid = uidExistant,
                datas = gson.toJson(modele),
                typeDraft = type,
                agentId = agent()
            )
        )
        true
    } catch (ex: Exception) {
        trace("Enregistrement du brouillon $type : ${ex.message}")
        false
    }

    // ------------------------------------------------------------------ relecture

    /** Relit le contenu d'un brouillon. Renvoie null si le contenu est illisible. */
    fun <T> lire(brouillon: DataDraftedModel?, classe: Class<T>): T? {
        val json = brouillon?.datas
        if (json.isNullOrBlank()) return null
        return try {
            gson.fromJson(json, classe)
        } catch (ex: Exception) {
            trace("Relecture du brouillon ${brouillon.typeDraft} : ${ex.message}")
            null
        }
    }

    /** Brouillon designe par son identifiant, ou null s'il a ete cloture entre-temps. */
    fun parId(context: Context, uid: Int): DataDraftedModel? = try {
        dao(context)?.getDraftedDataByID(uid)
    } catch (ex: Exception) {
        trace("Lecture du brouillon $uid : ${ex.message}")
        null
    }

    // ------------------------------------------------------------------ listes portees

    /**
     * Serialise une liste de fiches vers sa colonne Stringify.
     *
     * Les fiches enfant ne sont pas des champs du modele : elles sont portees par une colonne
     * texte, comme le fait deja le reste de l'application pour ses listes.
     */
    fun <T> serialiserListe(elements: List<T>?): String = gson.toJson(elements ?: emptyList<T>())

    /** Relit une colonne Stringify. Tolere une valeur absente ou illisible. */
    fun <T> deserialiserListe(stringify: String?, type: TypeToken<MutableList<T>>): MutableList<T> {
        if (stringify.isNullOrBlank()) return mutableListOf()
        return try {
            gson.fromJson<MutableList<T>>(stringify, type.type) ?: mutableListOf()
        } catch (ex: Exception) {
            trace("Relecture d'une liste de brouillon : ${ex.message}")
            mutableListOf()
        }
    }

    // ------------------------------------------------------------------ cloture

    /**
     * Marque le brouillon comme termine, une fois le formulaire enregistre en base locale.
     *
     * La cloture n'attend pas l'envoi au serveur : la donnee est deja dans sa table et suivra la
     * file d'attente d'envoi. Elle ne serait perdue qu'en supprimant le brouillon AVANT
     * l'enregistrement local.
     */
    fun cloturer(context: Context, uid: Int?) {
        if (uid == null || uid <= 0) return
        try {
            dao(context)?.completeDraft(uid)
        } catch (ex: Exception) {
            trace("Cloture du brouillon $uid : ${ex.message}")
        }
    }

    /** Nombre de brouillons en cours pour un module, pour les compteurs du tableau de bord. */
    fun compter(context: Context, type: String): Int = try {
        dao(context)?.countByType(agent(), type) ?: 0
    } catch (ex: Exception) {
        trace("Comptage des brouillons $type : ${ex.message}")
        0
    }
}
