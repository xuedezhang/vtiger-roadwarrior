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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.text.TextUtils;
import android.util.Log;

import com.roadwarrior.vtiger.Constants;
import com.roadwarrior.vtiger.R;

/**
 * Helper class for storing data in the platform content providers.
 */
public class ContactOperations {

    private final ContentValues mValues;

    private final BatchOperation mBatchOperation;
    private final Context mContext;
    private boolean mIsSyncOperation;
    private long mRawContactId;
    private int mBackReference;
    private boolean mIsNewContact;

    /**
     * Since we're sending a lot of contact provider operations in a single
     * batched operation, we want to make sure that we "yield" periodically
     * so that the Contact Provider can write changes to the DB, and can
     * open a new transaction.  This prevents ANR (application not responding)
     * errors.  The recommended time to specify that a yield is permitted is
     * with the first operation on a particular contact.  So if we're updating
     * multiple fields for a single contact, we make sure that we call
     * withYieldAllowed(true) on the first field that we update. We use
     * mIsYieldAllowed to keep track of what value we should pass to
     * withYieldAllowed().
     */
    private boolean mIsYieldAllowed;

    /**
     * Returns an instance of ContactOperations instance for adding new contact
     * to the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param userId the userId of the sample SyncAdapter user object
     * @param accountName the username for the SyncAdapter account
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of ContactOperations
     */
    public static ContactOperations createNewContact(Context context, long userId,
            String accountName, boolean isSyncOperation, BatchOperation batchOperation) {
        return new ContactOperations(context, userId, accountName, isSyncOperation, batchOperation);
    }

    /**
     * Returns an instance of ContactOperations for updating existing contact in
     * the platform contacts provider.
     * 
     * @param context the Authenticator Activity context
     * @param rawContactId the unique Id of the existing rawContact
     * @param isSyncOperation are we executing this as part of a sync operation?
     * @return instance of ContactOperations
     */
    public static ContactOperations updateExistingContact(Context context, long rawContactId,
            boolean isSyncOperation, BatchOperation batchOperation) {
        return new ContactOperations(context, rawContactId, isSyncOperation, batchOperation);
    }

    public ContactOperations(Context context, boolean isSyncOperation,
            BatchOperation batchOperation) {
        mValues = new ContentValues();
        mIsYieldAllowed = true;
        mIsSyncOperation = isSyncOperation;
        mContext = context;
        mBatchOperation = batchOperation;
    }

    public ContactOperations(Context context, long userId, String accountName,
            boolean isSyncOperation, BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mBackReference = mBatchOperation.size();
        mIsNewContact = true;
        mValues.put(RawContacts.SOURCE_ID, userId);
        mValues.put(RawContacts.ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        mValues.put(RawContacts.ACCOUNT_NAME, accountName);
        ContentProviderOperation.Builder builder =
                newInsertCpo(RawContacts.CONTENT_URI, mIsSyncOperation, true).withValues(mValues);
        mBatchOperation.add(builder.build());
    }

    public ContactOperations(Context context, long rawContactId, boolean isSyncOperation,
            BatchOperation batchOperation) {
        this(context, isSyncOperation, batchOperation);
        mIsNewContact = false;
        mRawContactId = rawContactId;
    }

   
    /**
     * Adds a contact name
     * 
     * @param name Name of contact
     * @param nameType type of name: family name, given name, etc.
     * @return instance of ContactOperations
     */
    public ContactOperations addName(String firstName, String lastName) {
        mValues.clear();
        if (!TextUtils.isEmpty(firstName)) {
            mValues.put(StructuredName.GIVEN_NAME, firstName);
            mValues.put(StructuredName.MIMETYPE,
                StructuredName.CONTENT_ITEM_TYPE);
        }
        if (!TextUtils.isEmpty(lastName)) {
            mValues.put(StructuredName.FAMILY_NAME, lastName);
            mValues.put(StructuredName.MIMETYPE,
                StructuredName.CONTENT_ITEM_TYPE);
        }
        if (mValues.size() > 0) {
            addInsertOp();
        }
        return this;
    }

    /**
     * Adds an email
     * 
     * @param new email for user
     * @return instance of ContactOperations
     */
    public ContactOperations addEmail(String email) {
        mValues.clear();
        if (!TextUtils.isEmpty(email)) {
            mValues.put(Email.DATA, email);
            mValues.put(Email.TYPE, Email.TYPE_WORK);
            mValues.put(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }

    /**
     * Adds a phone number
     * 
     * @param phone new phone number for the contact
     * @param phoneType the type: cell, home, etc.
     * @return instance of ContactOperations
     */
    public ContactOperations addPhone(String phone, int phoneType) {
        mValues.clear();
        if (!TextUtils.isEmpty(phone)) {
            mValues.put(Phone.NUMBER, phone);
            mValues.put(Phone.TYPE, phoneType);
            mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }


    /**
     * Adds a fax number
     * 
     * @param fax new phone number for the contact
    .
     * @return instance of ContactOperations
     */
    public ContactOperations addFax(String fax) {
        mValues.clear();
        if (!TextUtils.isEmpty(fax)) {
            mValues.put(Phone.NUMBER,fax);
            mValues.put(Phone.TYPE, Phone.TYPE_FAX_WORK);
            mValues.put(Phone.MIMETYPE, Phone.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }
    /**
     * Adds a website
     * 
     * @param phone new phone number for the contact
     * @param phoneType the type: cell, home, etc.
     * @return instance of ContactOperations
     */
    public ContactOperations addWebSite(String website) {
        mValues.clear();
        if (!TextUtils.isEmpty(website)) {
            mValues.put(Website.URL, website);
            mValues.put(Website.TYPE, Website.TYPE_HOMEPAGE);
            mValues.put(Website.MIMETYPE, Website.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }
    /**
     * Adds a website
     * 
     * @param phone new phone number for the contact
     * @param phoneType the type: cell, home, etc.
     * @return instance of ContactOperations
     */
    public ContactOperations addAddress(String address,String city,String region,String country,String pobox,String postcode,String AddressType) {
        mValues.clear();
        boolean ok = false;
        if (!TextUtils.isEmpty(address)) {
            mValues.put(StructuredPostal.STREET, address);
            ok = true;
        }
        if (!TextUtils.isEmpty(city)) {
            mValues.put(StructuredPostal.CITY, city);
            ok = true;
        }
        
        if (!TextUtils.isEmpty(pobox)) {
            mValues.put(StructuredPostal.POBOX, pobox);
            ok = true;
        }
        if (!TextUtils.isEmpty(postcode)) {
            mValues.put(StructuredPostal.POSTCODE, postcode);
            ok = true;
        }
        if (!TextUtils.isEmpty(country)) {
            mValues.put(StructuredPostal.COUNTRY, country);
            ok = true;
        }
        if (!TextUtils.isEmpty(region)) {
            mValues.put(StructuredPostal.REGION, region);
            ok = true;
        }
        mValues.put(StructuredPostal.LABEL,AddressType);
        
        if (ok){
            mValues.put(StructuredPostal.TYPE, StructuredPostal.TYPE_CUSTOM);
            mValues.put(StructuredPostal.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
    }
    /**
     * Adds organisation
     * 
     * @param orgnisation organisation the contact
     * @return instance of ContactOperations
     */
    public ContactOperations addOrganisation(String organisation,String organisation_department,String organisation_title) {
        boolean ok = false;
    	mValues.clear();

    	
        if (!TextUtils.isEmpty(organisation_title)) {
            mValues.put(Organization.TITLE,organisation_title);
            ok = true;
        }
        if (!TextUtils.isEmpty(organisation_department)) {
            mValues.put(Organization.DEPARTMENT, organisation_department);
            ok = true;
        }

        if (!TextUtils.isEmpty(organisation)) {
            mValues.put(Organization.COMPANY, organisation);
            ok = true;
            
        }
        mValues.put(Organization.LABEL,"Organization");
        if (ok){
            mValues.put(Organization.TYPE, Organization.TYPE_CUSTOM);
            mValues.put(Organization.MIMETYPE, Organization.CONTENT_ITEM_TYPE);
            addInsertOp();
        }
        return this;
  
    }
    
  
   
    /**
     * Adds a group membership
     *
     * @param id The id of the group to assign
     * @return instance of ContactOperations
     */
    public ContactOperations addGroupMembership(long groupId) {
        mValues.clear();
        mValues.put(GroupMembership.GROUP_ROW_ID, groupId);
        mValues.put(GroupMembership.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE);
        addInsertOp();
        return this;
    }

    /**
     * Adds a profile action
     * 
     * @param userId the userId of the sample SyncAdapter user object
     * @return instance of ContactOperations
     */
    public ContactOperations addProfileAction(long userId) {
        mValues.clear();
        if (userId != 0) {
            mValues.put(SampleSyncAdapterColumns.DATA_PID, userId);
            mValues.put(SampleSyncAdapterColumns.DATA_SUMMARY, mContext
                .getString(R.string.profile_action));
            mValues.put(SampleSyncAdapterColumns.DATA_DETAIL, mContext
                .getString(R.string.view_profile));
            mValues.put(Data.MIMETYPE, SampleSyncAdapterColumns.MIME_PROFILE);
            addInsertOp();
        }
        return this;
    }

    /**
     * Updates contact's email
     * 
     * @param email email id of the sample SyncAdapter user
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateEmail(String email, String existingEmail,
        Uri uri) {
        if (!TextUtils.equals(existingEmail, email)) {
            mValues.clear();
            mValues.put(Email.DATA, email);
            addUpdateOp(uri);
        }
        return this;
    }

    /**
     * Updates website
     * 
     * @param websitel id of the sample SyncAdapter user
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateWebsite(String website, String existingWebsite,
        Uri uri) {
        if (!TextUtils.equals(existingWebsite, website)) {
            mValues.clear();
            mValues.put(Website.DATA, website);
            addUpdateOp(uri);
        }
        return this;
    }
    /**
     * Updates contact's name
     * 
     * @param name Name of contact
     * @param existingName Name of contact stored in provider
     * @param nameType type of name: family name, given name, etc.
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateName(Uri uri, String existingFirstName,
        String existingLastName, String firstName, String lastName) {
        Log.i("ContactOperations", "ef=" + existingFirstName + "el="
            + existingLastName + "f=" + firstName + "l=" + lastName);
        mValues.clear();
        if (!TextUtils.equals(existingFirstName, firstName)) {
            mValues.put(StructuredName.GIVEN_NAME, firstName);
        }
        if (!TextUtils.equals(existingLastName, lastName)) {
            mValues.put(StructuredName.FAMILY_NAME, lastName);
        }
        if (mValues.size() > 0) {
            addUpdateOp(uri);
        }
        return this;
    }

 
    
    /**
     * generic Updates Field
     * 
     * @param FieldName FieldName
     * @param existingValue existing field  stored in contacts provider
     * @param newvalue new fieldname name for the contact
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateField(String FieldName,String existingValue, String newvalue,
            Uri uri) {
            if (!TextUtils.equals(newvalue,existingValue)) {
                mValues.clear();
                mValues.put(FieldName, newvalue);
                addUpdateOp(uri);
            }
            return this;
        }
    /**
     * Updates organisation department
     * 
     * @param existing organisation department stored in contacts provider
     * @param phone new organisation department for the contact
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateOrganisationDepartment(String existingValue, String newvalue,
            Uri uri) {
            if (!TextUtils.equals(newvalue,existingValue)) {
                mValues.clear();
                mValues.put(Organization.DEPARTMENT, newvalue);
                addUpdateOp(uri);
            }
            return this;
        }
    /**
     * Updates contact's phone
     * 
     * @param existingNumber phone number stored in contacts provider
     * @param phone new phone number for the contact
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updatePhone(String existingNumber, String phone,
        Uri uri) {
        if (!TextUtils.equals(phone, existingNumber)) {
            mValues.clear();
            mValues.put(Phone.NUMBER, phone);
            addUpdateOp(uri);
        }
        return this;
    }
    /**
     * Updates contact's fax
     * 
     * @param existingNumber phone number stored in contacts provider
     * @param phone new phone number for the contact
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateFax(String existingNumber, String phone,
        Uri uri) {
        if (!TextUtils.equals(phone, existingNumber)) {
            mValues.clear();
            mValues.put(Phone.NUMBER, phone);
            addUpdateOp(uri);
        }
        return this;
    }
    /**
     * Updates contact's profile action
     * 
     * @param userId sample SyncAdapter user id
     * @param uri Uri for the existing raw contact to be updated
     * @return instance of ContactOperations
     */
    public ContactOperations updateProfileAction(Integer userId, Uri uri) {
        mValues.clear();
        mValues.put(SampleSyncAdapterColumns.DATA_PID, userId);
        addUpdateOp(uri);
        return this;
    }

    /**
     * Adds an insert operation into the batch
     */
    private void addInsertOp() {
        if (!mIsNewContact) {
            mValues.put(Phone.RAW_CONTACT_ID, mRawContactId);
        }
        ContentProviderOperation.Builder builder =
                newInsertCpo(Data.CONTENT_URI, mIsSyncOperation, mIsYieldAllowed);
        builder.withValues(mValues);
        if (mIsNewContact) {
            builder.withValueBackReference(Data.RAW_CONTACT_ID, mBackReference);
        }
        mIsYieldAllowed = false;
        mBatchOperation.add(builder.build());
    }

    /**
     * Adds an update operation into the batch
     */
    private void addUpdateOp(Uri uri) {
        ContentProviderOperation.Builder builder =
                newUpdateCpo(uri, mIsSyncOperation, mIsYieldAllowed).withValues(mValues);
        mIsYieldAllowed = false;
        mBatchOperation.add(builder.build());
    }

    public static ContentProviderOperation.Builder newInsertCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newInsert(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    public static ContentProviderOperation.Builder newUpdateCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newUpdate(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    public static ContentProviderOperation.Builder newDeleteCpo(Uri uri,
            boolean isSyncOperation, boolean isYieldAllowed) {
        return ContentProviderOperation
                .newDelete(addCallerIsSyncAdapterParameter(uri, isSyncOperation))
                .withYieldAllowed(isYieldAllowed);
    }

    private static Uri addCallerIsSyncAdapterParameter(Uri uri, boolean isSyncOperation) {
        if (isSyncOperation) {
            // If we're in the middle of a real sync-adapter operation, then go ahead
            // and tell the Contacts provider that we're the sync adapter.  That
            // gives us some special permissions - like the ability to really
            // delete a contact, and the ability to clear the dirty flag.
            //
            // If we're not in the middle of a sync operation (for example, we just
            // locally created/edited a new contact), then we don't want to use
            // the special permissions, and the system will automagically mark
            // the contact as 'dirty' for us!
            return uri.buildUpon()
                    .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                    .build();
        }
        return uri;
    }

}
