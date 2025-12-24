package com.amazmod.service.ui.fragments;

import static com.amazmod.service.util.SystemProperties.isPace;
import static com.amazmod.service.util.SystemProperties.isStratos;
import static com.amazmod.service.util.SystemProperties.isStratos3;
import static com.amazmod.service.util.SystemProperties.isVerge;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.amazmod.service.R;
import com.amazmod.service.events.ActionNotificationEvent;
import com.amazmod.service.helper.MediaDataManager;
import com.amazmod.service.ui.MarqueeTextView;
import com.amazmod.service.util.ButtonListener;
import com.amazmod.service.util.TimeUtils;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import amazmod.com.transport.Transport;
import amazmod.com.transport.data.MediaData;
import amazmod.com.transport.util.ImageUtils;
import clc.sliteplugin.flowboard.ISpringBoardHostStub;

public class WearMusicFragment extends Fragment implements MediaDataManager.DataListener {
    private Transporter transporter;
    private Context mContext;
    private View mView;
    private MarqueeTextView title, name, appName;
    private ProgressBar volumeProgress, progressBar;
    private ImageView playPause, next, prev, volUp, volDown, like, dislike;
    private ButtonListener btnListener = new ButtonListener();
    private ImageView albumArt, albumArtFull, appIcon;

    private MediaData mediaData = null;
    private MediaData mediaDataForDiff = null;

    private ISpringBoardHostStub host = null;

    private boolean nowPlaying = false;

    private int currentVol, maxVol = 0;

    //Spotify, СберЗвук, Я.Музыка, YouTube
    private static List<String> likeList = Arrays.asList("Добавить в любимые", "Кнопка like", "Поставить отметку \"Нравится\"", "Поставить «Нравится»");
    private static List<String> dislikeList = Arrays.asList("Удалить из любимых", "Не включать этот трек", "Кнопка палец вниз", "Поставить отметку \"Не нравится\"");

    public String containsTitle(String[] actionTitles, List<String> checkList) {
        if (actionTitles == null) {
            return "";
        }

        for (String title : actionTitles) {
            if (checkList.contains(title)) {
                return title;
            }
        }
        return "";
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("WearMusicFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.info("WearMusicFragment onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("WearMusicFragment onCreateView");
        mView = inflater.inflate(R.layout.fragment_wear_music, container, false);
        return mView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("WearMusicFragment onViewCreated");
        init(); //initialize
        MediaDataManager.getInstance().addListener(this);
        requestMediaInfo();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        setupBtnListener();
    }

    @Override
    public void onPause() {
        super.onPause();
        btnListener.stop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MediaDataManager.getInstance().removeListener(this);
    }

    private void init() {
        title = mView.findViewById(R.id.title);
        name = mView.findViewById(R.id.name);
        volumeProgress = mView.findViewById(R.id.volumeProgress);
        albumArt = mView.findViewById(R.id.albumArt);
        albumArtFull = mView.findViewById(R.id.albumArtFull);
        appIcon = mView.findViewById(R.id.appIcon);
        appName = mView.findViewById(R.id.appName);
        progressBar = mView.findViewById(R.id.time_progress);
        like = mView.findViewById(R.id.like);
        dislike = mView.findViewById(R.id.dislike);

        playPause = mView.findViewById(R.id.music_play_pause);
        prev = mView.findViewById(R.id.music_prev);
        next = mView.findViewById(R.id.music_next);
        volDown = mView.findViewById(R.id.music_vol_down);
        volUp = mView.findViewById(R.id.music_vol_up);
        transporter = TransporterClassic.get(mContext, Transport.NAME);

        playPause.setOnClickListener(v -> playPause());
        prev.setOnClickListener(v -> prev());
        next.setOnClickListener(v -> next());
        volUp.setOnClickListener(v -> volUp());
        volDown.setOnClickListener(v -> volDown());

        updateLikeDislikeVisibility(this.mediaData);

        like.setOnClickListener(view -> {
            sendLike(likeList);
        });
        dislike.setOnClickListener(view -> {
            sendLike(dislikeList);
        });
    }

    private void updateLikeDislikeVisibility(MediaData mediaData) {
        if (mediaData != null) {
            String containsLike = containsTitle(mediaData.getActionTitles(), likeList);
            String containsDislike = containsTitle(mediaData.getActionTitles(), dislikeList);
            like.setVisibility(containsLike.isEmpty() ? View.GONE : View.VISIBLE);
            dislike.setVisibility(containsDislike.isEmpty() ? View.GONE : View.VISIBLE);
        } else {
            like.setVisibility(View.GONE);
            dislike.setVisibility(View.GONE);
        }
    }

    private void sendLike(List<String> list) {
        //loading(true);
        vibrate();
        if (mediaData != null) {
            String title = containsTitle(mediaData.getActionTitles(), list);
            if (!title.isEmpty()) {
                EventBus.getDefault().post(new ActionNotificationEvent(mediaData.getId(), title));
                Toast.makeText(mContext, "OK", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void playPause() {
        nowPlaying = !nowPlaying;
        updatePlayPause();
        loading(true);
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.TOGGLE_MUSIC));
    }

    private void updatePlayPause() {
        if (nowPlaying) {
            playPause.setBackgroundResource(R.drawable.baseline_pause_24);
        } else {
            playPause.setBackgroundResource(R.drawable.baseline_play_arrow_24);
        }
    }

    private void next() {
        //loading(true);
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.NEXT_MUSIC));
    }

    private void prev() {
        //loading(true);
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.PREV_MUSIC));
    }

    private void volUp() {
        updateVolume(+1);
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.VOL_UP));
    }

    private void requestMediaInfo() {
        loading(true);
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.GET_MEDIA_INFO));
    }

    private void volDown() {
        updateVolume(-1);
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.VOL_DOWN));
    }

    private void volMute() {
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.VOL_MUTE));
    }

    private void setupBtnListener() {
        Handler btnHandler = new Handler();
        btnListener.start(mContext, keyEvent -> {
            if ((isPace() || isVerge() || isStratos()) && keyEvent.getCode() == ButtonListener.KEY_CENTER) {
                btnHandler.post(this::playPause);
            } else if (isStratos3())
                if (keyEvent.getCode() == ButtonListener.S3_KEY_UP) {
                    btnHandler.post(this::playPause);
                } else if (keyEvent.getCode() == ButtonListener.S3_KEY_MIDDLE_UP) {
                    if (!keyEvent.isLongPress()) {
                        btnHandler.post(this::volUp);
                    } else {
                        btnHandler.post(this::next);
                    }
                } else if (keyEvent.getCode() == ButtonListener.S3_KEY_MIDDLE_DOWN) {
                    if (!keyEvent.isLongPress()) {
                        btnHandler.post(this::volDown);
                    } else {
                        btnHandler.post(this::prev);
                    }
                }

        });
    }

    public static WearMusicFragment newInstance() {
        Logger.info("WearCameraFragment newInstance");
        return new WearMusicFragment();
    }

    public void vibrate() {
        final Vibrator mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (mVibrator != null) {
            mVibrator.vibrate(30);
        }
    }

    @Override
    public void onDataUpdated(MediaData mediaData) {
        if (mediaData == null || (this.mediaDataForDiff != null && this.mediaDataForDiff.getTimestamp() > mediaData.getTimestamp())) {
            loading(false);
            return;
        }
        Logger.trace("WearMusicFragment " + mediaData.toString());

        String playState = mediaData.getPlayState();
        if (playState != null) {

            boolean isNewMediaOrState =
                    this.mediaDataForDiff == null ||
                            (this.mediaDataForDiff != null && !Objects.equals(this.mediaDataForDiff.getPlayState(), playState)) ||
                            (this.mediaDataForDiff != null && this.mediaDataForDiff.getId() != mediaData.getId());
            this.mediaDataForDiff = mediaData;

            if (isNewMediaOrState) {
                Logger.trace("NewMedia");
                if (playState.contains("BUFFERING") || playState.contains("LOADING")) {
                    Logger.trace("BUFFERING or LOADING");
                    loading(true);
                } else if (playState.contains("ART")) {
                    byte[] albumArt1 = mediaData.getAlbumArt();
                    Bitmap bitmap = ImageUtils.bytes2Bitmap(albumArt1);
                    if (bitmap != null) {
                        albumArt.setImageBitmap(bitmap);
                        albumArtFull.setImageBitmap(bitmap);
                    } else {
                        albumArt.setImageResource(R.drawable.baseline_music_note_24);
                        albumArtFull.setImageResource(R.mipmap.vinyl);
                    }

                    byte[] smallIcon = mediaData.getSmallIcon();
                    Bitmap smallIconBitmap = ImageUtils.bytes2Bitmap(smallIcon);

                    if (smallIconBitmap != null) {
                        appIcon.setImageBitmap(smallIconBitmap);
                    } else {
                        appIcon.setImageResource(R.mipmap.amazmod_small);
                    }
                } else {
                    this.mediaData = mediaData;
                    updateLikeDislikeVisibility(mediaData);

                    currentVol = mediaData.getVolume();
                    maxVol = mediaData.getMaxVolume();

                    title.setText(mediaData.getTitle());
                    name.setText(mediaData.getArtist());
                    appName.setText(mediaData.getAppName());

                    nowPlaying = playState.contains("PLAYING");
                    updatePlayPause();

                    updateVolume(0);
                }
                loading(false);
            } else {
                Logger.trace("Something else...");
                loading(false);
            }
        }else{
            Logger.trace("PlayState is null.");
            loading(false);
        }
    }

    private void loading(boolean now) {
        progressBar.setVisibility(now ? View.VISIBLE : View.GONE);
        progressBar.setIndeterminate(now);
    }

    private void updateVolume(int delta) {
        int newVol = currentVol += delta;
        if (newVol >= 0 && newVol <= maxVol) {
            volumeProgress.setMax(maxVol);
            volumeProgress.setProgress(newVol);
        }
    }
}
