package dev.adi.poc.bluetoothbattery;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.List;

import io.palaima.smoothbluetooth.Device;
import io.palaima.smoothbluetooth.SmoothBluetooth;

public class MainActivity2 extends AppCompatActivity {

    public static final String TAG = MainActivity2.class.getSimpleName();

    private SmoothBluetooth mSmoothBluetooth;
    private SmoothBluetooth.Listener mBluetoothListener = new SmoothBluetooth.Listener() {
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

        }

        @Override
        public void onDisconnected() {
            showToast("Bluetooth : Disconnect");
        }

        @Override
        public void onConnectionFailed(Device device) {

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
        public void onDevicesFound(final List<Device> deviceList, SmoothBluetooth.ConnectionCallback connectionCallback) {
            Log.i(TAG, "List device");
            showToast(deviceList.toString());
            showToast(deviceList.get(0).getAddress());
            connectionCallback.connectTo(deviceList.get(0));
        }

        @Override
        public void onDataReceived(int data) {
            showToast("Bluetooth : data");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSmoothBluetooth = new SmoothBluetooth(this, SmoothBluetooth.ConnectionTo.OTHER_DEVICE, SmoothBluetooth.Connection.INSECURE, mBluetoothListener);
        mSmoothBluetooth.tryConnection();
        Log.i(TAG, "Send data");
        mSmoothBluetooth.send("1");
    }

    void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

}
