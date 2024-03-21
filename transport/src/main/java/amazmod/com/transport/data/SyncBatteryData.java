package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class SyncBatteryData extends Transportable implements Parcelable {

    public static final String EXTRA = "Bundle";

    private static final String BATTERY_LEVEL = "BatteryLevel";
    private static final String BATTERY_IS_CHARGING = "BatteryIsCharging";
    private static final String CHARGING_TIME = "ChargingTime";
    private static final String CHARGING_INTERVAL_DAYS = "ChargingIntervalDays";

    private int level;
    private boolean charging;
    private long chargingTime;
    private int chargingIntervalDays;

    public SyncBatteryData() {}

    protected SyncBatteryData(Parcel in) {
        level = in.readInt();
        charging = in.readByte() != 0;
        chargingTime = in.readLong();
        chargingIntervalDays = in.readInt();
    }

    public static final Creator<SyncBatteryData> CREATOR = new Creator<SyncBatteryData>() {
        @Override
        public SyncBatteryData createFromParcel(Parcel in) {
            return new SyncBatteryData(in);
        }

        @Override
        public SyncBatteryData[] newArray(int size) {
            return new SyncBatteryData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putInt(BATTERY_LEVEL, level);
        dataBundle.putBoolean(BATTERY_IS_CHARGING, charging);
        dataBundle.putLong(CHARGING_TIME, chargingTime);
        dataBundle.putInt(CHARGING_INTERVAL_DAYS, chargingIntervalDays);

        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static SyncBatteryData fromBundle(Bundle bundle) {
        return bundle.getParcelable(EXTRA);
    }

    public static SyncBatteryData fromDataBundle(DataBundle dataBundle) {
        SyncBatteryData batteryData = new SyncBatteryData();

        int level = dataBundle.getInt(BATTERY_LEVEL);
        boolean charging = dataBundle.getBoolean(BATTERY_IS_CHARGING);
        long dateLastCharge = dataBundle.getLong(CHARGING_TIME);
        int chargingIntervalDays = dataBundle.getInt(CHARGING_INTERVAL_DAYS);

        batteryData.setLevel(level);
        batteryData.setCharging(charging);
        batteryData.setChargingTime(dateLastCharge);
        batteryData.setChargingIntervalDays(chargingIntervalDays);

        return batteryData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(level);
        dest.writeByte((byte) (charging ? 1 : 0));
        dest.writeLong(chargingTime);
        dest.writeInt(chargingIntervalDays);
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isCharging() {
        return charging;
    }

    public void setCharging(boolean charging) {
        this.charging = charging;
    }

    public long getChargingTime() {
        return chargingTime;
    }

    public void setChargingTime(long chargingTime) {
        this.chargingTime = chargingTime;
    }

    public int getChargingIntervalDays() {
        return chargingIntervalDays;
    }

    public void setChargingIntervalDays(int chargingIntervalDays) {
        this.chargingIntervalDays = chargingIntervalDays;
    }
}
