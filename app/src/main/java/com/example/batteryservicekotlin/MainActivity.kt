package com.example.batteryservicekotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProviders
import com.example.batteryservicekotlin.database.Unit
import com.example.batteryservicekotlin.service.*
import java.util.*

private const val TAG = "MyTag2"

class MainActivity : AppCompatActivity() {
    //Кнопки запуска и остановки сервиса
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var buttonTest: Button

    private var startDay: Long = 0
    private var endDay: Long = 0

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()

        // Запуск службы при запуске приложения
        actionOnService(Actions.START)

        test()

        // Создание и назначение обсервера
        val batteryObserver = Observer<List<Unit>> { units ->
            Log.d(TAG, "Количество записей: ${units.size}")
        }
        mainViewModel.todayUnitsLiveData(startDay, endDay).observe(this, batteryObserver)

        //val todayUnits = viewModel.todayUnitsLiveData(startDay, endDay)
        //Log.d(TAG, "Количество записей: ${mainViewModel.todayUnitsLiveData(startDay, endDay).value?.size}")
    }

    private fun test2() {
        //Log.d(TAG, "Количество записей: ${mainViewModel.todayUnitsLiveData(startDay, endDay).value?.size}")
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            log("Starting the service in < 26 Mode")
            startService(it)
        }
    }

    private fun init() {
        startButton = findViewById(R.id.button_start)
        startButton.setOnClickListener {
            actionOnService(Actions.START)
        }
        stopButton = findViewById(R.id.button_stop)
        stopButton.setOnClickListener {
            actionOnService(Actions.STOP)
        }
        buttonTest = findViewById(R.id.button1)
        buttonTest.setOnClickListener {
            test2()
        }
    }

    private fun test() {
        val todayDay = Calendar.getInstance()

        // Начало сегодняшнего дня с 00:00:00
        val todayStartTime = Calendar.getInstance()
        todayStartTime.set(
            todayDay.get(Calendar.YEAR),
            todayDay.get(Calendar.MONTH),
            todayDay.get(Calendar.DAY_OF_MONTH),
            0, 0, 0)
        // Конец сегодняшнего дня в 23:59:59
        val todayEndTime = Calendar.getInstance()
        todayEndTime.set(
            todayDay.get(Calendar.YEAR),
            todayDay.get(Calendar.MONTH),
            todayDay.get(Calendar.DAY_OF_MONTH),
            23, 59, 59)

        // Начало сегодня в миллисекундах
        startDay = todayStartTime.timeInMillis
        // Конец сегодня в миллисекундах
        endDay = todayEndTime.timeInMillis


//        Log.d(TAG, "Начало дня:\t ${todayStartTime.timeInMillis}")
//        Log.d(TAG, "Конец дня:\t ${todayEndTime.timeInMillis}")
//        Log.d(TAG, "Разница:\t \t ${todayEndTime.timeInMillis - todayStartTime.timeInMillis}")
//        Log.d(TAG, "В часах: \t ${(todayEndTime.timeInMillis - todayStartTime.timeInMillis) / 3600000}")
    }
}