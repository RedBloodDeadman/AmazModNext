package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class ApResultData extends Transportable implements Parcelable {

    public static final String EXTRA = "ap_result";
    public static final String KEY_RESULT_DATA = "key_result_data";
    public static final String KEY_RESULT_CODE = "key_result_code";


    private String resultData;
    private Integer resultCode;

    public ApResultData() {
    }

    protected ApResultData(Parcel in) {
        resultData = in.readString();
        resultCode = in.readInt();
    }

    public static final Creator<ApResultData> CREATOR = new Creator<ApResultData>() {
        @Override
        public ApResultData createFromParcel(Parcel in) {
            return new ApResultData(in);
        }

        @Override
        public ApResultData[] newArray(int size) {
            return new ApResultData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putInt(KEY_RESULT_CODE, resultCode);
        dataBundle.putString(KEY_RESULT_DATA, resultData);

        return dataBundle;
    }

    public static ApResultData fromDataBundle(DataBundle dataBundle) {
        ApResultData apResultData = new ApResultData();

        //Always returns NULL on Stratos 3
//        apResultData.setResultCode(dataBundle.getInt(KEY_RESULT_CODE));
//        apResultData.setResultData(dataBundle.getString(KEY_RESULT_DATA));

        return apResultData;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }


    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public void setResultCode(Integer resultCode) {
        this.resultCode = resultCode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(resultData);
        dest.writeInt(resultCode);
    }
}
