package com.example.utilitybox.helpers

import android.os.Handler
import android.os.Message

import android.view.KeyEvent
import androidx.annotation.IntDef

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.util.HashSet

class GestureDetector
/**
 * Detects gestures on [KeyEvent]s.
 *
 * @param keyCodes The key codes you're interested in. If more than one is provided, make sure
 * they're mutually exclusive (like Volume Up + Volume Down).
 */
    (vararg keyCodes: Int) {

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                State.HOLD -> onHold()
                State.RELEASE -> onRelease()
            }
        }
    }

    private var mHandledDownAlready: Boolean = false
    private val mKeyCodes = HashSet<Int>()

    /**
     * Current or last derived state of the key. For example, if a key is being held down, it's state
     * will be [State.HOLD].
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(State.UNKNOWN, State.HOLD, State.RELEASE)
    private annotation class State {
        companion object {
            const val UNKNOWN = 0
            const val HOLD = 1
            const val RELEASE = 2
        }
    }

    init {
        for (keyCode in keyCodes) {
            mKeyCodes.add(keyCode)
        }
    }

    /** The key is being held. Override this method to act on the event.  */
    protected fun onHold() {}

    /** The key has been released. Override this method to act on the event.  */
    protected fun onRelease() {}

    /** Processes a key event. Returns true if it consumes the event.  */
    fun onKeyEvent(event: KeyEvent): Boolean {
        if (!mKeyCodes.contains(event.keyCode)) {
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // KeyEvents will call ACTION_DOWN over and over again while held.
                // We only care about the first event, so we can ignore the rest.
                if (mHandledDownAlready) {

                }
                mHandledDownAlready = true
                mHandler.sendEmptyMessage(State.HOLD)
            }
            KeyEvent.ACTION_UP -> {
                mHandledDownAlready = false
                mHandler.sendEmptyMessage(State.RELEASE)
            }
        }

        return true
    }
}