package com.example.ramezelbaroudy.arffnew;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.ArrayAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import at.jku.pervasive.sedd.actclient.ClassLabel;
import at.jku.pervasive.sedd.actclient.CoordinatorClient;
import weka.classifiers.Classifier;
import weka.classifiers.lazy.IBk;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Created by ramezelbaroudy on 1/22/18.
 */

public class ArffRecorderService extends Service implements SensorEventListener {

    private NotificationManager mNM;
    private int NOTIFICATION = R.string.local_service_started;
    private Sensor mySensor;
    private SensorManager SM;
    private SensorDataProcessing sensorDataProcessing;
    CoordinatorClient connection;
    ArrayList<OtherUser> usersHistory;
    Users users;
    Intent intent;
    public static final String BROADCAST_ACTION = "displayevent";


    // Reading arff file and creating the instances for trainning
    public Instances readInstance() {
        File root = Environment.getExternalStorageDirectory();
        File infile = new File(root, "all2.arff");
        BufferedReader reader = null;
        Instances trainingData = null;

        try {
            reader = new BufferedReader(new FileReader(infile));
            trainingData = new Instances(reader);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (trainingData.classIndex() == -1)
            trainingData.setClassIndex(trainingData.numAttributes() - 1);
        return trainingData;

    }

    // trainning the classifer which is k-nearst neighbour with k = 3
    public Classifier trainningClassifier() {
        Instances trainingData = readInstance();
        Classifier trainedClassifier = new IBk(3);

        try {
            trainedClassifier.buildClassifier(trainingData);
        } catch (
                Exception e)

        {
            e.printStackTrace();
        }

        return trainedClassifier;
    }

    public void repeatFeaturePrediction(final Classifier trainedClassifer) {
        Timer repeatPrediction = new Timer();
        repeatPrediction.schedule(new TimerTask() {
            @Override
            public void run() {
                Pair newFeaturesCalculated = sensorDataProcessing.abs_varAndMeanZ();

                Instance di = new Instance(3); // you have the point classify it and system.out the class
                di.setValue(0, newFeaturesCalculated.firstItem);
                di.setValue(1, newFeaturesCalculated.secondItem);
                Instances trainingData = readInstance();
                di.setDataset(trainingData);

                double clLabel = 1;
                try {
                    clLabel = trainedClassifer.classifyInstance(di);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                di.setClassValue(clLabel);
                String label = di.classAttribute().value((int) clLabel);
                System.out.println("The label is " + label);
                connection.setCurrentActivity(ClassLabel.parse(label));
                intent = new Intent(BROADCAST_ACTION);
                intent.putExtra("usersArray", users.usersHistoryDisplayList);
                intent.putExtra("roomStatus", users.theRoomCondition);
                sendBroadcast(intent);
                System.out.println("still printing");
            }
        }, 5000, 3000);
    }

    public void onCreate() {
        // instantiate connection with server
        connection = new CoordinatorClient("11777167");
        SM = (SensorManager) getSystemService(SENSOR_SERVICE);
        mySensor = SM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorDataProcessing = new SensorDataProcessing();
        mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final Classifier trainedClassifer = trainningClassifier();
        usersHistory = new ArrayList<OtherUser>();

        // create listner to listen for activity change in all users and to continuosly predict and update users and room status
        users = new Users(connection);
        users.usersListner();
        users.repeatEvaluation();

        repeatFeaturePrediction(trainedClassifer);

        // Display a notification about us starting to record.
        showNotification();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        SM.registerListener(this, mySensor, SensorManager.SENSOR_DELAY_NORMAL);
        return START_NOT_STICKY;
    }

    // showing record notification when button is pressed
    private void showNotification() {
        CharSequence text = "Recording";
        PendingIntent content = PendingIntent.getActivity(this, 0,
                new Intent(this, ArffRecorderService.class), 0);


        NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
        nb.setContentIntent(content).setTicker(text).setSmallIcon(R.mipmap.icon)
                .setWhen(System.currentTimeMillis())
                .setContentText(text)
                .setContentIntent(content)
                .build();


        Notification notification = nb.getNotification();
        mNM.notify(NOTIFICATION, notification);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // record and save the readings from the sensor
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        SensorReading sr = new SensorReading(sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2]);
        sensorDataProcessing.addToSensorReading(sr);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
