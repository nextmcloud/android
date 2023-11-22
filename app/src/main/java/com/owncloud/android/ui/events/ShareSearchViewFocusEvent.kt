/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2022 TSI-mc
 * Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.events

/**
 * Event for search view focus while sharing a file/folder
 * this event will be used to hide the view only for landscape mode so that user will have more space
 */
class ShareSearchViewFocusEvent(val hasFocus: Boolean)