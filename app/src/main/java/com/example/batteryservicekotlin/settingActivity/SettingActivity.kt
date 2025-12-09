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
import com.example.batteryservicekotlin.databinding.ActivitySettingBinding
import com.example.batteryservicekotlin.log
import com.example.batteryservicekotlin.service.Actions
import com.example.batteryservicekotlin.service.EndlessService
import com.example.batteryservicekotlin.service.ServiceState
import com.example.batteryservicekotlin.service.getServiceState
//import kotlinx.android.synthetic.main.activity_setting.*
import java.util.concurrent.TimeUnit


private const val WORK_TAG = "work tag"

class SettingActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingBinding

    private lateinit var workerObserver: Observer<List<WorkInfo>>

    private val settingActivityViewModel: SettingActivityViewModel by lazy {
        ViewModelProviders.of(this).get(SettingActivityViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        if (settingActivityViewModel.getCurrentCorrect()) {
            binding.buttonCurrentCorrectOn.isEnabled = false
            binding.buttonCurrentCorrectOff.isEnabled = true
        } else {
            binding.buttonCurrentCorrectOn.isEnabled = true
            binding.buttonCurrentCorrectOff.isEnabled = false
        }

        if (settingActivityViewModel.getAutostartService()) {
            binding.buttonAutoStartServiceOn.isEnabled = false
            binding.buttonAutoStartServiceOff.isEnabled = true
        } else {
            binding.buttonAutoStartServiceOn.isEnabled = true
            binding.buttonAutoStartServiceOff.isEnabled = false
        }

        if(settingActivityViewModel.getTestRestartService()) {
            binding.buttonTestRestartOn.isEnabled = false
            binding.buttonTestRestartOff.isEnabled = true
        } else {
            binding.buttonTestRestartOn.isEnabled = true
            binding.buttonTestRestartOff.isEnabled = false
        }

        if(settingActivityViewModel.getDoubleBattery()) {
            binding.buttonDoubleBatteryOn.isEnabled = false
            binding.buttonDoubleBatteryOff.isEnabled = true
        } else {
            binding.buttonDoubleBatteryOn.isEnabled = true
            binding.buttonDoubleBatteryOff.isEnabled = false
        }

        if(settingActivityViewModel.getInversionCurrent()) {
            binding.buttonInversionOn.isEnabled = false
            binding.buttonInversionOff.isEnabled = true
        } else {
            binding.buttonInversionOn.isEnabled = true
            binding.buttonInversionOff.isEnabled = false
        }


        binding.buttonServiceStart.setOnClickListener {
            actionOnService(Actions.START)
        }

        binding.buttonServiceStop.setOnClickListener {
            actionOnService(Actions.STOP)
        }

        binding.switch2.setOnClickListener {
            if (binding.switch2.isChecked) {
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

        binding.buttonAutoStartServiceOn.setOnClickListener {
            settingActivityViewModel.setAutostartService(true)
            binding.buttonAutoStartServiceOn.isEnabled = false
            binding.buttonAutoStartServiceOff.isEnabled = true
        }

        binding.buttonAutoStartServiceOff.setOnClickListener {
            settingActivityViewModel.setAutostartService(false)
            binding.buttonAutoStartServiceOn.isEnabled = true
            binding.buttonAutoStartServiceOff.isEnabled = false
        }

        binding.buttonTestRestartOn.setOnClickListener {
            settingActivityViewModel.setTestRestartService(true)
            binding.buttonTestRestartOn.isEnabled = false
            binding.buttonTestRestartOff.isEnabled = true
        }

        binding.buttonTestRestartOff.setOnClickListener {
            settingActivityViewModel.setTestRestartService(false)
            binding.buttonTestRestartOn.isEnabled = true
            binding.buttonTestRestartOff.isEnabled = false
        }

        binding.textViewStep.text = String.format("%.2f", settingActivityViewModel.getStepRange())
        val step = settingActivityViewModel.getStepRange()
        if (step == 0.3F) {
            binding.buttonStepPlus.isEnabled = false
            binding.buttonStepMinus.isEnabled = true
        } else if (step < 0.06F) {
            binding.buttonStepPlus.isEnabled = true
            binding.buttonStepMinus.isEnabled = false
        } else {
            binding.buttonStepPlus.isEnabled = true
            binding.buttonStepMinus.isEnabled = true
        }

        binding.buttonStepPlus.setOnClickListener {
            val newStep = settingActivityViewModel.getStepRange() + 0.05F
            settingActivityViewModel.setStepRange(newStep)
            if (newStep == 0.3F) binding.buttonStepPlus.isEnabled = false
            if (newStep > 0.06F) binding.buttonStepMinus.isEnabled = true
            binding.textViewStep.text = String.format("%.2f", settingActivityViewModel.getStepRange())
        }

        binding.buttonStepMinus.setOnClickListener {
            val newStep = settingActivityViewModel.getStepRange() - 0.05F
            settingActivityViewModel.setStepRange(newStep)
            if (newStep < 0.06F) binding.buttonStepMinus.isEnabled = false
            if (newStep < 0.3F) binding.buttonStepPlus.isEnabled = true
            binding.textViewStep.text = String.format("%.2f", settingActivityViewModel.getStepRange())
        }

        binding.buttonDoubleBatteryOn.setOnClickListener {
            settingActivityViewModel.setDoubleBattery(true)
            binding.buttonDoubleBatteryOn.isEnabled = false
            binding.buttonDoubleBatteryOff.isEnabled = true
        }

        binding.buttonDoubleBatteryOff.setOnClickListener {
            settingActivityViewModel.setDoubleBattery(false)
            binding.buttonDoubleBatteryOff.isEnabled = false
            binding.buttonDoubleBatteryOn.isEnabled = true
        }

        binding.buttonInversionOn.setOnClickListener {
            settingActivityViewModel.setInversionCurrent(true)
            binding.buttonInversionOn.isEnabled = false
            binding.buttonInversionOff.isEnabled = true
        }

        binding.buttonInversionOff.setOnClickListener {
            settingActivityViewModel.setInversionCurrent(false)
            binding.buttonInversionOn.isEnabled = true
            binding.buttonInversionOff.isEnabled = false
        }

        binding.buttonCurrentCorrectOn.setOnClickListener {
            settingActivityViewModel.setCurrentCorrect(true)
            binding.buttonCurrentCorrectOn.isEnabled = false
            binding.buttonCurrentCorrectOff.isEnabled = true
        }
        binding.buttonCurrentCorrectOff.setOnClickListener {
            settingActivityViewModel.setCurrentCorrect(false)
            binding.buttonCurrentCorrectOn.isEnabled = true
            binding.buttonCurrentCorrectOff.isEnabled = false
        }



        // Вывод информации о задачах WorkManager
        workerObserver = Observer {
            //textView.text = "Кол-во: ${it.size}\n"
            log("Кол-во: ${it.size}\n")

            binding.switch2.isChecked = it.isNotEmpty()

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
        binding.textViewState.append(" ${rs.size}")
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