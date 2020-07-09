package com.example.utilitybox.helpers

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.util.*
import android.system.Os.accept



class ListenThread(){
    private var listenSocket: BluetoothSocket?=null
    private val buffer: ByteArray? = null
    var temp: BluetoothServerSocket?=null
    fun acceptConnect(adapter: BluetoothAdapter, mUUID: UUID):Boolean{

        try {
            temp = adapter.listenUsingRfcommWithServiceRecord("BTService", mUUID)
        } catch (e: IOException) {
            Log.d("LISTEN", "Error at listen using RFCOMM")
        }
        try {
            listenSocket = temp?.accept()
        } catch (e: IOException) {
            Log.d("LISTEN", "Error at accept connection")
        }
        if (listenSocket != null) {
            try {
                temp?.close()
            } catch (e: IOException) {
                Log.d("LISTEN", "Error at socket close")
            }

            return true
        }
        return false
    }
    // Close connection
    fun closeConnect(): Boolean {
        try {
            listenSocket?.close()
        } catch (e: IOException) {
            Log.d("LISTEN", "Failed at socket close")
            return false
        }

        return true
    }

    // Return socket object
    fun getSocket(): BluetoothSocket? {
        return listenSocket
    }
}