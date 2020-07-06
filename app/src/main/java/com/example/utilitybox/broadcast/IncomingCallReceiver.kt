package com.example.utilitybox.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.sip.SipAudioCall
import android.net.sip.SipProfile
import com.example.utilitybox.fragments.FragmentNotification


class IncomingCallReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        var incomingCall: SipAudioCall?=null
        try{
            val listener = object : SipAudioCall.Listener() {
                override fun onRinging(call: SipAudioCall, caller: SipProfile) {
                    try {
                        call.answerCall(30)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }
            val wtActivity = context as FragmentNotification

            incomingCall = wtActivity.manager?.takeAudioCall(intent, listener)
            incomingCall?.answerCall(30)
            incomingCall?.startAudio()
            incomingCall?.setSpeakerMode(true)
            if (incomingCall?.isMuted!!) {
                incomingCall?.toggleMute()
            }

            wtActivity.call = incomingCall

            wtActivity.updateStatus(incomingCall)
        }catch (e:Exception){
            System.out.println("Err-->"+e)
            if (incomingCall != null) {
                incomingCall.close();
            }
        }
    }
}