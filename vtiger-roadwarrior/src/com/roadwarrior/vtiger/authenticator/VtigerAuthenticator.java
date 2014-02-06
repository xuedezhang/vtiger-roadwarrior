/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.roadwarrior.vtiger.authenticator;
import com.roadwarrior.vtiger.Constants;
import com.roadwarrior.vtiger.R;
import com.roadwarrior.vtiger.client.NetworkUtilities;


import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Notification;
import android.app.NotificationManager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.app.Notification;
import android.app.Notification.Builder;
import android.app.NotificationManager;

import android.text.TextUtils;
import android.util.Log;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.roadwarrior.vtiger domain.
 */
class VtigerAuthenticator extends AbstractAccountAuthenticator {
	 /** The tag used to log to adb console. **/
    private static final String TAG = "VTiger.Authenticator";

    private String ValidAuthToken = null;
    // Authentication Service context
    private final Context mContext;

    public VtigerAuthenticator(Context context) {
        super(context);
        mContext = context;
    }


    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response,
        String accountType, String authTokenType, String[] requiredFeatures,
        Bundle options) {
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
            response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }

   
    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse response, Account account, Bundle options) {
        Log.v(TAG, "confirmCredentials()");
        return null;
    }


   
    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        Log.v(TAG, "editProperties()");
        throw new UnsupportedOperationException();
    }

    
    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle loginOptions) throws NetworkErrorException {
        Log.v(TAG, "getAuthToken()");

        // If the caller requested an authToken type we don't support, then
        // return an error
        if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }
        if (ValidAuthToken == null)
        {
	        // Extract the username and password from the Account Manager, and ask
	        // the server for an appropriate AuthToken.
	        final AccountManager am = AccountManager.get(mContext);
	        final String password = am.getPassword(account);
	        final String url = am.getUserData(account, "url");
	        if (password != null) {
	            String authToken = NetworkUtilities.authenticate(account.name, password,url);
	            if ((authToken != null) && (!TextUtils.isEmpty(authToken))) {
	            	ValidAuthToken = authToken;

	            }
	        }
        }
        if (ValidAuthToken != null)
        {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
            result.putString(AccountManager.KEY_AUTHTOKEN, ValidAuthToken);
            return result;
        }
        // If we get here, then we couldn't access the user's password - so we
        // need to re-prompt them for their credentials. We do that by creating
        // an intent to display our AuthenticatorActivity panel.
        final Intent intent = new Intent(mContext, AuthenticatorActivity.class);
        intent.putExtra(AuthenticatorActivity.PARAM_USERNAME, account.name);
        intent.putExtra(AuthenticatorActivity.PARAM_AUTHTOKEN_TYPE, authTokenType);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
        
    }


    @Override
    public String getAuthTokenLabel(String authTokenType) {
        // null means we don't support multiple authToken types
        Log.v(TAG, "getAuthTokenLabel()");

        return null;

    }


    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response, Account account, String[] features) {
        // This call is used to query whether the Authenticator supports
        // specific features. We don't expect to get called, so we always
        // return false (no) for any queries.
        Log.v(TAG, "hasFeatures()");
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle loginOptions) {
        Log.v(TAG, "updateCredentials()");
        return null;
    }
}
