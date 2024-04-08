package com.edotassi.amazmod.notification.factory;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;

import com.edotassi.amazmod.AmazModApplication;
import com.edotassi.amazmod.R;
import com.edotassi.amazmod.util.ActionsHolder;
import com.edotassi.amazmod.util.FilesUtil;
import com.edotassi.amazmod.util.PendingIntentHolder;
import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import amazmod.com.transport.Constants;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.util.ImageUtils;

public class NotificationFactory {
    public static NotificationData fromStatusBarNotification(Context context, StatusBarNotification statusBarNotification) {

        NotificationData notificationData = new NotificationData();
        Notification notification = statusBarNotification.getNotification();
        Bundle bundle = notification.extras;
        String text = "", title = "";

        Logger.trace("notification key: {}", statusBarNotification.getKey());

        //Notification time
        //Calendar c = Calendar.getInstance();
        //c.setTimeInMillis(notification.when);
        //String notificationTime = c.get(Calendar.HOUR_OF_DAY) + ":" + c.get(Calendar.MINUTE);
        String notificationTime = DateFormat.getTimeInstance(DateFormat.SHORT, AmazModApplication.defaultLocale).format(notification.when);

        //EXTRA_TITLE and EXTRA_TEXT are usually CharSequence and not regular Strings...
        CharSequence bigTitle = bundle.getCharSequence(Notification.EXTRA_TITLE);
        if (bigTitle != null) {
            title = bigTitle.toString();
        } else try {
            title = bundle.getString(Notification.EXTRA_TITLE);
        } catch (ClassCastException e) {
            Logger.debug(e, "NotificationFactory exception: " + e.toString() + " title: " + title);
        }

        CharSequence bigText = bundle.getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null) {
            text = bigText.toString();
        }

        // Use EXTRA_TEXT_LINES instead, if it exists
        CharSequence[] lines = bundle.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        if ((lines != null) && (lines.length > 0)) {
            text += "\n*Extra lines:\n" + lines[Math.min(lines.length - 1, 0)].toString();
            Logger.debug("NotificationFactory EXTRA_TEXT_LINES exists");
        }

        // Maybe use android.bigText instead?
        if (bundle.getCharSequence(Notification.EXTRA_BIG_TEXT) != null) {
            try {
                text = bundle.getCharSequence(Notification.EXTRA_BIG_TEXT).toString();
                Logger.debug("NotificationFactory EXTRA_BIG_TEXT exists");
            } catch (NullPointerException e) {
                Logger.debug(e, "NotificationFactory exception: " + e.toString() + " text: " + text);
            }
        }

        extractImagesFromNotification(context, statusBarNotification, notificationData);

        notificationData.setId(statusBarNotification.getId());
        notificationData.setKey(statusBarNotification.getKey());
        notificationData.setPackageName(statusBarNotification.getPackageName());
        notificationData.setTitle(title);
        notificationData.setText(text);
        notificationData.setTime(notificationTime);
        notificationData.setForceCustom(false);
        notificationData.setHideReplies(false);
        notificationData.setHideButtons(false);
        extractActions(notificationData, statusBarNotification);

        return notificationData;
    }

    private static void extractActions(NotificationData notificationData, StatusBarNotification statusBarNotification) {
        Notification notification = statusBarNotification.getNotification();
        List<String> actionsTitles = new ArrayList<>();
        List<String> replyTitles = new ArrayList<>();
        if (notification.actions != null) {
            for (int i = 0; i < notification.actions.length; i++) {
                Notification.Action action = notification.actions[i];
                if (action.getRemoteInputs() != null && action.getRemoteInputs().length != 0) {
                    replyTitles.add(String.valueOf(action.title));
                } else {
                    actionsTitles.add(String.valueOf(action.title));
                }
            }
        }
        notificationData.setActionTitles(actionsTitles.toArray(new String[0]));
        notificationData.setReplyTitles(replyTitles.toArray(new String[0]));

        ActionsHolder.save(statusBarNotification.getId(), notification.actions);
        PendingIntentHolder.save(statusBarNotification.getId(), notification.contentIntent);
    }

    private static void extractImagesFromNotification(Context context, StatusBarNotification statusBarNotification, NotificationData notificationData) {
        //Small Icon
        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_COLORED_ICON, Constants.PREF_NOTIFICATIONS_COLORED_ICON_DEFAULT)) {
            Drawable appIcon = getAppIcon(statusBarNotification.getPackageName(), context);
            byte[] smallIcon = ImageUtils.bitmap2bytesWebp(ImageUtils.drawableToBitmap(appIcon), ImageUtils.smallIconQuality);
            notificationData.setIcon(smallIcon);
            Logger.debug("Small icon size WebP: " + FilesUtil.formatBytes(smallIcon.length));
        } else {
            try {
                if (statusBarNotification.getNotification().getSmallIcon() != null
                        && statusBarNotification.getNotification().getSmallIcon().loadDrawable(context) != null) {
                    Bitmap bm = ImageUtils.drawableToBitmap(statusBarNotification.getNotification().getSmallIcon().loadDrawable(context));
                    byte[] smallIcon = ImageUtils.bitmap2bytesWebp(bm, ImageUtils.smallIconQuality);
                    notificationData.setIcon(smallIcon);
                    Logger.debug("Small icon size WebP: " + FilesUtil.formatBytes(smallIcon.length));
                }
            } catch (Resources.NotFoundException e) {
                e.printStackTrace();
            }
        }

        //Large Icon
        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_LARGE_ICON, Constants.PREF_NOTIFICATIONS_LARGE_ICON_DEFAULT)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (statusBarNotification.getNotification().getLargeIcon() != null) {
                    byte[] largeIcon = ImageUtils.bitmap2bytesWebp(ImageUtils.drawableToBitmap(statusBarNotification.getNotification().getLargeIcon().loadDrawable(context)), ImageUtils.largeIconQuality);
                    notificationData.setLargeIcon(largeIcon);
                    Logger.debug("Large icon size WebP: " + FilesUtil.formatBytes(largeIcon.length));
                }
            } else {
                if (statusBarNotification.getNotification().largeIcon != null) {
                    byte[] largeIcon = ImageUtils.bitmap2bytesWebp(statusBarNotification.getNotification().largeIcon, ImageUtils.largeIconQuality);
                    notificationData.setLargeIcon(largeIcon);
                    Logger.debug("Large icon size WebP: " + FilesUtil.formatBytes(largeIcon.length));
                }
            }
        }

        //Big Picture
        if (Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_IMAGES, Constants.PREF_NOTIFICATIONS_IMAGES_DEFAULT)) {
            Bundle sbnBundle = statusBarNotification.getNotification().extras;
            if (sbnBundle.get(Notification.EXTRA_PICTURE) != null) {
                byte[] bigPicture = ImageUtils.bitmap2bytesWebp((Bitmap) sbnBundle.get(Notification.EXTRA_PICTURE), ImageUtils.bigPictureQuality);
                notificationData.setPicture(bigPicture);
                Logger.debug("Picture size WebP: " + FilesUtil.formatBytes(bigPicture.length));
            }
        }
    }

    public static Drawable getAppIcon(String packageName, Context context) {
        Drawable appIcon = null;
        try {
            appIcon = context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return appIcon;
    }

    public static NotificationData getMapNotification(Context context, StatusBarNotification statusBarNotification) {

        Logger.debug("getMapNotification package: {} key: {}", statusBarNotification.getPackageName(), statusBarNotification.getKey());

        NotificationData notificationData = null;

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationData = fromStatusBarNotification(context, statusBarNotification);

            RemoteViews rmv = getContentView(context, statusBarNotification.getNotification());
            RemoteViews brmv = getBigContentView(context, statusBarNotification.getNotification());

            if (rmv == null) {
                rmv = brmv;
                Logger.debug("using BigContentView");
            } else {
                Logger.debug("using ContentView");
            }

            if (rmv != null) {

                //Get text from RemoteView using reflection
                List<String> txt = extractText(rmv);
                if ((txt.size() > 0) && (!(txt.get(0).isEmpty()))) {

                    //Get navigation icon from a child View drawn on Canvas
                    try {
                        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
                        View layout = inflater.inflate(R.layout.nav_layout, null);
                        ViewGroup frame = layout.findViewById(R.id.layout_navi);
                        frame.removeAllViews();
                        View newView = rmv.apply(context, frame);
                        frame.addView(newView);
                        View viewImage = ((ViewGroup) newView).getChildAt(0);
                        //View outerLayout = ((ViewGroup) newView).getChildAt(1);
                        viewImage.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                        Bitmap bitmap = Bitmap.createBitmap(viewImage.getMeasuredWidth(), viewImage.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        viewImage.layout(0, 0, viewImage.getMeasuredWidth(), viewImage.getMeasuredHeight());
                        viewImage.draw(canvas);
                        bitmap = Bitmap.createScaledBitmap(bitmap, 48, 48, true);

                        notificationData.setIcon(ImageUtils.bitmap2bytesWebp(bitmap, ImageUtils.smallIconQuality));
                    } catch (Exception e) {
                        notificationData.setIcon(new byte[]{});
                        Logger.error(e, "failed to get bitmap with exception: {}", e.getMessage());
                    }

                    notificationData.setTitle(txt.get(0));
                    if (txt.size() > 1)
                        notificationData.setText(txt.get(1));
                    else
                        notificationData.setText("");
                    notificationData.setHideReplies(true);
                    notificationData.setHideButtons(false);
                    notificationData.setForceCustom(true);
                }
                return notificationData;

            } else {
                Logger.warn("null remoteView");
                return null;
            }

        } else {
            notificationData = fromStatusBarNotification(context, statusBarNotification);
            notificationData.setHideReplies(true);
            notificationData.setHideButtons(false);
            notificationData.setForceCustom(true);
            return notificationData;
        }
    }

    private static List<String> extractText(RemoteViews views) {
        // Use reflection to examine the m_actions member of the given RemoteViews object.
        List<String> text = new ArrayList<>();
        try {
            Field field = views.getClass().getDeclaredField("mActions");
            field.setAccessible(true);
            //int counter = 0;

            @SuppressWarnings("unchecked")
            ArrayList<Parcelable> actions = (ArrayList<Parcelable>) field.get(views);

            // Find the setText() reflection actions
            for (Parcelable p : actions) {
                Parcel parcel = Parcel.obtain();
                p.writeToParcel(parcel, 0);
                parcel.setDataPosition(0);

                // The tag tells which type of action it is (2 is ReflectionAction, from the source)
                int tag = parcel.readInt();
                if (tag != 2) continue;

                // View ID
                parcel.readInt();

                String methodName = parcel.readString();
                if (methodName == null)
                    continue;
                    // Save strings
                else {

                    if (methodName.equals("setText")) {
                        // Parameter type (10 = Character Sequence)
                        parcel.readInt();

                        // Store the actual string
                        String t = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel).toString().trim();
                        text.add(t);
                        //Logger.debug("NotificationService extractText " + counter + " t: " + t);
                        //counter++;
                    }
                }
                parcel.recycle();
            }
        }
        // It's not usually good style to do this, but then again, neither is the use of reflection...
        catch (Exception e) {
            Logger.error(e, "extractText exception: {}", e.getMessage());
            text.add("ERROR");
        }
        return text;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private static RemoteViews getBigContentView(Context context, Notification notification) {
        if (notification.bigContentView != null)
            return notification.bigContentView;
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return Notification.Builder.recoverBuilder(context, notification).createBigContentView();
        else
            return null;
    }

    private static RemoteViews getContentView(Context context, Notification notification) {
        if (notification.contentView != null)
            return notification.contentView;
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return Notification.Builder.recoverBuilder(context, notification).createContentView();
        else
            return null;
    }

}
