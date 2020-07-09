package com.example.utilitybox.helpers

import androidx.fragment.app.Fragment
import android.media.AudioTrack
import android.media.AudioManager
import android.media.AudioRecord
import android.bluetooth.BluetoothSocket
import android.media.AudioFormat
import android.widget.Button
import java.io.InputStream
import java.io.OutputStream
import android.util.Log
import java.io.IOException
import android.media.MediaRecorder

class MainConversation: Fragment(){
    private val audioBtn: Button? = null
    private val context = null
    private var bSocket: BluetoothSocket? = null
    private var recordingThread: Thread? = null
    private var playThread: Thread? = null
    private var recorder: AudioRecord? = null
    private var track: AudioTrack? = null
    private val am: AudioManager? = null
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null
    private var buffer: ByteArray? = null
    private var playBuffer: ByteArray? = null
    var minSize = AudioTrack.getMinBufferSize(16000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
    private val bufferSize = minSize
    private var isRecording = false

    // Record Audio
    fun startRecording() {
        Log.d("AUDIO", "Assigning recorder")
        buffer = ByteArray(bufferSize)

        // Start Recording
        recorder?.startRecording()
        isRecording = true
        // Start a thread
        recordingThread = Thread(Runnable { sendRecording() }, "AudioRecorder Thread")
        recordingThread!!.start()
    }

    // Method for sending Audio
    private fun sendRecording() {
        // Infinite loop until microphone button is released
        while (isRecording) {
            try {
                recorder?.read(buffer, 0, bufferSize)
                outStream?.write(buffer)
            } catch (e: IOException) {
                Log.d("AUDIO", "Error when sending recording")
            }

        }
    }

    // Set input & output streams
    fun setupStreams() {
        try {
            inStream = bSocket?.inputStream
        } catch (e: IOException) {
            Log.e("SOCKET", "Error when creating input stream", e)
        }

        try {
            outStream = bSocket?.outputStream
        } catch (e: IOException) {
            Log.e("SOCKET", "Error when creating output stream", e)
        }

    }
    // Stop Recording and free up resources
    fun stopRecording() {
        if (recorder != null) {
            isRecording = false
            recorder?.stop()
        }
    }

    fun audioCreate() {
        // Audio track object
        track = AudioTrack(
            AudioManager.STREAM_MUSIC,
            16000, AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT, minSize, AudioTrack.MODE_STREAM
        )
        // Audio record object
        recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC, 16000,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }
    // Playback received audio
    fun startPlaying() {
        Log.d("AUDIO", "Assigning player")
        // Receive Buffer
        playBuffer = ByteArray(minSize)

        track?.play()
        // Receive and play audio
        playThread = Thread(Runnable { receiveRecording() }, "AudioTrack Thread")
        playThread?.start()
    }

    // Receive audio and write into audio track object for playback
    private fun receiveRecording() {
        val i = 0
        while (!isRecording)
        {
            try
            {
                if (inStream?.available() === 0)
                {
                    //Do nothing
                }
                else
                {
                    inStream?.read(playBuffer)
                    track?.write(playBuffer,0,playBuffer!!.size)
                }
            }
            catch (e:IOException) {
                Log.d("AUDIO", "Error when receiving recording")
            }
        }
    }
    fun stopPlaying() {
        if (track != null) {
            isRecording = true
            track?.stop()
        }
    }
    fun destroyProcesses() {
        //Release resources for audio objects
        track?.release()
        recorder?.release()
    }

    // Setter for socket object
    fun setSocket(bSocket: BluetoothSocket) {
        this.bSocket = bSocket
    }
}