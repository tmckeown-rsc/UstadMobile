package com.ustadmobile.sharedse.network


import android.bluetooth.*
import android.os.Build
import androidx.annotation.RequiresApi
import com.github.aakira.napier.Napier
import com.ustadmobile.sharedse.network.NetworkManagerBleCommon.Companion.MINIMUM_MTU_SIZE
import com.ustadmobile.sharedse.network.NetworkManagerBleCommon.Companion.MAXIMUM_MTU_SIZE
import com.ustadmobile.sharedse.network.NetworkManagerBleCommon.Companion.USTADMOBILE_BLE_SERVICE_UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

/**
 * This class handle all the GATT client Bluetooth Low Energy callback
 *
 * One instance of this callback is created per server that a client wants to communicate with.
 * When a connection is the client will discover services and then initiate an MTU change request.
 *
 * The message will put onto a channel. Channel processing begins after the mtu has been changed.
 *
 * Messages must be sent one at a time per characteristic used
 * to avoid corruption on the server or client. The client will make a characteristic write request
 * for each packet in the outgoing request. Once all request packets have been sent the client will
 * make a characteristic read request for each packet in the response.
 *
 * For more explanation about MTU and MTU throughput, below is an article you gan go through
 * @link https://interrupt.memfault.com/blog/ble-throughput-primer
 *
 * @author kileha3
 */

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
class BleMessageGattClientCallback() : BluetoothGattCallback() {

    private val receivedMessage: BleMessage

    private var messageToSend: BleMessage? = null

    private var responseListener: BleMessageResponseListener? = null

    private var packetIteration = 0

    private val serviceDiscoveryRef = AtomicBoolean(false)

    private val mConnected = AtomicBoolean(true)

    private val mClosed = AtomicBoolean(false)

    @Volatile
    private var lastActive: Long

    private var packetTransferAttemptsRemaining = 3

    private val MAX_DELAY_TIME  = 1000L

    private val currentMtu = AtomicInteger(MINIMUM_MTU_SIZE)

    private val mtuCompletableDeferred = CompletableDeferred<Int>()

    private val messageChannel = Channel<PendingMessage>(Channel.UNLIMITED)

    class PendingMessage(val messageId: Int, val outgoingMessage: BleMessage,
                         internal val incomingMessage: BleMessage = BleMessage(),
                         val messageReceived: CompletableDeferred<BleMessage> = CompletableDeferred(),
                         val responseListener: BleMessageResponseListener? = null) {

        lateinit var outgoingPackets: Array<ByteArray>

        var outgoingPacketNum: Int = 0

        var incomingPacketNum: Int = 0
    }

    //Map of characteristic UUID -> PendingMessageProcessor
    private val processorMap: MutableMap<UUID, PendingMessageProcessor> = ConcurrentHashMap()


    /**
     * This is a channel based request processor that will handle sending a BleMessage and receiving
     * the reply. The main class callback will call the onCharacteristicWrite and
     * readCharacteristic methods when they match our clientToServerCharacteristic UUID
     */
    class PendingMessageProcessor(val messageChannel: Channel<PendingMessage>,
                                  var mGatt: BluetoothGatt,
                                  val mtu: Int,
                                  val clientToServerCharacteristic: BluetoothGattCharacteristic){

        val currentPendingMessage = AtomicReference<PendingMessage?>()

        private val logPrefix
                get() = "BleMessageGattClientCallback: Request ID #" +
                        "${currentPendingMessage.get()?.outgoingMessage?.messageId} "

        private var lastSendStartTime = 0L

        private var lastReceiveStartTime = 0L

        suspend fun process() {
            for(message in messageChannel) {
                //send the packet itself
                currentPendingMessage.set(message)
                message.outgoingPackets = message.outgoingMessage.getPackets(mtu - BleGattServer.ATT_HEADER_SIZE)
                Napier.d("$logPrefix processor received message in channel " +
                        "${message.outgoingMessage.payload?.size} bytes MTU=${mtu}")
                val startTime = System.currentTimeMillis()
                lastSendStartTime = startTime
                try {
                    clientToServerCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    sendNextPacket(mGatt, clientToServerCharacteristic)
                    val messageReceived = message.messageReceived.await()
                    val duration = System.currentTimeMillis() - startTime
                    Napier.d({
                        val bytesSent = message.outgoingMessage.payload?.size ?: -1
                        val bytesReceived = messageReceived.payload?.size ?: -1
                        val speedKBps = (bytesSent + bytesReceived).toFloat() / ((duration.toFloat() * 1024) / 1000)
                        """$logPrefix SUCCESS sent $bytesSent bytes received 
                        |$bytesReceived bytes in ${duration}ms (${bytesSent + bytesReceived}
                        |@ ${String.format("%.2f", speedKBps)} KB/s
                    """.trimMargin()})
                    message.responseListener?.onResponseReceived("",
                            messageReceived, null)

                }catch(e: Exception) {
                    Napier.e("$logPrefix MessageProcessor exception", e)
                    message.responseListener?.onResponseReceived("", null,
                            e)
                }
                Napier.d("$logPrefix waiting for next message")
            }
        }


        fun sendNextPacket(gatt: BluetoothGatt, lastCharacteristic: BluetoothGattCharacteristic) {
            val pendingMessageVal = currentPendingMessage.get()
            if(pendingMessageVal != null) {
                val packetNum = pendingMessageVal.outgoingPacketNum
                lastCharacteristic.value = pendingMessageVal.outgoingPackets[packetNum]
                val submitted = gatt.writeCharacteristic(lastCharacteristic)
                Napier.d({"$logPrefix sent packet #$packetNum/${pendingMessageVal.outgoingPackets.size} accepted=$submitted"})
                if(!submitted) {
                    Napier.e("$logPrefix packet submission not accepted!")
                }
            }else {
                Napier.e("$logPrefix pendingmessageval = null")
            }
        }

        fun requestReadNextPacket(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val requestOk = gatt.readCharacteristic(characteristic)
            Napier.d("${logPrefix }Request read : accepted=$requestOk")
        }

        fun onCharacteristicWrite(gatt: BluetoothGatt,
                                  characteristic: BluetoothGattCharacteristic, status: Int) {
            Napier.d((if (status == BluetoothGatt.GATT_SUCCESS) "$logPrefix: Allowed to send packets"
            else "$logPrefix: Not allowed to send packets") + " to ${gatt.device.address}")

            val currentMessageVal = currentPendingMessage.get()
            if(currentMessageVal != null) {
                if (++currentMessageVal.outgoingPacketNum < currentMessageVal.outgoingPackets.size) {
                    sendNextPacket(gatt, characteristic)
                } else {
                    val sendDuration = System.currentTimeMillis() - lastSendStartTime
                    Napier.v("$logPrefix sent all " +
                            "${currentPendingMessage.get()?.outgoingPackets?.size} packets - " +
                            "${currentMessageVal.outgoingMessage.payload?.size}  bytes in " +
                            "$sendDuration ms. Starting read.")

                    lastReceiveStartTime = System.currentTimeMillis()
                    //now read the server's reply
                    requestReadNextPacket(gatt, characteristic)
                }
            }
        }


        fun readCharacteristics(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val currentMessageVal = currentPendingMessage.get()
            if(currentMessageVal != null) {
                val complete = currentMessageVal.incomingMessage.onPackageReceived(characteristic.value)
                currentMessageVal.incomingPacketNum++
                val expectedPayload = currentMessageVal.incomingMessage.length
                val totalExpectedPackets  = ceil(expectedPayload.toFloat() / currentMessageVal.outgoingMessage.mtu )
                Napier.d("$logPrefix received packet " +
                        "#${currentMessageVal.incomingPacketNum}/$totalExpectedPackets expect " +
                        "MTU= ${currentMessageVal.incomingMessage.mtu} message length= $expectedPayload bytes")
                if(complete) {
                    val duration = System.currentTimeMillis() - lastReceiveStartTime
                    val payloadSize = currentMessageVal.incomingMessage.payload?.size ?: 1
                    val speedKbps = payloadSize.toFloat() / ((duration.toFloat() * 1024)/1000)
                    Napier.v("$logPrefix SUCCESS received complete message " +
                            "${currentMessageVal.incomingMessage.payload?.size} bytes in " +
                            "$duration ms @ ${String.format("%.2f", speedKbps)} KB/s")
                    currentMessageVal.messageReceived.complete(currentMessageVal.incomingMessage)
                    currentMessageVal.responseListener?.onResponseReceived("",
                            currentMessageVal.incomingMessage, null)
                }else {
                    requestReadNextPacket(gatt, characteristic)
                }
            }else {
                Napier.e("$logPrefix currentMessageVal = null")
            }
        }
    }


    private val logPrefix
        get() = "BleMessageGattClientCallback Request ID# ${messageToSend?.messageId ?: -1}"

    init {
        receivedMessage = BleMessage()
        lastActive = System.currentTimeMillis()
    }

    suspend fun sendMessage(outgoingMessage: BleMessage,
                            responseListener: BleMessageResponseListener? = null): BleMessage {
        val pendingMessage = PendingMessage(0, outgoingMessage,
                responseListener = responseListener)

        Napier.d( "BleMessageGattClientCallback: sendMessage #" +
                "${outgoingMessage.messageId} MTU=$currentMtu")
        messageChannel.send(pendingMessage)
        return pendingMessage.messageReceived.await()
    }

    /**
     * Receive MTU change event when server device changed it's MTU to the requested value.
     */
    override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
        super.onMtuChanged(gatt, mtu, status)
        Napier.d("$logPrefix: MTU changed from $currentMtu to $mtu")
        currentMtu.set(mtu)
        mtuCompletableDeferred.complete(mtu)
    }

    /**
     * Start discovering GATT services when peer device is connected or disconnects from GATT
     * when connection failed.
     */
    override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)

        val remoteDeviceAddress = gatt.device.address

        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            Napier.d("$logPrefix: Device connected to $remoteDeviceAddress")

            if (!serviceDiscoveryRef.get()) {
                Napier.d("$logPrefix: Discovering services offered by $remoteDeviceAddress")
                serviceDiscoveryRef.set(true)
                gatt.discoverServices()
            }
        } else if(status != BluetoothGatt.GATT_SUCCESS && !mClosed.get()){
            Napier.e("$logPrefix: onConnectionChange not successful and connection is not " +
                    "closed $status from $remoteDeviceAddress")
            cleanup(gatt)
            responseListener?.onResponseReceived(remoteDeviceAddress, null,
                        IOException("BLE onConnectionStateChange not successful." +
                                "Status = " + status))
        }
    }


    /**
     * Enable notification to be sen't back when characteristics are modified
     * from the GATT server's side.
     */
    override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
        super.onServicesDiscovered(gatt, status)
        val service = gatt.services.firstOrNull { it.uuid.toString() == USTADMOBILE_BLE_SERVICE_UUID }  //findMatchingService(gatt.services)
        if (service == null) {
            Napier.e("$logPrefix: ERROR Ustadmobile Service not found on " +
                    gatt.device.address)
            responseListener?.onResponseReceived(gatt.device.address, null,
                    IOException("UstadMobile service not found on device"))
            cleanup(gatt)
            return
        }

        Napier.d("$logPrefix: Ustadmobile Service found on ${gatt.device.address}")
        val characteristics = service.characteristics

        val clientToServerCharacteristic = characteristics.firstOrNull {
            it.uuid == UUID.fromString(NetworkManagerBleCommon.BLE_CHARACTERISTIC)
        }

        if (clientToServerCharacteristic != null) {
            clientToServerCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT

            GlobalScope.launch(Dispatchers.Main) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Napier.d("$logPrefix: Requesting MTU changed from $currentMtu to " +
                            "$MAXIMUM_MTU_SIZE")
                    gatt.requestMtu(MAXIMUM_MTU_SIZE)
                    var changedMtu = -1
                    withTimeoutOrNull(MAX_DELAY_TIME) {
                        changedMtu = mtuCompletableDeferred.await()
                    }
                    Napier.d("$logPrefix: Deferrable MTU change: got $changedMtu")
                }
                val processor0 = PendingMessageProcessor(messageChannel, gatt, currentMtu.get(),
                        clientToServerCharacteristic)
                processorMap.put(clientToServerCharacteristic.uuid, processor0)

                GlobalScope.launch(Dispatchers.Main) {
                    processor0.process()
                }
            }
        }
    }



    /**
     * Start transmitting message packets to the peer device once given permission
     * to write on the clientToServerCharacteristic
     */
    override fun onCharacteristicWrite(gatt: BluetoothGatt,
                                       characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicWrite(gatt, characteristic, status)

        val messageProcessor = processorMap[characteristic.uuid]
        if(messageProcessor != null) {
            messageProcessor.onCharacteristicWrite(gatt, characteristic, status)
        }else {
            //log it
            Napier.v("onCharacteristicWrite no message processor " +
                    "for clientToServerCharacteristic UUID ${characteristic.uuid}")
        }
    }

    /**
     * Read modified valued from the characteristics when changed from GATT server's end.
     */
    override fun onCharacteristicRead(gatt: BluetoothGatt,
                                      characteristic: BluetoothGattCharacteristic, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        Napier.d("onCharacteristicRead")
        readCharacteristics(gatt, characteristic)
    }

    /**
     * Receive notification when characteristics value has been changed from GATT server's side.
     */
    override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                         characteristic: BluetoothGattCharacteristic) {
        super.onCharacteristicChanged(gatt, characteristic)
        Napier.d("onCharacteristicChanged")
        readCharacteristics(gatt, characteristic)
    }

    /**
     * Read values from the service clientToServerCharacteristic
     * @param gatt Bluetooth Gatt object
     * @param characteristic Modified service clientToServerCharacteristic to read that value from
     */
    private fun readCharacteristics(gatt: BluetoothGatt,
                                    characteristic: BluetoothGattCharacteristic) {
        val processor = processorMap[characteristic.uuid]
        if(processor != null) {
            processor.readCharacteristics(gatt, characteristic)
        }else {
            Napier.v("readCharacteristic no message processor " +
                    "for clientToServerCharacteristic UUID ${characteristic.uuid}")
            //log it
        }
    }

    private fun uuidMatches(uuidString: String, vararg matches: String): Boolean {
        for (match in matches) {
            if (uuidString.equals(match, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun cleanup(gatt: BluetoothGatt) {
        try {
            if (mConnected.get()) {
                gatt.disconnect()
                mConnected.set(false)
                Napier.i("$logPrefix: disconnected")
            }

            if (!mClosed.get()) {
                gatt.close()
                mClosed.set(true)
                Napier.i("$logPrefix: closed")
            }
        } catch (e: Exception) {
            Napier.e("$logPrefix: ERROR disconnecting")
        } finally {
            Napier.i("$logPrefix: cleanup done")
        }
    }
}
