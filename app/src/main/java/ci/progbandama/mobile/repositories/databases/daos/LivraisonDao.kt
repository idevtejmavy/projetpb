package ci.progbandama.mobile.repositories.databases.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import ci.progbandama.mobile.models.*

/**
 *  Created by didierboka.developer on 18/12/2021
 *  mail for work:   (didierboka.developer@gmail.com)
 */

@Dao
interface LivraisonDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(livraisonModel: LivraisonModel)

    @Transaction
    @Query("SELECT * FROM livraison WHERE agentId = :agentID")
    fun getAll(agentID: String?): MutableList<LivraisonModel>

    @Transaction
    @Query("SELECT * FROM Livraison WHERE isSynced = 0 AND agentId = :agentID")
    fun getUnSyncedAll(agentID: String?): MutableList<LivraisonModel>

    @Transaction
    @Query("UPDATE livraison SET id = :id, isSynced = :synced, origin = 'remote' WHERE uid = :localID")
    fun syncData(id: Int, synced: Boolean, localID: Int)

    @Transaction
    @Query("DELETE FROM livraison WHERE agentId = :agentID")
    fun deleteAgentDatas(agentID: String?)

    @Transaction
    @Query("DELETE FROM livraison")
    fun deleteAll()

    /** Suppression ciblee, utilisee pour ramener une donnee en attente vers les brouillons. */
    @Transaction
    @Query("DELETE FROM livraison WHERE uid = :uid")
    fun deleteByUid(uid: Int?)

    /**
     * Purge selective : ne supprime que les lignes deja transmises au serveur.
     * Une saisie locale en attente d'envoi survit ainsi a une mise a jour des donnees.
     */
    @Transaction
    @Query("DELETE FROM livraison WHERE isSynced = 1")
    fun deleteSynced()

    @Transaction
    @Query("SELECT * FROM livraison WHERE (isSynced = 0 AND producteursId = :producteurUid)")
    fun getUnSyncedByProdUid(producteurUid: String?): MutableList<LivraisonModel>
    @Transaction
    @Query("SELECT * FROM livraison WHERE isSynced = 0 AND agentId = :agentID")
    fun getUnSyncedAllLive(agentID: String?): LiveData<MutableList<LivraisonModel>>

}