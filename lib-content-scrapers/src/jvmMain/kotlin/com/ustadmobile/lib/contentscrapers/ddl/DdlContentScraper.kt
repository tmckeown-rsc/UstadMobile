package com.ustadmobile.lib.contentscrapers.ddl

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.db.dao.ContainerETagDao
import com.ustadmobile.core.db.dao.ContentCategoryDao
import com.ustadmobile.core.db.dao.ContentCategorySchemaDao
import com.ustadmobile.lib.contentscrapers.ContentScraperUtil
import com.ustadmobile.lib.contentscrapers.ScraperConstants.EMPTY_STRING
import com.ustadmobile.lib.contentscrapers.ScraperConstants.TIME_OUT_SELENIUM
import com.ustadmobile.lib.contentscrapers.UMLogUtil
import com.ustadmobile.lib.contentscrapers.abztract.HarScraper
import com.ustadmobile.lib.contentscrapers.abztract.ScraperException
import com.ustadmobile.lib.contentscrapers.ddl.IndexDdlContent.Companion.DDL
import com.ustadmobile.lib.db.entities.ContainerETag
import com.ustadmobile.lib.db.entities.ContentEntry
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.LICENSE_TYPE_CC_BY
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.LICENSE_TYPE_CC_BY_NC_ND
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.LICENSE_TYPE_CC_BY_NC_SA
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.LICENSE_TYPE_CC_BY_ND
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.LICENSE_TYPE_CC_BY_SA
import com.ustadmobile.lib.db.entities.ContentEntry.Companion.LICENSE_TYPE_PUBLIC_DOMAIN
import com.ustadmobile.lib.db.entities.ContentEntryRelatedEntryJoin.Companion.REL_TYPE_SEE_ALSO
import com.ustadmobile.lib.db.entities.ContentEntryRelatedEntryJoin.Companion.REL_TYPE_TRANSLATED_VERSION
import kotlinx.coroutines.runBlocking
import net.lightbody.bmp.core.har.HarEntry
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.exception.ExceptionUtils
import org.jsoup.Jsoup
import org.openqa.selenium.By
import org.openqa.selenium.NoSuchElementException
import org.openqa.selenium.TimeoutException
import org.openqa.selenium.WebElement
import org.openqa.selenium.support.ui.ExpectedConditions
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.util.*


/**
 * Once the resource page is opened.
 * You can download the list of files by searching with css selector - span.download-item a[href]
 * The url may contain spaces and needs to be encoded. This is done by constructing the url into a uri
 * Check if the file was downloaded before with etag or last modified
 * Create the content entry
 */
class DdlContentScraper(containerDir: File, db: UmAppDatabase, contentEntryUid: Long) : HarScraper(containerDir, db, contentEntryUid) {

    private val categorySchemaDao: ContentCategorySchemaDao
    private val contentCategoryDao: ContentCategoryDao
    private val containerEtagDao: ContainerETagDao
    private val repository: UmAppDatabase = db

    private lateinit var contentEntry: ContentEntry

    private val licenseList = listOf(
            "CC 0" to LICENSE_TYPE_PUBLIC_DOMAIN,
            "public domain" to LICENSE_TYPE_PUBLIC_DOMAIN,
            "دامنه عمومی" to LICENSE_TYPE_PUBLIC_DOMAIN,
            "عامه لمن" to LICENSE_TYPE_PUBLIC_DOMAIN,
            "CC BY" to LICENSE_TYPE_CC_BY,
            "CC BY-SA" to LICENSE_TYPE_CC_BY_SA,
            "CC BY-ND" to LICENSE_TYPE_CC_BY_ND,
            "CC BY-NC-SA" to LICENSE_TYPE_CC_BY_NC_SA,
            "CC BY-NC-ND" to LICENSE_TYPE_CC_BY_NC_ND)

    init {
        categorySchemaDao = repository.contentCategorySchemaDao
        contentCategoryDao = repository.contentCategoryDao
        containerEtagDao = repository.containerETagDao
    }

    override fun scrapeUrl(sourceUrl: String) {

        var fileUrl: URL? = null
        var eTagValue: String? = null
        val scraperResult: HarScraperResult?

        try {

            scraperResult = startHarScrape(sourceUrl, {

                it.until(ExpectedConditions.invisibilityOfElementLocated(By.cssSelector("div.se-pre-con")))
                it.until<WebElement>(ExpectedConditions.elementToBeClickable(
                        By.cssSelector("span.download-item a[href]")))
                val list = chromeDriver.findElements(By.cssSelector("span.download-item a[href]"))
                if (list.isNotEmpty()) {
                    val href = list[0].getAttribute("href")
                    list[0].click()
                    waitForJSandJQueryToLoad(it)

                    fileUrl = URL(URL(sourceUrl), href)

                    var fileEntry: HarEntry? = null

                    var counterRequest = 0
                    while (fileEntry == null && counterRequest < TIME_OUT_SELENIUM) {
                        fileEntry = proxy.har.log.entries.find { harEntry ->
                            harEntry.request.url == fileUrl.toString()
                        }
                        Thread.sleep(1000)
                        counterRequest++
                    }

                    if (fileEntry == null) {
                        throw ScraperException(ERROR_TYPE_LINK_NOT_FOUND, "no request found for link")
                    }

                    val isMediaFile = isMediaUrl(fileUrl.toString())

                    if (isMediaFile) {
                        return@startHarScrape
                    }


                    var counterResponse = 0
                    while (fileEntry.response.content.text.isNullOrEmpty() && counterResponse < TIME_OUT_SELENIUM) {
                        Thread.sleep(1000)
                        counterResponse++
                    }

                }

            }, addHarContent = false, filters = listOf { entry ->

                if (entry.request.url != fileUrl.toString()) {
                    entry.response = null
                }

                entry
            }) {

                val entry = it.har.log.entries.find { harEntry ->
                    harEntry.request.url == sourceUrl
                }

                if (entry == null) {
                    throw ScraperException(ERROR_TYPE_NO_SOURCE_URL_FOUND, "no source url found in har entry")
                }

                val doc = Jsoup.parse(entry.response.content.text)

                val thumbnail = doc!!.selectFirst("aside img")?.attr("src") ?: EMPTY_STRING

                val description = doc.selectFirst("meta[name=description]")?.attr("content")
                val author = doc.selectFirst("article.resource-view-details h3:contains(Author) ~ p")?.text()
                        ?: doc.selectFirst("article.resource-view-details h3:contains(نویسنده) ~ p")?.text()
                        ?: doc.selectFirst("article.resource-view-details h3:contains(لیکونکی) ~ p")?.text()
                        ?: EMPTY_STRING

                val publisher = doc.selectFirst("article.resource-view-details a[href*=publisher]")?.text()
                        ?: DDL

                val twoLangCode = sourceUrl.substring(sourceUrl.indexOf(".af/") + 3).substringBefore("/")

                val langEntity = ContentScraperUtil.insertOrUpdateLanguageByTwoCode(db.languageDao, twoLangCode)

                val licenseText = doc.selectFirst("article.resource-view-details h3:contains(جواز/ د چاپ حق لرونکی) ~ p")?.text()
                        ?: doc.selectFirst("article.resource-view-details h3:contains(License) ~ p")?.text()
                        ?: doc.selectFirst("article.resource-view-details h3:contains(جواز/ دارنده حق چاپ) ~ p")?.text()
                        ?: EMPTY_STRING

                val foundMatch = licenseList.firstOrNull { pair ->
                    licenseText.contains(pair.first)
                }

                val licenseType = foundMatch?.second ?: 0
                if (foundMatch == null && licenseText.isNotEmpty()) {
                    UMLogUtil.logError("found license: $licenseText that didn't match the list for $sourceUrl ")
                }

                contentEntry = ContentScraperUtil.createOrUpdateContentEntry(sourceUrl, doc.title(),
                        sourceUrl, publisher, licenseType, langEntity.langUid, null, description, true, author,
                        thumbnail, EMPTY_STRING, EMPTY_STRING, ContentEntry.ARTICLE_TYPE, contentEntryDao)

                if (licenseType == 0) {
                    throw ScraperException(ERROR_TYPE_INVALID_LICENSE, "License type not supported")
                }

                val subjectContainer = doc.select("article.resource-view-details a[href*=level]")

                val schemeName = when (twoLangCode) {
                    "en" -> "Resource Level"
                    "fa" -> "سطح منبع"
                    "ps" -> "د سرچینې کچي"
                    else -> "Resource Level"
                }

                val ddlSchema = ContentScraperUtil.insertOrUpdateSchema(categorySchemaDao, schemeName, "ddl/resource-level/$twoLangCode")

                val subjectList = subjectContainer.select("a")

                for (subject in subjectList) {

                    val title = subject.attr("title")

                    val categoryEntry = ContentScraperUtil.insertOrUpdateCategoryContent(contentCategoryDao, ddlSchema, title)
                    ContentScraperUtil.insertOrUpdateChildWithMultipleCategoriesJoin(
                            db.contentEntryContentCategoryJoinDao, categoryEntry, contentEntry)

                }

                val relatedList = doc.select("div.resource-related-items-box div a[href]")

                relatedList.forEach { element ->

                    val relatedHref = element.attr("href")
                    val index = relatedHref.indexOf("af/")
                    val relatedUrl = StringBuilder(relatedHref).insert(index + 3, "$twoLangCode/").toString()

                    val relatedEntry = ContentScraperUtil.insertTempContentEntry(contentEntryDao, relatedUrl, contentEntry.primaryLanguageUid, element.text()
                            ?: "")

                    ContentScraperUtil.insertOrUpdateRelatedContentJoin(db.contentEntryRelatedEntryJoinDao, relatedEntry, contentEntry, REL_TYPE_SEE_ALSO)
                }

                val translatedList = doc.select("article.resource-view-details a[title=language]:not([style])")

                if (translatedList.size > 1) {

                    translatedList.forEach { element ->

                        if (element.attr("hreflang") == twoLangCode) {
                            return@forEach
                        }

                        val relatedTwoCode = element.attr("hreflang")
                        val relatedLink = element.attr("href")

                        val translatedEntry = ContentScraperUtil.insertTempContentEntry(contentEntryDao, relatedLink, ContentScraperUtil.insertOrUpdateLanguageByTwoCode(db.languageDao, relatedTwoCode).langUid, "")
                        ContentScraperUtil.insertOrUpdateRelatedContentJoin(db.contentEntryRelatedEntryJoinDao, translatedEntry, contentEntry,
                                REL_TYPE_TRANSLATED_VERSION)

                    }
                }

                val fileEntry = it.har.log.entries.find { harEntry ->
                    harEntry?.request?.url == fileUrl.toString()
                }

                if (fileEntry == null) {
                    throw ScraperException(ERROR_TYPE_FILE_NOT_LOADED, "No File found for content in har entry")
                }

                val isMediaFile = isMediaUrl(fileUrl.toString())

                if (isMediaFile && fileEntry.response.content.text.isNullOrEmpty()) {

                    val base64 = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fileUrl))
                    fileEntry.response.content.encoding = "base64"
                    fileEntry.response.content.size = base64.length.toLong()
                    fileEntry.response.content.text = base64
                }

                val entryETag = fileEntry.response.headers.find { valuePair -> valuePair.name == ETAG }
                eTagValue = entryETag?.value

                val container = containerDao.getMostRecentContainerForContentEntry(contentEntry.contentEntryUid)
                        ?: return@startHarScrape true

                return@startHarScrape isContentUpdated(fileEntry, container)

            }
        } catch (e: Exception) {
            contentEntryDao.updateContentEntryInActive(contentEntryUid, true)
            UMLogUtil.logError("$DDL Exception - Error downloading resource from url $sourceUrl")
            close()
            if (e is TimeoutException || e is NoSuchElementException) {
                throw ScraperException(ERROR_TYPE_NO_FILE_AVAILABLE, "no file found in the website")
            }
            throw e
        }

        if (!scraperResult.updated) {
            contentEntryDao.updateContentEntryInActive(contentEntryUid, false)
            close()
            return
        }

        val containerManager = scraperResult.containerManager

        if (containerManager?.allEntries?.isEmpty() != false) {
            contentEntryDao.updateContentEntryInActive(contentEntryUid, true)
            UMLogUtil.logError("$DDL Debug - Did not find any content to download at url $sourceUrl")
            close()
            throw ScraperException(ERROR_TYPE_FILE_NOT_LOADED, "Container Manager did not have the file")
        }

        runBlocking {

            val entry = containerManager.allEntries[0]
            val mimeType = Files.probeContentType(File(entry.cePath ?: "").toPath())
            val container = containerDao.findByUid(containerManager.containerUid)
            contentEntryDao.updateContentEntryInActive(contentEntryUid, false)
            containerDao.updateMimeType(mimeType, container?.containerUid ?: 0)
            if (!eTagValue.isNullOrEmpty()) {
                val etagContainer = ContainerETag(container?.containerUid ?: 0, eTagValue!!)
                containerEtagDao.insert(etagContainer)
            }

        }

        close()
    }

    private fun isMediaUrl(file: String): Boolean {
        return file.endsWith(".mp3")
    }


    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            if (args.size < 2) {
                System.err.println("Usage: <ddl website url><container destination><lang en or fa or ps><optional log{trace, debug, info, warn, error, fatal}>")
                System.exit(1)
            }
            UMLogUtil.setLevel(if (args.size == 4) args[3] else "")
            UMLogUtil.logInfo(args[0])
            UMLogUtil.logInfo(args[1])
            try {
                DdlContentScraper(File(args[1]), UmAppDatabase.Companion.getInstance(Any()), 0).scrapeUrl(args[0])
            } catch (e: IOException) {
                UMLogUtil.logError(ExceptionUtils.getStackTrace(e))
                UMLogUtil.logError("$DDL Exception running scrapeContent ddl")
            }

        }

    }

}