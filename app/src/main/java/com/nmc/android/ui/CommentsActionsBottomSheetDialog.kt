package com.nmc.android.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.owncloud.android.databinding.CommentsActionsBottomSheetFragmentBinding
import com.owncloud.android.operations.comments.Comments


class CommentsActionsBottomSheetDialog(context: Context,
                                       private val comments: Comments,
                                       private val commentsBottomSheetActions: CommentsBottomSheetActions) : BottomSheetDialog(context) {

    private lateinit var binding: CommentsActionsBottomSheetFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = CommentsActionsBottomSheetFragmentBinding.inflate(layoutInflater)

        setContentView(binding.root)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        setOnShowListener {
            BottomSheetBehavior.from(binding.root.parent as View)
                .setPeekHeight(binding.root.measuredHeight)
        }


        binding.menuEditComment.setOnClickListener {
            commentsBottomSheetActions.onUpdateComment(comments)
            dismiss()
        }

        binding.menuDeleteComment.setOnClickListener {
            commentsBottomSheetActions.onDeleteComment(comments)
            dismiss()
        }


    }

    interface CommentsBottomSheetActions {
        fun onUpdateComment(comments: Comments)
        fun onDeleteComment(comments: Comments)
    }
}