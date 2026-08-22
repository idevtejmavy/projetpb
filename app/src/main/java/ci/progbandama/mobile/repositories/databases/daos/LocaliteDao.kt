package ci.progbandama.mobile.repositories.databases.daos

import androidx.room.*
import ci.progbandama.mobile.models.LocaliteModel

/**
 *  Created by didierboka.developer on 18/12/2021
 *  mail for work:   (didierboka.developer@gmail.com)
 */

@Dao
interface LocaliteDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(localite: LocaliteModel)

    @Transaction
    @Query("SELECT * FROM localite WHERE agentId = :agentID")
    fun getAll(agentID: String?): MutableList<LocaliteModel>

    @Transaction
    @Query("SELECT * FROM localite WHERE id = :id")
    fun getLocalite(id: Int): LocaliteModel

    @Transaction
    @Query("SELECT * FROM localite WHERE sectionId = :id")
    fun getLocaliteBySection(id: Int): MutableList<LocaliteModel>

    @Transaction
    @Update
    fun update(localite: LocaliteModel)

    @Transaction
    @Query("UPDATE localite SET id = :id, isSynced = :synced, origin = 'remote' WHERE uid = :localID")
    fun syncData(id: Int, synced: Boolean, localID: Int)

    @Transaction
    @Query("SELECT * FROM localite WHERE isSynced = 0 AND agentId = :agentID")
    fun getUnSyncedAll(agentID: String?): MutableList<LocaliteModel>

    @Transaction
    @Query("DELETE FROM localite WHERE agentId = :agentID")
    fun deleteAgentDatas(agentID: String?)

    @Transaction
    @Query("DELETE FROM localite")
    fun deleteAll()

    /** Suppression ciblee, utilisee pour ramener une donnee en attente vers les brouillons. */
    @Transaction
    @Query("DELETE FROM localite WHERE uid = :uid")
    fun deleteByUid(uid: Int)

    /**
     * Purge selective : ne supprime que les lignes deja transmises au serveur.
     * Une saisie locale en attente d'envoi survit ainsi a une mise a jour des donnees.
     */
    @Transaction
    @Query("DELETE FROM localite WHERE isSynced = 1")
    fun deleteSynced()

}