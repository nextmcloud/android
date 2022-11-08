package com.owncloud.android.utils

import org.junit.Assert
import org.junit.Test

class MimeTypeUtilTest {

    @Test
    fun isJpgOrPngFileTest() {
        Assert.assertEquals(false, MimeTypeUtil.isJpgOrPngFile(""))
        Assert.assertEquals(false, MimeTypeUtil.isJpgOrPngFile(null))
        Assert.assertEquals(false, MimeTypeUtil.isJpgOrPngFile("."))
        Assert.assertEquals(false, MimeTypeUtil.isJpgOrPngFile("dummy_file_name"))

        Assert.assertEquals(true, MimeTypeUtil.isJpgOrPngFile(".jpg"))
        Assert.assertEquals(true, MimeTypeUtil.isJpgOrPngFile("abc.jpg"))
    }

}