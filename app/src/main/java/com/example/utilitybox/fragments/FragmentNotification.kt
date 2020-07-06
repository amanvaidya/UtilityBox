package com.example.utilitybox.fragments

import android.content.IntentFilter
import android.net.sip.SipAudioCall
import android.net.sip.SipManager
import android.net.sip.SipProfile
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ToggleButton

import com.example.utilitybox.R
import com.example.utilitybox.broadcast.IncomingCallReceiver


class FragmentNotification : Fragment(), View.OnTouchListener {

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
        this.registerReceiver(callReceiver, filter)

        return v
    }


}
