package com.amazmod.service.ui.fragments;

import static android.content.Context.VIBRATOR_SERVICE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.DelayedConfirmationView;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
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
import com.amazmod.service.support.NotificationStore;
import com.amazmod.service.ui.NotificationWearActivity;
import com.amazmod.service.util.FragmentUtil;
import com.amazmod.service.util.SystemProperties;

import org.greenrobot.eventbus.EventBus;
import org.tinylog.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import amazmod.com.models.Reply;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.util.ImageUtils;

public class NotificationFragment extends Fragment implements DelayedConfirmationView.DelayedConfirmationListener {

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
    BoxInsetLayout rootLayout;
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

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mContext = activity.getBaseContext();
        Logger.info("NotificationFragment onAttach context: " + mContext);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        key = getArguments().getString(NotificationWearActivity.KEY);
        mode = getArguments().getString(NotificationWearActivity.MODE);

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
            getActivity().finish();
        }

        Logger.debug("key: {} mode: {} notificationKey: {}", key, mode, sbnId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        Logger.info("NotificationFragment onCreateView");

        return inflater.inflate(R.layout.fragment_notification, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Logger.info("NotificationFragment onViewCreated");

        updateContent();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

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

            doVibration(notificationData.getVibration());

        } catch (NullPointerException exception) {
            Logger.error(exception, exception.getMessage());
        }
    }

    private void initView() {
        rootLayout = getActivity().findViewById(R.id.fragment_custom_root_layout);
        scrollView = getActivity().findViewById(R.id.fragment_custom_scrollview);

        title = getActivity().findViewById(R.id.fragment_custom_notification_title);
        time = getActivity().findViewById(R.id.fragment_custom_notification_time);
        text = getActivity().findViewById(R.id.fragment_custom_notification_text);
        icon = getActivity().findViewById(R.id.fragment_custom_notification_icon);
        iconBadge = getActivity().findViewById(R.id.fragment_custom_notification_icon_badge);
        picture = getActivity().findViewById(R.id.fragment_custom_notification_picture);
        image = getActivity().findViewById(R.id.fragment_custom_notification_replies_image);

        delayedConfirmationViewTitle = getActivity().findViewById(R.id.fragment_notification_delayedview_title);
        delayedConfirmationView = getActivity().findViewById(R.id.fragment_notification_delayedview);
        delayedConfirmationViewBottom = getActivity().findViewById(R.id.fragment_notification_delayedview_bottom);

        // Buttons
        intentButton = getActivity().findViewById(R.id.fragment_notification_intent_button);
        deleteButton = getActivity().findViewById(R.id.fragment_delete_button);
        muteButton = getActivity().findViewById(R.id.fragment_notification_mute_button);

        // Replies view
        replies_layout = getActivity().findViewById(R.id.fragment_custom_notification_replies_layout);
        actionsLayout = getActivity().findViewById(R.id.fragment_actions_list);
        actionReplyList = getActivity().findViewById(R.id.fragment_action_reply_list);
        repliesListView = getActivity().findViewById(R.id.fragment_reply_list);
        repliesEditTextContainer = getActivity().findViewById(R.id.fragment_notifications_replies_edittext_container);
        replyEditText = getActivity().findViewById(R.id.fragment_notifications_replies_edittext);
        replyEditClose = getActivity().findViewById(R.id.fragment_notifications_replies_edittext_button_close);
        replyEditSend = getActivity().findViewById(R.id.fragment_notifications_replies_edittext_button_reply);

        // Mute view
        muteListView = getActivity().findViewById(R.id.fragment_mute_list);

    }

    //#todo mediacontrol
    //https://github.com/Freeyourgadget/Gadgetbridge/blob/master/app/src/main/java/nodomain/freeyourgadget/gadgetbridge/service/receivers/GBMusicControlReceiver.java
    private void initButtonsContainer() {
        String[] replyTitles = notificationData.getReplyTitles();
        String[] actionTitles = notificationData.getActionTitles();

        actionsLayout.removeAllViews();
        actionReplyList.removeAllViews();
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, FragmentUtil.getValueInDP(getActivity(), 48));
        layoutParams.setMargins(0, FragmentUtil.getValueInDP(getActivity(), 8), 0, 0);
        for (String mAction : actionTitles) {
            Button button = new Button(getActivity());
            button.setLayoutParams(layoutParams);
            TypedValue outValue = new TypedValue();
            if (enableInvertedTheme) {
                getActivity().getTheme().resolveAttribute(R.drawable.wear_button_rect_light_gray, outValue, true);
                button.setBackgroundDrawable(getResources().getDrawable(R.drawable.wear_button_rect_light_gray));
            } else {
                getActivity().getTheme().resolveAttribute(R.drawable.wear_button_rect_gray, outValue, true);
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
            Button button = new Button(getActivity());
            button.setLayoutParams(layoutParams);
            TypedValue outValue = new TypedValue();
            if (enableInvertedTheme) {
                getActivity().getTheme().resolveAttribute(R.drawable.wear_button_rect_light_gray, outValue, true);
                button.setBackgroundDrawable(getResources().getDrawable(R.drawable.wear_button_rect_light_gray));
            } else {
                getActivity().getTheme().resolveAttribute(R.drawable.wear_button_rect_gray, outValue, true);
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
            int px = FragmentUtil.getValueInDP(getActivity(), 72);
            replies_layout.setMinimumHeight(px);
        } else if (SystemProperties.isStratos3()) {
            int px = FragmentUtil.getValueInDP(getActivity(), 72);
            replies_layout.setMinimumHeight(px);
        }

        // Set theme and font size
        // Logger.debug("NotificationActivity enableInvertedTheme: " + enableInvertedTheme + " / fontSize: " + fontSize);
        if (enableInvertedTheme) {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.white));
            time.setTextColor(getResources().getColor(R.color.black));
            title.setTextColor(getResources().getColor(R.color.black));
            text.setTextColor(getResources().getColor(R.color.black));
            delayedConfirmationViewTitle.setTextColor(getResources().getColor(R.color.black));
            delayedConfirmationViewBottom.setTextColor(getResources().getColor(R.color.black));
        } else {
            rootLayout.setBackgroundColor(getResources().getColor(R.color.black));
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
                getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
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
        final LayoutInflater inflater = LayoutInflater.from(NotificationFragment.this.mContext);
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
                ((NotificationWearActivity) getActivity()).stopTimerFinish();
                ((NotificationWearActivity) getActivity()).setKeyboardVisible(true);
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
                        ((NotificationWearActivity) getActivity()).setKeyboardVisible(false);
                        keyboardIsEnable = false;
                        Logger.debug("keyboard NOT visible");
                        ((NotificationWearActivity) getActivity()).startTimerFinish();
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

        LayoutInflater inflater = LayoutInflater.from(NotificationFragment.this.mContext);

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
        ((NotificationWearActivity) getActivity()).setKeyboardVisible(false);
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
        ((NotificationWearActivity) getActivity()).stopTimerFinish();
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
        ((NotificationWearActivity) getActivity()).startTimerFinish();
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
        getActivity().finish();
    }

    private void doVibration(int duration) {
        if (duration > 0 && NotificationWearActivity.MODE_ADD.equals(mode)) {
            Vibrator vibrator = (Vibrator) mContext.getSystemService(VIBRATOR_SERVICE);
            try {
                if (vibrator != null) {
                    vibrator.vibrate(duration);
                }
            } catch (RuntimeException ex) {
                Logger.error(ex, ex.getMessage());
            }
        }
    }

    public static NotificationFragment newInstance(String key, String mode) {

        Logger.info("NotificationFragment newInstance key: " + key);
        NotificationFragment myFragment = new NotificationFragment();
        Bundle bundle = new Bundle();
        bundle.putString(NotificationWearActivity.KEY, key);
        bundle.putString(NotificationWearActivity.MODE, mode);
        myFragment.setArguments(bundle);

        return myFragment;
    }
}
