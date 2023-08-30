package ci.sicadevd.mobile.tools

import android.app.Activity
import ci.sicadevd.mobile.models.CourEauModel
import ci.sicadevd.mobile.models.EauUseeModel
import ci.sicadevd.mobile.models.GardeMachineModel
import ci.sicadevd.mobile.models.IntrantModel
import ci.sicadevd.mobile.models.LieuFormationModel
import ci.sicadevd.mobile.models.NationaliteModel
import ci.sicadevd.mobile.models.NiveauModel
import ci.sicadevd.mobile.models.NotationModel
import ci.sicadevd.mobile.models.OrdureMenagereModel
import ci.sicadevd.mobile.models.PaiementMobileModel
import ci.sicadevd.mobile.models.PersonneBlesseeModel
import ci.sicadevd.mobile.models.RecuModel
import ci.sicadevd.mobile.models.SourceEauModel
import ci.sicadevd.mobile.models.SourceEnergieModel
import ci.sicadevd.mobile.models.ThemeFormationModel
import ci.sicadevd.mobile.models.TypeDocumentModel
import ci.sicadevd.mobile.models.TypeLocaliteModel
import ci.sicadevd.mobile.models.TypeMachineModel
import ci.sicadevd.mobile.models.TypePieceModel
import ci.sicadevd.mobile.models.TypeProduitModel
import com.blankj.utilcode.util.GsonUtils
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class AssetFileHelper {



    companion object{


        val listAssetFile = arrayListOf<String>(
            "cours_eaux",
            "eaux_usees",
            "garde_machines",
            "geres_recus",
            "lieu_formations",
            "nationalites",
            "niveaux",
            "ordures_menageres",
            "paiement_mobile",
            "themes_formations",
            "type_documents",
            "type_intrants",
            "type_produits",
            "type_pieces",
            "personne_blessee",
            "type_localites",
            "sources_eaux",
            "sources_energies",
            "type_machines",
            "notations",
        );


        /**
           *     @return:
         *           0 = "cours_eaux",
           *         1 = "eaux_usees",
           *         2 = "garde_machines",
           *         3 = "geres_recus",
           *         4 = "lieu_formations",
           *         5 = "nationalites",
           *         6 = "niveaux",
           *         7 = "ordures_menageres",
           *         8 = "paiement_mobile",
           *         9 = "themes_formations",
           *         10 = "type_documents",
           *         11 = "type_intrants",
           *         12 = "type_produits"
           *         13 = "type_pieces",
           *         14 = "personne_blessee",
           *         15 = "type_localites",
           *         16 = "sources_eaux",
           *         17 = "sources_energies",
           *         18 = "type_machines",
           *         19 = "notations",
        */
        fun getListDataFromAsset(position: Int = 0, context: Activity): MutableList<*>? {
            val typer : Type? = when(position) {
                0 -> object : TypeToken<MutableList<CourEauModel>>() {}.type
                1 -> object : TypeToken<MutableList<EauUseeModel>>() {}.type
                2 -> object : TypeToken<MutableList<GardeMachineModel>>() {}.type
                3 -> object : TypeToken<MutableList<RecuModel>>() {}.type
                4 -> object : TypeToken<MutableList<LieuFormationModel>>() {}.type
                5 -> object : TypeToken<MutableList<NationaliteModel>>() {}.type
                6 -> object : TypeToken<MutableList<NiveauModel>>() {}.type
                7 -> object : TypeToken<MutableList<OrdureMenagereModel>>() {}.type
                8 -> object : TypeToken<MutableList<PaiementMobileModel>>() {}.type
                9 -> object : TypeToken<MutableList<ThemeFormationModel>>() {}.type
                10 -> object : TypeToken<MutableList<TypeDocumentModel>>() {}.type
                11 -> object : TypeToken<MutableList<IntrantModel>>() {}.type
                12 -> object : TypeToken<MutableList<TypeProduitModel>>() {}.type
                13 -> object : TypeToken<MutableList<TypePieceModel>>() {}.type
                14 -> object : TypeToken<MutableList<PersonneBlesseeModel>>() {}.type
                15 -> object : TypeToken<MutableList<TypeLocaliteModel>>() {}.type
                16 -> object : TypeToken<MutableList<SourceEauModel>>() {}.type
                17 -> object : TypeToken<MutableList<SourceEnergieModel>>() {}.type
                18 -> object : TypeToken<MutableList<TypeMachineModel>>() {}.type
                19 -> object : TypeToken<MutableList<NotationModel>>() {}.type
                else -> {
                    null
                }
            }
            return GsonUtils.fromJson(
                Commons.loadJSONFromAsset(
                    context,
                    listAssetFile[position].toString().plus(".json")
                ), typer!!
            )
        }
    }


}