package com.ozcanfatihcan.bluetoothproje;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG="MainActivity";

    //bluetoothu kontrol etmek için kullandığımız değişken
    BluetoothAdapter mBluetoothAdapter;
    //keşfedilebilirliği kontrol etmek için kullandığımız buton
    Button btnEnableDisable_Discoverable;

    BluetoothConnectionService mBluetoothConnection;

    //bağlantıyı başlatmak ve mesajı göndermek için kullanacağımız buton
    Button btnStartConnection;
    Button btnSend;

    TextView incomingMessages;
    StringBuilder messages;


    EditText etSend;

    private static final UUID MY_UUID_INSECURE=UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    BluetoothDevice mBTDevice;

    //keşfettiğimiz cihazların listesini tutacağımız bir arraylist oluşturuyoruz.
    public ArrayList<BluetoothDevice> mBTDevices=new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;

    //kayıt tutucu
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    //kayıt tutucu
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Bulunabilirlik aktif.");
                        break;
                    //Device not in discoverable mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Bağlantı kapatıldı. Bağlantı alınabilir.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Bağlantı kapatıldı. Bağlantı alınamaz.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Bağlanılıyor....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Bağlandı.");
                        break;
                }

            }
        }
    };
    //kayıt tutucu
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action= intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND");

            if(action.equals(BluetoothDevice.ACTION_FOUND)){
                //tarama sonucu bulunan cihazları listviewde gösteriyoruz.
                BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                Log.d(TAG, "onReceive: "+ device.getName()+": "+device.getAddress());
                mDeviceListAdapter=new DeviceListAdapter(context, R.layout.device_adapter_view,mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);

            }
        }
    };
    //kayıt tutucu
    private BroadcastReceiver mBroadcastReceiver4=new BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action= intent.getAction();

            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 CASE OLACAK
                //zaten bağlı
                if(mDevice.getBondState()==BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED ");
                    mBTDevice=mDevice;
                }
                //bağlantı oluşturuluyor
                if(mDevice.getBondState()==BluetoothDevice.BOND_BONDING){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING ");
                }
                //bağı bozma
                if(mDevice.getBondState()==BluetoothDevice.BOND_NONE){
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE ");
                }
            }

        }
    };
    //kayıt tutucuların uygulamayı kapadıktan sonra temizlenmesi için kullanılan kısım
    @Override
    protected void onDestroy() {
        Log.d(TAG,"onClick: enabling/disabling bluetooth.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }
    //izinlerin ilk etaptaki default tanımladığımız değeri
    ActivityResultLauncher<String[]> mPermmissionResultLauncher;
    private boolean BTPermmissionGranted=false;
    private boolean BTScanPermmissionGranted=false;
    private boolean BTConnectPermmissionGranted=false;
    private boolean BTAdmınPermmissionGranted=false;
    private boolean BTAdvertısePermmissionGranted=false;
    private boolean BTAFLPermmissionGranted=false;
    private boolean BTACLPermmissionGranted=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //xml kısmından belirlediğimiz id ile buradaki belirlenen değişkenleri eşitledik.
        Button btnONOFF=(Button) findViewById(R.id.btnONOFF);
        btnEnableDisable_Discoverable=(Button) findViewById(R.id.btnDiscoverable_on_off);
        lvNewDevices=(ListView) findViewById(R.id.lvNewDevices);
        mBTDevices=new ArrayList<>();

        btnStartConnection=(Button) findViewById(R.id.btnStartConnection);
        btnSend=(Button) findViewById(R.id.btnSend);
        etSend=(EditText) findViewById(R.id.editText);

        incomingMessages=(TextView) findViewById(R.id.incomingMessage);
        messages=new StringBuilder();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver,new IntentFilter("incomingMessage"));

        //bulunan cihazları yayınla
        IntentFilter filter=new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4,filter);

        mBluetoothAdapter= BluetoothAdapter.getDefaultAdapter();

        lvNewDevices.setOnItemClickListener(MainActivity.this);


        //açma kapama butonu işlevi
        btnONOFF.setOnClickListener((view) -> {
            Log.d(TAG, "onClick: enabling/disabling bluetooth ");
            enableDisableBT();
        });
        //bağlantı butonu işlevi
        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startConnection();
            }
        });
        //gönderme butonu işlevi
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes=etSend.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);

                etSend.setText("");
            }
        });


        //çekilen izinlerin manifestten çekilmesi
        mPermmissionResultLauncher=registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if(result.get(Manifest.permission.BLUETOOTH)!=null){
                    BTPermmissionGranted=result.get(Manifest.permission.BLUETOOTH);
                }
                if(result.get(Manifest.permission.BLUETOOTH_SCAN)!=null){
                    BTScanPermmissionGranted=result.get(Manifest.permission.BLUETOOTH_SCAN);
                }
                if(result.get(Manifest.permission.BLUETOOTH_CONNECT)!=null){
                    BTConnectPermmissionGranted=result.get(Manifest.permission.BLUETOOTH_CONNECT);
                }
                if(result.get(Manifest.permission.BLUETOOTH_ADMIN)!=null){
                    BTAdmınPermmissionGranted=result.get(Manifest.permission.BLUETOOTH_ADMIN);
                }
                if(result.get(Manifest.permission.BLUETOOTH_ADVERTISE)!=null){
                    BTAdvertısePermmissionGranted=result.get(Manifest.permission.BLUETOOTH_ADVERTISE);
                }
                if(result.get(Manifest.permission.ACCESS_COARSE_LOCATION)!=null){
                    BTACLPermmissionGranted=result.get(Manifest.permission.ACCESS_COARSE_LOCATION);
                }
                if(result.get(Manifest.permission.ACCESS_FINE_LOCATION)!=null){
                    BTAFLPermmissionGranted=result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });
        //zorunlu izin
        requestPermission();
    }
    //kayıt tutucu
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text=intent.getStringExtra("theMessage");
            messages.append(text+"\n");

            incomingMessages.setText(messages);
        }
    };


    //chati kontrol edecek servis methodu
    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    //eşelşem sonrası mesajlaşma için kullanılan bağlantı butonu
    public void startBTConnection(BluetoothDevice device,UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth connection.");
        mBluetoothConnection.startClient(device,uuid);

    }

    //aç kapa butonumuz için kullandığımız metod
    @SuppressLint("MissingPermission")
    public void enableDisableBT(){
        if(mBluetoothAdapter==null){
            Log.d(TAG,"enableDisableBT: Bluetooth bulunamadı.");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG,"enableDisableBT: Bluetooth açılıyor.");
            Intent enableBTIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent=new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1,BTIntent);
        }
        if(mBluetoothAdapter.isEnabled()){
            Log.d(TAG,"enableDisableBT: Bluetooth kapatıldı.");
            mBluetoothAdapter.disable();

            IntentFilter BTIntent=new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1,BTIntent);
        }

    }
    //zorunlu izin alacağımız kısımlar
    private void requestPermission(){
        BTPermmissionGranted= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)== PackageManager.PERMISSION_GRANTED;
        BTScanPermmissionGranted= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)== PackageManager.PERMISSION_GRANTED;
        BTConnectPermmissionGranted= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)== PackageManager.PERMISSION_GRANTED;
        BTAdmınPermmissionGranted= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN)== PackageManager.PERMISSION_GRANTED;
        BTAdvertısePermmissionGranted= ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)== PackageManager.PERMISSION_GRANTED;
        BTACLPermmissionGranted= ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED;
        BTAFLPermmissionGranted= ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED;
        List<String> permissionRequest=new ArrayList<String>();
        if(!BTPermmissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH);
        }
        if(!BTScanPermmissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if(!BTConnectPermmissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if(!BTAdmınPermmissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
        }
        if(!BTAdvertısePermmissionGranted){
            permissionRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        }
        if(!BTACLPermmissionGranted){
            permissionRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if(!BTAFLPermmissionGranted){
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(!permissionRequest.isEmpty()){
            mPermmissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }
    }
    //keşfedilebilirlik kontrolü
    @SuppressLint("MissingPermission")
    public void btnEnableDisable_Discoverable(View view) {
        Log.d(TAG, "btnEnableDisable_Discoverable: Cihazınız 300 saniye boyunca keşfedilebilir durumda olacak.");

        Intent discoverableIntent=new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,300);
        startActivity(discoverableIntent);

        IntentFilter intentFilter=new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2,intentFilter);

    }
    //tarama kontrolü
    @SuppressLint("MissingPermission")
    public void btnDiscover(View view) {
        Log.d(TAG, "btnDiscover: Eşleşecek cihazlar aranıyor.");
        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Tarama durduruluyor.");

            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent=new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3,discoverDevicesIntent);
        }
        if(!mBluetoothAdapter.isDiscovering()){
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent=new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3,discoverDevicesIntent);
        }
    }
    //üst sürümlerde çalışması için gerekli kod
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    //listviewe gelen cihazları tıklama kontrolü
    @SuppressLint("MissingPermission")
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mBluetoothAdapter.cancelDiscovery();

        Log.d(TAG, "onItemClick: Tıklama algılandı.");
        String deviceName=mBTDevices.get(i).getName();
        String deviceAddress=mBTDevices.get(i).getAddress();

        Log.d(TAG, "onItemClick: deviceName="+deviceName);
        Log.d(TAG, "onItemClick: deviceAddress="+deviceAddress);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            Log.d(TAG, "Trying to pair with " + deviceName);
            mBTDevices.get(i).createBond();

            mBTDevice=mBTDevices.get(i);
            mBluetoothConnection=new BluetoothConnectionService(MainActivity.this);
        }
    }
}