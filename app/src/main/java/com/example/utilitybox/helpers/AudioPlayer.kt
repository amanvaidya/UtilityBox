package com.example.utilitybox.helpers

import android.content.ContentValues.TAG
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.os.Process.setThreadPriority
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.IOException
import java.io.InputStream
import java.nio.Buffer

class AudioPlayer (inputStream:InputStream) {
    /** The audio stream we're reading from. */
    private var mInputStream: InputStream? = null
    /**
     * If true, the background thread will continue to loop and play audio. Once false, the thread
     * will shut down.
     */
    @Volatile
    var mAlive: Boolean = false
    /** The background thread recording audio for us.  */
    private var mThread: Thread? = null

    init {
        mInputStream = inputStream
    }

    /** @return True if currently playing.
     */
    fun isPlaying(): Boolean {
        return mAlive
    }
    fun start(){
        mAlive=true
        mThread = object :Thread(){
            @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
            override fun run() {
                setThreadPriority(THREAD_PRIORITY_AUDIO)
                var buffer:Buffer
                //pending from here
            }
        }
    }
}