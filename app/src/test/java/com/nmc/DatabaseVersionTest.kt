package com.nmc

import com.nextcloud.client.database.NextcloudDatabase
import org.junit.Test

class DatabaseVersionTest {

    @Test
    fun validateDatabaseVersion() {
        //for NMC the version will start from 64 only
        //validating via test case to check if any test changes done during rebasing or merging
        assert(64 == NextcloudDatabase.FIRST_ROOM_DB_VERSION)
    }
}