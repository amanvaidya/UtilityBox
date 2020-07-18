package com.example.utilitybox.helpers

import android.content.ContentValues.TAG
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process.THREAD_PRIORITY_AUDIO
import android.os.ParcelFileDescriptor
import android.os.Process.setThreadPriority
import android.util.Log
import java.io.IOException
import java.io.OutputStream


class AudioRecorder
/**
 * A simple audio recorder.
 *
 * @param file The output stream of the recording.
 */
    (file: ParcelFileDescriptor) {
    /** The stream to write to.  */
    private val mOutputStream: OutputStream

    /**
     * If true, the background thread will continue to loop and record audio. Once false, the thread
     * will shut down.
     */
    /** @return True if actively recording. False otherwise.
     */
    @Volatile
    var isRecording: Boolean = false
        private set

    /** The background thread recording audio for us.  */
    private var mThread: Thread? = null

    init {
        mOutputStream = ParcelFileDescriptor.AutoCloseOutputStream(file)
    }

    /** Starts recording audio.  */
    fun start() {
        if (isRecording) {
            Log.w(TAG, "Already running")
            return
        }

        isRecording = true
        mThread = object : Thread() {
            override fun run() {
                setThreadPriority(THREAD_PRIORITY_AUDIO)

                val buffer = Buffer()
                val record = AudioRecord(
                    MediaRecorder.AudioSource.DEFAULT,
                    buffer.sampleRate,
                    CHANNEL_IN_MONO,
                    ENCODING_PCM_16BIT,
                    buffer.size
                )

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.w(TAG, "Failed to start recording")
                    isRecording = false
                    return
                }

                record.startRecording()

                // While we're running, we'll read the bytes from the AudioRecord and write them
                // to our output stream.
                try {
                    while (isRecording) {
                        val len = record.read(buffer.data, 0, buffer.size)
                        if (len >= 0 && len <= buffer.size) {
                            mOutputStream.write(buffer.data, 0, len)
                            mOutputStream.flush()
                        } else {
                            Log.w(TAG, "Unexpected length returned: $len")
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Exception with recording stream", e)
                } finally {
                    stopInternal()
                    try {
                        record.stop()
                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "Failed to stop AudioRecord", e)
                    }

                    record.release()
                }
            }
        }
        mThread!!.start()
    }

    private fun stopInternal() {
        isRecording = false
        try {
            mOutputStream.close()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to close output stream", e)
        }

    }

    /** Stops recording audio.  */
    fun stop() {
        stopInternal()
        try {
            mThread!!.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while joining AudioRecorder thread", e)
            Thread.currentThread().interrupt()
        }

    }

    private class Buffer : AudioBuffer() {
        override fun validSize(size: Int): Boolean {
            return size != AudioRecord.ERROR && size != AudioRecord.ERROR_BAD_VALUE
        }

        override fun getMinBufferSize(sampleRate: Int): Int {
            return AudioRecord.getMinBufferSize(
                sampleRate, CHANNEL_IN_MONO, ENCODING_PCM_16BIT
            )
        }
    }
}