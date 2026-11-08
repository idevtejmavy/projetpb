package ci.progbandama.mobile.repositories.apis.services

import ci.progbandama.mobile.models.*
import ci.progbandama.mobile.repositories.datas.AgentAuthResponse
import ci.progbandama.mobile.repositories.datas.CommonData
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface ApiService {


    @POST("connexion")
    fun authAgent(@Body user: AgentModel): Call<AgentAuthResponse>

    @POST("getroleuser")
    fun getUserRoles(@Body data: CommonData): Call<AgentAuthResponse>

    // region SYNCHRONIZATION
    @POST("apiproducteur")
    fun synchronisationProducteur(@Body producteurModel: ProducteurModel): Call<ProducteurModel>

    @POST("apilocalite")
    fun synchronisationLocalite(@Body localiteModel: LocaliteModel): Call<LocaliteModel>

    @POST("apisuiviparcelle")
    fun synchronisationSuivi(@Body suiviParcelleModel: SuiviParcelleModel): Call<SuiviParcelleModel>

    @POST("apiparcelle")
    fun synchronisationParcelle(@Body parcelleModel: ParcelleModel): Call<ParcelleModel>

    @POST("apimenage")
    fun synchronisationMenage(@Body menageModel: ProducteurMenageModel): Call<ProducteurMenageModel>

    @POST("apisuiviformation")
    fun synchronisationFormation(@Body formationModel: FormationModel): Call<FormationModel>

    @POST("apivisiteur")
    fun synchronisationVisiteurFormation(@Body visiteurFormationModel: VisiteurFormationModel): Call<VisiteurFormationModel>

    @POST("apidistribution")
    fun synchronisationDistributionArbre(@Body distributionArbreModel: DistributionArbreModel): Call<DistributionArbreModel>

    @POST("postplanting")
    fun synchronisationPostPlanting(@Body postPlantingModel: PostPlantingModel): Call<PostPlantingModel>

    @POST("apiagroevaluation")
    fun synchronisationEvaluationBesoin(@Body evaluationArbreModel: EvaluationArbreModel): Call<EvaluationArbreModel>

    @POST("apilivraisonmagasinsection")
    fun synchronisationLivraisonSection(@Body livraisonModel: LivraisonModel): Call<LivraisonModel>

    @POST("apilivraisonmagasincentral")
    fun synchronisationLivraisonCentral(@Body livraisonCentralModel: LivraisonCentralModel): Call<LivraisonCentralModel>

    @POST("apievaluation")
    fun synchronisationInspection(@Body questionDTO: InspectionDTO): Call<InspectionDTOExt>

    @POST("updateinspection")
    fun synchronisationInspectionUpdate(@Body data: InspectionUpdateDTO): Call<InspectionDTOExt>

    @POST("apiinfosproducteur")
    fun synchronisationInfosProducteur(@Body infosProducteurDTO: InfosProducteurDTO): Call<InfosProducteurDTO>
    // endregion

    @POST("getmagasinsection")
    fun getMagasins(@Body commonData: CommonData): Call<MutableList<MagasinModel>>

    @POST("getmagasincentraux")
    fun getMagasinsCentraux(@Body commonData: CommonData): Call<MutableList<MagasinModel>>

    // region SERVICE WITH COROUTINES

    @POST("apiproducteur")
    fun synchronisationProducteurC(@Body producteurModel: ProducteurModel): Call<ProducteurModel>

    @POST("getproducteurs")
    fun getProducteurs(@Body agent: AgentModel): Call<MutableList<ProducteurModel>>

    @POST("getlistedatas")
    fun getDatasList(@Body table: CommonData): Call<MutableList<CommonData>>

    @POST("getcampagne")
    fun getCampagnes(@Body table: CommonData): Call<MutableList<CampagneModel>>

    @POST("getapplicateurs")
    fun getApplicateurs(@Body table: CommonData): Call<MutableList<ApplicateurModel>>

    @POST("getinspections")
    fun getInspections(@Body table: CommonData): Call<MutableList<InspectionDTOExt>>

    @POST("getapprovisionnementsection")
    fun getApprovisionnement(@Body table: CommonData): Call<MutableList<ApprovisionnementModel>>

    @POST("getlocalite")
    fun getLocalites(@Body table: CommonData): Call<MutableList<LocaliteModel>>

    @POST("getdomain")
    fun getDomaine(@Body table: CommonData): Call<CommonResponse>

    @POST("getproducteurupdate")
    fun getProducteurInfosUncomplete(@Body table: CommonData): Call<MutableList<ProducteurUpdateModel>>

    @POST("getparcelleupdate")
    fun getParcelleInfosUncomplete(@Body table: CommonData): Call<MutableList<ParcelleUpdateModel>>

    @POST("getdelegues")
    fun getDelegues(@Body table: CommonData): Call<MutableList<CommonData>>

    @POST("getparcelles")
    fun getParcelles(@Body table: CommonData): Call<MutableList<ParcelleExtModel>>

    @POST("getquestionnaire")
    fun getQuestionnaires(): Call<MutableList<InspectionQuestionnairesModel>>

    @POST("getnotation")
    fun getNotations(): Call<MutableList<NotationModel>>

    @POST("getthemes")
    fun getThemes(): Call<MutableList<ThemeFormationModel>>

    @POST("getsousthemes")
    fun getSousThemes(): Call<MutableList<SousThemeFormationModel>>

    @POST("apiestimation")
    fun synchronisationEstimation(@Body estimationModel: EstimationModel): Call<EstimationModel>

    @POST("apiapplication")
    fun synchronisationSuiviApplication(@Body suiviApplicationModel: SuiviApplicationModel): Call<SuiviApplicationModel>

    @POST("apissrteclrms")
    fun synchronisationEnqueteSsrt(@Body enqueteSsrtModel: EnqueteSsrtModel): Call<EnqueteSsrtModel>

    /**
     * Enquete Menage (PFTE) - lot 3.
     *
     * Le controleur mobile ne valide rien et ne renvoie aucune liste : il n'existe aucun endpoint
     * de lecture pour ce module. La reponse sert uniquement a recuperer l'identifiant serveur.
     */
    @POST("apienquetemenage")
    fun synchronisationEnqueteMenage(@Body enqueteMenage: EnqueteMenageModel): Call<EnqueteMenageModel>

    /**
     * Visite Plantation (PFTE) - lot 4.
     *
     * Seule des trois interfaces PFTE a valider ce qu'elle recoit : un refus renvoie donc un 422
     * exploitable, que le suivi des refus sait deja resumer.
     */
    @POST("apivisiteplantation")
    fun synchronisationVisitePlantation(@Body visite: VisitePlantationModel): Call<VisitePlantationModel>

    /**
     * Suivi Enfant Travailleur (PFTE) - lot 5.
     *
     * Comme l'Enquete Menage, ce controleur ne valide rien : les controles du formulaire sont le
     * seul filet avant l'enregistrement en base.
     */
    @POST("apisuivienfanttravailleur")
    fun synchronisationSuiviEnfant(@Body suivi: SuiviEnfantModel): Call<SuiviEnfantModel>

    /**
     * Enfants pouvant faire l'objet d'un suivi - lot 5.
     *
     * Renvoie les enfants actifs de la cooperative avec leur enquete d'origine, dont on ne retient
     * que le producteur et la localite. Mis en cache localement : l'agent ouvre le formulaire sans
     * reseau.
     */
    @POST("getenfantsmenage")
    fun getEnfantsSuivables(@Body agent: CommonData): Call<MutableList<EnfantSuivableDto>>

    @POST("apilocalite")
    fun synchronisationLocaliteC(@Body localiteModel: LocaliteModel): Call<LocaliteModel>

    @POST("gettypeformation")
    fun getTypeThemesFormations(): Call<MutableList<TypeFormationModel>>

    @POST("getstaff")
    fun getStaff(@Body table: CommonData): Call<MutableList<ConcernesModel>>

    @POST("getformationsbyuser")
    fun getFormationByUser(@Body table: CommonData): Call<MutableList<FormationModelExt>>

    @POST("getsections")
    fun getSections(@Body table: CommonData): Call<MutableList<SectionModel>>

    @POST("getprogrammes")
    fun getProgrammes(): Call<MutableList<ProgrammeModel>>

    @POST("getarbre")
    fun getArbreList(): Call<MutableList<ArbreModel>>

    @POST("getbesoinprod")
    fun getProductEvalList(@Body commonData: CommonData): Call<QuantiteArbrDistribuer>

    @POST("gettransporteurs")
    fun getTransporteurList(@Body table: CommonData): Call<MutableList<TransporteurModel>>

    @POST("getvehicules")
    fun getVehiculeList(@Body table: CommonData): Call<MutableList<VehiculeModel>>

    @POST("getremorques")
    fun getRemorqueList(@Body table: CommonData): Call<MutableList<RemorqueModel>>

    @POST("livraisonbroussebymagasinsection")
    fun getLivraisonVerMagCentralList(): Call<MutableList<LivraisonVerMagCentralModel>>

    @POST("getproducteursdistribues")
    fun getProducteursPostPlantingArbrDistribList(@Body commonData: CommonData): Call<MutableList<PostPlantingArbrDistribSecModel>>

}
