package com.zachchurchill.metronome

import android.media.AudioManager
import android.media.ToneGenerator
import java.util.Timer
import kotlin.concurrent.timerTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.util.Log

import kotlinx.android.synthetic.main.activity_main.*


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
    private var lowerLimitBpm = 40
    private var upperLimitBpm = 210

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        class RepetitiveUpdater : Runnable {
            override fun run() {
                if (autoIncrement) {
                    updateBPM(true)
                    repeatUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY)
                } else if (autoDecrement) {
                    updateBPM(false)
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
                        showCurrentBpmError(currentBpm == null || !checkBpmBounds(currentBpm))
                        metronomeToggle.isEnabled = false
                    } else {
                        metronomeToggle.isEnabled = true
                    }
                }
            }
        })

        metronomeToggle.setOnCheckedChangeListener { _, isChecked ->
            metronomeState = if (isChecked) MetronomeState.On else MetronomeState.Off

            updateBpmButtons()

            if (isChecked) {
                val currentBpm = getCurrentBpm()
                if (currentBpm != null && checkBpmBounds(currentBpm)) {
                    metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_on_background)
                    metronomeToggle.setTextColor(getColor(R.color.secondaryTextColor))

                    startMetronome((1000 * (60 / currentBpm.toDouble())).toLong())
                }
            } else {
                metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_off_background)
                metronomeToggle.setTextColor(getColor(R.color.primaryTextColor))
                stopMetronome()
            }
        }

        increaseBPM.setOnClickListener { updateBPM(true) }
        increaseBPM.setOnLongClickListener {
            autoIncrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        increaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP && autoIncrement) {
                autoIncrement = false
            }
            false
        }

        decreaseBPM.setOnClickListener { updateBPM(false) }
        decreaseBPM.setOnLongClickListener {
            autoDecrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        decreaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP && autoDecrement) {
                autoDecrement = false
            }
            false
        }

    }

    private fun startMetronome(sleepDuration: Long) {
        metronome = Timer("metronome", true)
        metronome.schedule(
            timerTask {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP)
                toneGenerator.release()
            },
            0L,
            sleepDuration
        )
    }

    private fun stopMetronome() {
        metronome.cancel()
    }

    private fun getCurrentBpm(): Int? {
        val currentBpmEditText = findViewById<EditText>(R.id.currentBPM)

        if (currentBpmEditText.text.isNotEmpty()) {
            return currentBpmEditText.text.toString().toInt()
        }

        return null
    }

    private fun checkBpmBounds(bpm: Int, lowerLimit: Int = lowerLimitBpm, upperLimit: Int = upperLimitBpm): Boolean {
        return (bpm in lowerLimit..upperLimit)
    }

    private fun showCurrentBpmError(showError: Boolean, lowerLimit: Int = lowerLimitBpm, upperLimit: Int = upperLimitBpm) {
        currentBPM.error = if (showError) "BPM must be between $lowerLimit and $upperLimit" else null
    }

    private fun enableIncreaseBpm() {
        increaseBPM.isEnabled = true
        increaseBPM.setBackgroundResource(R.drawable.btn_dark_background)
    }

    private fun enableDecreaseBpm() {
        decreaseBPM.isEnabled = true
        decreaseBPM.setBackgroundResource(R.drawable.btn_dark_background)
    }

    private fun enableCurrentBpm() {
        currentBPM.isEnabled = true
        currentBPM.setTextColor(getColor(R.color.colorAccent))
    }

    private fun disableIncreaseBpm() {
        increaseBPM.isEnabled = false
        increaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
    }

    private fun disableDecreaseBpm() {
        decreaseBPM.isEnabled = false
        decreaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
    }

    private fun disableCurrentBpm() {
        currentBPM.isEnabled = false
        currentBPM.setTextColor(getColor(R.color.colorAccent50Percent))
    }

    private fun updateBpmButtons() {
        when (metronomeState) {
            MetronomeState.Off -> {
                enableIncreaseBpm()
                enableDecreaseBpm()
                enableCurrentBpm()
            }
            MetronomeState.On -> {
                disableIncreaseBpm()
                disableDecreaseBpm()
                disableCurrentBpm()
            }
        }
    }

    private fun updateBPM(increase: Boolean, lowerLimit: Int = lowerLimitBpm, upperLimit: Int = upperLimitBpm) {
        val currentBpm = getCurrentBpm()

        if (currentBpm != null) {
            val newBpm = if (increase) currentBpm + 1 else currentBpm - 1
            val allowUpdate = if (increase) newBpm <= upperLimit else newBpm >= lowerLimit

            if (allowUpdate) {
                currentBPM.setText(newBpm.toString())
                showCurrentBpmError(!checkBpmBounds(newBpm))
            }
        }
    }
}
