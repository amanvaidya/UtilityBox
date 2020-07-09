package com.example.utilitybox.helpers

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log

import java.io.IOException
import java.util.UUID


class ConnectThread : Thread() {
    private var bDevice:BluetoothDevice?=null
    private var bSocket:BluetoothSocket?=null

    // Establish connection
    fun connect(device:BluetoothDevice, UUID:UUID):Boolean{
        // Get the MAC address
        bDevice = device
        try {
            // Create a RFCOMM socket with the UUID
            bSocket = bDevice!!.createRfcommSocketToServiceRecord(UUID)
        } catch (e: IOException) {
            Log.d("CONNECT", "Failed at create RFCOMM")
            return false
        }

        if (bSocket == null) {
            return false
        }
        try {
            // Try to connect
            bSocket!!.connect()
        } catch (e: IOException) {
            Log.d("CONNECT", "Failed at socket connect")
            try {
                bSocket!!.close()
            } catch (close: IOException) {
                Log.d("CONNECT", "Failed at socket close")
            }

            // Moved return false out from inner catch, making it return false when connect is unsuccessful.
            // Return value used to determine if intent switch to next screen.
            return false
        }

        return true
    }
    // Close connection
    fun closeConnect(): Boolean {
        try {
            bSocket?.close()
        } catch (e: IOException) {
            Log.d("CONNECT", "Failed at socket close")
            return false
        }

        return true
    }
    // Returns the bluetooth socket object
    fun getSocket(): BluetoothSocket? {
        return bSocket
    }


}