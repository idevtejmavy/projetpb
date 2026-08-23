package ci.progbandama.mobile.repositories.databases.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ci.progbandama.mobile.models.EnfantSuivableModel
import ci.progbandama.mobile.models.SuiviEnfantModel

/**
 * Suivis d'enfant travailleur enregistres sur l'appareil - lot 5.
 *
 * Comme pour les deux autres modules PFTE, aucune requete ne purge la table : le serveur n'expose
 * aucun endpoint de lecture pour les suivis, une donnee effacee localement serait perdue pour
 * l'agent meme si elle est bien arrivee.
 */
@Dao
interface SuiviEnfantDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(suivi: SuiviEnfantModel)

    @Transaction
    @Query("SELECT * FROM suivi_enfant WHERE agentId = :agentID ORDER BY uid DESC")
    fun getAll(agentID: String?): MutableList<SuiviEnfantModel>

    @Transaction
    @Query("SELECT * FROM suivi_enfant WHERE uid = :uid")
    fun getByUid(uid: Int): SuiviEnfantModel?

    @Transaction
    @Query("SELECT * FROM suivi_enfant WHERE isSynced = 0")
    fun getUnSyncedAll(): MutableList<SuiviEnfantModel>

    @Transaction
    @Query("SELECT COUNT(*) FROM suivi_enfant WHERE isSynced = 0 AND agentId = :agentID")
    fun countUnSynced(agentID: String?): Int?

    @Transaction
    @Query("SELECT COUNT(*) FROM suivi_enfant WHERE isSynced = 0 AND agentId = :agentID")
    fun countUnSyncedLive(agentID: String?): LiveData<Int?>

    /** Accuse de reception : on retient l'identifiant serveur pour ne pas renvoyer le suivi. */
    @Transaction
    @Query("UPDATE suivi_enfant SET id = :id, isSynced = :synced, origin = 'remote' WHERE uid = :localID")
    fun syncData(id: Int, synced: Boolean, localID: Int)

    /** Suppression ciblee, utilisee pour ramener une donnee en attente vers les brouillons. */
    @Transaction
    @Query("DELETE FROM suivi_enfant WHERE uid = :uid")
    fun deleteByUid(uid: Int?)
}

/**
 * Cache local des enfants pouvant faire l'objet d'un suivi.
 *
 * Contrairement aux suivis, cette table EST purgee a chaque mise a jour : c'est un reflet de ce
 * que le serveur connait, pas une saisie de l'agent. Le remplacement se fait en une seule
 * transaction - voir MajLocale.
 */
@Dao
interface EnfantSuivableDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(enfants: List<EnfantSuivableModel>)

    @Transaction
    @Query("SELECT * FROM enfant_suivable WHERE agentId = :agentID ORDER BY nom")
    fun getAll(agentID: String?): MutableList<EnfantSuivableModel>

    @Transaction
    @Query("SELECT * FROM enfant_suivable WHERE idServeur = :idServeur LIMIT 1")
    fun parIdServeur(idServeur: Int): EnfantSuivableModel?

    @Transaction
    @Query("SELECT COUNT(*) FROM enfant_suivable WHERE agentId = :agentID")
    fun compter(agentID: String?): Int

    @Transaction
    @Query("DELETE FROM enfant_suivable")
    fun effacerTout()
}
