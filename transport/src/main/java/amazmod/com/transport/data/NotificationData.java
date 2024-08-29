package amazmod.com.transport.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import amazmod.com.transport.Transportable;

public class NotificationData extends Transportable implements Parcelable {

    public static final String EXTRA = "notificationSpec";

    private final String DATA_KEY = "key";
    private final String DATA_PACKAGE_NAME = "packageName";
    private final String DATA_ID = "id";
    private final String DATA_TITLE = "title";
    private final String DATA_TIME = "time";
    private final String DATA_TEXT = "text";
    private final String DATA_ICON = "icon";
    private final String DATA_LARGE_ICON = "largeIcon";
    private final String DATA_VIBRATION = "vibration";
    private final String DATA_VIBRATION_AMOUNT = "vibration_amount";
    private final String DATA_FORCE_CUSTOM = "forceCustom";
    private final String DATA_HIDE_REPLIES = "hideReplies";
    private final String DATA_HIDE_BUTTONS = "hideButtons";
    private final String DATA_PICTURE = "picture";
    private final String DATA_ACTION_TITLES = "actionTitles";
    private final String DATA_REPLY_TITLES = "replyTitles";

    private String key;
    private String packageName;
    private int id;
    private String title;
    private String time;
    private String text;
    private byte[] icon;
    private byte[] largeIcon;
    private byte[] picture;
    private int vibration;
    private int vibrationAmount;
    private boolean isDeviceLocked;
    private int timeoutRelock;
    private boolean forceCustom;
    private boolean hideReplies;
    private boolean hideButtons;
    private String[] actionTitles;
    private String[] replyTitles;

    public NotificationData() {
    }

    protected NotificationData(Parcel in) {
        key = in.readString();
        packageName = in.readString();
        id = in.readInt();
        title = in.readString();
        time = in.readString();
        text = in.readString();
        icon = in.createByteArray();
        largeIcon = in.createByteArray();
        picture = in.createByteArray();
        vibration = in.readInt();
        vibrationAmount = in.readInt();
        isDeviceLocked = in.readByte() != 0;
        timeoutRelock = in.readInt();
        forceCustom = in.readByte() != 0;
        hideReplies = in.readByte() != 0;
        hideButtons = in.readByte() != 0;
        actionTitles = in.createStringArray();
        replyTitles = in.createStringArray();
    }

    public static final Parcelable.Creator<NotificationData> CREATOR = new Parcelable.Creator<NotificationData>() {
        @Override
        public NotificationData createFromParcel(Parcel in) {
            return new NotificationData(in);
        }

        @Override
        public NotificationData[] newArray(int size) {
            return new NotificationData[size];
        }
    };

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public byte[] getIcon() {
        return icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    public boolean getForceCustom() {
        return forceCustom;
    }

    public void setForceCustom(boolean forceCustom) {
        this.forceCustom = forceCustom;
    }

    public boolean getHideReplies() {
        return hideReplies;
    }

    public void setHideReplies(boolean hideReplies) {
        this.hideReplies = hideReplies;
    }

    public boolean getHideButtons() {
        return hideButtons;
    }

    public void setHideButtons(boolean hideButtons) {
        this.hideButtons = hideButtons;
    }

    public boolean isDeviceLocked() {
        return isDeviceLocked;
    }

    public void setDeviceLocked(boolean deviceLocked) {
        isDeviceLocked = deviceLocked;
    }

    public int getVibration() {
        return vibration;
    }

    public void setVibration(int vibration) {
        this.vibration = vibration;
    }

    public int getVibrationAmount() {
        return vibrationAmount;
    }

    public void setVibrationAmount(int vibrationAmount) {
        this.vibrationAmount = vibrationAmount;
    }

    public int getTimeoutRelock() {
        return timeoutRelock;
    }

    public void setTimeoutRelock(int timeoutRelock) {
        this.timeoutRelock = timeoutRelock;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTime() {
        return time;
    }


    public byte[] getLargeIcon() {
        return largeIcon;
    }

    public void setLargeIcon(byte[] largeIcon) {
        this.largeIcon = largeIcon;
    }

    public byte[] getPicture() {
        return picture;
    }

    public void setPicture(byte[] picture) {
        this.picture = picture;
    }

    public String[] getActionTitles() {
        return actionTitles;
    }

    public void setActionTitles(String[] actionTitles) {
        this.actionTitles = actionTitles;
    }

    public String[] getReplyTitles() {
        return replyTitles;
    }

    public void setReplyTitles(String[] replyTitles) {
        this.replyTitles = replyTitles;
    }

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putString(DATA_KEY, key);
        dataBundle.putString(DATA_PACKAGE_NAME, packageName);
        dataBundle.putInt(DATA_ID, id);
        dataBundle.putString(DATA_TITLE, title);
        dataBundle.putString(DATA_TIME, time);
        dataBundle.putString(DATA_TEXT, text);
        dataBundle.putByteArray(DATA_ICON, icon);
        dataBundle.putByteArray(DATA_LARGE_ICON, largeIcon);
        dataBundle.putByteArray(DATA_PICTURE, picture);
        dataBundle.putInt(DATA_VIBRATION, vibration);
        dataBundle.putInt(DATA_VIBRATION_AMOUNT, vibrationAmount);
        dataBundle.putBoolean(DATA_FORCE_CUSTOM, forceCustom);
        dataBundle.putBoolean(DATA_HIDE_REPLIES, hideReplies);
        dataBundle.putBoolean(DATA_HIDE_BUTTONS, hideButtons);
        dataBundle.putStringArray(DATA_ACTION_TITLES, actionTitles);
        dataBundle.putStringArray(DATA_REPLY_TITLES, replyTitles);

        return dataBundle;
    }

    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static NotificationData fromBundle(Bundle bundle) {
        return bundle.getParcelable(EXTRA);
    }

    public static NotificationData fromDataBundle(DataBundle dataBundle) {
        NotificationData notificationData = new NotificationData();

        String title = dataBundle.getString("title");
        String time = dataBundle.getString("time");
        String text = dataBundle.getString("text");
        int id = dataBundle.getInt("id");
        String key = dataBundle.getString("key");
        String packageName = dataBundle.getString("packageName");
        byte[] icon = dataBundle.getByteArray("icon");
        byte[] largeIcon = dataBundle.getByteArray("largeIcon");
        byte[] picture = dataBundle.getByteArray("picture");
        int vibration = dataBundle.getInt("vibration");
        int vibrationAmount = dataBundle.getInt("vibration_amount");
        boolean forceCustom = dataBundle.getBoolean("forceCustom");
        boolean hideReplies = dataBundle.getBoolean("hideReplies");
        boolean hideButtons = dataBundle.getBoolean("hideButtons");
        String[] actionTitles = dataBundle.getStringArray("actionTitles");
        String[] replyTitles = dataBundle.getStringArray("replyTitles");

        notificationData.setTitle(title);
        notificationData.setTime(time);
        notificationData.setText(text);
        notificationData.setIcon(icon);
        notificationData.setLargeIcon(largeIcon);
        notificationData.setPicture(picture);
        notificationData.setVibration(vibration);
        notificationData.setVibrationAmount(vibrationAmount);
        notificationData.setId(id);
        notificationData.setKey(key);
        notificationData.setPackageName(packageName);
        notificationData.setForceCustom(forceCustom);
        notificationData.setHideReplies(hideReplies);
        notificationData.setHideButtons(hideButtons);
        notificationData.setActionTitles(actionTitles);
        notificationData.setReplyTitles(replyTitles);

        return notificationData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(key);
        dest.writeString(packageName);
        dest.writeInt(id);
        dest.writeString(title);
        dest.writeString(time);
        dest.writeString(text);
        dest.writeByteArray(icon);
        dest.writeByteArray(largeIcon);
        dest.writeByteArray(picture);
        dest.writeInt(vibration);
        dest.writeInt(vibrationAmount);
        dest.writeByte((byte) (isDeviceLocked ? 1 : 0));
        dest.writeInt(timeoutRelock);
        dest.writeByte((byte) (forceCustom ? 1 : 0));
        dest.writeByte((byte) (hideReplies ? 1 : 0));
        dest.writeByte((byte) (hideButtons ? 1 : 0));
        dest.writeStringArray(actionTitles);
        dest.writeStringArray(replyTitles);
    }

}
