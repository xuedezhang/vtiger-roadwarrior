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

package com.roadwarrior.vtiger.client;

import android.accounts.Account;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.roadwarrior.vtiger.authenticator.AuthenticatorActivity;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Provides utility methods for communicating with the server.
 */
final public class NetworkUtilities {
    /** The tag used to log to adb console. */
    private static final String TAG = "NetworkUtilities";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_OPERATION = "operation";
    public static final String PARAM_SESSIONNAME = "sessionName";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_UPDATED = "timestamp";
    public static final String PARAM_ACCESSKEY = "accessKey";
    public static final String USER_AGENT = "AuthenticationService/1.0";
    public static final int HTTP_REQUEST_TIMEOUT_MS = 500 * 1000; // ms
    public static String AUTH_URI;
//    public static final String FETCH_FRIEND_UPDATES_URI =
//        BASE_URL + "/fetch_friend_updates";
//    public static final String FETCH_STATUS_URI = BASE_URL + "/fetch_status";

    public static  String sessionName;

    private NetworkUtilities() {
    }
    /**
     * Configures the httpClient to connect to the URL provided.
     */
    public static HttpClient getHttpClient() {
        HttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        return httpClient;
    }

   

    /**
     * Connects to the  server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @param handler The hander instance from the calling UI thread.
     * @param context The context of the calling Activity.
     * @return boolean The boolean result indicating whether the user was
     *         successfully authenticated.
     */
    public static String authenticate(String username, String accessKey,String base_url) {
        final HttpResponse resp;
        String token = null;
        String hash = null;
        //FIXME:
        AUTH_URI = base_url+ "/webservice.php";
        // =========== get challenge token ==============================
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        try {
			final HttpGet httpGet = new HttpGet(AUTH_URI+"?operation=getchallenge&username="+username);
			
			final ResponseHandler<String> resphandler = new BasicResponseHandler();
        	
			String body;
			body = getHttpClient().execute(httpGet,resphandler);
//			if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {  
//				String body ;
				
				Log.i(TAG,"message");
				Log.i(TAG,body);
				JSONObject result=new JSONObject(body);
				Log.i(TAG,result.getString("result"));
	            JSONObject data=new JSONObject(result.getString("result"));
	            token = data.getString("token");
	            Log.i(TAG,token);
             
        } catch (ClientProtocolException e) {
        	Log.i(TAG,"http protocol error");
            Log.e(TAG, e.getMessage());
           return null;
        } catch (IOException e) {
        	Log.e(TAG,"IO Exception");
            //Log.e(TAG, e.getMessage());
            Log.e(TAG,AUTH_URI+"?operation=getchallenge&username="+username);            
        	return null;
 
        } catch (JSONException e) {
        	Log.i(TAG,"json excpetion");
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
        // ================= login ==================

        try {
        	MessageDigest m = MessageDigest.getInstance("MD5");
        	 m.update(token.getBytes());
        	 m.update(accessKey.getBytes());
        	 hash = new BigInteger(1, m.digest()).toString(16);
        	 Log.i(TAG,"hash");
        	 Log.i(TAG,hash);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
//        // FIXME: sleep ?
        try {
			Thread.sleep(1000);
		} catch (InterruptedException e1) {
			
			Log.i(TAG,"sleep fail");
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    
        params.clear();
        params.add(new BasicNameValuePair(PARAM_OPERATION, "login"));
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_ACCESSKEY,hash));
        HttpEntity entity2 = null;
        try {

            entity2 = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            // this should never happen.
            throw new AssertionError(e);
        }
        final HttpPost post2 = new HttpPost(AUTH_URI);
        post2.setEntity(entity2);

        Log.i(TAG,"login");
        Log.i(TAG,username);
        Log.i(TAG,accessKey);
        try {
            final ResponseHandler<String> resphandler = new BasicResponseHandler();
            String body = getHttpClient().execute(post2, resphandler);
            Log.i(TAG,"message login");
            Log.i(TAG,body);
            JSONObject result=new JSONObject(body);
           
            String success = result.getString("success");
            Log.i(TAG,success);
            
            if (success == "true")
            	{
            	 Log.i(TAG,result.getString("result"));
            	Log.i(TAG,"sucesss is true");
                JSONObject data=new JSONObject(result.getString("result"));
            	sessionName = data.getString("sessionName");
            
            	Log.i(TAG,sessionName);
            	return token;
            	}
            else {
            	return null;
            }
            //token = data.getString("token");
            //Log.i(TAG,token);
        } catch (ClientProtocolException e) {
        	Log.i(TAG,"http protocol error");
            Log.e(TAG, e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
        }
        return null;
        // ========================================================================
//        try {
//            resp = mHttpClient.execute(post);
//            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//                if (Log.isLoggable(TAG, Log.VERBOSE)) {
//                    Log.v(TAG, "Successful authentication");
//                }
//                Log.i(TAG,"response"+resp);
//                sendResult(true, handler, context);
//                return true;
//            } else {
//                if (Log.isLoggable(TAG, Log.VERBOSE)) {
//                    Log.v(TAG, "Error authenticating" + resp.getStatusLine());
//                }
//                sendResult(false, handler, context);
//                return false;
//            }
//        } catch (final IOException e) {
//            if (Log.isLoggable(TAG, Log.VERBOSE)) {
//                Log.v(TAG, "IOException when getting authtoken", e);
//            }
//            sendResult(false, handler, context);
//            return false;
//        } finally {
//            if (Log.isLoggable(TAG, Log.VERBOSE)) {
//                Log.v(TAG, "getAuthtoken completing");
//            }
//        }
    }

   
    /**
     * Fetches the list of friend data updates from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in AccountManager for this account
     * @param lastUpdated The last time that sync was performed
     * @return list The list of updates received from the server.
     */
    public static List<User> fetchFriendUpdates(Account account,
        String authtoken, long serverSyncState/*Date lastUpdated*/,String type_contact) throws JSONException,
        ParseException, IOException, AuthenticationException {
        final ArrayList<User> friendList = new ArrayList<User>();
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OPERATION, "sync"));
        params.add(new BasicNameValuePair(PARAM_SESSIONNAME, sessionName));
        params.add(new BasicNameValuePair("modifiedTime","878925701" )); // il y a 14 ans.... 
        params.add(new BasicNameValuePair("elementType",type_contact));  // "Accounts,Leads , Contacts... 
        Log.i(TAG,"fetchFriendUpdates");
        //   params.add(new BasicNameValuePair(PARAM_QUERY, "select firstname,lastname,mobile,email,homephone,phone from Contacts;"));
//        if (lastUpdated != null) {
//            final SimpleDateFormat formatter =
//                new SimpleDateFormat("yyyy/MM/dd HH:mm");
//            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
//            params.add(new BasicNameValuePair(PARAM_UPDATED, formatter
//                .format(lastUpdated)));
//        }
        Log.i(TAG, params.toString());

        final HttpGet post = new HttpGet(AUTH_URI+"?"+URLEncodedUtils.format(params, "utf-8"));
       // post.addHeader(entity.getContentType());
        post.addHeader("accept","application/json");

        //post.setEntity(entity);
       // maybeCreateHttpClient();

        final HttpResponse resp = getHttpClient().execute(post);

        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
            // Succesfully connected to the samplesyncadapter server and
            // authenticated.
            // Extract friends data in json format.
            String response = EntityUtils.toString(resp.getEntity());
         	Log.i(TAG,"--response--");
        	Log.i(TAG, response);
        	// <Hack> to bypass vtiger 5.4 webservice bug:
        	int idx  = response.indexOf("{\"success");
        	response = response.substring(idx);
        	Log.i(TAG, response);
        	// </Hack>
        	Log.i(TAG,"--response end--");
        	JSONObject result=new JSONObject(response);
            Log.i(TAG,result.getString("result"));
            String success = result.getString("success");
            Log.i(TAG,"success is"+success);
            if (success == "true")
            {
            final JSONObject data = new JSONObject(result.getString("result"));
            final JSONArray friends = new JSONArray(data.getString("updated"));
            
            for (int i = 0; i < friends.length(); i++) {
                friendList.add(User.valueOf(friends.getJSONObject(i)));
            }
            }
            else {
            	
            // FIXME: else false...
            // possible error code :
            //{"success":false,"error":{"code":"AUTHENTICATION_REQUIRED","message":"Authencation required"}}
            //            	throw new AuthenticationException();
            }
        } else {
            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                Log.e(TAG,
                    "Authentication exception in fetching remote contacts");
                throw new AuthenticationException();
            } else {
                Log.e(TAG, "Server error in fetching remote contacts: "
                    + resp.getStatusLine());
                throw new IOException();
            }
        }
        return friendList;
    }

    /**
     * Fetches status messages for the user's friends from the server
     * 
     * @param account The account being synced.
     * @param authtoken The authtoken stored in the AccountManager for the
     *        account
     * @return list The list of status messages received from the server.
     */
    public static List<User.Status> fetchFriendStatuses(Account account,
        String authtoken) throws JSONException, ParseException, IOException,
        AuthenticationException {
        final ArrayList<User.Status> statusList = new ArrayList<User.Status>();
//        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
//        params.add(new BasicNameValuePair(PARAM_USERNAME, account.name));
//        params.add(new BasicNameValuePair(PARAM_PASSWORD, authtoken));
//
//        HttpEntity entity = null;
//        entity = new UrlEncodedFormEntity(params);
//        final HttpPost post = new HttpPost(FETCH_STATUS_URI);
//        post.addHeader(entity.getContentType());
//        post.setEntity(entity);
//        maybeCreateHttpClient();
//
//        final HttpResponse resp = mHttpClient.execute(post);
//        final String response = EntityUtils.toString(resp.getEntity());
//
//        if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//            // Succesfully connected to the samplesyncadapter server and
//            // authenticated.
//            // Extract friends data in json format.
//            final JSONArray statuses = new JSONArray(response);
//            for (int i = 0; i < statuses.length(); i++) {
//                statusList.add(User.Status.valueOf(statuses.getJSONObject(i)));
//            }
//        } else {
//            if (resp.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
//                Log.e(TAG,
//                    "Authentication exception in fetching friend status list");
//                throw new AuthenticationException();
//            } else {
//                Log.e(TAG, "Server error in fetching friend status list");
//                throw new IOException();
//            }
//        }
        return statusList;
    }

}
