package com.zachchurchill.metronome

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.widget.EditText
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MetronomeTimerTask : TimerTask() {
    private val metronomeClick = ToneGenerator(AudioManager.STREAM_MUSIC, 100)

    override fun run() {
        metronomeClick.startTone(ToneGenerator.TONE_CDMA_PIP, 50)
    }
}

class MainActivity : AppCompatActivity() {

    enum class MetronomeState {
        Off, On
    }

    private lateinit var metronome: Timer
    private var autoIncrement: Boolean = false
    private var autoDecrement: Boolean = false
    private val REPEAT_DELAY: Long = 50L
    private var repeatUpdateHandler: Handler = Handler()
    private var metronomeState = MetronomeState.Off

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        class RepetitiveUpdater : Runnable {
            override fun run() {
                if (autoIncrement) {
                    increaseBPM()
                    repeatUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY)
                } else if (autoDecrement) {
                    decreaseBPM()
                    repeatUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY)
                }
            }
        }

        metronomeToggle.setOnCheckedChangeListener { _, isChecked ->
            metronomeState = if (isChecked) MetronomeState.On else MetronomeState.Off

            Log.i("metronomeToggle", "Metronome State: $metronomeState")

            updateBpmButtons()

            if (isChecked) {
                metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_on_background)
                metronomeToggle.setTextColor(getColor(R.color.secondaryTextColor))
                startMetronome((1000 / (getCurrentBpm().toDouble() / 60)).toLong())
            } else {
                metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_off_background)
                metronomeToggle.setTextColor(getColor(R.color.primaryTextColor))
                stopMetronome()
            }
        }

        increaseBPM.setOnClickListener { increaseBPM() }
        increaseBPM.setOnLongClickListener {
            autoIncrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        increaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && autoIncrement) {
                autoIncrement = false
            }
            false
        }

        decreaseBPM.setOnClickListener { decreaseBPM() }
        decreaseBPM.setOnLongClickListener {
            autoDecrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        decreaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && autoDecrement) {
                autoDecrement = false
            }
            false
        }

    }

    private fun getCurrentBpm(): Int {
        return findViewById<EditText>(R.id.currentBPM).text.toString().toInt()
    }

    private fun startMetronome(sleepDuration: Long) {
        Log.i("startMetronome", "Starting metronome with sleep duration: $sleepDuration")
        metronome = Timer("metronome", false)
        metronome.scheduleAtFixedRate(MetronomeTimerTask(),0L, sleepDuration)
    }

    private fun stopMetronome() {
        Log.i("stopMetronome", "Stopping metronome")
        metronome.cancel()
    }

    private fun updateBpmButtons() {
        when (metronomeState) {
            MetronomeState.Off -> {
                increaseBPM.isEnabled = true
                decreaseBPM.isEnabled = true
            }
            MetronomeState.On -> {
                increaseBPM.isEnabled = false
                decreaseBPM.isEnabled = false
            }
        }
    }

    private fun increaseBPM() {
        val increasedBpmValue = getCurrentBpm() + 1

        if (increasedBpmValue <= 360) {
            currentBPM.setText(increasedBpmValue.toString())
        }
    }

    private fun decreaseBPM() {
        val decreasedBpmValue = getCurrentBpm() - 1

        if (decreasedBpmValue >= 40) {
            currentBPM.setText(decreasedBpmValue.toString())
        }
    }
}
