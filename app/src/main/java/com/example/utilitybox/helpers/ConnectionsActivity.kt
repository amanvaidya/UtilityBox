package com.example.utilitybox.helpers

import android.Manifest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.CallSuper
import com.example.utilitybox.R

import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback

import java.util.HashMap





abstract class ConnectionsActivity : AppCompatActivity() {


    private val REQUIRED_PERMISSIONS = arrayOf<String>(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private var REQUEST_CODE_REQUIRED_PERMISSIONS = 1
    private var mConnectionsClient: ConnectionsClient? = null
    private val mDiscoveredEndpoints:HashMap<String, EndpointDiscoveryCallback> = HashMap<String, EndpointDiscoveryCallback>()
    private val mEstablishedConnections:HashMap<String, EndpointDiscoveryCallback> = HashMap<String, EndpointDiscoveryCallback>()
    var mIsConnecting:Boolean=false
    var mIsDiscovering:Boolean=false
    var mIsAdvertising:Boolean=false


    private val mConnectionLifecycleCallback = object:ConnectionLifecycleCallback() {

        override fun onConnectionResult(p0: String, p1: ConnectionResolution) {

        }

        override fun onDisconnected(p0: String) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onConnectionInitiated(p0: String, p1: ConnectionInfo) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.example.utilitybox.R.layout.activity_connections)
    }


    @CallSuper
    protected fun logV(msg:String) {
        Log.v("WalkieTalkie", msg)
    }
    @CallSuper
    protected fun logD(msg:String) {
        Log.d("WalkieTalkie", msg)
    }
    @CallSuper
    protected fun logW(msg:String) {
        Log.w("WalkieTalkie", msg)
    }
    @CallSuper
    protected fun logW(msg:String, e:Throwable) {
        Log.w("WalkieTalkie", msg, e)
    }
    @CallSuper
    protected fun logE(msg:String, e:Throwable) {
        Log.e("WalkieTalkie", msg, e)
    }
}
