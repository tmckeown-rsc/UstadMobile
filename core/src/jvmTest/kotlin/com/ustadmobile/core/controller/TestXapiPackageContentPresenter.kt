//package com.ustadmobile.core.controller
//
//import com.ustadmobile.core.db.UmAppDatabase
//import com.ustadmobile.core.impl.UmCallback
//import com.ustadmobile.core.impl.UmCallbackUtil
//import com.ustadmobile.core.tincan.TinCanXML
//import com.ustadmobile.core.util.UMFileUtil
//import com.ustadmobile.core.view.XapiPackageContentView
//import com.ustadmobile.lib.db.entities.Container
//import com.ustadmobile.port.sharedse.container.ContainerManager
//import com.ustadmobile.port.sharedse.impl.http.EmbeddedHTTPD
//import com.ustadmobile.port.sharedse.util.UmFileUtilSe
//import com.ustadmobile.test.core.impl.PlatformTestUtil
//
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//
//import java.io.File
//import java.io.IOException
//import java.util.Hashtable
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.TimeUnit
//import java.util.zip.ZipFile
//
//import org.mockito.ArgumentMatchers.any
//import org.mockito.ArgumentMatchers.eq
//import org.mockito.Mockito.doAnswer
//import org.mockito.Mockito.mock
//import org.mockito.Mockito.timeout
//import org.mockito.Mockito.verify
//
//class TestXapiPackageContentPresenter {
//
//    private var db: UmAppDatabase? = null
//
//    private var repo: UmAppDatabase? = null
//
//    private var xapiTmpFile: File? = null
//
//    private var containerDirTmp: File? = null
//
//    private var xapiContainer: Container? = null
//
//    private var mockXapiPackageContentView: XapiPackageContentView? = null
//
//    private var httpd: EmbeddedHTTPD? = null
//
//    private val xapiXml: TinCanXML? = null
//
//    @Volatile
//    private var lastMountedUrl: String? = null
//
//    private val mountLatch = CountDownLatch(1)
//
//    @Before
//    @Throws(IOException::class)
//    fun setup() {
//        db = UmAppDatabase.getInstance(PlatformTestUtil.targetContext)
//        repo = db!!.getRepository("http://localhost/dummy/", "")
//        db!!.clearAllTables()
//
//        xapiContainer = Container()
//        xapiContainer!!.containerUid = repo!!.containerDao.insert(xapiContainer)
//
//        xapiTmpFile = File.createTempFile("testxapipackagecontentpresenter",
//                "xapiTmpFile")
//        UmFileUtilSe.extractResourceToFile("/com/ustadmobile/core/contentformats/XapiPackage-JsTetris_TCAPI.zip",
//                xapiTmpFile)
//
//        containerDirTmp = UmFileUtilSe.makeTempDir("testxapipackagecontentpresenter",
//                "containerDirTmp")
//        val containerManager = ContainerManager(xapiContainer, db, repo,
//                containerDirTmp!!.absolutePath)
//        val xapiZipFile = ZipFile(xapiTmpFile!!)
//        containerManager.addEntriesFromZip(xapiZipFile, ContainerManager.OPTION_COPY)
//        xapiZipFile.close()
//
//        httpd = EmbeddedHTTPD(0, PlatformTestUtil.targetContext, db, repo)
//        httpd!!.start()
//
//        mockXapiPackageContentView = mock(XapiPackageContentView::class.java)
//        doAnswer {
//            Thread {
//                lastMountedUrl = UMFileUtil.joinPaths(httpd!!.localHttpUrl,
//                        httpd!!.mountContainer(it.getArgument(0), null))
//                UmCallbackUtil.onSuccessIfNotNull<String>(it.getArgument<UmCallback<String>>(1),
//                        lastMountedUrl!!)
//                mountLatch.countDown()
//            }.start()
//            null
//        }.`when`<XapiPackageContentView>(mockXapiPackageContentView)
//                .mountContainer(eq(xapiContainer!!.containerUid), any<UmCallback<String>>())
//
//
//        doAnswer { invocation ->
//            Thread(invocation.getArgument<Any>(0) as Runnable).start()
//            null
//        }.`when`<XapiPackageContentView>(mockXapiPackageContentView).runOnUiThread(any())
//
//    }
//
//    @After
//    fun tearDown() {
//        xapiTmpFile!!.delete()
//        UmFileUtilSe.deleteRecursively(containerDirTmp!!)
//    }
//
//    @Test
//    @Throws(InterruptedException::class)
//    fun givenValidXapiPackage_whenCreated_shouldLoadAndSetTitle() {
//        val args = Hashtable<String,String>()
//        args.put(XapiPackageContentView.ARG_CONTAINER_UID, xapiContainer!!.containerUid.toString())
//
//        val xapiPresenter = XapiPackageContentPresenter(
//                PlatformTestUtil.targetContext, args, mockXapiPackageContentView!!)
//        xapiPresenter.onCreate(null)
//
//        mountLatch.await(15000, TimeUnit.MILLISECONDS)
//
//        verify<XapiPackageContentView>(mockXapiPackageContentView).mountContainer(
//                eq(xapiContainer!!.containerUid), any<UmCallback<String>>())
//
//        verify<XapiPackageContentView>(mockXapiPackageContentView, timeout(5000)).loadUrl(
//                UMFileUtil.joinPaths(lastMountedUrl!!, "tetris.html"))
//
//        verify<XapiPackageContentView>(mockXapiPackageContentView, timeout(15000)).setTitle("Tin Can Tetris Example")
//    }
//
//}