package io.homeassistant.companion.android.assist

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.AndroidEntryPoint
import io.homeassistant.companion.android.common.R
import io.homeassistant.companion.android.common.data.servers.ServerManager
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AssistPipelineEventType
import io.homeassistant.companion.android.common.data.websocket.impl.entities.AssistPipelineRunStart
import io.homeassistant.companion.android.common.util.AudioRecorder
import io.homeassistant.companion.android.common.util.assistChannel
import io.homeassistant.companion.android.util.ForegroundServiceLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class AssistWakeWordService : Service() {

    companion object {
        private const val TAG = "AssistWakeWordService"
        private const val NOTIFICATION_ID = 277478

        private val LAUNCHER = ForegroundServiceLauncher(AssistWakeWordService::class.java)

        fun start(context: Context) {
            LAUNCHER.startService(context)
        }
    }

    @Inject
    lateinit var serverManager: ServerManager

    @Inject
    lateinit var audioRecorder: AudioRecorder

    private val ioScope = CoroutineScope(Dispatchers.IO + Job())

    private var recorderJob: Job? = null
    private var recorderQueue: MutableList<ByteArray>? = null

    private var binaryHandlerId: Int? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        createNotification(this)
        ioScope.launch { doWork() }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        LAUNCHER.onServiceDestroy(this)
        stopRecording()
        ioScope.cancel()
        super.onDestroy()
    }

    private suspend fun doWork() = withContext(Dispatchers.IO) {
        val shouldRun = true
        if (!shouldRun) {
            return@withContext
        }

        runAssistPipeline()

        while (shouldRun) { }

        Log.d(TAG, "Done listening to Websocket")

        return@withContext
    }

    private fun createNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var notificationChannel =
                NotificationManagerCompat.from(context).getNotificationChannel(assistChannel)
            if (notificationChannel == null) {
                notificationChannel = NotificationChannel(
                    assistChannel,
                    applicationContext.getString(R.string.assist),
                    NotificationManager.IMPORTANCE_LOW
                )
                NotificationManagerCompat.from(context).createNotificationChannel(notificationChannel)
            }
        }

        val notification = NotificationCompat.Builder(applicationContext, assistChannel)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(applicationContext.getString(R.string.assist))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setGroup(assistChannel)
            .build()
        return try {
            LAUNCHER.onServiceCreated(this, NOTIFICATION_ID, notification)
            true
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun runAssistPipeline() {
        Log.d(TAG, "Running Assist pipeline")
        audioRecorder.startRecording()
        setupRecorderQueue()

        var job: Job? = null
        job = ioScope.launch {
            serverManager.webSocketRepository().runAssistPipelineForVoice(
                sampleRate = AudioRecorder.SAMPLE_RATE,
                outputTts = true,
                pipelineId = "01h0qezfdfrjxeaw856zvdefrf",
                conversationId = null,
                wakeWord = true
            )?.collect {
                // Do something
                Log.d(TAG, "Received $it")
                when (it.type) {
                    AssistPipelineEventType.RUN_START -> {
                        val data = (it.data as? AssistPipelineRunStart)?.runnerData
                        binaryHandlerId = data?.get("stt_binary_handler_id") as? Int
                    }
                    AssistPipelineEventType.WAKE_WORD_START -> {
                        ioScope.launch {
                            binaryHandlerId?.let { id ->
                                // Manually loop here to avoid the queue being reset too soon
                                recorderQueue?.forEach { data ->
                                    serverManager.webSocketRepository().sendVoiceData(id, data)
                                }
                            }
                            recorderQueue = null
                        }
                    }
                    AssistPipelineEventType.STT_START -> {
                        stopRecording()
                        startActivity(
                            AssistActivity.newInstance(this@AssistWakeWordService).apply {
                                flags += Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                        )
                        job?.cancel()
                    }
                    AssistPipelineEventType.STT_END -> {
                        stopRecording()
                    }
                    AssistPipelineEventType.RUN_END -> {
                        stopRecording()
                        restartPipeline()
                        job?.cancel()
                    }
                    AssistPipelineEventType.ERROR -> {
                        stopRecording()
                        restartPipeline()
                        job?.cancel()
                    }
                }
            }
        }
    }

    private fun setupRecorderQueue() {
        recorderQueue = mutableListOf()
        recorderJob = ioScope.launch {
            audioRecorder.audioBytes.collect {
                // Log.d(TAG, "Received ${it.size} bytes, queue? ${recorderQueue != null}")
                if (recorderQueue != null) {
                    recorderQueue?.add(it)
                } else {
                    sendVoiceData(it)
                    // Log.d(TAG, "Sending voice data actively")
                }
            }
        }
    }

    private fun sendVoiceData(data: ByteArray) {
        binaryHandlerId?.let {
            ioScope.launch {
                // Launch to prevent blocking the output flow if the network is slow
                serverManager.webSocketRepository().sendVoiceData(it, data)
            }
        }
    }

    private fun stopRecording() {
        audioRecorder.stopRecording()
        recorderJob?.cancel()
        recorderJob = null
        if (binaryHandlerId != null) {
            ioScope.launch {
                recorderQueue?.forEach {
                    sendVoiceData(it)
                }
                recorderQueue = null
                sendVoiceData(byteArrayOf()) // Empty message to indicate end of recording
                binaryHandlerId = null
            }
        } else {
            recorderQueue = null
        }
    }

    private fun restartPipeline() = ioScope.launch {
        delay(500L)
        runAssistPipeline()
    }
}
