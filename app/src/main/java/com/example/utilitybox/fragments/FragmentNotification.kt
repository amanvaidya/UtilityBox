package com.example.utilitybox.fragments


import android.Manifest
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.text.SpannableString
import android.text.format.DateFormat
import android.text.method.ScrollingMovementMethod
import android.text.style.ForegroundColorSpan
import android.view.*
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.view.ViewCompat

import com.example.utilitybox.R
import com.example.utilitybox.helpers.AudioPlayer
import com.example.utilitybox.helpers.AudioRecorder
import com.example.utilitybox.helpers.ConnectionsActivity
import com.example.utilitybox.helpers.GestureDetector
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.Strategy
import java.io.IOException
import java.util.*

class FragmentNotification : ConnectionsActivity(){

    /**
     * The state of the app. As the app changes states, the UI will update and advertising/discovery
     * will start/stop.
     */
    /** @return The current state.
     */
    /**
     * The state has changed. I wonder what we'll be doing now.
     *
     * @param state The new state.
     */

    private var state = FragmentNotification.State.UNKNOWN
        set(state) {
            if (this.state == state) {
                logW("State set to $state but already in that state")
                return
            }

            logD("State set to $state")
            val oldState = this.state
            field = state
            onStateChanged(oldState, state)
        }
    /** A random UID used as this device's endpoint name.  */
    /**
     * Queries the phone's contacts for their own profile, and returns their name. Used when
     * connecting to another device.
     */
    var mName:String=""
    override var name: String = ""
        private set

    /**
     * The background color of the 'CONNECTED' state. This is randomly chosen from the [.COLORS]
     * list, based off the authentication token.
     */
    @ColorInt
    private var mConnectedColor = FragmentNotification.COLORS[0]

    /** Displays the previous state during animation transitions.  */
    private var mPreviousStateView: TextView? = null

    /** Displays the current state.  */
    private var mCurrentStateView: TextView? = null

    /** An animator that controls the animation from previous state to current state.  */
    private var mCurrentAnimator: Animator? = null

    /** A running log of debug messages. Only visible when DEBUG=true.  */
    private lateinit var mDebugLogView: TextView

    /** Listens to holding/releasing the volume rocker.  */
    private val mGestureDetector = object : GestureDetector(KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP) {
        override fun onHold() {
            logV("onHold")
            try {
                startRecording()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        override fun onRelease() {
            logV("onRelease")
            stopRecording()
        }
    }

    /** For recording audio as the user speaks.  */
    private var mRecorder: AudioRecorder? = null

    /** For playing audio from other users nearby.  */
    private var mAudioPlayer: AudioPlayer? = null

    /** The phone's original media volume.  */
    private var mOriginalVolume: Int = 0

    lateinit var v: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        v = inflater.inflate(R.layout.fragment_fragment_notification, container, false)

        mPreviousStateView = v.findViewById<TextView>(R.id.previous_state)
        mCurrentStateView = v.findViewById<TextView>(R.id.current_state)
        mDebugLogView = v.findViewById<TextView>(R.id.debug_log)

        mDebugLogView!!.visibility = View.VISIBLE
        mDebugLogView!!.movementMethod = ScrollingMovementMethod()

        mName = generateRandomName()
        System.out.println("i am here")

        v.findViewById<TextView>(R.id.name).text = mName
        return v
    }


    /** @return True if currently playing.
     */
    private val isPlaying: Boolean
        get() = mAudioPlayer != null

    /** @return True if currently streaming from the microphone.
     */
    private val isRecording: Boolean
        get() = mRecorder != null && mRecorder!!.isRecording

    /** {@see ConnectionsActivity#getRequiredPermissions()}  */
    override val requiredPermissions: Array<String>
        get() = join(
            super.requiredPermissions,
            Manifest.permission.RECORD_AUDIO
        )

    /** {@see ConnectionsActivity#getServiceId()}  */
    public override val serviceId: String
        get() = FragmentNotification.SERVICE_ID

    /** {@see ConnectionsActivity#getStrategy()}  */
    public override val strategy: Strategy
        get() = FragmentNotification.STRATEGY

    fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (state == State.CONNECTED && mGestureDetector.onKeyEvent(event)) {
            true
        } else super.getActivity()?.dispatchKeyEvent(event)!!
    }

    override fun onStart() {
        super.onStart()


        // Set the media volume to max.
        requireActivity().volumeControlStream = AudioManager.STREAM_MUSIC
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mOriginalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0
        )

        state = State.SEARCHING
    }

    override fun onStop() {
        // Restore the original volume.
        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mOriginalVolume, 0)
        requireActivity().volumeControlStream = AudioManager.USE_DEFAULT_STREAM_TYPE

        // Stop all audio-related threads
        if (isRecording) {
            stopRecording()
        }
        if (isPlaying) {
            stopPlaying()
        }

        // After our Activity stops, we disconnect from Nearby Connections.
        state = State.UNKNOWN

        if (mCurrentAnimator != null && mCurrentAnimator!!.isRunning) {
            mCurrentAnimator!!.cancel()
        }

        super.onStop()
    }

    fun onBackPressed() {
        if (state == State.CONNECTED) {
            state = State.SEARCHING
            return
        }
        activity?.onBackPressed()
    }

    override fun onEndpointDiscovered(endpoint: ConnectionsActivity.Endpoint) {
        // We found an advertiser!
        stopDiscovering()
        connectToEndpoint(endpoint)
    }

    override fun onConnectionInitiated(endpoint: ConnectionsActivity.Endpoint, connectionInfo: ConnectionInfo) {
        // A connection to another device has been initiated! We'll use the auth token, which is the
        // same on both devices, to pick a color to use when we're connected. This way, users can
        // visually see which device they connected with.
        mConnectedColor = COLORS[connectionInfo.authenticationToken.hashCode() % COLORS.size]

        // We accept the connection immediately.
        acceptConnection(endpoint)
    }

    override fun onEndpointConnected(endpoint: ConnectionsActivity.Endpoint) {
        Toast.makeText(
            context, getString(R.string.toast_connected, endpoint.name), Toast.LENGTH_SHORT
        )
            .show()
        state = State.CONNECTED
    }

    override fun onEndpointDisconnected(endpoint: ConnectionsActivity.Endpoint) {
        Toast.makeText(
            context, getString(R.string.toast_disconnected, endpoint.name), Toast.LENGTH_SHORT
        )
            .show()
        state = State.SEARCHING
    }

    override fun onConnectionFailed(endpoint: ConnectionsActivity.Endpoint) {
        // Let's try someone else.
        if (state == State.SEARCHING) {
            startDiscovering()
        }
    }

    /**
     * State has changed.
     *
     * @param oldState The previous state we were in. Clean up anything related to this state.
     * @param newState The new state we're now in. Prepare the UI for this state.
     */
    private fun onStateChanged(oldState: State, newState: State) {
        if (mCurrentAnimator != null && mCurrentAnimator!!.isRunning) {
            mCurrentAnimator!!.cancel()
        }

        // Update Nearby Connections to the new state.
        when (newState) {
            FragmentNotification.State.SEARCHING -> {
                disconnectFromAllEndpoints()
                startDiscovering()
                startAdvertising()
            }
            FragmentNotification.State.CONNECTED -> {
                stopDiscovering()
                stopAdvertising()
            }
            FragmentNotification.State.UNKNOWN -> stopAllEndpoints()
            else -> {
            }
        }// no-op

        // Update the UI.
        when (oldState) {
            FragmentNotification.State.UNKNOWN ->
                // Unknown is our initial state. Whatever state we move to,
                // we're transitioning forwards.
                transitionForward(oldState, newState)
            FragmentNotification.State.SEARCHING -> when (newState) {
                FragmentNotification.State.UNKNOWN -> transitionBackward(oldState, newState)
                FragmentNotification.State.CONNECTED -> transitionForward(oldState, newState)
                else -> {
                }
            }// no-op
            FragmentNotification.State.CONNECTED ->
                // Connected is our final state. Whatever new state we move to,
                // we're transitioning backwards.
                transitionBackward(oldState, newState)
        }
    }

    /** Transitions from the old state to the new state with an animation implying moving forward.  */
    @UiThread
    private fun transitionForward(oldState: State, newState: State) {
        mPreviousStateView!!.visibility = View.VISIBLE
        mCurrentStateView!!.visibility = View.VISIBLE

        updateTextView(mPreviousStateView, oldState)
        updateTextView(mCurrentStateView, newState)

        if (ViewCompat.isLaidOut(mCurrentStateView!!)) {
            mCurrentAnimator = createAnimator(false /* reverse */)
            mCurrentAnimator!!.addListener(
                object : AnimatorListener() {
                    override fun onAnimationEnd(animator: Animator) {
                        updateTextView(mCurrentStateView, newState)
                    }
                })
            mCurrentAnimator!!.start()
        }
    }

    /** Transitions from the old state to the new state with an animation implying moving backward.  */
    @UiThread
    private fun transitionBackward(oldState: State, newState: State) {
        mPreviousStateView!!.visibility = View.VISIBLE
        mCurrentStateView!!.visibility = View.VISIBLE

        updateTextView(mCurrentStateView, oldState)
        updateTextView(mPreviousStateView, newState)

        if (ViewCompat.isLaidOut(mCurrentStateView!!)) {
            mCurrentAnimator = createAnimator(true /* reverse */)
            mCurrentAnimator!!.addListener(
                object : AnimatorListener() {
                    override fun onAnimationEnd(animator: Animator) {
                        updateTextView(mCurrentStateView, newState)
                    }
                })
            mCurrentAnimator!!.start()
        }
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun createAnimator(reverse: Boolean): Animator {
        val animator: Animator
        if (Build.VERSION.SDK_INT >= 21) {
            val cx = mCurrentStateView!!.measuredWidth / 2
            val cy = mCurrentStateView!!.measuredHeight / 2
            var initialRadius = 0
            var finalRadius = Math.max(mCurrentStateView!!.width, mCurrentStateView!!.height)
            if (reverse) {
                val temp = initialRadius
                initialRadius = finalRadius
                finalRadius = temp
            }
            animator = ViewAnimationUtils.createCircularReveal(
                mCurrentStateView, cx, cy, initialRadius.toFloat(), finalRadius.toFloat()
            )
        } else {
            var initialAlpha = 0f
            var finalAlpha = 1f
            if (reverse) {
                val temp = initialAlpha
                initialAlpha = finalAlpha
                finalAlpha = temp
            }
            mCurrentStateView!!.alpha = initialAlpha
            animator = ObjectAnimator.ofFloat(mCurrentStateView, "alpha", finalAlpha)
        }
        animator.addListener(
            object : AnimatorListener() {
                override fun onAnimationCancel(animator: Animator) {
                    mPreviousStateView!!.visibility = View.GONE
                    mCurrentStateView!!.alpha = 1f
                }

                override fun onAnimationEnd(animator: Animator) {
                    mPreviousStateView!!.visibility = View.GONE
                    mCurrentStateView!!.alpha = 1f
                }
            })
        animator.duration = ANIMATION_DURATION
        return animator
    }

    /** Updates the [TextView] with the correct color/text for the given [State].  */
    @UiThread
    private fun updateTextView(textView: TextView?, state: State) {
        when (state) {
            FragmentNotification.State.SEARCHING -> {
                textView!!.setBackgroundResource(R.color.state_searching)
                textView.setText(R.string.status_searching)
            }
            FragmentNotification.State.CONNECTED -> {
                textView!!.setBackgroundColor(mConnectedColor)
                textView.setText(R.string.status_connected)
            }
            else -> {
                textView!!.setBackgroundResource(R.color.state_unknown)
                textView.setText(R.string.status_unknown)
            }
        }
    }

    /** {@see ConnectionsActivity#onReceive(Endpoint, Payload)}  */
    override fun onReceive(endpoint: ConnectionsActivity.Endpoint?, payload: Payload) {
        if (payload.type == Payload.Type.STREAM) {
            if (mAudioPlayer != null) {
                mAudioPlayer!!.stop()
                mAudioPlayer = null
            }

            val player = object : AudioPlayer(payload.asStream()!!.asInputStream()) {
                @WorkerThread
                override fun onFinish() {
                    activity?.runOnUiThread { mAudioPlayer = null }
                }
            }
            mAudioPlayer = player
            player.start()
        }
    }

    /** Stops all currently streaming audio tracks.  */
    private fun stopPlaying() {
        logV("stopPlaying()")
        if (mAudioPlayer != null) {
            mAudioPlayer!!.stop()
            mAudioPlayer = null
        }
    }

    /** Starts recording sound from the microphone and streaming it to all connected devices.  */
    @Throws(IOException::class)
    private fun startRecording() {
        logV("startRecording()")
        try {
            val payloadPipe = ParcelFileDescriptor.createPipe()

            // Send the first half of the payload (the read side) to Nearby Connections.
            send(Payload.fromStream(payloadPipe[0]))

            // Use the second half of the payload (the write side) in AudioRecorder.
            mRecorder = AudioRecorder(payloadPipe[1])
            mRecorder!!.start()
        } catch (e: IOException) {
            logE("startRecording() failed", e)
        }

    }

    /** Stops streaming sound from the microphone.  */
    private fun stopRecording() {
        logV("stopRecording()")
        if (mRecorder != null) {
            mRecorder!!.stop()
            mRecorder = null
        }
    }

    override fun logV(msg: String) {
        super.logV(msg)
        appendToLogs(toColor(msg, resources.getColor(R.color.log_verbose)))
    }

    override fun logD(msg: String) {
        super.logD(msg)
        appendToLogs(toColor(msg, resources.getColor(R.color.log_debug)))
    }

    override fun logW(msg: String) {
        super.logW(msg)
        appendToLogs(toColor(msg, resources.getColor(R.color.log_warning)))
    }

    override fun logW(msg: String, e: Throwable) {
        super.logW(msg, e)
        appendToLogs(toColor(msg, resources.getColor(R.color.log_warning)))
    }

    override fun logE(msg: String, e: Throwable) {
        super.logE(msg, e)
        appendToLogs(toColor(msg, resources.getColor(R.color.log_error)))
    }

    private fun appendToLogs(msg: CharSequence) {
        mDebugLogView!!.append("\n")
        mDebugLogView!!.append(DateFormat.format("hh:mm", System.currentTimeMillis()).toString() + ": ")
        mDebugLogView!!.append(msg)
    }

    /**
     * Provides an implementation of Animator.AnimatorListener so that we only have to override the
     * method(s) we're interested in.
     */
    private abstract class AnimatorListener : Animator.AnimatorListener {
        override fun onAnimationStart(animator: Animator) {}

        override fun onAnimationEnd(animator: Animator) {}

        override fun onAnimationCancel(animator: Animator) {}

        override fun onAnimationRepeat(animator: Animator) {}
    }

    /** States that the UI goes through.  */
    enum class State {
        UNKNOWN,
        SEARCHING,
        CONNECTED
    }

    companion object {
        /** If true, debug logs are shown on the device.  */
        private val DEBUG = true

        /**
         * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
         * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
         */
        private val STRATEGY = Strategy.P2P_STAR

        /** Length of state change animations.  */
        private val ANIMATION_DURATION: Long = 600

        /**
         * A set of background colors. We'll hash the authentication token we get from connecting to a
         * device to pick a color randomly from this list. Devices with the same background color are
         * talking to each other securely (with 1/COLORS.length chance of collision with another pair of
         * devices).
         */
        @ColorInt
        private val COLORS = intArrayOf(
            -0xbbcca /* red */,
            -0x63d850 /* deep purple */,
            -0xff432c /* teal */,
            -0xb350b0 /* green */,
            -0x5500 /* amber */,
            -0x6800 /* orange */,
            -0x86aab8 /* brown */
        )

        /**
         * This service id lets us find other nearby devices that are interested in the same thing. Our
         * sample does exactly one thing, so we hardcode the ID.
         */
        private const val SERVICE_ID = "com.example.utilitybox.FragmentNotification.SERVICE_ID"

        /** Joins 2 arrays together.  */

        private fun join(a: Array<String>, vararg b: String): Array<String> {
            val join= arrayOf((a.size + b.size).toString())
            System.arraycopy(a, 0, join, 0, a.size)
            System.arraycopy(b, 0, join, a.size, b.size)
            return join
        }

        private fun toColor(msg: String, color: Int): CharSequence {
            val spannable = SpannableString(msg)
            spannable.setSpan(ForegroundColorSpan(color), 0, msg.length, 0)
            return spannable
        }

        private fun generateRandomName(): String {
            System.out.println("im called")
            var name = ""
            val random = Random()
            for (i in 0..4) {
                name += random.nextInt(10)
            }
            System.out.println(name)
            return name
        }
    }
}