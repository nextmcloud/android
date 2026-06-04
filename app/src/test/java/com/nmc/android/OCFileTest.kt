package com.nmc.android

import com.owncloud.android.datamodel.OCFile
import org.junit.Assert.assertEquals
import org.junit.Test

class OCFileTest {

    @Test
    fun testLongIds() {
        val sut = OCFile("/")

        //1 digit local id
        sut.remoteId = "1ocjycgrudn78"
        assertEquals(1, sut.localId)

        //2 digit local id
        sut.remoteId = "12ocjycgrudn78"
        assertEquals(12, sut.localId)

        //3 digit local id
        sut.remoteId = "123ocjycgrudn78"
        assertEquals(123, sut.localId)

        //4 digit local id
        sut.remoteId = "1234ocjycgrudn78"
        assertEquals(1234, sut.localId)

        //5 digit local id
        sut.remoteId = "12345ocjycgrudn78"
        assertEquals(12345, sut.localId)

        //6 digit local id
        sut.remoteId = "123456ocjycgrudn78"
        assertEquals(123456, sut.localId)

        //7 digit local id
        sut.remoteId = "1234567ocjycgrudn78"
        assertEquals(1234567, sut.localId)

        //8 digit local id
        sut.remoteId = "12345678ocjycgrudn78"
        assertEquals(12345678, sut.localId)

        //9 digit local id
        sut.remoteId = "123456789ocjycgrudn78"
        assertEquals(123456789, sut.localId)

        //10 digit local id
        sut.remoteId = "1234567890ocjycgrudn78"
        assertEquals(1234567890, sut.localId)

        //11 digit local id
        sut.remoteId = "12345678901ocjycgrudn78"
        assertEquals(12345678901, sut.localId)

        //12 digit local id
        sut.remoteId = "123456789012ocjycgrudn78"
        assertEquals(123456789012, sut.localId)

        //13 digit local id
        sut.remoteId = "1234567890123ocjycgrudn78"
        assertEquals(1234567890123, sut.localId)

        //14 digit local id
        sut.remoteId = "12345678901234ocjycgrudn78"
        assertEquals(12345678901234, sut.localId)

        //20 digit local id
        sut.remoteId = "1234567890123456233ocjycgrudn78"
        assertEquals(1234567890123456233L, sut.localId)
    }
}