package ci.progbandama.mobile.repositories.databases.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import ci.progbandama.mobile.models.*

/**
 *  Created by didierboka.developer on 18/12/2021
 *  mail for work:   (didierboka.developer@gmail.com)
 */

@Dao
interface ProducteurMenageDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(producteurMenageModel: ProducteurMenageModel)

    @Transaction
    @Query("SELECT * FROM menage WHERE agentId = :agentID")
    fun getAll(agentID: String?): MutableList<ProducteurMenageModel>

    @Transaction
    @Query("SELECT * FROM menage WHERE isSynced = 0 AND agentId = :agentID")
    fun getUnSyncedAll(agentID: String?): MutableList<ProducteurMenageModel>
    @Transaction
    @Query("SELECT * FROM menage WHERE isSynced = 0 AND agentId = :agentID")
    fun getUnSyncedAllLive(agentID: String?): LiveData<MutableList<ProducteurMenageModel>>

    @Transaction
    @Query("UPDATE menage SET id = :id, isSynced = :synced, origin = 'remote' WHERE uid = :localID")
    fun syncData(id: Int, synced: Boolean, localID: Int)



    @Transaction
    @Query("SELECT * FROM menage WHERE (isSynced = 0 AND producteurs_id = :producteurUid AND origin = 'local' AND agentId = :agentId)")
    fun getMenagesUnSynchronizedLocal(producteurUid: String?, agentId: String?): MutableList<ProducteurMenageModel>

    @Transaction
    @Query("DELETE FROM menage WHERE agentId = :agentID")
    fun deleteAgentDatas(agentID: String?)
    @Transaction
    @Query("DELETE FROM menage")
    fun deleteAll()

    /** Suppression ciblee, utilisee pour ramener une donnee en attente vers les brouillons. */
    @Transaction
    @Query("DELETE FROM menage WHERE uid = :uid")
    fun deleteByUid(uid: Int)

    /**
     * Purge selective : ne supprime que les lignes deja transmises au serveur.
     * Une saisie locale en attente d'envoi survit ainsi a une mise a jour des donnees.
     */
    @Transaction
    @Query("DELETE FROM menage WHERE isSynced = 1")
    fun deleteSynced()

    @Transaction
    @Query("DELETE FROM menage WHERE uid = :uId")
    fun deleteUid(uId: String?)

    @Transaction
    @Query("SELECT * FROM menage WHERE isSynced = 1 AND agentId = :agentID")
    fun getSyncedAll(agentID: String): MutableList<ProducteurMenageModel>

    @Transaction
    @Query("SELECT * FROM menage WHERE (isSynced = 0 AND producteurs_id = :producteurUid)")
    fun getUnSyncedByProdUid(producteurUid: String?): MutableList<ProducteurMenageModel>

}