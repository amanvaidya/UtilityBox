package com.example.utilitybox.fragments

import android.app.Dialog
import android.content.IntentFilter
import android.net.sip.SipAudioCall
import android.net.sip.SipManager
import android.net.sip.SipProfile
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import android.widget.ToggleButton
import com.example.utilitybox.broadcast.IncomingCallReceiver
import android.widget.TextView
import android.net.sip.SipException
import android.net.sip.SipRegistrationListener
import android.content.Intent
import android.app.PendingIntent
import android.preference.PreferenceManager
import java.text.ParseException
import android.util.Log
import com.example.utilitybox.R
import android.view.MotionEvent
import android.widget.EditText
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.example.utilitybox.broadcast.SipSettings


open class FragmentNotification : Fragment(), View.OnTouchListener {


    var sipAddress:String?=null
    var manager:SipManager?=null
    var me:SipProfile?=null
    var call:SipAudioCall?=null
    var callReceiver:IncomingCallReceiver?=null
    private val CALL_ADDRESS=1
    private val SET_AUTH_INFO=2
    private val UPDATE_SETTINGS_DIALOG=3
    private val HANG_UP=4

    lateinit var v:View
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v=inflater.inflate(R.layout.fragment_fragment_notification, container, false)

        var pushToTalkButton=v.findViewById(R.id.pushToTalk) as ToggleButton
        pushToTalkButton.setOnTouchListener(this)
        var filter = IntentFilter()
        filter.addAction("android.SipDemo.INCOMING_CALL")
        callReceiver = IncomingCallReceiver()
        this.activity?.registerReceiver(callReceiver, filter)

        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initializeManager()
        return v
    }
    override fun onStart(){
        super.onStart()
        initializeManager()
    }

    override fun onDestroy() {
        super.onDestroy()
        call?.close()
        closeLocalProfile()
        if (callReceiver != null) {
            this.activity?.unregisterReceiver(callReceiver)
        }
    }
    private fun initializeManager() {
        if (manager == null) {
            manager = SipManager.newInstance(this.requireContext())
        }

        initializeLocalProfile()
    }

    private fun initializeLocalProfile() {
        if (manager == null) {
            return
        }

        if (me != null) {
            closeLocalProfile()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(activity?.baseContext)
        val username = prefs.getString("namePref", "")
        val domain = prefs.getString("domainPref", "")
        val password = prefs.getString("passPref", "")

        if (username!!.isEmpty() || domain!!.isEmpty() || password!!.isEmpty()) {
            showDialog(UPDATE_SETTINGS_DIALOG)
            return
        }

        try {
            val builder = SipProfile.Builder(username, domain)
            builder.setPassword(password)
            me = builder.build()

            val i = Intent()
            i.action = "android.SipDemo.INCOMING_CALL"
            val pi = PendingIntent.getBroadcast(this.requireContext(), 0, i, Intent.FILL_IN_DATA)
            manager!!.open(me, pi, null)


            // This listener must be added AFTER manager.open is called,
            // Otherwise the methods aren't guaranteed to fire.

            manager!!.setRegistrationListener(me?.uriString, object : SipRegistrationListener {
                override fun onRegistering(localProfileUri: String) {
                    updateStatus("Registering with SIP Server...")
                }

                override fun onRegistrationDone(localProfileUri: String, expiryTime: Long) {
                    updateStatus("Ready")
                }

                override fun onRegistrationFailed(
                    localProfileUri: String, errorCode: Int,
                    errorMessage: String
                ) {
                    updateStatus("Registration failed.  Please check settings.")
                }
            })
        } catch (pe: ParseException) {
            updateStatus("Connection Error.")
        } catch (se: SipException) {
            updateStatus("Connection error.")
        }

    }

    private fun closeLocalProfile() {
        if (manager == null) {
            return
        }
        try {
            if (me != null) {
                manager!!.close(me!!.uriString)
            }
        } catch (ee: Exception) {
            Log.d("Alert--->", "Failed to close local profile.", ee)
        }

    }



    fun initiateCall() {

        sipAddress?.let { updateStatus(it) }

        try {
            val listener = object : SipAudioCall.Listener() {
                // Much of the client's interaction with the SIP Stack will
                // happen via listeners.  Even making an outgoing call, don't
                // forget to set up a listener to set things up once the call is established.
                override fun onCallEstablished(call: SipAudioCall) {
                    call.startAudio()
                    call.setSpeakerMode(true)
                    call.toggleMute()
                    updateStatus(call)
                }

                override fun onCallEnded(call: SipAudioCall) {
                    updateStatus("Ready.")
                }
            }

            call = manager?.makeAudioCall(me?.uriString, sipAddress, listener, 30)

        } catch (e: Exception) {
            Log.i("Exception--->", "Error when trying to close manager.", e)
            if (me != null) {
                try {
                    manager?.close(me?.uriString)
                } catch (ee: Exception) {
                    Log.i(
                        "Exception-->",
                        "Error when trying to close manager.", ee
                    )
                    ee.printStackTrace()
                }

            }
            if (call != null) {
                call?.close()
            }
        }

    }
    fun updateStatus(status: String) {
        // Be a good citizen.  Make sure UI changes fire on the UI thread.
        activity?.runOnUiThread(Runnable {
            val labelView = v.findViewById(R.id.sipLabel) as TextView
            labelView.text = status
        })
    }
    fun updateStatus(call: SipAudioCall) {
        var useName: String? = call.peerProfile.displayName
        if (useName == null) {
            useName = call.peerProfile.userName
        }
        updateStatus(useName + "@" + call.peerProfile.sipDomain)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (call == null) {
            return false
        } else if (event?.action == MotionEvent.ACTION_DOWN && call != null && call!!.isMuted) {
            call!!.toggleMute()
        } else if (event?.action == MotionEvent.ACTION_UP && !call!!.isMuted) {
            call!!.toggleMute()
        }
        return false
    }

    fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, CALL_ADDRESS, 0, "Call someone")
        menu.add(0, SET_AUTH_INFO, 0, "Edit your SIP Info.")
        menu.add(0, HANG_UP, 0, "End Current Call.")

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            CALL_ADDRESS -> showDialog(CALL_ADDRESS)
            SET_AUTH_INFO -> updatePreferences()
            HANG_UP -> if (call != null) {
                try {
                    call?.endCall()
                } catch (se: SipException) {
                    Log.d(
                        "Err",
                        "Error ending call.", se
                    )
                }

                call?.close()
            }
        }
        return true
    }

    private fun showDialog(id: Int): Dialog? {
        when (id) {
            CALL_ADDRESS -> {

                val factory = LayoutInflater.from(this.requireContext())
                val textBoxView = factory.inflate(R.layout.call_address_dialog, null)
                return AlertDialog.Builder(this.requireContext())
                    .setTitle("Call Someone.")
                    .setView(textBoxView)
                    .setPositiveButton(
                        android.R.string.ok
                    ) { _, _ ->
                        val textField = textBoxView.findViewById(R.id.calladdress_edit) as EditText
                        sipAddress = textField.text.toString()
                        initiateCall()
                    }
                    .setNegativeButton(
                        android.R.string.cancel
                    ) { _, _ ->
                        // Noop.
                    }
                    .create()
            }

            UPDATE_SETTINGS_DIALOG -> return AlertDialog.Builder(this.requireContext())
                .setMessage("Please update your SIP Account Settings.")
                .setPositiveButton(android.R.string.ok
                ) { _, _ -> updatePreferences() }
                .setNegativeButton(
                    android.R.string.cancel
                ) { _, _ ->
                    // Noop.
                }
                .create()
        }
        return null
    }

    private fun updatePreferences() {
        val settingsActivity = Intent(
            context,SipSettings::class.java
        )
        context?.startActivity(settingsActivity)
    }

}
