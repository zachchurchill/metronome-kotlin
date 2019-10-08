package com.zachchurchill.metronome

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
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

        currentBPM.setOnTouchListener {_, _ ->
            currentBPM.isCursorVisible = true
            false
        }

        currentBPM.setOnEditorActionListener { _, keyCode: Int?, _ ->

            if (keyCode == EditorInfo.IME_ACTION_DONE) {
                currentBPM.isCursorVisible = false
            }

            false
        }

        currentBPM.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank() || !s.isNullOrEmpty()) {
                    val currentBpm = getCurrentBpm()
                    if (currentBpm == null || !checkBpmBounds(currentBpm)) {
                        currentBPM.error = "BPM must be between 40 and 360"
                        metronomeToggle.isEnabled = false
                    } else {
                        metronomeToggle.isEnabled = true
                    }
                }
            }
        })

        metronomeToggle.setOnCheckedChangeListener { _, isChecked ->
            metronomeState = if (isChecked) MetronomeState.On else MetronomeState.Off

            Log.i("metronomeToggle", "Metronome State: $metronomeState")

            updateBpmButtons()

            if (isChecked) {
                val currentBpm = getCurrentBpm()
                if (currentBpm != null && checkBpmBounds(currentBpm)) {
                    metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_on_background)
                    metronomeToggle.setTextColor(getColor(R.color.secondaryTextColor))

                    startMetronome((1000 / (currentBpm.toDouble() / 60)).toLong())
                }
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

    private fun getCurrentBpm(): Int? {
        val currentBpmEditText = findViewById<EditText>(R.id.currentBPM)

        if (currentBpmEditText.text.isNotEmpty()) {
            return currentBpmEditText.text.toString().toInt()
        }

        return null
    }

    private fun checkBpmBounds(bpm: Int): Boolean {
        return (bpm in 40..360)
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
                currentBPM.isEnabled = true
                increaseBPM.isEnabled = true
                decreaseBPM.isEnabled = true

                currentBPM.setTextColor(getColor(R.color.colorAccent))
                increaseBPM.setBackgroundResource(R.drawable.btn_dark_background)
                decreaseBPM.setBackgroundResource(R.drawable.btn_dark_background)
            }
            MetronomeState.On -> {
                currentBPM.isEnabled = false
                increaseBPM.isEnabled = false
                decreaseBPM.isEnabled = false

                currentBPM.setTextColor(getColor(R.color.colorAccent50Percent))
                increaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
                decreaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
            }
        }
    }

    private fun increaseBPM() {
        val currentBpm = getCurrentBpm()

        if (currentBpm != null && currentBpm < 360) {
            currentBPM.setText((currentBpm + 1).toString())
        }
    }

    private fun decreaseBPM() {
        val currentBpm = getCurrentBpm()

        if (currentBpm != null && currentBpm > 40) {
            currentBPM.setText((currentBpm - 1).toString())
        }
    }
}
