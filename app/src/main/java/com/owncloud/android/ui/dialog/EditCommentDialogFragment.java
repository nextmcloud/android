package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import com.owncloud.android.R;
import com.owncloud.android.databinding.NoteDialogBinding;
import com.owncloud.android.operations.comments.Comments;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ThemeColorUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog to edit comment for a file
 */
public class EditCommentDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String ARG_COMMENT = "COMMENT";

    public static final String EDIT_COMMENT_FRAGMENT_TAG = "EDIT_COMMENT_FRAGMENT";

    private Comments comment;
    private NoteDialogBinding binding;
    private Button positiveButton;
    private OnEditCommentListener onEditCommentListener;

    public static EditCommentDialogFragment newInstance(Comments comment) {
        EditCommentDialogFragment frag = new EditCommentDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_COMMENT, comment);
        frag.setArguments(args);

        return frag;
    }

    public void setOnEditCommentListener(OnEditCommentListener onEditCommentListener) {
        this.onEditCommentListener = onEditCommentListener;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        if (alertDialog != null) {
            positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            throw new IllegalArgumentException("Arguments may not be null");
        }
        comment = getArguments().getParcelable(ARG_COMMENT);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //int primaryColor = ThemeColorUtils.primaryColor(getContext());

        // Inflate the layout for the dialog
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        binding = NoteDialogBinding.inflate(inflater, null, false);
        View view = binding.getRoot();

        // Setup layout
        binding.noteContainer.setHint(requireContext().getResources().getString(R.string.new_comment));
        binding.noteText.setText(comment.getMessage());
        binding.noteText.requestFocus();
       // ThemeTextInputUtils.colorTextInput(binding.noteContainer, binding.noteText, primaryColor, ThemeColorUtils.primaryAccentColor(getContext()));
        //binding.noteText.setHighlightColor(getResources().getColor(R.color.et_highlight_color));
        binding.noteContainer.setDefaultHintTextColor(new ColorStateList(
            new int[][]{
                new int[]{-android.R.attr.state_focused},
                new int[]{android.R.attr.state_focused},
            },
            new int[]{
                Color.GRAY,
                getResources().getColor(R.color.text_color)
            }
        ));

        binding.noteText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            /**
             * When user enters a same message or empty message
             */
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String message = "";
                if (binding.noteText.getText() != null) {
                    message = binding.noteText.getText().toString().trim();
                }

                if (TextUtils.isEmpty(message)) {
                    binding.noteContainer.setError(getText(R.string.empty_comment_message));
                    positiveButton.setEnabled(false);
                } else if (binding.noteContainer.getError() != null) {
                    binding.noteContainer.setError(null);
                    // Called to remove extra padding
                    binding.noteContainer.setErrorEnabled(false);
                    positiveButton.setEnabled(true);
                }
            }
        });


        // Build the dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());
        builder.setView(view)
            .setPositiveButton(R.string.done, this)
            .setNeutralButton(R.string.common_cancel, this)
            .setTitle(R.string.edit_comment);
        Dialog dialog = builder.create();

        Window window = dialog.getWindow();

        if (window != null) {
            window.setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == AlertDialog.BUTTON_POSITIVE) {
            String message = "";

            if (binding.noteText.getText() != null) {
                message = binding.noteText.getText().toString().trim();
            }

            if (onEditCommentListener != null) {
                onEditCommentListener.doUpdateComment(comment, message);
            } else {
                DisplayUtils.showSnackMessage(requireActivity(), R.string.error_comment_update);
            }

        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public interface OnEditCommentListener {
        void doUpdateComment(Comments comments, String message);
    }
}
