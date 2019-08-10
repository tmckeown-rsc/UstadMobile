package com.ustadmobile.lib.annotationprocessor.core

import androidx.room.*
import com.google.gson.Gson
import com.squareup.kotlinpoet.*
import com.ustadmobile.door.annotation.*
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.type.TypeMirror
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ustadmobile.lib.annotationprocessor.core.DbProcessorKtorServer.Companion.OPTION_KTOR_OUTPUT
import io.ktor.routing.Route
import com.ustadmobile.door.*
import java.util.*
import javax.lang.model.element.ElementKind
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.ExecutableType
import javax.tools.Diagnostic
import com.ustadmobile.door.DoorDatabaseSyncRepository
import com.ustadmobile.door.DoorDatabase
import kotlin.reflect.KClass

fun getEntitySyncTracker(entityEl: Element, processingEnv: ProcessingEnvironment): TypeMirror? {
    val syncEntityAnnotationIndex = entityEl.annotationMirrors.map {processingEnv.typeUtils.asElement(it.annotationType) as TypeElement }
            .indexOfFirst { it.qualifiedName.toString() == "com.ustadmobile.door.annotation.SyncableEntity" }
    if(syncEntityAnnotationIndex == -1)
        return null

    val annotationValue = entityEl.annotationMirrors[syncEntityAnnotationIndex].elementValues
            .filter { it.key.simpleName.toString() == "syncTrackerEntity" }.values.toList()[0]
    return annotationValue.value as TypeMirror
}

class DbProcessorSync: AbstractDbProcessor() {

    data class OutputDirs(val abstractOutputArg: String?, val implOutputArg: String?,
                          val ktorRouteOutputArg: String?)
    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment): Boolean {
        setupDb(roundEnv)
        messager.printMessage(Diagnostic.Kind.NOTE, "DbProcessorSync: Process")
        val (abstractOutputArg, implOutputArg, syncKtorRouteOutputArg) = OutputDirs(processingEnv.options[OPTION_ABSTRACT_OUTPUT_DIR],
                processingEnv.options[OPTION_IMPL_OUTPUT_DIR], processingEnv.options[OPTION_KTOR_OUTPUT])
        val (abstractOutputDir, implOutputDir, syncKtorRouteOutputDir) = listOf(abstractOutputArg, implOutputArg, syncKtorRouteOutputArg)
                .map {
                    if (it == null || it == "filer") {
                        processingEnv.options["kapt.kotlin.generated"]!!
                    } else {
                        implOutputArg!!
                    }
                }
        val dbs = roundEnv.getElementsAnnotatedWith(Database::class.java)

        for(dbTypeEl in dbs) {
            messager.printMessage(Diagnostic.Kind.NOTE, "DbProcessorSync: db: ${dbTypeEl.simpleName}")
            val (abstractFileSpec, implFileSpec, repoImplSpec) = generateSyncDaoInterfaceAndImpls(dbTypeEl as TypeElement)

            abstractFileSpec.writeTo(File(abstractOutputDir))
            implFileSpec.writeTo(File(implOutputDir))

            val syncRepoFileSpec = generateSyncRepository(dbTypeEl)
            syncRepoFileSpec.writeTo(File(implOutputDir))

            //TODO: use the normal ktor generator for this - it will refactor the query and do the required inserts
            val syncRouteFileSpec = generateSyncKtorRoute(dbTypeEl as TypeElement)
            syncRouteFileSpec.writeTo(File(syncKtorRouteOutputDir))
        }

        val daos = roundEnv.getElementsAnnotatedWith(Dao::class.java)
        daos.filter { !it.simpleName.endsWith(SUFFIX_SYNCDAO_ABSTRACT) }.forEach {daoElement ->
            messager.printMessage(Diagnostic.Kind.NOTE, "DbProcessorSync: DAO: ${daoElement.simpleName}")
            val daoTypeEl = daoElement as TypeElement
            val daoFileSpec = generateDaoSyncHelperInterface(daoTypeEl)
            daoFileSpec.writeTo(File(abstractOutputDir))
        }


        messager.printMessage(Diagnostic.Kind.NOTE, "DbProcessorSync: process Finished")
        return true
    }

    /**
     * Generates an interface that will be used for
     */
    fun generateDaoSyncHelperInterface(daoType: TypeElement): FileSpec {
        val syncableEntitiesOnDao = syncableEntitiesOnDao(daoType.asClassName(), processingEnv)
        val syncHelperInterface = TypeSpec.interfaceBuilder("${daoType.simpleName}_SyncHelper")

        syncableEntitiesOnDao.forEach {
            syncHelperInterface.addFunction(
                    FunSpec.builder("_replace${it.simpleName}")
                            .addParameter("entityList", List::class.asClassName().parameterizedBy(it))
                            .addModifiers(KModifier.ABSTRACT)
                            .build())
            val entitySyncTracker = getEntitySyncTracker(
                    processingEnv.elementUtils.getTypeElement(it.canonicalName), processingEnv)
            val entitySyncTrackerEl = processingEnv.typeUtils.asElement(entitySyncTracker)

            syncHelperInterface.addFunction(
                    FunSpec.builder("_replace${entitySyncTrackerEl.simpleName}")
                            .addParameter("entityTrackerList",
                                    List::class.asClassName().parameterizedBy(entitySyncTrackerEl.asType().asTypeName()))
                            .addModifiers(KModifier.ABSTRACT)
                            .build())

            syncHelperInterface.addFunction(
                    FunSpec.builder("_update${entitySyncTrackerEl.simpleName}Received")
                            .addParameter("status", BOOLEAN)
                            .addParameter("requestId", INT)
                            .addModifiers(KModifier.ABSTRACT)
                            .build())

        }

        return FileSpec.builder(pkgNameOfElement(daoType, processingEnv),
                "${daoType.simpleName}_SyncHelper")
                .addType(syncHelperInterface.build())
                .build()
    }


    fun generateSyncKtorRoute(dbType: TypeElement): FileSpec {
        val abstractDaoSimpleName = "${dbType.simpleName}$SUFFIX_SYNCDAO_ABSTRACT"
        val abstractDaoClassName = ClassName(pkgNameOfElement(dbType, processingEnv),
                abstractDaoSimpleName)
        val routeFileSpec = FileSpec.builder(pkgNameOfElement(dbType, processingEnv),
                "${dbType.simpleName}$SUFFIX_SYNC_ROUTE")
                .addImport("io.ktor.response", "header")
        val daoRouteFn = FunSpec.builder("${dbType.simpleName}$SUFFIX_SYNC_ROUTE")
                .receiver(Route::class)
                .addParameter("_dao", abstractDaoClassName)
                .addParameter("_db", DoorDatabase::class)
                .addParameter("_gson", Gson::class)

        val codeBlock = CodeBlock.builder()
        codeBlock.beginControlFlow("%M(%S)",
                MemberName("io.ktor.routing", "route"), abstractDaoSimpleName)
        syncableEntityTypesOnDb(dbType, processingEnv).forEach { entityType ->
            val syncableEntityInfo = SyncableEntityInfo(entityType.asClassName(), processingEnv)
            val entityTypeListClassName = List::class.asClassName().parameterizedBy(entityType.asClassName())
            val getAllSql = "SELECT * FROM ${entityType.simpleName}"
            val getAllFunSpec = FunSpec.builder("_findMasterUnsent${entityType.simpleName}")
                    .returns(entityTypeListClassName)
                    .addAnnotation(AnnotationSpec.builder(Query::class)
                            .addMember("%S", getAllSql).build())
                    .build()
            codeBlock.beginControlFlow("%M(%S)", DbProcessorKtorServer.GET_MEMBER,
                    "_findMasterUnsent${entityType.simpleName}")
                .add(generateKtorRouteSelectCodeBlock(getAllFunSpec, syncHelperDaoVarName = "_dao"))
                .endControlFlow()

            val replaceEntityFunSpec = FunSpec.builder("_replace${entityType.simpleName}")
                    .addParameter("entities", entityTypeListClassName)
                    .returns(UNIT)
                    .build()
            codeBlock.beginControlFlow("%M(%S)", DbProcessorKtorServer.POST_MEMBER,
                    "_replace${entityType.simpleName}")
                    .add(generateKtorPassToDaoCodeBlock(replaceEntityFunSpec))
                    .endControlFlow()

            codeBlock.add(generateUpdateTrackerReceivedCodeBlock(syncableEntityInfo.tracker,
                    syncHelperVarName = "_dao"))

        }
        codeBlock.endControlFlow()

        daoRouteFn.addCode(codeBlock.build())
        routeFileSpec.addFunction(daoRouteFn.build())
        return routeFileSpec.build()
    }


    data class SyncFileSpecs(val abstractFileSpec: FileSpec, val daoImplFileSpec: FileSpec, val repoImplFileSpec: FileSpec)


    fun generateSyncRepository(dbType: TypeElement): FileSpec {
        val dbClassName = dbType.asClassName()
        val syncDaoSimpleName = "${dbClassName.simpleName}${SUFFIX_SYNCDAO_ABSTRACT}"
        val syncRepoSimpleName =
                "${syncDaoSimpleName}_${DbProcessorRepository.SUFFIX_REPOSITORY}"
        val repoFileSpec = FileSpec.builder(dbClassName.packageName,
                syncRepoSimpleName)
        val daoClassName = ClassName(dbClassName.packageName,
                "${dbClassName.simpleName}$SUFFIX_SYNCDAO_ABSTRACT")
        val repoTypeSpec = newRepositoryClassBuilder(daoClassName, false)
                .addSuperinterface(DoorDatabaseSyncRepository::class as KClass<*>)

        val syncFnCodeBlock = CodeBlock.builder()

        repoTypeSpec.addFunction(FunSpec.builder("_findSyncNodeClientId")
                .addModifiers(KModifier.OVERRIDE)
                .returns(INT)
                .addCode("return _dao._findSyncNodeClientId()\n")
                .build())

        syncableEntityTypesOnDb(dbType, processingEnv).forEach { entityType ->
            val syncableEntityInfo = SyncableEntityInfo(entityType.asClassName(), processingEnv)
            val entityListTypeName = List::class.asClassName().parameterizedBy(entityType.asClassName())

            val replaceEntitiesFn = FunSpec.builder("_replace${entityType.simpleName}")
                    .addParameter("_entities", List::class.asClassName().parameterizedBy(entityType.asClassName()))
                    .addModifiers(KModifier.OVERRIDE)
                    .addCode("_dao._replace${entityType.simpleName}(_entities)\n")
                    .build()
            repoTypeSpec.addFunction(replaceEntitiesFn)


            repoTypeSpec.addFunction(FunSpec.builder("_replace${syncableEntityInfo.tracker.simpleName}")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("_entities",
                            List::class.asClassName().parameterizedBy(syncableEntityInfo.tracker))
                    .addCode("_dao._replace${syncableEntityInfo.tracker.simpleName}(_entities)\n")
                    .build())

            repoTypeSpec.addFunction(FunSpec.builder("_findLocalUnsent${entityType.simpleName}")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("destClientId", INT)
                    .addParameter("limit", INT)
                    .addCode("return _dao._findLocalUnsent${entityType.simpleName}(destClientId, limit)\n")
                    .returns(List::class.asClassName().parameterizedBy(syncableEntityInfo.syncableEntity))
                    .build())

            repoTypeSpec.addFunction(FunSpec.builder("_update${syncableEntityInfo.tracker.simpleName}Received")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("status", BOOLEAN)
                    .addParameter("requestId", INT)
                    .addCode("_dao._update${syncableEntityInfo.tracker.simpleName}Received(status, requestId)\n")
                    .build())


            val findMasterUnsentFnSpec = FunSpec.builder("_findMasterUnsent${entityType.simpleName}")
                    .returns(entityListTypeName)
                    .addModifiers(KModifier.SUSPEND)
                    .addAnnotation(AnnotationSpec.builder(Query::class)
                            .addMember(CodeBlock.of("%S", "SELECT * FROM ${entityType.simpleName}")).build())

            syncFnCodeBlock.beginControlFlow("if(entities == null || %T::class in entities)",
                    entityType)
                    .add(generateRepositoryGetSyncableEntitiesFun(findMasterUnsentFnSpec.build(),
                            syncDaoSimpleName, syncHelperDaoVarName = "_dao", addReturnDaoResult = false))
                    .add("val _entities = _findLocalUnsent${entityType.simpleName}(0, 100)\n")
                    .beginControlFlow("if(!_entities.isEmpty())")
                    .add(generateKtorRequestCodeBlockForMethod(httpEndpointVarName = "_endpoint",
                            daoName = syncDaoSimpleName, methodName = replaceEntitiesFn.name,
                            httpResultVarName = "_sendResult", httpResponseVarName = "_sendHttpResponse",
                            httpResultType = UNIT, params = replaceEntitiesFn.parameters))
                    .add(generateReplaceSyncableEntitiesTrackerCodeBlock("_entities",
                            entityListTypeName, syncHelperDaoVarName = "_dao", clientIdVarName = "0",
                            reqIdVarName = "0", processingEnv = processingEnv))
                    .endControlFlow()
                    .endControlFlow()

        }


        repoTypeSpec.addFunction(FunSpec.builder("sync")
                .addModifiers(KModifier.OVERRIDE, KModifier.SUSPEND)
                .addParameter("entities", List::class.asClassName().parameterizedBy(
                        KClass::class.asClassName().parameterizedBy(STAR)).copy(nullable = true))
                .addCode(syncFnCodeBlock.build())
                .build())

        return repoFileSpec.addType(repoTypeSpec.build()).build()
    }

    /**
     *
     * @return Pair of FileSpecs: first = the abstract DAO filespec, the second one is the implementation
     */
    fun generateSyncDaoInterfaceAndImpls(dbType: TypeElement): SyncFileSpecs {
        val abstractDaoSimpleName = "${dbType.simpleName}$SUFFIX_SYNCDAO_ABSTRACT"
        val abstractDaoClassName = ClassName(pkgNameOfElement(dbType, processingEnv),
                abstractDaoSimpleName)
        val abstractDaoTypeSpec = TypeSpec.classBuilder(abstractDaoSimpleName)
                .addAnnotation(Dao::class.asClassName())
                .addModifiers(KModifier.ABSTRACT)
                .addSuperinterface(ClassName(pkgNameOfElement(dbType, processingEnv), "I$abstractDaoSimpleName"))

        messager.printMessage(Diagnostic.Kind.NOTE, "DbProcessorSync: generateSyncDaoInterfaceAndImpl: ${dbType.simpleName}")
        daosOnDb(dbType.asClassName(), processingEnv, excludeDbSyncDao = true)
                .filter { syncableEntitiesOnDao(it, processingEnv).isNotEmpty()}
                .forEach {
                    abstractDaoTypeSpec.addSuperinterface(
                            ClassName(it.packageName, "${it.simpleName}_SyncHelper"))
                }

        val abstractDaoInterfaceTypeSpec = TypeSpec.interfaceBuilder("I$abstractDaoSimpleName")
        messager.printMessage(Diagnostic.Kind.NOTE, "DbProcessorSync: generateSyncDaoInterfaceAndImpl " +
                "- make DAO interface ${dbType.simpleName}")

        val abstractFileSpec = FileSpec.builder(pkgNameOfElement(dbType, processingEnv),
                abstractDaoSimpleName)
                .addImport("com.ustadmobile.door", "DoorDbType")


        val implDaoSimpleName = "${dbType.simpleName}$SUFFIX_SYNCDAO_IMPL"
        val implDaoClassName = ClassName(pkgNameOfElement(dbType, processingEnv),
                implDaoSimpleName)
        val implDaoTypeSpec = jdbcDaoTypeSpecBuilder(implDaoSimpleName, abstractDaoClassName)
        val implFileSpec = FileSpec.builder(pkgNameOfElement(dbType, processingEnv),
                implDaoSimpleName)
                .addImport("com.ustadmobile.door", "DoorDbType")

        //TODO: this is no longer made here - remove it from this fn
        val repoImplSimpleName = "${dbType.simpleName}${DbProcessorRepository.SUFFIX_REPOSITORY}"
        val repoImplSpec = FileSpec.builder(pkgNameOfElement(dbType, processingEnv),
                repoImplSimpleName)

        val (abstractDaoFindSyncClientIdFn, implFindSyncClientIdFn) = generateAbstractAndImplQueryFunSpecs(
                "SELECT nodeClientId FROM SyncNode", "_findSyncNodeClientId",
                INT, listOf(), addReturnStmt = true)
        abstractDaoTypeSpec.addFunction(abstractDaoFindSyncClientIdFn)
        implDaoTypeSpec.addFunction(implFindSyncClientIdFn)

        syncableEntityTypesOnDb(dbType, processingEnv).forEach {entityType ->
            val syncableEntityInfo = SyncableEntityInfo(entityType.asClassName(), processingEnv)
            val entityListClassName = List::class.asClassName().parameterizedBy(entityType.asClassName())
            val entitySyncTrackerListClassName = List::class.asClassName().parameterizedBy(syncableEntityInfo.tracker)

            //Generate the find local unsent changes function for this entity
            val findLocalUnsentSql = "SELECT * FROM " +
                    "(SELECT * FROM ${entityType.simpleName} ) AS ${entityType.simpleName} " +
                    "WHERE " +
                    "${syncableEntityInfo.entityLastChangedByField.name} = (SELECT nodeClientId FROM SyncNode) AND " +
                    "(${entityType.simpleName}.${syncableEntityInfo.entityLocalCsnField.name} > " +
                    "COALESCE((SELECT ${syncableEntityInfo.trackerCsnField.name} FROM ${syncableEntityInfo.tracker.simpleName} " +
                    "WHERE ${syncableEntityInfo.trackerPkField.name} = ${entityType.simpleName}.${syncableEntityInfo.entityPkField.name} " +
                    "AND ${syncableEntityInfo.trackerDestField.name} = :destClientId), 0)" +
                    ") LIMIT :limit"


            val findUnsentParamsList = listOf(ParameterSpec.builder("destClientId", INT).build(),
                    ParameterSpec.builder("limit", INT).build())
            val (abstractLocalUnsentChangeFun, implLocalUnsetChangeFun) =
                    generateAbstractAndImplQueryFunSpecs(findLocalUnsentSql,
                            "_findLocalUnsent${entityType.simpleName}",
                            entityListClassName, findUnsentParamsList)
            abstractDaoTypeSpec.addFunction(abstractLocalUnsentChangeFun)
            implDaoTypeSpec.addFunction(implLocalUnsetChangeFun)

            //generate an upsert function for the entity itself
            val (abstractInsertEntityFun, implInsertEntityFun, abstractInterfaceInsertEntityFun) =
                generateAbstractAndImplUpsertFuns(
                    "_replace${entityType.simpleName}",
                    ParameterSpec.builder("_entities", entityListClassName).build(),
                    implDaoTypeSpec, abstractFunIsOverride = true)
            abstractDaoTypeSpec.addFunction(abstractInsertEntityFun)
            abstractDaoInterfaceTypeSpec.addFunction(abstractInterfaceInsertEntityFun)
            implDaoTypeSpec.addFunction(implInsertEntityFun)

            val (abstractInsertTrackerFun, implInsertTrackerFun, abstractInterfaceInsertTrackerFun) =
                    generateAbstractAndImplUpsertFuns(
                    "_replace${syncableEntityInfo.tracker.simpleName}",
                    ParameterSpec.builder("_entities", entitySyncTrackerListClassName).build(),
                    implDaoTypeSpec, abstractFunIsOverride = true)
            abstractDaoTypeSpec.addFunction(abstractInsertTrackerFun)
            implDaoTypeSpec.addFunction(implInsertTrackerFun)
            abstractDaoInterfaceTypeSpec.addFunction(abstractInterfaceInsertTrackerFun)

            //generate an update function that can be used to set the status of the sync tracker
            val updateTrackerReceivedSql = "UPDATE ${syncableEntityInfo.tracker.simpleName} SET " +
                    "${syncableEntityInfo.trackerReceivedField.name} = :status WHERE " +
                    "${syncableEntityInfo.trackerReqIdField.name} = :requestId"
            val (abstractUpdateTrackerFun, implUpdateTrackerFun, abstractInterfaceUpdateTrackerFun) =
                    generateAbstractAndImplQueryFunSpecs(updateTrackerReceivedSql,
                            "_update${syncableEntityInfo.tracker.simpleName}Received",
                            UNIT, listOf(ParameterSpec.builder("status", BOOLEAN).build(),
                            ParameterSpec.builder("requestId", INT).build()),
                            addReturnStmt = false, abstractFunIsOverride = true)
            abstractDaoTypeSpec.addFunction(abstractUpdateTrackerFun)
            implDaoTypeSpec.addFunction(implUpdateTrackerFun)
            abstractDaoInterfaceTypeSpec.addFunction(abstractInterfaceUpdateTrackerFun)
        }


        abstractFileSpec.addType(abstractDaoInterfaceTypeSpec.build())
        abstractFileSpec.addType(abstractDaoTypeSpec.build())

        implFileSpec.addType(implDaoTypeSpec.build())
        messager.printMessage(Diagnostic.Kind.NOTE, "DbProcessorSync: generateSyncDaoInterfaceAndImpl " +
                "- finished making sync DAO interface for ${dbType.simpleName}")
        return SyncFileSpecs(abstractFileSpec.build(), implFileSpec.build(), repoImplSpec.build())
    }

    data class AbstractImplAndInterfaceFunSpecs(val abstractFunSpec: FunSpec, val implFunSpec: FunSpec,
                                                val interfaceFunSpec: FunSpec)
    private fun generateAbstractAndImplUpsertFuns(funName: String, paramSpec: ParameterSpec,
                                                  daoTypeBuilder: TypeSpec.Builder,
                                                  abstractFunIsOverride: Boolean = false): AbstractImplAndInterfaceFunSpecs {
        val funBuilders = (0..2).map {
            FunSpec.builder(funName)
                    .returns(UNIT)
                    .addParameter(paramSpec)
        }
        funBuilders[0].addModifiers(KModifier.ABSTRACT)
        funBuilders[2].addModifiers(KModifier.ABSTRACT)

        if(abstractFunIsOverride) {
            funBuilders[0].addModifiers(KModifier.OVERRIDE)
        }

        funBuilders[0].addAnnotation(AnnotationSpec.builder(Insert::class)
                .addMember("onConflict = %T.REPLACE", OnConflictStrategy::class).build())

        funBuilders[1].addModifiers(KModifier.OVERRIDE)
        funBuilders[1].addCode(generateInsertCodeBlock(paramSpec, UNIT, daoTypeBuilder,
                true))

        return AbstractImplAndInterfaceFunSpecs(funBuilders[0].build(), funBuilders[1].build(),
                funBuilders[2].build())
    }

    private fun generateAbstractAndImplQueryFunSpecs(querySql: String,
                                             funName: String,
                                             returnType: TypeName,
                                             params: List<ParameterSpec>,
                                             addReturnStmt: Boolean = true,
                                             abstractFunIsOverride: Boolean = false): AbstractImplAndInterfaceFunSpecs {
        val funBuilders = (0..2).map {
            FunSpec.builder(funName)
                    .returns(returnType)
                    .addParameters(params)
        }

        funBuilders[0].addModifiers(KModifier.ABSTRACT)
        if(abstractFunIsOverride)
            funBuilders[0].addModifiers(KModifier.OVERRIDE)
        funBuilders[1].addModifiers(KModifier.OVERRIDE)
        funBuilders[2].addModifiers(KModifier.ABSTRACT)


        funBuilders[0].addAnnotation(AnnotationSpec.builder(Query::class)
                .addMember("value = %S", querySql).build())

        funBuilders[1].addCode(generateQueryCodeBlock(returnType,
                params.map { it.name to it.type}.toMap(), querySql,
                null, null))

        if(addReturnStmt) {
            funBuilders[1].addCode("return _result\n")
        }

        return AbstractImplAndInterfaceFunSpecs(funBuilders[0].build(),
                funBuilders[1].build(), funBuilders[2].build())
    }


    companion object {

        const val OPTION_IMPL_OUTPUT_DIR = "door_sync_impl_output"

        const val OPTION_ABSTRACT_OUTPUT_DIR = "door_sync_interface_output"

        const val SUFFIX_SYNCDAO_ABSTRACT = "SyncDao"

        const val SUFFIX_SYNCDAO_IMPL = "SyncDao_JdbcKt"

        const val SUFFIX_SYNC_ROUTE = "SyncDao_Route"
    }

}