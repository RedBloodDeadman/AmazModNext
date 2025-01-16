package com.amazmod.service.ui;

import static java.lang.Math.sqrt;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.support.wearable.view.SwipeDismissFrameLayout;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.emoji.widget.EmojiTextView;

import com.amazmod.service.Constants;
import com.amazmod.service.R;
import com.amazmod.service.events.ActionNotificationEvent;
import com.amazmod.service.events.IntentNotificationEvent;
import com.amazmod.service.events.ReplyNotificationEvent;
import com.amazmod.service.events.SilenceApplicationEvent;
import com.amazmod.service.settings.SettingsManager;
import com.amazmod.service.sleep.sleepConstants;
import com.amazmod.service.support.ActivityFinishRunnable;
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.fragments.WearNotificationsFragment;
import com.amazmod.service.util.ButtonListener;
import com.amazmod.service.util.DeviceUtil;
import com.amazmod.service.util.FragmentUtil;
import com.amazmod.service.util.SystemProperties;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.util.ImageUtils;
import butterknife.BindView;
import butterknife.ButterKnife;

public class NotificationWearActivity extends Activity implements DelayedConfirmationView.DelayedConfirmationListener, SensorEventListener {
    @BindView(R.id.fragment_custom_root_layout)
    BoxInsetLayout rootLayout;

    @BindView(R.id.fragment_wear_swipe_layout)
    SwipeDismissFrameLayout swipeLayout;

    @BindView(R.id.fragment_wear_frame_layout)
    FrameLayout frameLayout;
    private Handler handler;
    private ActivityFinishRunnable activityFinishRunnable;

    private static boolean screenToggle = false, mustLockDevice = false, wasScreenLocked = false;
    private boolean keyboardVisible = false, specialNotification = false;

    private static int screenMode;
    private static int screenBrightness = 999989;


    private SettingsManager settingsManager;

    private static final String SCREEN_BRIGHTNESS_MODE = "screen_brightness_mode";
    public static final String KEY = "key";
    public static final String MODE = "mode";
    public static final String MODE_ADD = "add";
    public static final String MODE_VIEW = "view";

    //from fragment
    LinearLayout replies_layout;
    TextView title;
    TextView time;
    TextView text;
    ImageView icon, iconBadge;
    ImageView image;
    ImageView picture;
    Button intentButton, deleteButton, muteButton, replyEditClose, replyEditSend;
    LinearLayout actionsLayout, actionReplyList;
    EditText replyEditText;
    //LinearLayout repliesLayout;
    NotificationData notificationData;
    ScrollView scrollView;

    private TextView delayedConfirmationViewTitle, delayedConfirmationViewBottom;
    private DelayedConfirmationView delayedConfirmationView;
    private LinearLayout repliesListView, repliesEditTextContainer, muteListView;

    private String key, mode, selectedAction, selectedPackageName, selectedReply, selectedSilenceTime;
    private Integer sbnId;
    private boolean enableInvertedTheme, disableDelay, colorIcons;
    public static boolean keyboardIsEnable = false;

    private Context mContext;
    private FragmentUtil util;

    private String action;
    private static final String ACTION_INTENT = "intent";
    private static final String ACTION_ACTION = "action";
    private static final String ACTION_REPLY = "reply";
    private static final String ACTION_DELETE = "del";
    private static final String ACTION_MUTE = "mute";

    private ButtonListener buttonListener = new ButtonListener();
    private SensorManager sm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getIntent().getStringExtra(KEY);
        mode = getIntent().getStringExtra(MODE);

        if (NotificationStore.getCustomNotification(key) == null) {
            Logger.error("onCreate: invalid key '" + key + "' - notification will not be shown");
            finish();
            return;
        }

        this.mContext = this;

        setContentView(R.layout.fragment_notification);

        ButterKnife.bind(this);

        sm = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        //Batching disabled because it doesn't work on any amazfit
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                sleepConstants.SAMPLING_PERIOD_US);

        swipeLayout.addCallback(new SwipeDismissFrameLayout.Callback() {
            @Override
            public void onDismissed(SwipeDismissFrameLayout layout) {
                finish();
            }
        });

        settingsManager = new SettingsManager(this);

        wasScreenLocked = DeviceUtil.isDeviceLocked(getBaseContext());
        if (mustLockDevice && screenToggle)
            wasScreenLocked = true;
        mustLockDevice = wasScreenLocked;

        setWindowFlags(true);

        //Load preferences
        final boolean disableNotificationsScreenOn = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_SCREENON,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_SCREENON);
        boolean disableNotificationReplies = settingsManager.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_REPLIES,
                Constants.PREF_DEFAULT_DISABLE_NOTIFICATIONS_REPLIES);
        boolean enableSoundCustomUI = settingsManager.getBoolean(Constants.PREF_NOTIFICATION_ENABLE_SOUND,
                Constants.PREF_DEFAULT_NOTIFICATION_ENABLE_SOUND);

        final boolean notificationHasHideReplies = NotificationStore.getHideReplies(key);
        final boolean notificationHasForceCustom = NotificationStore.getForceCustom(key);

        if (notificationHasForceCustom && notificationHasHideReplies)
            specialNotification = true;

        Logger.debug("NotificationWearActivity specialNotification: {}", specialNotification);

        //Do not activate screen if it is disabled in settings and screen was off or it was disabled previously
        if (disableNotificationsScreenOn && (wasScreenLocked || screenToggle)) {
            if (wasScreenLocked)
                mustLockDevice = true;
            if (screenToggle)
                mustLockDevice = false;
            setScreenOff();
        }

        onAttach();
        setupBtnListener();

        handler = new Handler();
        activityFinishRunnable = new ActivityFinishRunnable(this);
        startTimerFinish();

        if (SystemProperties.isVerge() && MODE_ADD.equals(mode) && enableSoundCustomUI) {
            MediaPlayer mp = new MediaPlayer();
            mp.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            Logger.debug("Set to use notification channel");

            File file = new File(Constants.CUSTOM_NOTIFICATION_SOUND);
            Uri sound;
            if (!file.exists()) {
                Logger.debug("File " + Constants.CUSTOM_NOTIFICATION_SOUND + " does not exist, using default notification sound");
                sound = Uri.parse(Constants.RES_PREFIX + R.raw.alerts_notification);
            } else {
                Logger.debug("File " + Constants.CUSTOM_NOTIFICATION_SOUND + " detected, will use it as notification sound");
                sound = Uri.fromFile(file);
            }
            try {
                mp.setDataSource(getApplicationContext(), sound);
                mp.prepare();
                Logger.debug("Play notification sound");
            } catch (IOException e) {
                Logger.error("Can't play notification sound");
            }
            mp.start();
        }

        Logger.info("NotificationWearActivity onCreate key: " + key + " | mode: " + mode
                + " | wasLckd: " + wasScreenLocked + " | mustLck: " + mustLockDevice
                + " | scrTg: " + screenToggle);
    }

    private void onAttach() {
        notificationData = NotificationStore.getCustomNotification(key);
        if (notificationData != null) {
            sbnId = NotificationStore.getSbnId(key);
            String[] actionTitles = notificationData.getActionTitles();
            String[] replyTitles = notificationData.getReplyTitles();

            System.out.println("Notification actions-----------------------");
            System.out.println(Arrays.toString(actionTitles));

            System.out.println("Notification reply-----------------------");
            System.out.println(Arrays.toString(replyTitles));
        } else {
            Logger.error("null notificationData, finishing…");
            finish();
        }

        Logger.debug("key: {} mode: {} notificationKey: {}", key, mode, sbnId);

        updateContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        buttonListener.stop();
        sm.unregisterListener(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        try {
            rootLayout.dispatchTouchEvent(event);

            if (screenToggle)
                setScreenOn();

            if (!keyboardVisible) {
                startTimerFinish();
            }
        } catch (IllegalArgumentException e) {
            Logger.error(e.toString());
        }
        return false;
    }

    public void startTimerFinish() {
        if (!mode.equals(MODE_VIEW)) {
            Logger.debug("NotificationWearActivity startTimerFinish");
            if (activityFinishRunnable != null)
                handler.removeCallbacks(activityFinishRunnable);
            int timeOutRelock = NotificationStore.getTimeoutRelock(key);
            if (timeOutRelock == 0)
                timeOutRelock = settingsManager.getInt(Constants.PREF_NOTIFICATION_SCREEN_TIMEOUT, Constants.PREF_DEFAULT_NOTIFICATION_SCREEN_TIMEOUT);
            handler.postDelayed(activityFinishRunnable, timeOutRelock);
        }
    }

    public void stopTimerFinish() {
        Logger.debug("NotificationWearActivity stopTimerFinish");
        if (activityFinishRunnable != null)
            handler.removeCallbacks(activityFinishRunnable);
    }

    public void setKeyboardVisible(boolean visible) {
        keyboardVisible = visible;
    }

    @Override
    public void finish() {
        if (activityFinishRunnable != null)
            handler.removeCallbacks(activityFinishRunnable);
        setWindowFlags(false);
        keyboardIsEnable = false;
        Logger.debug("keyboard NOT visible");
        super.finish();

        boolean flag = true;
        Logger.info("NotificationWearActivity finish key: " + key
                + " | scrT: " + screenToggle + " | mustLck: " + mustLockDevice);

        if (screenToggle) {
            flag = false;
            setScreenOn();
        }

        if (mustLockDevice) {
            mustLockDevice = false;
            screenToggle = false;
            if (flag) {
                final Handler mHandler = new Handler();
                mHandler.postDelayed(new Runnable() {
                    public void run() {
                        lock();
                    }
                }, 500);
            } else
                lock();
        } else if (wasScreenLocked)
            mustLockDevice = true;

//        if (specialNotification)
//            NotificationStore.removeCustomNotification(key, mContext); // Remove custom notification
    }

    private void lock() {
        if (!DeviceUtil.isDeviceLocked(mContext)) {
            DevicePolicyManager mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            if (mDPM != null) {
                try {
                    mDPM.lockNow();
                } catch (SecurityException ex) {
                    //Toast.makeText(this, getResources().getText(R.string.device_owner), Toast.LENGTH_LONG).show();
                    Logger.error("NotificationWearActivity SecurityException: " + ex.toString());
                }
            }
        }
    }

    private void setWindowFlags(boolean enable) {

        if (MODE_VIEW.equals(mode))
            return;

        final int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;

        if (enable) {
            getWindow().addFlags(flags);
        } else {
            getWindow().clearFlags(flags);
        }
    }

    private void setScreenOff() {
        setScreenModeOff(true);
    }

    private void setScreenOn() {
        setScreenModeOff(false);
    }

    private void setScreenModeOff(boolean mode) {

        WindowManager.LayoutParams params = getWindow().getAttributes();
        if (mode) {
            Logger.info("NotificationWearActivity setScreenModeOff true");
            screenMode = DeviceUtil.systemGetInt(mContext, SCREEN_BRIGHTNESS_MODE, 0);
            screenBrightness = DeviceUtil.systemGetInt(mContext, Settings.System.SCREEN_BRIGHTNESS, 0);
            //Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, SCREEN_BRIGHTNESS_MODE_MANUAL);
            //Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_OFF;
            getWindow().setAttributes(params);
        } else {
            if (screenBrightness != 999989) {
                Logger.info("NotificationWearActivity setScreenModeOff false | screenMode: " + screenMode);
                //Settings.System.putInt(mContext.getContentResolver(), SCREEN_BRIGHTNESS_MODE, screenMode);
                //Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, screenBrightness);
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
                getWindow().setAttributes(params);
            }
        }
        screenToggle = mode;
    }

    //from fragment
    private void updateContent() {
        try {
            util = new FragmentUtil(mContext);
            disableDelay = util.getDisableDelay();

            // Load preferences
            boolean disableNotificationText = util.getDisableNotificationText();
            final boolean notificationHasHideReplies = NotificationStore.getHideReplies(key);
            final boolean notificationHasForceCustom = NotificationStore.getForceCustom(key);
            enableInvertedTheme = util.getInvertedTheme();

            Logger.info("NotificationFragment updateContent key: {} dt: {} hr: {} fc: {}",
                    key, disableNotificationText, notificationHasHideReplies, notificationHasForceCustom);

            initView();
            setTheme();

            if (notificationHasHideReplies) {
                actionsLayout.setVisibility(View.GONE);
                intentButton.setVisibility(View.GONE);
            } else {
                initButtonsContainer();
                intentButton.setOnClickListener(view -> {
                    selectedPackageName = notificationData.getPackageName();
                    sendIntent(view);
                });
                loadReplies();
            }

            if (notificationHasForceCustom && notificationHasHideReplies) {

                muteButton.setVisibility(View.GONE);
                deleteButton.setVisibility(View.GONE);

            } else {
                //Delete related stuff
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Logger.debug("NotificationFragment updateContent: deleteButton clicked!");
                        muteListView.setVisibility(View.GONE);
                        repliesListView.setVisibility(View.GONE);
                        sendDeleteCommand(v);
                    }
                });

                // Mute related stuff
                muteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Logger.debug("NotificationFragment updateContent: muteButton clicked!");
                        if (muteListView.getVisibility() == View.VISIBLE) {
                            muteListView.setVisibility(View.GONE);
                        } else {
                            //Prepare the View for the animation
                            muteListView.setVisibility(View.VISIBLE);
                            repliesListView.setVisibility(View.GONE);
                            focusOnView(scrollView, muteButton);
                        }
                    }
                });
                loadMuteOptions();
            }

            populateNotificationIcon(icon, iconBadge, notificationData);

            title.setText(notificationData.getTitle());
            time.setText(notificationData.getTime());
            text.setText(notificationData.getText());

            if (hasPicture(notificationData)) {
                populateNotificationPicture(picture, notificationData);
            }

            if (disableNotificationText)
                hideContent();

            doVibration(notificationData.getVibration(), notificationData.getVibrationAmount());

        } catch (NullPointerException exception) {
            Logger.error(exception, exception.getMessage());
        }
    }

    private void initView() {
        scrollView = findViewById(R.id.fragment_custom_scrollview);

        title = findViewById(R.id.fragment_custom_notification_title);
        time = findViewById(R.id.fragment_custom_notification_time);
        text = findViewById(R.id.fragment_custom_notification_text);
        icon = findViewById(R.id.fragment_custom_notification_icon);
        iconBadge = findViewById(R.id.fragment_custom_notification_icon_badge);
        picture = findViewById(R.id.fragment_custom_notification_picture);
        image = findViewById(R.id.fragment_custom_notification_replies_image);

        delayedConfirmationViewTitle = findViewById(R.id.fragment_notification_delayedview_title);
        delayedConfirmationView = findViewById(R.id.fragment_notification_delayedview);
        delayedConfirmationViewBottom = findViewById(R.id.fragment_notification_delayedview_bottom);

        // Buttons
        intentButton = findViewById(R.id.fragment_notification_intent_button);
        deleteButton = findViewById(R.id.fragment_delete_button);
        muteButton = findViewById(R.id.fragment_notification_mute_button);

        // Replies view
        replies_layout = findViewById(R.id.fragment_custom_notification_replies_layout);
        actionsLayout = findViewById(R.id.fragment_actions_list);
        actionReplyList = findViewById(R.id.fragment_action_reply_list);
        repliesListView = findViewById(R.id.fragment_reply_list);
        repliesEditTextContainer = findViewById(R.id.fragment_notifications_replies_edittext_container);
        replyEditText = findViewById(R.id.fragment_notifications_replies_edittext);
        replyEditClose = findViewById(R.id.fragment_notifications_replies_edittext_button_close);
        replyEditSend = findViewById(R.id.fragment_notifications_replies_edittext_button_reply);

        // Mute view
        muteListView = findViewById(R.id.fragment_mute_list);

    }

    //#todo mediacontrol
    //https://github.com/Freeyourgadget/Gadgetbridge/blob/master/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/receivers/GBMusicControlReceiver.java
    private void initButtonsContainer() {
        String[] replyTitles = notificationData.getReplyTitles();
        String[] actionTitles = notificationData.getActionTitles();

        actionsLayout.removeAllViews();
        actionReplyList.removeAllViews();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FragmentUtil.getValueInDP(this, 48));
        layoutParams.setMargins(0, FragmentUtil.getValueInDP(this, 8), 0, 0);
        for (String mAction : actionTitles) {
            Button button = new Button(this);
            button.setLayoutParams(layoutParams);
            TypedValue outValue = new TypedValue();
            if (enableInvertedTheme) {
                getTheme().resolveAttribute(R.drawable.wear_button_rect_light_gray, outValue, true);
                button.setBackgroundDrawable(getResources().getDrawable(R.drawable.wear_button_rect_light_gray));
            } else {
                getTheme().resolveAttribute(R.drawable.wear_button_rect_gray, outValue, true);
                button.setBackgroundDrawable(getResources().getDrawable(R.drawable.wear_button_rect_gray));
            }
            button.setText(mAction);
            button.setTextColor(getResources().getColor(R.color.white));

            button.setOnClickListener(view -> {
                selectedAction = mAction;
                sendAction(view);
            });

            actionsLayout.addView(button);
        }
        for (String replyAction : replyTitles) {
            Button button = new Button(this);
            button.setLayoutParams(layoutParams);
            TypedValue outValue = new TypedValue();
            if (enableInvertedTheme) {
                getTheme().resolveAttribute(R.drawable.wear_button_rect_light_gray, outValue, true);
                button.setBackgroundDrawable(getResources().getDrawable(R.drawable.wear_button_rect_light_gray));
            } else {
                getTheme().resolveAttribute(R.drawable.wear_button_rect_gray, outValue, true);
                button.setBackgroundDrawable(getResources().getDrawable(R.drawable.wear_button_rect_gray));
            }
            button.setText(replyAction);
            button.setTextColor(getResources().getColor(R.color.white));

            //Replies related stuff
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedAction = replyAction;
                    Logger.debug("NotificationFragment updateContent: replyButton clicked!");
                    if (repliesListView.getVisibility() == View.VISIBLE) {
                        repliesListView.setVisibility(View.GONE);
                        focusOnViewBottom(scrollView, actionReplyList);
                    } else {
                        // Prepare the View for the animation
                        repliesListView.setVisibility(View.VISIBLE);
                        muteListView.setVisibility(View.GONE);
                        focusOnView(scrollView, actionReplyList);
                    }
                }
            });
            actionReplyList.addView(button);
        }
    }

    private void setTheme() {
        // Adjust minimum height based on device (so that reply button stays at the bottom of screen)
        if (SystemProperties.isVerge()) {
            int px = FragmentUtil.getValueInDP(this, 72);
            replies_layout.setMinimumHeight(px);
        } else if (SystemProperties.isStratos3()) {
            int px = FragmentUtil.getValueInDP(this, 72);
            replies_layout.setMinimumHeight(px);
        }

        // Set theme and font size
        // Logger.debug("NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            frameLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            delayedConfirmationViewTitle.setTextColor(getResources().getColor(R.color.black));
            delayedConfirmationViewBottom.setTextColor(getResources().getColor(R.color.black));
        } else {
            frameLayout.setBackgroundColor(getResources().getColor(R.color.black));
        }

        time.setTextSize(util.getFontTitleSizeSP());
        title.setTextSize(util.getFontTitleSizeSP());
        text.setTextSize(util.getFontSizeSP());

        // Code changed to identify special languages (eg Hebrew)
        util.setFontLocale(title, notificationData.getTitle());
        util.setFontLocale(text, notificationData.getText());
    }

    private void hideContent() {
        text.setVisibility(View.GONE);
        picture.setVisibility(View.GONE);
        image.setVisibility(View.VISIBLE);
        if (enableInvertedTheme)
            image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_black_48));
        else
            image.setImageDrawable(getResources().getDrawable(R.drawable.outline_screen_lock_portrait_white_48));
    }

    private final void focusOnView(final ScrollView scroll, final View view) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int vPosition = view.getTop();
                scroll.smoothScrollTo(0, vPosition);
            }
        });
    }

    private final void focusOnViewBottom(final ScrollView scroll, final View view) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int height = metrics.heightPixels;
                //int width = metrics.widthPixels;
                int vPosition = view.getTop() + view.getHeight() - height;
                scroll.smoothScrollTo(0, vPosition);
            }
        });
    }

    private boolean hasPicture(NotificationData notificationData) {
        if (notificationData == null)
            return false;
        byte[] pictureData = notificationData.getPicture();
        return (pictureData != null) && (pictureData.length > 0);
    }

    private void populateNotificationIcon(ImageView iconView, ImageView iconAppView, NotificationData notificationData) {
        Logger.trace("hasPicture: {}", hasPicture(notificationData));
        try {
            byte[] largeIconData = notificationData.getLargeIcon();
            if ((largeIconData != null) && (largeIconData.length > 0)) {
                iconView.setImageBitmap(ImageUtils.bytes2Bitmap(largeIconData));
                setIconBadge(iconAppView);
            } else {
                setIconBadge(iconView);
                iconAppView.setVisibility(View.GONE);
            }
        } catch (Exception exception) {
            Logger.debug(exception, exception.getMessage());
        }
    }

    private void setIconBadge(ImageView iconView) {
        byte[] iconData = notificationData.getIcon();
        Bitmap bitmap = ImageUtils.bytes2Bitmap(iconData);
        iconView.setImageBitmap(bitmap);
    }

    private void populateNotificationPicture(ImageView pictureView, NotificationData notificationData) {
        Logger.trace("hasPicture: {}", hasPicture(notificationData));
        try {
            if (hasPicture(notificationData)) {
                byte[] pictureData = notificationData.getPicture();
                pictureView.setImageBitmap(ImageUtils.bytes2Bitmap(pictureData));
                pictureView.setVisibility(View.VISIBLE);
            }
        } catch (Exception exception) {
            Logger.debug(exception, exception.getMessage());
        }
    }

    @SuppressLint("CheckResult")
    public void loadReplies() {
        Logger.info("NotificationFragment loadReplies");
        List<Reply> replyList = util.listReplies();
        final LayoutInflater inflater = LayoutInflater.from(this.mContext);
        for (final Reply reply : replyList) {
            final View row = inflater.inflate(R.layout.row_reply, repliesListView, false);
            EmojiTextView replyView = row.findViewById(R.id.row_reply_text);
            replyView.setText(reply.getValue());
            // Set language to hebrew or arabic if needed
            util.setFontLocale(replyView, reply.getValue());
            if (enableInvertedTheme) {
                replyView.setTextColor(getResources().getColor(R.color.black));
                replyView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.send, 0, 0, 0);
            }
            replyView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedReply = reply.getValue();
                    sendReply(view);
                    Logger.debug("NotificationFragment replyView OnClick: " + selectedReply);
                }
            });
            // set item content in view
            repliesListView.addView(row);
        }
        final View row = inflater.inflate(R.layout.row_reply, repliesListView, false);
        ConstraintLayout replyView = row.findViewById(R.id.row_reply_view);
        EmojiTextView replyTextView = row.findViewById(R.id.row_reply_text);
        replyTextView.setText(getResources().getString(R.string.keyboard));
        if (enableInvertedTheme) {
            replyTextView.setTextColor(getResources().getColor(R.color.black));
            //replyView.setCompoundDrawables(getResources().getDrawable(R.drawable.send), null, null, null);
        }
        //replyView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        replyView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Logger.debug("NotificationFragment replyView OnClick: KEYBOARD");
                scrollView.setVisibility(View.GONE);
                repliesEditTextContainer.setVisibility(View.VISIBLE);
                stopTimerFinish();
                setKeyboardVisible(true);
                keyboardIsEnable = true;
                Logger.debug("keyboard IS visible");

                replyEditSend.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedReply = replyEditText.getText().toString();
                        repliesEditTextContainer.setVisibility(View.GONE);
                        sendReply(v);
                    }
                });
                //Cancel button
                replyEditClose.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        scrollView.setVisibility(View.VISIBLE);
                        repliesEditTextContainer.setVisibility(View.GONE);
                        setKeyboardVisible(false);
                        keyboardIsEnable = false;
                        Logger.debug("keyboard NOT visible");
                        startTimerFinish();
                    }
                });
            }
        });
        // set item content in view
        repliesListView.addView(row);
    }

    private void loadMuteOptions() {

        List<Integer> silenceList = new ArrayList<>();
        silenceList.add(5);
        silenceList.add(15);
        silenceList.add(30);
        silenceList.add(60);

        LayoutInflater inflater = LayoutInflater.from(this.mContext);

        //Add one View for each item in SilenceList
        for (final Integer silence : silenceList) {
            final View row = inflater.inflate(R.layout.row_mute, muteListView, false);
            EmojiTextView muteView = row.findViewById(R.id.row_mute_value);
            muteView.setText(String.format("%s " + getString(R.string.minutes), silence.toString()));
            if (enableInvertedTheme) {
                muteView.setTextColor(getResources().getColor(R.color.black));
            }
            muteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    selectedPackageName = notificationData.getPackageName();
                    selectedSilenceTime = silence.toString();
                    sendMuteCommand(view);
                    Logger.debug("NotificationFragment loadMuteOptions muteView OnClick: " + selectedSilenceTime);
                }
            });
            // set item content in view
            muteListView.addView(row);
        }

        //Create a Item for Muting App for One Day
        final View row_day = inflater.inflate(R.layout.row_mute, muteListView, false);
        EmojiTextView muteView = row_day.findViewById(R.id.row_mute_value);
        muteView.setText(getString(R.string.one_day));
        if (enableInvertedTheme) {
            muteView.setTextColor(getResources().getColor(R.color.black));
        }
        muteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPackageName = notificationData.getPackageName();
                selectedSilenceTime = "1440";
                sendMuteCommand(view);
                Logger.debug("NotificationFragment loadMuteOptions muteView OnClick: " + selectedSilenceTime);
            }
        });
        muteListView.addView(row_day);


        //Create a Item for BLOCKING APP (Removes it From List of Apps)
        final View row_block = inflater.inflate(R.layout.row_mute, muteListView, false);
        muteView = row_block.findViewById(R.id.row_mute_value);
        muteView.setText(R.string.block_app);
        if (enableInvertedTheme)
            muteView.setTextColor(getResources().getColor(R.color.dark_red));
        else
            muteView.setTextColor(getResources().getColor(R.color.red_a200));
        muteView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPackageName = notificationData.getPackageName();
                selectedSilenceTime = Constants.BLOCK_APP;
                sendMuteCommand(view);
                Logger.debug("NotificationFragment loadMuteOptions muteView OnClick: " + selectedSilenceTime);
            }
        });
        muteListView.addView(row_block);
    }


    private void sendDeleteCommand(View v) {
        sendCommand(ACTION_DELETE, v);
    }

    private void sendReply(View v) {
        setKeyboardVisible(false);
        keyboardIsEnable = false;
        Logger.debug("keyboard NOT visible");
        sendCommand(ACTION_REPLY, v);
    }

    private void sendAction(View v) {
        sendCommand(ACTION_ACTION, v);
    }

    private void sendIntent(View v) {
        sendCommand(ACTION_INTENT, v);
    }

    private void sendMuteCommand(View v) {
        sendCommand(ACTION_MUTE, v);
    }

    private void sendCommand(String command, View v) {
        action = command;
        String confirmationMessage;
        switch (action) {
            case ACTION_DELETE:
                delayedConfirmationView.setTotalTimeMs(1500);
                confirmationMessage = getString(R.string.removing);
                break;
            case ACTION_MUTE:
                delayedConfirmationView.setTotalTimeMs(3000);
                confirmationMessage = getString(R.string.muting);
                break;
            case ACTION_REPLY:
                delayedConfirmationView.setTotalTimeMs(3000);
                confirmationMessage = getString(R.string.sending_reply);
                break;
            case ACTION_ACTION:
                delayedConfirmationView.setTotalTimeMs(3000);
                confirmationMessage = getString(R.string.sending_action);
                break;
            case ACTION_INTENT:
                delayedConfirmationView.setTotalTimeMs(3000);
                confirmationMessage = getString(R.string.sending_intent);
                break;
            default:
                return;
        }
        stopTimerFinish();
        if (disableDelay) {
            Logger.info("NotificationFragment sendCommand without delay : command '" + command + "'");
            onTimerFinished(v);
        } else {
            Logger.debug("NotificationFragment sendCommand with delay : command '" + command + "'");
            util.setParamMargins(0, 24, 0, 4);
            showDelayed(confirmationMessage);
            Logger.info("NotificationFragment sendSilenceCommand isPressed: " + delayedConfirmationView.isPressed());
        }
    }

    @Override
    public void onTimerSelected(View v) {
        action = "";
        v.setPressed(true);
        delayedConfirmationView.reset();
        // Prevent onTimerFinished from being heard.
        ((DelayedConfirmationView) v).setListener(null);
        startTimerFinish();
        hideDelayed();
        Logger.info("NotificationFragment onTimerSelected isPressed: " + v.isPressed());
    }

    private void showDelayed(String text) {
        scrollView.setVisibility(View.GONE);
        repliesEditTextContainer.setVisibility(View.GONE);
        delayedConfirmationViewTitle.setText(text);
        delayedConfirmationViewTitle.setVisibility(View.VISIBLE);
        delayedConfirmationViewBottom.setVisibility(View.VISIBLE);
        delayedConfirmationView.setVisibility(View.VISIBLE);
        delayedConfirmationView.setPressed(false);
        delayedConfirmationView.start();
        delayedConfirmationView.setListener(this);

    }

    private void hideDelayed() {
        scrollView.setVisibility(View.VISIBLE);
        delayedConfirmationView.setVisibility(View.GONE);
        delayedConfirmationViewTitle.setVisibility(View.GONE);
        delayedConfirmationViewBottom.setVisibility(View.GONE);
    }

    private void doVibration(int duration, int vibrationAmount) {
        if (duration > 0 && NotificationWearActivity.MODE_ADD.equals(mode)) {
            Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
            if (vibrationAmount == 0) vibrationAmount = 1;
            long[] pattern = new long[vibrationAmount * 2];
            if (pattern.length > 0) {
                pattern[0] = 0; //first delay
                for (int i = 1; i < vibrationAmount * 2; i++) {
                    pattern[i] = duration;
                }
                try {
                    if (vibrator != null) {
                        vibrator.vibrate(pattern, -1);
                    }
                } catch (RuntimeException ex) {
                    //Logger.error(ex, ex.getMessage());
                }
            }
        }
    }

    @Override
    public void onTimerFinished(View v) {
        Logger.info("NotificationFragment onTimerFinished isPressed: " + v.isPressed());

        if (v instanceof DelayedConfirmationView)
            ((DelayedConfirmationView) v).setListener(null);

        String confirmationMessage;

        switch (action) {
            case ACTION_DELETE:
                confirmationMessage = getString(R.string.deleted) + "!";
                break;
            case ACTION_MUTE:
                confirmationMessage = getString(R.string.muted) + "!";
                break;
            case ACTION_REPLY:
                confirmationMessage = getString(R.string.reply_sent) + "!";
                break;
            case ACTION_ACTION:
                confirmationMessage = getString(R.string.action_sent) + "!";
                break;
            case ACTION_INTENT:
                confirmationMessage = getString(R.string.intent_sent) + "!";
                break;
            default:
                return;
        }
        Logger.info("NotificationFragment onTimerFinished action :" + action);

        Intent intent = new Intent(mContext, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, confirmationMessage);
        startActivity(intent);

        //NotificationStore.removeCustomNotification(key, mContext); // Remove custom notification
        if (NotificationWearActivity.MODE_VIEW.equals(mode))
            WearNotificationsFragment.getInstance().loadNotifications();

        switch (action) {
            case ACTION_DELETE:
                NotificationStore.removeCustomNotification(key, mContext); // Remove custom notification
                break;
            case ACTION_MUTE:
                if (selectedPackageName != null)
                    EventBus.getDefault().post(new SilenceApplicationEvent(selectedPackageName, selectedSilenceTime));
                else
                    Logger.error("cannot silence null key");
                break;
            case ACTION_REPLY:
                if (sbnId != null)
                    EventBus.getDefault().post(new ReplyNotificationEvent(sbnId, selectedAction, selectedReply));
                else
                    Logger.error("cannot reply null key");
                break;
            case ACTION_ACTION:
                if (sbnId != null)
                    EventBus.getDefault().post(new ActionNotificationEvent(sbnId, selectedAction));
                else
                    Logger.error("cannot action null key");
                break;
            case ACTION_INTENT:
                if (sbnId != null)
                    EventBus.getDefault().post(new IntentNotificationEvent(sbnId, selectedPackageName));
                else
                    Logger.error("cannot intent null key");
                break;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 1500);
    }

    private void setupBtnListener() {
        buttonListener.start(this, keyEvent -> {
            if (SystemProperties.isStratos3())
                switch (keyEvent.getCode()) {
                    case ButtonListener.S3_KEY_MIDDLE_UP:
                        startTimerFinish();
                        scrollView.smoothScrollTo(scrollView.getScrollX(), scrollView.getScrollY() - 100);
                        break;
                    case ButtonListener.S3_KEY_MIDDLE_DOWN:
                        startTimerFinish();
                        scrollView.smoothScrollTo(scrollView.getScrollX(), scrollView.getScrollY() + 100);
                        break;
                }
        });
    }


    private long oldTimestamp = 0;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long timestamp = (sensorEvent.timestamp / 1_000_000L);

        float x = sensorEvent.values[0];
        float y = sensorEvent.values[1];
        float z = sensorEvent.values[2];
        float max_raw = (float) sqrt((x * x) + (y * y) + (z * z));
        //Logger.debug("max_raw: {}", max_raw);
        if (max_raw > 20) {
            if (timestamp - oldTimestamp > 300) {
                Logger.debug("max_raw: triggered ({})", max_raw);
                startTimerFinish();
                scrollView.smoothScrollTo(scrollView.getScrollX(), scrollView.getScrollY() + 200);
                oldTimestamp = timestamp;
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
}
