package com.mtkclientandroid

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * USB Host surface used by androidusblib.py via Chaquopy.
 * Permission dialogs must be requested from an Activity first.
 */
object UsbBridge {
    private const val TAG = "UsbBridge"

    private var appContext: Context? = null
    private var manager: UsbManager? = null

    private var device: UsbDevice? = null
    private var connection: UsbDeviceConnection? = null
    private var iface: UsbInterface? = null
    private var epIn: UsbEndpoint? = null
    private var epOut: UsbEndpoint? = null

    @Volatile
    private var lastStatus: String = "not initialized"

    @JvmStatic
    fun init(context: Context) {
        appContext = context.applicationContext
        manager = appContext?.getSystemService(Context.USB_SERVICE) as? UsbManager
        lastStatus = "initialized"
    }

    @JvmStatic
    fun getLastStatus(): String = lastStatus

    @JvmStatic
    fun getVid(): Int = device?.vendorId ?: -1

    @JvmStatic
    fun getPid(): Int = device?.productId ?: -1

    private fun findDevice(vid: Int, pid: Int): UsbDevice? =
        manager?.deviceList?.values?.firstOrNull { it.vendorId == vid && it.productId == pid }

    @JvmStatic
    fun detectDevices(): String {
        val arr = JSONArray()
        manager?.deviceList?.values?.forEach { d ->
            val o = JSONObject()
            o.put("vid", d.vendorId)
            o.put("pid", d.productId)
            o.put("name", d.deviceName)
            o.put("interfaces", d.interfaceCount)
            o.put("hasPermission", manager?.hasPermission(d) == true)
            val ifaces = JSONArray()
            for (i in 0 until d.interfaceCount) {
                val itf = d.getInterface(i)
                var bulkIn = 0
                var bulkOut = 0
                for (e in 0 until itf.endpointCount) {
                    val ep = itf.getEndpoint(e)
                    if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                        if (ep.direction == UsbConstants.USB_DIR_IN) bulkIn++ else bulkOut++
                    }
                }
                val io = JSONObject()
                io.put("index", i)
                io.put("class", itf.interfaceClass)
                io.put("subclass", itf.interfaceSubclass)
                io.put("bulkIn", bulkIn)
                io.put("bulkOut", bulkOut)
                ifaces.put(io)
            }
            o.put("ifaceDetail", ifaces)
            arr.put(o)
        }
        return arr.toString()
    }

    @JvmStatic
    fun hasPermission(vid: Int, pid: Int): Boolean {
        val d = findDevice(vid, pid) ?: return false
        return manager?.hasPermission(d) ?: false
    }

    @JvmStatic
    fun connect(portConfigJson: String, devClass: Int): Boolean {
        close(false)
        val mgr = manager
        if (mgr == null) {
            lastStatus = "UsbManager is null (init not called?)"
            Log.e(TAG, lastStatus)
            return false
        }

        val configs = JSONArray(portConfigJson)
        var found: UsbDevice? = null
        var wantIface = -1
        loop@ for (i in 0 until configs.length()) {
            val c = configs.getJSONArray(i)
            val vid = c.getInt(0)
            val pid = c.getInt(1)
            val ifaceNum = if (c.length() > 2) c.getInt(2) else -1
            val d = findDevice(vid, pid)
            if (d != null) {
                found = d
                wantIface = ifaceNum
                break@loop
            }
        }
        if (found == null) {
            found = mgr.deviceList?.values?.firstOrNull { it.vendorId == 0x0E8D }
            wantIface = -1
        }
        val dev = found
        if (dev == null) {
            lastStatus = "No MTK USB device attached (looked for portconfig=$portConfigJson)"
            Log.w(TAG, lastStatus)
            return false
        }
        if (!mgr.hasPermission(dev)) {
            lastStatus = "No USB permission for ${dev.deviceName} (vid=0x${dev.vendorId.toString(16)} pid=0x${dev.productId.toString(16)}). Tap Detect Device first."
            Log.w(TAG, lastStatus)
            return false
        }
        val conn = mgr.openDevice(dev)
        if (conn == null) {
            lastStatus = "openDevice() failed for ${dev.deviceName}"
            Log.w(TAG, lastStatus)
            return false
        }

        try {
            if (dev.configurationCount > 0) {
                val cfg = dev.getConfiguration(0)
                val okCfg = conn.setConfiguration(cfg)
                Log.i(TAG, "setConfiguration(0) -> $okCfg (interfaces=${cfg.interfaceCount})")
                if (!okCfg) {
                    Log.w(TAG, "setConfiguration returned false; continuing")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "setConfiguration: ${e.message}")
        }

        var selectedIface: UsbInterface? = null
        if (wantIface in 0 until dev.interfaceCount) {
            selectedIface = dev.getInterface(wantIface)
        } else {
            for (i in 0 until dev.interfaceCount) {
                val itf = dev.getInterface(i)
                if (devClass == -1 || itf.interfaceClass == devClass) {
                    selectedIface = itf
                    break
                }
            }
            if (selectedIface == null) {
                for (i in 0 until dev.interfaceCount) {
                    val itf = dev.getInterface(i)
                    var hasIn = false
                    var hasOut = false
                    for (e in 0 until itf.endpointCount) {
                        val ep = itf.getEndpoint(e)
                        if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                            if (ep.direction == UsbConstants.USB_DIR_IN) hasIn = true
                            else hasOut = true
                        }
                    }
                    if (hasIn && hasOut) {
                        Log.i(TAG, "Falling back to interface $i (class=0x${itf.interfaceClass.toString(16)}) with bulk endpoints")
                        selectedIface = itf
                        break
                    }
                }
            }
        }
        if (selectedIface == null) {
            lastStatus = "No bulk interface on ${dev.deviceName} (interfaces=${dev.interfaceCount}, devClass=$devClass)"
            Log.w(TAG, lastStatus)
            conn.close()
            return false
        }
        if (!conn.claimInterface(selectedIface, true)) {
            lastStatus = "claimInterface(${selectedIface.id}) failed"
            Log.w(TAG, lastStatus)
            conn.close()
            return false
        }

        var inEp: UsbEndpoint? = null
        var outEp: UsbEndpoint? = null
        for (i in 0 until selectedIface.endpointCount) {
            val ep = selectedIface.getEndpoint(i)
            if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                if (ep.direction == UsbConstants.USB_DIR_IN) inEp = ep else outEp = ep
            }
        }
        if (inEp == null || outEp == null) {
            lastStatus = "No bulk IN/OUT on interface ${selectedIface.id}"
            Log.w(TAG, lastStatus)
            conn.releaseInterface(selectedIface)
            conn.close()
            return false
        }

        device = dev
        connection = conn
        iface = selectedIface
        epIn = inEp
        epOut = outEp
        // Report the interface the same way MainActivity's "Detect Device"
        // diagnostic does (positional index into getInterface(i)), not
        // selectedIface.id (the descriptor's own bInterfaceNumber field).
        // The two numbers don't have to match -- composite MTK USB
        // descriptors commonly declare interface numbers out of the order
        // Android returns them in -- and logging the descriptor's number
        // here made it look like the wrong interface had been claimed even
        // when the actually-selected one had the correct class and bulk
        // endpoints all along.
        var selectedIdx = -1
        for (i in 0 until dev.interfaceCount) {
            if (dev.getInterface(i) === selectedIface) { selectedIdx = i; break }
        }
        lastStatus = "connected vid=0x${dev.vendorId.toString(16)} pid=0x${dev.productId.toString(16)} " +
            "iface[$selectedIdx] (bInterfaceNumber=${selectedIface.id}) class=0x${selectedIface.interfaceClass.toString(16)} " +
            "epIn=0x${inEp.address.toString(16)}(${inEp.maxPacketSize}) " +
            "epOut=0x${outEp.address.toString(16)}(${outEp.maxPacketSize})"
        Log.i(TAG, lastStatus)
        return true
    }

    @JvmStatic
    fun close(reset: Boolean) {
        try {
            iface?.let { connection?.releaseInterface(it) }
            connection?.close()
        } catch (e: Exception) {
            Log.w(TAG, "close() error: ${e.message}")
        }
        connection = null
        iface = null
        epIn = null
        epOut = null
        device = null
        lastStatus = "closed"
    }

    /** Abort an in-flight transfer so a cancelled command can return. */
    @JvmStatic
    fun abort() {
        close(false)
    }

    @JvmStatic
    fun isConnected(): Boolean = connection != null && epIn != null && epOut != null

    @JvmStatic
    fun getMaxPacketIn(): Int = epIn?.maxPacketSize ?: 0

    @JvmStatic
    fun getMaxPacketOut(): Int = epOut?.maxPacketSize ?: 0

    @JvmStatic
    fun bulkWrite(data: ByteArray, timeout: Int): Int {
        if (UiBridge.isCancelled()) return -1
        val conn = connection ?: return -1
        val ep = epOut ?: return -1
        val n = conn.bulkTransfer(ep, data, data.size, timeout)
        if (n < 0) Log.w(TAG, "bulkWrite failed n=$n len=${data.size} timeout=$timeout")
        return n
    }

    @JvmStatic
    fun bulkRead(length: Int, timeout: Int): ByteArray {
        if (UiBridge.isCancelled()) return ByteArray(0)
        val conn = connection ?: return ByteArray(0)
        val ep = epIn ?: return ByteArray(0)
        val buf = ByteArray(length.coerceAtLeast(1))
        val n = conn.bulkTransfer(ep, buf, buf.size, timeout)
        return if (n > 0) buf.copyOf(n) else ByteArray(0)
    }

    @JvmStatic
    fun controlTransferOut(requestType: Int, request: Int, value: Int, index: Int, data: ByteArray, timeout: Int): Int {
        val conn = connection ?: return -1
        return conn.controlTransfer(requestType, request, value, index, data, data.size, timeout)
    }

    @JvmStatic
    fun controlTransferIn(requestType: Int, request: Int, value: Int, index: Int, length: Int, timeout: Int): ByteArray {
        val conn = connection ?: return ByteArray(0)
        val buf = ByteArray(length.coerceAtLeast(0))
        if (buf.isEmpty()) return buf
        val n = conn.controlTransfer(requestType, request, value, index, buf, buf.size, timeout)
        return if (n > 0) buf.copyOf(n) else ByteArray(0)
    }

    /**
     * Android equivalent of the desktop Kamakiri exploit's raw-pyusb
     * reset dance (kamakiri.py Kamakiri.exploit()).
     *
     * After the watchdog-reset payload runs, the device briefly drops off
     * the bus and re-enumerates. Desktop mtkclient re-opens it with pyusb
     * and disables libusb's managed interface claim so a control transfer
     * can go out without re-claiming the (now stale) interface. On Android
     * that workaround isn't needed: control transfers addressed to
     * endpoint 0 target the device itself, not a claimed interface, so a
     * plain openDevice() + controlTransfer() is sufficient -- no
     * claimInterface() call at all.
     *
     * This does not touch the primary `device`/`connection`/`iface` state;
     * it opens and closes its own short-lived connection so it can't
     * disturb a session the rest of the class is mid-way through.
     */
    @JvmStatic
    fun waitAndRawControlTransfer(
        vid: Int, waitMs: Int,
        requestType: Int, request: Int, value: Int, index: Int, length: Int
    ): Boolean {
        val mgr = manager
        if (mgr == null) {
            lastStatus = "Kamakiri: UsbManager is null (init not called?)"
            Log.e(TAG, lastStatus)
            return false
        }

        val deadline = System.currentTimeMillis() + waitMs.coerceAtLeast(0)
        var dev: UsbDevice? = null
        while (System.currentTimeMillis() < deadline) {
            val candidate = mgr.deviceList?.values?.firstOrNull { it.vendorId == vid }
            if (candidate != null && mgr.hasPermission(candidate)) {
                dev = candidate
                break
            }
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                break
            }
        }
        if (dev == null) {
            lastStatus = "Kamakiri: device (vid=0x${vid.toString(16)}) did not reappear with " +
                "USB permission within ${waitMs}ms after the reset. If it re-enumerated with a " +
                "new device id, tap Detect Device again quickly and retry."
            Log.w(TAG, lastStatus)
            return false
        }

        val conn = mgr.openDevice(dev)
        if (conn == null) {
            lastStatus = "Kamakiri: openDevice() failed on reappeared device"
            Log.w(TAG, lastStatus)
            return false
        }
        return try {
            val len = length.coerceAtLeast(0)
            val buf = if (len > 0) ByteArray(len) else null
            val n = conn.controlTransfer(requestType, request, value, index, buf, len, 2000)
            lastStatus = "Kamakiri: raw control transfer after reset -> $n"
            Log.i(TAG, lastStatus)
            n >= 0
        } catch (e: Exception) {
            lastStatus = "Kamakiri: raw control transfer threw: ${e.message}"
            Log.w(TAG, lastStatus)
            false
        } finally {
            conn.close()
        }
    }
}
