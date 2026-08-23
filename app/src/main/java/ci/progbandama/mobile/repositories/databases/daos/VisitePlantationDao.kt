package ci.progbandama.mobile.repositories.databases.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ci.progbandama.mobile.models.VisitePlantationModel

/**
 * Visites de plantation enregistrees sur l'appareil - lot 4.
 *
 * Meme structure que l'Enquete Menage, et pour la meme raison aucune requete ne purge la table en
 * entier : le serveur n'expose aucun endpoint de lecture pour ce module, une visite effacee
 * localement serait perdue pour l'agent meme si elle est bien arrivee.
 */
@Dao
interface VisitePlantationDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(visite: VisitePlantationModel)

    @Transaction
    @Query("SELECT * FROM visite_plantation WHERE agentId = :agentID ORDER BY uid DESC")
    fun getAll(agentID: String?): MutableList<VisitePlantationModel>

    @Transaction
    @Query("SELECT * FROM visite_plantation WHERE uid = :uid")
    fun getByUid(uid: Int): VisitePlantationModel?

    @Transaction
    @Query("SELECT * FROM visite_plantation WHERE isSynced = 0")
    fun getUnSyncedAll(): MutableList<VisitePlantationModel>

    @Transaction
    @Query("SELECT * FROM visite_plantation WHERE isSynced = 0 AND agentId = :agentID ORDER BY uid DESC")
    fun getUnSyncedAllLive(agentID: String?): LiveData<MutableList<VisitePlantationModel>>

    @Transaction
    @Query("SELECT COUNT(*) FROM visite_plantation WHERE isSynced = 0 AND agentId = :agentID")
    fun countUnSynced(agentID: String?): Int?

    @Transaction
    @Query("SELECT COUNT(*) FROM visite_plantation WHERE isSynced = 0 AND agentId = :agentID")
    fun countUnSyncedLive(agentID: String?): LiveData<Int?>

    /** Accuse de reception : on retient l'identifiant serveur pour ne pas renvoyer la visite. */
    @Transaction
    @Query("UPDATE visite_plantation SET id = :id, isSynced = :synced, origin = 'remote' WHERE uid = :localID")
    fun syncData(id: Int, synced: Boolean, localID: Int)

    /** Suppression ciblee, utilisee pour ramener une donnee en attente vers les brouillons. */
    @Transaction
    @Query("DELETE FROM visite_plantation WHERE uid = :uid")
    fun deleteByUid(uid: Int?)
}
