package com.nmc.android.ui

import android.text.Spannable
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.Description
import org.hamcrest.Matcher

object ClickableSpanTestHelper {

    /**
     * method to get clickable span form a text view
     * example: val clickableSpan = getClickableSpan("Link text", onView(withId(R.id.text_id)))
     */
    fun getClickableSpan(spanText: String, matcher: ViewInteraction?): ClickableSpan? {
        val clickableSpans = arrayOf<ClickableSpan?>(null)

        // Get the SpannableString from the TextView
        matcher?.check(matches(isDisplayed()))
        matcher?.perform(object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return isAssignableFrom(TextView::class.java)
            }

            override fun getDescription(): String {
                return "get text from TextView"
            }

            override fun perform(uiController: UiController, view: View) {
                val textView = view as TextView
                val text = textView.text
                if (text is Spannable) {
                    val spans = text.getSpans(
                        0, text.length,
                        ClickableSpan::class.java
                    )
                    for (span in spans) {
                        val start = text.getSpanStart(span)
                        val end = text.getSpanEnd(span)
                        val spanString = text.subSequence(start, end).toString()
                        if (spanString == spanText) {
                            clickableSpans[0] = span
                            return
                        }
                    }
                }
                throw java.lang.RuntimeException("ClickableSpan not found")
            }
        })
        return clickableSpans[0]
    }

    /**
     * perform click on the spanned string
     * @link getClickableSpan() method to get clickable span
     */
    fun performClickSpan(clickableSpan: ClickableSpan?): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(TextView::class.java)
            }

            override fun getDescription(): String {
                return "clicking on a span"
            }

            override fun perform(uiController: UiController, view: View) {
                val textView = view as TextView
                val spannable = textView.text as Spannable
                val spans = spannable.getSpans(
                    0, spannable.length,
                    ClickableSpan::class.java
                )
                for (span in spans) {
                    if (span == clickableSpan) {
                        span.onClick(textView)
                        return
                    }
                }
                throw RuntimeException("ClickableSpan not found")
            }
        }
    }

    fun verifyClickSpan(clickableSpan: ClickableSpan?): Matcher<View?> {
        return object : BoundedMatcher<View?, TextView>(TextView::class.java) {
            override fun describeTo(description: Description) {
                description.appendText("clickable span")
            }

            override fun matchesSafely(textView: TextView): Boolean {
                val spannable = textView.text as Spannable
                val spans = spannable.getSpans(
                    0, spannable.length,
                    ClickableSpan::class.java
                )
                for (span in spans) {
                    if (span == clickableSpan) {
                        return true
                    }
                }
                return false
            }
        }
    }
}