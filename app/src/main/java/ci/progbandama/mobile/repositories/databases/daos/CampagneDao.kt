package ci.progbandama.mobile.repositories.databases.daos

import androidx.room.*
import ci.progbandama.mobile.models.CampagneModel

/**
 *  Created by didierboka.developer on 18/12/2021
 *  mail for work:   (didierboka.developer@gmail.com)
 */

@Dao
interface CampagneDao {

    @Transaction
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(campagneModel: CampagneModel)

    /**
     * Campagnes connues de l'appareil, la plus recente en tete.
     *
     * L'ordre compte depuis que getcampagne renvoie toutes les campagnes actives de la
     * cooperative : le selecteur pre-positionne la premiere ligne, et la synchronisation s'en
     * sert comme repli pour les saisies qui ne portent aucune campagne.
     */
    @Transaction
    @Query("SELECT * FROM campagne ORDER BY id DESC")
    fun getAll(): MutableList<CampagneModel>

    @Transaction
    @Query("DELETE FROM campagne")
    fun deleteAll()
}
