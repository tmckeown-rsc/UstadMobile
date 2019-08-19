package com.ustadmobile.lib.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
open class ContainerEntry() {

    @PrimaryKey(autoGenerate = true)
    var ceUid: Long = 0

    @ColumnInfo(index = true)
    var ceContainerUid: Long = 0

    var cePath: String? = null

    var ceCefUid: Long = 0

    constructor(cePath: String, container: Container, entryFile: ContainerEntryFile) : this() {
        this.cePath = cePath
        this.ceCefUid = entryFile.cefUid
        this.ceContainerUid = container.containerUid
    }
}
