package com.example.batteryservicekotlin

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.batteryservicekotlin.service.*
import java.util.*
import com.example.batteryservicekotlin.database.Unit as Unit

private const val TAG = "MainActivityTag"

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var textView2: TextView
    private lateinit var textView3: TextView

    private val mainActivityViewModel: MainViewModel by lazy {
        ViewModelProviders.of(this).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.textView)
        textView2 = findViewById(R.id.textView2)
        textView3 = findViewById(R.id.textView3)

        title = "Endless Service"

        findViewById<Button>(R.id.btnStartService).let {
            it.setOnClickListener {
                log("START THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.START)
            }
        }

        findViewById<Button>(R.id.btnStopService).let {
            it.setOnClickListener {
                log("STOP THE FOREGROUND SERVICE ON DEMAND")
                actionOnService(Actions.STOP)
            }
        }

        // Observer, который следит за постоянно изменящимся размером БД
        // и обновляет TextView
        val batteryObserver = Observer<List<Unit>> { units ->
            textView.text = "Size is ${units.size}"

            // Текущая дата и время
            val calendar = Calendar.getInstance()
            // Начало дня текущей даты
            val startDay = GregorianCalendar(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                0, 0, 0)
            // Конец дня текущей даты
            val endDay = GregorianCalendar(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                23, 59, 59)
            // Даты в миллисекундах
            val calendarInMillis = calendar.timeInMillis
            val startDayInMillis = startDay.timeInMillis
            val endDayInMillis = endDay.timeInMillis
            // Вывод количества записей в БД сегодня
            var list = arrayListOf<Unit>() // Пустой изменяемый лист, для хранения выборки сегодняшнего дня
            // Перебираем всю БД на выбранный диапазон
            // в данном случае с начала дня до текущего момента
            units.forEach { unit ->
                if (unit.date.time in (startDayInMillis + 1) until calendarInMillis) {
                    list.add(unit)
                }
            }
            textView2.text = "Size today is ${list.size}" // Выводим в TextView

            // Вывод количества записей, которое должно быть сделано к текущему
            // моменту сегодня с интревалом записи 5 секунд
            val countUnitMustBe = (calendarInMillis - startDayInMillis) / 5000  // Разница между началом дня и текущим моментом на интервал 5 сек
            textView3.text = "Size must be today $countUnitMustBe"              // Вывод в TextView
        }
        mainActivityViewModel.unitListLiveData.observe(this, batteryObserver)
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