package com.ustadmobile.sharedse.ext

import com.ustadmobile.core.container.ContainerManager
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.io.ConcatenatedInputStream
import com.ustadmobile.core.io.ConcatenatedPart
import com.ustadmobile.core.util.ext.encodeBase64
import com.ustadmobile.core.util.ext.toHexString
import com.ustadmobile.lib.db.entities.Container
import com.ustadmobile.lib.db.entities.ContainerEntryFile
import com.ustadmobile.util.test.extractTestResourceToFile
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.File
import com.ustadmobile.port.sharedse.ext.generateConcatenatedFilesResponse
import org.junit.Assert
import java.io.ByteArrayInputStream
import java.security.MessageDigest

class ContainerEntryFileDaoExtTest {

    private val testFileNames = listOf("testfile1.png", "testfile2.png")

    private lateinit var db: UmAppDatabase

    private lateinit var containerTmpDir: File

    private val context = Any()

    lateinit var testFiles: MutableList<File>

    @Before
    fun setup() {
        db = UmAppDatabase.getInstance(context)
        db.clearAllTables()

        testFiles = mutableListOf()
        testFileNames.forEachIndexed {index,name ->
            val tmpFile = File.createTempFile("testContainerMgr", "testFile${index}")
            extractTestResourceToFile("/com/ustadmobile/port/sharedse/container/$name",
                    tmpFile)
            testFiles.add(tmpFile)
        }

        containerTmpDir = File.createTempFile("ContainerEntryFileDaoExtTest",
                "${System.currentTimeMillis()}").also {
            it.delete()
            it.mkdirs()
        }
    }

    @Test
    fun givenExistingContainerEntryFiles_whenGenerateFileResponse_thenShouldGenerateStrean() {
        runBlocking {
            val md5s = mutableListOf<ByteArray>()
            val containerEntryFiles = testFiles.map {file ->
                val messageDigest = MessageDigest.getInstance("MD5")
                val fileBytes = file.readBytes()
                messageDigest.digest(fileBytes)
                val fileMd5 = messageDigest.digest()
                md5s.add(fileMd5)

                ContainerEntryFile(fileMd5.encodeBase64(), file.length(), file.length(), 0,
                        System.currentTimeMillis()).also {
                    it.cefPath = file.absolutePath
                    it.cefUid = db.containerEntryFileDao.insert(it)
                }
            }


            val response = db.containerEntryFileDao.generateConcatenatedFilesResponse(
                    containerEntryFiles.joinToString(separator = ";") { it.cefUid.toString() })

            val concatenatedData = response.dataSrc!!.readBytes()

            val concatenatedInputStream = ConcatenatedInputStream(ByteArrayInputStream(concatenatedData))

            var concatenatedPart: ConcatenatedPart? = null
            var fileCount = 0
            while(concatenatedInputStream.nextPart().also { concatenatedPart = it } != null) {
                val fileBytes = testFiles[fileCount].readBytes()
                val partBytes = concatenatedInputStream.readBytes()
                Assert.assertArrayEquals("File content bytes in response is the same",
                        fileBytes, partBytes)
                fileCount++
            }

            Assert.assertEquals("Correct number of parts", testFiles.size, fileCount)
        }



    }

}