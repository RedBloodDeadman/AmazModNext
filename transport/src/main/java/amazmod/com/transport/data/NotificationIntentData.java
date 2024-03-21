package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class NotificationIntentData extends Transportable implements Parcelable {

    public static final String EXTRA = "notificationIntent";

    public static final String NOTIFICATION_ID = "notificationId";
    public static final String PACKAGE_NAME = "packageName";

    private Integer notificationId;
    private String packageName;

    public NotificationIntentData() {
    }

    protected NotificationIntentData(Parcel in) {
        notificationId = in.readInt();
        packageName = in.readString();
    }

    public static final Creator<NotificationIntentData> CREATOR = new Creator<NotificationIntentData>() {
        @Override
        public NotificationIntentData createFromParcel(Parcel in) {
            return new NotificationIntentData(in);
        }

        @Override
        public NotificationIntentData[] newArray(int size) {
            return new NotificationIntentData[size];
        }
    };

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public static NotificationIntentData fromDataBundle(DataBundle dataBundle) {
        NotificationIntentData notificationReplyData = new NotificationIntentData();

        notificationReplyData.setNotificationId(dataBundle.getInt("id"));
        notificationReplyData.setPackageName(dataBundle.getString("packageName"));

        return notificationReplyData;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putInt(NOTIFICATION_ID, notificationId);
        dataBundle.putString(PACKAGE_NAME, packageName);

        return dataBundle;
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
        dest.writeInt(notificationId);
        dest.writeString(packageName);
    }
}
