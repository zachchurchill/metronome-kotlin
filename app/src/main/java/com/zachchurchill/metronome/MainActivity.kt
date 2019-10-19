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

import kotlinx.android.synthetic.main.activity_main.*


typealias BPM = Long

const val REPEAT_DELAY_FOR_LONG_CLICKS: Long = 50L
const val METRONOME_LOWER_BOUND: BPM = 40
const val METRONOME_UPPER_BOUND: BPM = 210

enum class MetronomeState {
    OFF,
    ON
}


object Metronome {
    private const val METRONOME_TONE = ToneGenerator.TONE_PROP_BEEP
    private const val MILLISECONDS_IN_SECOND: Int = 1000
    private const val SECONDS_IN_MINUTE: Int = 60

    private var metronomeState: MetronomeState
    private var metronome: Timer

    init {
        metronome = Timer("metronome", true)
        metronomeState = MetronomeState.OFF
    }

    private fun createNewTimer() {
        if (this.isOff()) {
            this.metronome = Timer("metronome", true)
        }
    }

    private fun calculateSleepDuration(bpm: BPM): Long {
        return (MILLISECONDS_IN_SECOND * (SECONDS_IN_MINUTE / bpm.toDouble())).toLong()
    }

    fun start(bpm: BPM): Boolean {
        if (this.isOn()) {
            return false
        }

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

        return true
    }

    fun stop(): Boolean {
        if (this.isOff()) {
            return false
        }

        this.metronomeState = MetronomeState.OFF
        this.metronome.cancel()
        createNewTimer()

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

    // These private variables help keep track of increasing/decreasing the BPM
    private var autoIncrement: Boolean = false
    private var autoDecrement: Boolean = false
    private var repeatUpdateHandler: Handler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
            updateMetronomeStatus(isChecked)
        }

        increaseBPM.setOnClickListener {
            updateBPM(true)
        }
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

        decreaseBPM.setOnClickListener {
            updateBPM(false)
        }
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

    private fun updateMetronomeStatus(turnOn: Boolean) {
        if (turnOn) {
            val currentBpm = getCurrentBpm()
            if (currentBpm != null && checkBpmBounds(currentBpm)) {
                Metronome.start(currentBpm)
            }
        } else {
            Metronome.stop()
        }

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
        if (Metronome.isOn()) {
            metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_on_background)
            metronomeToggle.setTextColor(getColor(R.color.black75Percent))
        } else {
            metronomeToggle.setBackgroundResource(R.drawable.btn_toggled_off_background)
            metronomeToggle.setTextColor(getColor(R.color.primaryTextColor))
        }
    }

    private fun disableIncreaseBpm() {
        autoIncrement = false
        increaseBPM.isEnabled = false
        increaseBPM.setBackgroundResource(R.drawable.btn_disabled_background)
    }

    private fun disableDecreaseBpm() {
        autoDecrement = false
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
