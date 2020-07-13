package com.example.utilitybox.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.utilitybox.R
import android.view.LayoutInflater
import android.bluetooth.BluetoothSocket
import com.example.utilitybox.helpers.ConnectThread
import com.example.utilitybox.helpers.ListenThread
import com.example.utilitybox.helpers.MainConversation
import com.example.utilitybox.helpers.DeviceInfo
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import android.util.Log
import android.widget.*
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import java.util.*
import kotlin.collections.ArrayList

open class FragmentNotification : Fragment(){

    //permission
    private var REQUEST_RECORD_AUDIO_PERMISSION = 200
    private var permissionToRecordAccepted = false
    private var permissions = arrayOf<String>(Manifest.permission.RECORD_AUDIO)
    //ui
    private var listen: Button? = null
    private var connect: Button? = null
    private var disconnect: Button? = null
    private var audio: Button? = null
    private var listView: ListView? = null
    //Bluetooth
    private var MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    private lateinit var mBluetoothAdapter: BluetoothAdapter
    private var pairedDevices: Set<BluetoothDevice>? = null
    private var device: BluetoothDevice? = null
    private var deviceList: ArrayList<DeviceInfo>? = null
    private var adapter: ArrayAdapter<DeviceInfo>? = null
    private var audioClient: MainConversation? = null
    private var listenThread: ListenThread? = null
    private var connectThread: ConnectThread? = null
    private var bSocket: BluetoothSocket? = null
    private var listenAttempt = false
    private var connectAttempt = false

    lateinit var v:View
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v=inflater.inflate(R.layout.fragment_fragment_notification, container, false)

        ActivityCompat.requestPermissions(this.requireActivity(), permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        //Bluetooth
        Log.d("BLUETOOTH", "On create")
        mBluetoothAdapter   = BluetoothAdapter.getDefaultAdapter()
        listView            = v.findViewById(R.id.listViewItems)
        connect             = v.findViewById(R.id.connect)
        listen              = v.findViewById(R.id.listen)
        disconnect          = v.findViewById(R.id.disconnect)
        audio               = v.findViewById(com.example.utilitybox.R.id.audioBtn)

        listenThread        = ListenThread()
        connectThread       = ConnectThread()
        audioClient         = MainConversation()


        audio?.visibility=View.GONE


        // Microphone button pressed/released
        audio?.setOnTouchListener { _, event ->
            val action = event.action
            if (action == MotionEvent.ACTION_DOWN) {
                audioClient?.stopPlaying()
                audioClient?.startRecording()
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                audioClient?.stopRecording()
                audioClient?.startPlaying()
            }
            false
        }

        // Send CONNECT request
        connect?.setOnClickListener(View.OnClickListener {
            System.out.println("Am i clicked")
            //fun onClick(arg0:View){
                System.out.println("Am i")
                Log.d("BLUETOOTH", "Connect button pressed")
                listView?.visibility=ListView.VISIBLE
                connect?.isEnabled

                deviceList = ArrayList<DeviceInfo>()
                pairedDevices=mBluetoothAdapter?.bondedDevices

                if(pairedDevices?.isNotEmpty()!!) {
                    Log.d("BLUETOOTH", "Pair devices > 0")
                    for (device in pairedDevices!!) {
                        val newDevice = DeviceInfo(device.name, device.address)
                        deviceList!!.add(newDevice)
                    }
                }else{
                    Log.d("BLUETOOTH", "No paired devices found")
                }
                // No devices found
                if (deviceList!!.size === 0) {
                    deviceList!!.add(DeviceInfo("No devices found", ""))
                }

                adapter = ArrayAdapter(this.requireContext(), android.R.layout.simple_list_item_1, deviceList)
                listView?.adapter = adapter

            //}
        })

        // Listen for connection requests
        listen?.setOnClickListener(View.OnClickListener() {

            fun onClick(arg0:View) {
                // Handle UI elements - change status
                listen!!.isEnabled = false
                connect!!.isEnabled = false
                listView!!.visibility = ListView.GONE
                // Accept connection
                val connectSuccess:Boolean = listenThread!!.acceptConnect(mBluetoothAdapter, MY_UUID)
                Log.d("BLUETOOTH", "Listen")

                if (connectSuccess) {
                    // Connection successful - get socket object, start listening, change visibility of UI elements
                    bSocket = listenThread?.getSocket()
                    audioClient?.audioCreate()
                    bSocket?.let { it1 -> audioClient?.setSocket(it1) }
                    audioClient?.setupStreams()
                    audioClient?.startPlaying()
                    Toast.makeText(this.requireContext(), "Connection was successful", Toast.LENGTH_LONG).show()
                    audio?.visibility = View.VISIBLE
                    listenAttempt = true
                } else {
                    // Connection Unsuccessful - change visibility of UI elements
                    Toast.makeText(activity, "Connection was unsuccessful", Toast.LENGTH_LONG).show()
                    listen?.isEnabled = true
                    connect?.isEnabled = true
                }
            }
        });

        // Disconnect
        disconnect?.setOnClickListener(View.OnClickListener() {

            fun onClick(arg0:View) {

                var disconnectListen:Boolean = false
                var disconnectConnect:Boolean = false
                // Enable buttons and disable listView
                listen?.isEnabled = true
                connect?.isEnabled = true
                listView?.visibility = ListView.GONE
                // Close the bluetooth socket
                if (listenAttempt) {
                    disconnectListen = listenThread?.closeConnect()!!
                    listenAttempt = false
                }
                if (connectAttempt) {
                    disconnectConnect = connectThread?.closeConnect()!!
                    connectAttempt = false
                }

                audioClient?.destroyProcesses()

                Log.d("BLUETOOTH", "Disconnect")

                if (disconnectListen || disconnectConnect) {
                    // Disconnect successful - Handle UI element change
                    audio?.visibility = View.GONE
                    listen?.isEnabled = true
                    connect?.isEnabled = true
                } else {
                    // Unsuccessful disconnect - Do nothing
                }
            }
        })

        fun onRequestPermissionsResult(requestCode:Int, @NonNull permissions:Array<String>, @NonNull grantResults:IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            when (requestCode) {
                REQUEST_RECORD_AUDIO_PERMISSION ->
                    // Permission granted
                    permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
            }
            if (!permissionToRecordAccepted)
            {
                //finish()
            }
        }

        return v
    }
    companion object {
        // Requesting permission to RECORD_AUDIO
        private val REQUEST_RECORD_AUDIO_PERMISSION = 200
        //Bluetooth parameters
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
    }


}
