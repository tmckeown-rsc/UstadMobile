package com.ustadmobile.sharedse.network

import com.ustadmobile.core.db.JobStatus
import com.ustadmobile.core.db.UmAppDatabase
import com.ustadmobile.core.impl.UMLog
import com.ustadmobile.core.impl.UmAccountManager
import com.ustadmobile.core.impl.UmResultCallback
import com.ustadmobile.core.networkmanager.DownloadJobItemStatusProvider
import com.ustadmobile.core.networkmanager.LocalAvailabilityListener
import com.ustadmobile.core.networkmanager.LocalAvailabilityMonitor
import com.ustadmobile.core.networkmanager.OnDownloadJobItemChangeListener
import com.ustadmobile.core.util.UMIOUtils
import com.ustadmobile.lib.db.entities.*
import com.ustadmobile.lib.util.getSystemTimeInMillis
//import com.ustadmobile.port.sharedse.impl.http.EmbeddedHTTPD
//import com.ustadmobile.port.sharedse.util.LiveDataWorkQueue
import kotlinx.atomicfu.AtomicInt
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.io.ByteArrayInputStream
import kotlinx.io.ByteArrayOutputStream

//import java.io.*
//import java.util.*
//import java.util.concurrent.Executors
//import java.util.concurrent.RejectedExecutionException
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.atomic.AtomicInteger
//import java.util.concurrent.atomic.AtomicReference

/**
 * This is an abstract class which is used to implement platform specific NetworkManager
 *
 * @author kileha3
 */

abstract class NetworkManagerBleCommon : LocalAvailabilityMonitor,/*, LiveDataWorkQueue.OnQueueEmptyListener*/
        /*DownloadJobItemManager.OnDownloadJobItemChangeListener,*/
        DownloadJobItemStatusProvider {

    private val knownNodesLock = Any()

    private var mContext: Any? = null

    private var isStopMonitoring = false

    //private val entryStatusTaskExecutorService = Executors.newFixedThreadPool(5)

    private val availabilityMonitoringRequests = mutableMapOf<Any, List<Long>>()

    protected var knownBadNodeTrackList = mutableMapOf<String, AtomicInt>()

//    /**
//     * @return Active URLConnectionOpener
//     */
//    var localConnectionOpener: URLConnectionOpener? = null
//        protected set

    /**
     * Holds all created entry status tasks
     */
    private val entryStatusTasks = mutableListOf<BleEntryStatusTask>()


    //private var downloadJobItemWorkQueue: LiveDataWorkQueue<DownloadJobItem>? = null

    private val entryStatusResponses = mutableMapOf<Long, MutableList<EntryStatusResponse>>()

    private val locallyAvailableContainerUids = mutableSetOf<Long>()

    //protected var connectivityStatusRef = atomic(null as ConnectivityStatus?)

    protected var wifiLockHolders = mutableListOf<Any>()

    private val knownPeerNodes = mutableMapOf<String, Long>()

    //private val scheduledExecutorService = Executors.newScheduledThreadPool(3)

    private lateinit var umAppDatabase: UmAppDatabase

    private val localAvailabilityListeners = mutableListOf<LocalAvailabilityListener>()

    //private Map<Integer, DownloadJobItemManager> downloadJobItemManagerMap = new HashMap<>();

    private var jobItemManagerList: DownloadJobItemManagerList? = null


//    private val mJobItemAdapter = object : LiveDataWorkQueue.WorkQueueItemAdapter<DownloadJobItem> {
//        override fun makeRunnable(item: DownloadJobItem): Runnable {
//            return DownloadJobItemRunner(mContext!!, item, this@NetworkManagerBleCommon,
//                    umAppDatabase!!, UmAccountManager.getRepositoryForActiveAccount(mContext!!),
//                    UmAccountManager.getActiveEndpoint(mContext!!)!!,
//                    connectivityStatusRef.get())
//        }
//
//        override fun getUid(item: DownloadJobItem): Long {
//            return item.djiUid.hashCode().toLong() shl 32 or item.numAttempts.toLong()
//        }
//    }

//    private val nodeLastSeenTrackerTask = Runnable {
//        if (knownPeerNodes.size > 0) {
//            val nodeMap = HashMap(knownPeerNodes)
//            GlobalScope.launch {
//                umAppDatabase!!.networkNodeDao.updateNodeLastSeen(nodeMap)
//                UMLog.l(UMLog.DEBUG, 694, "Updating "
//                        + knownPeerNodes.size + " nodes from the Db")
//            }
//
//
//        }
//    }

    /**
     * Check if WiFi is enabled / disabled on the device
     * @return boolean true, if enabled otherwise false.
     */
    abstract val isWiFiEnabled: Boolean


    /**
     * Check if the device is Bluetooth Low Energy capable
     * @return True is capable otherwise false
     */
    abstract val isBleCapable: Boolean

    /**
     * Check if bluetooth is enabled on the device
     * @return True if enabled otherwise false
     */
    abstract val isBluetoothEnabled: Boolean

    /**
     * Get all unique entry UUID's to be monitored
     * @return Set of all unique UUID's
     */
    private val allUidsToBeMonitored: Set<Long>
        get() = availabilityMonitoringRequests.flatMap { it.value }.toSet()

//        {
//            val uidsToBeMonitoredSet = TreeSet<Long>()
//            for (uidList in availabilityMonitoringRequests.values) {
//                uidsToBeMonitoredSet.addAll(uidList)
//            }
//            return uidsToBeMonitoredSet
//        }

    /**
     * @return Active RouterNanoHTTPD
     */
    //abstract val httpd: EmbeddedHTTPD

    abstract val isVersionLollipopOrAbove: Boolean

    abstract val isVersionKitKatOrBelow: Boolean

//    val activeDownloadJobItemManagers: List<DownloadJobItemManager>
//        get() = jobItemManagerList!!.getActiveDownloadJobItemManagers()

    /**
     * Constructor to be used when creating new instance
     * @param context Platform specific application context
     */
    constructor(context: Any) {
        this.mContext = context
    }

    /**
     * Constructor to be used for testing purpose (mocks)
     */
    constructor() {

    }

    /**
     * Set platform specific context
     * @param context Platform's context to be set
     */
    fun setContext(context: Any) {
        this.mContext = context
    }

    /**
     * Only for testing - allows the unit test to set this without running the main onCreate method
     *
     * @param jobItemManagerList DownloadJobItemManagerList
     */
    fun setJobItemManagerList(jobItemManagerList: DownloadJobItemManagerList) {
        this.jobItemManagerList = jobItemManagerList
    }

    /**
     * Start web server, advertising and discovery
     */
    open fun onCreate() {
        umAppDatabase = UmAppDatabase.getInstance(mContext!!)
//        jobItemManagerList = DownloadJobItemManagerList(umAppDatabase!!)
//        downloadJobItemWorkQueue = LiveDataWorkQueue(MAX_THREAD_COUNT)
//        downloadJobItemWorkQueue!!.adapter = mJobItemAdapter
//        downloadJobItemWorkQueue!!.start(umAppDatabase!!.downloadJobItemDao.findNextDownloadJobItems())
//        scheduledExecutorService.scheduleAtFixedRate(nodeLastSeenTrackerTask, 0, 10, TimeUnit.SECONDS)
    }

    /*override */fun onQueueEmpty() {
//        if (connectivityStatusRef.get() != null && connectivityStatusRef.get().connectivityState == ConnectivityStatus.STATE_CONNECTED_LOCAL) {
//            Thread(Runnable { this.restoreWifi() }).start()
//        }
    }

    /**
     * Check if the device can create BLE service and advertise it to the peer devices
     * @return true if can advertise its service else false
     */
    abstract fun canDeviceAdvertise(): Boolean


    /**
     * This should be called by the platform implementation when BLE discovers a nearby device
     * @param node The nearby device discovered
     */
    fun handleNodeDiscovered(node: NetworkNode) {
        synchronized(knownNodesLock) {

            val networkNodeDao = UmAppDatabase.getInstance(mContext!!).networkNodeDao

            if (!knownPeerNodes.containsKey(node.bluetoothMacAddress)) {

                node.lastUpdateTimeStamp = getSystemTimeInMillis()

//                GlobalScope.launch {
//                    val result = networkNodeDao.updateLastSeenAsync(node.bluetoothMacAddress!!,
//                            node.lastUpdateTimeStamp)
//                    knownPeerNodes[node.bluetoothMacAddress!!] = node.lastUpdateTimeStamp
//                    if (result == 0) {
//                        networkNodeDao.insertAsync(node)
//                        UMLog.l(UMLog.DEBUG, 694, "New node with address "
//                                + node.bluetoothMacAddress + " found, added to the Db")
//
//                        val entryUidsToMonitor = ArrayList(allUidsToBeMonitored)
//
//                        if (!isStopMonitoring) {
//                            if (entryUidsToMonitor.size > 0) {
//                                val entryStatusTask = makeEntryStatusTask(mContext, entryUidsToMonitor, node)
//                                entryStatusTasks.add(entryStatusTask)
//                                entryStatusTaskExecutorService.execute(entryStatusTask)
//                            }
//                        }
//                    }
//
//
//                }
            } else {
                knownPeerNodes.put(node.bluetoothMacAddress!!, getSystemTimeInMillis())
            }
        }
    }

    //abstract fun awaitWifiDirectGroupReady(timeout: Long, timeoutUnit: TimeUnit): WiFiDirectGroupBle

    /**
     * Open bluetooth setting section from setting panel
     */
    abstract fun openBluetoothSettings()

    /**
     * Enable or disable WiFi on the device
     *
     * @param enabled Enable when true otherwise disable
     * @return true if the operation is successful, false otherwise
     */
    abstract fun setWifiEnabled(enabled: Boolean): Boolean


    /**
     * Start monitoring availability of specific entries from peer devices
     * @param monitor Object to monitor e.g Presenter
     * @param entryUidsToMonitor List of entries to be monitored
     */
    override fun startMonitoringAvailability(monitor: Any, entryUidsToMonitor: List<Long>) {
        try {
            //isStopMonitoring = false;
            availabilityMonitoringRequests[monitor] = entryUidsToMonitor
            UMLog.l(UMLog.DEBUG, 694, "Registered a monitor with "
                    + entryUidsToMonitor.size + " entry(s) to be monitored")

            val networkNodeDao = UmAppDatabase.getInstance(mContext!!).networkNodeDao
            val responseDao = UmAppDatabase.getInstance(mContext!!).entryStatusResponseDao

            val lastUpdateTime = getSystemTimeInMillis() - (60 * 1000)

            val uniqueEntryUidsToMonitor = ArrayList(allUidsToBeMonitored)
            val knownNetworkNodes = getAllKnownNetworkNodeIds(networkNodeDao.findAllActiveNodes(lastUpdateTime, 1))

            UMLog.l(UMLog.DEBUG, 694,
                    "Found total of   " + uniqueEntryUidsToMonitor.size +
                            " uids to check their availability status")

            val entryWithoutRecentResponses = responseDao.findEntriesWithoutRecentResponse(
                    uniqueEntryUidsToMonitor, knownNetworkNodes,
                    getSystemTimeInMillis() - (2 * 60 * 1000))

            //Group entryUUid by node where their status will be checked from
            val nodeToCheckEntryList = LinkedHashMap<Int, MutableList<Long>>()

            for (entryResponse in entryWithoutRecentResponses) {

                val nodeIdToCheckFrom = entryResponse.nodeId
                if (!nodeToCheckEntryList.containsKey(nodeIdToCheckFrom))
                    nodeToCheckEntryList[nodeIdToCheckFrom] = arrayListOf()

                nodeToCheckEntryList[nodeIdToCheckFrom]!!.add(entryResponse.containerUid)
            }

            UMLog.l(UMLog.DEBUG, 694,
                    "Created total of  " + nodeToCheckEntryList.entries.size
                            + " entry(s) to be checked from")

            //Make entryStatusTask as per node list and entryUuids found
            for (nodeId in nodeToCheckEntryList.keys) {
                val networkNode = networkNodeDao.findNodeById(nodeId.toLong())
                val entryStatusTask = makeEntryStatusTask(mContext,
                        nodeToCheckEntryList[nodeId]!!, networkNode)
                entryStatusTasks.add(entryStatusTask!!)
                //entryStatusTaskExecutorService.execute(entryStatusTask)
                UMLog.l(UMLog.DEBUG, 694,
                        "Status check started for " + nodeToCheckEntryList[nodeId]!!.size
                                + " entry(s) task from " + networkNode!!.bluetoothMacAddress)
            }
        } catch (e: Exception) {
            //e.printStackTrace()
        }

    }

    /**
     * Stop monitoring the availability of entries from peer devices
     * @param monitor Monitor object which created a monitor (e.g Presenter)
     */
    override fun stopMonitoringAvailability(monitor: Any) {
        availabilityMonitoringRequests.remove(monitor)
        isStopMonitoring = availabilityMonitoringRequests.size == 0
    }

    /**
     * Get all peer network nodes that we know about
     * @param networkNodes Known NetworkNode
     * @return List of all known nodes
     */
    private fun getAllKnownNetworkNodeIds(networkNodes: List<NetworkNode>): List<Long> {
        val nodeIdList = ArrayList<Long>()
        for (networkNode in networkNodes) {
            nodeIdList.add(networkNode.nodeId)
        }
        return nodeIdList
    }

    /**
     * Connecting a client to a group network for content acquisition
     * @param ssid Group network SSID
     * @param passphrase Group network passphrase
     */
    abstract fun connectToWiFi(ssid: String, passphrase: String, timeout: Int)

    fun connectToWiFi(ssid: String, passphrase: String) {
        connectToWiFi(ssid, passphrase, DEFAULT_WIFI_CONNECTION_TIMEOUT)
    }

    /**
     * Restore the 'normal' WiFi connection
     */
    abstract fun restoreWifi()


    /**
     * Create entry status task for a specific peer device,
     * it will request status of the provided entries from the provided peer device
     * @param context Platform specific mContext
     * @param entryUidsToCheck List of entries to be checked from the peer device
     * @param peerToCheck Peer device to request from
     * @return Created BleEntryStatusTask
     *
     * @see BleEntryStatusTask
     */
    abstract fun makeEntryStatusTask(context: Any?, entryUidsToCheck: List<Long>, peerToCheck: NetworkNode?): BleEntryStatusTask?

    /**
     * Create entry status task for a specific peer device,
     * it will request status of the provided entries from the provided peer device
     * @param context Platform specific mContext
     * @param message Message to be sent to the peer device
     * @param peerToSendMessageTo Peer device to send message to.
     * @param responseListener Message response listener object
     * @return Created BleEntryStatusTask
     *
     * @see BleEntryStatusTask
     */
    abstract fun makeEntryStatusTask(context: Any, message: BleMessage,
                                     peerToSendMessageTo: NetworkNode,
                                     responseListener: BleMessageResponseListener): BleEntryStatusTask?

    abstract fun makeDeleteJobTask(`object`: Any?, args: Map<String, String>): DeleteJobTaskRunner

    /**
     * Send message to a specific device
     * @param context Platform specific context
     * @param message Message to be send
     * @param peerToSendMessageTo Peer device to receive the message
     * @param responseListener Message response listener object
     */
    fun sendMessage(context: Any, message: BleMessage, peerToSendMessageTo: NetworkNode,
                    responseListener: BleMessageResponseListener) {
        makeEntryStatusTask(context, message, peerToSendMessageTo, responseListener)?.run()
    }


    /**
     * Cancel all download set and set items
     * @param downloadJobUid The download job uid that should be canceled and deleted
     */
    fun cancelAndDeleteDownloadJob(downloadJobUid: Int) {
        umAppDatabase!!.downloadJobDao.updateJobAndItems(downloadJobUid,
                JobStatus.CANCELED, -1, JobStatus.CANCELED)
        val taskArgs = HashMap<String, String>()
        taskArgs[DeleteJobTaskRunner.ARG_DOWNLOAD_JOB_UID] = downloadJobUid.toString()

        makeDeleteJobTask(mContext, taskArgs).run()
    }


    /**
     * Add listener to the list of local availability listeners
     * @param listener listener object to be added.
     */
    fun addLocalAvailabilityListener(listener: LocalAvailabilityListener) {
        if (!localAvailabilityListeners.contains(listener)) {
            localAvailabilityListeners.add(listener)
        }
    }

    /**
     * Remove a listener from a list of all available listeners
     * @param listener listener to be removed
     */
    fun removeLocalAvailabilityListener(listener: LocalAvailabilityListener) {
        localAvailabilityListeners.remove(listener)
    }

    /**
     * Trigger availability status change event to all listening parts
     */
    fun fireLocalAvailabilityChanged() {
        val listenerList = ArrayList(localAvailabilityListeners)
        for (listener in listenerList) {
            listener.onLocalAvailabilityChanged(locallyAvailableContainerUids)
        }
    }

    /**
     * All all availability statuses received from the peer node
     * @param responses response received
     */
    fun handleLocalAvailabilityResponsesReceived(responses: MutableList<EntryStatusResponse>) {
        if (responses.isEmpty())
            return

        val nodeId = responses[0].erNodeId
        if (!entryStatusResponses.containsKey(nodeId))
            entryStatusResponses[nodeId] = mutableListOf()

        entryStatusResponses[nodeId]!!.addAll(responses)
        locallyAvailableContainerUids.clear()

        for (responseList in entryStatusResponses.values) {
            for (response in responseList) {
                if (response.available)
                    locallyAvailableContainerUids.add(response.erContainerUid)
            }
        }

        fireLocalAvailabilityChanged()
    }

    //testing purpose only
    fun clearHistories() {
        locallyAvailableContainerUids.clear()
        knownPeerNodes.clear()
    }

    /**
     * Used for unit testing purposes only.
     *
     * @hide
     * @param database
     */
    fun setDatabase(database: UmAppDatabase) {
        this.umAppDatabase = database
    }

    open fun lockWifi(lockHolder: Any) {
        wifiLockHolders.add(lockHolder)
    }

    open fun releaseWifiLock(lockHolder: Any) {
        wifiLockHolders.remove(lockHolder)
    }

    /**
     * Handle node connection history, delete node which failed to connect for over 5 attempts
     * @param bluetoothAddress node bluetooth address
     * @param success connection status , True if the connection was made successfully,
     * otherwise false
     */
    fun handleNodeConnectionHistory(bluetoothAddress: String, success: Boolean) {

//        var record: AtomicInteger? = knownBadNodeTrackList[bluetoothAddress]
//
//        if (record == null || success) {
//            record = AtomicInteger(0)
//            knownBadNodeTrackList[bluetoothAddress] = record
//            UMLog.l(UMLog.DEBUG, 694,
//                    "Connection succeeded bad node counter was set to " + record.get()
//                            + " for " + bluetoothAddress)
//        }
//
//        if (!success) {
//            record.set(record.incrementAndGet())
//            knownBadNodeTrackList[bluetoothAddress] = record
//            UMLog.l(UMLog.DEBUG, 694,
//                    "Connection failed and bad node counter set to " + record.get()
//                            + " for " + bluetoothAddress)
//        }
//
//        if (knownBadNodeTrackList[bluetoothAddress]!!.get() > 5) {
//            UMLog.l(UMLog.DEBUG, 694,
//                    "Bad node counter exceeded threshold (5), removing node with address "
//                            + bluetoothAddress + " from the list")
//            knownBadNodeTrackList.remove(bluetoothAddress)
//            knownPeerNodes.remove(bluetoothAddress)
//            umAppDatabase!!.networkNodeDao.deleteByBluetoothAddress(bluetoothAddress)
//
//            UMLog.l(UMLog.DEBUG, 694, "Node with address "
//                    + bluetoothAddress + " removed from the list")
//        }
    }

    /**
     * Get bad node by bluetooth address
     * @param bluetoothAddress node bluetooth address
     * @return bad node
     */
    fun getBadNodeTracker(bluetoothAddress: String): AtomicInt? {
        return knownBadNodeTrackList[bluetoothAddress]
    }

    fun isEntryLocallyAvailable(containerUid: Long): Boolean {
        return locallyAvailableContainerUids.contains(containerUid)
    }

    fun getLocallyAvailableContainerUids(): Set<Long> {
        return locallyAvailableContainerUids
    }


    /**
     * Clean up the network manager for shutdown
     */
    open fun onDestroy() {
        //downloadJobItemWorkQueue.shutdown();
        //entryStatusTaskExecutorService.shutdown()
    }

    /**
     * Convert IP address to decimals
     * @param address IPV4 address
     * @return decimal representation of an IP address
     */
    private fun convertIpAddressToInteger(address: String): Int {
        var result = 0
        val ipAddressInArray = address.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in 3 downTo 0) {
            //val ip = Integer.parseInt(ipAddressInArray[3 - i])
            val ip = ipAddressInArray[3 - i].toInt()
            result = result or (ip shl i * 8)
        }
        return result
    }

    /**
     * Convert decimal representation of an ip address back to IPV4 format.
     * @param ip decimal representation
     * @return IPV4 address
     */
    private fun convertIpAddressToString(ip: Int): String {
        return ((ip shr 24 and 0xFF).toString() + "." + (ip shr 16 and 0xFF) + "."
                + (ip shr 8 and 0xFF) + "." + (ip and 0xFF))
    }


    /**
     * Convert group information to bytes so that they can be transmitted using [BleMessage]
     * @param group WiFiDirectGroupBle
     * @return constructed bytes  array from the group info.
     */

    fun getWifiGroupInfoAsBytes(group: WiFiDirectGroupBle): ByteArray {
        val bos = ByteArrayOutputStream()
        var infoAsbytes = byteArrayOf()
//        val outputStream = DataOutputStream(bos)
//        try {
//            outputStream.writeUTF(group.ssid)
//            outputStream.writeUTF(group.passphrase)
//            outputStream.writeInt(convertIpAddressToInteger(group.ipAddress!!))
//            outputStream.writeChar((group.port + 'a'.toInt()).toChar().toInt())
//            infoAsbytes = bos.toByteArray()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        } finally {
//            UMIOUtils.closeOutputStream(outputStream)
//            UMIOUtils.closeOutputStream(bos)
//        }
        return infoAsbytes
    }


    /**
     * Construct WiFiDirectGroupBle from received message payload
     * @param payload received payload
     * @return constructed WiFiDirectGroupBle
     */
    fun getWifiGroupInfoFromBytes(payload: ByteArray): WiFiDirectGroupBle {
        val inputStream = ByteArrayInputStream(payload)
        //val dataInputStream = DataInputStream(inputStream)
        var groupBle: WiFiDirectGroupBle? = null
        try {
//            groupBle = WiFiDirectGroupBle(dataInputStream.readUTF(), dataInputStream.readUTF())
//            groupBle.ipAddress = convertIpAddressToString(dataInputStream.readInt())
//            groupBle.port = dataInputStream.readChar() - 'a'
        } catch (e: Exception) {
//            e.printStackTrace()
        } finally {
//            UMIOUtils.closeInputStream(dataInputStream)
            inputStream.close()
        }

        UMLog.l(UMLog.INFO, 699,
                "Group information received with ssid = " + groupBle!!.ssid)
        return groupBle
    }

    /**
     * Inserts a DownloadJob into the database for a given
     *
     * @param newDownloadJob the new DownloadJob to be created (with properties set)
     *
     * @return
     */
    fun createNewDownloadJobItemManager(newDownloadJob: DownloadJob): DownloadJobItemManager {
        return jobItemManagerList!!.createNewDownloadJobItemManager(newDownloadJob)
    }

    fun createNewDownloadJobItemManager(rootContentEntryUid: Long): DownloadJobItemManager {
        return createNewDownloadJobItemManager(DownloadJob(rootContentEntryUid,
                getSystemTimeInMillis()))
    }


    fun getDownloadJobItemManager(downloadJobId: Int): DownloadJobItemManager? {
        return jobItemManagerList!!.getDownloadJobItemManager(downloadJobId)
    }

    fun deleteUnusedDownloadJob(downloadJobUid: Int) {
        jobItemManagerList!!.deleteUnusedDownloadJob(downloadJobUid)
    }

    override suspend fun findDownloadJobItemStatusByContentEntryUid(contentEntryUid: Long)
            = jobItemManagerList!!.findDownloadJobItemStatusByContentEntryUid(contentEntryUid)

    override fun addDownloadChangeListener(listener: OnDownloadJobItemChangeListener)
         = jobItemManagerList!!.addDownloadChangeListener(listener)


    override fun removeDownloadChangeListener(listener: OnDownloadJobItemChangeListener)
        = jobItemManagerList!!.removeDownloadChangeListener(listener)


    companion object {

        /**
         * Flag to indicate entry status request
         */
        const val ENTRY_STATUS_REQUEST = 111.toByte()

        /**
         * Flag to indicate entry status response
         */
        const val ENTRY_STATUS_RESPONSE = 112.toByte()

        /**
         * Flag to indicate WiFi direct group request (for content download)
         */
        const val WIFI_GROUP_REQUEST = 113.toByte()

        /**
         * Flag to indicate WiFi direct group creation response
         */
        const val WIFI_GROUP_CREATION_RESPONSE = 114.toByte()


        /**
         * Commonly used MTU for android devices
         */
        const val DEFAULT_MTU_SIZE = 20

        /**
         * Maximum MTU for the packet transfer
         */
        const val MAXIMUM_MTU_SIZE = 512

        /**
         * Bluetooth Low Energy service UUID for our app
         */
        val USTADMOBILE_BLE_SERVICE_UUID = "7d2ea28a-f7bd-485a-bd9d-92ad6ecfe93a"

        const val WIFI_DIRECT_GROUP_SSID_PREFIX = "DIRECT-"

        private const val MAX_THREAD_COUNT = 1

        const val DEFAULT_WIFI_CONNECTION_TIMEOUT = 30 * 1000
    }

}