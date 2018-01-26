package com.example.ramezelbaroudy.arffnew;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Timer;

public class MainActivity extends AppCompatActivity {

    private ToggleButton startServiceToggle;
    private ListView listOfUsers;
    private TextView roomCondition;
    BroadcastReceiver broadcastReceiver;
    ArrayAdapter<String> itemsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startServiceToggle = (ToggleButton) findViewById(R.id.startServiceToggle);
        listOfUsers = (ListView) findViewById(R.id.listOfUsers);
        roomCondition = (TextView) findViewById(R.id.roomCondition);

        startServiceToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean pressedOn) {
                if (pressedOn) {

                    startService(new Intent(getApplicationContext(), ArffRecorderService.class));

                    // for receiving the users activities and the room condition from the service and showing them on screen
                    broadcastReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            ArrayList<String> usersHistoryDisplayList = intent.getStringArrayListExtra("usersArray");
                            itemsAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_list_item_1, usersHistoryDisplayList);
                            listOfUsers.setAdapter(itemsAdapter);
                            roomCondition.setText(intent.getStringExtra("roomStatus"));
                        }
                    };
                    registerReceiver(broadcastReceiver, new IntentFilter(ArffRecorderService.BROADCAST_ACTION));

                } else {
                    // Stop the service and unregister when button off is pressed
                    stopService(new Intent(getApplicationContext(), ArffRecorderService.class));
                    unregisterReceiver(broadcastReceiver);
                    roomCondition.setText("Service off");
                }
            }
        });
    }
}
