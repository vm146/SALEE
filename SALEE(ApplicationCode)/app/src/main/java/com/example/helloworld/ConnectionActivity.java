package com.example.helloworld;

import static com.example.helloworld.NotificationApp.CHANNEL_1_ID;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ProcessLifecycleOwner;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.lang.Math;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionActivity extends AppCompatActivity {

    // Strg + alt + L: Auto-Einrücken

    Socket socket;
    Thread Thread1 = null;
    Button msgButton;
    Button autoLabelButton;
    TextView infoMessagesTwo;
    TextView unlabeled;
    String inMessage;

    // LinkedBlockingQueue<HashMap<String, Float>> eventQueue = new LinkedBlockingQueue<>(20);
    public LinkedList<HashMap<String, Float>> eventQueue = new LinkedList<>();
    public LinkedList<String> labelQueue = new LinkedList<>();
    public LinkedList<String> autoLabelQueue = new LinkedList<>();
    // Counter for evaluation of propositions made by app
    // When true pos rate of propositions gets above a certain threshold, label auto
    public Float eventCounter = 0.0f;
    public Integer propOneCounter = 0;
    public Integer propTwoCounter = 0;
    public double truePosPropOne;
    public double truePosPropTwo;
    public boolean autoLabelSwitch = false;

    // boolean value: is app in foreground(active use) or background(passive use)
    // use for thread-safety atomic booleans
    public static AtomicBoolean resume = new AtomicBoolean(true);

    public ArrayList<appliance> appliances = new ArrayList<>();
    public ArrayList<appliance> appliancesAutoLabeled = new ArrayList<>();

    private NotificationManagerCompat notificationManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        // Blue background color
        int niceBlue = Color.parseColor("#427EA8");
        getWindow().getDecorView().setBackgroundColor(niceBlue);

        infoMessagesTwo = findViewById(R.id.infoMessagesTwo);
        unlabeled = findViewById(R.id.unlabeledNot);


        // Check for stored data

        loadList();

        // Check whether we're recreating a previously destroyed instance
        if (savedInstanceState != null) {
            // Restore value of members from saved state
            MainActivity.SERVER_IP = savedInstanceState.getString("IP");
            MainActivity.SERVER_PORT = savedInstanceState.getInt("Port");
        }

        // Thread for TCP Connection
        // NOTE! Mobile data of Smartphone might need to be turned off (if WLAN has no internet)

        Thread1 = new Thread(new Thread1());
        Thread1.start();


        notificationManager = NotificationManagerCompat.from(this);

        msgButton = findViewById(R.id.buttonMsg);
        msgButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Clear notifications
                notificationManager.cancel(1);
                notificationManager.cancel(2);

                HashMap<String, Float> event;
                HashMap<String, Float> eventTwo;

                boolean type;

                eventCounter++;

                // for testing
                /*event = convStr("Up|16.40|2022.04.24 14:12:20", true);
                eventTwo = convStr("Down|-18.40|2022.04.24 10:12:20", false);

                eventQueue.add(event);
                eventQueue.add(eventTwo);*/


                while (eventQueue.size() > 0) {
                    // get power of event
                    event = eventQueue.removeLast();
                    Map.Entry<String, Float> entry = event.entrySet().iterator().next();
                    Float power = entry.getValue();

                    if (power > 0) {
                        type = true;
                    } else {
                        type = false;
                    }
                    // First two added appliances need to be labeled separately (for propositions)
                    if (appliances.size() < 2) {
                        showDialog(event, type);
                        break;
                    }

                    showDialog(event, type);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoMessagesTwo.setText("");
                    }
                });
            }
        });

        autoLabelButton = findViewById(R.id.buttonAuto);
        autoLabelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                autoLabelSwitch = !autoLabelSwitch;
                if (!autoLabelSwitch) {
                    autoLabelButton.setText("Autolabeling: OFF");
                } else {
                    autoLabelButton.setText("Autolabeling: ON");
                }
            }
        });
    }

    // Thread 1 Connection to Server

    // alternative: private PrintWriter output;
    private DataOutputStream output;            // PrintWriter directly converts bytes into string
    private BufferedReader input;               // We need bytes for python server -> DataStream


    class Thread1 implements Runnable {
        public void run() {
            try {
                socket = new Socket(MainActivity.SERVER_IP, MainActivity.SERVER_PORT);
                output = new DataOutputStream(socket.getOutputStream());
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Server only receives msg with "Info:" or "Data:" as prefix and \n as suffix
                output.writeUTF("Info: App\n");   // Identification String for Server
                output.flush();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoMessagesTwo.setText("Connected\n");
                    }
                });
                new Thread(new Thread3()).start();
                new Thread(new Thread2()).start();
            } catch (IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoMessagesTwo.setText("Connection unsuccessful:\nGo back, " +
                                "check your IP address/port and try again\n");
                        unlabeled.setText("Unlabeled events: " + eventQueue.size());
                    }
                });
                e.printStackTrace();
            }
        }
    }

    // Thread 2 listening to Server

    class Thread2 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    inMessage = input.readLine();
                    if (inMessage != null) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                infoMessagesTwo.append(inMessage + "\n");
                                infoMessagesTwo.append(truePosPropOne + "\n");
                                infoMessagesTwo.append(truePosPropTwo + "\n");
                                infoMessagesTwo.append(autoLabelQueue.size() + "\n");
                            }
                        });
                        // TODO Set textview unlabeled counter directly on 0
                        // catch event
                        HashMap<String, Float> event;
                        if (inMessage.contains("Up")) {

                            event = convStr(inMessage, true);

                            if (autoLabelSwitch && truePosPropOne >= 0.8 ||
                                    autoLabelSwitch && truePosPropTwo >= 0.8) {
                                autoLabel(event, true);
                            } else {
                                sendNot("Up event detected", "Which appliance " +
                                        "was turned on?", 1);

                                eventQueue.add(event);
                            }

                            /*if (truePosPropOne < 0.8 && truePosPropTwo < 0.8 && !autoLabelSwitch) {
                                sendNot("Up event detected", "Which appliance " +
                                        "was turned on?", 1);

                                eventQueue.add(event);

                            } else {
                                autoLabel(event, true);
                            }*/
                        } else if (inMessage.contains("Down")) {

                            event = convStr(inMessage, false);

                            if (autoLabelSwitch && truePosPropOne >= 0.8 ||
                                    autoLabelSwitch && truePosPropTwo >= 0.8) {
                                autoLabel(event, true);
                            } else {
                                sendNot("Up event detected", "Which appliance " +
                                        "was turned on?", 1);

                                eventQueue.add(event);
                            }
                            /*if (truePosPropOne < 0.8 && truePosPropTwo < 0.8) {
                                sendNot("Down event detected", "Which appliance " +
                                        "was turned off?", 1);


                                eventQueue.add(event);

                            } else {
                                autoLabel(event, false);
                            }*/
                        }
                        // Connection Error
                        else if (inMessage.contains("DEAD")) {
                            String device = inMessage.substring(0, inMessage.length() - 5);
                            sendNot("Connection broken", "Check for " + device +
                                    "\n Maybe unplug/plug it again?", 2);
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                unlabeled.setText("");
                                unlabeled.append("Unlabeled events: " + eventQueue.size());
                            }
                        });
                    } else {
                        Thread1 = new Thread(new Thread1());
                        Thread1.start();
                        return;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // less CPU Consumption
                SystemClock.sleep(100);
            }
        }
    }

    // Keep Alive Thread - sends keep alive message and labels back to server

    class Thread3 implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    // send appliance label back to server: '#' indicates server it's a label
                    if (labelQueue.size() > 0) {
                        output.writeUTF("Info: #" + labelQueue.remove() + "\n");
                        output.flush();
                    }
                    // same thing for autolabeled events: '##' indicates it's auto labeled
                    if (autoLabelQueue.size() > 0) {
                        output.writeUTF("Info: ##" + autoLabelQueue.remove() + "\n");
                        output.flush();
                    }
                    output.writeUTF("Info: KeepAlive\n");
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                SystemClock.sleep(5000);
            }
        }
    }


    public HashMap<String, Float> convStr(String rString, boolean eventType) {
        /* Converts received String into usable data: Read power consumption and event time of
         * event and put it into HashMap
         *
         *  Args:
         *      rString(String): Received data from server
         *      eventType(boolean): declares if event is Up(true) or Down(false)
         *
         *  Returns:
         *      event(HashMap<String, Float>):
         *      with key
         *          eventTime(String): time the event occurred
         *      and value
         *          difference(Float): difference of power consumption before and after event
         */
        HashMap<String, Float> event = new HashMap<>();
        Float difference;
        String eventTime;
        String cStringDiff;
        String cStringTime;
        if (eventType) {
            // example: "Up|16.4|2022.04.24 14:12:20"
            cStringDiff = rString.substring(3, 7);
            cStringTime = rString.substring(8);
        } else {
            cStringDiff = rString.substring(5, 10);
            cStringTime = rString.substring(11);
        }
        difference = Float.parseFloat(cStringDiff);
        eventTime = cStringTime;

        event.put(eventTime, difference);

        return event;
    }

    // dialog function when event detected

    public void showDialog(HashMap<String, Float> eventLoc, boolean eventType) {
        /* UI to label received event
         * Args:
         *      eventLoc(HashMap<String, Float>): local event with event time(String)
         *                                          and power consumption(Float)
         *      eventType(boolean): declares if event is Up(true) or Down(false)
         *
         * Returns:
         *      None
         * */
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // get timestamp and difference of event
        Map.Entry<String, Float> entry = eventLoc.entrySet().iterator().next();
        String time = entry.getKey();
        Float power = entry.getValue();
        String timeToday = time.substring(11, 16);

        if (eventType) {
            builder.setTitle("Up event of " + power + "Watt at " + timeToday + " detected. " +
                    "Choose appliance:");
        } else {
            builder.setTitle("Down event of " + power + "Watt at " + timeToday + " detected. " +
                    "Choose appliance:");
        }


        // Set up the input
        final EditText inputDialog = new EditText(this);
        inputDialog.setInputType(InputType.TYPE_CLASS_TEXT);
        inputDialog.setHint("new appliance");
        builder.setView(inputDialog);

        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                String newDevice;
                // catch user appliance info
                newDevice = inputDialog.getText().toString();

                // in case user enters nothing
                if (newDevice.equals("")) {
                    return;
                }
                labelQueue.add(newDevice);

                /*runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // print list with appliances and their attributes
                        infoMessagesTwo.setText("");
                        for (int i = 0; i < appliances.size(); i++) {
                            infoMessagesTwo.append("name " + appliances.get(i).getName() + ", ");
                            infoMessagesTwo.append("events " + appliances.get(i).getEvents() + ", ");
                            infoMessagesTwo.append("levels " + appliances.get(i).getLevels() + " ");
                        }

                        infoMessagesTwo.append("\n");
                    }
                });*/

                // add it to ArrayList with rest of event data
                storeInList(appliances, newDevice, eventType, time, power);

                saveList();
            }
        });
        String tempProp = "";
        if (appliances.size() > 0) {
            String prop = propAp(power);
            // remove "(on)" before storing in list (but keep it for UI)
            tempProp = prop;
            if (tempProp.contains("(on)")) {
                tempProp = tempProp.substring(0, tempProp.length() - 4);
            }
            String finalProp = tempProp;
            builder.setNegativeButton(prop, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    propOneCounter++;
                    truePosPropOne = propOneCounter / eventCounter;
                    // infoMessagesTwo.append("truePos rate: " + truePosPropOne + "\n propOne: " +
                    //        propOneCounter + "\n");
                    labelQueue.add(finalProp);
                    storeInList(appliances, finalProp, eventType, time, power);
                    saveList();

                }
            });
        }
        if (appliances.size() > 1) {
            // propose Application that might have been turned on
            String propTwo = propApTwo(power, tempProp);

            // remove "(on)" before storing in list
            String tempPropTwo = propTwo;
            if (tempPropTwo.contains("(on)")) {
                tempPropTwo = tempPropTwo.substring(0, tempPropTwo.length() - 4);
            }
            String finalPropTwo = tempPropTwo;

            builder.setNeutralButton(propTwo, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    propTwoCounter++;
                    truePosPropTwo = propTwoCounter / eventCounter;
                    // infoMessagesTwo.append("truePos2 rate: " + truePosPropTwo + "\n propTwo: " +
                    //        propTwoCounter + "\n eventcounter: " + eventCounter + "\n");
                    labelQueue.add(finalPropTwo);
                    storeInList(appliances, finalPropTwo, eventType, time, power);
                    saveList();
                }
            });
        }

        builder.show();
    }

    public void storeInList(ArrayList<appliance> apList, String ap, boolean et, String t,
                            Float p) {
        /* store (new) appliance and event into ArrayList<appliance>
         *      Args:
         *          apList(ArrayList<appliance>): list in which appliance and data will be stored.
         *              Note(!) auto-labeled events get stored in a different ArrayList than
         *              manual labeled events
         *          ap(String): string of appliance name to be stored
         *          et(Boolean): eventType - Up(true) or down(false)
         *          t(String): timestamp of event
         *          p(Float): power consumption of event
         *      Returns:
         *          None
         */
        appliance app;
        for (int i = 0; i < apList.size(); i++) {
            String name = apList.get(i).getName();
            if (ap.equals(name)) {
                apList.get(i).events.put(t, p);
                // set counter for appliance
                if (et) {
                    apList.get(i).raiseCounter();
                    apList.get(i).levels.put(apList.get(i).getCounter(),
                            Math.round(p));
                } else {
                    apList.get(i).lowerCounter();
                }
                return;
            }
        }
        // in case new appliance was added, initialize
        HashMap<Integer, Integer> initLevel = new HashMap<>();
        HashMap<String, Float> initEvent = new HashMap<>();
        if (et) {
            initLevel.put(1, Math.round(p));
            initEvent.put(t, p);
            app = new appliance(ap, initEvent, initLevel, 1);
        } else {
            // Note(!) this case shouldn't happen.
            // When starting the app make sure no appliance is already switched on
            app = new appliance(ap, initEvent, initLevel, -1);
        }
        // level 0: appliance is turned off
        app.getLevels().put(0, 0);
        apList.add(app);
    }

    public boolean almostEqual(double powerEv, double powerAp, Integer limit) {
        /* Checks if given doubles have almost equal value
         *      Args:
         *          powerEv(double): power measured on event
         *          powerAp(double): power of appliance
         *          limit(Integer): limit of difference to be considered as "equal"
         *      Returns:
         *          boolean: true if inputs are, depending on limit, almost equal else false
         */
        return Math.abs(powerEv - powerAp) < limit;
    }

    public String propAp(float pow) {
        /* Proposes an appliance that might have been turned on/off depending on event
         * Args:
         *      pow(float): power of event
         * Returns:
         *      A string with the name of the appliance that might have been turned on/off
         *
         */
        int limit = 5;
        while (true) {
            for (int i = 0; i < appliances.size(); i++) {
                HashMap<Integer, Integer> tempHM = appliances.get(i).getLevels();
                for (Integer tempPow : tempHM.values()) {
                    if (almostEqual(tempPow, pow, limit)) {
                        // propose closest equal consumption
                        if (appliances.get(i).getCounter() != 0) {
                            return appliances.get(i).getName() + "(on)";
                        }
                        return appliances.get(i).getName();
                    }

                }
            }
            limit += 5;
        }
    }

    public String propApTwo(float pow, String propB) {
        /* Proposes an appliance that might have been turned on/off by comparing it with the power
         *  consumption of other appliances
         * Args:
         *      pow(float): power of event
         *      propB(String): proposition made by another function
         * Returns:
         *      A string with the name of the appliance that might have been turned on/off
         *
         */

        // almost the same as propAp, only it iterates through events and has smaller limit
        int limit = 3;
        while (true) {
            for (int i = 0; i < appliances.size(); i++) {
                HashMap<String, Float> tempHM = appliances.get(i).getEvents();
                for (Float tempPow : tempHM.values()) {
                    if (almostEqual(tempPow, pow, limit)) {
                        String prop = appliances.get(i).getName();
                        // if propositions are the same, propose last added to list
                        int j = 1;
                        while ((prop.equals(propB) || prop.equals(propB + "(on)")) && j < 3) {
                            if (appliances.get(appliances.size() - j).getCounter() != 0) {
                                prop = appliances.get(appliances.size() - j).getName() + "(on)";
                            } else {
                                prop = appliances.get(appliances.size() - j).getName();
                            }
                            j++;
                        }
                        // add "(on)" if counter of appliance != 0 (i.e. already switched on)
                        if (appliances.get(i).getCounter() != 0 && !prop.contains("(on)")) {
                            return prop + "(on)";
                        }
                        return prop;
                    }

                }
            }
            limit += 3;
        }
    }

    public void autoLabel(HashMap<String, Float> eventLoc, boolean eventType) {
        /* Labels event automatically without asking the user
         *  Args:
         *      eventLoc(HashMap<String, Float>): local event with event time and power consumption
         *      eventType(boolean): declares if event is Up(true) or Down(false)
         *  Returns:
         *      None
         */

        String propA;
        String propB;
        // get timestamp and difference of event
        Map.Entry<String, Float> entry = eventLoc.entrySet().iterator().next();
        String time = entry.getKey();
        Float power = entry.getValue();

        eventCounter++;

        propA = propAp(power);
        propB = truePosPropOne >= 0.8 ? propA : propApTwo(power, propA);

        // remove "(on)" before storing in list
        String tempPropTwo = propB;
        if (tempPropTwo.contains("(on)")) {
            tempPropTwo = tempPropTwo.substring(0, propB.length() - 4);
        }
        String finalPropTwo = tempPropTwo;

        if (truePosPropOne >= 0.8) {
            propOneCounter++;
            truePosPropOne = propOneCounter / eventCounter;
        } else {
            propTwoCounter++;
            truePosPropTwo = propTwoCounter / eventCounter;
        }
        autoLabelQueue.add(finalPropTwo);
        storeInList(appliancesAutoLabeled, finalPropTwo, eventType, time, power);
        saveList();

    }

    public void sendNot(String title, String text, Integer notGroup) {
        /* Sends notification to user with given message
         *      Args:
         *          title(String): String which will be the title of the notification
         *          text(String): String which will be the text of the notification
         *          notGroup(Integer): specifies "category/group" of notification. Notifications of
         *              the same category overwrite themselves when new notification is pushed
         *              Current convention: Group "1" is for up/down events
         *                                  Group "2" is for connection errors
         *
         *      Returns:
         *          None
         */
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        // TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // stackBuilder.addNextIntentWithParentStack(resultIntent);
        // Get the PendingIntent containing the entire back stack
        /*PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);*/

        Intent intent = new Intent(this, ConnectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                ConnectionActivity.this, CHANNEL_1_ID)
                .setSmallIcon(R.drawable.ic_baseline_announcement_24)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_EVENT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(notGroup, notification.build());
    }

    private void saveList() {
        /* Writes ArrayList with appliance entries into a file. Note you can reset all saved data
         *  in the settings of your smartphone app
         *      Args:
         *          yourList(ArrayList<appliance>): list to be stored
         *      Returns:
         *          None
         */

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());

        SharedPreferences.Editor prefsEditor = prefs.edit();

        // save eventCounter&co
        prefsEditor.putFloat("eventCounter", eventCounter).commit();
        prefsEditor.putInt("propOneCounter", propOneCounter).commit();
        prefsEditor.putInt("propTwoCounter", propTwoCounter).commit();

        Gson gson = new Gson();
        String json = gson.toJson(appliances);
        String jsonTwo = gson.toJson(appliancesAutoLabeled);

        prefsEditor.putString("salee.txt", json);
        prefsEditor.putString("saleeTwo.txt", jsonTwo);

        prefsEditor.apply();
    }

    private void loadList() {
        /* Loads ArrayList out of dataFile
         *      Args:
         *          None
         *      Returns:
         *          if something stored: ArrayList<appliance>
         *          else: null object
         */

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext());

        ArrayList<appliance> temp = new ArrayList<>();
        ArrayList<appliance> tempTwo = new ArrayList<>();

        // load eventCounter&co
        eventCounter = prefs.getFloat("eventCounter", 0f); // 0 if var not found
        propOneCounter = prefs.getInt("propOneCounter", 0);
        propTwoCounter = prefs.getInt("propTwoCounter", 0);

        // load arraylists

        Gson gson = new Gson();
        String json = prefs.getString("salee.txt", null);
        String jsonTwo = prefs.getString("saleeTwo.txt", null);


        Type type = new TypeToken<ArrayList<appliance>>() {
        }.getType();

        temp = gson.fromJson(json, type);
        tempTwo = gson.fromJson(jsonTwo, type);

        if (temp != null && tempTwo != null) {
            appliances = temp;
            appliancesAutoLabeled = tempTwo;
        }
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
        savedInstanceState.putString("IP", MainActivity.SERVER_IP);
        savedInstanceState.putInt("Port", MainActivity.SERVER_PORT);

        // Always call the superclass so it can save the view hierarchy state
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        /* Will be called when the activity is destroyed
         */
        super.onDestroy();
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setContentView(R.layout.activity_connection);
    }

    /*public void onBackPressed() {
        /* function that gets called when back-button is pressed

        saveList(appliances);
    }*/
}
// TODO: test

// TODO: Use Case for autolabeling:

// TODO: Server needs to be stopped repeatedly
