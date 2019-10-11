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
    private lateinit var metronomeState: MetronomeState

    private var autoIncrement: Boolean = false
    private var autoDecrement: Boolean = false
    private val REPEAT_DELAY: Long = 50L
    private var repeatUpdateHandler: Handler = Handler()
    private var lowerLimitBpm = 40
    private var upperLimitBpm = 210

    private fun log_info(message: String) {
        Log.i("Metronome", message)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val savedMetronomeState = savedInstanceState?.getCharSequence("metronomeState")
        metronomeState = if (savedMetronomeState != null && savedMetronomeState == "On") MetronomeState.On else MetronomeState.Off

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
                val currentBpm = getCurrentBpm()
                showCurrentBpmError(currentBpm == null || !checkBpmBounds(currentBpm))
                updateBpmButtons()
            }
        })

        metronomeToggle.setOnCheckedChangeListener { _, isChecked ->
            log_info("metronomeToggle | BEFORE metronomeState = $metronomeState")
            updateMetronomeStatus(isChecked)
            log_info("metronomeToggle | AFTER metronomeState = $metronomeState")
        }

        increaseBPM.setOnClickListener {
            log_info("setOnClickListener | Increasing BPM")
            updateBPM(true)
        }
        increaseBPM.setOnLongClickListener {
            log_info("setOnLongClickListener | Increasing BPM until ACTION_UP")
            autoIncrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        increaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP && autoIncrement) {
                log_info("setOnTouchListener | ACTION_UP triggered - Stop increasing BPM")
                autoIncrement = false
            }
            false
        }

        decreaseBPM.setOnClickListener {
            log_info("setOnClickListener | Decreasing BPM")
            updateBPM(false)
        }
        decreaseBPM.setOnLongClickListener {
            log_info("setOnLongClickListener | Decreasing BPM until ACTION_UP")
            autoDecrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        decreaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP && autoDecrement) {
                log_info("setOnTouchListener | ACTION_UP triggered - Stop decreasing BPM")
                autoDecrement = false
            }
            false
        }

    }

    override fun onStop() {
        super.onStop()

        if (metronomeState == MetronomeState.On) {
            stopMetronome()
        }

        metronomeToggle.isChecked = false   // Quick fix to stop metronome from playing again
    }

    private fun updateMetronomeStatus(turnOn: Boolean) {
        if (turnOn) {
            val currentBpm = getCurrentBpm()
            if (currentBpm != null && checkBpmBounds(currentBpm)) {
                startMetronome((1000 * (60 / currentBpm.toDouble())).toLong())
            }
        } else {
            stopMetronome()
        }
    }

    private fun startMetronome(sleepDuration: Long) {
        log_info("startMetronome | BEFORE metronomeState = $metronomeState")
        if (metronomeState == MetronomeState.Off) {
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

            metronomeState = MetronomeState.On
        }
        log_info("startMetronome | AFTER metronomeState = $metronomeState")

        updateBpmButtons()
    }

    private fun stopMetronome() {
        log_info("stopMetronome | BEFORE metronomeState = $metronomeState")
        if (metronomeState == MetronomeState.On) {
            metronome.cancel()

            metronomeState = MetronomeState.Off
        }
        log_info("stopMetronome | AFTER metronomeState = $metronomeState")

        updateBpmButtons()
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

    private fun enableMetronomeToggle() {
        metronomeToggle.isEnabled = true
        when (metronomeState) {
            MetronomeState.On -> {
                metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_on_background)
                metronomeToggle.setTextColor(getColor(R.color.black75Percent))
            }
            MetronomeState.Off -> {
                metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_off_background)
                metronomeToggle.setTextColor(getColor(R.color.primaryTextColor))
            }
        }
    }

    private fun disableIncreaseBpm() {
        if (autoIncrement) {
            log_info("disableIncreaseBpm | IncreaseBPM button is disable - stop increasing BPM")
            autoIncrement = false
        }
        increaseBPM.isEnabled = false
        increaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
    }

    private fun disableDecreaseBpm() {
        if (autoDecrement) {
            log_info("disableDecreaseBpm | DecreaseBPM button is disable - stop decreasing BPM")
            autoDecrement = false
        }
        decreaseBPM.isEnabled = false
        decreaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
    }

    private fun disableCurrentBpm() {
        currentBPM.isEnabled = false
        currentBPM.setTextColor(getColor(R.color.colorAccent50Percent))
    }

    private fun disableMetronomeToggle() {
        metronomeToggle.isEnabled = false
        metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_disabled_background)
        metronomeToggle.setTextColor(getColor(R.color.primaryTextColor))
    }

    private fun updateBpmButtons() {
        val currentBpm = getCurrentBpm()

        if (currentBpm == null) {
            disableIncreaseBpm()
            disableDecreaseBpm()
            disableMetronomeToggle()
        } else if (currentBpm < 40) {
            enableIncreaseBpm()

            disableDecreaseBpm()
            disableMetronomeToggle()
        } else if (currentBpm == 40) {
            enableMetronomeToggle()
            when (metronomeState) {
                MetronomeState.Off -> {
                    enableIncreaseBpm()
                    disableDecreaseBpm()
                    enableCurrentBpm()
                }
                MetronomeState.On -> {
                    disableIncreaseBpm()
                    disableDecreaseBpm()
                    disableCurrentBpm()
                }
            }
        } else if (currentBpm < 210) {
            enableMetronomeToggle()
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
        } else if (currentBpm == 210) {
            enableMetronomeToggle()
            when (metronomeState) {
                MetronomeState.Off -> {
                    disableIncreaseBpm()
                    enableDecreaseBpm()
                    enableCurrentBpm()
                }
                MetronomeState.On -> {
                    disableIncreaseBpm()
                    disableDecreaseBpm()
                    disableCurrentBpm()
                }
            }
        } else {
            disableIncreaseBpm()
            enableDecreaseBpm()
            disableMetronomeToggle()
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
