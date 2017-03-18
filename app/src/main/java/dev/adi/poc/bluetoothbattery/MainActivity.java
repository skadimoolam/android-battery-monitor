package dev.adi.poc.bluetoothbattery;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.graphics.Color;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import io.palaima.smoothbluetooth.Device;
import io.palaima.smoothbluetooth.SmoothBluetooth;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getSimpleName();
    private static final String PREF_NAME = "dev.adi.poc.bluetoothbattery.prefs";
    private static final String SELECTED_DEVICE_ADDRESS = "selected_device_address";
    private static final String SELECTED_DEVICE_NAME = "selected_device_name";

    SharedPreferences.Editor prefEditor;
    SharedPreferences preferences;

    private List<Device> pairedDevicesList = new ArrayList<>();

    BroadcastReceiver batteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int batteryState = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

            setBattPercentage(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0));

            switch (batteryState) {
                case BatteryManager.BATTERY_STATUS_FULL:
                    setBattStatus("Full");
                    mSmoothBluetooth.send("1".getBytes());
                    break;

                case BatteryManager.BATTERY_STATUS_CHARGING:
                    setBattStatus("Charging");
                    break;

                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    setBattStatus("Discharging");
                    if (mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                        mSmoothBluetooth.disconnect();
                        mSmoothBluetooth.stop();
                    }
                    break;
            }
        }
    };

    BluetoothAdapter mBluetoothAdapter;
    SmoothBluetooth mSmoothBluetooth;
    SmoothBluetooth.ConnectionCallback connectionCallback;
    SmoothBluetooth.Listener mBluetoothListener = new SmoothBluetooth.Listener() {
        @Override
        public void onBluetoothNotSupported() {
            showToast("Bluetooth : Not Supported");
        }

        @Override
        public void onBluetoothNotEnabled() {
            showToast("Bluetooth : Not Enabled");
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 1);
        }

        @Override
        public void onConnecting(Device device) {

        }

        @Override
        public void onConnected(Device device) {
            setBlueConnected(device.getName());
            showToast("Bluetooth : Conneted to " + device.getName());
        }

        @Override
        public void onDisconnected() {
            setBlueConnected("disconnected");
            showToast("Bluetooth : Disconnect");
        }

        @Override
        public void onConnectionFailed(Device device) {
            showToast("Bluetooth : Could not connect, try again");
        }

        @Override
        public void onDiscoveryStarted() {

        }

        @Override
        public void onDiscoveryFinished() {

        }

        @Override
        public void onNoDevicesFound() {

        }

        @Override
        public void onDevicesFound(final List<Device> deviceList, SmoothBluetooth.ConnectionCallback cCallback) {
            connectionCallback = cCallback;
            pairedDevicesList = deviceList;

            if (preferences.getString(SELECTED_DEVICE_ADDRESS, "testing").equals("testing")) {
                selectDefaultDevice(pairedDevicesList, connectionCallback, true);
            } else {
                for(int i = 0; i < deviceList.size(); ++i){
                    if (deviceList.get(i).getAddress().contains(preferences.getString(SELECTED_DEVICE_ADDRESS, "sample"))) {
                        connectionCallback.connectTo(deviceList.get(i));
                    }
                }
            }
        }

        @Override
        public void onDataReceived(int data) {
            // showToast(String.valueOf((char) data));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        registerReceiver(batteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefEditor = preferences.edit();

        if (preferences.getString(SELECTED_DEVICE_ADDRESS, "testing").equals("testing")) {
            showToast("Please select a device");
        }

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mSmoothBluetooth = new SmoothBluetooth(this, SmoothBluetooth.ConnectionTo.OTHER_DEVICE, SmoothBluetooth.Connection.INSECURE, mBluetoothListener);
        mSmoothBluetooth.tryConnection();
    }

    public void selectDefaultDevice(final List<Device> deviceList, final SmoothBluetooth.ConnectionCallback cCallback, boolean exec) {
        List<String> pairedDevicesNames = new ArrayList<>();

        if (deviceList.size() > 0) {
            for (Device dev: deviceList) {
                pairedDevicesNames.add(dev.getName() + " - " + dev.getAddress());
            }
        } else {
            showToast("No paired devices");
        }

        final String[] namesArr = pairedDevicesNames.toArray(new String[pairedDevicesNames.size()]);

        int selectedIndex = 0;

        for(int i = 0; i < pairedDevicesNames.size(); ++i){
            if (pairedDevicesNames.get(i).contains(preferences.getString(SELECTED_DEVICE_ADDRESS, "sample"))) {
                selectedIndex = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Select A Device")
                .setSingleChoiceItems(namesArr, selectedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int pos) {
                        cCallback.connectTo(deviceList.get(pos));
                        mSmoothBluetooth.tryConnection();
                        prefEditor.putString(SELECTED_DEVICE_ADDRESS, deviceList.get(pos).getAddress());
                        prefEditor.putString(SELECTED_DEVICE_NAME, deviceList.get(pos).getName());
                        prefEditor.apply();
                    }
                })
                .setCancelable(false)
                .setNegativeButton("CANCEL", null)
                .setPositiveButton("SAVE", null)
                .show();
    }

    public void setBattPercentage(int battPercentage) {
        TextView tvBattPercent = (TextView) findViewById(R.id.tv_batt_percentage);
        tvBattPercent.setText(battPercentage + "%");
    }

    public void setBattStatus(String batStatus) {
        TextView tvBattPercent = (TextView) findViewById(R.id.tv_batt_status);
        RelativeLayout parent = (RelativeLayout) findViewById(R.id.activity_main);
        tvBattPercent.setText("STATUS : " + batStatus);

        if (batStatus.equals("Full")) {
            parent.setBackgroundColor(Color.parseColor("#b4eeb4"));
        } else if (batStatus.equals("Charging")) {
            parent.setBackgroundColor(Color.parseColor("#b0e0e6"));
        } else {
            parent.setBackgroundColor(Color.parseColor("#ff525e"));
        }
    }

     public void setBlueConnected(String connectedTo) {
         TextView tvConnectedTo = (TextView) findViewById(R.id.tv_blue_connected);

         if (connectedTo == "disconnected") {
             tvConnectedTo.setText("Bluetooth : Disconnected");
         } else {
             tvConnectedTo.setText("ConnectedTo : " + connectedTo);
         }
     }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == 0) {
            showToast("Bluetooth : Not Enabled");
        } else {
            mSmoothBluetooth = new SmoothBluetooth(this, SmoothBluetooth.ConnectionTo.OTHER_DEVICE, SmoothBluetooth.Connection.INSECURE, mBluetoothListener);
            mSmoothBluetooth.tryConnection();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(batteryInfoReceiver);
        mSmoothBluetooth.stop();
    }

    public void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    public void setDefaultDevice(View view) {
        selectDefaultDevice(pairedDevicesList, connectionCallback, false);
    }
}
