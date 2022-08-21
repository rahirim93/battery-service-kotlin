package com.example.batteryservicekotlin

import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.*
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
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


/** Пока что не получается сделать запрос и получить ответ единожды.
 * Я могу только подписаться на LiveData c помощью Observer.
 * Поэтому пока напишем то что можно но более менее чисто.
 * Итого в данный момент делаем так.
 * При открытии подргужаем данные сегоднящнего дня. Отражаем это где нибудь.
 * При выборе даты делаем запрос с новым промежуктом времени.
 * Все полученные данные не должны автоматически обновляться, поэтому надоэ
 * сделать флажок который после ответа от бд будет закрывать обновление.
 * При работ с данными одного дня работаем уже без запросов к БД, только с тем что уже подгружено.
 *
 */

class MainActivity : AppCompatActivity() {
    // Массивы для хранения данных для графика
    private lateinit var dataCurrentNow: ArrayList<DataEntry>
    private lateinit var dataCurrentAverage: ArrayList<DataEntry>
    private lateinit var dataTemperature: ArrayList<DataEntry>
    private lateinit var dataVoltage: ArrayList<DataEntry>
    private lateinit var dataCapacityInMicroampereHours: ArrayList<DataEntry>
    private lateinit var dataCapacityInPercentage: ArrayList<DataEntry>

    // Выбор даты. (Material Date Picker)
    private lateinit var datePicker: MaterialDatePicker<Long>

    private lateinit var batteryObserver: Observer<List<Unit>>
    // Кнопки
    private lateinit var startButton: Button    // Кнопка запуска сервиса
    private lateinit var stopButton: Button     // Кнопка остановки сервиса
    private lateinit var buttonDate: Button     // Кнопка выбора даты
    private lateinit var buttonTest: Button     // Кнопка
    private lateinit var buttonFull: Button     // Кнопка
    private lateinit var buttonRemove: Button   // Кнопка
    private lateinit var button123: Button

    private lateinit var anyChartView: AnyChartView

    private lateinit var todayListUnits: List<Unit>

    private lateinit var chart: com.anychart.charts.Cartesian

    private lateinit var textView: TextView

    private lateinit var seekBar: SeekBar

    private var startDay: Long = 0
    private var endDay: Long = 0

    private var list: List<Unit>? = null

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        actionOnService(Actions.START) // Запуск службы при запуске приложения

        init() // Инициализация виджетов

        // Запрос данных БД сегодняшнего дня
        getDataFromDB(startDayMillis(), endDayMillis())

        //list = mainViewModel.todayUnitsList(startDayMillis(),endDayMillis())
        //log("${list?.size}")
    }

    private fun initDatePicker() {
        datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("SelectDate").build()
        datePicker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = datePicker.selection!!
            var sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            Toast.makeText(this, "${sdf.format(calendar.time)}", Toast.LENGTH_SHORT).show()
            var sdf2 = SimpleDateFormat("HH:mm", Locale.getDefault())
            Toast.makeText(this, "${sdf2.format(calendar.time)}", Toast.LENGTH_SHORT).show()
            mainViewModel.todayUnitsLiveData(
                startChosenDay(calendar).timeInMillis,
                endChosenDay(calendar).timeInMillis).observe(this, batteryObserver)
            chart.removeAllSeries()
            testChart2()
        }
    }

    // Запрос данных БД выбранного промежутка времени
    private fun getDataFromDB(startTimeOfRequest: Long, endTimeOfRequest: Long) {
        batteryObserver = Observer<List<Unit>> { units ->
            todayListUnits = units
            // Отображение количество записей в БД. Сколько есть и сколько должно быть
            textView.text = "Записей должно быть: ${(Calendar.getInstance().timeInMillis - startTimeOfRequest) / 10000}. Факт: ${todayListUnits.size}"
        }
        mainViewModel.todayUnitsLiveData(startTimeOfRequest, endTimeOfRequest).observe(this, batteryObserver)
    }

    // Подготовка данных для графика
    private fun filteredDataForGraph(startTime: Long, endTime: Long) {
        dataCurrentNow = arrayListOf()
        dataCurrentAverage = arrayListOf()
        dataTemperature = arrayListOf()
        dataVoltage = arrayListOf()
        dataCapacityInMicroampereHours = arrayListOf()
        dataCapacityInPercentage = arrayListOf()

        todayListUnits.forEach { unit ->
            if (unit.date.time > startTime && unit.date.time < endTime) {
                val timeHours = timeInHours(unit.date)
                dataCurrentNow.add(ValueDataEntry(timeHours, unit.currentNow))
                dataCurrentAverage.add(ValueDataEntry(timeHours, unit.currentAverage + 2000))
                dataTemperature.add(ValueDataEntry(timeHours, unit.temperature))
                dataVoltage.add(ValueDataEntry(timeHours, unit.voltage))
                dataCapacityInMicroampereHours.add(ValueDataEntry(timeHours, unit.capacityInMicroampereHours / 1000))
                dataCapacityInPercentage.add(ValueDataEntry(timeHours, unit.capacityInPercentage))
            }
        }
    }

    private fun drawGraph() {
        chart.run {
            line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
            line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
            line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
            line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
            line(dataCapacityInMicroampereHours).stroke("0.2 purple").name("Емк.мач(ф)")
            line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
        }
    }

    private fun testChart3() {

    }

    private fun testChart2() {
        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()
        val dataTemperature = arrayListOf<DataEntry>()
        val dataVoltage = arrayListOf<DataEntry>()
        val dataCapacityInMicroampereHours = arrayListOf<DataEntry>()
        val dataCapacityInPercentage = arrayListOf<DataEntry>()

        todayListUnits.forEach { unit ->
            val timeHours = timeInHours(unit.date)
            dataCurrentNow.add(ValueDataEntry(timeHours, unit.currentNow))
            dataCurrentAverage.add(ValueDataEntry(timeHours, unit.currentAverage + 2000))
            dataTemperature.add(ValueDataEntry(timeHours, unit.temperature))
            dataVoltage.add(ValueDataEntry(timeHours, unit.voltage))
            dataCapacityInMicroampereHours.add(ValueDataEntry(timeHours, unit.capacityInMicroampereHours / 1000))
            dataCapacityInPercentage.add(ValueDataEntry(timeHours, unit.capacityInPercentage))
        }

        chart.run {
            line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
            line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
            line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
            line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
            line(dataCapacityInMicroampereHours).stroke("0.2 purple").name("Емк.мач(ф)")
            line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
        }
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
                val timeHours = timeInHours(unit.date)
                dataCurrentNow.add(ValueDataEntry(timeHours, unit.currentNow))
                dataCurrentAverage.add(ValueDataEntry(timeHours, unit.currentAverage + 2000))
                dataTemperature.add(ValueDataEntry(timeHours, unit.temperature))
                dataVoltage.add(ValueDataEntry(timeHours, unit.voltage))
                dataCapacityInMicroampereHours.add(ValueDataEntry(timeHours, unit.capacityInMicroampereHours / 1000))
                dataCapacityInPercentage.add(ValueDataEntry(timeHours, unit.capacityInPercentage))
            }
        }

        chart.run {
            line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
            line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
            line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
            line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
            line(dataCapacityInMicroampereHours).stroke("0.2 purple").name("Емк.мач(ф)")
            line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
        }
    }

    protected override fun onCreateDialog(id: Int): Dialog {
        if (id == 1) {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            var datePickerDialog = DatePickerDialog(this, myCallBack, year, month, day)
            return datePickerDialog
        }
        return super.onCreateDialog(id)
    }
    var myCallBack = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->

        val calendar = Calendar.getInstance() // Текущая дата
        calendar.set(year, month, dayOfMonth) // Изменяем на выбранную
        val dateFormat = SimpleDateFormat("dd.MM.yyyy") //Настройка формата вывода в кнопку
        buttonDate.text = dateFormat.format(calendar.time) // Вывод в кнопку

        // Здесь нужно добавить сохранение выбранной даты в переменную
        // для последующего использования для запроса в БД
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
        initButtons()
        initChart()
        initDatePicker()

        textView = findViewById(R.id.textView)

        seekBar = findViewById(R.id.seekBar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            seekBar.min = 3
        }
        seekBar.max = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textView.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    chart.removeAllSeries()
                    test(seekBar.progress)
                }
            }
        })

    }

    private fun initButtons() {
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
            datePicker.show(supportFragmentManager, "datePicker")
        }
        buttonFull = findViewById(R.id.button2)
        buttonFull.setOnClickListener {
            chart.removeAllSeries()
            testChart2()
        }
        buttonRemove = findViewById(R.id.button_remove)
        buttonRemove.setOnClickListener {
            chart.removeAllSeries()
        }
        buttonDate = findViewById(R.id.button_date )
        buttonDate.text = Date().toString()
        buttonDate.setOnClickListener {
            showDialog(1)
        }

        button123 = findViewById(R.id.button123)
        button123.isEnabled = true
        button123.setOnClickListener {
            log("${list?.size}")
        }
    }

    override fun onRestart() {
        super.onRestart()
        log("onRestart")
    }

    override fun onStart() {
        super.onStart()
        log("onStart")
    }

    override fun onResume() {
        super.onResume()
        log("onResume")
        seekBar.max = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + 1
    }

    override fun onStop() {
        super.onStop()
        log("onStop")
    }

    private fun initChart() {
        // Инициализация и настройка графика
        anyChartView = findViewById(R.id.any_chart_view)
        chart = AnyChart.line()
        chart.xScale(ScaleTypes.LINEAR)
        // Добавление линии нуля сетки графика
        val zeroLine = chart.lineMarker(0).value(0).stroke("0.1 grey")
        anyChartView.setZoomEnabled(true)
        anyChartView.setChart(chart)
    }
}