package com.example.ramezelbaroudy.arffnew;

/**
 * Created by ramezelbaroudy on 1/22/18.
 */

public class SensorReading {
    double xCoordinate;
    double yCoordinate;
    double zCoordinate;
    long time;

    public SensorReading(double xCoordinate, double yCoordinate, double zCoordinate) {
        this.xCoordinate = xCoordinate;
        this.yCoordinate = yCoordinate;
        this.zCoordinate = zCoordinate;
        this.time = System.currentTimeMillis();
    }
}
