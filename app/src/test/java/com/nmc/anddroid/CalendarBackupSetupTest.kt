package com.nmc.anddroid

import android.content.res.Resources
import com.owncloud.android.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class CalendarBackupSetupTest {

    @Mock
    lateinit var resources: Resources

    private lateinit var mocks: AutoCloseable

    @Before
    fun setUp() {
        mocks = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun assertShowCalendarBackupBooleanFalse(){
        assert(!resources.getBoolean(R.bool.show_calendar_backup))
    }

    @After
    fun tearDown() {
        mocks.close()
    }

}