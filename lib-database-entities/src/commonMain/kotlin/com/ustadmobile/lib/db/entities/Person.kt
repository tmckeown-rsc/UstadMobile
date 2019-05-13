package com.ustadmobile.lib.db.entities

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmPrimaryKey
import com.ustadmobile.lib.database.annotation.UmSyncLastChangedBy
import com.ustadmobile.lib.database.annotation.UmSyncLocalChangeSeqNum
import com.ustadmobile.lib.database.annotation.UmSyncMasterChangeSeqNum

import com.ustadmobile.lib.db.entities.Person.Companion.TABLE_ID

/**
 * Created by mike on 3/8/18.
 */

@UmEntity(tableId = TABLE_ID)
@Entity
class Person {

    @UmPrimaryKey(autoGenerateSyncable = true)
    @PrimaryKey
    var personUid: Long = 0

    var username: String? = null

    var firstNames: String? = null

    var lastName: String? = null

    var emailAddr: String? = null

    var phoneNum: String? = null

    var gender: Int = 0

    var active: Boolean = false

    var admin: Boolean = false

    @UmSyncMasterChangeSeqNum
    var personMasterChangeSeqNum: Long = 0

    @UmSyncLocalChangeSeqNum
    var personLocalChangeSeqNum: Long = 0

    @UmSyncLastChangedBy
    var personLastChangedBy: Int = 0

    constructor()

    constructor(username: String, firstNames: String, lastName: String) {
        this.username = username
        this.firstNames = firstNames
        this.lastName = lastName
    }

    companion object {

        const val TABLE_ID = 9

        const val GENDER_UNSET = 0

        const val GENDER_FEMALE = 1

        const val GENDER_MALE = 2

        const val GENDER_OTHER = 4
    }
}