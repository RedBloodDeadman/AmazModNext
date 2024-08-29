package com.amazmod.service.ui.fragments;

import static com.amazmod.service.util.SystemProperties.isPace;
import static com.amazmod.service.util.SystemProperties.isStratos;
import static com.amazmod.service.util.SystemProperties.isStratos3;
import static com.amazmod.service.util.SystemProperties.isVerge;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.view.CircledImageView;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.amazmod.service.R;
import com.amazmod.service.util.ButtonListener;
import com.huami.watch.transport.Transporter;
import com.huami.watch.transport.TransporterClassic;

import org.tinylog.Logger;

import amazmod.com.transport.Transport;

public class WearMusicFragment extends Fragment {
    private Transporter transporter;
    private Context mContext;
    private View mView;
    private CircledImageView playPause, next, prev, volUp, volDown, volMute;
    private ButtonListener btnListener = new ButtonListener();

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
    }

    private void init() {
        playPause = mView.findViewById(R.id.music_play_pause);
        prev = mView.findViewById(R.id.music_prev);
        next = mView.findViewById(R.id.music_next);
        volDown = mView.findViewById(R.id.music_vol_down);
        volMute = mView.findViewById(R.id.music_vol_mute);
        volUp = mView.findViewById(R.id.music_vol_up);
        transporter = TransporterClassic.get(mContext, Transport.NAME);

        playPause.setOnClickListener(v -> playPause());
        prev.setOnClickListener(v -> prev());
        next.setOnClickListener(v -> next());
        volUp.setOnClickListener(v -> volUp());
        volDown.setOnClickListener(v -> volDown());
        volMute.setOnClickListener(v -> volMute());
    }

    private void playPause() {
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.TOGGLE_MUSIC));
    }

    private void next() {
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.NEXT_MUSIC));
    }

    private void prev() {
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.PREV_MUSIC));
    }

    private void volUp() {
        vibrate();
        if (!transporter.isTransportServiceConnected()) transporter.connectTransportService();
        new Handler().post(() -> transporter.send(Transport.VOL_UP));
    }

    private void volDown() {
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
            mVibrator.vibrate(100);
        }
    }
}
