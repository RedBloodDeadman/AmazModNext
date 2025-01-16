package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

import com.huami.watch.transport.DataBundle;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import amazmod.com.transport.Transportable;

public class SleepData extends Transportable implements Parcelable {
    public static final String EXTRA_PAUSE_TIMESTAMP = "ptimestamp";

    public static final String EXTRA = "sleepData";
    public static final String EXTRA_ACTION = "sleepaction";
    public static final String EXTRA_SUSPENDED = "suspended";
    public static final String EXTRA_HR_MONITORING = "dohrmonitoring";
    public static final String EXTRA_BATCH_SIZE = "batchsize";
    public static final String EXTRA_DELAY = "delay";
    public static final String EXTRA_HOUR = "hour";
    public static final String EXTRA_MINUTE = "min";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_TEXT = "text";
    public static final String EXTRA_REPEAT = "repeat";
    public static final String EXTRA_MAX_DATA = "maxdata";
    public static final String EXTRA_MAX_RAW_DATA = "maxrawdata";
    public static final String EXTRA_HRDATA = "hrdata";

    private int action;
    private boolean suspended;
    private boolean doHrMonitoring;
    private long batchsize;
    private int delay;
    private int hour;
    private int minute;
    private long timestamp;
    private String title;
    private String text;
    private int repeat;
    private float[] max_data;
    private float[] max_raw_data;
    private float[] hrdata;

    public SleepData() {
    }

    protected SleepData(Parcel in) {
        action = in.readInt();
        suspended = in.readByte() != 0;
        doHrMonitoring = in.readByte() != 0;
        batchsize = in.readLong();
        delay = in.readInt();
        hour = in.readInt();
        minute = in.readInt();
        timestamp = in.readLong();
        title = in.readString();
        text = in.readString();
        repeat = in.readInt();
        max_data = in.createFloatArray();
        max_raw_data = in.createFloatArray();
        hrdata = in.createFloatArray();
    }

    public static final Creator<SleepData> CREATOR = new Creator<SleepData>() {
        @Override
        public SleepData createFromParcel(Parcel in) {
            return new SleepData(in);
        }

        @Override
        public SleepData[] newArray(int size) {
            return new SleepData[size];
        }
    };

    public void setAction(int action) {
        this.action = action;
    }

    public int getAction() {
        return action;
    }

    public boolean isSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    public boolean isDoHrMonitoring() {
        return doHrMonitoring;
    }

    public void setDoHrMonitoring(boolean doHrMonitoring) {
        this.doHrMonitoring = doHrMonitoring;
    }

    public long getBatchsize() {
        return batchsize;
    }

    public void setBatchsize(long batchsize) {
        this.batchsize = batchsize;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getMinute() {
        return minute;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        this.repeat = repeat;
    }

    public float[] getMax_data() {
        return max_data;
    }

    public void setMax_data(float[] max_data) {
        this.max_data = max_data;
    }

    public float[] getMax_raw_data() {
        return max_raw_data;
    }

    public void setMax_raw_data(float[] max_raw_data) {
        this.max_raw_data = max_raw_data;
    }

    public float[] getHrdata() {
        return hrdata;
    }

    public void setHrdata(float[] hrdata) {
        this.hrdata = hrdata;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putInt(EXTRA_ACTION, action);
        dataBundle.putBoolean(EXTRA_SUSPENDED, suspended);
        dataBundle.putBoolean(EXTRA_HR_MONITORING, doHrMonitoring);
        dataBundle.putLong(EXTRA_BATCH_SIZE, batchsize);
        dataBundle.putInt(EXTRA_DELAY, delay);
        dataBundle.putInt(EXTRA_HOUR, hour);
        dataBundle.putInt(EXTRA_MINUTE, minute);
        dataBundle.putLong(EXTRA_TIMESTAMP, timestamp);
        dataBundle.putString(EXTRA_TITLE, title);
        dataBundle.putString(EXTRA_TEXT, text);
        dataBundle.putInt(EXTRA_REPEAT, repeat);
        dataBundle.putString(EXTRA_MAX_DATA, Arrays.toString(max_data));
        dataBundle.putString(EXTRA_MAX_RAW_DATA, Arrays.toString(max_raw_data));
        dataBundle.putString(EXTRA_HRDATA, Arrays.toString(hrdata));
        return dataBundle;
    }

    public static SleepData fromDataBundle(DataBundle dataBundle) {
        SleepData sleepData = new SleepData();
        sleepData.setAction(dataBundle.getInt(SleepData.EXTRA_ACTION));
        sleepData.setSuspended(dataBundle.getBoolean(EXTRA_SUSPENDED));
        sleepData.setDoHrMonitoring(dataBundle.getBoolean(EXTRA_HR_MONITORING));
        sleepData.setBatchsize(dataBundle.getLong(EXTRA_BATCH_SIZE));
        sleepData.setDelay(dataBundle.getInt(EXTRA_DELAY));
        sleepData.setHour(dataBundle.getInt(EXTRA_HOUR));
        sleepData.setMinute(dataBundle.getInt(EXTRA_MINUTE));
        sleepData.setTimestamp(dataBundle.getLong(EXTRA_TIMESTAMP));
        sleepData.setTitle(dataBundle.getString(EXTRA_TITLE));
        sleepData.setText(dataBundle.getString(EXTRA_TEXT));
        sleepData.setRepeat(dataBundle.getInt(EXTRA_REPEAT));
        String maxDataString = dataBundle.getString(EXTRA_MAX_DATA);
        System.out.println("maxDataString: " + maxDataString);
        float[] maxData = getFloats(maxDataString);
        sleepData.setMax_data(maxData);
        String maxRawDataString = dataBundle.getString(EXTRA_MAX_RAW_DATA);
        float[] maxRawData = getFloats(maxRawDataString);
        sleepData.setMax_raw_data(maxRawData);
        String hrDataString = dataBundle.getString(EXTRA_HRDATA);
        float[] hrData = getFloats(hrDataString);
        sleepData.setHrdata(hrData);
        return sleepData;
    }

    private static float[] getFloats(String stringDataArray) {
        if (stringDataArray!=null && !stringDataArray.equals("null")) {
            String[] stringDataArrayList = stringDataArray.substring(1, stringDataArray.length() - 1).split(", ");
            float[] maxData = new float[stringDataArrayList.length];
            for (int i = 0; i < stringDataArrayList.length; i++) {
                String string = stringDataArrayList[i];
                if (string != null && !string.equals("null"))
                    maxData[i] = Float.parseFloat(string);
            }
            return maxData;
        }
        return new float[0];
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(action);
        dest.writeByte((byte) (suspended ? 1 : 0));
        dest.writeByte((byte) (doHrMonitoring ? 1 : 0));
        dest.writeLong(batchsize);
        dest.writeInt(delay);
        dest.writeInt(hour);
        dest.writeInt(minute);
        dest.writeLong(timestamp);
        dest.writeString(title);
        dest.writeString(text);
        dest.writeInt(repeat);
        dest.writeFloatArray(max_data);
        dest.writeFloatArray(max_raw_data);
        dest.writeFloatArray(hrdata);
    }

    public static class actions {
        //Actions from phone
        public static final int ACTION_START_TRACKING = 0;
        public static final int ACTION_STOP_TRACKING = 1;
        public static final int ACTION_SET_PAUSE = 2;
        public static final int ACTION_SET_SUSPENDED = 3;
        public static final int ACTION_SET_BATCH_SIZE = 4;
        public static final int ACTION_START_ALARM = 5;
        public static final int ACTION_STOP_ALARM = 6;
        public static final int ACTION_UPDATE_ALARM = 7;
        public static final int ACTION_SHOW_NOTIFICATION = 8;
        public static final int ACTION_HINT = 9;
        //Actions from watch
        public static final int ACTION_DATA_UPDATE = 10;
        public static final int ACTION_HRDATA_UPDATE = 11;
        //public static final int ACTION_PAUSE_FROM_WATCH = 12; //Not added yet
        //public static final int ACTION_RESUME_FROM_WATCH = 13; //Not added yet
        public static final int ACTION_SNOOZE_FROM_WATCH = 14;
        public static final int ACTION_DISMISS_FROM_WATCH = 15;
    }

    @Override
    public String toString() {
        return "SleepData{" +
                ", action=" + action +
                ", suspended=" + suspended +
                ", doHrMonitoring=" + doHrMonitoring +
                ", batchsize=" + batchsize +
                ", delay=" + delay +
                ", hour=" + hour +
                ", minute=" + minute +
                ", timestamp=" + timestamp +
                ", title='" + title + '\'' +
                ", text='" + text + '\'' +
                ", repeat=" + repeat +
                ", max_data=" + Arrays.toString(max_data) +
                ", max_raw_data=" + Arrays.toString(max_raw_data) +
                ", hrdata=" + Arrays.toString(hrdata) +
                '}';
    }
}
