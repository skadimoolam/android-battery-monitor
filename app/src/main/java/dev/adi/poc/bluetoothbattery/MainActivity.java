package dev.adi.poc.bluetoothbattery;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.palaima.smoothbluetooth.Device;
import io.palaima.smoothbluetooth.SmoothBluetooth;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final String PREF_NAME = "dev.adi.testapp.prefs";
    private static final String SELECTED_DEVICE_ADDRESS = "selected_device_address";
    private static final String SELECTED_DEVICE_NAME = "selected_device_name";

    SharedPreferences.Editor prefEditor;
    SharedPreferences preferences;

    private BluetoothAdapter bluetoothAdapter;
    private ArrayList<BluetoothDevice> pairedDevicesList = new ArrayList<>();
    List<String> pairedDevicesNames = new ArrayList<>();

    BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryState = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            setBattPercentage(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));

            switch (batteryState) {
                case BatteryManager.BATTERY_STATUS_FULL:
                    Toast.makeText(context, "Battery Full", Toast.LENGTH_SHORT).show();

                    if (preferences.getString(SELECTED_DEVICE_ADDRESS, null) != null) {
                        for (BluetoothDevice device : pairedDevicesList) {
                            if (device.getAddress() == preferences.getString(SELECTED_DEVICE_ADDRESS, null)) {
                                showToast("found and connecting");
                                connectAndSend(device);
                            }
                        }
                    } else {
                        showToast("Please select a device");
                    }
                    break;

                default:
//                    Toast.makeText(context, "Battery Default message", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefEditor = preferences.edit();

        if (preferences.getString(SELECTED_DEVICE_ADDRESS, "sample") == "sample") {
            showToast("Please select a device");
        }

        setupBluetooth();
    }

    public void setDefaultDevice(View view) {
        final String[] namesArr = pairedDevicesNames.toArray(new String[pairedDevicesNames.size()]);

        int selectedIndex = 0;

        for(int i = 0; i < pairedDevicesNames.size(); ++i){
            if (pairedDevicesNames.get(i).contains(preferences.getString(SELECTED_DEVICE_ADDRESS, "sample"))) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Device")
                .setSingleChoiceItems(namesArr, selectedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int pos) {
                        prefEditor.putString(SELECTED_DEVICE_ADDRESS, pairedDevicesList.get(pos).getAddress());
                        prefEditor.putString(SELECTED_DEVICE_NAME, pairedDevicesList.get(pos).getName());
                        prefEditor.apply();
                    }
                })
                .setCancelable(false)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", null)
                .show();
    }

    private void setupBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
                showToast("Bluetooth: Enabled");
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                if (pairedDevices.size() > 0) {
                    for (BluetoothDevice device: pairedDevices) {
                        pairedDevicesList.add(device);
                        pairedDevicesNames.add(device.getName() + " - " + device.getAddress());
                    }
                } else {
                    showToast("No paired devices");
                }
            } else {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 1);
            }
        } else {
            showToast("Your phone does not support bluetooth");
        }
    }

    public void connectAndSend(BluetoothDevice device) {
        UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        BluetoothSocket socket = null;

        try {
            socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            socket.connect();
        } catch (Exception e) {
            showToast(e.toString());
            e.printStackTrace();
        }

        if (socket != null) {
            try {
                OutputStream outStream = socket.getOutputStream();
                outStream.write("1".getBytes());
                showToast("Wrote to output");
                socket.close();
            } catch (Exception e) {
                showToast(e.toString());
                e.printStackTrace();
            }
        }
    }

    public void setBattPercentage(int battPercentage) {
        TextView tvBattPercent = (TextView) findViewById(R.id.tv_batt_percentage);
        tvBattPercent.setText(battPercentage + "%");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryInfoReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 0) {
            showToast("Bluetooth : Not Enabled");
        } else {
            setupBluetooth();
        }
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }
}
