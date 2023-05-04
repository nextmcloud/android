/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.preference.SwitchPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.core.content.res.ResourcesCompat;

/**
 * Themeable switch preference TODO Migrate to androidx
 */
public class ThemeableSwitchPreference extends SwitchPreference {
    @Inject
    ViewThemeUtils viewThemeUtils;

    public ThemeableSwitchPreference(Context context) {
        super(context);
        MainApp.getAppComponent().inject(this);
    }

    public ThemeableSwitchPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        MainApp.getAppComponent().inject(this);
    }

    public ThemeableSwitchPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        MainApp.getAppComponent().inject(this);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        if (view instanceof ViewGroup) {
            findSwitch((ViewGroup) view);
        }
    }

    private void findSwitch(ViewGroup viewGroup) {
        ColorStateList thumbColorStateList;
        ColorStateList trackColorStateList;

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View child = viewGroup.getChildAt(i);

            if (child instanceof Switch) {
                Switch switchView = (Switch) child;

                int[][] states = new int[][]{
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_checked}, // enabled and checked
                    new int[]{android.R.attr.state_enabled, -android.R.attr.state_checked}, // enabled and unchecked
                    new int[]{-android.R.attr.state_enabled}  // disabled
                };

                int thumbColorCheckedEnabled = ResourcesCompat.getColor(
                    switchView.getContext().getResources(),
                    R.color.switch_thumb_checked_enabled,
                    switchView.getContext().getTheme());
                int thumbColorUncheckedEnabled = ResourcesCompat.getColor(
                    switchView.getContext().getResources(),
                    R.color.switch_thumb_unchecked_enabled,
                    switchView.getContext().getTheme());
                int thumbColorDisabled =
                    ResourcesCompat.getColor(
                        switchView.getContext().getResources(),
                        R.color.switch_thumb_disabled,
                        switchView.getContext().getTheme());

                int[] thumbColors = new int[]{
                    thumbColorCheckedEnabled,
                    thumbColorUncheckedEnabled,
                    thumbColorDisabled
                };

                thumbColorStateList = new ColorStateList(states, thumbColors);

                int trackColorCheckedEnabled = ResourcesCompat.getColor(
                    switchView.getContext().getResources(),
                    R.color.switch_track_checked_enabled,
                    switchView.getContext().getTheme());
                int trackColorUncheckedEnabled = ResourcesCompat.getColor(
                    switchView.getContext().getResources(),
                    R.color.switch_track_unchecked_enabled,
                    switchView.getContext().getTheme());
                int trackColorDisabled = ResourcesCompat.getColor(
                    switchView.getContext().getResources(),
                    R.color.switch_track_disabled,
                    switchView.getContext().getTheme());

                int[] trackColors = new int[]{
                    trackColorCheckedEnabled,
                    trackColorUncheckedEnabled,
                    trackColorDisabled
                };
                trackColorStateList = new ColorStateList(states, trackColors);

                switchView.setThumbTintList(thumbColorStateList);
                switchView.setTrackTintList(trackColorStateList);

                break;
            } else if (child instanceof ViewGroup) {
                findSwitch((ViewGroup) child);
            }
        }
    }
}
