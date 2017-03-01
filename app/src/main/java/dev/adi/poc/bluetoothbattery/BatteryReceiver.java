package dev.adi.poc.bluetoothbattery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.widget.Toast;

public class BatteryReceiver extends BroadcastReceiver {

    public static final String TAG = BatteryReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent nextIntent = new Intent(context.getApplicationContext(), MainActivity.class);
        nextIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(nextIntent);
    }

}
