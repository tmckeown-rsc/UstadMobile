package com.ustadmobile.lib.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ustadmobile.lib.database.annotation.UmEntity
import com.ustadmobile.lib.database.annotation.UmSyncLastChangedBy
import com.ustadmobile.lib.database.annotation.UmSyncLocalChangeSeqNum
import com.ustadmobile.lib.database.annotation.UmSyncMasterChangeSeqNum

@UmEntity(tableId = 67)
@Entity
open class SaleProduct() {

    @PrimaryKey(autoGenerate = true)
    var saleProductUid: Long = 0

    //Name eg: Blue ribbon
    var saleProductName: String? = null

    var saleProductNameDari: String? = null

    var saleProductDescDari: String? = null

    var saleProductNamePashto: String? = null

    var saleProductDescPashto: String? = null

    //Description eg: A blue ribbon used for gift wrapping.
    var saleProductDesc: String? = null

    //Date added in unix datetime
    var saleProductDateAdded: Long = 0

    //Person who added this product (person uid)
    var saleProductPersonAdded: Long = 0

    //Picture uid <-> SaleProductPicture 's pk
    var saleProductPictureUid: Long = 0

    //If the product active . False is effectively delete
    var saleProductActive: Boolean = false

    //If it is a category it is true. If it is a product it is false (default)
    var saleProductCategory: Boolean = false

    @UmSyncMasterChangeSeqNum
    var saleProductMCSN: Long = 0

    @UmSyncLocalChangeSeqNum
    var saleProductLCSN: Long = 0

    @UmSyncLastChangedBy
    var saleProductLCB: Int = 0

    init {
        this.saleProductCategory = false
    }

    constructor(name: String, decs: String) : this() {
        this.saleProductName = name
        this.saleProductDesc = decs
        this.saleProductActive = true
        this.saleProductCategory = false

    }

    constructor(name: String, decs: String, category: Boolean) : this() {
        this.saleProductName = name
        this.saleProductDesc = decs
        this.saleProductActive = true
        this.saleProductCategory = category

    }

    constructor(name: String, decs: String, category: Boolean, isActive: Boolean) : this() {
        this.saleProductName = name
        this.saleProductDesc = decs
        this.saleProductActive = isActive
        this.saleProductCategory = category

    }
}