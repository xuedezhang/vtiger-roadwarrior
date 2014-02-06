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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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
    private static final String TAG = "VTiger.NetworkUtilities";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_OPERATION = "operation";
    public static final String PARAM_SESSIONNAME = "sessionName";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_QUERY = "query";
    public static final String PARAM_UPDATED = "timestamp";
    public static final String PARAM_ACCESSKEY = "accessKey";
    public static final String USER_AGENT = "AuthenticationService/1.0";
    public static final int HTTP_REQUEST_TIMEOUT_MS =  10000; // ms
    public static String AUTH_URI;
//    public static final String FETCH_FRIEND_UPDATES_URI =
//        BASE_URL + "/fetch_friend_updates";
//    public static final String FETCH_STATUS_URI = BASE_URL + "/fetch_status";
    private static boolean LastFetchOperationStatus = false;
    public static  String authenticate_log_text;
    public static  String sessionName;

    private NetworkUtilities() {
    	authenticate_log_text = "-";
    }

    public static String getLogStatus(){
    	return authenticate_log_text;
    }

    /**
     * Connects to the  server, authenticates the provided username and
     * password.
     * 
     * @param username The user's username
     * @param password The user's password
     * @return String The authentication token returned by the server (or null)
     */
    public static String authenticate(String username, String accessKey,String base_url) {
        String token = null;
        String hash = null;
        authenticate_log_text = "authenticate()\n";
        AUTH_URI = base_url+ "/webservice.php";
        authenticate_log_text= authenticate_log_text + "url: " + AUTH_URI +"?operation=getchallenge&username="+username+"\n";

        
        // =========== get challenge token ==============================
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        try {
        	
        	URL url;

			// HTTP GET REQUEST
			url = new URL(AUTH_URI+"?operation=getchallenge&username="+username);
			HttpURLConnection con;
			con = (HttpURLConnection) url.openConnection();		    
		    con.setRequestMethod("GET");
	        con.setRequestProperty("Content-length", "0");
	        con.setUseCaches(false);
	        con.setAllowUserInteraction(false);
	        int timeout = 20000;
			con.setConnectTimeout(timeout );
	        con.setReadTimeout(timeout);
	        con.connect();
	        int status = con.getResponseCode();

	        authenticate_log_text= authenticate_log_text + "status = " + status;
	        switch (status) {
	            case 200:
	            case 201:
	                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
	                StringBuilder sb = new StringBuilder();
	                String line;
	                while ((line = br.readLine()) != null) {
	                    sb.append(line+"\n");
	                }
	                br.close();
					Log.d(TAG,"message body");
					Log.d(TAG,sb.toString());
					
					authenticate_log_text = authenticate_log_text + "body : " + sb.toString(); 
	                
					JSONObject result=new JSONObject(sb.toString());
					Log.d(TAG,result.getString("result"));
		            JSONObject data=new JSONObject(result.getString("result"));
		            token = data.getString("token");
		            break;
	            case 401: 
	            	 Log.e(TAG, "Server auth error: ");// + readResponse(con.getErrorStream()));
	            	 authenticate_log_text = authenticate_log_text + "Server auth error";
	            	 return null;
			default:
	            	 Log.e(TAG, "connection status code " + status);// + readResponse(con.getErrorStream()));
	            	 authenticate_log_text = authenticate_log_text + "connection status code :" + status;
	            	return null;
	        }

		  
             
        } catch (ClientProtocolException e) {
        	Log.i(TAG,"http protocol error");
            Log.e(TAG, e.getMessage());
            authenticate_log_text = authenticate_log_text + "ClientProtocolException :" + e.getMessage() + "\n";
           return null;
        } catch (IOException e) {
        	Log.e(TAG,"IO Exception");
            //Log.e(TAG, e.getMessage());
            Log.e(TAG,AUTH_URI+"?operation=getchallenge&username="+username);  
            authenticate_log_text = authenticate_log_text + "IO Exception : "+e.getMessage()+"\n";
        	return null;
 
        } catch (JSONException e) {
        	Log.i(TAG,"json excpetion");
        	authenticate_log_text = authenticate_log_text + "JSon exception\n";
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
        	authenticate_log_text = authenticate_log_text + "MD5 => no such algorithm\n"; 
            e.printStackTrace();
        }
  
        try {
        	 String charset;
             charset = "utf-8";
             String query = String.format("operation=login&username=%s&accessKey=%s", 
            	     URLEncoder.encode(username, charset), 
            	     URLEncoder.encode(hash, charset));
             
             URLConnection connection = new URL(AUTH_URI).openConnection();
             connection.setDoOutput(true); // Triggers POST.
      
             connection.setRequestProperty("Accept-Charset", charset);
             connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);

             OutputStream output = connection.getOutputStream();
             try {
                  output.write(query.getBytes(charset));
             } finally {
                  try { output.close(); } catch (IOException logOrIgnore) {}
             }
            BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }
            br.close();
    		Log.d(TAG,"message post body");
    		Log.d(TAG,sb.toString());
    		authenticate_log_text = authenticate_log_text + "post message body :" + sb.toString() + "\n";
    		JSONObject result=new JSONObject(sb.toString());
           
            String success = result.getString("success");
            Log.i(TAG,success);
            
            if (success == "true")
            	{
            	 Log.i(TAG,result.getString("result"));
            	Log.i(TAG,"sucesssfully logged  in is");
                JSONObject data=new JSONObject(result.getString("result"));
            	sessionName = data.getString("sessionName");
            
            	Log.i(TAG,sessionName);
            	authenticate_log_text = authenticate_log_text  + "successfully logged in\n";
            	return token;
            	}
            else {
            	authenticate_log_text = authenticate_log_text  + "CAN NOT LOG IN\n";
            	
            	return null;
            }
            //token = data.getString("token");
            //Log.i(TAG,token);
        } catch (ClientProtocolException e) {
        	Log.i(TAG,"http protocol error");
            Log.e(TAG, e.getMessage());
        	authenticate_log_text = authenticate_log_text  + "HTTP Protocol error " + e.getMessage()+"\n";
        } catch (IOException e) {

        	Log.e(TAG, e.getMessage());
        	authenticate_log_text = authenticate_log_text  + "IO Exception " + e.getMessage()+"\n";

        } catch (JSONException e) {
			// TODO Auto-generated catch block
        	authenticate_log_text = authenticate_log_text  + "JSON exception " + e.getMessage()+"\n";

			e.printStackTrace();
        }
        return null;
        // ========================================================================

    }

    public static boolean getLastOperationStatus()
    {
    	return LastFetchOperationStatus;
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
    	String auth_url,
        String authtoken, long serverSyncState/*Date lastUpdated*/,String type_contact) throws JSONException,
        ParseException, IOException, AuthenticationException {
        ArrayList<User> friendList = new ArrayList<User>();
        ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_OPERATION, "sync"));
        params.add(new BasicNameValuePair(PARAM_SESSIONNAME, sessionName));
        params.add(new BasicNameValuePair("modifiedTime","878925701" )); // il y a 14 ans.... 
        params.add(new BasicNameValuePair("elementType",type_contact));  // "Accounts,Leads , Contacts... 
        Log.d(TAG,"fetchFriendUpdates");
        //   params.add(new BasicNameValuePair(PARAM_QUERY, "select firstname,lastname,mobile,email,homephone,phone from Contacts;"));
//        if (lastUpdated != null) {
//            final SimpleDateFormat formatter =
//                new SimpleDateFormat("yyyy/MM/dd HH:mm");
//            formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
//            params.add(new BasicNameValuePair(PARAM_UPDATED, formatter
//                .format(lastUpdated)));
//        }
       
     // HTTP GET REQUEST
		URL url = new URL(auth_url+"/webservice.php?"+URLEncodedUtils.format(params, "utf-8"));
		HttpURLConnection con;
		con = (HttpURLConnection) url.openConnection();		    
	    con.setRequestMethod("GET");
        con.setRequestProperty("Content-length", "0");
        con.setRequestProperty("accept", "application/json");

        con.setUseCaches(false);
        con.setAllowUserInteraction(false);
        int timeout = 10000; // si tiemout pas assez important la connection echouait =>IOEXception
		con.setConnectTimeout(timeout );
        con.setReadTimeout(timeout);
        con.connect();
        int status = con.getResponseCode();

        LastFetchOperationStatus = true;
        if (status == HttpURLConnection.HTTP_OK) {
            // Succesfully connected to the samplesyncadapter server and
            // authenticated.
            // Extract friends data in json format.
        	
        	BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line+"\n");
            }
            br.close();
            
            
            String response = sb.toString();
         	Log.i(TAG,"--response--");
        	// <Hack> to bypass vtiger 5.4 webservice bug:
        	int idx  = response.indexOf("{\"success");
        	response = response.substring(idx);
        	Log.i(TAG, response);
        	// </Hack>
        	Log.i(TAG,"--response end--");
        	JSONObject result=new JSONObject(response);
        	
        	
            String success = result.getString("success");
            Log.i(TAG,"success is"+success);
            if (success == "true")
            {
            Log.i(TAG,result.getString("result"));
            final JSONObject data = new JSONObject(result.getString("result"));
            final JSONArray friends = new JSONArray(data.getString("updated"));
            
            for (int i = 0; i < friends.length(); i++) {
                friendList.add(User.valueOf(friends.getJSONObject(i)));
            }
            }
            else {
            	LastFetchOperationStatus = false;
            // FIXME: else false...
            // possible error code :
            //{"success":false,"error":{"code":"AUTHENTICATION_REQUIRED","message":"Authencation required"}}
            //            	throw new AuthenticationException();
            }
        } else {
            if (status== HttpURLConnection.HTTP_UNAUTHORIZED) {
            	LastFetchOperationStatus = false;

                Log.e(TAG,
                    "Authentication exception in fetching remote contacts");
                throw new AuthenticationException();
            } else {
            	LastFetchOperationStatus = false;

                Log.e(TAG, "Server error in fetching remote contacts: ");
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
