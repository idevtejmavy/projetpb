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
 * Valeur d'une liste de reference PFTE.
 *
 * Une seule entite generique pour les onze referentiels du programme, plutot qu'une entite
 * et un DAO par liste : les tables serveur exposent toutes la meme forme (identifiant, nom)
 * et ne servent qu'a alimenter des listes deroulantes. Onze entites auraient impose onze DAO,
 * onze migrations et onze etapes de telechargement pour aucun gain.
 *
 * Lot 1 - socle commun aux modules PFTE.
 */
@Entity(
    tableName = Constants.TABLE_REF_PFTE,
    indices = [Index(value = ["tableRef", "idRef"], unique = true)]
)
data class RefPfteModel(
    @PrimaryKey(autoGenerate = true) var uid: Int = 0,
    /** Nom de la table serveur d'origine : situations_pftes, mesures_enfants, classes... */
    var tableRef: String = "",
    /** Identifiant de la valeur cote serveur. */
    var idRef: Int = 0,
    /** Libelle presente a l'agent, et valeur transmise dans les tableaux de saisie. */
    var nom: String = "",
    /**
     * Rattachement a une valeur d'un autre referentiel, 0 si sans objet.
     * Utilise pour regrouper les classes par niveau d'etude, comme le fait le back-office.
     */
    var parentId: Int = 0,
    /** Ordre d'affichage : reprend l'identifiant serveur, qui suit la progression scolaire. */
    var ordre: Int = 0,
    var agentId: String = ""
)

@Dao
interface RefPfteDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(valeur: RefPfteModel)

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(valeurs: List<RefPfteModel>)

    @Transaction
    @Query("SELECT * FROM ref_pfte WHERE tableRef = :tableRef ORDER BY ordre ASC, nom ASC")
    fun pourTable(tableRef: String): MutableList<RefPfteModel>

    /** Libelles seuls, dans l'ordre : alimente directement les listes deroulantes. */
    @Transaction
    @Query("SELECT nom FROM ref_pfte WHERE tableRef = :tableRef ORDER BY ordre ASC, nom ASC")
    fun nomsPourTable(tableRef: String): MutableList<String>

    /** Valeurs rattachees a un parent : les classes d'un niveau d'etude donne. */
    @Transaction
    @Query("SELECT * FROM ref_pfte WHERE tableRef = :tableRef AND parentId = :parentId ORDER BY ordre ASC")
    fun pourTableEtParent(tableRef: String, parentId: Int): MutableList<RefPfteModel>

    @Transaction
    @Query("SELECT COUNT(*) FROM ref_pfte WHERE tableRef = :tableRef")
    fun compter(tableRef: String): Int

    @Transaction
    @Query("SELECT COUNT(*) FROM ref_pfte")
    fun compterTout(): Int

    @Transaction
    @Query("DELETE FROM ref_pfte WHERE tableRef = :tableRef")
    fun effacerTable(tableRef: String)

    @Transaction
    @Query("DELETE FROM ref_pfte")
    fun effacerTout()
}
