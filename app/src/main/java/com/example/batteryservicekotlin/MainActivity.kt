package com.example.batteryservicekotlin

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.anychart.AnyChart
import com.anychart.chart.common.dataentry.DataEntry
import com.anychart.chart.common.dataentry.ValueDataEntry
import com.anychart.charts.Cartesian
import com.anychart.enums.ScaleTypes
import com.example.batteryservicekotlin.database.Unit
import com.example.batteryservicekotlin.service.Actions
import com.example.batteryservicekotlin.service.EndlessService
import com.example.batteryservicekotlin.service.ServiceState
import com.example.batteryservicekotlin.service.getServiceState
import com.example.batteryservicekotlin.settingActivity.SettingActivity
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.slider.RangeSlider
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.lang.IllegalArgumentException
import java.text.SimpleDateFormat
import java.util.*


/** РЕШЕНО
 *  Пока что не получается сделать запрос и получить ответ единожды.
 * Я могу только подписаться на LiveData c помощью Observer.
 * Поэтому пока напишем то что можно, но более менее чисто.
 * Итого в данный момент делаем так.
 * При открытии подгружаем данные сегодняшнего дня. Отражаем это где-нибудь.
 * При выборе даты делаем запрос с новым промежутком времени.
 * Все полученные данные не должны автоматически обновляться, поэтому надо
 * сделать флажок который после ответа от бд будет закрывать обновление.
 * При работе с данными одного дня работаем уже без запросов к БД, только с тем что уже подгружено.
 * РЕШЕНО
 *
 * Нужно сделать чтобы при подгрузке отрисовывался полный график сегодняшнего дня.
 */


class MainActivity : AppCompatActivity() {
    //private val stepSlider = 0.2F // Шаг слайдера

    private lateinit var datePicker: MaterialDatePicker<Long>    // Выбор даты. (Material Date Picker)

    private lateinit var chosenDay: Calendar // Выбранный день

    private lateinit var chosenListUnits: List<Unit> // Данные выбранного дня

    private lateinit var batteryObserver: Observer<List<Unit>> // Наблюдатель данных выбранного дня

    private lateinit var chosenDayLiveData: LiveData<List<Unit>>

    private lateinit var chart: Cartesian // График

    private val mainViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    private var flag = true
    private var flagRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        buttonFull.setOnClickListener {
            GlobalScope.launch(Dispatchers.Unconfined) {
                log("Start count.")
                val count = mainViewModel.getCount()
                log("Count: $count")

                val countOffset = count / 100000

                var counter = 0
                for (i in 0..countOffset) {
                    val a = mainViewModel.getLimit(i * 100000)
                    val file = File(filesDir, "testFile.txt")
                    val csvWriter = FileWriter(file, true)
                    a.forEach {
                        counter += 1
                        log("$counter из $count")
                        csvWriter.write(
                            "${it.id}," +
                                    "${it.date.time}," +
                                    "${it.capacityInMicroampereHours}," +
                                    "${it.capacityInPercentage}," +
                                    "${it.currentAverage}," +
                                    "${it.currentNow},${it.temperature},${it.voltage}\n")
                    }
                    csvWriter.close()
                }
            }

        }

        button_start.setOnClickListener {
            val file = File(filesDir, "testFile.txt")
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                    this@MainActivity,
                    "com.example.batteryservicekotlin.fileprovider",
                    file
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
            val intent = Intent(Intent.ACTION_SEND)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.type = "text/plain"
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(Intent.createChooser(intent, "Share file:"))
        }

        button_stop.setOnClickListener {
            val file = File(filesDir, "testFile.txt")
            file.delete()
        }

        disEnableButtons()

        chosenDay = Calendar.getInstance()

        if (mainViewModel.getAutostartService()) {
            actionOnService(Actions.START) // Запуск службы при запуске приложения
        }

        init() // Инициализация виджетов

        getDataFromDB(startChosenDay(chosenDay).timeInMillis, endChosenDay(chosenDay).timeInMillis) // Запрос данных БД сегодняшнего дня
    }

    private fun disEnableButtons() {
        buttonStartPrev.isEnabled = false
        buttonEndPrev.isEnabled = false
        buttonStartNext.isEnabled = false
        buttonEndNext.isEnabled = false
        slider.isEnabled = false
    }

    private fun enableButtons() {
        buttonStartPrev.isEnabled = true
        buttonEndPrev.isEnabled = true
        buttonStartNext.isEnabled = true
        buttonEndNext.isEnabled = true
        slider.isEnabled = true
    }

    // Запрос данных БД выбранного промежутка времени
    private fun getDataFromDB(startTimeOfRequest: Long, endTimeOfRequest: Long) {
        batteryObserver = Observer<List<Unit>> { units ->
            Log.d("myTag", "${Calendar.getInstance().time}")
            if (true) {
                chosenListUnits = units
                //flag = false
                Toast.makeText(this, "Данные получены", Toast.LENGTH_SHORT).show()
                log("Размер: ${chosenListUnits.size}")

                enableButtons()
//                // Обновление графика в текущем отрезке
//                if (flagRefresh) {
//                    flagRefresh = false
//                    chart.removeAllSeries()
//                    //chosenGraph(sliderStartInCalendar(slider, datePicker), sliderEndInCalendar(slider, datePicker))
//                }
            }
        }
        chosenDayLiveData = mainViewModel.chosenDayUnitsLiveData(startTimeOfRequest, endTimeOfRequest)
        chosenDayLiveData.observe(this, batteryObserver)
    }

    private fun fullGraph() {
        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()
        val dataTemperature = arrayListOf<DataEntry>()
        val dataVoltage = arrayListOf<DataEntry>()
        val dataCapacityInMicroamperesHours = arrayListOf<DataEntry>()
        val dataCapacityInPercentage = arrayListOf<DataEntry>()

        chosenListUnits.forEach { unit ->
            val timeHours = timeInHours(unit.date)
            dataCurrentNow.add(ValueDataEntry(timeHours, unit.currentNow))
            dataCurrentAverage.add(ValueDataEntry(timeHours, unit.currentAverage + 2000))
            dataTemperature.add(ValueDataEntry(timeHours, unit.temperature))
            dataVoltage.add(ValueDataEntry(timeHours, unit.voltage))
            dataCapacityInMicroamperesHours.add(ValueDataEntry(timeHours, unit.capacityInMicroampereHours / 1000))
            dataCapacityInPercentage.add(ValueDataEntry(timeHours, unit.capacityInPercentage))
        }

        chart.run {
            line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
            line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
            line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
            line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
            line(dataCapacityInMicroamperesHours).stroke("0.2 purple").name("Емк.мач(ф)")
            line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
        }
    }

    private fun chosenGraph(startTime: Calendar, endTime: Calendar) {
        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()
        val dataTemperature = arrayListOf<DataEntry>()
        val dataVoltage = arrayListOf<DataEntry>()
        val dataCapacityInMicroamperesHours = arrayListOf<DataEntry>()
        val dataCapacityInPercentage = arrayListOf<DataEntry>()

        val startTimeMillis = startTime.timeInMillis

        val endTimeMillis = endTime.timeInMillis

        chosenListUnits.forEach { unit ->
            if (unit.date.time > startTimeMillis && unit.date.time < endTimeMillis) {
                val timeHours = timeInHours(unit.date)
                dataCurrentNow.add(ValueDataEntry(timeHours, unit.currentNow / 100.0))
                dataCurrentAverage.add(ValueDataEntry(timeHours, unit.currentAverage / 100.0))
                dataTemperature.add(ValueDataEntry(timeHours, unit.temperature!! / 10.0))
                dataVoltage.add(ValueDataEntry(timeHours, unit.voltage!! / 100.0))
                dataCapacityInMicroamperesHours.add(ValueDataEntry(timeHours, unit.capacityInMicroampereHours / 1000000.0))
                dataCapacityInPercentage.add(ValueDataEntry(timeHours, unit.capacityInPercentage))
            }
        }

        chart.run {
            if(checkBoxCurrentNow.isChecked) {
                line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
            }
            if(checkBoxCurrentAverage.isChecked) {
                line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
            }
            if(checkBoxTemperature.isChecked) {
                line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
            }
            if(checkBoxVoltage.isChecked) {
                line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
            }
            if(checkBoxCapacityInMicroamperesHours.isChecked) {
                line(dataCapacityInMicroamperesHours).stroke("0.2 purple").name("Емк.мач(ф)")
            }
            if(checkBoxCapacityInPercentage.isChecked) {
                line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
            }
        }
    }

    private fun chosenGraphTest(list: ArrayList<Unit>) {
        if (list.size == 0) {
            Toast.makeText(this, "Выход за пределы диапазона", Toast.LENGTH_SHORT).show()
            return
        }
        val dataCurrentNow = arrayListOf<DataEntry>()
        val dataCurrentAverage = arrayListOf<DataEntry>()
        val dataTemperature = arrayListOf<DataEntry>()
        val dataVoltage = arrayListOf<DataEntry>()
        val dataCapacityInMicroamperesHours = arrayListOf<DataEntry>()
        val dataCapacityInPercentage = arrayListOf<DataEntry>()

        // Здесь будем хранить суммарную емкость
        val dataCapacitySum = arrayListOf<DataEntry>()
        // Здесь у нас уже есть отфильтрованный ток. Здесь нужно сформировать график суммарной емкости.
        // Добавляем первую точку с нулевой емкостью.
        val timeFirstPointCapacitySum = timeInHours(list[0].date)
        val valueFirstPointCapacitySum = 0
        dataCapacitySum.add(ValueDataEntry(timeFirstPointCapacitySum, valueFirstPointCapacitySum))
        var sumCapacity = 0.0

        list.forEach { unit ->
            // 1 - сдвоенная батарея
            // 2 - инверсия значения тока
            if (mainViewModel.getDoubleBattery() && mainViewModel.getInversionCurrent()) {                  // 1 и 2
                dataCurrentNow.add(ValueDataEntry(timeInHours(unit.date), - unit.currentNow * 2))
            } else if (mainViewModel.getDoubleBattery() && !mainViewModel.getInversionCurrent()) {          // Только 1
                dataCurrentNow.add(ValueDataEntry(timeInHours(unit.date), unit.currentNow * 2))
            } else if (!mainViewModel.getDoubleBattery() && mainViewModel.getInversionCurrent()) {          // Только 2
                dataCurrentNow.add(ValueDataEntry(timeInHours(unit.date), - unit.currentNow))
            } else if (!mainViewModel.getDoubleBattery() && !mainViewModel.getInversionCurrent()) {         // Ни то, ни другое
                if (mainViewModel.getCurrentCorrect()) {
                    dataCurrentNow.add(ValueDataEntry(timeInHours(unit.date), unit.currentNow / 1000))
                } else {
                    dataCurrentNow.add(ValueDataEntry(timeInHours(unit.date), unit.currentNow))
                }
            }

            // Условие для учета сдвоенной батареи
            if (mainViewModel.getDoubleBattery()) {
                dataCurrentAverage.add(ValueDataEntry(timeInHours(unit.date), unit.currentAverage * 2))
            } else {
                dataCurrentAverage.add(ValueDataEntry(timeInHours(unit.date), unit.currentAverage))
            }




            dataTemperature.add(ValueDataEntry(timeInHours(unit.date), unit.temperature!!))
            dataVoltage.add(ValueDataEntry(timeInHours(unit.date), unit.voltage!! / 10.0))
            dataCapacityInMicroamperesHours.add(ValueDataEntry(timeInHours(unit.date), unit.capacityInMicroampereHours / 3000.0))
            dataCapacityInPercentage.add(ValueDataEntry(timeInHours(unit.date), unit.capacityInPercentage * 13))

            //sumCapacity += unit.currentAverage
            //dataCapacitySum.add(ValueDataEntry(timeInHours(unit.date), sumCapacity))
            //log("$sumCapacity")
            // Неправильно считаю. Я просто складываю ток, а надо считать емкость и складывать емкость.
        }

        list.forEachIndexed { index, unit ->
            if (index > 0) {
                val current = list[index - 1].currentAverage
                val time = timeInHours(list[index].date) - timeInHours(list[index - 1].date)
                val capacity = current * time
                sumCapacity += capacity
                dataCapacitySum.add(ValueDataEntry(timeInHours(unit.date), sumCapacity/1000))
                log("$sumCapacity")
            }
        }




        chart.run {
            if(checkBoxCurrentNow.isChecked) {
                line(dataCurrentNow).stroke("0.2 black").name("Тек.ток(ч)")
            }
            if(checkBoxCurrentAverage.isChecked) {
                line(dataCurrentAverage).stroke("0.2 red").name("Ср.ток(к)")
            }
            if(checkBoxTemperature.isChecked) {
                line(dataTemperature).stroke("0.2 blue").name("Темп.(г)")
            }
            if(checkBoxVoltage.isChecked) {
                line(dataVoltage).stroke("0.2 green").name("Напр.(з)")
            }
            if(checkBoxCapacityInMicroamperesHours.isChecked) {
                line(dataCapacityInMicroamperesHours).stroke("0.2 purple").name("Емк.мач(ф)")
            }
            if(checkBoxCapacityInPercentage.isChecked) {
                line(dataCapacityInPercentage).stroke("0.2 cyan").name("Емк.%(ц)")
            }
            if(checkBoxCapacitySum.isChecked) {
                line(dataCapacitySum).stroke("0.2 black").name("Сум.емк.")
            }

        }
    }

    private fun filteredList(list: List<Unit>, startCalendar: Calendar, endCalendar: Calendar): ArrayList<Unit> {
        val arrayList = ArrayList<Unit>()
        list.forEach { unit ->
            if (unit.date.time > startCalendar.timeInMillis && unit.date.time < endCalendar.timeInMillis) {
                arrayList.add(unit)
            }
        }
        return arrayList
    }

    private fun init() {
        initButtons()
        initChart()
        initDatePicker()
        initCheckBoxes()


        slider.addOnSliderTouchListener(object : RangeSlider.OnSliderTouchListener{
            override fun onStartTrackingTouch(slider: RangeSlider) {

            }

            override fun onStopTrackingTouch(slider: RangeSlider) {
                chart.removeAllSeries()
                chosenGraphTest(filteredList(
                    chosenListUnits,
                    sliderStartInCalendar(slider, chosenDay),
                    sliderEndInCalendar(slider, chosenDay)))
            }
        })
        slider.addOnChangeListener { slider, value, fromUser ->
//            chart.removeAllSeries()
//            chosenGraphTest(filteredList(
//                chosenListUnits,
//                sliderStartInCalendar(slider, chosenDay),
//                sliderEndInCalendar(slider, chosenDay)))
        }
    }

    private fun initCheckBoxes() {

    }

    private fun initButtons() {

        buttonSettings.setOnClickListener {
            val intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
        }

//        button_start.setOnClickListener {
//            actionOnService(Actions.START)
//        }
//
//        button_stop.setOnClickListener {
//            actionOnService(Actions.STOP)
//        }

//        buttonFull.setOnClickListener {
//            chart.removeAllSeries()
//            fullGraph()
//        }

//        buttonRefresh.setOnClickListener {
//            flag = true
//            flagRefresh = true
//        }

//        buttonRemove.setOnClickListener {
//            chart.removeAllSeries()
//        }

        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        button_date.text = sdf.format(calendar.time)
        button_date.setOnClickListener {
            datePicker.show(supportFragmentManager, "datePicker")
            //datePicker.
        }

        buttonNextDate.setOnClickListener {

            disEnableButtons()

            chosenDay.timeInMillis = chosenDay.timeInMillis + 24 * 60 * 60 * 1000
            if(chosenDayLiveData.hasObservers()) {
                chosenDayLiveData.removeObserver(batteryObserver)
                chosenDayLiveData = mainViewModel.chosenDayUnitsLiveData(
                    startChosenDay(chosenDay).timeInMillis,
                    endChosenDay(chosenDay).timeInMillis)
                chosenDayLiveData.observe(this, batteryObserver)
            }
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            button_date.text = sdf.format(chosenDay.time)
        }

        buttonPreviousDate.setOnClickListener {

            disEnableButtons()
            
            chosenDay.timeInMillis = chosenDay.timeInMillis - 24 * 60 * 60 * 1000
            if(chosenDayLiveData.hasObservers()) {
                chosenDayLiveData.removeObserver(batteryObserver)
                chosenDayLiveData = mainViewModel.chosenDayUnitsLiveData(
                    startChosenDay(chosenDay).timeInMillis,
                    endChosenDay(chosenDay).timeInMillis)
                chosenDayLiveData.observe(this, batteryObserver)
            }
            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            button_date.text = sdf.format(chosenDay.time)
        }

        // Управление перенесено в окно настроек
//        buttonStartWorker.setOnClickListener {
//            val myWorkRequest = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES).build()
//            WorkManager.getInstance(this).enqueue(myWorkRequest)
//        }
//
//        buttonStopWorker.setOnClickListener {
//            WorkManager.getInstance(this).cancelAllWork()
//            WorkManager.getInstance(this).pruneWork()
//        }

        buttonStartPrev.setOnClickListener {
            // Программная установка движков слайдера
            val list = mutableListOf<Float>()
            val newStartThumbValue = slider.values[0] - mainViewModel.getStepRange()  // Значение ползунка начала
            val endThumbValue = slider.values[1]                    // Значение ползунка конца
            if (newStartThumbValue < 0) return@setOnClickListener   // Проверяем не выйдет ли слайдер за 0, если выходит, то прерываем выполнение
            list.add(newStartThumbValue)                            // Добавляем в массив с величинами слайдера измененное значение первого ползунка
            list.add(endThumbValue)                                 // Добавляем в массив с величинами слайдера значение второго ползунка (значение не меняем)
            slider.values = list                                    // Устанавливаем положение ползунка передавая лист с величинами первого и второго ползунка

            chart.removeAllSeries()
            chosenGraphTest(filteredList(
                chosenListUnits,
                sliderStartInCalendar(slider, chosenDay),
                sliderEndInCalendar(slider, chosenDay)))
        }

        buttonStartNext.setOnClickListener {
            // Программная установка движков слайдера
            val list = mutableListOf<Float>()
            val newStartThumbValue = slider.values[0] + mainViewModel.getStepRange()                          // Новое значение ползунка начала
            val endThumbValue = slider.values[1]                                            // Значение ползунка конца
            if (endThumbValue - newStartThumbValue < mainViewModel.getStepRange()) return@setOnClickListener  // Проверяем не совместится ли слайдер со вторым, если совместится, то прерываем выполнение
            list.add(newStartThumbValue)                                                    // Добавляем в массив с величинами слайдера измененное значение первого ползунка
            list.add(endThumbValue)                                                         // Добавляем в массив с величинами слайдера значение второго ползунка (значение не меняем)
            slider.values = list                                                            // Устанавливаем положение ползунка передавая лист с величинами первого и второго ползунка

            chart.removeAllSeries()
            chosenGraphTest(filteredList(
                chosenListUnits,
                sliderStartInCalendar(slider, chosenDay),
                sliderEndInCalendar(slider, chosenDay)))
        }

        buttonEndPrev.setOnClickListener {
            // Программная установка движков слайдера
            val list = mutableListOf<Float>()
            val startThumbValue = slider.values[0]                                          // Значение ползунка начала
            val newEndThumbValue = slider.values[1] - mainViewModel.getStepRange()                            // Новое значение ползунка конца
            if (newEndThumbValue - startThumbValue < mainViewModel.getStepRange()) return@setOnClickListener  // Проверяем не совместится ли слайдер со вторым, если совместится, то прерываем выполнение
            list.add(startThumbValue)                                                       // Добавляем в массив с величинами слайдера значение первого ползунка (значение не меняем)
            list.add(newEndThumbValue)                                                      // Добавляем в массив с величинами слайдера измененное значение второго ползунка
            slider.values = list                                                            // Устанавливаем положение ползунка передавая лист с величинами первого и второго ползунка

            chart.removeAllSeries()
            chosenGraphTest(filteredList(
                chosenListUnits,
                sliderStartInCalendar(slider, chosenDay),
                sliderEndInCalendar(slider, chosenDay)))
        }

        buttonEndNext.setOnClickListener {
            // Программная установка движков слайдера
            val list = mutableListOf<Float>()
            val startThumbValue = slider.values[0]                  // Значение ползунка начала
            val newEndThumbValue = slider.values[1] + mainViewModel.getStepRange()    // Новое значение ползунка конца
            if (newEndThumbValue > 24) return@setOnClickListener    // Проверяем не выйдет ли слайдер за максимальное значение, если выйдет, то прерываем выполнение
            list.add(startThumbValue)                               // Добавляем в массив с величинами слайдера значение первого ползунка (значение не меняем)
            list.add(newEndThumbValue)                              // Добавляем в массив с величинами слайдера измененное значение второго ползунка
            slider.values = list                                    // Устанавливаем положение ползунка передавая лист с величинами первого и второго ползунка

            chart.removeAllSeries()
            chosenGraphTest(filteredList(
                chosenListUnits,
                sliderStartInCalendar(slider, chosenDay),
                sliderEndInCalendar(slider, chosenDay)))
        }
    }

    private fun initDatePicker() {
        datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Выберете дату").build()
        datePicker.addOnPositiveButtonClickListener {
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = datePicker.selection!!
            Log.d("myTag", "Дата ${calendar.time}")

            val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            button_date.text = sdf.format(calendar.time)

            chosenDay = calendar

            if(chosenDayLiveData.hasObservers()) {
                chosenDayLiveData.removeObserver(batteryObserver)
                chosenDayLiveData = mainViewModel.chosenDayUnitsLiveData(
                    startChosenDay(chosenDay).timeInMillis,
                    endChosenDay(chosenDay).timeInMillis)
                chosenDayLiveData.observe(this, batteryObserver)
            }
        }
    }

    private fun initChart() {
        // Инициализация и настройка графика
        chart = AnyChart.line()
        chart.xScale(ScaleTypes.LINEAR)
        chart.yAxis(0).enabled(false)
        //chart.legend().enabled(true)

        // Настройка отображения времени во всплывающей подсказке
        chart.tooltip().titleFormat("function() {\n" +
                "var hours = Math.trunc(this.x);\n" +
                "var minutes = Math.trunc((this.x - hours) * 60);\n" +
                "var seconds = Math.trunc((((this.x - hours)*60) - minutes) * 60);\n" +
                "return 'Время: ' + hours + ':' + minutes + ':' + seconds;" +
                "\n" +
                "}")

        // Настройка отображения величин "х" во всплывающей подсказке
        chart
            .tooltip()
            .format("function() {\n" +
                    "if (this.seriesName == 'Тек.ток(ч)') {"+
                    "return this.seriesName + ': ' + this.value + ' мА';"+
                    "} else if (this.seriesName == 'Ср.ток(к)') {"+
                    "return this.seriesName + ': ' + this.value + ' мА';"+
                    "} else if (this.seriesName == 'Темп.(г)') {"+
                    "return this.seriesName + ': ' + (Number(this.value) / 10.0) + ' \u2103';"+
                    "} else if (this.seriesName == 'Напр.(з)') {"+
                    "return this.seriesName + ': ' + (Number(this.value)/100.0).toFixed(2) + ' В';"+
                    "} else if (this.seriesName == 'Емк.мач(ф)') {"+
                    "return this.seriesName + ': ' + (Number(this.value)* 3).toFixed(0) + ' мА\u00B7ч';"+
                    "} else if (this.seriesName == 'Емк.%(ц)') {"+
                    "return this.seriesName + ': ' + (Number(this.value)/ 13).toFixed(0) + ' \u0025';"+
                    "} else if (this.seriesName == 'Сум.емк.') {"+
                    "return this.seriesName + ': ' + (Number(this.value)).toFixed(0) + ' мАч';"+
                    "}"+
                    "\n" +
                    "}")

        // Добавление линии нуля сетки графика
        val zeroLine = chart.lineMarker(0).value(0).stroke("0.1 grey")
        anyChartView.setZoomEnabled(true)
        anyChartView.setChart(chart)
    }

    private fun actionOnService(action: Actions) {
        if (getServiceState(this) == ServiceState.STOPPED && action == Actions.STOP) return
        Intent(this, EndlessService::class.java).also {
            //it.addFlags(PendingIntent.FLAG_IMMUTABLE)
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