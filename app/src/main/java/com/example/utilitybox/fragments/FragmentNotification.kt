package com.example.utilitybox.fragments

import android.animation.Animator
import android.net.NetworkInfo
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.method.ScrollingMovementMethod
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.Nullable
import androidx.fragment.app.Fragment
import com.example.utilitybox.R
import com.example.utilitybox.helpers.AudioPlayer
import com.example.utilitybox.helpers.AudioRecorder
import com.example.utilitybox.helpers.ConnectionsActivity
import com.example.utilitybox.helpers.GestureDetector
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import java.io.IOException
import java.util.*


class FragmentNotification : Fragment(), ConnectionsActivity{

    /** If true, debug logs are shown on the device. */
    private var DEBUG:Boolean=true

    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    private var STRATEGY: Strategy = Strategy.P2P_STAR

    /** Length of state change animations. */
    private var ANIMATION_DURATION:Long=600

    /**
     * A set of background colors. We'll hash the authentication token we get from connecting to a
     * device to pick a color randomly from this list. Devices with the same background color are
     * talking to each other securely (with 1/COLORS.length chance of collision with another pair of
     * devices).
     */
    @ColorInt
    private var COLORS = intArrayOf(-0xbbcca /* red */, -0x63d850 /* deep purple */, -0xff432c /* teal */, -0xb350b0 /* green */, -0x5500 /* amber */, -0x6800 /* orange */, -0x86aab8 /* brown */)

    /**
     * This service id lets us find other nearby devices that are interested in the same thing. Our
     * sample does exactly one thing, so we hardcode the ID.
     */
    private var SERVICE_ID:String= "com.google.location.nearby.apps.walkietalkie.automatic.SERVICE_ID"

    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    private var mState: State = State.UNKNOWN

    /** A random UID used as this device's endpoint name. */
    private var mName:String=""

    /**
     * The background color of the 'CONNECTED' state. This is randomly chosen from the {@link #COLORS}
     * list, based off the authentication token.
     */
    @ColorInt private var mConnectedColor:Int = COLORS[0]

    /** Displays the previous state during animation transitions.  */
    private var mPreviousStateView: TextView? = null
    /** Displays the previous state during animation transitions. */
    private var mCurrentStateView:TextView?=null

    /** An animator that controls the animation from previous state to current state. */
    @Nullable
    private var mCurrentAnimator: Animator?= null

    /** A running log of debug messages. Only visible when DEBUG=true.  */
    private lateinit var mDebugLogView: TextView

    /** Listens to holding/releasing the volume rocker. */
    private var mGestureDetector = object: GestureDetector(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP) {
        protected override fun onHold() {
            logV("onHold")
            startRecording()
        }
        protected override fun onRelease() {
            logV("onRelease")
            stopRecording()
        }
    }
    /** For recording audio as the user speaks. */
    @Nullable private var mRecorder: AudioRecorder ?= null
    /** For playing audio from other users nearby. */
    @Nullable private var mAudioPlayer: AudioPlayer ?= null
    /** The phone's original media volume. */
    private var mOriginalVolume:Int = 0

    private var v:View?=null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) {

        }
        v = inflater.inflate(R.layout.fragment_fragment_notification, container, false)

        mPreviousStateView  = findViewById(R.id.previous_state)
        mCurrentStateView   = findViewById(R.id.current_state)
        mDebugLogView       = findViewById(R.id.debug_log)

        mDebugLogView.visibility = if (DEBUG) View.VISIBLE else View.GONE
        mDebugLogView.movementMethod = ScrollingMovementMethod()

        mName = generateRandomName()

        (findViewById<TextView>(R.id.name)).text = mName
        //To start from here

        return v
    }





    private fun generateRandomName():String {
        var name = ""
        val random = Random()
        for (i in 0..4)
        {
            name += random.nextInt(10)
        }
        return name
    }

    /** Starts recording sound from the microphone and streaming it to all connected devices. */
    private fun startRecording() {
        logV("startRecording()")
        try
        {
            val payloadPipe = ParcelFileDescriptor.createPipe()
            // Send the first half of the payload (the read side) to Nearby Connections.
            send(Payload.fromStream(payloadPipe[0]))
            // Use the second half of the payload (the write side) in AudioRecorder.
            mRecorder = AudioRecorder(payloadPipe[1])
            mRecorder!!.start()
        }
        catch (e: IOException) {
            logE("startRecording() failed", e)
        }
    }

    /** Stops streaming sound from the microphone. */
    private fun stopRecording() {
        logV("stopRecording()")
        if (mRecorder != null)
        {
            mRecorder!!.stop()
            mRecorder = null
        }
    }

    /** States that the UI goes through.  */
    enum class State {
        UNKNOWN,
        SEARCHING,
        CONNECTED
    }
}
