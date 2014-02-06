package com.roadwarrior.vtiger.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.roadwarrior.vtiger.receiver.Diagnoser;
public class DiagnoserReceiver extends BroadcastReceiver {
	@Override
    public void onReceive(Context context, Intent intent) {
        if ("android.provider.Telephony.SECRET_CODE".equals(intent.getAction())) {
            // open the diagnoser view 
        	Intent i = new Intent(Intent.ACTION_MAIN);
            i.setClass(context.getApplicationContext(), Diagnoser.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
            
           
            // FIXME: sending an email .. unfortunately does not work at the moment
//            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
//                    "mailto","abc@gmail.com", "toto"));
//            emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "EXTRA_SUBJECT");
//            context.startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
     }
}