package com.elpassion.mspek

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.rule.ActivityTestRule
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.startsWith
import org.junit.runner.RunWith
import com.elpassion.mspek.MiniSpek.o
import com.elpassion.mspek.MiniSpek.mspek

@RunWith(MiniSpekRunner::class)
class AndroCalcInstrumentedTest {

    init {
        val activityRule = ActivityTestRule<com.elpassion.mspek.MainActivity>(com.elpassion.mspek.MainActivity::class.java, false, false)
        mspek("andro tests") {

            "on main activity" o {

                try {
                    activityRule.finishActivity()
                } catch (e: IllegalStateException) { }

                activityRule.launchActivity(null)

                "it should display some hello message" o {
                    onView(withId(com.elpassion.mspek.R.id.helloTextView)).check(matches(withText(startsWith("Hello"))))
                }

                "on click on hello text two times" o {

                    onView(withId(com.elpassion.mspek.R.id.helloTextView)).perform(click())
                    onView(withId(com.elpassion.mspek.R.id.helloTextView)).perform(click())

                    "it should display 9 at the end of hello text" o {
                        onView(withId(com.elpassion.mspek.R.id.helloTextView)).check(matches(withText(endsWith("9"))))
                    }
                }

                "on click on hello text four times" o {

                    repeat(4) { onView(withId(com.elpassion.mspek.R.id.helloTextView)).perform(click()) }

                    "it should display 81 at the end of hello text" o {
                        onView(withId(com.elpassion.mspek.R.id.helloTextView)).check(matches(withText(endsWith("81"))))
                    }
                }
            }
        }
    }
}
