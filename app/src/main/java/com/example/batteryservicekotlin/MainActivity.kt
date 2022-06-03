package com.example.batteryservicekotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.anychart.AnyChart
import com.anychart.AnyChartView
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.enums.ScaleTypes
import com.example.batteryservicekotlin.database.Unit
import com.example.batteryservicekotlin.service.Actions
import com.example.batteryservicekotlin.service.EndlessService
import com.example.batteryservicekotlin.service.ServiceState
import com.example.batteryservicekotlin.service.getServiceState
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
        val dataTemperature = arrayListOf<DataEntry>()
        val dataVoltage = arrayListOf<DataEntry>()
        val dataCapacityInMicroampereHours = arrayListOf<DataEntry>()
        val dataCapacityInPercentage = arrayListOf<DataEntry>()

        todayListUnits.forEach { unit ->
            // Вынести перевод в часы в отдельную функцию
            val hours = unit.date.hours.toDouble()
            val minutes = unit.date.minutes.toDouble()
            val seconds = unit.date.seconds.toDouble()
            val timeHours = hours + (minutes / 60) + (seconds/3600) // Время в часах для оси X
            dataCurrentNow.add(ValueDataEntry(timeHours, unit.currentNow))
            dataCurrentAverage.add(ValueDataEntry(timeHours, unit.currentAverage + 2000))
            dataTemperature.add(ValueDataEntry(timeHours, unit.temperature))
            dataVoltage.add(ValueDataEntry(timeHours, unit.voltage))
            dataCapacityInMicroampereHours.add(ValueDataEntry(timeHours, unit.capacityInMicroampereHours / 1000))
            dataCapacityInPercentage.add(ValueDataEntry(timeHours, unit.capacityInPercentage))
        }

        var seriesData = line.line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
        var seriesData2 = line.line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
        var seriesData3 = line.line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
        var seriesData4 = line.line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
        var seriesData5 = line.line(dataCapacityInMicroampereHours).stroke("0.2 purple").name("Емк.мач(ф)")
        var seriesData6 = line.line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
    }

    private fun test(hour: Int) {
        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()
        val dataTemperature = arrayListOf<DataEntry>()
        val dataVoltage = arrayListOf<DataEntry>()
        val dataCapacityInMicroampereHours = arrayListOf<DataEntry>()
        val dataCapacityInPercentage = arrayListOf<DataEntry>()

        val todayDay = Calendar.getInstance()
        val startTimeCalendar = Calendar.getInstance()
        startTimeCalendar.set(
            todayDay.get(Calendar.YEAR),
            todayDay.get(Calendar.MONTH),
            todayDay.get(Calendar.DAY_OF_MONTH),
            hour - 3, 0, 0)
        val startTimeMillis = startTimeCalendar.timeInMillis

        val endTimeCalendar = Calendar.getInstance()
        endTimeCalendar.set(
            todayDay.get(Calendar.YEAR),
            todayDay.get(Calendar.MONTH),
            todayDay.get(Calendar.DAY_OF_MONTH),
            hour, 0, 0)
        val endTimeMillis = endTimeCalendar.timeInMillis

        todayListUnits.forEach { unit ->
            if (unit.date.time > startTimeMillis && unit.date.time < endTimeMillis) {
                val hours = unit.date.hours.toDouble()
                val minutes = unit.date.minutes.toDouble()
                val seconds = unit.date.seconds.toDouble()
                val timeHours = hours + (minutes / 60) + (seconds/3600) // Время в часах для оси X
                dataCurrentNow.add(ValueDataEntry(timeHours, unit.currentNow))
                dataCurrentAverage.add(ValueDataEntry(timeHours, unit.currentAverage + 2000))
                dataTemperature.add(ValueDataEntry(timeHours, unit.temperature))
                dataVoltage.add(ValueDataEntry(timeHours, unit.voltage))
                dataCapacityInMicroampereHours.add(ValueDataEntry(timeHours, unit.capacityInMicroampereHours / 1000))
                dataCapacityInPercentage.add(ValueDataEntry(timeHours, unit.capacityInPercentage))
            }
        }

        var seriesData = line.line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
        var seriesData2 = line.line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
        var seriesData3 = line.line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
        var seriesData4 = line.line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
        var seriesData5 = line.line(dataCapacityInMicroampereHours).stroke("0.2 purple").name("Емк.мач(ф)")
        var seriesData6 = line.line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
    }



    private fun init() {
        // Инициализация и настройка графика
        anyChartView = findViewById(R.id.any_chart_view)
        line = AnyChart.line()
        line.xScale(ScaleTypes.LINEAR)
        // Добавление линии нуля сетки графика
        val zeroLine = line.lineMarker(0).value(0).stroke("0.1 grey")
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
        }
        buttonFull = findViewById(R.id.button2)
        buttonFull.setOnClickListener {
            line.removeAllSeries()
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
            seekBar.min = 3
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
                    line.removeAllSeries()
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