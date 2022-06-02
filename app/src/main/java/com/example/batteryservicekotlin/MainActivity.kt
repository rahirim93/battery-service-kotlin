package com.example.batteryservicekotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startForegroundService
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.core.series.Cartesian
import com.anychart.enums.ScaleTypes
import com.example.batteryservicekotlin.database.Unit
import com.example.batteryservicekotlin.service.*
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var anyChartView: AnyChartView

    private lateinit var todayListUnits: List<Unit>

    private lateinit var line: com.anychart.charts.Cartesian

    // Кнопки запуска и остановки сервиса
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    // Остальные кнопки
    private lateinit var buttonTest: Button
    private lateinit var buttonFull: Button
    private lateinit var buttonRemove: Button

    private lateinit var textView: TextView

    private lateinit var editTextView: EditText

    private lateinit var seekBar: SeekBar

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

        // Создание и назначение обсервера за количеством сегоднящнего дня
        startDay = startDayMillis() // Начало дня для запроса выборки из БД
        endDay = endDayMillis() // Конец дня для запроса выборки из БД
        val batteryObserver = Observer<List<Unit>> { units ->
            todayListUnits = units
        }
        mainViewModel.todayUnitsLiveData(startDay, endDay).observe(this, batteryObserver)
    }

    private fun testChart2() {
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

        var seriesData = line.line(dataCurrentNow).stroke("0.1 black")

        var seriesData2 = line.line(dataCurrentAverage).stroke("0.1 red")
    }

    private fun test(hour: Int) {
        var dataCurrentNow = arrayListOf<DataEntry>()
        var dataCurrentAverage = arrayListOf<DataEntry>()

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

        var seriesData = line.line(dataCurrentNow).stroke("0.2 black")

        var seriesData2 = line.line(dataCurrentAverage).stroke("0.2 red")
    }



    private fun init() {
        // Инициализация и настройка графика
        anyChartView = findViewById(R.id.any_chart_view)
        line = AnyChart.line()
        line.xScale(ScaleTypes.LINEAR)
        anyChartView.setZoomEnabled(true)
        anyChartView.setChart(line)

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
            //test()
        }
        buttonFull = findViewById(R.id.button2)
        buttonFull.setOnClickListener {
            testChart2()
        }

        buttonRemove = findViewById(R.id.button_remove)
        buttonRemove.setOnClickListener {
            line.removeAllSeries()
        }

        textView = findViewById(R.id.textView)

        editTextView = findViewById(R.id.editTextNumber)

        seekBar = findViewById(R.id.seekBar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.min = 0
        }
        //val b = Calendar.getInstance().
        seekBar.max = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    test(seekBar.progress)
                }
            }
        })
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