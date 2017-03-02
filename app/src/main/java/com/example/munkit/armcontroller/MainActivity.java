package com.example.munkit.armcontroller;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import static android.R.color.holo_green_light;
import static android.R.color.holo_orange_light;
import static android.R.color.holo_red_light;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private MqttAndroidClient mqttAndroidClient;
    private final String serverUri = "tcp://iot.eclipse.org:1883";
    private final String clientId = "myAndClient";
    private static final String TAG = "Mymessage";
    private final String pubchannel = "RoboticArm/message";


    private boolean terminate_tran = true;
    //true mean release
    private boolean clampstat = true;

    //sensor variable
    private float[] gravity = {0,0,-9.81f};
    private float[] linear_acceleration = new float[3];
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float[] lvelocity = {0,0,0};

    private int[] counter = {0,0,0,0,0,0};



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Log.i(TAG, "oncreate");

        mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {

                TextView respondtext = (TextView) findViewById(R.id.respondtext);
                if (reconnect) {
                    respondtext.setText("Reconnected to : " + serverURI);
                    respondtext.setTextColor(getResources().getColor(holo_orange_light));
                    //Log.i(TAG,"Reconnected to : " + serverURI);
                    // Because Clean Session is true, we need to re-subscribe
                    //subscribeToTopic(subchannel);
                } else {
                    respondtext.setText("Connected to: " + serverURI);
                    respondtext.setTextColor(getResources().getColor(holo_green_light));
                    //Log.i(TAG,"Connected to: " + serverURI);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                TextView respondtext = (TextView) findViewById(R.id.respondtext);
                respondtext.setText("The Connection was lost.");
                respondtext.setTextColor(getResources().getColor(holo_red_light));
                //Log.i(TAG,"The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                //Log.i(TAG,"Incoming message: " + new String(message.getPayload()));
                //set transition
                //transition = true;
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });

        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    //subscribeToTopic(subchannel);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    //Log.i(TAG,"Failed to connect to: " + serverUri);
                    //respondtext.setText("Failed to connect to: " + serverUri);
                    //respondtext.setTextColor(getResources().getColor(holo_red_light));
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
        //accelerometer command
        //lasttime = System.currentTimeMillis();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        //mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        //button
        final Button button1 = (Button)findViewById(R.id.connectbutton);

        button1.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        //click to stop publish message
                        terminate_tran = !terminate_tran;
                        if (terminate_tran) {
                            button1.setText("Start");
                        }
                        else
                        {
                            button1.setText("stop");
                        }

                    }
                });
        final Button clampButton = (Button)findViewById(R.id.clampbutton);
        clampButton.setOnClickListener(
                new Button.OnClickListener(){
                    public void onClick(View v){
                        //click to stop publish message
                        clampstat = !clampstat;
                        if (!terminate_tran) {
                            if (clampstat) {
                                clampButton.setText("Hold");
                                publishMessage(pubchannel, "release");
                            } else {
                                clampButton.setText("release");
                                publishMessage(pubchannel, "Hold");
                            }
                        }
                    }
                });
    }

    public void subscribeToTopic(String subscriptionTopic){
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.i(TAG,"Subscribed!");

                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.i(TAG,"Failed to subscribe");
                }
            });

        } catch (MqttException ex){
            Log.i(TAG,"Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void publishMessage(String Channel, String pubmessage){

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(pubmessage.getBytes());
            mqttAndroidClient.publish(Channel, message);
            if(!mqttAndroidClient.isConnected()){
                Log.i(TAG,mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            Log.i(TAG,"Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }
    //accelerometer event
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event)
    {
        // alpha is calculated as t / (t + dT)
        // with t, the low-pass filter's time-constant
        // and dT, the event delivery rate
        final float alpha = 0.8f;
        final float threshold = 1.0f;
        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            if (!terminate_tran) {

                float[] velocity = new float[3];
                velocity[0] = lvelocity[0] - event.values[0];
                velocity[1] = lvelocity[1] - event.values[1];
                velocity[2] = lvelocity[2] - event.values[2];
                lvelocity[0] = event.values[0];
                lvelocity[1] = event.values[1];
                lvelocity[2] = event.values[2];
                //Log.i(TAG, "acc");
                if (velocity[0]>1.0)
                {
                    counter[0]++;
                    if (counter[0]> 3) {
                        Log.i(TAG, "right");
                        publishMessage(pubchannel,"right");
                    }
                    resetcounter(0);
                }
                else if (velocity[0]<-1.0)
                {
                    counter[1]++;
                    if (counter[1]> 3) {
                        Log.i(TAG, "left");
                        publishMessage(pubchannel,"left");
                    }
                    resetcounter(1);
                }
                if (velocity[1]>0.8)
                {
                    counter[2]++;
                    if (counter[2]> 2) {
                        Log.i(TAG, "front");
                        publishMessage(pubchannel,"front");
                    }
                    resetcounter(2);
                }
                else if (velocity[1]<-0.8)
                {
                    counter[3]++;
                    if (counter[3]> 2) {
                        Log.i(TAG, "back");
                        publishMessage(pubchannel,"back");
                    }
                    resetcounter(3);
                }
                if (velocity[2]>1.0)
                {
                    counter[4]++;
                    if (counter[4]> 3) {
                        Log.i(TAG, "up");
                        publishMessage(pubchannel,"up");
                    }
                    resetcounter(4);
                }
                else if (velocity[2]<-1.0)
                {
                    counter[5]++;
                    if (counter[5]> 3) {
                        Log.i(TAG, "down");
                        publishMessage(pubchannel,"down");
                    }
                    resetcounter(5);
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        Log.i(TAG, "onResume");
    }


    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        Log.i(TAG, "onPause");
    }

    public void resetcounter(int exp){
        for (int i = 0;i<counter.length;i++)
        {
            if(i == exp)
                continue;
            counter[i]= 0;
        }
    }
}
