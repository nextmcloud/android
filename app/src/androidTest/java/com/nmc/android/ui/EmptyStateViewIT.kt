package com.nmc.android.ui

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.NotificationsActivity
import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity
import com.owncloud.android.ui.activity.UploadListActivity
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SearchType
import com.owncloud.android.ui.trashbin.TrashbinActivity
import org.junit.Rule
import org.junit.Test

/**
 * test to validate empty state on different screens
 */
class EmptyStateViewIT : AbstractIT() {

    @get:Rule
    val testActivityRule = ActivityScenarioRule(TestActivity::class.java)

    @Test
    fun validate_emptyState_NoSearch() {
        loadOCFileListFragmentWithSearchType(SearchType.NO_SEARCH)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No files here")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Upload some content or sync with your devices.")))
    }

    @Test
    fun validate_emptyState_FileSearch() {
        loadOCFileListFragmentWithSearchType(SearchType.FILE_SEARCH)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No results")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Search for a file (at least 2 characters)")))
    }

    @Test
    fun validate_emptyState_FavoriteSearch() {
        loadOCFileListFragmentWithSearchType(SearchType.FAVORITE_SEARCH)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("Nothing favorited yet")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Files and folders you mark as favorites will show up here.")))
    }

    @Test
    fun validate_emptyState_RecentSearch() {
        loadOCFileListFragmentWithSearchType(SearchType.RECENTLY_MODIFIED_SEARCH)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No results")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Found no files modified within the last 7 days")))
    }

    @Test
    fun validate_emptyState_SharedSearch() {
        loadOCFileListFragmentWithSearchType(SearchType.SHARED_FILTER)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("Nothing shared yet")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Files and folders you share will show up here.")))
    }

    @Test
    fun validate_emptyState_GallerySearch() {
        loadGalleryFragment()
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No files here")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("No photos or videos uploaded yet")))
    }

    @Test
    fun validate_emptyState_Notification() {
        val activityScenario = ActivityScenario.launch(NotificationsActivity::class.java)
        waitForIdleSync()
        activityScenario.onActivity {
            it.runOnUiThread { it.populateList(ArrayList<Notification>()) }
        }

        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No notifications")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Please check back later.")))
    }

    @Test
    fun validate_errorState_Trashbin() {
        ActivityScenario.launch(TrashbinActivity::class.java)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("Error")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Loading trash bin failed!")))
    }

    @Test
    fun validate_emptyState_Trashbin() {
        val activityScenario = ActivityScenario.launch(TrashbinActivity::class.java)
        activityScenario.onActivity {
            it.showTrashbinFolder(emptyList())
        }
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No deleted files")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("You will be able to recover deleted files from here.")))
    }

    @Test
    fun validate_emptyState_FolderPicker() {
        ActivityScenario.launch(FolderPickerActivity::class.java)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No files here")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Upload some content or sync with your devices.")))
    }

    @Test
    fun validate_emptyState_ReceivedExternalFiles() {
        ActivityScenario.launch(ReceiveExternalFilesActivity::class.java)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No files here")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("")))
    }

    @Test
    fun validate_emptyState_UploadList() {
        ActivityScenario.launch(UploadListActivity::class.java)
        onView(withId(R.id.empty_list_icon)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_headline)).check(matches(withText("No uploads available")))
        onView(withId(R.id.empty_list_view_text)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.empty_list_view_text)).check(matches(withText("Upload some content or activate auto upload.")))
    }

    private fun loadOCFileListFragmentWithSearchType(searchType: SearchType) {
        testActivityRule.scenario.onActivity {
            it.addFragment(OCFileListFragment())
        }

        waitForIdleSync()

        testActivityRule.scenario.onActivity {
            val fragment = (it.fragment as OCFileListFragment)
            fragment.setEmptyListMessage(searchType)
        }
    }

    private fun loadGalleryFragment() {
        testActivityRule.scenario.onActivity {
            it.addFragment(GalleryFragment())
        }

        waitForIdleSync()

        testActivityRule.scenario.onActivity {
            val fragment = (it.fragment as GalleryFragment)
            fragment.setEmptyListMessage(SearchType.GALLERY_SEARCH)
        }
    }
}