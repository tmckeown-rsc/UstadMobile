package com.ustadmobile.lib.contentscrapers

import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.lib.contentscrapers.ScraperConstants.ETAG_TXT
import com.ustadmobile.lib.contentscrapers.ScraperConstants.LAST_MODIFIED_TXT
import com.ustadmobile.lib.contentscrapers.ScraperConstants.UTF_ENCODING
import com.ustadmobile.lib.contentscrapers.africanbooks.AsbScraper
import com.ustadmobile.lib.contentscrapers.ddl.DdlContentScraper
import com.ustadmobile.lib.contentscrapers.prathambooks.IndexPrathamContentScraper
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.Okio
import org.apache.commons.io.IOUtils
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import java.io.File
import java.io.IOException
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files

class TestPrathamContentScraper {


    internal val dispatcher: Dispatcher = object : Dispatcher() {
        @Throws(InterruptedException::class)
        override fun dispatch(request: RecordedRequest): MockResponse {

            try {

                if (request.path.contains("json")) {

                    val fileName = request.path.substring(5)
                    val body = IOUtils.toString(javaClass.getResourceAsStream(fileName), UTF_ENCODING)
                    val response = MockResponse().setResponseCode(200)
                    response.setHeader("ETag", UTF_ENCODING.hashCode())
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.setBody(body)

                    return response

                } else if (request.path.contains("content")) {

                    val fileLocation = request.path.substring(8)
                    val videoIn = javaClass.getResourceAsStream(fileLocation)
                    val source = Okio.buffer(Okio.source(videoIn))
                    val buffer = Buffer()
                    source.readAll(buffer)

                    val response = MockResponse().setResponseCode(200)
                    response.setHeader("ETag", (buffer.size().toString() + UTF_ENCODING).hashCode())
                    if (!request.method.equals("HEAD", ignoreCase = true))
                        response.body = buffer

                    return response
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }

            return MockResponse().setResponseCode(404)
        }
    }

    @Before
    fun clearDb() {
        val db = UmAppDatabase.getInstance(Any())
        db.clearAllTables()
    }


    @Test
    @Throws(IOException::class, URISyntaxException::class)
    fun givenServerOnline_whenDdlSiteScraped_thenShouldFindConvertAndDownloadAllFiles() {

        val tmpDir = Files.createTempDirectory("testindexPrathamcontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val scraper = spy(IndexPrathamContentScraper())
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/pratham/prathamonebook.txt").url()).`when`(scraper).generatePrathamUrl("1")
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/pratham/prathamlist.txt").url()).`when`(scraper).generatePrathamUrl("2")
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/pratham/prathamempty.txt").url()).`when`(scraper).generatePrathamUrl("3")
        doReturn(mockWebServer.url("/content/com/ustadmobile/lib/contentscrapers/pratham/24620-a-book-for-puchku.zip").url()).`when`(scraper).generatePrathamEPubFileUrl(Mockito.anyString())
        doReturn("").`when`(scraper).loginPratham()

        scraper.findContent(tmpDir, containerDir)

        val resourceFolder = File(tmpDir, "24620")
        Assert.assertEquals(true, resourceFolder.isDirectory)

        val contentFile = File(resourceFolder, "24620$ETAG_TXT")
        Assert.assertEquals(true, ContentScraperUtil.fileHasContent(contentFile))


    }

    @Test
    @Throws(IOException::class)
    fun givenServerOnline_whenAsbSiteScraped_thenShouldFindConvertAndDownloadAllFiles() {

        val tmpDir = Files.createTempDirectory("testindexAsbcontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val scraper = spy(AsbScraper())
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/africanbooks/abslist.txt").url()).`when`(scraper).generateURL()
        doReturn(mockWebServer.url("/content/com/ustadmobile/lib/contentscrapers/africanbooks/asb18187.epub").url()).`when`(scraper).generateEPubUrl(Mockito.any<URL>(), Mockito.anyString())
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/africanbooks/abslist.txt").url()).`when`(scraper).generatePublishUrl(Mockito.any<URL>(), Mockito.anyString())
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/africanbooks/abslist.txt").url()).`when`(scraper).generateMakeUrl(Mockito.any<URL>(), Mockito.anyString())
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/africanbooks/asbreader.txt").url().toString()).`when`(scraper).generateReaderUrl(Mockito.any<URL>(), Mockito.anyString())
        doReturn(mockWebServer.url("/json/com/ustadmobile/lib/contentscrapers/africanbooks/asburl.txt").url().toString()).`when`(scraper).africanStoryBookUrl

        scraper.findContent(tmpDir, containerDir)

        val contentFile = File(tmpDir, "10674$LAST_MODIFIED_TXT")
        Assert.assertEquals(true, ContentScraperUtil.fileHasContent(contentFile))

    }


    @Test
    @Throws(IOException::class)
    fun givenServerOnline_whenDdlEpubScraped_thenShouldConvertAndDownload() {

        val tmpDir = Files.createTempDirectory("testindexDdlontentscraper").toFile()
        val containerDir = Files.createTempDirectory("container").toFile()

        val mockWebServer = MockWebServer()
        mockWebServer.setDispatcher(dispatcher)

        val scraper = DdlContentScraper(
                mockWebServer.url("json/com/ustadmobile/lib/contentscrapers/ddl/ddlcontent.txt").toString(),
                tmpDir, containerDir, "en")
        scraper.scrapeContent()
        scraper.parentSubjectAreas

        val contentFolder = File(tmpDir, "ddlcontent")
        Assert.assertEquals(true, contentFolder.isDirectory)

        val contentFile = File(contentFolder, "311$ETAG_TXT")
        Assert.assertEquals(true, ContentScraperUtil.fileHasContent(contentFile))

        Assert.assertTrue("container has the file", containerDir.listFiles()!!.size > 0)
    }

}