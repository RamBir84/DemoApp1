package com.example.demoapp;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {
	public static final int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	NotificationCompat.Builder builder;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
		
		// The getMessageType() intent parameter must be the intent you received in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) { // has effect of unparcelling Bundle
			/*
			 * Filter messages based on message type. Since it is likely that
			 * GCM will be extended in the future with new message types, just
			 * ignore any message types you're not interested in, or that you
			 * don't recognize.
			 */

			/*if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
				sendNotification("Send error: " + extras.toString());
			} else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
				sendNotification("Deleted messages on server: " + extras.toString());
			} else*/
			
			// If it's a regular GCM message, do some work.
				if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
				
				// Post notification of received message.
				sendNotification(extras.getString("message"));
				Log.i(HomeScreen.TAG, "Received: " + extras.toString());
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

/*   
	Put the message into a notification and post it. 
	This is just one simple example of what you might choose to do with a GCM message.
	
	The msg fields: 1. type of message 2. ID 3. name 4. tag (empty in type '1')
	For example: 2,301633590,or bokobza,in some place.
*/
	private void sendNotification(String msg) {
		char type;
		String id, name;
		String tag = "";
		int start = 0;
		int end = 1;
		
		// type field
		type = msg.charAt(start);
		
		// Id field
		start = end + 1;
		//end++;
		end = start;
		while (msg.charAt(end) != ',')
			end++;
		id = msg.substring(start, end);
		
		// name field
		start = end + 1;
		//end++;
		end = start;
		while (msg.charAt(end) != ',' && msg.charAt(end) != '.')
			end++;
		name = msg.substring(start, end);
		
		// tag field
		if (msg.charAt(end) == ',') {
			start = end + 1;
			//end++;
			end = start;
			while (msg.charAt(end) != '.')
				end++;
			tag = msg.substring(start, end);
		}
		notification(type, id, name, tag);
	}

	
	private void notification(char type, String id, String name, String tag) {
		mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
		NotificationCompat.Builder mBuilder;
		PendingIntent contentIntent;

		if (type == '1') { // If from type 1 (request Received)
			
			Intent intentForNotification = new Intent(this, TagsScreen.class);
			intentForNotification.putExtra("gcm_id", id);
			
			contentIntent = PendingIntent.getActivity(this, 0, intentForNotification, PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_notification) // have to set a new icon here(if we want to)
					.setContentTitle("Request Received")
					.setStyle(new NotificationCompat.BigTextStyle()
					.bigText("Request received from: " + name))
					.setContentText("Request received from: " + name);
			
		} else { // If from type 2 (location Received)
			
			Intent intentForNotification = new Intent(this, NewHomeScreen.class);
			intentForNotification.putExtra("user_id", id);
			intentForNotification.putExtra("tag", tag);			
			
			contentIntent = PendingIntent.getActivity(this, 0, intentForNotification, 0);
			mBuilder = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_notification) // have to set a new icon here(if we want to)
					.setContentTitle("Location Received")
					.setStyle(new NotificationCompat.BigTextStyle()
					.bigText("location received from: " + name))
					.setContentText("location received from: " + name);
		}

		//mBuilder.setDefaults(Notification.DEFAULT_ALL);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());

		// Here we can send the Id and tag Tag to the NewHomeScreen
	}
}

/*switch (type) {
case 1:

	contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, NewHomeScreen.class), 0);
	mBuilder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_launcher) // have to set a new icon here(if we want to)
			.setContentTitle("Location Received")
			.setStyle(new NotificationCompat.BigTextStyle()
			.bigText("location received from: " + name))
			.setContentText("location received from: " + name);
	
	 * Here we can should handle the Id field
	 

	// If from type 2 (request Received)

	break;

case 2:

	contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, TagsScreen.class), 0);
	mBuilder = new NotificationCompat.Builder(this)
			.setSmallIcon(R.drawable.ic_launcher) // have to set a new icon here(if we want to)
			.setContentTitle("Request Received")
			.setStyle(new NotificationCompat.BigTextStyle()
			.bigText("Request received from: " + name))
			.setContentText("Request received from: " + name);
	
	 * Here we can should handle the Id and Tag field
	 

	break;
	
default:
	
	break;
}*/
