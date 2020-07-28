package com.example.utilitybox.helpers

import android.Manifest
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.CallSuper
import com.example.utilitybox.R
import com.google.android.gms.nearby.Nearby

import androidx.core.app.ActivityCompat
import android.os.Build
import com.google.android.gms.auth.api.signin.GoogleSignIn.hasPermissions
import android.widget.Toast
import android.content.pm.PackageManager
import androidx.annotation.NonNull
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

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
    private val mDiscoveredEndpoints: HashMap<String, EndpointDiscoveryCallback> =
        HashMap<String, EndpointDiscoveryCallback>()
    private val mEstablishedConnections: HashMap<String, EndpointDiscoveryCallback> =
        HashMap<String, EndpointDiscoveryCallback>()
    private val mPendingConnections: HashMap<String, EndpointDiscoveryCallback> =
        HashMap<String, EndpointDiscoveryCallback>()
    var mIsConnecting: Boolean = false
    var mIsDiscovering: Boolean = false
    var mIsAdvertising: Boolean = false


    /**
     * Sets the device to discovery mode. It will now listen for devices in advertising mode. Either
     * {@link #onDiscoveryStarted()} or {@link #onDiscoveryFailed()} will be called once we've found
     * out if we successfully entered this mode.
     */
    protected fun startDiscovering() {
        mIsDiscovering = true
        mDiscoveredEndpoints.clear()
        val discoveryOptions = DiscoveryOptions.Builder()
        discoveryOptions.setStrategy(getStrategy())
        mConnectionsClient?.startDiscovery(
                getServiceId(),
                object:EndpointDiscoveryCallback() {
                    override fun onEndpointFound(endpointId:String, info:DiscoveredEndpointInfo) {
                        logD(
                            String.format(
                                "onEndpointFound(endpointId=%s, serviceId=%s, endpointName=%s)",
                                endpointId, info.serviceId, info.endpointName
                            ))
                        if (getServiceId() == info.serviceId)
                        {
                            val endpoint = Endpoint(endpointId, info.endpointName)
                            mDiscoveredEndpoints[endpointId] = endpoint
                            onEndpointDiscovered(endpoint)
                        }
                    }
                    override fun onEndpointLost(endpointId:String) {
                        logD(String.format("onEndpointLost(endpointId=%s)", endpointId))
                    }
                },
                discoveryOptions.build())?.addOnSuccessListener { onDiscoveryStarted() }
            ?.addOnFailureListener { e ->
                mIsDiscovering = false
                logW("startDiscovering() failed.", e)
                onDiscoveryFailed()
            }
        }

        /** Stops discovery. */
        protected fun stopDiscovering() {
            mIsDiscovering = false
            mConnectionsClient?.stopDiscovery()
        }
        /** Returns {@code true} if currently discovering. */
        protected fun isDiscovering():Boolean {
            return mIsDiscovering
        }
        /** Called when discovery successfully starts. Override this method to act on the event. */
        protected fun onDiscoveryStarted() {}
        /** Called when discovery fails to start. Override this method to act on the event. */
        protected fun onDiscoveryFailed() {}
        /**
         * Called when a remote endpoint is discovered. To connect to the device, call {@link
         * #connectToEndpoint(Endpoint)}.
         */
        protected fun onEndpointDiscovered(endpoint:Endpoint) {}
        /** Disconnects from the given endpoint. */
        protected fun disconnect(endpoint:Endpoint) {
            mConnectionsClient?.disconnectFromEndpoint(endpoint.getId())
            mEstablishedConnections.remove(endpoint.getId())
        }

    /** Disconnects from all currently connected endpoints. */
    protected fun disconnectFromAllEndpoints() {
        for (endpoint in mEstablishedConnections.values)
        {
            mConnectionsClient?.disconnectFromEndpoint(endpoint.getId())
        }
        mEstablishedConnections.clear()
    }
    /** Resets and clears all state in Nearby Connections. */
    protected fun stopAllEndpoints() {
        mConnectionsClient?.stopAllEndpoints()
        mIsAdvertising = false
        mIsDiscovering = false
        mIsConnecting = false
        mDiscoveredEndpoints.clear()
        mPendingConnections.clear()
        mEstablishedConnections.clear()
    }
    /**
     * Sends a connection request to the endpoint. Either {@link #onConnectionInitiated(Endpoint,
     * ConnectionInfo)} or {@link #onConnectionFailed(Endpoint)} will be called once we've found out
     * if we successfully reached the device.
     */
    protected fun connectToEndpoint(endpoint:Endpoint) {
        logV("Sending a connection request to endpoint $endpoint")
        // Mark ourselves as connecting so we don't connect multiple times
        mIsConnecting = true
        // Ask to connect
        mConnectionsClient
            ?.requestConnection(getName(), endpoint.getId(), mConnectionLifecycleCallback)
            ?.addOnFailureListener { e ->
                logW("requestConnection() failed.", e)
                mIsConnecting = false
                onConnectionFailed(endpoint)
            }
    }
    /** Returns {@code true} if we're currently attempting to connect to another device. */
    protected fun isConnecting():Boolean {
        return mIsConnecting
    }
    private fun connectedToEndpoint(endpoint:Endpoint) {
        logD(String.format("connectedToEndpoint(endpoint=%s)", endpoint))
        mEstablishedConnections[endpoint.getId()] = endpoint
        onEndpointConnected(endpoint)
    }
    private fun disconnectedFromEndpoint(endpoint:Endpoint) {
        logD(String.format("disconnectedFromEndpoint(endpoint=%s)", endpoint))
        mEstablishedConnections.remove(endpoint.getId())
        onEndpointDisconnected(endpoint)
    }

    /**
     * Called when a connection with this endpoint has failed. Override this method to act on the
     * event.
     */
    protected fun onConnectionFailed(endpoint:Endpoint) {}
    /** Called when someone has connected to us. Override this method to act on the event. */
    protected fun onEndpointConnected(endpoint:Endpoint) {}
    /** Called when someone has disconnected. Override this method to act on the event. */
    protected fun onEndpointDisconnected(endpoint:Endpoint) {}
    /** Returns a list of currently connected endpoints. */
    protected fun getDiscoveredEndpoints(): HashSet<EndpointDiscoveryCallback> {
        return HashSet(mDiscoveredEndpoints.values)
    }
    /** Returns a list of currently connected endpoints. */
    protected fun getConnectedEndpoints(): HashSet<EndpointDiscoveryCallback> {
        return HashSet(mEstablishedConnections.values)
    }
    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     *
     * @param payload The data you want to send.
     */
    protected fun send(payload:Payload) {
        send(payload, mEstablishedConnections.keySet())
    }
    private fun send(payload:Payload, endpoints:Set<String>) {
        mConnectionsClient
            ?.sendPayload(ArrayList(endpoints), payload)
            ?.addOnFailureListener { e -> logW("sendPayload() failed.", e) }
    }
   /**
     * Someone connected to us has sent us data. Override this method to act on the event.
     *
     * @param endpoint The sender.
     * @param payload The data.
     */
    protected fun onReceive(endpoint:Endpoint, payload:Payload) {}
    /**
     * An optional hook to pool any permissions the app needs with the permissions ConnectionsActivity
     * will request.
     *
     * @return All permissions required for the app to properly function.
     */
    protected fun getRequiredPermissions():Array<String> {
        return REQUIRED_PERMISSIONS
    }
    /** Returns the client's name. Visible to others when connecting. */
    protected abstract fun getName():String
    /**
     * Returns the service id. This represents the action this connection is for. When discovering,
     * we'll verify that the advertiser has the same service id before we consider connecting to them.
     */
    protected abstract fun getServiceId():String
    /**
     * Returns the strategy we use to connect to other devices. Only devices using the same strategy
     * and service id will appear when discovering. Stragies determine how many incoming and outgoing
     * connections are possible at the same time, as well as how much bandwidth is available for use.
     */
    protected abstract fun getStrategy():Strategy
    /**
     * Transforms a {@link Status} into a English-readable message for logging.
     *
     * @param status The current status
     * @return A readable String. eg. [404]File not found.
     */
    private fun toString(status: Status):String {
        return String.format(
            Locale.US,
            "[%d]%s",
            status.statusCode,
            if (status.statusMessage != null)
                status.statusMessage
            else
                ConnectionsStatusCodes.getStatusCodeString(status.statusCode))
    }
    /**
     * Returns {@code true} if the app was granted all the permissions. Otherwise, returns {@code
     * false}.
     */
    fun hasPermissions(context: Context, vararg permissions:String):Boolean {
        for (permission in permissions)
        {
            if ((ContextCompat.checkSelfPermission(context, permission) !== PackageManager.PERMISSION_GRANTED))
            {
                return false
            }
        }
        return true
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

    /** Represents a device we can talk to. */
    protected class Endpoint(@NonNull id:String, @NonNull name:String) {
        @NonNull @get:NonNull
        val id:String = id
        @NonNull @get:NonNull
        val name:String = name

        public override fun equals(obj: Any?): Boolean {
            if (obj is Endpoint)
            {
                val other = obj as Endpoint
                return id == other.id
            }
            return false
        }
        public override fun hashCode():Int {
            return id.hashCode()
        }
        public override fun toString():String {
            return String.format("Endpoint{id=%s, name=%s}", id, name)
        }
    }

}
