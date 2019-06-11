package com.ustadmobile.sharedse.network

import com.ustadmobile.core.db.*
import com.ustadmobile.core.impl.UMLog
import com.ustadmobile.door.*
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.db.entities.ConnectivityStatus.Companion.STATE_DISCONNECTED
import com.ustadmobile.lib.db.entities.ConnectivityStatus.Companion.STATE_METERED
import com.ustadmobile.lib.db.entities.DownloadJobItemHistory.Companion.MODE_CLOUD
import com.ustadmobile.lib.db.entities.DownloadJobItemHistory.Companion.MODE_LOCAL
import com.ustadmobile.lib.util.getSystemTimeInMillis
//import com.ustadmobile.port.sharedse.container.ContainerManager
//import com.ustadmobile.port.sharedse.impl.http.IContainerEntryListService
//import com.ustadmobile.port.sharedse.networkmanager.*
//import com.ustadmobile.port.sharedse.networkmanager.NetworkManagerBle.Companion.WIFI_GROUP_CREATION_RESPONSE
//import com.ustadmobile.port.sharedse.networkmanager.NetworkManagerBle.Companion.WIFI_GROUP_REQUEST
import com.ustadmobile.sharedse.container.ContainerManager
import com.ustadmobile.sharedse.io.FileSe
import io.ktor.client.HttpClient
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.produce
import kotlinx.io.IOException
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.converter.scalars.ScalarsConverterFactory
//import java.io.File
//import java.io.IOException
//import java.util.*
//import java.util.concurrent.CountDownLatch
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicBoolean
//import java.util.concurrent.atomic.AtomicInteger
//import java.util.concurrent.atomic.AtomicLong
//import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.coroutineContext

/**
 * Class which handles all file downloading tasks, it reacts to different status as changed
 * in the Db from either UI or Network change.
 *
 * i.e Decides where to get the file based on the entry status response,
 * connecting to the peer device via BLE and WiFiP2P for the actual download
 * and Change its status based on Network status.
 *
 * @author kileha3
 */
class DownloadJobItemRunner
/**
 * Constructor to be used when creating new instance of the runner.
 * @param downloadItem Item to be downloaded
 * @param networkManager BLE network manager for network operation controls.
 * @param appDb Application database instance
 * @param endpointUrl Endpoint to get the file from.
 */
(private val context: Any, private val downloadItem: DownloadJobItem,
 private val networkManager: NetworkManagerBleCommon, private val appDb: UmAppDatabase,
 private val appDbRepo: UmAppDatabase,
 private val endpointUrl: String, private var connectivityStatus: ConnectivityStatus?) {

    private val downloadJobItemManager: DownloadJobItemManager?

    private var statusLiveData: DoorLiveData<ConnectivityStatus?>? = null

    private var statusObserver: DoorObserver<ConnectivityStatus?>? = null

    //TODO: enable switching to local download when available after basic p2p cases complete
    //private UmObserver<EntryStatusResponse> entryStatusObserver;

    //private UmLiveData<EntryStatusResponse> entryStatusLiveData;

    private var downloadJobItemObserver: DoorObserver<Int>? = null

    private var downloadJobItemLiveData: DoorLiveData<Int>? = null

    private var downloadSetConnectivityData: DoorLiveData<Boolean>? = null

    private var downloadSetConnectivityObserver: DoorObserver<Boolean>? = null

    private var httpDownload: ResumableDownload2? = null

    //private val httpDownloadRef: AtomicReference<ResumableHttpDownload>

    private val completedEntriesBytesDownloaded = atomic(0L)

    //private val statusCheckTimer = Timer()

    private val runnerStatus = atomic(JobStatus.NOT_QUEUED)

    private val meteredConnectionAllowed = atomic(-1)

    private var lWiFiConnectionTimeout = 30

    private val wiFiDirectGroupBle = atomic<WiFiDirectGroupBle?>(null)

    private var currentNetworkNode: NetworkNode? = null

    /**
     * Boolean to indicate if we are waiting for a local connection.
     */
    private val waitingForLocalConnection = atomic(false)

    private val downloadWiFiLock = Any()

    //private val containerEntryListService: IContainerEntryListService

    private var destinationDir: String? = null

    private val numFailures = atomic(0)

    /**
     * Timer task to keep track of the download status
     */
//    private inner class StatusCheckTask : TimerTask() {
//
//        override fun run() {
//            val httpDownload = httpDownloadRef.get()
//            if (runnerStatus.get() == JobStatus.RUNNING) {
//                val bytesSoFar = completedEntriesBytesDownloaded.get() + (httpDownload?.downloadedSoFar
//                        ?: 0)
//                TODO("Fix this for coroutine")
////                downloadJobItemManager!!.updateProgress(downloadItem.djiUid.toInt(),
////                        bytesSoFar, downloadItem.downloadLength)
//            }
//        }
//    }


    init {
        this.downloadJobItemManager = networkManager
                .getDownloadJobItemManager(downloadItem.djiDjUid.toInt())
        //this.httpDownloadRef = AtomicReference()

        //Note: the url is passed as a parameter at runtime
//        val retrofit = Retrofit.Builder()
//                .addConverterFactory(ScalarsConverterFactory.create())
//                .addConverterFactory(GsonConverterFactory.create())
//                .baseUrl("http://localhost/dummy/").build()
//        containerEntryListService = retrofit.create(IContainerEntryListService::class.java)
    }


    fun setWiFiConnectionTimeout(lWiFiConnectionTimeout: Int) {
        this.lWiFiConnectionTimeout = lWiFiConnectionTimeout
    }


    /**
     * Handle changes triggered when connectivity status changes.
     * @param newStatus changed connectivity status
     */
    private fun handleConnectivityStatusChanged(newStatus: ConnectivityStatus?) {
        this.connectivityStatus = newStatus
        UMLog.l(UMLog.DEBUG, 699, mkLogPrefix() +
                " Connectivity state changed: " + newStatus)
        if (waitingForLocalConnection.value)
            return

        if (connectivityStatus != null) {
            when (newStatus!!.connectivityState) {
                STATE_METERED -> if (meteredConnectionAllowed.value == 0) {
                    GlobalScope.launch { stop(JobStatus.WAITING_FOR_CONNECTION) }
                }

                STATE_DISCONNECTED -> GlobalScope.launch { stop(JobStatus.WAITING_FOR_CONNECTION) }
            }//TODO: check CONNECTING_LOCAL - if the status changed, but we are not the job that asked for that
        }
    }

    /**
     * Handle changes triggered when Download set metered connection flag changes
     * @param meteredConnection changed metered connection flag.
     */
    private fun handleDownloadSetMeteredConnectionAllowedChanged(meteredConnection: Boolean?) {
        if (meteredConnection != null) {
            if(meteredConnection) {
                meteredConnectionAllowed.value = 1
            }else {
                meteredConnectionAllowed.value = 0
            }

            if (meteredConnectionAllowed.value == 0 && connectivityStatus != null
                    && connectivityStatus!!.connectivityState == STATE_METERED) {
                UMLog.l(UMLog.DEBUG, 699, mkLogPrefix() + " : no longer allowed to run on metered network - stopping")
                GlobalScope.launch { stop(JobStatus.WAITING_FOR_CONNECTION) }
            }
        }
    }

    /**
     * Handle changes triggered when the download job item status changes
     * @param newDownloadStatus changed download job item status
     */

    private fun handleDownloadJobItemStatusChanged(newDownloadStatus: Int) {
        if (newDownloadStatus == JobStatus.STOPPING) {
            GlobalScope.launch { stop(JobStatus.WAITING_FOR_CONNECTION) }
        }
    }

    //TODO: re-enable when we add support for switching dynamically
    //    /**
    //     * Handle changes triggered when file which wasn't available locally changes
    //     * @param entryStatusResponse new file entry status
    //     */
    //    private void handleContentEntryFileStatus(EntryStatusResponse entryStatusResponse){
    //        if(entryStatusResponse != null){
    //            availableLocally.set(entryStatusResponse.isAvailable() ? 1:0);
    //            if(availableLocally.get() == 1 && currentEntryStatusResponse!= null
    //                    && !currentEntryStatusResponse.isAvailable()){
    //                this.currentNetworkNode =
    //                        appDb.getNetworkNodeDao().findNodeById(entryStatusResponse.getErNodeId());
    //                connectToLocalNodeNetwork();
    //            }
    //        }
    //    }


//    /**
//     * Stop download task Async
//     * @param newStatus net status
//     */
//    suspend fun stopAsync(newStatus: Int) {
//        runnerStatus.value = JobStatus.STOPPING
//        Thread { stop(newStatus) }.start()
//    }

    /**
     * Stop the download task from continuing (if not already stopped). Calling stop for a second
     * time will have no effect.
     *
     * @param newStatus new status to be set
     */
    suspend fun stop(newStatus: Int) {
        if(!runnerStatus.compareAndSet(JobStatus.STOPPED, JobStatus.STOPPED)) {
            if (httpDownload != null) {
                //httpDownload!!.stop()
            }

            statusLiveData!!.removeObserver(statusObserver!!)
            downloadJobItemLiveData!!.removeObserver(downloadJobItemObserver!!)
            downloadSetConnectivityData!!.removeObserver(downloadSetConnectivityObserver!!)
            //entryStatusLiveData.removeObserver(entryStatusObserver);

            //statusCheckTimer.cancel()

            updateItemStatus(newStatus)
            networkManager.releaseWifiLock(this)
        }

    }


    suspend fun download() {
        runnerStatus.value = JobStatus.RUNNING
        updateItemStatus(JobStatus.RUNNING)
        val downloadJobId = downloadItem.djiDjUid
        appDb.downloadJobDao.update(downloadJobId, JobStatus.RUNNING)

        networkManager.startMonitoringAvailability(this,
                listOf(downloadItem.djiContainerUid))

        statusLiveData = appDb.connectivityStatusDao.statusLive()
        downloadJobItemLiveData = appDb.downloadJobItemDao.getLiveStatus(downloadItem.djiUid)

        //get the download set
        downloadSetConnectivityData = appDb.downloadJobDao.getLiveMeteredNetworkAllowed(downloadJobId)

        //TODO: re-enable after basic p2p cases run
        //        entryStatusLiveData = appDb.getEntryStatusResponseDao()
        //                .getLiveEntryStatus(downloadItem.getDjiContentEntryFileUid());

        //downloadSetConnectivityObserver = DoorObserver { t -> handleDownloadSetMeteredConnectionAllowedChanged(t) }
        downloadSetConnectivityObserver = object : DoorObserver<Boolean> {
            override fun onChanged(t: Boolean) {
                handleDownloadSetMeteredConnectionAllowedChanged(t)
            }
        }

        statusObserver = ObserverFnWrapper(this::handleConnectivityStatusChanged)
        downloadJobItemObserver = ObserverFnWrapper(this::handleDownloadJobItemStatusChanged)

        //entryStatusObserver = this::handleContentEntryFileStatus;
        statusLiveData!!.observeForever(statusObserver!!)


        downloadJobItemLiveData!!.observeForever(downloadJobItemObserver!!)
        downloadSetConnectivityData!!.observeForever(downloadSetConnectivityObserver!!)
        //entryStatusLiveData.observeForever(entryStatusObserver);

        destinationDir = appDb.downloadJobDao.getDestinationDir(downloadJobId)
        if (destinationDir == null) {
            val e = IllegalArgumentException(
                    "DownloadJobItemRunner destinationdir is null for " + downloadItem.djiDjUid)
            UMLog.l(UMLog.CRITICAL, 699,
                    mkLogPrefix() + " destinationDir = null", e)
            throw e
        }

        startDownload()
    }


    /**
     * Start downloading a file
     */
    private suspend fun startDownload() {
        UMLog.l(UMLog.INFO, 699, mkLogPrefix() +
                " StartDownload: ContainerUid = " + downloadItem.djiContainerUid)
        var attemptsRemaining = 3

        var downloaded = false
//        val statusCheckTask = StatusCheckTask()
//        statusCheckTimer.scheduleAtFixedRate(statusCheckTask,
//                0, TimeUnit.SECONDS.toMillis(1))

        val container = appDbRepo.containerDao
                .findByUid(downloadItem.djiContainerUid)

        var containerManager = ContainerManager(container!!, appDb, appDbRepo, destinationDir!!)




        //do {
            val currentTimeStamp = getSystemTimeInMillis()
            val minLastSeen = currentTimeStamp - (60 * 1000)
            val maxFailureFromTimeStamp = currentTimeStamp - (5 * 60 * 1000)

            //TODO: if the content is available on the node we already connected to, take that one
            currentNetworkNode = appDb.networkNodeDao
                    .findLocalActiveNodeByContainerUid(downloadItem.djiContainerUid,
                            minLastSeen, BAD_PEER_FAILURE_THRESHOLD, maxFailureFromTimeStamp)

            val isFromCloud = currentNetworkNode == null
            val history = DownloadJobItemHistory()
            history.mode = if (isFromCloud) MODE_CLOUD else MODE_LOCAL
            history.startTime = getSystemTimeInMillis()
            history.downloadJobItemId = downloadItem.djiUid
            history.networkNode = if (isFromCloud) 0L else currentNetworkNode!!.nodeId
            history.id = appDb.downloadJobItemHistoryDao.insert(history).toInt()

            val downloadEndpoint: String?
            //var connectionOpener: URLConnectionOpener? = null
            if (isFromCloud) {
                if (connectivityStatus!!.wifiSsid != null && connectivityStatus!!.wifiSsid!!.toUpperCase().startsWith("DIRECT-")) {
                    //we are connected to a local peer, but need the normal wifi
                    //TODO: if the wifi is just not available and is required, don't mark as a failure of this job
                    // set status to waiting for connection and stop
                    if (!connectToCloudNetwork()) {
                        //connection has failed
                        attemptsRemaining--
                        recordHistoryFinished(history, false)
                        //continue
                    }
                }
                downloadEndpoint = endpointUrl
            } else {
                if (currentNetworkNode!!.groupSsid == null || currentNetworkNode!!.groupSsid != connectivityStatus!!.wifiSsid) {

                    if (!connectToLocalNodeNetwork()) {
                        //recording failure will push the node towards the bad threshold, after which
                        // the download will be attempted from the cloud
                        recordHistoryFinished(history, false)
                        //continue
                    }
                }

                downloadEndpoint = currentNetworkNode!!.endpointUrl
                //connectionOpener = networkManager.localConnectionOpener
            }

            val containerEntryListClient = HttpClient() {
                install(JsonFeature)
            }

//
//
//
//
//            val containerEntryListCall = containerEntryListService
//                    .findByContainerWithMd5(downloadEndpoint!! + CONTAINER_ENTRY_LIST_PATH,
//                            downloadItem.djiContainerUid)

            history.url = downloadEndpoint

            UMLog.l(UMLog.INFO, 699, mkLogPrefix() +
                    " starting download from " + downloadEndpoint + " FromCloud=" + isFromCloud +
                    " Attempts remaining= " + attemptsRemaining)

            try {
                appDb.downloadJobItemDao.incrementNumAttempts(downloadItem.djiUid)

                val containerEntryList = containerEntryListClient.get<List<ContainerEntryWithMd5>>(
                        "$downloadEndpoint/$CONTAINER_ENTRY_LIST_PATH?containerUid=${downloadItem.djiContainerUid}")

                //val response = containerEntryListCall.execute()
                //if (response.isSuccessful) {
                    //val containerEntryList = response.body()
                val entriesToDownload = containerManager.linkExistingItems(containerEntryList)
                                //?: arrayListOf())//returns items we don't have yet
//                    completedEntriesBytesDownloaded.set(appDb.containerEntryFileDao
//                            .sumContainerFileEntrySizes(container!!.containerUid))
                history.startTime = getSystemTimeInMillis()
                withContext(coroutineContext) {
                    val producer = produce<ContainerEntryWithMd5> {
                        entriesToDownload.forEach { send(it) }
                    }

                    repeat(4) {
                        launch {
                            for(entry in producer) {
                                val destFile = FileSe(FileSe(destinationDir!!),
                                        entry.ceCefUid.toString() + ".tmp")
                                val resumableDownload = ResumableDownload2(downloadEndpoint +
                                        CONTAINER_ENTRY_FILE_PATH + entry.ceCefUid,
                                        destFile.getAbsolutePath())
//                            httpDownload!!.connectionOpener = connectionOpener
//                            httpDownloadRef.set(httpDownload)
                                if (resumableDownload.download()) {
                                    completedEntriesBytesDownloaded.addAndGet(destFile.length())
                                    //TODO : Update download manager to add this here
//                                    containerManager.addEntry(destFile, entry.cePath!!,
//                                            ContainerManager.OPTION_COPY)
                                }else {
                                    numFailures.incrementAndGet()
                                }
                            }
                        }
                    }
                }

//                    var downloadedCount = 0
//                    UMLog.l(UMLog.INFO, 699, "Downloading " +
//                            entriesToDownload.size + " ContainerEntryFiles from " + downloadEndpoint)
//                    for (entry in entriesToDownload) {
//                        val destFile = File(File(destinationDir!!),
//                                entry.ceCefUid.toString() + ".tmp")
//                        httpDownload = ResumableHttpDownload(downloadEndpoint +
//                                CONTAINER_ENTRY_FILE_PATH + entry.ceCefUid,
//                                destFile.absolutePath)
//                        httpDownload!!.connectionOpener = connectionOpener
//                        httpDownloadRef.set(httpDownload)
//                        if (httpDownload!!.download()) {
//                            completedEntriesBytesDownloaded.addAndGet(destFile.length())
//                            containerManager.addEntry(destFile, entry.cePath!!,
//                                    ContainerManager.OPTION_COPY)
//                            downloadedCount++
//                        }
//
//                        if (!destFile.delete())
//                            destFile.deleteOnExit()
//                    }
//
//                    downloaded = downloadedCount == entriesToDownload.size
//                //}
            } catch (e: IOException) {
                UMLog.l(UMLog.ERROR, 699, mkLogPrefix() +
                        "Failed to download a file from " + endpointUrl, e)
            }

            if (!downloaded) {
                //wait before retry
                delay(3000)
            }
            attemptsRemaining--
            recordHistoryFinished(history, downloaded)
        //} while (runnerStatus.get() == JobStatus.RUNNING && !downloaded && attemptsRemaining > 0)

        //httpdownloadref usage is finished
        //httpDownloadRef.set(null)

        //if (downloaded) {
        val numFails = numFailures.value
        if(numFails == 0) {
            appDb.downloadJobDao.updateBytesDownloadedSoFarAsync(downloadItem.djiDjUid)

            //val totalDownloaded = completedEntriesBytesDownloaded.get() + if (httpDownload != null) httpDownload!!.downloadedSoFar else 0

            TODO("Fix this to use suspend fun")
//            downloadJobItemManager!!.updateProgress(downloadItem.djiUid.toInt(),
//                    totalDownloaded, totalDownloaded)
        }

        stop(if (downloaded) JobStatus.COMPLETE else JobStatus.FAILED)

    }

    private fun recordHistoryFinished(history: DownloadJobItemHistory, successful: Boolean) {
        history.endTime = getSystemTimeInMillis()
        history.successful = successful
        appDb.downloadJobItemHistoryDao.update(history)
    }

    /**
     * Try to connect to the 'normal' wifi
     *
     * @return true if file should be do downloaded from the cloud otherwise false.
     */
    private suspend fun connectToCloudNetwork(): Boolean {
        UMLog.l(UMLog.DEBUG, 699, "Reconnecting cloud network")
        networkManager.restoreWifi()
        waitForLiveData(statusLiveData!!, CONNECTION_TIMEOUT * 1000.toLong()) {
            val checkStatus = connectivityStatus
            if (checkStatus == null) {
                false
            }else if (checkStatus.connectivityState == ConnectivityStatus.STATE_UNMETERED) {
                networkManager.lockWifi(downloadWiFiLock)
                true
            }else {
                checkStatus.connectivityState == STATE_METERED && meteredConnectionAllowed.value == 1
            }
        }
//        runBlocking {
//            waitForLiveData(statusLiveData!!, CONNECTION_TIMEOUT * 1000.toLong()) { connectivityStatus ->
//                if (connectivityStatus == null) {
//                    false
//                }else if (connectivityStatus.connectivityState == ConnectivityStatus.STATE_UNMETERED) {
//                    networkManager.lockWifi(downloadWiFiLock)
//                    true
//                }else {
//                    connectivityStatus.connectivityState == STATE_METERED && meteredConnectionAllowed.get() == 1
//                }
//            }
//        }

//        WaitForLiveData.observeUntil(statusLiveData!!, (CONNECTION_TIMEOUT * 1000).toLong(), object : WaitForLiveData.WaitForChecker<ConnectivityStatus> {
//            override fun done(value: ConnectivityStatus): Boolean {
//                if (connectivityStatus == null)
//                    return false
//
//                if (connectivityStatus!!.connectivityState == ConnectivityStatus.STATE_UNMETERED) {
//                    networkManager.lockWifi(downloadWiFiLock)
//                    return true
//                }
//
//                return connectivityStatus!!.connectivityState == STATE_METERED && meteredConnectionAllowed.get() == 1
//            }
//
//        })
        return connectivityStatus!!.connectivityState == ConnectivityStatus.STATE_UNMETERED
                || (meteredConnectionAllowed.value == 1 && connectivityStatus!!.connectivityState == ConnectivityStatus.STATE_METERED)
    }

    /**
     * Start local peers connection handshake
     *
     * @return true if successful, false otherwise
     */
    private fun connectToLocalNodeNetwork(): Boolean {
//        waitingForLocalConnection.set(true)
//        val requestGroupCreation = BleMessage(WIFI_GROUP_REQUEST,
//                BleMessage.getNextMessageIdForReceiver(currentNetworkNode!!.bluetoothMacAddress!!),
//                BleMessageUtil.bleMessageLongToBytes(listOf(1L)))
//        UMLog.l(UMLog.DEBUG, 699, mkLogPrefix() + " connecting local network: requesting group credentials ")
//        val latch = CountDownLatch(1)
//        val connectionRequestActive = AtomicBoolean(true)
//        networkManager.lockWifi(downloadWiFiLock)
//
//        networkManager.sendMessage(context, requestGroupCreation, currentNetworkNode!!, object : BleMessageResponseListener {
//            override fun onResponseReceived(sourceDeviceAddress: String, response: BleMessage?, error: Exception?) {
//                UMLog.l(UMLog.INFO, 699, mkLogPrefix() +
//                        " BLE response received: from " + sourceDeviceAddress + ":" + response +
//                        " error: " + error)
//                if (latch.count > 0 && connectionRequestActive.get()
//                        && response != null
//                        && response.requestType == WIFI_GROUP_CREATION_RESPONSE) {
//                    connectionRequestActive.set(false)
//                    val lWifiDirectGroup = networkManager.getWifiGroupInfoFromBytes(response.payload!!)
//                    wiFiDirectGroupBle.set(lWifiDirectGroup)
//
//                    val acquiredEndPoint = ("http://" + lWifiDirectGroup.ipAddress + ":"
//                            + lWifiDirectGroup.port + "/")
//                    currentNetworkNode!!.endpointUrl = acquiredEndPoint
//                    appDb.networkNodeDao.updateNetworkNodeGroupSsid(currentNetworkNode!!.nodeId,
//                            lWifiDirectGroup.ssid, acquiredEndPoint)
//
//                    UMLog.l(UMLog.INFO, 699, mkLogPrefix() +
//                            "Connecting to P2P group network with SSID " + lWifiDirectGroup.ssid)
//                }
//                latch.countDown()
//
//            }
//
//        })
//        try {
//            latch.await(20, TimeUnit.SECONDS)
//        } catch (ignored: InterruptedException) {
//        }
//
//        connectionRequestActive.set(false)
//
//
//        //There was an exception trying to communicate with the peer to get the wifi direct group network
//        if (wiFiDirectGroupBle.get() == null) {
//            UMLog.l(UMLog.ERROR, 699, mkLogPrefix() +
//                    "Requested group network" +
//                    "from bluetooth address " + currentNetworkNode!!.bluetoothMacAddress +
//                    "but did not receive group network credentials")
//            return false
//        }
//
//        //disconnect first
//        if (connectivityStatus!!.connectivityState != ConnectivityStatus.STATE_DISCONNECTED && connectivityStatus!!.wifiSsid != null) {
//            runBlocking {
//                waitForLiveData(statusLiveData!!, 10 * 1000.toLong()) {
//                    it != null && it.connectivityState != ConnectivityStatus.STATE_UNMETERED
//                }
//            }
////            WaitForLiveData.observeUntil(statusLiveData!!, 10 * 1000, object : WaitForLiveData.WaitForChecker<ConnectivityStatus> {
////                override fun done(value: ConnectivityStatus): Boolean {
////                    return connectivityStatus != null && connectivityStatus!!.connectivityState != ConnectivityStatus.STATE_UNMETERED
////                }
////
////            })
//            UMLog.l(UMLog.INFO, 699, "Disconnected existing wifi network")
//        }
//
//        UMLog.l(UMLog.INFO, 699, "Connection initiated to " + wiFiDirectGroupBle.get().ssid)
//
//        networkManager.connectToWiFi(wiFiDirectGroupBle.get().ssid,
//                wiFiDirectGroupBle.get().passphrase)
//
//        val statusRef = AtomicReference<ConnectivityStatus?>()
//        runBlocking {
//            waitForLiveData(statusLiveData!!, (lWiFiConnectionTimeout * 1000).toLong()) {
//                statusRef.set(it)
//                it != null && isExpectedWifiDirectGroup(it)
//            }
//        }
//
//
////        WaitForLiveData.observeUntil(statusLiveData!!, (lWiFiConnectionTimeout * 1000).toLong(), object : WaitForLiveData.WaitForChecker<ConnectivityStatus> {
////            override fun done(value: ConnectivityStatus): Boolean {
////                statusRef.set(value)
////                if (value == null)
////                    return false
////
////                return isExpectedWifiDirectGroup(value)
////            }
////
////        })
//        waitingForLocalConnection.set(false)
//        val currentStatus = statusRef.get()
//        return currentStatus != null && isExpectedWifiDirectGroup(currentStatus)
        return false
    }


    /**
     * Update status of the currently downloading job item.
     * @param itemStatus new status to be set
     * @see JobStatus
     */
    private fun updateItemStatus(itemStatus: Int) {
//        val latch = CountDownLatch(1)
//        UMLog.l(UMLog.INFO, 699, mkLogPrefix() +
//                " Setting status to: " + JobStatus.statusToString(itemStatus))
//        TODO("Update this to using suspend fun")
////        downloadJobItemManager!!.updateStatus(downloadItem.djiUid.toInt(), itemStatus,
////                object : UmResultCallback<Void?> {
////                    override fun onDone(result: Void?) {
////                        latch.countDown()
////                    }
////                })
//        try {
//            latch.await(5, TimeUnit.SECONDS)
//        } catch (e: InterruptedException) {/* should not happen */
//        }

    }

    private fun isExpectedWifiDirectGroup(status: ConnectivityStatus): Boolean {
        val lWifiDirectGroupBle = wiFiDirectGroupBle.value
        return (status.connectivityState == ConnectivityStatus.STATE_CONNECTED_LOCAL
                && status.wifiSsid != null
                && lWifiDirectGroupBle != null
                && status.wifiSsid == lWifiDirectGroupBle.ssid)
    }


    private fun mkLogPrefix(): String {
        return "DownloadJobItem #" + downloadItem.djiUid + ":"
    }

    companion object {

        internal val CONTAINER_ENTRY_LIST_PATH = "ContainerEntryList/findByContainerWithMd5"

        internal val CONTAINER_ENTRY_FILE_PATH = "ContainerEntryFile/"

        val BAD_PEER_FAILURE_THRESHOLD = 2

        private val CONNECTION_TIMEOUT = 60
    }
}