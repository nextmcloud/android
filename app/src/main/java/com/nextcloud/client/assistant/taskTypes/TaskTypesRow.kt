/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.assistant.taskTypes

import android.annotation.SuppressLint
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.owncloud.android.R
import com.owncloud.android.lib.resources.assistant.model.TaskTypeData

@SuppressLint("ResourceType")
@Composable
fun TaskTypesRow(selectedTaskType: TaskTypeData?, data: List<TaskTypeData>, selectTaskType: (TaskTypeData) -> Unit) {
    val selectedTabIndex = data.indexOfFirst { it.id == selectedTaskType?.id }.takeIf { it >= 0 } ?: 0

    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        edgePadding = 0.dp,
        containerColor = colorResource(R.color.actionbar_color),
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(it[selectedTabIndex]),
                color = colorResource(R.color.primary),
            )
        }
    ) {
        data.forEach { taskType ->
            taskType.name?.let { taskTypeName ->
                Tab(
                    selected = selectedTaskType?.id == taskType.id,
                    onClick = { selectTaskType(taskType) },
                    selectedContentColor = colorResource(R.color.text_color),
                    unselectedContentColor = colorResource(R.color.disabled_text),
                    text = { Text(text = taskTypeName) }
                )
            }
        }
    }
}

@Composable
@Preview
private fun TaskTypesRowPreview() {
    val selectedTaskType = TaskTypeData("1", "Free text to text prompt", "", null, null)
    val taskTypes = listOf(
        TaskTypeData("1", "Free text to text prompt", "", null, null),
        TaskTypeData("2", "Extract topics", "", null, null),
        TaskTypeData("3", "Generate Headline", "", null, null),
        TaskTypeData("4", "Summarize", "", null, null)
    )

    TaskTypesRow(selectedTaskType, taskTypes) { }
}
