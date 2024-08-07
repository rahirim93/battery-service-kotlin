package com.example.batteryservicekotlin.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.*
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.lifecycle.ViewModelProviders
import com.example.batteryservicekotlin.BatteryRepository
import com.example.batteryservicekotlin.MainActivity
import com.example.batteryservicekotlin.R
import com.example.batteryservicekotlin.database.Unit
import com.example.batteryservicekotlin.listFragment.ListViewModel
import com.example.batteryservicekotlin.listFragment.getMustNumberInsertsToday
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

private const val TAG = "MyTag"

class EndlessService : Service() {

    // Счетчик времени работы сервиса без запуска приложения
    private var counterTimeWork: Double = 0.0

    private var counter = 5
    private lateinit var builder: Notification.Builder
    private lateinit var notificationManager: NotificationManager

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    private val batteryRepository = BatteryRepository.get()

    override fun onBind(intent: Intent): IBinder? {
        log("Some component want to bind with the service")
        // We don't provide binding, so return null
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("onStartCommand executed with startId: $startId")
        if (intent != null) {
            val action = intent.action
            log("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> log("This should never happen. No action in the received intent")
            }
        } else {
            log(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        log("The service has been created".uppercase(Locale.getDefault()))
        builder = createNotification()
        val notification = createNotification().build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("The service has been destroyed".uppercase(Locale.getDefault()))
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
        builder.setContentText("Service was destroyed")
        notificationManager.notify(1, builder.build())
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent = Intent(applicationContext, EndlessService::class.java).also {
            it.setPackage(packageName)
        }
        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(this, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
        applicationContext.getSystemService(Context.ALARM_SERVICE)
        val alarmService: AlarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000, restartServicePendingIntent)
    }

    private fun startService() {
        counterTimeWork = 0.0
        if (isServiceStarted) return
        log("Starting the foreground service task")
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EndlessService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        GlobalScope.launch(Dispatchers.IO) {
            while (isServiceStarted) {
                launch(Dispatchers.IO) {
                    addUnit()
                    updateNotification()
                    counterTimeWork += 5
                }
                delay(10000)
            }
            log("End of the loop for the service")
        }
    }

    private fun updateNotification(){
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val currentNow = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        if (counter == 1) {
            counter = 0
        } else {
            counter = 1
        }
        val size = BatteryRepository.get().getUnits().value?.size
        val string = "Мгновенный ток: ${currentNow} мкА \n" +
                "Напряжение: ${(voltage?.div(1000.0))} В \n" +
                "Температура: ${temperature?.div(10.0)} \u2103 \n" +
                "Признак работы: $counter \n" +
                "Автономное время работы: ${String.format("%.3f", counterTimeWork / 3600.0)} ч"
        builder.style = Notification.BigTextStyle().bigText(string)
        notificationManager.notify(1, builder.build())
    }

    // Функция добавления ТМИ в БД
    private fun addUnit() {

        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager

        //Оставшаяся емкость аккумулятора в виде целого процента от общей емкости (без дробной части).
        val capacityInPercentage = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        //Log.d(TAG, "Capacity: $capacityInPercentage") // Чтобы видеть при закрытом приложении в логе что ТМИ пишется

        // Емкость аккумулятора в микроампер-часах, как целое число.
        val capacityInMicroampereHours = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        // Средний ток батареи в микроамперах, как целое число.
        // То ли в infinix, то ли в android 13 в значение выдается миллиамперах
        val currentAverage = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)

        // Мгновенный ток батареи в микроамперах, как целое число.

        val currentNow = batteryManager
            .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)

        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus = registerReceiver(null, intentFilter)

        // Дополнительно за намерение.ACTION_BATTERY_CHANGED: целое число, содержащее текущую температуру батареи.
        val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)

        // Дополнительно за намерение.ACTION_BATTERY_CHANGED: целое число, содержащее текущий уровень напряжения батареи.
        val voltage = batteryStatus?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)

        // Заполнение объекта сущности ТМИ
        var unit = Unit()
        unit.capacityInMicroampereHours = capacityInMicroampereHours
        unit.capacityInPercentage = capacityInPercentage
        unit.currentAverage = currentAverage
        unit.currentNow = currentNow
        unit.temperature = temperature
        unit.voltage = voltage

        // Добавление ТМИ в БД
        batteryRepository.addUnit(unit)
    }

    private fun stopService() {
        log("Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            log("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun createNotification(): Notification.Builder {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("Endless Service")
            .setContentText("This is your favorite endless service working")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
    }
}