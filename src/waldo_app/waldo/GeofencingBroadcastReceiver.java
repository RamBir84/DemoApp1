package waldo_app.waldo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class GeofencingBroadcastReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Intent startServiceIntent = new Intent(context, GeofencingService.class);
		context.startService(startServiceIntent);
	}
}
