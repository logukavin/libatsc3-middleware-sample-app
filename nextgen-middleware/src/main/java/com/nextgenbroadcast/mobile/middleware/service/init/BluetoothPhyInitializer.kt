package com.nextgenbroadcast.mobile.middleware.service.init

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.*
import android.content.res.XmlResourceParser
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.nextgenbroadcast.mobile.middleware.BuildConfig
import com.nextgenbroadcast.mobile.middleware.IAtsc3ReceiverCore
import com.nextgenbroadcast.mobile.middleware.atsc3.source.BluetoothPhyAtsc3Source
import com.nextgenbroadcast.mobile.middleware.atsc3.utils.XmlUtils
import com.nextgenbroadcast.mobile.middleware.service.BindableForegroundService
import org.ngbp.libatsc3.middleware.android.phy.CeWiBluetoothPHYAndroid
import org.xmlpull.v1.XmlPullParser
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*

internal class BluetoothPhyInitializer(
        private val receiver: IAtsc3ReceiverCore,
        private val bluetoothManager: BluetoothManager
) : IServiceInitializer {

    @Volatile
    private var isCanceled = false

    private var bluetoothAdapter: BluetoothAdapter? = null

    //BluetoothAdapter.ACTION_STATE_CHANGED
    private var broadcastReceiver:BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive: intent: $intent")
            var bundle: Bundle? = intent?.extras
            var state = bundle?.get(BluetoothAdapter.EXTRA_STATE)
            if(state == BluetoothAdapter.STATE_ON) {
//                if(!checkBondedDevices()) {
//
//                }

                startBluetoothListenerThread()
            }
        }

    }

    //jjustman-2023-05-24 - todo
    //        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
    override suspend fun initialize(context: Context, components: Map<Class<*>, Pair<Int, String>>): Boolean {
        bluetoothAdapter = bluetoothManager.getAdapter()
        if(bluetoothAdapter == null) {
            Log.d(TAG, "::initialize - bluetoothAdapter is NULL!");
            return false;
        }

        if (bluetoothAdapter?.isEnabled == false) {
            context.registerReceiver(broadcastReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            BindableForegroundService.MainActivityReference?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            //(context as Application)
            //getParentActivity()?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            //context.getParentActivity()?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            //getActivity(context)?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            //context.activity()?.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

            return false;
        }

        //otherwise
        startBluetoothListenerThread()


//
//        components.filter { (clazz, _) ->
//            Atsc3NdkPHYClientBase::class.java.isAssignableFrom(clazz)
//        }.map { (clazz, data) ->
//            val (resource) = data
//            @Suppress("UNCHECKED_CAST")
//            Pair(clazz as Class<Atsc3NdkPHYClientBase>, resource)
//        }.forEach { (component, resource) ->
//            try {
//                val parser = context.resources.getXml(resource)
//                val params = readPhyAttributes(parser)
//
//                val instance: Any = component.getDeclaredConstructor().newInstance()
//                val phy = instance as Atsc3NdkPHYClientBase
//
//                var connected = false
//                params.forEach params@{ (fd, devicePath, freqKhz) ->
//                    if (isCanceled) return@forEach
//
//                    try {
//                        connected = suspendCancellableCoroutine { cont ->
//                            receiver.openRoute(NdkPhyAtsc3Source(phy, fd, devicePath, freqKhz)) { result ->
//                                cont.resume(result)
//                            }
//                        }
//                        if (connected) return@params
//                    } catch (t: Throwable) {
//                        t.printStackTrace()
//                    }
//                }
//
//                if (connected) {
//                    return true
//                } else {
//                    try {
//                        phy.deinit()
//                    } catch (e: Exception) {
//                        Log.d(TAG, "Crash when trying deinit an Onboard Phy", e)
//                    }
//                }
//            } catch (e: Resources.NotFoundException) {
//                Log.w(TAG, "Onboard Phy resource reading error: ", e)
//            }
//
//            if (isCanceled) return@forEach
//        }
//
       return false
    }

    private lateinit var deviceListenerThread:DeviceListenerThread

    fun startBluetoothListenerThread() {

        if(!isCanceled && !ConnectThreadIsRunning) {
            deviceListenerThread = DeviceListenerThread(bluetoothAdapter!!);
            deviceListenerThread.start()
            ConnectThreadIsRunning = true
        }
    /*

    2023-02-12 01:30:31.837 16115-16115 pairedDevices           com...cast.mobile.middleware.sample  D  name: jjbtstreamer24 58:93:D8:4B:9C:92, hardwareAddr: 58:93:D8:4B:9C:92
    2023-02-12 01:30:31.847 16115-16115 btUUIDS                 com...cast.mobile.middleware.sample  D  00001101-0000-1000-8000-00805f9b34fb
     */
    }

    /*

    jjustman-2023-05-03 - TODO: if we aren't bonded with the dongle, initiate startDiscovery()

    https://developer.android.com/reference/android/bluetooth/BluetoothAdapter#startDiscovery()
     */

    private inner class DeviceListenerThread(bluetoothAdapter: BluetoothAdapter) : Thread() {

        private val bluetoothAdapter: BluetoothAdapter = bluetoothAdapter
        var matchingdevice: BluetoothDevice? = null
        var connectThread: ConnectThread? = null

        @SuppressLint("MissingPermission")
        public override fun run() {

            while(!isCanceled) {
                Log.d("bt:DeviceListenerThread", String.format("loop"))

                val pairedDevices: Set<BluetoothDevice>? =
                    bluetoothAdapter?.bondedDevices

                pairedDevices?.forEach { device ->
                    val deviceName = device.name
                    val deviceHardwareAddress = device.address // MAC address
                    if (deviceName.contains("CeWi", true)) {

                        matchingdevice = device
                        Log.d("bt:pairedDevices", String.format("name: %s, hardwareAddr: %s", deviceName, deviceHardwareAddress));
                    }
                }

                //fetchUuidsWithSdp
                var btUUIDs: Array<ParcelUuid>? = matchingdevice?.getUuids()
                btUUIDs?.forEach { entry ->
                    Log.d("btUUIDS", entry.toString())
                }


                if (matchingdevice != null &&  matchingdevice?.bondState == BluetoothDevice.BOND_BONDED) {
                    //jjustman-2023-03-19 - not needed if we are paired
                    // matchingdevice?.fetchUuidsWithSdp();
                    try {
                        bluetoothAdapter.cancelDiscovery();
                    } catch (ex:Exception) {
                        Log.d("bt:pairedDevices", String.format("bluetoothAdapter.cancelDiscovery exception is: %s", ex));

                    }

                    connectThread = ConnectThread(matchingdevice!!)
                    connectThread?.start()

                    Log.d("bt:DeviceListenerThread", String.format("calling connectThread.join"))
                    connectThread?.join();
                }
                sleep(10000)
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            //jjustman-2023-05-24 - dirty hack
            connectThread?.interrupt()
        }
    }


    fun manageMyConnectedSocket(socket: BluetoothSocket) {

        val cewiBluetoothPhy: CeWiBluetoothPHYAndroid = CeWiBluetoothPHYAndroid()

        val mmInStream: InputStream = socket.inputStream
        val mmOutStream: OutputStream = socket.outputStream

        cewiBluetoothPhy.setBluetoothOutputStream(mmOutStream);
        receiver.openRoute(BluetoothPhyAtsc3Source(cewiBluetoothPhy, -1, "bluetooth", 0), false, {})


        //jjustman-2023-05-03 - using 1024 bytes on S10+ -> results in about 350kbit/s, trying to increase to larger buf size
        //          no difference when using 16384
        //          - added mmOutStream.write(0xFF) when bufSize=8192
        //jjustman-2023-05-11-01.58 val bufSize = 1024; //jjustman-2023-04-15 - hack, BT RFCOMM mtu on Android is 990 bytes...
        val bufSize = 1024;

        // jjustman-2023-04-05 - hack! 16544; //188*10
        val mmBuffer: ByteArray = ByteArray(bufSize) // mmBuffer store for the stream
        var numBytes: Int // bytes returned from read()

        var bytesReceived: Int = 0;
        var lastBytesReceived = 0;
        var lastTimestamp: Long = System.currentTimeMillis()
        var loopCount: Int = 0;

        while(!isCanceled && socket.isConnected) {
            //read

            numBytes = try {
                mmInStream.read(mmBuffer)
            } catch (e: IOException) {
                Log.d(TAG, "bt:Input stream was disconnected", e)
                break
            }
            loopCount++;
            bytesReceived += numBytes; //mmBuffer.size;
            cewiBluetoothPhy.RxDataCallback(mmBuffer, numBytes);

            var timeDiffMS = System.currentTimeMillis() - lastTimestamp
            //if((loopCount++ % 100) == 0) {
            if(timeDiffMS >= 1000) {

                var bytes_last_sample_diff = bytesReceived - lastBytesReceived
                var bits_per_second_last_sample = (bytes_last_sample_diff * 8.0) / (timeDiffMS / 1000.0)
                Log.d(
                    "bt", String.format(
                        "received $bytesReceived, packet count: $loopCount, last interval: $bits_per_second_last_sample bits/s, last packet: $numBytes, bytes: 0x%02x 0x%02x 0x%02x 0x%02x",
                        mmBuffer.get(0), mmBuffer.get(1), mmBuffer.get(2), mmBuffer.get(3),
                    )
                    // last 4: 0x%02x 0x%02x 0x%02x 0x%02x"
                    // mmBuffer.get(bufSize - 4), mmBuffer.get(bufSize - 3), mmBuffer.get(bufSize - 2), mmBuffer.get(bufSize - 1),
                )

                lastBytesReceived = bytesReceived;
                lastTimestamp = System.currentTimeMillis()
            }

            //retune example
//            if((loopCount % 1000) == 0) {
//                //tune command
//                //ntohl for frequency: 533000 - 0x00 08 22 08
//                //plp0: 0x00
//                var byteArray = byteArrayOf(0xF0.toByte(), 0x08.toByte(), 0x22.toByte(), 0x08.toByte(), 0x00.toByte(), 0x00.toByte());
//                mmOutStream.write(byteArray);
//                mmOutStream.flush();
//            }
        }

        cewiBluetoothPhy.stop()
    }

    private val SPP_UUID: UUID =  UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {

        private val device: BluetoothDevice = device
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createRfcommSocketToServiceRecord(SPP_UUID)
            //device.createL2capChannel(0x01)
            //device.createInsecureL2capChannel(0x01)
        }
        var loopCount:Int = 0;

        public override fun run() {
            try {
                priority = MAX_PRIORITY
                mmSocket?.let { socket ->
                    while (!isCanceled && !socket.isConnected && (loopCount++) < 10) {
                        socket.connect()
                        if (socket.isConnected) {
                            // The connection attempt succeeded. Perform work associated with
                            // the connection in a separate thread.
                            Log.d("bt", "connected!");
                            manageMyConnectedSocket(socket)
                            Log.d("bt", "return from manageMyConnectedSocket, isConnected: $socket.isConnected");
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e("bt", "exception $ex")
                sleep(1000);
                loopCount++;
            }
        }

        // Closes the client socket and causes the thread to finish.
        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    override fun cancel() {
        isCanceled = true
        deviceListenerThread.cancel()

    }

    private fun readPhyAttributes(parser: XmlResourceParser): List<Triple<Int, String?, Int>> {
        val result = ArrayList<Triple<Int, String?, Int>>()

        try {
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) {
                    continue
                }

                if (parser.name == "resources") {
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType != XmlPullParser.START_TAG) {
                            continue
                        }

                        if (parser.name == "phy-device") {
                            var fd = -1
                            var path: String? = null
                            var freqKhz = -1

                            for (i in 0 until parser.attributeCount) {
                                when (parser.getAttributeName(i)) {
                                    "fd" -> fd = parser.getAttributeIntValue(i, -1)
                                    "path" -> path = parser.getAttributeValue(i)
                                    "freqKhz" -> freqKhz = parser.getAttributeIntValue(i, -1)
                                }
                            }

                            result.add(Triple(fd, path, freqKhz))
                        } else {
                            XmlUtils.skip(parser)
                        }
                    }
                } else {
                    XmlUtils.skip(parser)
                }
            }
        } finally {
            parser.close()
            return result
        }
    }
//
////    private fun getActivityByContext(context: Context?): Activity? {
////        if (context == null) {
////            return null
////        } else if (context is ContextWrapper && context is Activity) {
////            return context
////        } else if (context is ContextWrapper) {
////            return getActivity((context as ContextWrapper).getBaseContext())
////        }
////        return null
////    }
//
//    private fun getActivity(context: Context?): Activity? {
//        if (context == null) {
//            return null
//        } else if (context is ContextWrapper) {
//            return if (context is Activity) {
//                context
//            } else {
//                getActivity(context.baseContext)
//            }
//        }
//        return null
//    }
////    private fun Context?.getParentActivity() : AppCompatActivity? = when {
////        this is ContextWrapper -> if (this is AppCompatActivity) this else this.baseContext.getParentActivity()
////        else -> null
////    }
//
//    tailrec fun Context.activity(): Activity? = when {
//        this is Activity -> this
//        else -> (this as? ContextWrapper)?.baseContext?.activity()
//    }



    companion object {
        val TAG: String = BluetoothPhyInitializer::class.java.simpleName

        private const val SERVICE_ACTION = "${BuildConfig.LIBRARY_PACKAGE_NAME}.intent.action"

        private var ConnectThreadIsRunning:Boolean = false
        const val REQUEST_ENABLE_BT = 31338

    }
}