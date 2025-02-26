/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.owncloud.android.R

class AlbumsAdapter(val context: Context) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val gridView = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (gridView) {
            val itemView: View = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false)
            return AlbumsListItemViewHolder(itemView)
        } else {
            val itemView: View = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
            return AlbumsListItemViewHolder(itemView)
        }
    }

    override fun getItemCount(): Int {
        return 0;
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    }

    internal class AlbumsListItemViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        private val fileSize: TextView = itemView.findViewById(R.id.file_size)
        private val lastModification: TextView = itemView.findViewById(R.id.last_mod)
        private val fileSeparator: TextView = itemView.findViewById(R.id.file_separator)

        init {
            itemView.findViewById<View>(R.id.sharedAvatars).visibility = View.GONE
            itemView.findViewById<View>(R.id.overflow_menu).visibility = View.GONE
            itemView.findViewById<View>(R.id.tagsGroup).visibility = View.GONE
        }
    }
}