package com.ljyh.mei.playback.usb

import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice

internal fun UsbDevice.hasPcmOutput(): Boolean = (0 until interfaceCount).any { i ->
    val usbInterface = getInterface(i)
    usbInterface.interfaceClass == UsbConstants.USB_CLASS_AUDIO && usbInterface.interfaceSubclass == 2 &&
        (0 until usbInterface.endpointCount).any { e ->
            val endpoint = usbInterface.getEndpoint(e)
            endpoint.direction == UsbConstants.USB_DIR_OUT && endpoint.type == UsbConstants.USB_ENDPOINT_XFER_ISOC
        }
}

internal object UsbNativeOwnership {
    private var owner: Any? = null
    @Synchronized fun acquire(token: Any): Boolean {
        if (owner != null) return false
        owner = token
        return true
    }
    @Synchronized fun release(token: Any) { if (owner === token) owner = null }
}
