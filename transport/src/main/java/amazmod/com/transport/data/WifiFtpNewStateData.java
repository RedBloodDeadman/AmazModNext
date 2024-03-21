package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class WifiFtpNewStateData extends Transportable implements Parcelable {

    public static final String EXTRA = "wifi_ftp_state";
    public static final String KEY_NEW_STATE = "key_new_state";


    private Integer newState;

    public WifiFtpNewStateData() {
    }

    protected WifiFtpNewStateData(Parcel in) {
        newState = in.readInt();
    }

    public static final Creator<WifiFtpNewStateData> CREATOR = new Creator<WifiFtpNewStateData>() {
        @Override
        public WifiFtpNewStateData createFromParcel(Parcel in) {
            return new WifiFtpNewStateData(in);
        }

        @Override
        public WifiFtpNewStateData[] newArray(int size) {
            return new WifiFtpNewStateData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putInt(KEY_NEW_STATE, newState);

        return dataBundle;
    }

    public static WifiFtpNewStateData fromDataBundle(DataBundle dataBundle) {
        WifiFtpNewStateData wifiFtpNewStateData = new WifiFtpNewStateData();

        //Always returns NULL on Stratos 3
        //wifiFtpNewStateData.setNewState(dataBundle.getInt(KEY_NEW_STATE));

        return wifiFtpNewStateData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }


    public Integer getNewState() {
        return newState;
    }

    public void setNewState(Integer newState) {
        this.newState = newState;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(newState);
    }
}
