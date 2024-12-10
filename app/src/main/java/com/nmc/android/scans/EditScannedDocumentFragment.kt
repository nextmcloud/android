package com.nmc.android.scans

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.nmc.android.adapters.ViewPagerFragmentAdapter
import com.nmc.android.interfaces.OnDocScanListener
import com.nmc.android.interfaces.OnFragmentChangeListener
import com.nmc.android.scans.ScanDocumentFragment.Companion.newInstance
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentEditScannedDocumentBinding

class EditScannedDocumentFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentEditScannedDocumentBinding
    private lateinit var pagerFragmentAdapter: ViewPagerFragmentAdapter
    private var onFragmentChangeListener: OnFragmentChangeListener? = null
    private var onDocScanListener: OnDocScanListener? = null

    private var selectedScannedDocFile: Bitmap? = null
    private var currentSelectedItemIndex = 0
    private var currentItemIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let{
            currentItemIndex = it.getInt(ARG_CURRENT_INDEX, 0)
        }
        //Fragment screen orientation normal both portrait and landscape
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onFragmentChangeListener = context as OnFragmentChangeListener
            onDocScanListener = context as OnDocScanListener
        } catch (ignored: Exception) {
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (requireActivity() is ScanActivity) {
            (requireActivity() as ScanActivity).showHideToolbar(true)
            (requireActivity() as ScanActivity).showHideDefaultToolbarDivider(true)
            (requireActivity() as ScanActivity).updateActionBarTitleAndHomeButtonByString(
                resources.getString(R.string.title_edit_scan)
            )
        }
        binding = FragmentEditScannedDocumentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpViewPager()

        binding.cropDocButton.setOnClickListener(this)
        binding.scanMoreButton.setOnClickListener(this)
        binding.filterDocButton.setOnClickListener(this)
        binding.rotateDocButton.setOnClickListener(this)
        binding.deleteDocButton.setOnClickListener(this)

        addMenuHost()
    }

    private fun setUpViewPager() {
        pagerFragmentAdapter = ViewPagerFragmentAdapter(this)
        val filesList = onDocScanListener?.getScannedDocs() ?: emptyList()
        if (filesList.isEmpty()) {
            onScanMore(true)
            return
        }
        for (i in filesList.indices) {
            pagerFragmentAdapter.addFragment(ScanPagerFragment.newInstance(i))
        }
        binding.editScannedViewPager.adapter = pagerFragmentAdapter
        binding.editScannedViewPager.post { binding.editScannedViewPager.setCurrentItem(currentItemIndex, false) }
        binding.editScannedViewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                currentSelectedItemIndex = position
                selectedScannedDocFile = filesList[position]
                updateDocCountText(position, filesList.size)
            }
        })

        if (filesList.size == 1) {
            binding.editScanDocCountLabel.visibility = View.INVISIBLE
        } else {
            binding.editScanDocCountLabel.visibility = View.VISIBLE
            updateDocCountText(currentItemIndex, filesList.size)
        }
    }

    private fun updateDocCountText(position: Int, totalSize: Int) {
        binding.editScanDocCountLabel.text = String.format(
            resources.getString(R.string.scanned_doc_count),
            position + 1, totalSize
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.scanMoreButton -> onScanMore(false)
            R.id.cropDocButton -> onFragmentChangeListener?.onReplaceFragment(
                CropScannedDocumentFragment.newInstance(currentSelectedItemIndex),
                ScanActivity.FRAGMENT_CROP_SCAN_TAG, false
            )

            R.id.filterDocButton -> showFilterDialog()
            R.id.rotateDocButton -> {
                val fragment = pagerFragmentAdapter.getFragment(currentSelectedItemIndex)
                if (fragment is ScanPagerFragment) {
                    fragment.rotate()
                }
            }

            R.id.deleteDocButton -> {
                val isRemoved =
                    onDocScanListener?.removedScannedDoc(selectedScannedDocFile, currentSelectedItemIndex) ?: false
                if (isRemoved) {
                    setUpViewPager()
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setUpViewPager()
    }

    /**
     * check if fragment has to open on + button click or when all scans removed
     *
     * @param isNoItem
     */
    private fun onScanMore(isNoItem: Boolean) {
        onFragmentChangeListener?.onReplaceFragment(
            newInstance(if (isNoItem) ScanActivity.TAG else TAG),
            ScanActivity.FRAGMENT_SCAN_TAG, false
        )
    }

    private fun showFilterDialog() {
        val fragment = pagerFragmentAdapter.getFragment(currentSelectedItemIndex)
        if (fragment is ScanPagerFragment) {
            fragment.showApplyFilterDialog()
        }
    }

    private fun addMenuHost() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.edit_scan, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_save -> {
                            val fragment = pagerFragmentAdapter.getFragment(currentSelectedItemIndex)
                            if (fragment is ScanPagerFragment) {
                                // if applying filter is not in process then only show save fragment
                                if (!fragment.isFilterApplyInProgress) {
                                    saveScannedDocs()
                                }
                            } else {
                                saveScannedDocs()
                            }
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun saveScannedDocs() {
        onFragmentChangeListener?.onReplaceFragment(
            SaveScannedDocumentFragment.newInstance(),
            ScanActivity.FRAGMENT_SAVE_SCAN_TAG, false
        )
    }

    companion object {
        private const val ARG_CURRENT_INDEX = "current_index"
        const val TAG: String = "EditScannedDocumentFragment"

        fun newInstance(currentIndex: Int): EditScannedDocumentFragment {
            val args = Bundle()
            args.putInt(ARG_CURRENT_INDEX, currentIndex)
            val fragment = EditScannedDocumentFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
