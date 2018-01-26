package com.example.ramezelbaroudy.arffnew;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by ramezelbaroudy on 1/22/18.
 */

public class SensorDataProcessing {
    Queue<SensorReading> sensorReadingsQueue;
    // check if 3000 window exceeded then remove old elements
    boolean exceededTime;


    public SensorDataProcessing() {
        sensorReadingsQueue = new ArrayDeque<SensorReading>();
        delay();
        exceededTime = false;
    }

    public void delay() {
        Timer slidingWindowTimer = new Timer();
        slidingWindowTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                exceededTime = true;
            }
        }, 3000);
    }

    public void addToSensorReading(SensorReading newSensorReading) {

        sensorReadingsQueue.add(newSensorReading);
        if (exceededTime) {
            sensorReadingsQueue.remove();
        }
    }

    // calculate mean-z and abs-var for time window 3000ms and pack them as pair
    public Pair abs_varAndMeanZ() {
        double meanX = 0;
        double meanY = 0;
        double meanZ = 0;
        double varianceX = 0;
        double varianceY = 0;
        double varianceZ = 0;
        for (SensorReading sr : sensorReadingsQueue) {
            meanX += sr.xCoordinate;
            meanY += sr.yCoordinate;
            meanZ += sr.zCoordinate;
        }
        meanX /= sensorReadingsQueue.size();
        meanY /= sensorReadingsQueue.size();
        meanZ /= sensorReadingsQueue.size();

        for (SensorReading sr : sensorReadingsQueue) {
            varianceX += Math.pow((sr.xCoordinate - meanX), 2);
            varianceY += Math.pow((sr.yCoordinate - meanY), 2);
            varianceZ += Math.pow((sr.zCoordinate - meanZ), 2);
        }
        varianceX = varianceX / sensorReadingsQueue.size();
        varianceY = varianceY / sensorReadingsQueue.size();
        varianceZ = varianceZ / sensorReadingsQueue.size();

        double magntiudeVariance = Math.sqrt((varianceX + varianceY + varianceZ));
        Pair returnedPair = new Pair(meanZ, magntiudeVariance);
        return returnedPair;
    }


}
