package com.example.batteryservicekotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.enums.ScaleTypes
import com.example.batteryservicekotlin.database.Unit
import com.example.batteryservicekotlin.service.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var anyChartView: AnyChartView

    private lateinit var todayListUnits: List<Unit>

    //Кнопки запуска и остановки сервиса
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var buttonTest: Button
    private lateinit var buttonFull: Button

    private lateinit var textView: TextView

    private lateinit var editTextView: EditText

    private var startDay: Long = 0
    private var endDay: Long = 0

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init() // Инициализация виджетов
        actionOnService(Actions.START) // Запуск службы при запуске приложения

        startDay = startDayMillis()
        endDay = endDayMillis()

        // Создание и назначение обсервера за количеством сегоднящнего дня
        val batteryObserver = Observer<List<Unit>> { units ->
            todayListUnits = units
        }
        mainViewModel.todayUnitsLiveData(startDay, endDay).observe(this, batteryObserver)

        //test(15)
    }

    private fun testChart2() {
        //anyChartView.clear()

        val line = AnyChart.line()

        //line.removeAllSeries()

        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()

        todayListUnits.forEach { unit ->
            // Вынести перевод в часы в отдельную функцию
            val hours = unit.date.hours.toDouble()
            val minutes = unit.date.minutes.toDouble()
            val seconds = unit.date.seconds.toDouble()
            val x = hours + (minutes / 60) + (seconds/3600)
            val y = unit.currentNow
            val i = unit.voltage
            dataCurrentNow.add(ValueDataEntry(x, y))
            dataCurrentAverage.add(ValueDataEntry(x, i))
        }

        var seriesData = line.line(dataCurrentNow)
        seriesData.stroke("0.1 black")

        var seriesData2 = line.line(dataCurrentAverage)
        seriesData2.stroke("0.1 red")

        line.xScale(ScaleTypes.LINEAR)

        anyChartView.setZoomEnabled(true)
        anyChartView.setChart(line)
    }

    private fun test() {
        //anyChartView.clear()

        val b = Calendar.getInstance()

        var hour = 0

        if (editTextView.text.toString() != "") {
            hour = editTextView.text.toString().toInt()
        }

        //val hour = b.get(Calendar.HOUR_OF_DAY)

        textView.text = "$hour"

        val line = AnyChart.line()

        //line.removeAllSeries()

        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()

        val todayDay = Calendar.getInstance()
        val startTimeCalendar = Calendar.getInstance()
        startTimeCalendar.set(
            todayDay.get(Calendar.YEAR),
            todayDay.get(Calendar.MONTH),
            todayDay.get(Calendar.DAY_OF_MONTH),
            hour - 1, 0, 0)
        val startTimeMillis = startTimeCalendar.timeInMillis
        log("Начало: ${startTimeCalendar.get(Calendar.HOUR)}")

        val endTimeCalendar = Calendar.getInstance()
        endTimeCalendar.set(
            todayDay.get(Calendar.YEAR),
            todayDay.get(Calendar.MONTH),
            todayDay.get(Calendar.DAY_OF_MONTH),
            hour, 0, 0)
        val endTimeMillis = endTimeCalendar.timeInMillis
        log("Конец: ${endTimeCalendar.get(Calendar.HOUR)}")

        todayListUnits.forEach { unit ->
            if (unit.date.time > startTimeMillis && unit.date.time < endTimeMillis) {
                val hours = unit.date.hours.toDouble()
                val minutes = unit.date.minutes.toDouble()
                val seconds = unit.date.seconds.toDouble()
                val x = hours + (minutes / 60) + (seconds/3600)
                val y = unit.currentNow
                val i = unit.voltage
                dataCurrentNow.add(ValueDataEntry(x, y))
                dataCurrentAverage.add(ValueDataEntry(x, i))
            }
        }

        var seriesData = line.line(dataCurrentNow)
        seriesData.stroke("0.2 black")

        var seriesData2 = line.line(dataCurrentAverage)
        seriesData2.stroke("0.2 red")

        line.xScale(ScaleTypes.LINEAR)

        anyChartView.setZoomEnabled(true)
        anyChartView.setChart(line)
    }



    private fun init() {
        anyChartView = findViewById(R.id.any_chart_view)

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
            //testChart2()
            test()
        }
        buttonFull = findViewById(R.id.button2)
        buttonFull.setOnClickListener {
            testChart2()
        }

        textView = findViewById(R.id.textView)

        editTextView = findViewById(R.id.editTextNumber)
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
}