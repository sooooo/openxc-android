package com.openxc.remote.sources;

import java.net.URI;
import java.net.URISyntaxException;

import java.util.Map;

import android.content.Context;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;

import android.util.Log;

public class UsbVehicleDataSource extends JsonVehicleDataSource {
    private static final String TAG = "UsbVehicleDataSource";
    private static URI DEFAULT_USB_DEVICE_NAME = null;
    static {
        try {
            DEFAULT_USB_DEVICE_NAME = new URI("usb://TODO");
        } catch(URISyntaxException e) { }
    }

    private boolean mRunning;
    private String mDeviceName;

    private UsbDeviceConnection mConnection;
    private UsbEndpoint mEndpoint;

    public UsbVehicleDataSource(Context context,
            VehicleDataSourceCallbackInterface callback, URI deviceName)
            throws VehicleDataSourceException {
        super(callback);
        if(deviceName == null) {
            throw new VehicleDataSourceException(
                    "No USB device name specified for the data source");
        }

        mRunning = true;
        mDeviceName = deviceName.toString();
        setupDevice((UsbManager) context.getSystemService(Context.USB_SERVICE),
                mDeviceName);

        Log.d(TAG, "Starting new USB data source with device" +
                mDeviceName);
    }

    public UsbVehicleDataSource(Context context,
            VehicleDataSourceCallbackInterface callback)
            throws VehicleDataSourceException{
        this(context, callback, DEFAULT_USB_DEVICE_NAME);
    }

    public void stop() {
        Log.d(TAG, "Stopping USB listener");
        mRunning = false;
    }

    public void run() {
        byte[] bytes = new byte[128];

        StringBuffer buffer = new StringBuffer();;
        while(mRunning) {
            int received = mConnection.bulkTransfer(
                    mEndpoint, bytes, bytes.length, 0);
            byte[] receivedBytes = new byte[received];
            System.arraycopy(bytes, 0, receivedBytes, 0, received);
            buffer.append(new String(receivedBytes));

            parseStringBuffer(buffer);
        }
    }

    private void parseStringBuffer(StringBuffer buffer) {
        int newlineIndex = buffer.indexOf("\r\n");
        if(newlineIndex != -1) {
            final String messageString = buffer.substring(0, newlineIndex);
            buffer.delete(0, newlineIndex + 1);
            handleJson(messageString);
        }
    }

    private void setupDevice(UsbManager manager, String deviceName) {
        Log.i(TAG, "Initializing USB device " + deviceName);

        Map<String, UsbDevice> devices = manager.getDeviceList();
        UsbDevice device = devices.get(deviceName);
        UsbInterface iface = device.getInterface(0);
        Log.d(TAG, "Connecting to endpoint 1 on interface " + iface);
        mEndpoint = iface.getEndpoint(1);
        mConnection = manager.openDevice(device);
        mConnection.claimInterface(iface, true);
    }
}
