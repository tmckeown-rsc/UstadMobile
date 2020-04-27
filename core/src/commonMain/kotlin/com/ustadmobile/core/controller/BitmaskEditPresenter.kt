package com.ustadmobile.core.controller

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.generated.locale.MessageID
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UstadMobileSystemImpl
import com.ustadmobile.core.model.BitmaskFlag
import com.ustadmobile.core.util.LongWrapper
import com.ustadmobile.core.util.ext.putEntityAsJson
import com.ustadmobile.core.view.BitmaskEditView
import com.ustadmobile.door.DoorLifecycleOwner
import com.ustadmobile.door.DoorLiveData

import com.ustadmobile.lib.db.entities.UmAccount
import com.ustadmobile.core.view.UstadView.Companion.ARG_ENTITY_UID
import com.ustadmobile.core.view.UstadEditView.Companion.ARG_ENTITY_JSON
import com.ustadmobile.door.DoorMutableLiveData
import com.ustadmobile.lib.db.entities.Clazz
import kotlinx.serialization.json.Json


class BitmaskEditPresenter(context: Any,
                          arguments: Map<String, String>, view: BitmaskEditView,
                          lifecycleOwner: DoorLifecycleOwner,
                          systemImpl: UstadMobileSystemImpl,
                          db: UmAppDatabase, repo: UmAppDatabase,
                          activeAccount: DoorLiveData<UmAccount?> = UmAccountManager.activeAccountLiveData)
    : UstadEditPresenter<BitmaskEditView, LongWrapper>(context, arguments, view, lifecycleOwner, systemImpl,
        db, repo, activeAccount) {






    override val persistenceMode: PersistenceMode
        get() = PersistenceMode.JSON

    /*
     * TODO: Add any required one to many join helpers here - use these templates (type then hit tab)
     * onetomanyhelper: Adds a one to many relationship using OneToManyJoinEditHelper
     */
    override fun onCreate(savedState: Map<String, String>?) {
        super.onCreate(savedState)

        //TODO: Set any additional fields (e.g. joinlist) on the view
    }

    override suspend fun onLoadEntityFromDb(db: UmAppDatabase): LongWrapper? {
        val entityUid = arguments[ARG_ENTITY_UID]?.toLong() ?: 0L

        //this is not loaded from the database
        return null
    }

    override fun onLoadFromJson(bundle: Map<String, String>): LongWrapper? {
        super.onLoadFromJson(bundle)

        val entityJsonStr = bundle[ARG_ENTITY_JSON]
        var longWrapper: LongWrapper? = null
        if(entityJsonStr != null) {
            longWrapper = Json.parse(LongWrapper.serializer(), entityJsonStr)
        }else {
            longWrapper = LongWrapper(0L)
        }

        view.bitmaskList = DoorMutableLiveData(
                FLAGS_AVAILABLE.map { BitmaskFlag(it.flagVal, it.messageId,
                (longWrapper.longValue and it.flagVal) == it.flagVal) })

        return longWrapper
    }

    override fun onSaveInstanceState(savedState: MutableMap<String, String>) {
        super.onSaveInstanceState(savedState)
        val entityVal = entity
        savedState.putEntityAsJson(ARG_ENTITY_JSON, null,
                entityVal)
    }

    override fun handleClickSave(entity: LongWrapper) {
        val saveVal = view.bitmaskList?.getValue()?.fold(0L, {acc, flag ->
            acc + (if(flag.enabled) flag.flagVal else 0)
        }) ?: 0L
        view.finishWithResult(listOf(LongWrapper(saveVal)))
    }

    companion object {

        //TODO: Add constants for keys that would be used for any One To Many Join helpers


        val FLAGS_AVAILABLE = listOf(
                BitmaskFlag(Clazz.CLAZZ_FEATURE_ACTIVITY, MessageID.activity, false),
                BitmaskFlag(Clazz.CLAZZ_FEATURE_ATTENDANCE, MessageID.attendance, false),
                BitmaskFlag(Clazz.CLAZZ_FEATURE_ASSIGNMENT, MessageID.assignments, false),
                BitmaskFlag(Clazz.CLAZZ_FEATURE_SEL, MessageID.sel, false)
        )
    }

}