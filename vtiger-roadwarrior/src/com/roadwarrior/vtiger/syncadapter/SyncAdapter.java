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

package com.roadwarrior.vtiger.syncadapter;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.roadwarrior.vtiger.Constants;
import com.roadwarrior.vtiger.client.NetworkUtilities;
import com.roadwarrior.vtiger.client.User;
import com.roadwarrior.vtiger.client.User.Status;
import com.roadwarrior.vtiger.platform.ContactManager;

import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.json.JSONException;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "VTiger.SyncAdapter";
    private static final String SYNC_MARKER_KEY = "com.roadwarrior.vtiger.marker";
    private static final boolean NOTIFY_AUTH_FAILURE = true;

    private final AccountManager mAccountManager;
    private final Context mContext;



    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
        String authtoken = null;

         try {
        	 Log.d(TAG,"----- SYNCING onPerformSync  ----");
        	 // see if we already have a sync-state attached to this account. By handing
            // This value to the server, we can just get the contacts that have
            // been updated on the server-side since our last sync-up
            long lastSyncMarker = getServerSyncMarker(account);
            Log.d(TAG,String.valueOf(lastSyncMarker));
            // By default, contacts from a 3rd party provider are hidden in the contacts
            // list. So let's set the flag that causes them to be visible, so that users
            // can actually see these contacts.
            if (lastSyncMarker == 0) {
            	Log.i(TAG,"set Accounts visible");
                ContactManager.setAccountContactsVisibility(getContext(), account, true);
            }
            List<User> users,users_accounts,users_leads;
            List<Status> statuses;

             // use the account manager to request the credentials
             authtoken =
                mAccountManager.blockingGetAuthToken(account,
                    Constants.AUTHTOKEN_TYPE, true /* notifyAuthFailure */);
             if (authtoken != null)
             {
             Log.d(TAG,"authtoken obtained");
             // Make sure that the Vtiger group exists
             final long groupId = ContactManager.ensureSampleGroupExists(mContext, account,"VTiger Contacts");
             final long groupId1 = ContactManager.ensureSampleGroupExists(mContext, account,"VTiger Leads");
             final long groupId2 = ContactManager.ensureSampleGroupExists(mContext, account,"VTiger Accounts");
             ContactManager.groupLeads = groupId1;
             ContactManager.groupAccounts = groupId2;
             ContactManager.groupContacts = groupId;
             String url = mAccountManager.getUserData(account, "url");
             Log.d(TAG,"URL is \nURL ");
             Log.d(TAG,url);
             
             
             // fetch updates from the sample service over the cloud
             users =
                NetworkUtilities.fetchFriendUpdates(account, url,authtoken,
                    lastSyncMarker,"Contacts");
             if (! NetworkUtilities.getLastOperationStatus())
            	 {
            	 mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, authtoken);
             	 return;
            	 }
             // update platform contacts.
             Log.d(TAG, "Calling contactManager's sync contacts");
             ContactManager.syncContacts(mContext, account.name, users,groupId);
             
             
             users_accounts =
                 NetworkUtilities.fetchFriendUpdates(account, url,authtoken,
                     lastSyncMarker,"Accounts");
             if (! NetworkUtilities.getLastOperationStatus())
        	 {
        	 mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, authtoken);
         	 return;
        	 }
             ContactManager.syncContacts(mContext, account.name, users_accounts,groupId2);

             
             users_leads =
                 NetworkUtilities.fetchFriendUpdates(account, url,authtoken,
                     lastSyncMarker,"Leads");
             if (! NetworkUtilities.getLastOperationStatus())
        	 {
        	 mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE, authtoken);
         	 return;
        	 }
 

            ContactManager.syncContacts(mContext, account.name, users_leads,groupId1);
            // fetch and update status messages for all the synced users.
//            statuses = NetworkUtilities.fetchFriendStatuses(account, authtoken);
//
//
//            ContactManager.insertStatuses(mContext, account.name, statuses);
            
            // Save off the new sync marker. On our next sync, we only want to receive
            // contacts that have changed since this sync...
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            long secondsSinceEpoch = calendar.getTimeInMillis() / 1000L;
            
            setServerSyncMarker(account, secondsSinceEpoch);
            }
             else 
             {
            	 Log.d(TAG,"authtoken not obtained");
             }

        } catch (final AuthenticatorException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "AuthenticatorException", e);
        } catch (final OperationCanceledException e) {
            Log.e(TAG, "OperationCanceledExcetpion", e);
        } catch (final IOException e) {
            Log.e(TAG, "IOException", e);
            syncResult.stats.numIoExceptions++;
        } catch (final AuthenticationException e) {
            mAccountManager.invalidateAuthToken(Constants.ACCOUNT_TYPE,
                authtoken);
            syncResult.stats.numAuthExceptions++;
            Log.e(TAG, "AuthenticationException", e);
        } catch (final ParseException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "ParseException", e);
        } catch (final JSONException e) {
            syncResult.stats.numParseExceptions++;
            Log.e(TAG, "JSONException", e);
        }
    }

    /**
     * This helper function fetches the last known high-water-mark
     * we received from the server - or 0 if we've never synced.
     * @param account the account we're syncing
     * @return the change high-water-mark
     */
    private long getServerSyncMarker(Account account) {
        String markerString = mAccountManager.getUserData(account, SYNC_MARKER_KEY);
        if (!TextUtils.isEmpty(markerString)) {
            return Long.parseLong(markerString);
        }
        return 0;
    }

    /**
     * Save off the high-water-mark we receive back from the server.
     * @param account The account we're syncing
     * @param marker The high-water-mark we want to save.
     */
    private void setServerSyncMarker(Account account, long marker) {
        mAccountManager.setUserData(account, SYNC_MARKER_KEY, Long.toString(marker));
    }
}
