/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import android.Manifest
import android.accounts.AccountManager
import android.app.Activity
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.InputType
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.VisibleForTesting
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.appbar.AppBarLayout
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.EditorUtils
import com.nextcloud.client.preferences.AppPreferences
import com.nextcloud.client.utils.IntentUtil
import com.nextcloud.utils.extensions.getParcelableArgument
import com.nextcloud.utils.mdm.MDMConfig.shareViaUser
import com.nmc.android.utils.DisplayUtils.isLandscapeOrientation
import com.nmc.android.utils.SearchViewThemeUtils
import com.nmc.android.marketTracking.AdjustSdkUtils
import com.nmc.android.marketTracking.TealiumSdkUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsSharingFragmentBinding
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.status.NextcloudVersion
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.providers.UsersAndGroupsSearchConfig
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.adapter.ShareeListAdapter
import com.owncloud.android.ui.adapter.ShareeListAdapterListener
import com.owncloud.android.ui.asynctasks.RetrieveHoverCardAsyncTask
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment.Companion.newInstance
import com.owncloud.android.ui.events.ShareSearchViewFocusEvent
import com.owncloud.android.ui.fragment.QuickSharingPermissionsBottomSheetDialog.QuickPermissionSharingBottomSheetActions
import com.owncloud.android.ui.fragment.share.RemoteShareRepository
import com.owncloud.android.ui.fragment.util.FileDetailSharingFragmentHelper
import com.owncloud.android.ui.fragment.util.SharePermissionManager
import com.owncloud.android.ui.helpers.FileOperationsHelper
import com.owncloud.android.utils.ClipboardUtil.copyToClipboard
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.PermissionUtil.checkSelfPermission
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass", "TooGenericExceptionCaught", "ReturnCount")
class FileDetailSharingFragment :
    Fragment(),
    ShareeListAdapterListener,
    AvatarGenerationListener,
    Injectable,
    FileDetailsSharingMenuBottomSheetActions,
    QuickPermissionSharingBottomSheetActions {
    private var file: OCFile? = null
    private var user: User? = null
    private var capabilities: OCCapability? = null

    private var fileOperationsHelper: FileOperationsHelper? = null
    private var fileActivity: FileActivity? = null
    private var fileDataStorageManager: FileDataStorageManager? = null

    private var binding: FileDetailsSharingFragmentBinding? = null

    private var onEditShareListener: OnEditShareListener? = null

    private var internalShareeListAdapter: ShareeListAdapter? = null

    private var externalShareeListAdapter: ShareeListAdapter? = null

    private var isSearchViewFocused = false

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    @Inject
    lateinit var editorUtils: EditorUtils

    @Inject
    lateinit var searchConfig: UsersAndGroupsSearchConfig

    @Inject
    lateinit var appPreferences: AppPreferences

    // region lifecycle methods
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initArguments(savedInstanceState)
        fileActivity = (activity as FileActivity?)

        requireNotNull(file) { "File may not be null" }
        requireNotNull(user) { "Account may not be null" }
        requireNotNull(fileActivity) { "FileActivity may not be null" }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fileActivity ?: return
        fileDataStorageManager = fileActivity?.storageManager
        fileOperationsHelper = fileActivity?.fileOperationsHelper

        startAnimation()

        val userId = getUserId()

        setupInternalShares(userId)
        setupExternalShares(userId)

        binding?.pickContactEmailBtn?.setOnClickListener { checkContactPermission() }

        binding?.shareCreateNewLink?.setOnClickListener { _ -> createPublicShareLink() }

        // remove focus from search view on click of root view
        binding?.shareContainer?.setOnClickListener { _ -> binding?.searchView?.clearFocus() }

        // enable-disable scrollview scrolling
        binding?.fileDetailsNestedScrollView?.setOnTouchListener { _, _ -> isLandscapeOrientation() && isSearchViewFocused }

        fetchSharees()
        setupView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FileDetailsSharingFragmentBinding.inflate(inflater, container, false)
        return binding!!.getRoot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        require(activity is FileActivity) { "Calling activity must be of type FileActivity" }

        try {
            onEditShareListener = context as OnEditShareListener
        } catch (e: Exception) {
            throw IllegalArgumentException("Calling activity must implement the interface$e")
        }
    }

    override fun onStart() {
        super.onStart()
        searchConfig.searchOnlyUsers = (file?.isEncrypted == true)
    }

    override fun onStop() {
        super.onStop()
        searchConfig.reset()
    }
    // endregion

    // region private methods
    private fun initArguments(savedInstanceState: Bundle?) {
        val args = (savedInstanceState ?: arguments) ?: return
        file = args.getParcelableArgument(ARG_FILE, OCFile::class.java)
        user = args.getParcelableArgument(ARG_USER, User::class.java)
    }

    private fun getUserId(): String {
        val accountManager = AccountManager.get(requireContext())
        return accountManager.getUserData(
            user?.toPlatformAccount(),
            AccountUtils.Constants.KEY_USER_ID
        )
    }

    private fun setupInternalShares(userId: String) {
        internalShareeListAdapter = createShareListAdapter(userId)
        binding?.sharesList?.run {
            adapter = internalShareeListAdapter
            layoutManager = createShareListLayoutManager()
        }
    }

    private fun setupExternalShares(userId: String) {
        externalShareeListAdapter = createShareListAdapter(userId)
        binding?.linkSharesList?.run {
            adapter = externalShareeListAdapter
            layoutManager = createShareListLayoutManager()
        }
    }

    private fun createShareListAdapter(userId: String): ShareeListAdapter = ShareeListAdapter(
        fileActivity!!,
        ArrayList(),
        this,
        userId,
        user,
        viewThemeUtils,
        (file?.isEncrypted == true)
    ).apply {
        setHasStableIds(true)
    }

    private fun createShareListLayoutManager(): LinearLayoutManager = LinearLayoutManager(requireContext())

    private fun startAnimation() {
        val blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)
        binding?.shimmerLayout?.getRoot()?.startAnimation(blinkAnimation)
    }

    private fun fetchSharees() {
        val activity = fileActivity ?: return
        val clientRepository = activity.clientRepository ?: return
        val storageManager = fileDataStorageManager ?: return
        val remotePath = file?.remotePath ?: return

        val shareRepository = RemoteShareRepository(clientRepository, storageManager)
        lifecycleScope.launch {
            val result = shareRepository.fetchSharees(remotePath)
            if (binding == null) {
                return@launch
            }

            if (result) {
                refreshCapabilitiesFromDB()
                refreshSharesFromDB()
                stopLoadingAnimationAndShowShareContainer()
                return@launch
            }

            stopLoadingAnimationAndShowShareContainer()
            DisplayUtils.showSnackMessage(this@FileDetailSharingFragment, R.string.error_fetching_sharees)
        }
    }

    private fun stopLoadingAnimationAndShowShareContainer() {
        binding?.run {
            shimmerLayout.root.run {
                clearAnimation()
                visibility = View.GONE
            }
            shareContainer.visibility = View.VISIBLE
        }
    }

    private fun setupView() {
        binding?.run {
            FileDetailSharingFragmentHelper.setupSearchView(
                fileActivity?.getSystemService(Context.SEARCH_SERVICE) as SearchManager?,
                searchView,
                fileActivity?.componentName
            )

            themeView(this)
            setupShareView(this)

            setShareWithYou()

            checkShareViaUser()
        }
    }

    private fun themeView(binding: FileDetailsSharingFragmentBinding) {
        binding.run {
            SearchViewThemeUtils.themeSearchView(requireContext(), searchView)
        }
    }

    /**
     * @param isDeviceRotated true when user rotated the device and false when user is already in landscape mode
     */
    private fun scrollToSearchViewPosition(isDeviceRotated: Boolean) {
        if (isLandscapeOrientation()) {
            if (isSearchViewFocused) {
                binding?.fileDetailsNestedScrollView?.post {

                }
                binding?.run {
                    fileDetailsNestedScrollView.post {

                        //need to hide app bar to have more space in landscape mode while search view is focused
                        hideAppBar()

                        //send the event to hide the share top view to have more space
                        //need to use this here else white view will be visible for sometime
                        EventBus.getDefault().post(ShareSearchViewFocusEvent(isSearchViewFocused))

                        if (isDeviceRotated) {
                            // during the rotation we need to use getTop() method for proper alignment of search view
                            // -25 just to avoid blank space at top
                            fileDetailsNestedScrollView.smoothScrollTo(0, searchView.top - 20)
                        } else {
                            // when user is already in landscape mode and search view gets focus
                            // we need to user getBottom() method for proper alignment of search view
                            // -100 just to avoid blank space at top
                            fileDetailsNestedScrollView.smoothScrollTo(
                                0,
                                searchView.bottom - 100
                            )
                        }
                    }
                }
            } else {
                //send the event to show the share top view again
                EventBus.getDefault().post(ShareSearchViewFocusEvent(isSearchViewFocused))
            }
        } else {
            //in portrait mode we need to see the layout everytime
            //send the event to show the share top view
            EventBus.getDefault().post(ShareSearchViewFocusEvent(false))
        }
    }

    private fun hideAppBar() {
        if (requireActivity() is FileDisplayActivity) {
            val appBarLayout = requireActivity().findViewById<AppBarLayout?>(R.id.appbar)

            if (appBarLayout != null) {
                appBarLayout.setExpanded(false, true)
            }
        }
    }

    private fun setupShareView(binding: FileDetailsSharingFragmentBinding) {
        binding.run {
            searchView.setQueryHint(resources.getString(R.string.share_search))
            searchView.visibility = View.VISIBLE
            pickContactEmailBtn.visibility = View.VISIBLE

            searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
                isSearchViewFocused = hasFocus
                scrollToSearchViewPosition(false)
            }
        }
    }

    private fun setupDisabledShareView(binding: FileDetailsSharingFragmentBinding) {
        binding.run {
            searchView.inputType = InputType.TYPE_NULL
            pickContactEmailBtn.visibility = View.GONE
            binding.orSectionLayout.visibility = View.GONE
            binding.linkShareSectionHeading.visibility = View.GONE
            binding.linkSharesList.visibility = View.GONE
            binding.shareCreateNewLink.visibility = View.GONE

            binding.sharedWithDivider.visibility = View.GONE
            binding.tvYourShares.visibility = View.GONE
            binding.sharesList.visibility = View.GONE
            binding.tvEmptyShares.visibility = View.GONE

            binding.tvResharingStatus.text = resources.getString(R.string.reshare_not_allowed)
            toggleSearchViewEnable(searchView, false)
        }
    }

    private fun checkShareViaUser() {
        if (shareViaUser(requireContext())) {
            return
        }

        binding?.searchContainer?.visibility = View.GONE
    }

    private fun toggleSearchViewEnable(view: View, enable: Boolean) {
        view.isEnabled = enable
        if (view is ViewGroup) {
            for (i in 0..<view.size) {
                toggleSearchViewEnable(view.getChildAt(i), enable)
            }
        }
    }

    private fun setShareWithYou() {
        binding?.run {
            if (accountManager.userOwnsFile(file, user)) {
                tvResharingInfo.visibility = View.GONE
                tvResharingStatus.visibility = View.GONE
                return
            }

            tvResharingInfo.text = DisplayUtils.createTextWithSpan(
                String.format(getString(R.string.resharing_user_info), file?.ownerDisplayName),
                file?.ownerDisplayName,
                StyleSpan(Typeface.BOLD)
            )

            if (file?.canReshare() == true) {
                tvResharingStatus.text = resources.getString(R.string.reshare_allowed)
            } else {
                setupDisabledShareView(this)
            }

            tvResharingStatus.visibility = View.VISIBLE
            tvResharingInfo.visibility = View.VISIBLE
        }
    }

    /**
     * will be called from FileActivity when user is sharing from PreviewImageFragment
     *
     * @param shareeName
     * @param shareType
     */
    fun initiateSharingProcess(shareeName: String, shareType: ShareType, secureShare: Boolean) {
        val file = file ?: return
        val user = user ?: return
        val capabilities = capabilities ?: return
        requireActivity().supportFragmentManager.beginTransaction().replace(
            R.id.share_fragment_container,
            FileDetailsSharingProcessFragment.newInstance(
                file,
                shareeName,
                shareType,
                secureShare,
                SharePermissionManager.canEditFile(user, capabilities, file, editorUtils)
            ),
            FileDetailsSharingProcessFragment.TAG
        )
            .addToBackStack(null)
            .commit()
    }

    /**
     * open the new sharing screen process to modify the created share this will be called from PreviewImageFragment
     *
     * @param share
     * @param screenTypePermission
     * @param isReshareShown
     */
    fun editExistingShare(share: OCShare, screenTypePermission: Int, isReshareShown: Boolean) {
        val file = file ?: return
        val user = user ?: return
        val capabilities = capabilities ?: return
        requireActivity().supportFragmentManager.beginTransaction().replace(
            R.id.share_fragment_container,
            FileDetailsSharingProcessFragment.newInstance(
                share,
                screenTypePermission,
                isReshareShown,
                SharePermissionManager.canEditFile(user, capabilities, file, editorUtils)
            ),
            FileDetailsSharingProcessFragment.TAG
        )
            .addToBackStack(null)
            .commit()
    }

    @VisibleForTesting
    internal fun createInternalLink(user: User, file: OCFile, capabilities: OCCapability?): String {
        val linkPath = if (capabilities?.modRewriteWorking?.isTrue == true) {
            INTERNAL_LINK_PATH_PRETTY
        } else {
            INTERNAL_LINK_PATH_DEFAULT
        }
        return user.server.uri.toString() + linkPath + file.localId
    }

    private fun showSendLinkTo(publicShare: OCShare) {
        val file = file ?: return

        if (!file.isSharedViaLink) {
            return
        }

        if (publicShare.shareLink.isNullOrEmpty()) {
            fileOperationsHelper?.getFileWithLink(file, viewThemeUtils)
            return
        }

        FileActivity.showShareLinkDialog(fileActivity, file, publicShare.shareLink)
    }

    private fun refreshUiFromDB() {
        refreshSharesFromDB()
        setupView()
    }

    private fun unShareWith(share: OCShare) {
        fileOperationsHelper?.unShareShare(file, share.id)
    }

    private fun addExternalAndPublicShares() {
        if (FileDetailSharingFragmentHelper.isPublicShareDisabled(capabilities)
            || file?.canReshare() == false
        ) {
            return
        }

        externalShareeListAdapter?.removeAll() ?: run {
            DisplayUtils.showSnackMessage(this@FileDetailSharingFragment, R.string.could_not_retrieve_shares)
            return
        }

        val publicShares =
            fileDataStorageManager?.getSharesByPathAndType(file?.remotePath, ShareType.PUBLIC_LINK, "") ?: emptyList()
        externalShareeListAdapter?.addShares(publicShares)

        showHideLinkShareView(publicShares.isEmpty())
    }

    private fun showHideLinkShareView(isEmptyList: Boolean) {
        binding?.linkSharesList?.visibility = if (isEmptyList) View.GONE else View.VISIBLE
    }

    private fun showHideEmailShareView(isEmptyList: Boolean) {
        binding?.run {
            sharesList.visibility = if (isEmptyList) View.GONE else View.VISIBLE
            // additional check to hide the empty shares if file cannot be shared
            if (file?.canReshare() == false) {
                tvEmptyShares.visibility = View.GONE
                return
            }
            tvEmptyShares.visibility = if (isEmptyList) View.VISIBLE else View.GONE
        }
    }

    private fun checkContactPermission() {
        val canReadContacts = (checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS))
        if (canReadContacts) {
            pickContactEmail()
            return
        }

        requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun pickContactEmail() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Email.CONTENT_URI)

        if (intent.resolveActivity(requireContext().packageManager) != null) {
            onContactSelectionResultLauncher.launch(intent)
            return
        }

        DisplayUtils.showSnackMessage(this, R.string.file_detail_sharing_fragment_no_contact_app_message)
    }

    private fun handleContactResult(contactUri: Uri) {
        // Define the projection to get all email addresses.
        val projection = arrayOf<String?>(ContactsContract.CommonDataKinds.Email.ADDRESS)

        val cursor = fileActivity?.contentResolver?.query(contactUri, projection, null, null, null)
        if (cursor == null) {
            DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
            Log_OC.e(
                TAG,
                "Failed to pick email address as Cursor is null."
            )
            return
        }

        if (!cursor.moveToFirst()) {
            DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
            Log_OC.e(
                TAG,
                "Failed to pick email address as no Email found."
            )
            return
        }

        // The contact has only one email address, use it.
        val columnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        if (columnIndex == -1) {
            DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
            Log_OC.e(TAG, "Failed to pick email address.")
            cursor.close()
            return
        }

        // Use the email address as needed.
        // email variable contains the selected contact's email address.
        val email = cursor.getString(columnIndex)
        binding?.searchView?.post(
            Runnable {
                if (binding == null) {
                    return@Runnable
                }
                binding?.searchView?.setQuery(email, false)
                binding?.searchView?.requestFocus()
            }
        )
        cursor.close()
    }

    private fun isReshareForbidden(share: OCShare): Boolean = (
        ShareType.FEDERATED == share.shareType ||
            capabilities?.filesSharingResharing?.isFalse == true
        )

    private fun modifyExistingShare(share: OCShare, screenTypePermission: Int) {
        onEditShareListener?.editExistingShare(share, screenTypePermission, !isReshareForbidden(share))
    }

    private val externalShareTypes = setOf(
        ShareType.PUBLIC_LINK,
        ShareType.FEDERATED_GROUP,
        ShareType.FEDERATED,
        ShareType.EMAIL
    )

    private suspend fun loadShares(): List<OCShare> = withContext(Dispatchers.IO) {
        val shares = fileDataStorageManager
            ?.getSharesWithForAFile(file?.remotePath, user?.accountName)
            ?: emptyList()

        val linkShares = shares
            .filter { it.shareType != null }

        return@withContext linkShares
    }
    // endregion

    // region overridden methods
    override fun onQuickPermissionChanged(share: OCShare, permission: Int) {
        fileOperationsHelper?.setPermissionsToShare(share, permission)
    }

    override fun openIn(share: OCShare?) {
        fileOperationsHelper?.sendShareFile(file, true)
    }

    override fun advancedPermissions(share: OCShare) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_PERMISSION)
    }

    override fun sendNewEmail(share: OCShare) {
        modifyExistingShare(share, FileDetailsSharingProcessFragment.SCREEN_TYPE_NOTE)
    }

    override fun unShare(share: OCShare) {
        if (binding == null) {
            return
        }

        unShareWith(share)

        val entity = fileDataStorageManager?.getFileEntity(file)

        if (binding?.sharesList?.adapter is ShareeListAdapter) {
            val adapter = binding?.sharesList?.adapter as ShareeListAdapter
            adapter.remove(share)
            if (entity != null && adapter.isAdapterEmpty()) {
                entity.sharedWithSharee = 0
                fileDataStorageManager?.updateFileEntity(entity)
            }
        } else if (binding?.linkSharesList?.adapter is ShareeListAdapter) {
            val adapter = binding?.linkSharesList?.adapter as ShareeListAdapter
            adapter.remove(share)
            if (entity != null && adapter.isAdapterEmpty()) {
                entity.sharedViaLink = 0
                fileDataStorageManager?.updateFileEntity(entity)
            }
        } else {
            DisplayUtils.showSnackMessage(this, R.string.failed_update_ui)
        }
    }

    override fun sendLink(share: OCShare) {
        if (file?.isSharedViaLink == true && !share.shareLink.isNullOrEmpty()) {
            FileActivity.showShareLinkDialog(fileActivity, file, share.shareLink)
        } else {
            showSendLinkTo(share)
        }
    }

    override fun copyInternalLink() {
        val user = user

        if (user == null) {
            DisplayUtils.showSnackMessage(this, R.string.could_not_retrieve_url)
            return
        }

        file?.let { FileActivity.showShareLinkDialog(fileActivity, file, createInternalLink(user, it, capabilities)) }
    }

    private fun OCCapability?.isPasswordEnforced(): Boolean =
        this?.filesSharingPublicPasswordEnforced?.isTrue == true &&
            filesSharingPublicAskForOptionalPassword.isTrue

    override fun createPublicShareLink() {
        if (capabilities?.isPasswordEnforced() == true) {
            requestPasswordForShareViaLink(
                true,
                (capabilities?.filesSharingPublicAskForOptionalPassword?.isTrue == true)
            )
            return
        }

        // create without password
        fileOperationsHelper?.shareFileViaPublicShare(file, null)

        // track event on creating share link
        AdjustSdkUtils.trackEvent(AdjustSdkUtils.EVENT_TOKEN_CREATE_SHARING_LINK, appPreferences)
        TealiumSdkUtils.trackEvent(TealiumSdkUtils.EVENT_CREATE_SHARING_LINK, appPreferences)
    }

    override fun createSecureFileDrop() {
        fileOperationsHelper?.shareFolderViaSecureFileDrop(file!!)
    }

    override fun copyLink(share: OCShare) {
        val file = file ?: return
        if (!file.isSharedViaLink) {
            return
        }

        if (share.shareLink.isNullOrEmpty()) {
            fileOperationsHelper?.getFileWithLink(file, viewThemeUtils)
            return
        }

        copyToClipboard(requireActivity(), share.shareLink)

        // NMC: send link after copying it to clipboard
        sendLink(share)
    }

    @VisibleForTesting
    override fun showSharingMenuActionSheet(share: OCShare?) {
        if (fileActivity == null || fileActivity?.isFinishing == true) {
            return
        }

        FileDetailSharingMenuBottomSheetDialog(
            fileActivity,
            this,
            share,
            viewThemeUtils,
            file?.isEncrypted == true
        ).show()
    }

    override fun showPermissionsDialog(share: OCShare?) {
        QuickSharingPermissionsBottomSheetDialog(
            fileActivity,
            this,
            share,
            viewThemeUtils,
            file?.isEncrypted == true
        ).show()
    }
    override fun requestPasswordForShare(share: OCShare?, askForPassword: Boolean) {
        val dialog = newInstance(share, askForPassword)
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT)
    }

    override fun showProfileBottomSheet(user: User, shareWith: String?) {
        if (!user.server.version.isNewerOrEqual(NextcloudVersion.nextcloud_23)) {
            return
        }

        val userId = shareWith ?: return
        val activity = fileActivity ?: return

        RetrieveHoverCardAsyncTask(
            user,
            userId,
            activity,
            clientFactory,
            viewThemeUtils
        ).execute()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.run {
            putParcelable(ARG_FILE, file)
            putParcelable(ARG_USER, user)
        }
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        // NMC: not required
        // binding?.sharedWithYouAvatar?.setImageDrawable(avatarDrawable)
    }

    override fun shouldCallGeneratedCallback(tag: String?, callContext: Any?): Boolean = false
    // endregion

    // region public methods
    fun search(query: String?) {
        binding?.searchView?.setQuery(query, true)
    }

    fun onUpdateShareInformation(result: RemoteOperationResult<*>, file: OCFile?) {
        this.file = file
        onUpdateShareInformation(result)
    }

    fun onUpdateShareInformation(result: RemoteOperationResult<*>) {
        if (binding == null) {
            return
        }

        if (result.isSuccess) {
            refreshUiFromDB()
        } else {
            setupView()
        }
    }

    fun requestPasswordForShareViaLink(createShare: Boolean, askForPassword: Boolean) {
        val dialog = newInstance(
            file,
            createShare,
            askForPassword
        )
        dialog.show(getChildFragmentManager(), SharePasswordDialogFragment.PASSWORD_FRAGMENT)
    }

    fun refreshCapabilitiesFromDB() {
        capabilities = fileDataStorageManager?.getCapability(user?.accountName)
    }

    fun refreshSharesFromDB() {
        file = file?.fileId?.let { fileDataStorageManager?.getFileById(it) } ?: file

        internalShareeListAdapter?.removeAll() ?: run {
            DisplayUtils.showSnackMessage(this, R.string.could_not_retrieve_shares)
            return
        }

        lifecycleScope.launch {
            val internalShares = loadShares()

            withContext(Dispatchers.Main) {
                internalShareeListAdapter?.addShares(internalShares)
                showHideEmailShareView(internalShares.isEmpty())
                addExternalAndPublicShares()
            }
        }
    }
    // endregion

    // region private values
    private val requestContactPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            pickContactEmail()
        } else {
            DisplayUtils.showSnackMessage(this, R.string.contact_no_permission)
        }
    }

    private val onContactSelectionResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            if (intent == null) {
                DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
                return@registerForActivityResult
            }

            val contactUri = intent.data
            if (contactUri == null) {
                DisplayUtils.showSnackMessage(this, R.string.email_pick_failed)
                return@registerForActivityResult
            }

            handleContactResult(contactUri)
        }
    }
    // endregion

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // when user is in portrait mode and search view is focused and keyboard is open
        // so when user rotate the device we have to fix the search view properly in landscape mode
        scrollToSearchViewPosition(true)
    }

    interface OnEditShareListener {
        fun editExistingShare(share: OCShare?, screenTypePermission: Int, isReshareShown: Boolean)

        fun onShareProcessClosed()

        fun onLinkShareDownloadLimitFetched(downloadLimit: Long, downloadCount: Long)
    }

    companion object {
        private const val TAG = "FileDetailSharingFragment"
        private const val ARG_FILE = "FILE"
        private const val ARG_USER = "USER"
        private const val INTERNAL_LINK_PATH_PRETTY = "/f/"
        private const val INTERNAL_LINK_PATH_DEFAULT = "/index.php/f/"

        @JvmStatic
        fun newInstance(file: OCFile?, user: User?): FileDetailSharingFragment = FileDetailSharingFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_FILE, file)
                putParcelable(ARG_USER, user)
            }
        }
    }
}
