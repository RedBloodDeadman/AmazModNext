package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class NotificationActionData extends Transportable implements Parcelable {

    public static final String EXTRA = "notificationAction";

    public static final String NOTIFICATION_ID = "notificationId";
    public static final String TITLE = "title";

    private Integer notificationId;
    private String title;

    public NotificationActionData() {
    }

    protected NotificationActionData(Parcel in) {
        notificationId = in.readInt();
        title = in.readString();
    }

    public static final Creator<NotificationActionData> CREATOR = new Creator<NotificationActionData>() {
        @Override
        public NotificationActionData createFromParcel(Parcel in) {
            return new NotificationActionData(in);
        }

        @Override
        public NotificationActionData[] newArray(int size) {
            return new NotificationActionData[size];
        }
    };

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static NotificationActionData fromDataBundle(DataBundle dataBundle) {
        NotificationActionData notificationReplyData = new NotificationActionData();

        notificationReplyData.setNotificationId(dataBundle.getInt("id"));
        notificationReplyData.setTitle(dataBundle.getString("title"));

        return notificationReplyData;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putInt(NOTIFICATION_ID, notificationId);
        dataBundle.putString(TITLE, title);

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
        dest.writeString(title);
    }
}
