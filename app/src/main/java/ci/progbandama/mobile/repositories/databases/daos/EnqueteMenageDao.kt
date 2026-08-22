package ci.progbandama.mobile.repositories.databases.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ci.progbandama.mobile.models.EnqueteMenageModel

/**
 * Enquetes Menage enregistrees sur l'appareil - lot 3.
 *
 * Meme structure que les autres formulaires : insertion, file d'attente d'envoi, marquage apres
 * accuse de reception, suppression ciblee pour ramener une saisie vers les brouillons.
 *
 * Aucune requete ne purge la table en entier : le serveur n'expose aucun endpoint de lecture pour
 * ce module, une enquete effacee localement serait donc perdue pour l'agent meme si elle est bien
 * arrivee. La mise a jour de la base locale ne touche pas cette table.
 */
@Dao
interface EnqueteMenageDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(enquete: EnqueteMenageModel)

    @Transaction
    @Query("SELECT * FROM enquete_menage WHERE agentId = :agentID ORDER BY uid DESC")
    fun getAll(agentID: String?): MutableList<EnqueteMenageModel>

    @Transaction
    @Query("SELECT * FROM enquete_menage WHERE uid = :uid")
    fun getByUid(uid: Int): EnqueteMenageModel?

    @Transaction
    @Query("SELECT * FROM enquete_menage WHERE isSynced = 0")
    fun getUnSyncedAll(): MutableList<EnqueteMenageModel>

    @Transaction
    @Query("SELECT * FROM enquete_menage WHERE isSynced = 0 AND agentId = :agentID ORDER BY uid DESC")
    fun getUnSyncedAllLive(agentID: String?): LiveData<MutableList<EnqueteMenageModel>>

    @Transaction
    @Query("SELECT COUNT(*) FROM enquete_menage WHERE isSynced = 0 AND agentId = :agentID")
    fun countUnSynced(agentID: String?): Int?

    @Transaction
    @Query("SELECT COUNT(*) FROM enquete_menage WHERE isSynced = 0 AND agentId = :agentID")
    fun countUnSyncedLive(agentID: String?): LiveData<Int?>

    /** Accuse de reception : on retient l'identifiant serveur pour ne pas renvoyer l'enquete. */
    @Transaction
    @Query("UPDATE enquete_menage SET id = :id, isSynced = :synced, origin = 'remote' WHERE uid = :localID")
    fun syncData(id: Int, synced: Boolean, localID: Int)

    /** Suppression ciblee, utilisee pour ramener une donnee en attente vers les brouillons. */
    @Transaction
    @Query("DELETE FROM enquete_menage WHERE uid = :uid")
    fun deleteByUid(uid: Int?)
}
