package com.ustadmobile.port.sharedse.contentformats.xapi.endpoints

class StatementRequestException(s: String?) : IllegalArgumentException(s) {

    var errorCode = 404

    constructor(s: String, errorCode: Int) : this(s) {
        this.errorCode = errorCode
    }


}