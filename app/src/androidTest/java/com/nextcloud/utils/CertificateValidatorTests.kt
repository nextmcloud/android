/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.owncloud.android.datamodel.Credentials
import com.owncloud.android.ui.dialog.setupEncryption.CertificateValidator
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.InputStreamReader

class CertificateValidatorTests {

    private var sut: CertificateValidator? = null
    private val gson = Gson()

    @Before
    fun setup() {
        sut = CertificateValidator()
    }

    @After
    fun destroy() {
        sut = null
    }

    @Test
    fun testValidateWhenGivenValidServerKeyAndCertificateShouldReturnTrue() {
        val isCertificateValid = validateCertificate("credentials.json")
        assert(isCertificateValid)
    }

    @Test
    fun testValidateWhenGivenAnotherValidServerKeyAndCertificateShouldReturnTrue() {
        val isCertificateValid = validateCertificate("invalid_certs.json")
        assert(isCertificateValid)
    }

    private fun validateCertificate(filename: String): Boolean {
        val inputStream =
            InstrumentationRegistry.getInstrumentation().context.assets.open(filename)

        val credentials = InputStreamReader(inputStream).use { reader ->
            gson.fromJson(reader, Credentials::class.java)
        }

        return sut?.validate(credentials.publicKey, credentials.certificate) ?: false
    }
}
