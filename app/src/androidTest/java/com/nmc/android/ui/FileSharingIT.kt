package com.nmc.android.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.nextcloud.test.RetryTestRule
import com.nextcloud.test.TestActivity
import com.nmc.android.ui.RecyclerViewAssertions.clickChildViewWithId
import com.nmc.android.ui.RecyclerViewAssertions.withRecyclerView
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileDetailSharingFragment
import com.owncloud.android.ui.fragment.util.SharingMenuHelper
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FileSharingIT : AbstractIT() {
    @get:Rule
    val activityScenarioRule = ActivityScenarioRule(TestActivity::class.java)

    @get:Rule
    val retryRule = RetryTestRule()

    lateinit var file: OCFile
    lateinit var folder: OCFile

    @Before
    fun before() {
        activityScenarioRule.scenario.onActivity {
            file = OCFile("/test.md").apply {
                remoteId = "00000001"
                parentId = it.storageManager.getFileByEncryptedRemotePath("/").fileId
                permissions = OCFile.PERMISSION_CAN_RESHARE
            }

            folder = OCFile("/test").apply {
                setFolder()
                remoteId = "00000002"
                parentId = it.storageManager.getFileByEncryptedRemotePath("/").fileId
                permissions = OCFile.PERMISSION_CAN_RESHARE
            }
        }
    }

    private fun show(file: OCFile) {
        val fragment = FileDetailFragment.newInstance(file, user, 0)

        activityScenarioRule.scenario.onActivity {
            it.addFragment(fragment)
        }

        waitForIdleSync()

        shortSleep()
    }

    private fun getFragment(): FileDetailSharingFragment? {
        var fragment: FileDetailSharingFragment? = null
        activityScenarioRule.scenario.onActivity {
            fragment =
                it.supportFragmentManager.findFragmentByTag("SHARING_DETAILS_FRAGMENT") as FileDetailSharingFragment
        }
        return fragment
    }

    @Test
    fun validateUiOfFileDetailFragment() {
        show(file)

        onView(withId(R.id.filename)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.filename)).check(matches(withText("test.md")))
        onView(withId(R.id.favorite)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.size)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.file_separator)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.last_modification_timestamp)).check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun validateUiForEmptyShares() {
        show(file)

        onView(withId(R.id.shared_with_you_container)).check(matches(not(isCompletelyDisplayed())))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(withText("You can create links or send shares by mail. If you invite MagentaCLOUD users, you have more opportunities for collaboration.")))
        onView(withId(R.id.searchView)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.label_personal_share)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.label_personal_share)).check(matches(withText("Personal share by mail")))

        onView(withId(R.id.share_create_new_link)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.share_create_new_link)).check(matches(withText("Create Link")))

        onView(withId(R.id.tv_your_shares)).check(matches(not(isDisplayed())))
        onView(withId(R.id.sharesList)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tv_empty_shares)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_empty_shares)).check(matches(withText("No shares created yet.")))
    }

    @Test
    fun validateUiForFileWithShares() {
        activityScenarioRule.scenario.onActivity {
            OCShare(file.decryptedRemotePath).apply {
                remoteId = 1
                shareType = ShareType.USER
                sharedWithDisplayName = "Admin"
                permissions = OCShare.READ_PERMISSION_FLAG
                userId = getUserId(user)
                it.storageManager.saveShare(this)
            }

            OCShare(file.decryptedRemotePath).apply {
                remoteId = 3
                shareType = ShareType.EMAIL
                permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
                sharedWithDisplayName = "johndoe@gmail.com"
                userId = getUserId(user)
                it.storageManager.saveShare(this)
            }

            OCShare(file.decryptedRemotePath).apply {
                remoteId = 4
                shareType = ShareType.PUBLIC_LINK
                permissions = OCShare.READ_PERMISSION_FLAG
                label = "Customer"
                it.storageManager.saveShare(this)
            }

            OCShare(file.decryptedRemotePath).apply {
                remoteId = 5
                shareType = ShareType.PUBLIC_LINK
                permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
                label = "Colleagues"
                it.storageManager.saveShare(this)
            }

        }
        show(file)

        onView(withId(R.id.shared_with_you_container)).check(matches(not(isCompletelyDisplayed())))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(withText("You can create links or send shares by mail. If you invite MagentaCLOUD users, you have more opportunities for collaboration.")))
        onView(withId(R.id.searchView)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.label_personal_share)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.label_personal_share)).check(matches(withText("Personal share by mail")))

        onView(withId(R.id.share_create_new_link)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.share_create_new_link)).check(matches(withText("Create Link")))

        onView(withId(R.id.tv_your_shares)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_your_shares)).check(matches(withText("Your Shares")))
        onView(withId(R.id.sharesList)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_empty_shares)).check(matches(not(isDisplayed())))
    }

    @Test
    fun validateUiWithResharingNotAllowed() {
        file = file.apply {
            permissions = ""
            ownerDisplayName = "John Doe"
            ownerId = "JohnDoe"
            note = "Shared for testing purpose."
        }
        show(file)

        onView(withId(R.id.shared_with_you_container)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(withText("Resharing is not allowed.")))
        onView(withId(R.id.shared_with_you_username)).check(matches(withText("Shared with you by John Doe")))
        onView(withId(R.id.shared_with_you_note)).check(matches(withText("Shared for testing purpose.")))
        onView(withId(R.id.shared_with_you_avatar)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.searchView)).check(matches(not(isDisplayed())))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(not(isDisplayed())))
        onView(withId(R.id.label_personal_share)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_create_new_link)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tv_your_shares)).check(matches(not(isDisplayed())))
        onView(withId(R.id.sharesList)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tv_empty_shares)).check(matches(not(isDisplayed())))
    }

    @Test
    fun validateUiWithResharingAllowed() {
        file = file.apply {
            ownerDisplayName = "John Doe"
            ownerId = "JohnDoe"
        }
        show(file)

        onView(withId(R.id.shared_with_you_container)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_sharing_details_message)).check(matches(withText("Resharing is allowed. You can create links or send shares by mail. If you invite MagentaCLOUD users, you have more opportunities for collaboration.")))
        onView(withId(R.id.shared_with_you_username)).check(matches(withText("Shared with you by John Doe")))
        onView(withId(R.id.shared_with_you_avatar)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.shared_with_you_note_container)).check(matches(not(isDisplayed())))

        onView(withId(R.id.searchView)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.label_personal_share)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.share_create_new_link)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_your_shares)).check(matches(not(isDisplayed())))
        onView(withId(R.id.sharesList)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tv_empty_shares)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_empty_shares)).check(matches(withText("No shares created yet.")))
    }

    @Test
    fun validateQuickPermissionDialogForFiles() {
        val sharesList: MutableList<OCShare> = mutableListOf()

        activityScenarioRule.scenario.onActivity { it ->
            OCShare(file.decryptedRemotePath).apply {
                remoteId = 1
                shareType = ShareType.USER
                sharedWithDisplayName = "Admin"
                permissions = OCShare.READ_PERMISSION_FLAG
                userId = getUserId(user)
                isFolder = false
                it.storageManager.saveShare(this)
            }

            OCShare(file.decryptedRemotePath).apply {
                remoteId = 3
                shareType = ShareType.EMAIL
                sharedWithDisplayName = "johndoe@gmail.com"
                permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FILE
                userId = getUserId(user)
                isFolder = false
                it.storageManager.saveShare(this)
            }

            OCShare(file.decryptedRemotePath).apply {
                remoteId = 4
                shareType = ShareType.PUBLIC_LINK
                permissions = OCShare.READ_PERMISSION_FLAG
                userId = getUserId(user)
                label = "Customer"
                isFolder = false
                it.storageManager.saveShare(this)
            }

            //get other shares
            sharesList.addAll(it.storageManager.getSharesWithForAFile(file.remotePath, user.accountName))

            //get public link shares
            sharesList.addAll(it.storageManager.getSharesByPathAndType(file.remotePath, ShareType.PUBLIC_LINK, ""))

            sharesList.sortByDescending { share -> share.shareType }
        }


        assertEquals(3, sharesList.size)

        show(file)

        for (i in sharesList.indices) {
            val share = sharesList[i]
            //since for public link the quick permission button is disabled
            if (share.shareType == ShareType.PUBLIC_LINK) {
                continue
            }
            showQuickPermissionDialogAndValidate(i, file.isFolder, share)
            pressBack()
        }
    }

    @Test
    fun validateQuickPermissionDialogForFolder() {
        val sharesList: MutableList<OCShare> = mutableListOf()

        activityScenarioRule.scenario.onActivity { it ->
            OCShare(folder.decryptedRemotePath).apply {
                remoteId = 1
                shareType = ShareType.USER
                sharedWithDisplayName = "Admin"
                permissions = OCShare.CREATE_PERMISSION_FLAG
                userId = getUserId(user)
                isFolder = true
                it.storageManager.saveShare(this)
            }

            OCShare(folder.decryptedRemotePath).apply {
                remoteId = 3
                shareType = ShareType.EMAIL
                sharedWithDisplayName = "johndoe@gmail.com"
                permissions = SharingMenuHelper.CAN_EDIT_PERMISSIONS_FOR_FOLDER
                userId = getUserId(user)
                isFolder = true
                it.storageManager.saveShare(this)
            }

            OCShare(folder.decryptedRemotePath).apply {
                remoteId = 4
                shareType = ShareType.PUBLIC_LINK
                permissions = OCShare.READ_PERMISSION_FLAG
                userId = getUserId(user)
                label = "Customer"
                isFolder = true
                it.storageManager.saveShare(this)
            }

            //get other shares
            sharesList.addAll(it.storageManager.getSharesWithForAFile(folder.remotePath, user.accountName))

            //get public link shares
            sharesList.addAll(it.storageManager.getSharesByPathAndType(folder.remotePath, ShareType.PUBLIC_LINK, ""))

            sharesList.sortByDescending { share -> share.shareType }
        }


        assertEquals(3, sharesList.size)

        show(folder)

        for (i in sharesList.indices) {
            val share = sharesList[i]
            showQuickPermissionDialogAndValidate(i, folder.isFolder, share)
            pressBack()
        }
    }

    private fun showQuickPermissionDialogAndValidate(index: Int, isFolder: Boolean, ocShare: OCShare) {
        onView(withId(R.id.sharesList)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                index,
                clickChildViewWithId(if (ocShare.shareType == ShareType.USER) R.id.share_name_layout else R.id.share_by_link_container)
            )
        )

        val permissionList = permissionList(isFolder, ocShare.shareType!!)

        for (i in permissionList.indices) {
            // Scroll to the item at position i
            onView(withId(R.id.rv_quick_share_permissions)).perform(
                RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(
                    i
                )
            )

            val permissionTextView = onView(
                withRecyclerView(R.id.rv_quick_share_permissions)
                    .atPositionOnView(i, R.id.tv_quick_share_name)
            )
            permissionTextView.check(matches(withText(permissionList[i])))

            val permissionCheckView = onView(
                withRecyclerView(R.id.rv_quick_share_permissions)
                    .atPositionOnView(i, R.id.tv_quick_share_check_icon)
            )
            if ((permissionList[i] == "Read only" && SharingMenuHelper.isReadOnly(ocShare))
                || (permissionList[i] == "Can edit" && SharingMenuHelper.isUploadAndEditingAllowed(ocShare))
                || (permissionList[i] == "Filedrop only" && SharingMenuHelper.isFileDrop(ocShare))
            ) {
                permissionCheckView.check(matches(isDisplayed()))
            }
        }
    }

    @After
    override fun after() {
        activityScenarioRule.scenario.onActivity {
            it.storageManager.cleanShares()
            it.finish()
        }
        super.after()
    }

    companion object {
        private val filePermissionList = listOf("Read only", "Can edit")
        private val folderExternalAndLinkSharePermissionList = listOf("Read only", "Can edit", "Filedrop only")
        private val folderOtherSharePermissionList = listOf("Read only", "Can edit")

        fun permissionList(isFolder: Boolean, shareType: ShareType): List<String> =
            if (isFolder) {
                if (shareType == ShareType.PUBLIC_LINK || shareType == ShareType.EMAIL) folderExternalAndLinkSharePermissionList
                else folderOtherSharePermissionList
            } else filePermissionList
    }
}
