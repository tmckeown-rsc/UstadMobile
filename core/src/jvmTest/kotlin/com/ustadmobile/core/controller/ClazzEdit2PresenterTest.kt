package com.ustadmobile.core.controller

import com.ustadmobile.lib.db.entities.ClazzWithHolidayCalendar
import io.ktor.client.features.json.defaultSerializer
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test

class ClazzEdit2PresenterTest {

    @Test
    fun testSerializer() {
        val clazzWithHolidays = ClazzWithHolidayCalendar()
        val outgoing = defaultSerializer().write(clazzWithHolidays)
        val str = (outgoing as TextContent).text
        val json = Json.stringify(ClazzWithHolidayCalendar.serializer(), clazzWithHolidays)
        Assert.assertNotNull(json)
    }

}