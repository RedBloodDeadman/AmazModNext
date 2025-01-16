package com.amazmod.service.sleep;

import android.hardware.SensorManager;

public class sleepConstants {

    //HR Sensor
    public static final int HR_VALUES = 10;
    public static final long HR_INTERVAL = 5 * 60 * 1000; //5m

    //Accelerometer
    public static final int SECS_PER_MAX_VALUE = 10;//10s
    public static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_NORMAL;

    //Other
    public static final int NOTIFICATION_ID = 1834;
    public static final String NOTIFICATION_KEY = "amazmod|SAA";


}
