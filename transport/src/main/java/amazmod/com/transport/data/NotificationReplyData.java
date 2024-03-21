package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class NotificationReplyData extends Transportable implements Parcelable {

    public static final String EXTRA = "notificationReply";

    public static final String NOTIFICATION_ID = "notificationId";
    public static final String REPLY = "reply";
    public static final String TITLE = "title";

    private Integer notificationId;
    private String title;
    private String reply;

    public NotificationReplyData() {
    }

    protected NotificationReplyData(Parcel in) {
        notificationId = in.readInt();
        title = in.readString();
        reply = in.readString();
    }

    public static final Creator<NotificationReplyData> CREATOR = new Creator<NotificationReplyData>() {
        @Override
        public NotificationReplyData createFromParcel(Parcel in) {
            return new NotificationReplyData(in);
        }

        @Override
        public NotificationReplyData[] newArray(int size) {
            return new NotificationReplyData[size];
        }
    };

    public Integer getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Integer notificationId) {
        this.notificationId = notificationId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public static NotificationReplyData fromDataBundle(DataBundle dataBundle) {
        NotificationReplyData notificationReplyData = new NotificationReplyData();

        notificationReplyData.setNotificationId(dataBundle.getInt("id"));
        notificationReplyData.setReply(dataBundle.getString("message"));
        notificationReplyData.setTitle(dataBundle.getString("title"));

        return notificationReplyData;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {

        dataBundle.putInt(NOTIFICATION_ID, notificationId);
        dataBundle.putString(REPLY, reply);
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
        dest.writeString(reply);
        dest.writeString(title);
    }
}
