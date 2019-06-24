package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmPrimaryKey

@UmEntity
@Entity
class DiscussionMessage() : SyncableEntity {

    @UmPrimaryKey
    @PrimaryKey
    var discussionMessageUid: Long = 0

    var posterPersonUid: Long = 0

    var message: String? = null

    override var masterChangeSeqNum: Long = 0

    override var localChangeSeqNum: Long = 0
}