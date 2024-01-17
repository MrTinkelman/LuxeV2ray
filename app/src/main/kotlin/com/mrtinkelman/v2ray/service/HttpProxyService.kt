package com.mrtinkelman.v2ray.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.mrtinkelman.v2ray.AppConfig
import com.mrtinkelman.v2ray.R
import com.mrtinkelman.v2ray.ui.MainActivity
import com.mrtinkelman.v2ray.ui.VPNHotspot
import com.mrtinkelman.v2ray.util.HttpProxy
import rx.Subscription

class HttpProxyService : Service() {
    private lateinit var wakeLock: PowerManager.WakeLock
    private var port: Int = 1081
    private var httpProxy: HttpProxy? = null
    private var mBuilder: NotificationCompat.Builder? = null
    private var mNotificationManager: NotificationManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private val NOTIFICATION_ID = 123325
    private val NOTIFICATION_PENDING_INTENT_CONTENT = 0

    override fun onCreate() {
        super.onCreate()
        showNotification()
        // Acquire a partial WakeLock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HttpProxyService:WakeLock")
        wakeLock.acquire()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startHttpProxy(intent)
        return START_STICKY
    }

    private fun startHttpProxy(intent: Intent?) {
        val port = intent?.getIntExtra("port", 1081) ?: 1081
        if (httpProxy != null) {
            if (port != this.port) {
                httpProxy?.stop()
            } else if (httpProxy!!.started) {
                return
            }
        }
        this.port = port
        httpProxy = HttpProxy.start(port)
        httpProxy!!.messageListener = {
            handler.post {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        httpProxy?.stop()
        httpProxy = null
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
    }
    private fun showNotification() {

        val startMainIntent = Intent(this, VPNHotspot::class.java)
        val contentPendingIntent = PendingIntent.getActivity(this,
            NOTIFICATION_PENDING_INTENT_CONTENT, startMainIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            })

        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            } else {
                // If earlier version channel ID is not used
                // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                ""
            }

        mBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.hotspo)
            .setContentTitle("Http proxy server")
            .setContentText("VPNHotspot is running...")
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(contentPendingIntent)


       startForeground(NOTIFICATION_ID, mBuilder?.build())
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "jhjghjhgjjgghjhgjh"
        val channelName = "VPNHotspot Background Service"
        val chan = NotificationChannel(channelId,
            channelName, NotificationManager.IMPORTANCE_HIGH)
        chan.lightColor = Color.DKGRAY
        chan.importance = NotificationManager.IMPORTANCE_NONE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        getNotificationManager()?.createNotificationChannel(chan)
        return channelId
    }

   

 
    private fun getNotificationManager(): NotificationManager? {
        if (mNotificationManager == null) {
             mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return mNotificationManager
    }
    companion object {
        fun newStartIntent(context: Context, port: Int): Intent {
            val intent = Intent(context, HttpProxyService::class.java)
            intent.putExtra("port", port)
            return intent
        }

        fun newStopIntent(context: Context): Intent {
            return Intent(context, HttpProxyService::class.java)
        }
    }
}