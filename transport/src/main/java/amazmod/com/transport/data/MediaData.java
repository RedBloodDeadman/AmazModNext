package amazmod.com.transport.data;

import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import com.huami.watch.transport.DataBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import amazmod.com.transport.Transportable;

public class MediaData extends Transportable implements Parcelable {

    public static final String EXTRA = "mediaSpec";
    private static final String DATA_ID = "id";
    private static final String DATA_APP_NAME = "appName";
    private static final String DATA_TITLE = "title";
    private static final String DATA_ARTIST = "artist";
    private static final String DATA_DURATION = "duration";
    private static final String DATA_POSITION = "position";
    private static final String DATA_PLAY_STATE = "playState";
    private static final String DATA_VOLUME = "volume";
    private static final String DATA_SMALL_ICON = "smallIcon";
    private static final String DATA_ALBUM_ART = "albumArt";
    private static final String DATA_ACTIONS = "actionTitles";
    private static final String DATA_MAX_VOLUME = "maxVolume";
    private static final String DATA_TIMESTAMP = "timestamp";

    private int id;
    private String appName;
    private String title;
    private String artist;
    private long duration;
    private long position;
    private String playState = "STOPPED";
    private byte[] smallIcon;
    private byte[] albumArt;
    private String[] actionTitles;
    private int volume;
    private int maxVolume;
    private long timestamp;

    public MediaData() {
    }

    public MediaData(int id, long timestamp) {
        this.id = id;
        this.timestamp = timestamp;
    }

    public MediaData(
            int id,
            String appName,
            String title,
            String artist,
            long duration,
            long position,
            String playState,
            byte[] smallIcon,
            byte[] albumArt,
            String[] actionTitles,
            int volume,
            int maxVolume,
            long timestamp) {
        this.id = id;
        this.appName = appName;
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.position = position;
        this.playState = playState;
        this.smallIcon = smallIcon;
        this.albumArt = albumArt;
        this.actionTitles = actionTitles;
        this.volume = volume;
        this.maxVolume = maxVolume;
        this.timestamp = timestamp;
    }

    protected MediaData(Parcel in) {
        id = in.readInt();
        appName = in.readString();
        title = in.readString();
        artist = in.readString();
        duration = in.readLong();
        position = in.readLong();
        playState = in.readString();
        smallIcon = in.createByteArray();
        albumArt = in.createByteArray();
        actionTitles = in.createStringArray();
        volume = in.readInt();
        maxVolume = in.readInt();
        timestamp = in.readLong();
    }

    public static final Parcelable.Creator<MediaData> CREATOR = new Parcelable.Creator<MediaData>() {
        @Override
        public MediaData createFromParcel(Parcel in) {
            return new MediaData(in);
        }

        @Override
        public MediaData[] newArray(int size) {
            return new MediaData[size];
        }
    };

    @Override
    public DataBundle toDataBundle(DataBundle dataBundle) {
        dataBundle.putInt(DATA_ID, id);
        dataBundle.putString(DATA_APP_NAME, appName);
        dataBundle.putString(DATA_TITLE, title);
        dataBundle.putString(DATA_ARTIST, artist);
        dataBundle.putLong(DATA_DURATION, duration);
        dataBundle.putLong(DATA_POSITION, position);
        dataBundle.putString(DATA_PLAY_STATE, playState);
        dataBundle.putInt(DATA_VOLUME, volume);
        dataBundle.putByteArray(DATA_SMALL_ICON, smallIcon);
        dataBundle.putByteArray(DATA_ALBUM_ART, albumArt);
        dataBundle.putStringArray(DATA_ACTIONS, actionTitles);
        dataBundle.putInt(DATA_MAX_VOLUME, maxVolume);
        dataBundle.putLong(DATA_TIMESTAMP, timestamp);
        return dataBundle;
    }

    @Override
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(EXTRA, this);
        return bundle;
    }

    public static MediaData fromBundle(Bundle bundle) {
        return bundle.getParcelable(EXTRA);
    }

    public static MediaData fromDataBundle(DataBundle dataBundle) {
        MediaData mediaData = new MediaData();

        int id = dataBundle.getInt(DATA_ID);
        String appName = dataBundle.getString(DATA_APP_NAME);
        String title = dataBundle.getString(DATA_TITLE);
        String artist = dataBundle.getString(DATA_ARTIST);
        String playState = dataBundle.getString(DATA_PLAY_STATE);
        long duration = dataBundle.getLong(DATA_DURATION);
        long position = dataBundle.getLong(DATA_POSITION);
        byte[] smallIcon = dataBundle.getByteArray(DATA_SMALL_ICON);
        byte[] albumArt = dataBundle.getByteArray(DATA_ALBUM_ART);
        String[] actionTitles = dataBundle.getStringArray(DATA_ACTIONS);
        int volume = dataBundle.getInt(DATA_VOLUME);
        int volumeMax = dataBundle.getInt(DATA_MAX_VOLUME);
        long timestamp = dataBundle.getLong(DATA_TIMESTAMP);


        mediaData.setId(id);
        mediaData.setAppName(appName);
        mediaData.setTitle(title);
        mediaData.setArtist(artist);
        mediaData.setPlayState(playState);
        mediaData.setDuration(duration);
        mediaData.setPosition(position);
        mediaData.setSmallIcon(smallIcon);
        mediaData.setAlbumArt(albumArt);
        mediaData.setActionTitles(actionTitles);
        mediaData.setVolume(volume);
        mediaData.setMaxVolume(volumeMax);
        mediaData.setTimestamp(timestamp);

        return mediaData;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(appName);
        dest.writeString(title);
        dest.writeString(artist);
        dest.writeLong(duration);
        dest.writeLong(position);
        dest.writeString(playState);
        dest.writeByteArray(smallIcon);
        dest.writeByteArray(albumArt);
        dest.writeStringArray(actionTitles);
        dest.writeInt(volume);
        dest.writeInt(maxVolume);
        dest.writeLong(timestamp);
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getPlayState() {
        return playState;
    }

    public void setPlayState(String playState) {
        this.playState = playState;
    }

    public byte[] getAlbumArt() {
        return albumArt;
    }

    public void setAlbumArt(byte[] albumArt) {
        this.albumArt = albumArt;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(int maxVolume) {
        this.maxVolume = maxVolume;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public byte[] getSmallIcon() {
        return smallIcon;
    }

    public void setSmallIcon(byte[] smallIcon) {
        this.smallIcon = smallIcon;
    }

    public String[] getActionTitles() {
        return actionTitles;
    }

    public void setActionTitles(String[] actionTitles) {
        this.actionTitles = actionTitles;
    }

    public long getPosition() {
        return position;
    }

    public void setPosition(long position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "MediaData{" +
                "id=" + id +
                ", appName='" + appName + '\'' +
                ", title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", duration=" + duration +
                ", position=" + position +
                ", playState='" + playState + '\'' +
                ", actionTitles=" + Arrays.toString(actionTitles) +
                ", volume=" + volume +
                ", maxVolume=" + maxVolume +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MediaData mediaData = (MediaData) o;
        return id == mediaData.id && duration == mediaData.duration
                && volume == mediaData.volume
                && maxVolume == mediaData.maxVolume
                && Objects.equals(appName, mediaData.appName)
                && Objects.equals(title, mediaData.title)
                && Objects.equals(artist, mediaData.artist)
                && Objects.equals(playState, mediaData.playState)
                && Objects.deepEquals(actionTitles, mediaData.actionTitles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, appName, volume, maxVolume, title, artist, duration, playState, Arrays.hashCode(actionTitles));
    }
}
