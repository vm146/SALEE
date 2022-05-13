package com.example.helloworld;

import androidx.appcompat.app.AppCompatActivity;

// import android.support.v7.app.AppCombatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    EditText IP, Port;
    TextView instruction;
    TextView infoMessages;
    Button btnConnect;
    public static String SERVER_IP;
    public static int SERVER_PORT;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            // Restore value of members from saved state
            SERVER_IP = savedInstanceState.getString("IP");
            SERVER_PORT = savedInstanceState.getInt("Port");
        }

        // Background color
        int niceBlue = Color.parseColor("#427EA8");
        getWindow().getDecorView().setBackgroundColor(niceBlue);

        Log.d("myTag", "onCreate: " + SERVER_IP);

        IP = findViewById(R.id.IP);
        Port = findViewById(R.id.Port);
        instruction = findViewById(R.id.instruction);
        infoMessages = findViewById(R.id.infoMessages);
        // infoMessages.append("IP: " + SERVER_IP + "\n");
        // infoMessages.append("Port: " + SERVER_PORT + "\n");
        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (IP.getText().toString().trim().isEmpty() ||
                        Port.getText().toString().trim().isEmpty()) {
                    infoMessages.setText("Please enter valid ip or port");
                    return;
                }
                // note Server_IP needs to be a string for socket
                SERVER_IP = IP.getText().toString().trim();
                SERVER_PORT = Integer.parseInt(Port.getText().toString().trim());
                openConnectionActivity();
            }
        });
    }

    public void openConnectionActivity() {
        Intent intent = new Intent(this, ConnectionActivity.class);
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        /*
        onSaveInstanceState always gets called before an activity will be destroyed
        An activity will be destroyed and recreated for example when user turns his screen.
        Note that an activity also gets destroyed and recreated when user gets through notification
        to activity
         */
        // Save the user's current game state
        savedInstanceState.putString("IP", SERVER_IP);
        savedInstanceState.putInt("Port", SERVER_PORT);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }
}