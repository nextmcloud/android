/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2009 The Android Open Source Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.owncloud.android.authentication;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import com.nextcloud.utils.extensions.IntentExtensionsKt;
import com.nextcloud.utils.extensions.WindowExtensionsKt;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;

/*
 * Base class for implementing an Activity that is used to help implement an AbstractAccountAuthenticator.
 * If the AbstractAccountAuthenticator needs to use an activity to handle the request then it can have the activity extend
 * AccountAuthenticatorActivity. The AbstractAccountAuthenticator passes in the response to the intent using the following:
 * intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
 *
 * The activity then sets the result that is to be handed to the response via setAccountAuthenticatorResult(android.os.Bundle).
 * This result will be sent as the result of the request when the activity finishes. If this is never set or if it is set to null
 * then error AccountManager.ERROR_CODE_CANCELED will be called on the response.
 */
public abstract class AccountAuthenticatorActivity extends AppCompatActivity {

    private AccountAuthenticatorResponse mAccountAuthenticatorResponse;
    private Bundle mResultBundle;

    /**
     * Set the result that is to be sent as the result of the request that caused this Activity to be launched.
     * If result is null or this method is never called then the request will be canceled.
     *
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    /**
     * Retrieves the AccountAuthenticatorResponse from either the intent of the icicle, if the
     * icicle is non-zero.
     * @param savedInstanceState the save instance data of this Activity, may be null
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // NMC-3936 and NMC-3813 fix
        boolean isApiLevel35OrHigher = (Build.VERSION.SDK_INT >= 35);

        if (isApiLevel35OrHigher) {
            enableEdgeToEdge();
            WindowExtensionsKt.addSystemBarPaddings(getWindow());
        }

        super.onCreate(savedInstanceState);

        mAccountAuthenticatorResponse =
            IntentExtensionsKt.getParcelableArgument(getIntent(),
                                                     AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                                                     AccountAuthenticatorResponse.class);

        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }
    }

    private void enableEdgeToEdge() {
        final var style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT);
        EdgeToEdge.enable(this, style, style);
    }

    /**
     * Sends the result or a Constants.ERROR_CODE_CANCELED error if a result isn't present.
     */
    @Override
    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            // send the result bundle back if set, otherwise send an error.
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }
}
