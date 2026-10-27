package com.example.kingdomguardsassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Local planner only. This service DOES NOT click the game or send attacks.
 * It keeps a foreground notification and logs planned steps for safe testing.
 */
class PlannerService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopPlanner()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, notification("Planner running — test mode"))
                if (job?.isActive != true) runPlanner()
            }
        }
        return START_NOT_STICKY
    }

    private fun runPlanner() {
        job = serviceScope.launch {
            var completed = 0
            while (isActive) {
                val targets = PlanStore.loadTargets(this@PlannerService)
                val max = PlanStore.maxAttacks(this@PlannerService).coerceIn(1, 100)
                if (targets.isEmpty()) {
                    updateNotification("Stopped: no targets configured")
                    stopPlanner()
                    stopSelf()
                    break
                }
                if (completed >= max) {
                    updateNotification("Test plan complete: $completed steps")
                    stopPlanner()
                    stopSelf()
                    break
                }

                val target = targets[completed % targets.size]
                // Safe dry-run: log intent only, no interaction with Kingdom Guards.
                Log.i("KGAssistant", "DRY RUN step ${completed + 1}: ${target.alliance} / ${target.tower} / ${target.side}")
                completed++
                updateNotification("Test mode: step $completed / $max")
                val interval = PlanStore.interval(this@PlannerService).coerceIn(30, 3600)
                delay(interval * 1000L)
            }
        }
    }

    private fun stopPlanner() {
        job?.cancel()
        job = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "KG Assistant planner", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Shows local planner status" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun notification(text: String): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder.setContentTitle("Kingdom Guards Assistant")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(text))
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.kingdomguardsassistant.START"
        const val ACTION_STOP = "com.example.kingdomguardsassistant.STOP"
        private const val CHANNEL_ID = "kg_planner"
        private const val NOTIFICATION_ID = 4102
    }
}
