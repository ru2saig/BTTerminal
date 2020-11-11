package com.examplebtterminal;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

  private int REQUEST_ENABLE_BT = 1;
  private static final int REQUEST_LOCATION = 123;
  private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
  private Button send;
  private TextView status;
  private EditText toSend;
  private TextView verbose;
  private int duration = Toast.LENGTH_SHORT;
  private static final UUID BtModuleUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
  private CustomAdapter customAdapter;
  private Set<BluetoothDevice> pairedDevices;
  private ArrayList<SingleRow> pairedDevicesList;
  private BluetoothDevice[] btArray;
  private ClientClass clientClass;
  private SendReceive sendReceive;


    //constants for the handler
  static final int STATE_LISTENING = 1;
  static final int STATE_CONNECTING = 2;
  static final int STATE_CONNECTED = 3;
  static final int STATE_CONNECTION_FAILED = 4;
  static final int STATE_MESSAGE_RECEIVED = 5;


    BroadcastReceiver broadcastReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent){
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                status.append("Found device: " +device.getAddress()+"\n");
            }
        }
    };

    BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)){
                int modeValue = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,BluetoothAdapter.ERROR);

                if(modeValue == BluetoothAdapter.SCAN_MODE_CONNECTABLE){
                    status.append("The device is not in discoverable but can still receive connections. yay \n" );
                }else if(modeValue == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                    status.append("The device is discoverable. yay \n");
                }else if(modeValue == BluetoothAdapter.SCAN_MODE_NONE){
                    status.append("Device ain't discoverable \n");
                }else {
                    status.append("Something screwed up \n");
                }

            }


        }
    };

    public static void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pairedDevicesList = new ArrayList<>();
        toSend = (EditText) findViewById(R.id.editText);
        send = (Button) findViewById(R.id.button);
        verbose = (TextView) findViewById(R.id.Verbose);
        status = (TextView) findViewById(R.id.Status);
        status.setMovementMethod(new ScrollingMovementMethod());
//        verbose.setMovementMethod(new ScrollingMovementMethod());


        if(bluetoothAdapter == null){
            Toast.makeText(getApplicationContext(),"Bluetooth adapter unavailable.",duration).show();
            finish();
        }



        if (!bluetoothAdapter.isEnabled()) {
                Intent enableBt = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBt, REQUEST_ENABLE_BT);

        }else{

            status.append("Bluetooth already enabled \n");

        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,

                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH,
                            Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_LOCATION);
        }

        IntentFilter intentFiler = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        IntentFilter scanIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(broadcastReceiver,intentFiler);
        registerReceiver(scanModeReceiver,scanIntentFilter);

    }


    public void sendData(View v){
        hideKeyboardFrom(getApplicationContext(),getCurrentFocus());
        String string = String.valueOf(toSend.getText());
        sendReceive.write(string.getBytes());
        verbose.append(string + "\n");
        toSend.setText("");


    }


    public void listPairedDevices(View v){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View row = inflater.inflate(R.layout.row_item,null);
        ListView li = (ListView)row.findViewById(R.id.listView);
        li.setAdapter(customAdapter);


        builder.setView(row);
        final AlertDialog dialog = builder.create();
        dialog.show();


        li.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clientClass = new ClientClass(btArray[position]);
                clientClass.start();
                status.append("Connecting to "+btArray[position]+"\n");
                dialog.dismiss();
            }
        });

    }


    Handler handler = new Handler(new Handler.Callback(){
        @Override
        public boolean handleMessage(Message msg) {

            switch(msg.what){

                case STATE_LISTENING:

                    status.append("Listening \n");
                    break;

                case STATE_CONNECTING:
                    status.append("Connecting \n");
                    break;

                case STATE_CONNECTED:
                    status.append("Connected \n");
                    break;

                case STATE_CONNECTION_FAILED:
                    status.append("Connection failed  :( \n");
                    break;

                case STATE_MESSAGE_RECEIVED:
                    byte[] readBuffer = (byte[]) msg.obj;
                    String tempMsg = new String(readBuffer,0,msg.arg1);
                    verbose.append(tempMsg + "\n");

                    break;

            }


            return false;
        }
    });


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){

            case R.id.connect:
                pairedDevices = bluetoothAdapter.getBondedDevices();
                pairedDevicesList = new ArrayList<>(pairedDevices.size());
                String[] strings = new String[pairedDevices.size()];
                btArray = new BluetoothDevice[pairedDevices.size()];
                int index = 0;

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {

                        btArray[index] = device;
                        String deviceName = device.getName();
                        strings[index] = deviceName;
                        pairedDevicesList.add(new SingleRow(deviceName,device.getAddress()));
                        index++;

                    }
                }

                customAdapter = new CustomAdapter(this,pairedDevicesList);

                listPairedDevices(findViewById(android.R.id.content));


                return true;

            case R.id.makediscoverable:
                Intent makeDiscoverable = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                makeDiscoverable.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,10);
                startActivity(makeDiscoverable);

                return true;


            case R.id.discover:

                if(bluetoothAdapter.isDiscovering()){
                    status.append("Already discovering. Restarting... \n");
                    bluetoothAdapter.cancelDiscovery();
                }

                status.append("Discovery initiated... \n");
                bluetoothAdapter.startDiscovery();

                return true;

            case R.id.canceldiscovery:
                if(bluetoothAdapter.isDiscovering()){
                    bluetoothAdapter.cancelDiscovery();
                    status.append("Discovery cancelled \n");
                }else{
                    status.append("Wasn't discovering \n");
                }

                return true;


            default:
                return super.onOptionsItemSelected(item);

        }


    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode,Intent data) {

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Exiting app because bluetooth not enabled", duration).show();
                finish();
            }

            if (resultCode == RESULT_OK) {
                status.setText("");
                status.append("Bluetooth enabled \n");
            }
        }

    }




    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }

        unregisterReceiver(broadcastReceiver);
        unregisterReceiver(scanModeReceiver);


        //disconnect bluetooth and stuff
    }

    private class ClientClass extends Thread{

        private BluetoothDevice device;
        private BluetoothSocket socket;

        public ClientClass(BluetoothDevice device1){
            this.device = device1;

            try {
                socket = device.createRfcommSocketToServiceRecord(BtModuleUUID);
            }catch (IOException e){
                e.printStackTrace();
            }

        }

        public void run(){

            try {
                socket.connect();
                Message message = Message.obtain();
                message.what = STATE_CONNECTED;
                handler.sendMessage(message);

                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message = Message.obtain();
                message.what = STATE_CONNECTION_FAILED;
                handler.sendMessage(message);
            }
        }




    }

    private class SendReceive extends Thread{

        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public SendReceive(BluetoothSocket socket){
            bluetoothSocket = socket;
            InputStream tempIn = null;
            OutputStream tempOut  = null;

            try {
                tempIn = bluetoothSocket.getInputStream();
                tempOut = bluetoothSocket.getOutputStream();

            } catch (IOException e) {
                e.printStackTrace();
            }

            inputStream = tempIn;
            outputStream = tempOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSAGE_RECEIVED,bytes,-1,buffer).sendToTarget();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        public void write(byte[] bytes){
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }



    }

}






