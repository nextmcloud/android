package com.nmc.android

import com.nextcloud.ui.fileactions.FileAction
import org.junit.Assert
import org.junit.Test
import com.owncloud.android.R

class FileActionIconTest {

    @Test
    fun verifyShareAndSyncFileIcon(){
       val list = FileAction.SORTED_VALUES
        Assert.assertEquals(R.drawable.ic_share,list[4].icon)
        Assert.assertEquals(R.drawable.ic_content_copy,list[7].icon)
        Assert.assertEquals(R.drawable.ic_cloud_download,list[8].icon)
    }
}