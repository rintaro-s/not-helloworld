package com.lorinta.helloworld

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import android.os.Looper
import okhttp3.*
import java.io.IOException
import javax.mail.Authenticator
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import com.lorinta.helloworld.MainActivity
import java.util.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var targetLocation: Location? = null

    val lineNotifyToken = "VqaiWrOso0aMtn2bnjEnIkLmMrLWEfhaq7p4jugSIWd" // LINE Notifyトークン

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // 「service開始！」をLINE Notifyに送信
        sendLineNotify("service開始！")

        targetLocation = intent.getParcelableExtra("targetLocation")
        if (targetLocation == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundService()

        val locationRequest = LocationRequest.Builder(30000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(15000)
            .build()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

        return START_STICKY
    }

    private fun startForegroundService() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = "location_service_channel"
        val channelName = "Location Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Location Monitoring")
            .setContentText("The app is monitoring your location.")
            .setSmallIcon(R.drawable.ic_location)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(1, notification)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val currentLocation = locationResult.lastLocation
            if (currentLocation != null && targetLocation != null) {
                val distance = currentLocation.distanceTo(targetLocation!!)
                Log.d("LocationService", "Distance to target: $distance meters")
                if (distance <= 530) {
                    sendEmail(distance)
                    sendLineNotify("come!")
                } else {
                    sendLineNotify(distance.toString())
                }
            } else {
                Log.e("LocationService", "Current or target location is null.")
                sendLineNotify("Current or target location is null.")
            }
        }
    }

    private fun sendEmail(distance: Float) {
        val recipient = "rin.c.mint@gmail.com"
        val subject = "Alert!!"
        val messageBody = "You are within $distance meters of the target location."

        val username = "lorinta.dev@gmail.com"
        val password = "wfxa ptmc edok ukel"

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(username, password)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(username))
                setRecipients(javax.mail.Message.RecipientType.TO, InternetAddress.parse(recipient))
                this.subject = subject
                setText(messageBody)
            }

            Transport.send(message)
            Log.d("LocationService", "Email sent successfully")

        } catch (e: javax.mail.MessagingException) {
            Log.e("LocationService", "Error sending email", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun sendLineNotify(message: String) {
        val client = OkHttpClient()
        val requestBody = FormBody.Builder()
            .add("message", message)
            .build()

        val request = Request.Builder()
            .url("https://notify-api.line.me/api/notify")
            .addHeader("Authorization", "Bearer $lineNotifyToken")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // 成功時の処理があればここに記述
                }
            }
        })
    }
}
