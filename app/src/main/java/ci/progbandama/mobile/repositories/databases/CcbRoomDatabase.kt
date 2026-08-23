package ci.progbandama.mobile.repositories.databases

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ci.progbandama.mobile.models.*
import ci.progbandama.mobile.repositories.databases.daos.*
import androidx.sqlite.db.SupportSQLiteDatabase

import androidx.room.migration.Migration
import ci.progbandama.mobile.tools.Constants
import ci.progbandama.mobile.tools.ListConverters


/**
 *  Created by didierboka.developer on 18/12/2021
 *  mail for work:   (didierboka.developer@gmail.com)
 */

@Database(
    version = Constants.BUILD_VERSION, exportSchema = false,
    entities = [
        CoopModel::class,
        AgentModel::class,
        EauUseeModel::class,
        CourEauModel::class,
        GardeMachineModel::class,
        LieuFormationModel::class,
        NationaliteModel::class,
        NiveauModel::class,
        OrdureMenagereModel::class,
        PersonneBlesseeModel::class,
        SourceEauModel::class,
        SourceEnergieModel::class,
        TypeMachineModel::class,
        PaiementMobileModel::class,
        TypePieceModel::class,
        VarieteCacaoModel::class,
        LocaliteModel::class,
        TypeLocaliteModel::class,
        ProducteurModel::class,
        ProducteurMenageModel::class,
        ParcelleModel::class,
        RecuModel::class,
        LivraisonModel::class,
        FormationModel::class,
        TypeFormationModel::class,
        SousThemeFormationModel::class,
        DelegueModel::class,
        SuiviParcelleModel::class,
        IntrantModel::class,
        ParcelleMappingModel::class,
        CampagneModel::class,
        SuiviApplicationModel::class,
        ApplicateurModel::class,
        NotationModel::class,
        MagasinModel::class,
        InspectionQuestionnairesModel::class,
        EstimationModel::class,
        EnqueteSsrtModel::class,
        InspectionDTO::class,
        LienParenteModel::class,
        MoyenTransportModel::class,
        TypeDocumentModel::class,
        InfosProducteurDTO::class,
        OperateurModel::class,
        ThemeFormationModel::class,
        TypeProduitModel::class,
        DataDraftedModel::class,
        ConcernesModel::class,
        SectionModel::class,
        ProgrammeModel::class,
        DistributionArbreModel::class,
        EvaluationArbreModel::class,
        VisiteurFormationModel::class,
        ArbreModel::class,
        StaffFormationModel::class,
        MagasinCentralModel::class,
        EntrepriseModel::class,
        TransporteurModel::class,
        VehiculeModel::class,
        RemorqueModel::class,
        LivraisonCentralModel::class,
        LivraisonVerMagCentralModel::class,
        ApprovisionnementModel::class,
        PostPlantingModel::class,
        PostPlantingArbrDistribModel::class,
        SyncErreurModel::class,
        RefPfteModel::class,
        EnqueteMenageModel::class,
        VisitePlantationModel::class,
        SuiviEnfantModel::class,
        EnfantSuivableModel::class,
    ],
//    autoMigrations = [
//        AutoMigration(from = Constants.LAST_BUILD_VERSION, to = Constants.BUILD_VERSION)
//    ]
)
@TypeConverters(ListConverters::class)
abstract class ProgBandRoomDatabase : RoomDatabase() {

    abstract fun coopDao(): CoopDao
    abstract fun draftedDatasDao(): DraftedDatasDao
    abstract fun typeProduitDao(): TypeProduitDao
    abstract fun themeFormationDao(): ThemeFormationDao
    abstract fun sousThemeFormationDao(): SousThemeFormationDao
    abstract fun moyenTransport(): MoyenTransportDao
    abstract fun lienParenteDao(): LienParenteDao
    abstract fun infosProducteurDao(): InfosProducteurDao
    abstract fun typeDocumentDao(): TypeDocumentDao
    abstract fun operateurDao(): OperateurDao
    abstract fun inspectionDao(): InspectionDao
    abstract fun typeFormationDao(): TypeFormationDao
    abstract fun questionnaireDao(): QuestionnaireDao
    abstract fun enqueteSsrtDao(): EnqueteSsrteDao
    abstract fun estimationDao(): EstimationDao
    abstract fun magasinSectionDao(): MagasinDao
    abstract fun magasinCentralDao(): MagasinCentralDao
    abstract fun notationDao(): NotationDao
    abstract fun courEauDoa(): CourEauDao
    abstract fun applicateurDao(): ApplicateurDao
    abstract fun campagneDao(): CampagneDao
    abstract fun parcelleMappingDao(): ParcelleMappingDao
    abstract fun agentDoa(): AgentDao
    abstract fun eauUseeDoa(): EauUseeDao
    abstract fun gardeMachineDoa(): GardeMachineDao
    abstract fun lieuFormationDoa(): LieuFormationDao
    abstract fun nationaliteDoa(): NationaliteDao
    abstract fun niveauDoa(): NiveauDao
    abstract fun ordureMenagereDoa(): OrdureMenagereDao
    abstract fun persBlesseeDoa(): PersonneBlesseeDao
    abstract fun sourceEauDoa(): SourceEauDao
    abstract fun sourceEnergieDoa(): SourceEnergieDao
    abstract fun typeLocaliteDao(): TypeLocaliteDao
    abstract fun typeMachineDao(): TypeMachineDao
    abstract fun typePieceDao(): TypePieceDao
    abstract fun varieteCacaoDao(): VarieteCacaoDao
    abstract fun localiteDoa(): LocaliteDao
    abstract fun producteurDoa(): ProducteurDao
    abstract fun producteurMenageDoa(): ProducteurMenageDao
    abstract fun parcelleDao(): ParcelleDao
    abstract fun suiviParcelleDao(): SuiviParcelleDao
    abstract fun suiviApplicationDao(): SuiviApplicationDao
    abstract fun recuDao(): RecuDao
    abstract fun livraisonDao(): LivraisonDao
    abstract fun formationDao(): FormationDao
    abstract fun delegueDao(): DelegueDao
    abstract fun intrantDao(): IntrantDao
    abstract fun paiementMobileDao(): PaiementMobileDao
    abstract fun concernesDao(): ConcernesDao
    abstract fun sectionsDao(): SectionsDao
    abstract fun programmesDao(): ProgrammesDao
    abstract fun distributionArbreDao(): DistributionArbreDao
    abstract fun evaluationArbreDao(): EvaluationArbreDao
    abstract fun visiteurFormationDao(): VisiteurFormationDao
    abstract fun arbreDao(): ArbreDao
    abstract fun staffFormation(): StaffFormationDao
    abstract fun entrepriseDao(): EntrepriseDao
    abstract fun transporteurDao(): TransporteurDao
    abstract fun vehiculeDao(): VehiculeDao
    abstract fun remorqueDao(): RemorqueDao
    abstract fun livraisonCentralDao(): LivraisonCentralDao
    abstract fun livraisonVerMagCentralDao(): LivraisonVerMagCentralDao
    abstract fun approvisionnementDao(): ApprovisionnementDao
    abstract fun postplantingDao(): PostplantingDao
    abstract fun postPlantingArbrDistribDao(): PostPlantingArbrDistribDao
    abstract fun syncErreurDao(): SyncErreurDao
    abstract fun refPfteDao(): RefPfteDao
    abstract fun enqueteMenageDao(): EnqueteMenageDao
    abstract fun visitePlantationDao(): VisitePlantationDao
    abstract fun suiviEnfantDao(): SuiviEnfantDao
    abstract fun enfantSuivableDao(): EnfantSuivableDao


    companion object {
        var INSTANCE: ProgBandRoomDatabase? = null


        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }


        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }


        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }


        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }


        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_71_72 = object : Migration(71, 72) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout des nouvelles colonnes à la table EstimationModel
                database.execSQL("ALTER TABLE estimation ADD COLUMN ajustement TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN typeEstimation TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN recolteEstime TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN rendFinal TEXT")
            }
        }

        val MIGRATION_72_73 = object : Migration(72, 73) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout des nouvelles colonnes à la table EstimationModel
                database.execSQL("ALTER TABLE estimation ADD COLUMN ajustement TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN typeEstimation TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN recolteEstime TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN rendFinal TEXT")
            }
        }

        val MIGRATION_73_74 = object : Migration(73, 74) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout des nouvelles colonnes à la table EstimationModel
                database.execSQL("ALTER TABLE estimation ADD COLUMN ajustement TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN typeEstimation TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN recolteEstime TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN rendFinal TEXT")
            }
        }

        val MIGRATION_74_75 = object : Migration(74, 75) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout des nouvelles colonnes à la table EstimationModel
                database.execSQL("ALTER TABLE estimation ADD COLUMN ajustement TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN typeEstimation TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN recolteEstime TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN rendFinal TEXT")
            }
        }

        val MIGRATION_75_76 = object : Migration(75, 76) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout des nouvelles colonnes à la table EstimationModel
                database.execSQL("ALTER TABLE estimation ADD COLUMN ajustement TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN typeEstimation TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN recolteEstime TEXT")
                database.execSQL("ALTER TABLE estimation ADD COLUMN rendFinal TEXT")
            }
        }

        /**
         * Creation des tables du Suivi Enfant Travailleur (lot 5).
         *
         * Deux tables : les suivis saisis par l'agent, et le cache des enfants pouvant faire
         * l'objet d'un suivi. Ce module ne part pas d'un producteur mais d'un enfant deja recense,
         * dont la liste doit etre disponible hors ligne.
         *
         * Migration REELLE : la base est construite avec fallbackToDestructiveMigration(), une
         * montee de version sans migration effacerait les brouillons et les saisies non
         * transmises des agents.
         */
        val MIGRATION_81_82 = object : Migration(81, 82) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `suivi_enfant` (" +
                            "`uid` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "`id` INTEGER NOT NULL, " +
                            "`userid` INTEGER, " +
                            "`enfantId` TEXT, " +
                            "`dateEnquete` TEXT, " +
                            "`nomEnqueteur` TEXT, " +
                            "`nom` TEXT, " +
                            "`dateNaissance` TEXT, " +
                            "`sexe` TEXT, " +
                            "`lienParente` TEXT, " +
                            "`autreLienParente` TEXT, " +
                            "`raisonNeVitPasParents` TEXT, " +
                            "`autreRaisonNeVitPasParents` TEXT, " +
                            "`situationScolaire` TEXT, " +
                            "`niveauScolaire` TEXT, " +
                            "`autreRaisonNonScolarisation` TEXT, " +
                            "`extraitNaissance` TEXT, " +
                            "`presentDisponible` TEXT, " +
                            "`raisonAbsent` TEXT, " +
                            "`heuresTravailSemaine` TEXT, " +
                            "`joursTravail` TEXT, " +
                            "`heuresTravailJournee` TEXT, " +
                            "`autreRaisonTravailAbus` TEXT, " +
                            "`autreMesure` TEXT, " +
                            "`autreThemeSensibilisation` TEXT, " +
                            "`nombreHommesSensibilises` TEXT, " +
                            "`nombreFemmesSensibilisees` TEXT, " +
                            "`nombreGarconsSensibilises` TEXT, " +
                            "`nombreFillesSensibilisees` TEXT, " +
                            "`telephoneProducteurSensibilisation` TEXT, " +
                            "`photoSensibilisation` TEXT, " +
                            "`etatSoumission` TEXT, " +
                            "`actionsRemediationStringify` TEXT, " +
                            "`raisonNonScolarisationStringify` TEXT, " +
                            "`raisonPasExtraitStringify` TEXT, " +
                            "`situationsPfteStringify` TEXT, " +
                            "`raisonTravailAbusStringify` TEXT, " +
                            "`mesuresEnfantStringify` TEXT, " +
                            "`mesuresMenageStringify` TEXT, " +
                            "`mesuresCommunauteStringify` TEXT, " +
                            "`themesSensibilisationStringify` TEXT, " +
                            "`outilsSensibilisationStringify` TEXT, " +
                            "`isSynced` INTEGER NOT NULL, " +
                            "`origin` TEXT, " +
                            "`agentId` TEXT, " +
                            "`enfantNom` TEXT, " +
                            "`producteurNom` TEXT, " +
                            "`localiteNom` TEXT)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_suivi_enfant_uid` " +
                            "ON `suivi_enfant` (`uid`)"
                )

                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `enfant_suivable` (" +
                            "`uid` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "`idServeur` INTEGER NOT NULL, " +
                            "`codeEnfant` TEXT, " +
                            "`nom` TEXT, " +
                            "`dateNaissance` TEXT, " +
                            "`sexe` TEXT, " +
                            "`lienParente` TEXT, " +
                            "`autreLienParente` TEXT, " +
                            "`raisonNeVitPasParents` TEXT, " +
                            "`autreRaisonNeVitPasParents` TEXT, " +
                            "`situationScolaire` TEXT, " +
                            "`niveauScolaire` TEXT, " +
                            "`autreRaisonNonScolarisation` TEXT, " +
                            "`extraitNaissance` TEXT, " +
                            "`autreRaisonTravailAbus` TEXT, " +
                            "`autreMesure` TEXT, " +
                            "`producteurNom` TEXT, " +
                            "`localiteNom` TEXT, " +
                            "`agentId` TEXT)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_enfant_suivable_idServeur` " +
                            "ON `enfant_suivable` (`idServeur`)"
                )
            }
        }

        /**
         * Creation de la table des Visites Plantation (lot 4).
         *
         * Migration REELLE : la base est construite avec fallbackToDestructiveMigration(), une
         * montee de version sans migration effacerait les brouillons et les saisies non
         * transmises des agents.
         */
        val MIGRATION_80_81 = object : Migration(80, 81) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `visite_plantation` (" +
                            "`uid` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "`id` INTEGER NOT NULL, " +
                            "`userid` INTEGER, " +
                            "`sectionId` TEXT, " +
                            "`localiteId` TEXT, " +
                            "`producteurId` TEXT, " +
                            "`dateEnquete` TEXT, " +
                            "`jourVisite` TEXT, " +
                            "`nomEnqueteur` TEXT, " +
                            "`latitude` TEXT, " +
                            "`longitude` TEXT, " +
                            "`altitude` TEXT, " +
                            "`precisionGps` TEXT, " +
                            "`estProducteurRepondant` TEXT, " +
                            "`nomRepondant` TEXT, " +
                            "`titreRepondant` TEXT, " +
                            "`producteurDisponible` TEXT, " +
                            "`raisonIndisponibilite` TEXT, " +
                            "`datePlanification` TEXT, " +
                            "`autreRaisonRefus` TEXT, " +
                            "`consentement` TEXT, " +
                            "`superficiePlantation` TEXT, " +
                            "`nombreManoeuvresPermanents` TEXT, " +
                            "`manoeuvresPermanentsMoins18` TEXT, " +
                            "`nombreManoeuvresJournaliers` TEXT, " +
                            "`manoeuvresJournaliersMoins18` TEXT, " +
                            "`nombreEnfants0a4` TEXT, " +
                            "`nombreEnfants5a17` TEXT, " +
                            "`nombrePersonnesTrouvees` TEXT, " +
                            "`etatSoumission` TEXT, " +
                            "`raisonRefusStringify` TEXT, " +
                            "`enfantsStringify` TEXT, " +
                            "`isSynced` INTEGER NOT NULL, " +
                            "`origin` TEXT, " +
                            "`agentId` TEXT, " +
                            "`sectionNom` TEXT, " +
                            "`localiteNom` TEXT, " +
                            "`producteurNom` TEXT)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_visite_plantation_uid` " +
                            "ON `visite_plantation` (`uid`)"
                )
            }
        }

        /**
         * Creation de la table des Enquetes Menage (lot 3).
         *
         * Migration REELLE : la base est construite avec fallbackToDestructiveMigration(), une
         * montee de version sans migration effacerait les brouillons et les saisies non
         * transmises des agents.
         */
        val MIGRATION_79_80 = object : Migration(79, 80) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `enquete_menage` (" +
                            "`uid` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "`id` INTEGER NOT NULL, " +
                            "`userid` INTEGER, " +
                            "`sectionId` TEXT, " +
                            "`localiteId` TEXT, " +
                            "`producteurId` TEXT, " +
                            "`dateEnquete` TEXT, " +
                            "`nomEnqueteur` TEXT, " +
                            "`latitude` TEXT, " +
                            "`longitude` TEXT, " +
                            "`altitude` TEXT, " +
                            "`precisionGps` TEXT, " +
                            "`estProducteurRepondant` TEXT, " +
                            "`nomRepondant` TEXT, " +
                            "`titreRepondant` TEXT, " +
                            "`producteurDisponible` TEXT, " +
                            "`raisonIndisponibilite` TEXT, " +
                            "`datePlanification` TEXT, " +
                            "`autreRaisonRefus` TEXT, " +
                            "`consentement` TEXT, " +
                            "`situationMatrimoniale` TEXT, " +
                            "`nombreAdultes` TEXT, " +
                            "`nombreEnfants0a4` TEXT, " +
                            "`nombreEnfants5a17` TEXT, " +
                            "`aEnfantACharge` TEXT, " +
                            "`nombreEnfantsACharge` TEXT, " +
                            "`nombreEnfantsEnquetes` TEXT, " +
                            "`autreThemeSensibilisation` TEXT, " +
                            "`nombreHommesSensibilises` TEXT, " +
                            "`nombreFemmesSensibilisees` TEXT, " +
                            "`nombreGarconsSensibilises` TEXT, " +
                            "`nombreFillesSensibilisees` TEXT, " +
                            "`telephoneProducteurSensibilisation` TEXT, " +
                            "`photoSensibilisation` TEXT, " +
                            "`etatSoumission` TEXT, " +
                            "`raisonRefusStringify` TEXT, " +
                            "`themesSensibilisationStringify` TEXT, " +
                            "`outilsSensibilisationStringify` TEXT, " +
                            "`enfantsStringify` TEXT, " +
                            "`isSynced` INTEGER NOT NULL, " +
                            "`origin` TEXT, " +
                            "`agentId` TEXT, " +
                            "`sectionNom` TEXT, " +
                            "`localiteNom` TEXT, " +
                            "`producteurNom` TEXT)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_enquete_menage_uid` " +
                            "ON `enquete_menage` (`uid`)"
                )
            }
        }

        /**
         * Creation de la table generique des referentiels PFTE.
         *
         * Migration reelle, pour la meme raison que les precedentes.
         */
        val MIGRATION_78_79 = object : Migration(78, 79) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `ref_pfte` (" +
                            "`uid` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "`tableRef` TEXT NOT NULL, " +
                            "`idRef` INTEGER NOT NULL, " +
                            "`nom` TEXT NOT NULL, " +
                            "`parentId` INTEGER NOT NULL, " +
                            "`ordre` INTEGER NOT NULL, " +
                            "`agentId` TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_ref_pfte_tableRef_idRef` " +
                            "ON `ref_pfte` (`tableRef`, `idRef`)"
                )
            }
        }

        /**
         * Creation de la table des refus serveur.
         *
         * Migration REELLE, et non un corps vide comme les precedentes : la base est
         * construite avec fallbackToDestructiveMigration(), donc sans cette migration la
         * montee de version effacerait les brouillons et les saisies non transmises des
         * agents. Voir le lot "Fiabilisation de la mise a jour des donnees".
         */
        val MIGRATION_77_78 = object : Migration(77, 78) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `sync_erreur` (" +
                            "`uid` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
                            "`module` TEXT NOT NULL, " +
                            "`code` INTEGER NOT NULL, " +
                            "`message` TEXT NOT NULL, " +
                            "`heure` TEXT NOT NULL, " +
                            "`agentId` TEXT NOT NULL)"
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sync_erreur_module` ON `sync_erreur` (`module`)"
                )
            }
        }

        val MIGRATION_76_77 = object : Migration(76, 77) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout des nouvelles colonnes à la table EstimationModel
//                database.execSQL("ALTER TABLE estimation ADD COLUMN ajustement TEXT")
//                database.execSQL("ALTER TABLE estimation ADD COLUMN typeEstimation TEXT")
//                database.execSQL("ALTER TABLE estimation ADD COLUMN recolteEstime TEXT")
//                database.execSQL("ALTER TABLE estimation ADD COLUMN rendFinal TEXT")
            }
        }

//        val MIGRATION_76_77 = object : Migration(75, 76) {
//            override fun migrate(database: SupportSQLiteDatabase) {
//                // Ajout des nouvelles colonnes à la table EstimationModel
//                database.execSQL("ALTER TABLE parcelle ADD COLUMN otherTestVar TEXT")
//            }
//        }

        fun escapeSql(value: String): String {
            val builder = StringBuilder()
            for (char in value) {
                when (char) {
                    '\'', '%' -> builder.append(char) // Escape single quote and percent sign builder.append('')
                    else -> builder.append(char)
                }
            }
//            LogUtils.d(builder.toString())
            return "%"+builder.toString()+"%"
        }

        fun getDatabase(context: Context): ProgBandRoomDatabase? {
            return INSTANCE ?: synchronized(ProgBandRoomDatabase::class) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ProgBandRoomDatabase::class.java,
                    "progbandama.db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
//                    .addMigrations(MIGRATION_71_76)
                    .addMigrations(
//                        MIGRATION_71_72,
//                        MIGRATION_72_73,
//                        MIGRATION_73_74,
//                        MIGRATION_74_75,
//                        MIGRATION_75_76,
                        MIGRATION_76_77,
                        MIGRATION_77_78,
                        MIGRATION_78_79,
                        MIGRATION_79_80,
                        MIGRATION_80_81,
                        MIGRATION_81_82,
                        )
                    .build()
                INSTANCE = instance

                instance
            }
        }
    }
}
