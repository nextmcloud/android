package com.nmc.android.ui

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
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

    @Test
    fun validateUiOfFileDetailFragment() {
        show(file)

        onView(withId(R.id.header_image)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.filename)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.filename)).check(matches(withText("test.md")))
        onView(withId(R.id.favorite)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.file_size_label)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.size)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.file_modified_label)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.last_modification_timestamp)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.file_created_label)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.created_timestamp)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.file_uploaded_label)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.uploaded_timestamp)).check(matches(isCompletelyDisplayed()))
    }

    private fun validateCommonUI() {
        onView(withId(R.id.sharing_heading_title)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.sharing_heading_title)).check(matches(withText("Send link by mail")))

        onView(withId(R.id.searchView)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.searchView)).check(matches(isEnabled()))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(isEnabled()))

        onView(withId(R.id.or_section_layout)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.link_share_section_heading)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.link_share_section_heading)).check(matches(withText("Copy link")))

        onView(withId(R.id.share_create_new_link)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.share_create_new_link)).check(matches(withText("Create new link")))

        onView(withId(R.id.shared_with_divider)).check(matches(isCompletelyDisplayed()))

        onView(withId(R.id.tv_your_shares)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_your_shares)).check(matches(withText("Shared with")))
    }

    @Test
    fun validateUiForEmptyShares() {
        show(file)

        validateCommonUI()

        onView(withId(R.id.linkSharesList)).check(matches(not(isDisplayed())))

        onView(withId(R.id.sharesList)).check(matches(not(isDisplayed())))

        onView(withId(R.id.tv_empty_shares)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_empty_shares)).check(matches(withText("You have not yet shared your file/folder. Share to give others access.")))
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

        validateCommonUI()

        onView(withId(R.id.linkSharesList)).check(matches(isCompletelyDisplayed()))
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

        onView(withId(R.id.tv_resharing_info)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_resharing_info)).check(matches(withText("This file / folder was shared with you by John Doe")))

        onView(withId(R.id.tv_resharing_status)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_resharing_status)).check(matches(withText("Resharing is not allowed.")))

        onView(withId(R.id.searchView)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.searchView)).check(matches(not(isEnabled())))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.pick_contact_email_btn)).check(matches(not(isEnabled())))

        onView(withId(R.id.or_section_layout)).check(matches(not(isDisplayed())))
        onView(withId(R.id.link_share_section_heading)).check(matches(not(isDisplayed())))
        onView(withId(R.id.linkSharesList)).check(matches(not(isDisplayed())))
        onView(withId(R.id.share_create_new_link)).check(matches(not(isDisplayed())))
        onView(withId(R.id.shared_with_divider)).check(matches(not(isDisplayed())))
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

       validateCommonUI()

        onView(withId(R.id.tv_resharing_info)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_resharing_info)).check(matches(withText("This file / folder was shared with you by John Doe")))

        onView(withId(R.id.tv_resharing_status)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_resharing_status)).check(matches(withText("Resharing is allowed.")))

        onView(withId(R.id.linkSharesList)).check(matches(not(isDisplayed())))
        onView(withId(R.id.sharesList)).check(matches(not(isDisplayed())))
        onView(withId(R.id.tv_empty_shares)).check(matches(isCompletelyDisplayed()))
        onView(withId(R.id.tv_empty_shares)).check(matches(withText("You have not yet shared your file/folder. Share to give others access.")))
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
