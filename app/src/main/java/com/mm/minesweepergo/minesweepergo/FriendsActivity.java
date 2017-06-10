package com.mm.minesweepergo.minesweepergo;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mm.minesweepergo.minesweepergo.DomainModel.User;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class FriendsActivity extends AppCompatActivity {
    ArrayList<User> users;
    ListView lvPaired, lvShow;
    ArrayList<BluetoothDevice> arrayListPairedBluetoothDevices;
    ArrayList<BluetoothDevice> arrayListAllBluetoothDevices;
    ArrayList<String> listPairedDevices;
    ArrayList<String> listAllDevices;
    BluetoothAdapter bluetoothAdapter;
    ArrayAdapter<String> adapterPaired;
    ArrayAdapter<String> adapterAll;
    private ProgressDialog mProgressDlg;
    private BluetoothDevice bdDevice;
    private BluetoothDevice bdDeviceFriend;
    TextView txtFriends, txtSearch;
    int index = -1;
    private String userName;
    private Handler guiThread;
    List<User> friends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.friendsToolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        int permissionCheck = 0;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                permissionCheck);

        guiThread = new Handler();

        Bundle bnd = getIntent().getExtras();
        if (bnd != null)
            userName = bnd.getString("UserName");

        listPairedDevices = new ArrayList<String>();
        listAllDevices = new ArrayList<String>();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        arrayListPairedBluetoothDevices = new ArrayList<BluetoothDevice>();
        arrayListAllBluetoothDevices = new ArrayList<BluetoothDevice>();

        adapterPaired = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listPairedDevices);
        adapterAll = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, listAllDevices);

        this.Init();
        this.getPairedDevices();
        this.CreateProgressDialog();
    }


    private void MakeFriends(BluetoothDevice bdDevice) {
        final String address = bdDevice.getAddress();

        //Log.e("BT_DEVICE2", address);
        ExecutorService transThread = Executors.newSingleThreadExecutor();
        transThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    HTTP.startOrEndFriendship(userName,address,true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }

    private void EndFriendship(final String bdDevice) {
        ExecutorService transThread = Executors.newSingleThreadExecutor();

        transThread.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    //String dodato = MyPlacesHTTPHelper.FriendUnRegister(userName, bdDevice);
                    for (User u : users)
                        if (u.btDevice.equals(bdDevice))
                            users.remove(u);
                    HTTP.startOrEndFriendship(userName,"",false);

                    guiNotifyUser(users);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

    }


    private void Init() {
        txtFriends = (TextView) findViewById(R.id.txtFriendsList);
        txtSearch = (TextView) findViewById(R.id.txtSearchList);
        lvPaired = (ListView) findViewById(R.id.listViewParied);
        lvPaired.setAdapter(adapterPaired);
        lvPaired.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                Log.e("tag", (String) lvPaired.getItemAtPosition(i));
                String device = (String) lvPaired.getItemAtPosition(i);
                final String[] nameDevice = device.split(" ");
                AlertDialog.Builder dialog = new AlertDialog.Builder(FriendsActivity.this);
                dialog.setTitle("Unfriend");
                dialog.setMessage("Do you really want to unfriend with " + nameDevice[0] + "...?");
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        EndFriendship(nameDevice[1]);
                        adapterPaired.notifyDataSetChanged();
                        MakeToast("Friendship ended!");

                        dialogInterface.dismiss();
                    }
                });

                dialog.show();
            }
        });

        lvShow = (ListView) findViewById(R.id.listViewAlldevices);
        lvShow.setAdapter(adapterAll);
        lvShow.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                bdDevice = arrayListAllBluetoothDevices.get(i);
                MakeToast(bdDevice.getName());
                index = i;

                AlertDialog.Builder dialog = new AlertDialog.Builder(FriendsActivity.this);
                dialog.setTitle("Make Friend");
                dialog.setMessage("Do you realy want to become friend with " + bdDevice.getName() + "...?");
                dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                dialog.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            Boolean isBonded = false;

                            for (User u : users) {
                                if (u.btDevice.equals(bdDevice.getAddress()))
                                    isBonded = true;
                            }
                            if (!isBonded) {
                                MakeToast("Trying to create friendship...");
                                createBond(bdDevice);
                                getPairedDevices();
                            } else
                                MakeToast("You are already friends!");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        dialogInterface.dismiss();
                    }
                });
                dialog.show();


            }

        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.menu_friends, menu);
//        menu.add(0, 1, 1, "Enable bluetooth");
//        menu.add
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.item_enable_bt) {
            this.EnableBluetooth();
        } else if (id == R.id.item_discoverable) {
            this.MakeDiscoverable();
        } else if (id == R.id.item_disableBT) {
            this.DisableBluetooth();
        } else if (id == R.id.item_friends) {
            this.getPairedDevices();

            txtSearch.setVisibility(View.INVISIBLE);
            lvShow.setVisibility(View.INVISIBLE);
            txtFriends.setVisibility(View.VISIBLE);
            lvPaired.setVisibility(View.VISIBLE);
        } else if (id == R.id.item_friends_search) {
            txtFriends.setVisibility(View.INVISIBLE);
            lvPaired.setVisibility(View.INVISIBLE);
            txtSearch.setVisibility(View.VISIBLE);
            lvShow.setVisibility(View.VISIBLE);

            this.SearchNewDevices();

        }else if(id==android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }






    private void EnableBluetooth()
    {
        if(!bluetoothAdapter.isEnabled())
        {
            final AlertDialog.Builder dialog=new AlertDialog.Builder(this);
            dialog.setTitle("Enable Bluetooth");
            dialog.setMessage("Do you want to enable bluetooth...?");

            dialog.setNegativeButton("NO",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    dialogInterface.dismiss();
                }
            });
            dialog.setPositiveButton("YES",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    bluetoothAdapter.enable();
                    dialogInterface.dismiss();
                    MakeToast("Bluetooth enabled!");

                }
            });
            dialog.show();
        }
        else
            MakeToast("Bluetooth enabled!");

    }
    private  void MakeDiscoverable()
    {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

    }
    private  void DisableBluetooth()
    {
        if(bluetoothAdapter.isEnabled())
        {
            final AlertDialog.Builder dialog=new AlertDialog.Builder(this);
            dialog.setTitle("Disable Bluetooth");
            dialog.setMessage("Do you want to disable bluetooth...?");

            dialog.setNegativeButton("NO",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    dialogInterface.dismiss();
                }
            });
            dialog.setPositiveButton("YES",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i)
                {
                    bluetoothAdapter.disable();
                    dialogInterface.dismiss();
                    MakeToast("Bluetooth disabled");

                }
            });
            dialog.show();
        }
        else
            MakeToast("Bluetooth disabled");
    }
    private  void CreateProgressDialog()
    {
        mProgressDlg = new ProgressDialog(this);

        mProgressDlg.setMessage("Scanning...");
        mProgressDlg.setCancelable(false);
        mProgressDlg.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                bluetoothAdapter.cancelDiscovery();
            }
        });
    }
    private void MakeToast(String message)
    {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void SearchNewDevices()
    {
        Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();

        for(BluetoothDevice bd:pairedDevice)
            try
            {
                removeBond(bd);
            } catch (Exception e)
            {
                e.printStackTrace();
            }

        if(bluetoothAdapter.isEnabled())
        {
            IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
            FriendsActivity.this.registerReceiver(myReceiver, intentFilter);
            listAllDevices.clear();
            arrayListAllBluetoothDevices.clear();
            adapterAll.notifyDataSetChanged();
            bluetoothAdapter.startDiscovery();
        }
        else
            MakeToast("Need to enable bluetooth first!");
    }

    public boolean isOnline()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
    private void getPairedDevices()
    {

        if(isOnline())
        {
            listPairedDevices.clear();
            adapterPaired.notifyDataSetChanged();
            arrayListPairedBluetoothDevices.clear();
            ExecutorService transThread = Executors.newSingleThreadExecutor();
            transThread.submit(new Runnable() {
                @Override
                public void run() {
                    try {

                        friends = HTTP.getAllFriends(userName);
                        guiNotifyUser(users);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            });
        }
        else
            Toast.makeText(FriendsActivity.this,"Please enable internet connection!",Toast.LENGTH_SHORT).show();


    }
    private void guiNotifyUser(final ArrayList<User> usersP)
    {
        if (usersP!=null)
        {
            guiThread.post(new Runnable()
            {
                @Override
                public void run()
                {
                    listPairedDevices.clear();
                    adapterPaired.notifyDataSetChanged();

                        for (User u : usersP)
                        {
                            listPairedDevices.add(u.username + " " + u.btDevice);

                        }


                    adapterPaired.notifyDataSetChanged();

                }
            });
        }


    }
    public boolean removeBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class btClass = Class.forName("android.bluetooth.BluetoothDevice");
        Method removeBondMethod = btClass.getMethod("removeBond");
        Boolean returnValue = (Boolean) removeBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }


    public boolean createBond(BluetoothDevice btDevice)
            throws Exception
    {
        Class class1 = Class.forName("android.bluetooth.BluetoothDevice");
        Method createBondMethod = class1.getMethod("createBond");
        Boolean returnValue = (Boolean) createBondMethod.invoke(btDevice);
        return returnValue.booleanValue();
    }


    private BroadcastReceiver myReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {

            String action = intent.getAction();


            if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action))
                mProgressDlg.show();

            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if (arrayListAllBluetoothDevices.size() < 1)
                {
                    Toast.makeText(context, "Found new Device", Toast.LENGTH_SHORT).show();
                    adapterAll.add(device.getName() + "\n" + device.getAddress());
                    arrayListAllBluetoothDevices.add(device);
                    adapterAll.notifyDataSetChanged();
                }
                else {
                    boolean flag = true;

                    for (int i = 0; i < arrayListAllBluetoothDevices.size(); i++) {
                        if (device.getAddress().equals(arrayListAllBluetoothDevices.get(i).getAddress())) {
                            flag = false;
                        }
                    }
                    if (flag) {
                        Toast.makeText(context, "Found new Device", Toast.LENGTH_SHORT).show();
                        adapterAll.add(device.getName() + "\n" + device.getAddress());
                        arrayListAllBluetoothDevices.add(device);
                        adapterAll.notifyDataSetChanged();
                    }
                }
            }


            else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
                mProgressDlg.dismiss();

            else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action))
            {
                final int state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                final int prevState	= intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);

                if (state == BluetoothDevice.BOND_BONDED && prevState == BluetoothDevice.BOND_BONDING) {

                    adapterPaired.notifyDataSetChanged();

                    MakeFriends(bdDevice);
                    MakeToast("You are friend now!");
                }


            }

        }
    };
}
