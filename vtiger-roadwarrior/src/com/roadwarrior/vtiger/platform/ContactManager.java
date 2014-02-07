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

package com.roadwarrior.vtiger.platform;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.Settings;
import android.provider.ContactsContract.StatusUpdates;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.text.format.DateUtils;
import android.util.Log;

import com.roadwarrior.vtiger.Constants;
import com.roadwarrior.vtiger.R;
import com.roadwarrior.vtiger.client.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 * Class for managing contacts sync related mOperations
 */
public class ContactManager {
    /**
     * Custom IM protocol used when storing status messages.
     */
    public static final String CUSTOM_IM_PROTOCOL = "SampleSyncAdapter";
    private static final String TAG = "VTiger.ContactManager";
    public static long groupLeads = 0;
    public static long groupContacts = 0;
    public static long groupAccounts = 0;
    
    public static long ensureSampleGroupExists(Context context, Account account,String GroupName) {
        final ContentResolver resolver = context.getContentResolver();
        
        Log.i(TAG,"ensureSampleGroupExists");
        
        // Lookup the sample group
        long groupId = 0;
        final Cursor cursor = resolver.query(Groups.CONTENT_URI, new String[] { Groups._ID },
                Groups.ACCOUNT_NAME + "=? AND " + Groups.ACCOUNT_TYPE + "=? AND " +
                Groups.TITLE + "=?",
                new String[] { account.name, account.type, GroupName}, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    groupId = cursor.getLong(0);
                }
            } finally {
                cursor.close();
            }
        }

        if (groupId == 0) {
            // Sample group doesn't exist yet, so create it
        	Log.i(TAG,"Creating group "+ GroupName);
            final ContentValues contentValues = new ContentValues();
            contentValues.put(Groups.ACCOUNT_NAME, account.name);
            contentValues.put(Groups.ACCOUNT_TYPE, account.type);
            contentValues.put(Groups.TITLE, GroupName);
            contentValues.put(Groups.GROUP_VISIBLE, true);
           // contentValues.put(Groups.GROUP_IS_READ_ONLY, true);

            final Uri newGroupUri = resolver.insert(Groups.CONTENT_URI, contentValues);
            groupId = ContentUris.parseId(newGroupUri);
        }
        
        return groupId;
    }

    /**
     * Synchronize raw contacts
     * 
     * @param context The context of Authenticator Activity
     * @param account The username for the account
     * @param users The list of users
     */
    public static void syncContacts(Context context,
        String account, List<User> users, long groupId) {
        long userId;
        long rawContactId = 0;
        //FIXME:calendar
        if (0==1)
        {
        // ===== SYNC CALENDAR ==============
     // query for the user's available and selected calendars
     // note the URI
//     Cursor cursor = context.getContentResolver().query(Uri.parse("content://calendar/calendars"),
//                     new String[] { "_id", "displayName" }, "selected=1", null, null);
//     if (cursor != null && cursor.moveToFirst()) {
//          String[] calNames = new String[cursor.getCount()];
//          int[] calIds = new int[cursor.getCount()];
//          for (int i = 0; i < calNames.length; i++) {
//               // retrieve the calendar names and ids
//               // at this stage you can print out the display names to get an idea of what calendars the user has
//               calIds[i] = cursor.getInt(0);
//               calNames[i] = cursor.getString(1);
//              Log.i(TAG,"calendar");
//              Log.i(TAG,calNames[i]);
//               cursor.moveToNext();
//           }
//           cursor.close();
//           if (calIds.length > 0) {
//                // we're safe here to do any further work
//           }
//     }
//    	String calId = null;
//		ContentResolver contentResolver = context.getContentResolver();
//
//		// Fetch a list of all calendars synced with the device, their display names and whether the
//		// user has them selected for display.
//		final Cursor cursor = contentResolver.query(Uri.parse("content://com.android.calendar/calendars"),
//				(new String[] { "_id", "displayName", "selected" }), null, null, null);
//		// For a full list of available columns see http://tinyurl.com/yfbg76w
//		HashSet<String> calendarIds = new HashSet<String>();
//		
//		while (cursor.moveToNext()) {
//
//			final String _id = cursor.getString(0);
//			final String displayName = cursor.getString(1);
//			final Boolean selected = !cursor.getString(2).equals("0");
//			
//			Log.i("hgg","Id: " + _id + " Display Name: " + displayName + " Selected: " + selected);
//			calendarIds.add(_id);
//			calId = _id;
//			
//			
//		}
//		Calendar cal = Calendar.getInstance();              
//		Intent intent = new Intent(Intent.ACTION_EDIT);
//		intent.setType("vnd.android.cursor.item/event");
//		intent.putExtra("beginTime", cal.getTimeInMillis());
//		intent.putExtra("allDay", true);
//		intent.putExtra("rrule", "FREQ=YEARLY");
//		intent.putExtra("endTime", cal.getTimeInMillis()+60*60*1000);
//		intent.putExtra("title", "A Test Event from android app");
//		startActivity(intent);
//
//		// http://www.developer.com/ws/article.php/3850276/Working-with-the-Android-Calendar.htm
//		//http://www.andreabaccega.com/blog/2010/08/09/add-events-on-google-calendar-on-android-froyo/
//		// add an event in calendar
//		ContentValues event = new ContentValues();
//		event.put("calendar_id", "2");
//		event.put("title", "Event Title");
//		event.put("description", "Event Desc");
//		event.put("eventLocation", "Event Location");
//		long startTime = new Date().getTime();;
//		long endTime = startTime+DateUtils.WEEK_IN_MILLIS;
//		event.put("dtstart", startTime);
//		event.put("dtend", endTime);
//		event.put("allDay", 1);   // 0 for false, 1 for true
//		//event.put("hasAlarm", 0);   // 0 for false, 1 for true
//
//		Uri eventsUri = Uri.parse("content://com.android.calendar/calendar/events");
//		  Uri url = contentResolver.insert(eventsUri, event);
//		// For each calendar, display all the events from the previous week to the end of next week.		
//		for (String id : calendarIds) {
//			Uri.Builder builder = Uri.parse("content://calendar/instances/when").buildUpon();
//			long now = new Date().getTime();
//			ContentUris.appendId(builder, now - DateUtils.WEEK_IN_MILLIS);
//			ContentUris.appendId(builder, now + DateUtils.WEEK_IN_MILLIS);
//
//			Cursor eventCursor = contentResolver.query(builder.build(),
//					new String[] { "title", "begin", "end", "allDay"}, "Calendars._id=" + id,
//					null, "startDay ASC, startMinute ASC"); 
//			// For a full list of available columns see http://tinyurl.com/yfbg76w
//
//			while (eventCursor.moveToNext()) {
//				final String title = eventCursor.getString(0);
//				final Date begin = new Date(eventCursor.getLong(1));
//				final Date end = new Date(eventCursor.getLong(2));
//				final Boolean allDay = !eventCursor.getString(3).equals("0");
//				
//				System.out.println("Title: " + title + " Begin: " + begin + " End: " + end +
//						" All Day: " + allDay);
//			}
//		}
	
        }
     // ===============end sync calendar ========
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
        Log.d(TAG, "In SyncContacts");
        for (final User user : users) {
        	
            userId = user.getUserId();
            if (groupId == groupLeads)
            { 
            	userId = userId | 0x80000;
            }
            if (groupId == groupAccounts)
            { 
            	userId = userId | 0x90000;
            }
            // Check to see if the contact needs to be inserted or updated
            rawContactId = lookupRawContact(resolver, userId);
            if (rawContactId != 0) {
                if (!user.isDeleted()) {
                    // update contact
                    updateContact(context, resolver, account, user,
                        true,rawContactId, batchOperation);
                } else {
                    // delete contact
                    deleteContact(context, rawContactId, batchOperation);
                }
            } else {
                // add new contact
                Log.d(TAG, "In addContact");
                if (!user.isDeleted()) {
                	addContact(context, account, user,groupId, true,batchOperation);
                }
            }
            // A sync adapter should batch operations on multiple contacts,
            // because it will make a dramatic performance difference.
            // (UI updates, etc)
            if (batchOperation.size() >= 50) {
                batchOperation.execute();
            }
        }
        batchOperation.execute();

    }

    /**
     * Add a list of status messages to the contacts provider.
     * 
     * @param context the context to use
     * @param accountName the username of the logged in user
     * @param statuses the list of statuses to store
     */
    public static void insertStatuses(Context context, String username,
        List<User.Status> list) {
        final ContentValues values = new ContentValues();
        final ContentResolver resolver = context.getContentResolver();
        final BatchOperation batchOperation =
            new BatchOperation(context, resolver);
        for (final User.Status status : list) {
            // Look up the user's sample SyncAdapter data row
            final long userId = status.getUserId();
            final long profileId = lookupProfile(resolver, userId);

            // Insert the activity into the stream
            if (profileId > 0) {
                values.put(StatusUpdates.DATA_ID, profileId);
                values.put(StatusUpdates.STATUS, status.getStatus());
                values.put(StatusUpdates.PROTOCOL, Im.PROTOCOL_CUSTOM);
                values.put(StatusUpdates.CUSTOM_PROTOCOL, CUSTOM_IM_PROTOCOL);
                values.put(StatusUpdates.IM_ACCOUNT, username);
                values.put(StatusUpdates.IM_HANDLE, status.getUserId());
                values.put(StatusUpdates.STATUS_RES_PACKAGE, context
                    .getPackageName());
                values.put(StatusUpdates.STATUS_ICON, R.drawable.icon);
                values.put(StatusUpdates.STATUS_LABEL, R.string.label);

                batchOperation
                    .add(ContactOperations.newInsertCpo(
                        StatusUpdates.CONTENT_URI, true,true).withValues(values)
                        .build());
                // A sync adapter should batch operations on multiple contacts,
                // because it will make a dramatic performance difference.
                if (batchOperation.size() >= 50) {
                    batchOperation.execute();
                }
            }
        }
        batchOperation.execute();
    }
    /**
     *  create a group
     */
   
    /**
     * Adds a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param accountName the account the contact belongs to
     * @param user the sample SyncAdapter User object
     */
    public static void addContact(Context context, String accountName,
        User user,long groupId, boolean inSync, BatchOperation batchOperation) {
    	// ===================================================================
        // Put the data in the contacts provider
        final ContactOperations contactOp =
            ContactOperations.createNewContact(context, user.getUserId(),
                accountName, inSync, batchOperation);
        contactOp.addName(user.getFirstName(), 
        				  user.getLastName())
        				  .addFax(user.getFax())
        				  .addEmail(user.getEmail()).addPhone(user.getCellPhone(), 
        								  Phone.TYPE_MOBILE)
            .addPhone(user.getHomePhone(), Phone.TYPE_OTHER).addWebSite(user.getWebsite()).addOrganisation(user.getOrganisation())
            .addAddress(user.getAddress(),user.getCity(),user.getRegion(),user.getCountry(),user.getPobox(),user.getPostCode(),Constants.billing_address)
            .addAddress(user.getOtherAddress(),user.getOtherCity(),user.getOtherRegion(),user.getOtherCountry(),user.getOtherPobox(),user.getOtherPostCode(),Constants.shipping_address)
            .addGroupMembership(groupId)
            .addProfileAction(
                user.getUserId());
    }

    /**
     * Updates a single contact to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param resolver the ContentResolver to use
     * @param accountName the account the contact belongs to
     * @param user the sample SyncAdapter contact object.
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    public static void updateContact(Context context,
        ContentResolver resolver, String accountName, User user,
        boolean inSync, long rawContactId,BatchOperation batchOperation) {

        String cellPhone = null;
        String otherPhone = null;
        String email = null;
        String website = null;
        String fax = null;
        String organisation = null;
        String organisation_department = null;
        String organisation_title = null;
        String address = null;
        String city = null;
        String pobox = null;
        String postcode = null;
        String region   = null;
        String country  = null;
        String shipping_address = null;
        String shipping_city = null;
        String shipping_pobox = null;
        String shipping_postcode = null;
        String shipping_region   = null;
        String shipping_country  = null;
        final Cursor c =
                resolver.query(DataQuery.CONTENT_URI, DataQuery.PROJECTION, DataQuery.SELECTION,
                new String[] {String.valueOf(rawContactId)}, null);
        final ContactOperations contactOp =
                ContactOperations.updateExistingContact(context, rawContactId,
                inSync, batchOperation);
        try {
            // Iterate over the existing rows of data, and update each one
            // with the information we received from the server.
            while (c.moveToNext()) {
                final long id = c.getLong(DataQuery.COLUMN_ID);
                final String mimeType = c.getString(DataQuery.COLUMN_MIMETYPE);
                final Uri uri = ContentUris.withAppendedId(Data.CONTENT_URI, id);

                if (mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                    final String lastName =
                        c.getString(DataQuery.COLUMN_FAMILY_NAME);
                    final String firstName =
                        c.getString(DataQuery.COLUMN_GIVEN_NAME);
                    contactOp.updateName(uri, firstName, lastName, user
                        .getFirstName(), user.getLastName());
                }
                if (mimeType.equals(Organization.CONTENT_ITEM_TYPE)) {
                    organisation =
                      c.getString(DataQuery.COLUMN_ORGANISATION_NAME);                   
                    contactOp.updateOrganisation(organisation, user.getOrganisation(),uri);
                    
                    organisation_title =
                            c.getString(DataQuery.COLUMN_ORGANISATION_TITLE);                   
                    contactOp.updateOrganisationTitle(organisation_title, user.getOrganisationTitle(),uri);
                    
                    organisation_department =
                            c.getString(DataQuery.COLUMN_ORGANISATION_DEPARTMENT);                   
                    contactOp.updateOrganisationTitle(organisation_department, user.getOrganisationDepartment(),uri);
                }
                // TODO: update postal address
                
                else if (mimeType.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
//                    address,city,region,pobox,postcode,country,
//                    ship_address,ship_city,ship_region,ship_pobox,ship_postcode,ship_country,
                	String address_label;
                    address_label = c.getString(DataQuery.COLUMN_LABEL_POSTAL);
                    if (address_label.equals(Constants.billing_address))
                    {
                                    
                    address = c.getString(DataQuery.COLUMN_ADRESS1_POSTAL);
                    contactOp.updateField(StructuredPostal.STREET,address, user.getAddress(),uri);

                    city = c.getString(DataQuery.COLUMN_CITY1_POSTAL);
                    contactOp.updateField(StructuredPostal.CITY,city, user.getCity(),uri);
                   
                    region = c.getString(DataQuery.COLUMN_REGION1_POSTAL);
                    contactOp.updateField(StructuredPostal.REGION,region, user.getRegion(),uri);

                    pobox = c.getString(DataQuery.COLUMN_POBOX1_POSTAL);
                    contactOp.updateField(StructuredPostal.POBOX,pobox, user.getPobox(),uri);

                    postcode = c.getString(DataQuery.COLUMN_POSTCODE1_POSTAL);
                    contactOp.updateField(StructuredPostal.POSTCODE,postcode, user.getPostCode(),uri);

                    country = c.getString(DataQuery.COLUMN_COUNTRY1_POSTAL);
                    contactOp.updateField(StructuredPostal.COUNTRY,country, user.getCountry(),uri);
                    }
                    // ==== shipping adress ===
                    if (address_label.equals(Constants.shipping_address))
                    {
                    
                    shipping_address = c.getString(DataQuery.COLUMN_ADRESS1_POSTAL);
                    contactOp.updateField(StructuredPostal.STREET,shipping_address, user.getOtherAddress(),uri);

                    shipping_city = c.getString(DataQuery.COLUMN_CITY1_POSTAL);
                    contactOp.updateField(StructuredPostal.CITY,shipping_city, user.getOtherCity(),uri);

                    shipping_region = c.getString(DataQuery.COLUMN_REGION1_POSTAL);
                    contactOp.updateField(StructuredPostal.REGION,shipping_region, user.getOtherRegion(),uri);

                    shipping_pobox = c.getString(DataQuery.COLUMN_POBOX1_POSTAL);
                    contactOp.updateField(StructuredPostal.POBOX,shipping_pobox, user.getOtherPobox(),uri);

                    shipping_postcode = c.getString(DataQuery.COLUMN_POSTCODE1_POSTAL);
                    contactOp.updateField(StructuredPostal.POSTCODE,shipping_postcode, user.getOtherPostCode(),uri);

                    shipping_country = c.getString(DataQuery.COLUMN_COUNTRY1_POSTAL);
                    contactOp.updateField(StructuredPostal.COUNTRY,shipping_country, user.getOtherCountry(),uri);
                    }
                    
                }
                else if (mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                    final int type = c.getInt(DataQuery.COLUMN_PHONE_TYPE);

                    if (type == Phone.TYPE_MOBILE) {
                        cellPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updatePhone(cellPhone, user.getCellPhone(),
                            uri);
                    } else if (type == Phone.TYPE_OTHER) {
                        otherPhone = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updatePhone(otherPhone, user.getHomePhone(),
                            uri);
                    }
                    else if (type == Phone.TYPE_FAX_WORK) {
                        fax = c.getString(DataQuery.COLUMN_PHONE_NUMBER);
                        contactOp.updateFax(fax, user.getFax(),
                            uri);
                    }
                }
                else if (mimeType.equals(Website.CONTENT_ITEM_TYPE)) {
                    website = c.getString(DataQuery.COLUMN_WEBSITE_ADDRESS);
                    contactOp.updateWebsite(user.getWebsite(),website, uri);               
                    }
                else if (mimeType.equals(Email.CONTENT_ITEM_TYPE)) {                	
                    email = c.getString(DataQuery.COLUMN_EMAIL_ADDRESS);
                    Log.d(TAG,email);
                    Log.d(TAG,user.getEmail());
                    contactOp.updateEmail(user.getEmail(), email, uri);               }
            } // while
        } finally {
            c.close();
        }

        if ((address == null) && (city == null) && (region == null) && (country == null) && (pobox == null) && (postcode == null))
        	{
        	address = user.getAddress();
        	city = user.getCity();
        	region = user.getRegion();
        	country = user.getCountry();
        	pobox = user.getPobox();
        	postcode = user.getPostCode();
        	contactOp.addAddress(address,city,region,country,pobox,postcode, Constants.billing_address);

        	}
        
        if ((shipping_address == null) && (shipping_city == null) && (shipping_region == null) && (shipping_country == null) && (shipping_pobox == null) && (shipping_postcode == null))
    	{
    	shipping_address = user.getOtherAddress();
    	shipping_city = user.getOtherCity();
    	shipping_region = user.getOtherRegion();
    	shipping_country = user.getOtherCountry();
    	shipping_pobox = user.getOtherPobox();
    	shipping_postcode = user.getOtherPostCode();
    	contactOp.addAddress(shipping_address,shipping_city,shipping_region,shipping_country,shipping_pobox,shipping_postcode, Constants.shipping_address);

    	}  
        // Add the cell phone, if present and not updated above
        if (fax == null) {
            contactOp.addPhone(user.getFax(), Phone.TYPE_FAX_WORK);
        }
        // Add the cell phone, if present and not updated above
        if (cellPhone == null) {
            contactOp.addPhone(user.getCellPhone(), Phone.TYPE_MOBILE);
        }
        // Add the cell phone, if present and not updated above
        if (website == null) {
            contactOp.addWebSite(user.getWebsite());
        }
        // Add the other phone, if present and not updated above
        if (otherPhone == null) {
            contactOp.addPhone(user.getHomePhone(), Phone.TYPE_OTHER);
        }
        // Add the email address, if present and not updated above
        if (email == null) {
            contactOp.addEmail(user.getEmail());
        }

        // Add the organisation, if present and not updated above
        if (organisation == null) {
            contactOp.addOrganisation(user.getOrganisation());
        }
        // Add the organisation department, if present and not updated above
        if (organisation_department == null) {
            contactOp.addOrganisationDepartment(user.getOrganisationDepartment());
        }
        if (organisation_title == null) {
            contactOp.addOrganisationTitle(user.getOrganisationTitle());
        }
    }
    /**
     * When we first add a sync adapter to the system, the contacts from that
     * sync adapter will be hidden unless they're merged/grouped with an existing
     * contact.  But typically we want to actually show those contacts, so we
     * need to mess with the Settings table to get them to show up.
     *
     * @param context the Authenticator Activity context
     * @param account the Account who's visibility we're changing
     * @param visible true if we want the contacts visible, false for hidden
     */
    public static void setAccountContactsVisibility(Context context, Account account,
            boolean visible) {
        ContentValues values = new ContentValues();
        values.put(RawContacts.ACCOUNT_NAME, account.name);
        values.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        values.put(Settings.UNGROUPED_VISIBLE, visible ? 1 : 0);

        context.getContentResolver().insert(Settings.CONTENT_URI, values);
    }

        /**
     * Deletes a contact from the platform contacts provider. This method is used
     * both for contacts that were deleted locally and then that deletion was synced
     * to the server, and for contacts that were deleted on the server and the
     * deletion was synced to the client.
     *
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id for this rawContact in contacts
     *        provider
     */
    private static void deleteContact(Context context, long rawContactId,
        BatchOperation batchOperation) {
        batchOperation.add(ContactOperations.newDeleteCpo(
            ContentUris.withAppendedId(RawContacts.CONTENT_URI, rawContactId),
            true,true).build());
    }

    /**
     * Returns the RawContact id for a sample SyncAdapter contact, or 0 if the
     * sample SyncAdapter user isn't found.
     *
     * @param resolver the content resolver to use
     * @param serverContactId the sample SyncAdapter user ID to lookup
     * @return the RawContact id, or 0 if not found
     */
    private static long lookupRawContact(ContentResolver resolver, long serverContactId) {
        long rawContactId = 0;
        final Cursor c =
            resolver.query(UserIdQuery.CONTENT_URI, UserIdQuery.PROJECTION,
                UserIdQuery.SELECTION, new String[] {String.valueOf(serverContactId)},
                null);
        try {
            if ((c != null) && c.moveToFirst()) {
                rawContactId = c.getLong(UserIdQuery.COLUMN_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return rawContactId;
    }

    /**
     * Returns the Data id for a sample SyncAdapter contact's profile row, or 0
     * if the sample SyncAdapter user isn't found.
     * 
     * @param resolver a content resolver
     * @param userId the sample SyncAdapter user ID to lookup
     * @return the profile Data row id, or 0 if not found
     */
    private static long lookupProfile(ContentResolver resolver, long userId) {
        long profileId = 0;
        final Cursor c =
            resolver.query(Data.CONTENT_URI, ProfileQuery.PROJECTION, ProfileQuery.SELECTION,
                new String[] {String.valueOf(userId)}, null);
        try {
            if ((c != null) && c.moveToFirst()) {
                profileId = c.getLong(ProfileQuery.COLUMN_ID);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return profileId;
    }

    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
    final private static class ProfileQuery {

        private ProfileQuery() {
        }

        public final static String[] PROJECTION = new String[] {Data._ID};

        public final static int COLUMN_ID = 0;

        public static final String SELECTION =
            Data.MIMETYPE + "='" + SampleSyncAdapterColumns.MIME_PROFILE
                + "' AND " + SampleSyncAdapterColumns.DATA_PID + "=?";
    }
    /**
     * Constants for a query to find a contact given a sample SyncAdapter user
     * ID.
     */
final private static class UserIdQuery {

        private UserIdQuery() {
        }
        public final static String[] PROJECTION =
            new String[] {RawContacts._ID};

        public final static int COLUMN_ID = 0;
        public final static Uri CONTENT_URI = RawContacts.CONTENT_URI;
        public static final String SELECTION =
            RawContacts.ACCOUNT_TYPE + "='" + Constants.ACCOUNT_TYPE + "' AND "
                + RawContacts.SOURCE_ID + "=?";
    }

    /**
     * Constants for a query to get contact data for a given rawContactId
     */
    final private static class DataQuery {

        private DataQuery() {
        }

        public static final String[] PROJECTION =
            new String[] {Data._ID, Data.MIMETYPE, Data.DATA1, Data.DATA2,
                Data.DATA3,Data.DATA4,Data.DATA5,Data.DATA6,Data.DATA7,Data.DATA8,Data.DATA9,Data.DATA10 };

        public static final int COLUMN_ID = 0;
        public static final int COLUMN_MIMETYPE = 1;
        public static final int COLUMN_DATA1 = 2;
        public static final int COLUMN_DATA2 = 3;
        public static final int COLUMN_DATA3 = 4;
        public static final int COLUMN_DATA4 = 5;
        public static final int COLUMN_DATA5 = 6;
        public static final int COLUMN_DATA6 = 7;
        public static final int COLUMN_DATA7 = 8;
        public static final int COLUMN_DATA8 = 9;
        public static final int COLUMN_DATA9 = 10;
        public static final int COLUMN_DATA10 = 11;
        
        public static final Uri CONTENT_URI = Data.CONTENT_URI;
        
        public static final int COLUMN_PHONE_NUMBER = COLUMN_DATA1;
        public static final int COLUMN_PHONE_TYPE = COLUMN_DATA2;
        
        public static final int COLUMN_EMAIL_ADDRESS = COLUMN_DATA1;
        
        public static final int COLUMN_GIVEN_NAME = COLUMN_DATA2;
        public static final int COLUMN_FAMILY_NAME = COLUMN_DATA3;

        public static final int COLUMN_WEBSITE_ADDRESS = COLUMN_DATA1;
        
        public static final int COLUMN_ORGANISATION_NAME = COLUMN_DATA1;
        public static final int COLUMN_ORGANISATION_TITLE = COLUMN_DATA2;
        public static final int COLUMN_ORGANISATION_DEPARTMENT = COLUMN_DATA3;
        // StructuredPostalCode
        public static final int COLUMN_LABEL_POSTAL = COLUMN_DATA3;
        public static final int COLUMN_ADRESS1_POSTAL = COLUMN_DATA4;
        public static final int COLUMN_POBOX1_POSTAL  = COLUMN_DATA5;
        public static final int COLUMN_CITY1_POSTAL   = COLUMN_DATA7;
        public static final int COLUMN_REGION1_POSTAL = COLUMN_DATA8;
        public static final int COLUMN_POSTCODE1_POSTAL = COLUMN_DATA9;
        public static final int COLUMN_COUNTRY1_POSTAL = COLUMN_DATA10;

        
        public static final String SELECTION = Data.RAW_CONTACT_ID + "=?";
    }
}
