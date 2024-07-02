/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.utils;


import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import java.util.Locale;
import android.graphics.Typeface;
import android.text.style.StyleSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Helper class for handling and manipulating strings.
 */
public final class StringUtils {

    private StringUtils() {
        // prevent class from being constructed
    }

    public static List<String> convertStringToList(String input) {
        return Arrays.asList(input.split("\\s*,\\s*"));
    }

    public static @NonNull
    String searchAndColor(@Nullable String text, @Nullable String searchText,
                          @ColorInt int color) {

        if (text != null) {

            if (text.isEmpty() || searchText == null || searchText.isEmpty()) {
                return text;
            }

            Matcher matcher = Pattern.compile(searchText,
                                              Pattern.CASE_INSENSITIVE | Pattern.LITERAL).matcher(text);

            StringBuffer stringBuffer = new StringBuffer();

            while (matcher.find()) {
                String replacement = matcher.group().replace(
                    matcher.group(),
                    String.format(Locale.getDefault(), "<font color='%d'><b>%s</b></font>", color,
                                  matcher.group())
                                                            );
                matcher.appendReplacement(stringBuffer, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(stringBuffer);

            return stringBuffer.toString();
        } else {
            return "";
        }
    }

    public static Spannable getColorSpan(@NonNull String title, @ColorInt int color) {
        Spannable text = new SpannableString(title);
        text.setSpan(new ForegroundColorSpan(color),
                     0,
                     text.length(),
                     Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        return text;
    }

    public static
    @NonNull
    String removePrefix(@NonNull String s, @NonNull String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    /**
     * make the passed text bold
     *
     * @param fullText   actual text
     * @param textToBold to be bold
     * @return
     */
    public static Spannable makeTextBold(String fullText, String textToBold) {
        Spannable spannable = new SpannableString(fullText);
        int indexStart = fullText.indexOf(textToBold);
        int indexEnd = indexStart + textToBold.length();
        spannable.setSpan(new StyleSpan(Typeface.BOLD), indexStart, indexEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannable;
    }
}
