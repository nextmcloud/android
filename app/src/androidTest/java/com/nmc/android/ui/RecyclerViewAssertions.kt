package com.nmc.android.ui

import android.content.res.Resources
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher

object RecyclerViewAssertions {

    fun clickChildViewWithId(id: Int): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): Matcher<View>? {
                return null
            }

            override fun getDescription(): String {
                return "Click on a child view with specified id."
            }

            override fun perform(uiController: UiController?, view: View) {
                val v: View = view.findViewById(id)
                v.performClick()
            }
        }
    }

    fun withRecyclerView(recyclerViewId: Int): RecyclerViewMatcher {
        return RecyclerViewMatcher(recyclerViewId)
    }

    class RecyclerViewMatcher(private val recyclerViewId: Int) {
        fun atPosition(position: Int): Matcher<View> {
            return atPositionOnView(position, -1)
        }

        fun atPositionOnView(position: Int, targetViewId: Int): Matcher<View> {
            return object : TypeSafeMatcher<View>() {
                var resources: Resources? = null
                var childView: View? = null

                override fun describeTo(description: Description?) {
                    var idDescription = recyclerViewId.toString()
                    resources?.let {
                        idDescription = try {
                            resources!!.getResourceName(recyclerViewId)
                        } catch (exception: Resources.NotFoundException) {
                            "$recyclerViewId (resource name not found)"
                        }
                    }

                    description?.appendText("with id: $idDescription")
                }

                override fun matchesSafely(view: View?): Boolean {
                    resources = view?.resources

                    if (childView == null) {
                        val recyclerView = view?.rootView?.findViewById<RecyclerView>(recyclerViewId)

                        if (recyclerView != null && recyclerView.id == recyclerViewId) {
                            childView = recyclerView.findViewHolderForAdapterPosition(position)?.itemView
                        } else {
                            return false
                        }
                    }

                    return if (targetViewId == -1) {
                        view == childView
                    } else {
                        val targetView = childView?.findViewById<View>(targetViewId)
                        view == targetView
                    }
                }
            }
        }
    }
}