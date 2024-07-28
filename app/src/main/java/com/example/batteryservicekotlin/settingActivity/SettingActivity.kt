package com.example.batteryservicekotlin.settingActivity

import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.batteryservicekotlin.MyWorker
import com.example.batteryservicekotlin.R
import com.example.batteryservicekotlin.log
import com.example.batteryservicekotlin.service.Actions
import com.example.batteryservicekotlin.service.EndlessService
import com.example.batteryservicekotlin.service.ServiceState
import com.example.batteryservicekotlin.service.getServiceState
import kotlinx.android.synthetic.main.activity_setting.*
import java.util.concurrent.TimeUnit


private const val WORK_TAG = "work tag"

class SettingActivity : AppCompatActivity() {

    private lateinit var workerObserver: Observer<List<WorkInfo>>

    private val settingActivityViewModel: SettingActivityViewModel by lazy {
        ViewModelProviders.of(this).get(SettingActivityViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        if (settingActivityViewModel.getCurrentCorrect()) {
            buttonCurrentCorrectOn.isEnabled = false
            buttonCurrentCorrectOff.isEnabled = true
        } else {
            buttonCurrentCorrectOn.isEnabled = true
            buttonCurrentCorrectOff.isEnabled = false
        }

        if (settingActivityViewModel.getAutostartService()) {
            buttonAutoStartServiceOn.isEnabled = false
            buttonAutoStartServiceOff.isEnabled = true
        } else {
            buttonAutoStartServiceOn.isEnabled = true
            buttonAutoStartServiceOff.isEnabled = false
        }

        if(settingActivityViewModel.getTestRestartService()) {
            buttonTestRestartOn.isEnabled = false
            buttonTestRestartOff.isEnabled = true
        } else {
            buttonTestRestartOn.isEnabled = true
            buttonTestRestartOff.isEnabled = false
        }

        if(settingActivityViewModel.getDoubleBattery()) {
            buttonDoubleBatteryOn.isEnabled = false
            buttonDoubleBatteryOff.isEnabled = true
        } else {
            buttonDoubleBatteryOn.isEnabled = true
            buttonDoubleBatteryOff.isEnabled = false
        }

        if(settingActivityViewModel.getInversionCurrent()) {
            buttonInversionOn.isEnabled = false
            buttonInversionOff.isEnabled = true
        } else {
            buttonInversionOn.isEnabled = true
            buttonInversionOff.isEnabled = false
        }


        buttonServiceStart.setOnClickListener {
            actionOnService(Actions.START)
        }

        buttonServiceStop.setOnClickListener {
            actionOnService(Actions.STOP)
        }

        switch2.setOnClickListener {
            if (switch2.isChecked) {
                val myWorkRequest = PeriodicWorkRequestBuilder<MyWorker>(15, TimeUnit.MINUTES)
                    .addTag(WORK_TAG)
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(myWorkRequest)
            } else {
                WorkManager.getInstance(applicationContext).cancelAllWork()
                WorkManager.getInstance(applicationContext).pruneWork()
            }
        }
//        buttonWorkerOn.setOnClickListener {
//
//        }
//
//        buttonWorkerOff.setOnClickListener {
//
//        }

        buttonAutoStartServiceOn.setOnClickListener {
            settingActivityViewModel.setAutostartService(true)
            buttonAutoStartServiceOn.isEnabled = false
            buttonAutoStartServiceOff.isEnabled = true
        }

        buttonAutoStartServiceOff.setOnClickListener {
            settingActivityViewModel.setAutostartService(false)
            buttonAutoStartServiceOn.isEnabled = true
            buttonAutoStartServiceOff.isEnabled = false
        }

        buttonTestRestartOn.setOnClickListener {
            settingActivityViewModel.setTestRestartService(true)
            buttonTestRestartOn.isEnabled = false
            buttonTestRestartOff.isEnabled = true
        }

        buttonTestRestartOff.setOnClickListener {
            settingActivityViewModel.setTestRestartService(false)
            buttonTestRestartOn.isEnabled = true
            buttonTestRestartOff.isEnabled = false
        }

        textViewStep.text = String.format("%.2f", settingActivityViewModel.getStepRange())
        val step = settingActivityViewModel.getStepRange()
        if (step == 0.3F) {
            buttonStepPlus.isEnabled = false
            buttonStepMinus.isEnabled = true
        } else if (step < 0.06F) {
            buttonStepPlus.isEnabled = true
            buttonStepMinus.isEnabled = false
        } else {
            buttonStepPlus.isEnabled = true
            buttonStepMinus.isEnabled = true
        }

        buttonStepPlus.setOnClickListener {
            val newStep = settingActivityViewModel.getStepRange() + 0.05F
            settingActivityViewModel.setStepRange(newStep)
            if (newStep == 0.3F) buttonStepPlus.isEnabled = false
            if (newStep > 0.06F) buttonStepMinus.isEnabled = true
            textViewStep.text = String.format("%.2f", settingActivityViewModel.getStepRange())
        }

        buttonStepMinus.setOnClickListener {
            val newStep = settingActivityViewModel.getStepRange() - 0.05F
            settingActivityViewModel.setStepRange(newStep)
            if (newStep < 0.06F) buttonStepMinus.isEnabled = false
            if (newStep < 0.3F) buttonStepPlus.isEnabled = true
            textViewStep.text = String.format("%.2f", settingActivityViewModel.getStepRange())
        }

        buttonDoubleBatteryOn.setOnClickListener {
            settingActivityViewModel.setDoubleBattery(true)
            buttonDoubleBatteryOn.isEnabled = false
            buttonDoubleBatteryOff.isEnabled = true
        }

        buttonDoubleBatteryOff.setOnClickListener {
            settingActivityViewModel.setDoubleBattery(false)
            buttonDoubleBatteryOff.isEnabled = false
            buttonDoubleBatteryOn.isEnabled = true
        }

        buttonInversionOn.setOnClickListener {
            settingActivityViewModel.setInversionCurrent(true)
            buttonInversionOn.isEnabled = false
            buttonInversionOff.isEnabled = true
        }

        buttonInversionOff.setOnClickListener {
            settingActivityViewModel.setInversionCurrent(false)
            buttonInversionOn.isEnabled = true
            buttonInversionOff.isEnabled = false
        }

        buttonCurrentCorrectOn.setOnClickListener {
            settingActivityViewModel.setCurrentCorrect(true)
            buttonCurrentCorrectOn.isEnabled = false
            buttonCurrentCorrectOff.isEnabled = true
        }
        buttonCurrentCorrectOff.setOnClickListener {
            settingActivityViewModel.setCurrentCorrect(false)
            buttonCurrentCorrectOn.isEnabled = true
            buttonCurrentCorrectOff.isEnabled = false
        }



        // Вывод информации о задачах WorkManager
        workerObserver = Observer {
            //textView.text = "Кол-во: ${it.size}\n"
            log("Кол-во: ${it.size}\n")

            switch2.isChecked = it.isNotEmpty()

            it.forEach { workInfo ->
                //textView.append("\n${workInfo.id}\n${workInfo.outputData}\n${workInfo.progress}\n${workInfo.state}\n${workInfo.runAttemptCount}\n${workInfo.tags}\n")
                log("\n" +
                        "Id: ${workInfo.id}\n" +
                        "Data: ${workInfo.outputData}\n" +
                        "Progress: ${workInfo.progress}\n" +
                        "State: ${workInfo.state}\n" +
                        "RunAttemptCount: ${workInfo.runAttemptCount}\n" +
                        "Tags: ${workInfo.tags}\n")
            }
        }
        WorkManager.getInstance(applicationContext).getWorkInfosByTagLiveData(WORK_TAG).observe(this, workerObserver)

        // Так можно узнать о включенном сервисе в данном приложении
        val am = this.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val rs = am.getRunningServices(50)
        log("Кол-во сервисов: ${rs.size}")
        textViewState.append(" ${rs.size}")
        rs.forEach {
        }
        for (i in rs.indices) {
            val rsi = rs[i]
            Log.i("rahirim", "Process " + rsi.process + " with component " + rsi.service.className)
        }
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