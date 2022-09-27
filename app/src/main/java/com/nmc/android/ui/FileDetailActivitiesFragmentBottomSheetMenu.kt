package com.nmc.android.ui

import android.os.Bundle
import com.nmc.android.ui.FileDetailActivitiesFragmentBottomSheetMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentFileDetailActivitiesBottomSheetMenuBinding


class FileDetailActivitiesFragmentBottomSheetMenu() : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentFileDetailActivitiesBottomSheetMenuBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentFileDetailActivitiesBottomSheetMenuBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.menuEditComment.setOnClickListener {
            dismissAllowingStateLoss()
            //todo
        }

        binding.menuDeleteComment.setOnClickListener {
            dismissAllowingStateLoss()
            //todo
        }

        return view
    }

}