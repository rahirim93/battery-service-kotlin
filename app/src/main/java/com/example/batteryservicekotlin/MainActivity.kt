package com.example.batteryservicekotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.slider.RangeSlider
import java.text.SimpleDateFormat
import java.util.*


/** РЕШЕНО
 *  Пока что не получается сделать запрос и получить ответ единожды.
 * Я могу только подписаться на LiveData c помощью Observer.
 * Поэтому пока напишем то что можно но более менее чисто.
 * Итого в данный момент делаем так.
 * При открытии подргужаем данные сегоднящнего дня. Отражаем это где нибудь.
 * При выборе даты делаем запрос с новым промежуктом времени.
 * Все полученные данные не должны автоматически обновляться, поэтому надоэ
 * сделать флажок который после ответа от бд будет закрывать обновление.
 * При работ с данными одного дня работаем уже без запросов к БД, только с тем что уже подгружено.
 * РЕШЕНО
 *
 * Нужно сделать чтобы при подгрузке отрисовывался полный график сегодняшнего дня
 */

class MainActivity : AppCompatActivity() {
    // Выбор даты. (Material Date Picker)
    private lateinit var datePicker: MaterialDatePicker<Long>

    private lateinit var batteryObserver: Observer<List<Unit>>
    // Кнопки
    private lateinit var startButton: Button    // Кнопка запуска сервиса
    private lateinit var stopButton: Button     // Кнопка остановки сервиса
    private lateinit var buttonDate: Button     // Кнопка выбора даты
    private lateinit var buttonFull: Button     // Кнопка вывода графика за весь день
    private lateinit var buttonRemove: Button   // Кнопка очистки графика
    private lateinit var buttonRefresh: Button  // Кнопка обновления графика

    private lateinit var anyChartView: AnyChartView

    private lateinit var todayListUnits: List<Unit>

    private lateinit var chart: com.anychart.charts.Cartesian

    private lateinit var textView: TextView

    private lateinit var slider: RangeSlider

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    private var flag = true
    private var flagRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        actionOnService(Actions.START) // Запуск службы при запуске приложения

        init() // Инициализация виджетов

        // Запрос данных БД сегодняшнего дня
        getDataFromDB(startDayMillis(), endDayMillis())
    }

    // Запрос данных БД выбранного промежутка времени
    private fun getDataFromDB(startTimeOfRequest: Long, endTimeOfRequest: Long) {
        batteryObserver = Observer<List<Unit>> { units ->
            if (flag) {
                todayListUnits = units
                flag = false
                Toast.makeText(this, "Данные получены", Toast.LENGTH_SHORT).show()
                log("Размер: ${todayListUnits.size}")

                // Обновление графика в текущем отрезке
                if (flagRefresh) {
                    flagRefresh = false
                    chart.removeAllSeries()
                    chosenGraph(
                        sliderStartInCalendar(slider, datePicker),
                        sliderEndInCalendar(slider, datePicker))
                } else {

                }
            }
            // Отображение количество записей в БД. Сколько есть и сколько должно быть
            //textView.text = "Записей должно быть: ${(Calendar.getInstance().timeInMillis - startTimeOfRequest) / 10000}. Факт: ${todayListUnits.size}"
        }
        mainViewModel.todayUnitsLiveData(startTimeOfRequest, endTimeOfRequest).observe(this, batteryObserver)
    }

    private fun fullGraph() {
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

    private fun chosenGraph(startTime: Calendar, endTime: Calendar) {
        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()
        val dataTemperature = arrayListOf<DataEntry>()
        val dataVoltage = arrayListOf<DataEntry>()
        val dataCapacityInMicroampereHours = arrayListOf<DataEntry>()
        val dataCapacityInPercentage = arrayListOf<DataEntry>()

        val startTimeMillis = startTime.timeInMillis

        val endTimeMillis = endTime.timeInMillis

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

    private fun init() {
        initButtons()
        initChart()
        initDatePicker()

        textView = findViewById(R.id.textView)

        slider = findViewById(R.id.slider)
        slider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener{
            override fun onStartTrackingTouch(slider: RangeSlider) {

            }

            override fun onStopTrackingTouch(slider: RangeSlider) {
                // При запуске на datePicker не установлена дата, при обращении
                // выкидывает исключение. Поэтому сначала надо выбрать дату
                // иначе выкидываем предупреждающий тост
                if (datePicker.selection != null) {
                    chart.removeAllSeries()
                    chosenGraph(
                        sliderStartInCalendar(slider, datePicker),
                        sliderEndInCalendar(slider, datePicker))
                } else {
                    myToast(applicationContext, "Выберете дату")
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

        buttonFull = findViewById(R.id.buttonFull)
        buttonFull.setOnClickListener {
            chart.removeAllSeries()
            fullGraph()
        }

        buttonRefresh = findViewById(R.id.button_refresh)
        buttonRefresh.setOnClickListener {
            flag = true
            flagRefresh = true
        }

        buttonRemove = findViewById(R.id.button_remove)
        buttonRemove.setOnClickListener {
            chart.removeAllSeries()
        }

        buttonDate = findViewById(R.id.button_date )
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        buttonDate.text = sdf.format(calendar.time)
        buttonDate.setOnClickListener {
            datePicker.show(supportFragmentManager, "datePicker")
        }
    }

    private fun initDatePicker() {
        datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Выберете дату").build()
        datePicker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = datePicker.selection!!

            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            buttonDate.text = sdf.format(calendar.time)

            flag = true
            mainViewModel.todayUnitsLiveData(
                startChosenDay(calendar).timeInMillis,
                endChosenDay(calendar).timeInMillis).observe(this, batteryObserver)
        }
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

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            it.action = action.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //log("Starting the service in >=26 Mode")
                startForegroundService(it)
                return
            }
            //log("Starting the service in < 26 Mode")
            startService(it)
        }
    }
}