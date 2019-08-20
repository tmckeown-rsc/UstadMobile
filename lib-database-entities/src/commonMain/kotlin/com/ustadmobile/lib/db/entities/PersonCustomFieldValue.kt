package com.ustadmobile.lib.db.entities


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
class PersonCustomFieldValue() {

    @PrimaryKey(autoGenerate = true)
    var personCustomFieldValueUid: Long = 0

    var personCustomFieldValuePersonCustomFieldUid: Long = 0

    var personCustomFieldValuePersonUid: Long = 0

    var fieldValue: String? = null
}
