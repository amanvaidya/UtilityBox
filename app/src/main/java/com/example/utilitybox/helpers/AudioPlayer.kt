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

open class AudioPlayer/**
 * A simple audio player.
 *
 * @param inputStream The input stream of the recording.
 */
    (inputStream:InputStream) {
    /** The audio stream we're reading from. */
    private val mInputStream:InputStream = inputStream
    /**
     * If true, the background thread will continue to loop and play audio. Once false, the thread
     * will shut down.
     */
    /** @return True if currently playing. */
    @Volatile var isPlaying:Boolean = false
    /** The background thread recording audio for us. */
    private var mThread: Thread? =null

    /** Starts playing the stream. */
    fun start() {
        isPlaying = true
        mThread = object:Thread() {
            public override fun run() {
                setThreadPriority(THREAD_PRIORITY_AUDIO)
                val buffer = Buffer()
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    buffer.sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    buffer.size,
                    AudioTrack.MODE_STREAM)
                audioTrack.play()
                var len:Int=0
                var bData:Int=mInputStream.read(buffer.data)
                try
                {
                    while ((isPlaying && (len == bData)) !=null)
                    {
                        audioTrack.write(buffer.data, 0, len)
                    }
                }
                catch (e:IOException) {
                    Log.e(TAG, "Exception with playing stream", e)
                }
                finally
                {
                    stopInternal()
                    audioTrack.release()
                    onFinish()
                }
            }
        }
        (mThread as Thread).start()
    }
    private fun stopInternal() {
        isPlaying = false
        try
        {
            mInputStream.close()
        }
        catch (e:IOException) {
            Log.e(TAG, "Failed to close input stream", e)
        }
    }
    /** Stops playing the stream. */
    fun stop() {
        stopInternal()
        try
        {
            mThread?.join()
        }
        catch (e:InterruptedException) {
            Log.e(TAG, "Interrupted while joining AudioRecorder thread", e)
            Thread.currentThread().interrupt()
        }
    }
    /** The stream has now ended. */
    protected fun onFinish() {}
    private class Buffer:AudioBuffer() {
        override fun validSize(size:Int):Boolean {
            return size != AudioTrack.ERROR && size != AudioTrack.ERROR_BAD_VALUE
        }

        override fun getMinBufferSize(sampleRate:Int):Int {
            return AudioTrack.getMinBufferSize(
                sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        }
    }
}