package com.nmc.android

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.withHint
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.internal.runner.junit4.statement.UiThreadStatement
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.ui.dialog.SetupEncryptionDialogFragment
import org.junit.Rule
import org.junit.Test
import com.owncloud.android.R

class SetupEncryptionDialogFragmentIT : AbstractIT() {

    @get:Rule
    val testActivityRule = IntentsTestRule(TestActivity::class.java, true, false)

    @Test
    fun validatePassphraseInputHint() {
        val activity = testActivityRule.launchActivity(null)

        val sut = SetupEncryptionDialogFragment.newInstance(user, 0)

        sut.show(activity.supportFragmentManager, "1")

        val keyWords = arrayListOf(
            "ability",
            "able",
            "about",
            "above",
            "absent",
            "absorb",
            "abstract",
            "absurd",
            "abuse",
            "access",
            "accident",
            "account",
            "accuse"
        )

        shortSleep()

        UiThreadStatement.runOnUiThread {
            sut.setMnemonic(keyWords)
            sut.showMnemonicInfo()
        }

        waitForIdleSync()

        onView(withId(R.id.encryption_passwordInput)).check(matches(withHint("Passphrase…")))
    }
}