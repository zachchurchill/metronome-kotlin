package com.zachchurchill.metronome

import android.media.AudioManager
import android.media.ToneGenerator
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
import java.util.*

typealias BPM = Long

const val MILLISECONDS_IN_SECONDS: Int = 1000
const val SECONDS_IN_MINUTES: Int = 60
const val REPEAT_DELAY_FOR_LONG_CLICKS: Long = 50L
const val METRONOME_TONE = ToneGenerator.TONE_PROP_BEEP
const val METRONOME_LOWER_BOUND: BPM = 40
const val METRONOME_UPPER_BOUND: BPM = 210

enum class MetronomeState {
    OFF,
    ON
}

object Metronome {
    var metronomeState: MetronomeState = MetronomeState.OFF
    private var metronome: Timer

    init {
        metronome = Timer("metronome", true)
    }

    private fun createNewTimer() {
        if (this.isOff()) {
            this.metronome = Timer("metronome", true)
        }
    }

    private fun calculateSleepDuration(bpm: BPM): Long {
        return (MILLISECONDS_IN_SECONDS * (SECONDS_IN_MINUTES / bpm.toDouble())).toLong()
    }

    fun start(bpm: BPM): Boolean {
        when (this.metronomeState) {
            MetronomeState.OFF -> {
                this.metronomeState = MetronomeState.ON
                this.metronome.schedule(
                    timerTask {
                        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                        toneGenerator.startTone(METRONOME_TONE)
                        toneGenerator.release()
                    },
                    0L,
                    calculateSleepDuration(bpm)
                )
            }
            MetronomeState.ON -> return false
        }
        return true
    }

    fun stop(): Boolean {
        when (this.metronomeState) {
            MetronomeState.OFF -> return false
            MetronomeState.ON -> {
                this.metronomeState = MetronomeState.OFF
                this.metronome.cancel()
                createNewTimer()
            }
        }
        return true
    }

    fun isOn(): Boolean {
        return this.metronomeState == MetronomeState.ON
    }

    fun isOff(): Boolean {
        return this.metronomeState == MetronomeState.OFF
    }
}

class MainActivity : AppCompatActivity() {

    private var autoIncrement: Boolean = false
    private var autoDecrement: Boolean = false
    private var repeatUpdateHandler: Handler = Handler()

    private fun logInfo(function: String, message: String) {
        Log.i("Metronome", "$function | $message")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val savedMetronomeState = savedInstanceState?.getCharSequence("metronomeState")
        Metronome.metronomeState = if (savedMetronomeState != null && savedMetronomeState == "On") MetronomeState.ON else MetronomeState.OFF

        class RepetitiveUpdater : Runnable {
            override fun run() {
                if (autoIncrement) {
                    updateBPM(true)
                    repeatUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY_FOR_LONG_CLICKS)
                } else if (autoDecrement) {
                    updateBPM(false)
                    repeatUpdateHandler.postDelayed(RepetitiveUpdater(), REPEAT_DELAY_FOR_LONG_CLICKS)
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
            logInfo("metronomeToggle", "BEFORE metronomeState = ${Metronome.metronomeState}")
            updateMetronomeStatus(isChecked)
            logInfo("metronomeToggle" ,"AFTER metronomeState = ${Metronome.metronomeState}")
        }

        increaseBPM.setOnClickListener {
            logInfo("increaseBPM.setOnClickListener", "Increasing BPM")
            updateBPM(true)
        }
        increaseBPM.setOnLongClickListener {
            logInfo("increaseBPM.setOnLongClickListener", "Increasing BPM until ACTION_UP")
            autoIncrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        increaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP && autoIncrement) {
                logInfo("increaseBPM.setOnTouchListener", "ACTION_UP triggered - Stop increasing BPM")
                autoIncrement = false
            }
            false
        }

        decreaseBPM.setOnClickListener {
            logInfo("decreaseBPM.setOnClickListener", "Decreasing BPM")
            updateBPM(false)
        }
        decreaseBPM.setOnLongClickListener {
            logInfo("decreaseBPM.setOnLongClickListener", "Decreasing BPM until ACTION_UP")
            autoDecrement = true
            repeatUpdateHandler.post(RepetitiveUpdater())
            false
        }
        decreaseBPM.setOnTouchListener { _, motionEvent ->
            if (motionEvent.action == MotionEvent.ACTION_UP && autoDecrement) {
                logInfo("decreaseBPM.setOnTouchListener", "ACTION_UP triggered - Stop decreasing BPM")
                autoDecrement = false
            }
            false
        }

    }

    override fun onStop() {
        super.onStop()
        Metronome.stop()
        metronomeToggle.isChecked = false   // Quick fix to stop metronome from playing again
    }

    private fun updateMetronomeStatus(turnOn: Boolean) {
        if (turnOn) {
            val currentBpm = getCurrentBpm()
            if (currentBpm != null && checkBpmBounds(currentBpm)) {
                startMetronome(currentBpm)
            }
        } else {
            stopMetronome()
        }
    }

    private fun startMetronome(bpm: BPM) {
        logInfo("startMetronome", "BEFORE metronomeState = ${Metronome.metronomeState}")
        Metronome.start(bpm)
        logInfo("startMetronome", "AFTER metronomeState = ${Metronome.metronomeState}")

        updateBpmButtons()
    }

    private fun stopMetronome() {
        logInfo("stopMetronome", "BEFORE metronomeState = ${Metronome.metronomeState}")
        Metronome.stop()
        logInfo("stopMetronome", "AFTER metronomeState = ${Metronome.metronomeState}")

        updateBpmButtons()
    }

    private fun getCurrentBpm(): BPM? {
        val currentBpmEditText = findViewById<EditText>(R.id.currentBPM)

        return if (currentBpmEditText.text.isNotEmpty()) {
            currentBpmEditText.text.toString().toLong()
        } else {
            null
        }
    }

    private fun checkBpmBounds(bpm: BPM): Boolean {
        return (bpm in METRONOME_LOWER_BOUND..METRONOME_UPPER_BOUND)
    }

    private fun showCurrentBpmError(showError: Boolean) {
        currentBPM.error = if (showError) "BPM must be between $METRONOME_LOWER_BOUND and $METRONOME_UPPER_BOUND" else null
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
        when (Metronome.metronomeState) {
            MetronomeState.ON -> {
                metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_on_background)
                metronomeToggle.setTextColor(getColor(R.color.black75Percent))
            }
            MetronomeState.OFF -> {
                metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_off_background)
                metronomeToggle.setTextColor(getColor(R.color.primaryTextColor))
            }
        }
    }

    private fun disableIncreaseBpm() {
        if (autoIncrement) {
            logInfo("disableIncreaseBpm", "IncreaseBPM button is disable - stop increasing BPM")
            autoIncrement = false
        }
        increaseBPM.isEnabled = false
        increaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
    }

    private fun disableDecreaseBpm() {
        if (autoDecrement) {
            logInfo("disableDecreaseBpm", "DecreaseBPM button is disable - stop decreasing BPM")
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

        when {
            currentBpm == null -> {
                disableIncreaseBpm()
                disableDecreaseBpm()
                disableMetronomeToggle()
            }
            currentBpm < METRONOME_LOWER_BOUND -> {
                enableIncreaseBpm()

                disableDecreaseBpm()
                disableMetronomeToggle()
            }
            currentBpm == METRONOME_LOWER_BOUND -> {
                enableMetronomeToggle()
                if (Metronome.isOff()) {
                    enableIncreaseBpm()
                    disableDecreaseBpm()
                    enableCurrentBpm()
                } else {
                    disableIncreaseBpm()
                    disableDecreaseBpm()
                    disableCurrentBpm()
                }
            }
            currentBpm < METRONOME_UPPER_BOUND -> {
                enableMetronomeToggle()
                if (Metronome.isOff()) {
                    enableIncreaseBpm()
                    enableDecreaseBpm()
                    enableCurrentBpm()
                } else {
                    disableIncreaseBpm()
                    disableDecreaseBpm()
                    disableCurrentBpm()
                }
            }
            currentBpm == METRONOME_UPPER_BOUND -> {
                enableMetronomeToggle()
                if (Metronome.isOff()) {
                    disableIncreaseBpm()
                    enableDecreaseBpm()
                    enableCurrentBpm()
                } else {
                    disableIncreaseBpm()
                    disableDecreaseBpm()
                    disableCurrentBpm()
                }
            }
            else -> {
                disableIncreaseBpm()
                enableDecreaseBpm()
                disableMetronomeToggle()
            }
        }
    }

    private fun updateBPM(increase: Boolean) {
        val currentBpm = getCurrentBpm()

        if (currentBpm != null) {
            val newBpm = if (increase) currentBpm + 1 else currentBpm - 1
            val allowUpdate = if (increase) newBpm <= METRONOME_UPPER_BOUND else newBpm >= METRONOME_LOWER_BOUND

            if (allowUpdate) {
                currentBPM.setText(newBpm.toString())
                showCurrentBpmError(!checkBpmBounds(newBpm))
            }
        }
    }
}
