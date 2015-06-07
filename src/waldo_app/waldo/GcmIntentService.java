package waldo_app.waldo;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {

	public static int NOTIFICATION_ID = 1;
	private NotificationManager mNotificationManager;
	public SharedPreferences settings = null;

	public GcmIntentService() {
		super("GcmIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Bundle extras = intent.getExtras();
		GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);

		// The getMessageType() intent parameter must be the intent you received
		// in your BroadcastReceiver.
		String messageType = gcm.getMessageType(intent);

		if (!extras.isEmpty()) {
			// If it's a regular GCM message, do some work.
			if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {

				String gcmMsg = extras.getString("message");
				if (gcmMsg == null) {
					return;
				}

				// Post notification of received message.
				sendNotification(gcmMsg);
			}
		}
		// Release the wake lock provided by the WakefulBroadcastReceiver.
		GcmBroadcastReceiver.completeWakefulIntent(intent);
	}

	/*
	 * Put the message into a notification and post it. This is just one simple
	 * example of what you might choose to do with a GCM message.
	 * fields: 1. type of message 2. ID 3. name 4. tag (empty in type '1') For
	 * example: 2,301633590,or bokobza,in some place.
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
		end = start;
		while (msg.charAt(end) != ',')
			end++;
		id = msg.substring(start, end);

		// name field
		start = end + 1;
		end = start;
		while (msg.charAt(end) != ',' && msg.charAt(end) != '.')
			end++;
		name = msg.substring(start, end);

		// tag field
		if (msg.charAt(end) == ',') {
			start = end + 1;
			// end++;
			end = start;
			while (msg.charAt(end) != '.')
				end++;
			tag = msg.substring(start, end);
		}
		notification(type, id, name, tag);
	}

	private void notification(char type, String id, String name, String tag) {

		mNotificationManager = (NotificationManager) this
				.getSystemService(Context.NOTIFICATION_SERVICE);

		NotificationCompat.Builder mBuilder;

		PendingIntent contentIntent;

		if (type == '1') { // If from type 1 (request Received)

			Intent intentForNotification = new Intent(this, TagsScreen.class);
			intentForNotification.putExtra("gcm_id", id);

			contentIntent = PendingIntent.getActivity(this, 0, intentForNotification,
					PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_notification)
					// have to set a new icon here(if we want to)
					.setContentTitle("Request Received")
					.setStyle(
							new NotificationCompat.BigTextStyle().bigText("Request received from: "
									+ name)).setContentText("Request received from: " + name)
					.setAutoCancel(true);

		} else { // If from type 2 (location Received)
			settings = getSharedPreferences("UserInfo", 0);
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("location_received", 1);
			editor.commit();

			Intent intentForNotification = new Intent(this, NewHomeScreen.class);
			intentForNotification.putExtra("user_id", id);
			intentForNotification.putExtra("tag", tag);

			contentIntent = PendingIntent.getActivity(this, 0, intentForNotification,
					PendingIntent.FLAG_UPDATE_CURRENT);
			mBuilder = new NotificationCompat.Builder(this)
					.setSmallIcon(R.drawable.ic_notification)
					// have to set a new icon here(if we want to)
					.setContentTitle("Location Received")
					.setStyle(
							new NotificationCompat.BigTextStyle()
									.bigText("location received from: " + name))
					.setContentText("location received from: " + name).setAutoCancel(true);
			NOTIFICATION_ID++;
		}

		mBuilder.setDefaults(Notification.DEFAULT_ALL);
		mBuilder.setContentIntent(contentIntent);
		mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
	}
}
