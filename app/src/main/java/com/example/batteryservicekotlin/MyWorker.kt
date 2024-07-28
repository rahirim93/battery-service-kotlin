package com.example.batteryservicekotlin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startForegroundService
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.batteryservicekotlin.service.Actions
import com.example.batteryservicekotlin.service.EndlessService
import com.example.batteryservicekotlin.service.ServiceState
import com.example.batteryservicekotlin.service.getServiceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

private const val SAVED_SIZE = "saved size"

class MyWorker(private val context: Context, workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private var pref: SharedPreferences? = null

    override suspend fun doWork(): Result {

        pref = context.getSharedPreferences("Name", Context.MODE_PRIVATE)

        BatteryRepository.initialize(context)

        val size = BatteryRepository.get().getChosenDayUnitsList(startDayMillis(), endDayMillis()).size
        val savedSize = pref?.getInt(SAVED_SIZE, 0)

        val myIntent = Intent(context, MainActivity::class.java)
        val myPendingIntent = PendingIntent.getActivity(context, 0, myIntent, PendingIntent.FLAG_IMMUTABLE)

        if (size == savedSize) {
            val resources = context.resources
            val notification = NotificationCompat
                .Builder(context, "rahirim")
                .setTicker("Hello")
                .setContentIntent(myPendingIntent)
                .setContentText("Сервис остановлен")
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setAutoCancel(true)
                .build()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(0, notification)

            //val intent = Intent(applicationContext, MainActivity::class.java)
            //applicationContext.startActivity(intent)

            actionOnService(Actions.START)

            if(BatteryRepository.get().getTestRestartService()) {
                // Не работает
//                val intent = Intent(context, MainActivity::class.java)
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                GlobalScope.launch() {
//                    context.startActivity(intent)
//                }
                //setAlarm()
            }
        } else {
//            val notification = NotificationCompat
//                .Builder(context, "rahirim")
//                .setTicker("Hello")
//                .setContentText("Было: $savedSize Стало: $size")
//                .setSmallIcon(android.R.drawable.ic_menu_report_image)
//                .setAutoCancel(true)
//                .build()
//
//            val notificationManager = NotificationManagerCompat.from(context)
//            notificationManager.notify(0, notification)
        }

        val editor = pref?.edit()
        editor?.putInt(SAVED_SIZE, size)
        editor?.apply()



        return Result.success()
    }

    fun setAlarm() {
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager

        val timeAlarmInMillis = Calendar.getInstance().timeInMillis + 1000
        val timeAlarmInCalendar = Calendar.getInstance()
        timeAlarmInCalendar.timeInMillis = timeAlarmInMillis

        val alarmClockInfo = AlarmManager.AlarmClockInfo(timeAlarmInCalendar.timeInMillis, getAlarmInfoPendingIntent())

        //val alarmActionPendingIntent = PendingIntent.getBroadcast(context, 0, Intent(context, RestartReceiver::class.java), 0)


        alarmManager.setAlarmClock(alarmClockInfo, getAlarmActionPendingIntent())

    }

    private fun getAlarmInfoPendingIntent(): PendingIntent {
        val alarmInfoIntent = Intent(context, MainActivity::class.java)
        alarmInfoIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 0, alarmInfoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun getAlarmActionPendingIntent(): PendingIntent {
        val alarmInfoIntent = Intent(context, MainActivity::class.java)
        alarmInfoIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        return PendingIntent.getActivity(context, 1, alarmInfoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(context) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(context, EndlessService::class.java).also {
            //it.addFlags(PendingIntent.FLAG_IMMUTABLE)
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //log("Starting the service in >=26 Mode")
                context.startForegroundService(it)
                return
            }

            //log("Starting the service in < 26 Mode")
            context.startService(it)
        }
    }
}