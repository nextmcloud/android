/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.decoration;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

/**
 * DividerItemDecoration based on {@link DividerItemDecoration} adding a 72dp left padding.
 */
public class SimpleListItemDividerDecoration extends DividerItemDecoration {
    private static final int[] ATTRS = new int[]{android.R.attr.listDivider};

    private final Rect bounds = new Rect();
    private Drawable divider;
    private int leftPadding = 0;
    private boolean hasFooter;

    /**
     * Default divider will be used
     */
    public SimpleListItemDividerDecoration(Context context) {
        super(context, DividerItemDecoration.VERTICAL);
        final TypedArray styledAttributes = context.obtainStyledAttributes(ATTRS);
        divider = styledAttributes.getDrawable(0);
        leftPadding = Math.round(72 * (context.getResources().getDisplayMetrics().xdpi / DisplayMetrics.DENSITY_DEFAULT));
        styledAttributes.recycle();
    }

    /**
     * Custom divider will be used
     *
     * @param hasFooter if recyclerview has footer and no divider should be shown for footer then pass true else false
     */
    public SimpleListItemDividerDecoration(Context context, int resId, boolean hasFooter) {
        super(context, DividerItemDecoration.VERTICAL);
        this.hasFooter = hasFooter;
        divider = ContextCompat.getDrawable(context, resId);
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        canvas.save();
        final int right;
        //noinspection AndroidLintNewApi - NewApi lint fails to handle overrides.
        if (parent.getClipToPadding()) {
            right = parent.getWidth() - parent.getPaddingRight();
            canvas.clipRect(leftPadding, parent.getPaddingTop(), right,
                    parent.getHeight() - parent.getPaddingBottom());
        } else {
            right = parent.getWidth();
        }

        int childCount = parent.getChildCount();

        if (hasFooter) {
            childCount = childCount - 1;
        }

        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            parent.getDecoratedBoundsWithMargins(child, bounds);
            final int bottom = bounds.bottom + Math.round(child.getTranslationY());
            final int top = bottom - 1;

            if (divider != null) {
                divider.setBounds(leftPadding, top, right, bottom);
                divider.draw(canvas);
            }
        }
        canvas.restore();
    }
}
