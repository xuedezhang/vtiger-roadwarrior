package com.roadwarrior.vtiger.receiver;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.roadwarrior.vtiger.R;
import com.roadwarrior.vtiger.client.NetworkUtilities;

public class Diagnoser extends Activity {
	private TextView mMessage;

/** Called when the activity is first created. */
@Override
public void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);
setContentView(R.layout.diagnosis);
mMessage = (TextView) findViewById(R.id.message);
mMessage.setText(NetworkUtilities.getLogStatus());
}
}