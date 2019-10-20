package com.zachchurchill.metronome

import android.content.pm.ActivityInfo
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import org.hamcrest.Matchers.*

@RunWith(AndroidJUnit4ClassRunner::class)
@LargeTest
class MainActivityUITest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivity::class.java)

    @Test fun appStartsAt60BpmWithAllButtonsEnabled() {
        onView(withText("60")).check(matches(isDisplayed()))

        // Enabled Buttons
        onView(withId(R.id.increaseBPM))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_dark_background.toString()))))

        onView(withId(R.id.decreaseBPM))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_dark_background.toString()))))

        onView(withId(R.id.metronomeToggle))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_toggled_off_background.toString()))))

        // Disabled Buttons
    }

    @Test fun nullCurrentBpmShowsErrorAndDisablesAllButtons() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .check(matches(hasErrorText(startsWith("BPM must be between"))))

        // Enabled Buttons

        // Disabled Buttons
        onView(withId(R.id.increaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))

        onView(withId(R.id.decreaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))

        onView(withId(R.id.metronomeToggle))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_toggled_disabled_background.toString()))))
    }

    @Test fun currentBpmLTLowerThresholdShowsErrorAndDisablesToggleAndDecreaseButtons() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText((Metronome.BPM_LOWER_THRESHOLD- 5).toString()))
            .check(matches(hasErrorText(startsWith("BPM must be between"))))

        // Enabled Buttons
        onView(withId(R.id.increaseBPM))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_dark_background.toString()))))

        // Disabled Buttons
        onView(withId(R.id.decreaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))

        onView(withId(R.id.metronomeToggle))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_toggled_disabled_background.toString()))))
    }

    @Test fun currentBpmEqualToLowerThresholdDisablesDecreaseButtons() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText(Metronome.BPM_LOWER_THRESHOLD.toString()))

        // Enabled Buttons
        onView(withId(R.id.increaseBPM))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_dark_background.toString()))))

        onView(withId(R.id.metronomeToggle))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_toggled_off_background.toString()))))

        // Disabled Buttons
        onView(withId(R.id.decreaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))
    }

    @Test fun currentBpmGTUpperThresholdShowsErrorAndDisablesToggleAndIncreaseButtons() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText((Metronome.BPM_UPPER_THRESHOLD + 5).toString()))
            .check(matches(hasErrorText(startsWith("BPM must be between"))))

        // Enabled Buttons
        onView(withId(R.id.decreaseBPM))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_dark_background.toString()))))

        // Disabled Buttons
        onView(withId(R.id.increaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))

        onView(withId(R.id.metronomeToggle))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_toggled_disabled_background.toString()))))
    }

    @Test fun currentBpmEqualToUpperThresholdDisablesIncreaseButtons() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText(Metronome.BPM_UPPER_THRESHOLD.toString()))

        // Enabled Buttons
        onView(withId(R.id.decreaseBPM))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_dark_background.toString()))))

        onView(withId(R.id.metronomeToggle))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_toggled_off_background.toString()))))

        // Disabled Buttons
        onView(withId(R.id.increaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))
    }

    @Test fun singlePressOfIncreaseButtonIncrementsCurrentBpmByOne() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText("60"))

        onView(withId(R.id.increaseBPM))
            .perform(click())

        onView(withId(R.id.currentBPM))
            .check(matches(withText("61")))
    }

    @Test fun longPressOfIncreaseButtonIncrementsCurrentBpmByMoreThanOne() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText("60"))

        onView(withId(R.id.increaseBPM))
            .perform(longClick())

        onView(withId(R.id.currentBPM))
            .check(matches(withText(greaterThan("61"))))
    }

    @Test fun singlePressOfDecreaseButtonDecrementsCurrentBpmByOne() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText("60"))

        onView(withId(R.id.decreaseBPM))
            .perform(click())

        onView(withId(R.id.currentBPM))
            .check(matches(withText("59")))
    }

    @Test fun longPressOfDecreaseButtonDecrementsCurrentBpmByMoreThanOne() {
        onView(withId(R.id.currentBPM))
            .perform(clearText())
            .perform(typeText("60"))

        onView(withId(R.id.decreaseBPM))
            .perform(longClick())

        onView(withId(R.id.currentBPM))
            .check(matches(withText(lessThan("59"))))
    }

    @Test fun allOtherUiComponentsDisabledWhenMetronomeToggledOn() {
        onView(withId(R.id.metronomeToggle))
            .perform(click())

        Thread.sleep(500)

        // Disabled Buttons
        onView(withId(R.id.increaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))

        onView(withId(R.id.decreaseBPM))
            .check(matches(not(isEnabled())))
            .check(matches(withTagValue(hasToString(R.drawable.btn_disabled_background.toString()))))

        // Disabled BPM
        onView(withId(R.id.currentBPM))
            .check(matches(not(isEnabled())))
            .check(matches(hasTextColor(R.color.colorAccent50Percent)))

        // Clean-up
        onView(withId(R.id.metronomeToggle))
            .perform(click())
    }

    @Test fun metronomeStaysOnAfterScreenFlip() {
        onView(withId(R.id.metronomeToggle))
            .perform(click())

        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        Thread.sleep(500)
        onView(withId(R.id.metronomeToggle))
            .check(matches(isEnabled()))
            .check(matches(withTagValue(hasToString(R.drawable.btn_toggled_on_background.toString()))))

        // Clean-up
        onView(withId(R.id.metronomeToggle))
            .perform(click())

        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}
