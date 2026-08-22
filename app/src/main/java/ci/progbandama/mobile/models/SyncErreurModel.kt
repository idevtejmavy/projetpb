package ci.progbandama.mobile.models

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import ci.progbandama.mobile.tools.Constants

/**
 * Dernier refus du serveur lors d'une tentative d'envoi, par module.
 *
 * Jusqu'ici un echec de transmission etait invisible pour l'agent : la donnee restait
 * simplement en attente, sans qu'il sache si le reseau avait manque ou si le serveur
 * avait rejete la saisie. Cette table conserve le motif du dernier refus afin de
 * l'afficher sous les elements concernes.
 *
 * Une seule ligne par module : les donnees en attente d'un meme module echouent en
 * pratique pour la meme raison, et l'agent a besoin du motif, pas d'un historique.
 */
@Entity(
    tableName = Constants.TABLE_SYNC_ERREUR,
    indices = [Index(value = ["module"], unique = true)]
)
data class SyncErreurModel(
    @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    /** Code fonctionnel du module : PRODUCTEUR, MENAGE, PARCELLE... */
    var module: String = "",
    /** Code HTTP renvoye par le serveur, ou 0 si l'appel n'a pas abouti. */
    var code: Int = 0,
    /** Message lisible presente a l'agent. */
    var message: String = "",
    /** Horodatage du refus, au format HH:mm. */
    var heure: String = "",
    var agentId: String = ""
)

@Dao
interface SyncErreurDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(erreur: SyncErreurModel)

    @Transaction
    @Query("SELECT * FROM sync_erreur WHERE module = :module AND agentId = :agentID LIMIT 1")
    fun pourModule(module: String, agentID: String): SyncErreurModel?

    @Transaction
    @Query("DELETE FROM sync_erreur WHERE module = :module")
    fun effacerModule(module: String)

    @Transaction
    @Query("DELETE FROM sync_erreur")
    fun effacerTout()
}
