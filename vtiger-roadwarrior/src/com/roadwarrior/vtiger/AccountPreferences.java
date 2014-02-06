package com.roadwarrior.vtiger;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.util.Log;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.EditTextPreference;
import android.preference.PreferenceManager;
import android.preference.PreferenceActivity;

import com.roadwarrior.vtiger.R;
import com.roadwarrior.vtiger.authenticator.*;

public class AccountPreferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {

public static final String TAG = "VTiger.AccountPreferences";
private boolean shouldForceSync = false;
private Preference mUsername;
private Preference mPassword;
private Preference mVtiger_url;
private ListPreference sync_period;

@Override
public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    Log.i(TAG, "onCreate");
  
    addPreferencesFromResource(R.xml.sync_settings);

    mUsername = findPreference("username");
    mUsername.setSummary( ((EditTextPreference) mUsername).getText());
    
    mPassword = findPreference("password");
    mPassword.setSummary( ((EditTextPreference) mPassword).getText());
    
    mVtiger_url= findPreference("vtiger_url");
    mVtiger_url.setSummary( ((EditTextPreference) mVtiger_url).getText());
    
    sync_period = (ListPreference) findPreference("sync_period");
    sync_period.setSummary(sync_period.getEntry().toString());

    
}

public void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

}

@Override
public void onPause() {
    super.onPause();

    if (shouldForceSync) {
    	Log.i(TAG,"reSyncAccount");
    	VtigerAuthenticator.resyncAccount(this);
    }
    Log.i(TAG,"PAUUUUSE");
    Log.i(TAG,this.toString());
    PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);

}

public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    // Let's do something a preference value changes
    shouldForceSync = true;
    mUsername.setSummary(sharedPreferences.getString("username", ""));
    mPassword.setSummary(sharedPreferences.getString("password", ""));
    mVtiger_url.setSummary(sharedPreferences.getString("vtiger_url", ""));
    sync_period.setSummary(sync_period.getEntry().toString());
}



}