package com.edotassi.amazmod.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.collection.ArrayMap;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.edotassi.amazmod.db.model.NotificationEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity;
import com.edotassi.amazmod.db.model.NotificationPreferencesEntity_Table;
import com.edotassi.amazmod.event.local.ActionToNotificationLocal;
import com.edotassi.amazmod.event.local.IntentToNotificationLocal;
import com.edotassi.amazmod.event.local.ReplyToNotificationLocal;
import com.edotassi.amazmod.notification.factory.NotificationFactory;
import com.edotassi.amazmod.support.SilenceApplicationHelper;
import com.edotassi.amazmod.transport.TransportService;
import com.edotassi.amazmod.util.ActionsHolder;
import com.edotassi.amazmod.util.NotificationUtils;
import com.edotassi.amazmod.util.PendingIntentHolder;
import com.edotassi.amazmod.util.Screen;
import com.edotassi.amazmod.watch.Watch;
import com.huami.watch.notification.data.StatusBarNotificationData;
import com.huami.watch.transport.DataBundle;
import com.pixplicity.easyprefs.library.Prefs;
import com.raizlabs.android.dbflow.config.FlowManager;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.tinylog.Logger;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Optional;

import amazmod.com.transport.Constants;
import amazmod.com.transport.Transport;
import amazmod.com.transport.data.NotificationActionData;
import amazmod.com.transport.data.NotificationData;
import amazmod.com.transport.data.NotificationIntentData;
import amazmod.com.transport.data.NotificationReplyData;

import static java.lang.Math.abs;

public class NotificationService extends NotificationListenerService {

    public static final int FLAG_WEARABLE_REPLY = 0x00000001;
    private static final long BLOCK_INTERVAL = 60000 * 30L; //Thirty minutes
    private static final long REPEATING_BLOCK_INTERVAL = 60000 * 5L; //Five minutes
    private static final long MAPS_INTERVAL = 60000 * 3L; //Three minutes
    private static final long VOICE_INTERVAL = 1000 * 5L; //Five seconds

    private static final long JOB_INTERVAL = 1000 * 5L; //Five seconds
    private static final long JOB_MAX_INTERVAL = 1000 * 60L; //One minute
    private static final long KEEP_SERVICE_RUNNING_INTERVAL = 60000L * 15L; //Fifteen minutes
    private static final long CUSTOMUI_LATENCY = 1L;

    private static final String[] APP_WHITELIST = { //apps that do not fit some filter (need to be list as sorted)
            "com.contapps.android",
            "com.google.android.googlequicksearchbox",
            "com.microsoft.office.outlook",
            "com.skype.raider"
    };

    private static final String[] VOICE_APP_LIST = { //apps may use voice calls without notifications
            "com.skype.m2",
            "com.skype.raider",
            "org.telegram.messenger",
            "org.telegram.plus",
            "org.thunderdog.challegram",
            "com.viber.voip",
            "org.thoughtcrime.securesms",
            "eu.siacs.conversations",
            "com.whatsapp",
            "com.discord"
    };

    private ArrayMap<String, String> notificationTimeGone;
    private ArrayMap<String, StatusBarNotification> notificationsAvailableToReply;
    Hashtable<Integer, int[]> grouped_notifications = new Hashtable<>();

    private static long lastTimeNotificationArrived = 0;
    private static long lastTimeRepeatingArrived = 0;
    private static long lastTimeNotificationSent = 0;
    private static String lastTxt = "";
    private static String lastNotificationTxt = "";
    private static byte lastFilter;

    private static ComponentName serviceComponent;
    private static JobScheduler jobScheduler;

    @Override
    public void onCreate() {
        super.onCreate();

        EventBus.getDefault().register(this);

        notificationsAvailableToReply = new ArrayMap<>();

        NotificationStore notificationStore = new NotificationStore();

        serviceComponent = new ComponentName(getApplicationContext(), NotificationJobService.class);
        jobScheduler = (JobScheduler) getApplicationContext().getSystemService(JOB_SCHEDULER_SERVICE);

        Logger.debug("onCreate");

        startPersistentNotification();
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Logger.debug("onListenerConnected");

        //Causes the job to restart cyclically...

//        startPersistentNotification();
//
//        // Cancel all pending jobs to keep service running, then schedule a new one
//        cancelPendingJobs(0);
//        scheduleJob(0, 0, null);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.debug("onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        Logger.debug("onDestroy");
        super.onDestroy();
    }

    private String getSbnCustomKey(StatusBarNotification statusBarNotification) {
        String key = statusBarNotification.getKey();
        key += statusBarNotification.getNotification().when;
        CharSequence charSequence = statusBarNotification.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
        if (charSequence != null) {
            key += charSequence.toString();
        }
        return key;
    }

    private String sbnCustomKeyLast = "";
    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        String sbnCustomKey = getSbnCustomKey(statusBarNotification);
        Logger.debug("sbnCustomKey: " + sbnCustomKey);
        if (!sbnCustomKey.equals(sbnCustomKeyLast)) {
            sbnCustomKeyLast = sbnCustomKey;

            String notificationPackage = statusBarNotification.getPackageName();

            String notificationTxt = "";
            CharSequence charSequence = statusBarNotification.getNotification().extras.getCharSequence(Notification.EXTRA_TEXT);
            if (charSequence != null) {
                notificationTxt = charSequence.toString();
            }

            if (!isPackageAllowed(notificationPackage)) {
                //Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + ((char) (byte) Constants.FILTER_PACKAGE));
                Logger.debug("[Notification Blocked] Notifications from {} are blocked.", notificationPackage);
                checkAndLog(notificationPackage, notificationTxt, Constants.FILTER_PACKAGE);
                return;
            } else if (isPackageSilenced(notificationPackage)) {
                //Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + ((char) (byte) Constants.FILTER_SILENCE));
                Logger.debug("[Notification Blocked] Notifications from {} are currently silenced.", notificationPackage);
                checkAndLog(notificationPackage, notificationTxt, Constants.FILTER_SILENCE);
                return;
            } else if (isPackageFiltered(statusBarNotification)) {
                //Logger.debug("onNotificationPosted blocked: " + notificationPackage + " / " + ((char) (byte) Constants.FILTER_TEXT));
                Logger.debug("[Notification Blocked] Notifications from {} was blocked because of content filters.", notificationPackage);
                checkAndLog(notificationPackage, notificationTxt, Constants.FILTER_TEXT);
                return;
            } else if (isNotificationsDisabled()) {
                Logger.debug("[Notification Blocked] All notifications are disabled (disabled, DND, Driving etc). {}", notificationPackage);
                checkAndLog(notificationPackage, notificationTxt, Constants.FILTER_NOTIFICATIONS_DISABLED);
                return;
            } else if (isNotificationsDisabledWhenScreenOn()) {
                if (!Screen.isDeviceLocked(this)) {
                    Logger.debug("[Notification Blocked] Device is unlocked. {}", notificationPackage);
                    checkAndLog(notificationPackage, notificationTxt, Constants.FILTER_SCREENON);
                    return;
                } else if (!isNotificationsEnabledWhenScreenLocked()) {
                    Logger.debug("[Notification Blocked] Device is in lock-screen. {}", notificationPackage);
                    checkAndLog(notificationPackage, notificationTxt, Constants.FILTER_SCREENLOCKED);
                    return;
                }
            }

            Logger.debug("[New Notification] Notification is posted: " + statusBarNotification.getKey());

            byte filterResult = filter(statusBarNotification);

            Logger.debug("[New Notification] pkg: {}, filterResult: {}", notificationPackage, ((char) (byte) filterResult));

            // Logger.debug("Filters: U=" + (filterResult == Constants.FILTER_UNGROUP) +" C="+ (filterResult == Constants.FILTER_CONTINUE) +" K="+ (filterResult == Constants.FILTER_LOCALOK) );
            if (filterResult == Constants.FILTER_CONTINUE || filterResult == Constants.FILTER_UNGROUP || filterResult == Constants.FILTER_LOCALOK) {

                StatusBarNotification sbn = null;

                // Check if notification is in group
                if (filterResult == Constants.FILTER_UNGROUP && Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_UNGROUP, false)) {
                    //Logger.debug("NotificationService onNotificationPosted ungroup01 key: " + statusBarNotification.getKey() + " \\ id: " + statusBarNotification.getId());
                    int nextId = statusBarNotification.getId() + newUID();
                    sbn = new StatusBarNotification(notificationPackage, "", nextId,
                            statusBarNotification.getTag(), 0, 0, 0,
                            statusBarNotification.getNotification(), statusBarNotification.getUser(),
                            statusBarNotification.getPostTime());

                    if (grouped_notifications.containsKey(statusBarNotification.getId())) {
                        //Logger.debug("NotificationService onNotificationPosted ungroup02 id exists: " + statusBarNotification.getId() + " \\ nextId: " + nextId);
                        // Get array
                        int[] grouped = grouped_notifications.get(statusBarNotification.getId());
                        // Define the new array
                        if (grouped != null) {
                            int[] newArray = new int[grouped.length + 1];
                            // Copy values into new array
                            System.arraycopy(grouped, 0, newArray, 0, grouped.length);
                            newArray[newArray.length - 1] = nextId;
                            grouped_notifications.put(statusBarNotification.getId(), newArray);
                            //Logger.debug("NotificationService onNotificationPosted ungroup03 id exists newArray: " + Arrays.toString(newArray));
                        } else
                            Logger.error("grouped: could not create array");
                    } else {
                        //Logger.debug("NotificationService onNotificationPosted ungroup04 new id: " + statusBarNotification.getId() + " \\ nextId: " + nextId);
                        // New in array
                        grouped_notifications.put(statusBarNotification.getId(), new int[]{nextId});
                    }
                }

                if (sbn == null)
                    sbn = statusBarNotification;

                // Select between Standard UI and Custom UI notifications
                if (isCustomUIEnabled())
                    sendNotificationWithCustomUI(filterResult, sbn);
                else
                    sendNotificationWithStandardUI(filterResult, sbn);

                //Logger.debug("onNotificationPosted sent: " + notificationPackage + " / " + ((char) (byte) filterResult));
                storeForStats(notificationPackage, filterResult);

            } else {
                if (isRingingNotification(filterResult, notificationPackage)) { // Messenger voice call notifications
                    Logger.debug("[Ringing Notification] New notifications is a ringing notification: " + ((char) (byte) filterResult));
                    handleCall(statusBarNotification, notificationPackage);

                } else if (isMapsNotification(filterResult, notificationPackage)) { // Maps notification
                    Logger.debug("[Maps Notification] New notifications is a MapsNotification: " + ((char) (byte) filterResult));
                    mapNotification(statusBarNotification);

                } else { // Blocked
                    Logger.debug("[New Notification] New notifications is blocked. (pkg: {}, marked as: {})", notificationPackage, ((char) (byte) filterResult));
                    checkAndLog(notificationPackage, notificationTxt, filterResult);
                }
            }
        }
    }

    // Remove notification from watch if it was removed from phone
    @Override
    public void onNotificationRemoved(final StatusBarNotification statusBarNotification) {
        Logger.info("onNotificationRemoved statusBarNotification: " + statusBarNotification);
        if (statusBarNotification == null)
            return;

        String key = statusBarNotification.getKey();

        Logger.info("onNotificationRemoved Check settings");
        // Check settings
        if (!Prefs.getBoolean(Constants.PREF_ENABLE_NOTIFICATIONS, Constants.PREF_DEFAULT_ENABLE_NOTIFICATIONS)
                || (Prefs.getBoolean(Constants.PREF_DISABLE_REMOVE_NOTIFICATIONS, false))) {
            Logger.debug("[Notification Remove] Notification wont be removed due to current settings. (key: {})", key);
            return;
        }


        Logger.info("onNotificationRemoved Check filters");
        if (!(isPackageAllowed(statusBarNotification.getPackageName())
                //&& (!NotificationCompat.isGroupSummary(statusBarNotification.getNotification()))
                && ((statusBarNotification.getNotification().flags & Notification.FLAG_ONGOING_EVENT) != Notification.FLAG_ONGOING_EVENT))) {
            Logger.debug("[Notification Remove] App {} is ignored: P || G || O", statusBarNotification.getPackageName());
            return;
        }

        //Logger.debug("[Notification Remove] key {}", key);

        /*
        * Disabled while testing JobScheduler
        *
        // Connect transporter
        if(!isJobSchedulerEnabled())
        Transporter notificationTransporter = TransporterClassic.get(this, "com.huami.action.notification");
        notificationTransporter.connectTransportService();
        */

        DataBundle dataBundle = new DataBundle();
        dataBundle.putString("key", statusBarNotification.getKey());
        Logger.info("onNotificationRemoved dataBundle: " + dataBundle);

        String uuid = newKey(key);
        NotificationStore.addRemovedNotification(uuid, dataBundle);
        int id = NotificationJobService.NOTIFICATION_REMOVED;
        int jobId = statusBarNotification.getId() + newUID();

        Logger.info("onNotificationRemoved scheduleJob started");
        scheduleJob(id, jobId, uuid);

        Logger.debug("[Notification Remove] Remove scheduled. key {}, jobId: {}, uuid: {}", key, jobId, uuid);
        //Logger.info("onNotificationRemoved jobScheduled: " + jobId + " \\ uuid: " + uuid);

        /*
        * Disabled while testing JobScheduler
        *
        notificationTransporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
            @Override
            public void onResultBack(DataTransportResult dataTransportResult) {
                log.d(dataTransportResult.toString());
                Logger.debug("NotificationService onNotificationRemoved id: " + statusBarNotification.getId());
            }
        });
        */

        // Check if notification is grouped
        if (grouped_notifications.containsKey(statusBarNotification.getId())) {
            //Logger.debug("NotificationService onNotificationRemoved ungroup01 key: " + statusBarNotification.getKey() + " \\ id: " + statusBarNotification.getId());
            // Initial array
            int[] grouped = grouped_notifications.get(statusBarNotification.getId());
            //Logger.debug("NotificationService onNotificationRemoved ungroup02 key: " + statusBarNotification.getKey()  + " \\ grouped: " + Arrays.toString(grouped));

            // Loop each notification in group
            assert grouped != null;
            for (int groupedId : grouped) {
                //int nextId = abs((int) (long) (statusBarNotification.getId() % 10000L)) + i;
                jobId = groupedId + newUID();
                //Logger.debug("NotificationService onNotificationRemoved ungroup i: " + groupedId);

                dataBundle = new DataBundle();
                StatusBarNotification sbn = new StatusBarNotification(statusBarNotification.getPackageName(), "",
                        groupedId, statusBarNotification.getTag(), 0, 0, 0,
                        statusBarNotification.getNotification(), statusBarNotification.getUser(),
                        statusBarNotification.getPostTime());
                dataBundle.putString("key", sbn.getKey());
                Logger.info("onNotificationRemoved dataBundle: " + dataBundle);

                uuid = newKey(statusBarNotification.getKey());
                NotificationStore.addRemovedNotification(uuid, dataBundle);

                scheduleJob(id, jobId, uuid);

                Logger.info("onNotificationRemoved ungroup jobScheduled: " + jobId + " \\ uuid: " + uuid);

                /*
                * Disabled while testing JobScheduler
                *
                notificationTransporter.send("del", dataBundle, new Transporter.DataSendResultCallback() {
                    @Override
                    public void onResultBack(DataTransportResult dataTransportResult) {
                        log.d(dataTransportResult.toString());
                    }
                });
                */
            }
            grouped_notifications.remove(statusBarNotification.getId());
        }

        /*
        * Disabled while testing JobScheduler
        *
        //Disconnect transporter to avoid leaking
        notificationTransporter.disconnectTransportService();
        */

        //Reset time of last notification when notification is removed
        if (lastTimeNotificationArrived > 0) {
            lastTimeNotificationArrived = 0;
        }
        if (lastTimeNotificationSent > 0) {
            lastTimeNotificationSent = 0;
        }
    }

    private void sendNotificationWithCustomUI(byte filterResult, StatusBarNotification statusBarNotification) {
        Logger.debug("[Custom UI Notification]  pkg: " + statusBarNotification.getPackageName() + " \\ filterResult: " + filterResult);

        NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
        notificationsAvailableToReply.put(notificationData.getKey(), statusBarNotification);

        notificationData.setVibration(getDefaultVibration());
        notificationData.setVibrationAmount(getDefaultVibrationAmount());
        notificationData.setHideButtons(true);
        notificationData.setForceCustom(false);

        // Hide replies
        if (filterResult == Constants.FILTER_LOCALOK)
            notificationData.setHideReplies(true);
        else
            notificationData.setHideReplies(false);

        if (isJobSchedulerEnabled()) {
            // Schedule Notification
            final String uuid = newKey(statusBarNotification.getKey());
            NotificationStore.addCustomNotification(uuid, notificationData);
            int id = NotificationJobService.NOTIFICATION_POSTED_CUSTOM_UI;
            int jobId = statusBarNotification.getId() + newUID();
            scheduleJob(id, jobId, uuid);
            Logger.info("sendNotificationWithCustomUI jobScheduled: " + jobId + " \\ uuid: " + uuid);
        } else {
            // Send notification directly
            Watch.get().postNotification(notificationData);
            Logger.info("sendNotificationWithCustomUI sent without schedule: " + statusBarNotification.getKey());
        }
    }

    private void sendNotificationWithStandardUI(byte filterResult, StatusBarNotification statusBarNotification) {
        DataBundle dataBundle = new DataBundle();
        dataBundle.putParcelable("data", StatusBarNotificationData.from(this, statusBarNotification, false));

        if (isJobSchedulerEnabled()) {
            // Schedule Notification
            String uuid = newKey(statusBarNotification.getKey());
            //Logger.debug("sendNotificationWithStandardUI uuid: " + uuid + " \\ filterResult: " + filterResult);
            int notificationId = statusBarNotification.getId();
            int id = NotificationJobService.NOTIFICATION_POSTED_STANDARD_UI;
            int jobId = notificationId + newUID();
            NotificationStore.addStandardNotification(uuid, dataBundle);
            scheduleJob(id, jobId, uuid);
            Logger.info("sendNotificationWithStandardUI jobScheduled: " + jobId + " \\ uuid: " + uuid);

        } else {
            // Send notification directly
            TransportService.sendWithTransporterHuami("add", dataBundle);
            Logger.info("sendNotificationWithStandardUI: " + dataBundle.toString());
        }
    }

    private void scheduleJob(int id, int jobId, String uuid) {

        Logger.debug("id: {} jobId: {} uuid: {}", id, jobId, uuid);
        JobInfo.Builder builder = new JobInfo.Builder(jobId, serviceComponent);

        if (jobId == 0) {
            builder.setPeriodic(KEEP_SERVICE_RUNNING_INTERVAL);

        } else {
            if (id == NotificationJobService.NOTIFICATION_POSTED_CUSTOM_UI
                    && (!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, true))) {
                builder.setMinimumLatency(CUSTOMUI_LATENCY);
            } else {
                builder.setMinimumLatency(1L);
            }

            PersistableBundle bundle = new PersistableBundle();
            bundle.putInt(NotificationJobService.NOTIFICATION_MODE, id);
            bundle.putString(NotificationJobService.NOTIFICATION_UUID, uuid);

            builder.setBackoffCriteria(JOB_INTERVAL, JobInfo.BACKOFF_POLICY_LINEAR);
            builder.setOverrideDeadline(JOB_MAX_INTERVAL);
            builder.setOverrideDeadline(1L);
            builder.setExtras(bundle);
        }
        jobScheduler.schedule(builder.build());
    }

    private int newUID() {
        return abs((int) (long) (System.currentTimeMillis() % 10000L));
    }

    private String newKey(String key) {
        return key + "|" + System.currentTimeMillis();
    }

    private void cancelPendingJobs(int id) {
        JobInfo pendingJob = jobScheduler.getPendingJob(id);
        if (pendingJob != null) {
            Logger.debug("cancelPendingJobs jobInfo: " + pendingJob.toString());
            jobScheduler.cancel(pendingJob.getId());
        }
    }

    public static void cancelPendingJobs() {
        List<JobInfo> jobInfoList = jobScheduler.getAllPendingJobs();
        final int pendingJobs = jobInfoList.size();
        Logger.debug("cancelPendingJobs pendingJobs: " + pendingJobs);
        if (pendingJobs > 0)
            for (JobInfo jobInfo : jobInfoList) {
                Logger.debug("cancelPendingJobs jobInfo: " + jobInfo.toString());
                jobScheduler.cancel(jobInfo.getId());
            }
    }

    private int getAudioManagerMode() {
        try {
            return ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getMode();
        } catch (NullPointerException e) {
            Logger.error(e, "isRinging Exception: {}", e.getMessage());
            return AudioManager.MODE_INVALID;
        }
    }

    private int getDefaultVibration() {
        return Integer.parseInt(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION, Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION));

    }

    private int getDefaultVibrationAmount() {
        return Integer.parseInt(Prefs.getString(Constants.PREF_NOTIFICATIONS_VIBRATION_AMOUNT, Constants.PREF_DEFAULT_NOTIFICATIONS_VIBRATION_AMOUNT));

    }

    private int isRinging() {
        //Logger.debug("NotificationJobService isRinging AudioManager.MODE_IN_CALL = " + AudioManager.MODE_IN_CALL);
        //Logger.debug("NotificationJobService isRinging AudioManager.MODE_IN_COMMUNICATION = " + AudioManager.MODE_IN_COMMUNICATION);
        //Logger.debug("NotificationJobService isRinging AudioManager.MODE_RINGTONE = " + AudioManager.MODE_RINGTONE);
        //Logger.debug("NotificationJobService isRinging AudioManager.MODE_CURRENT = " + AudioManager.MODE_CURRENT);
        //Logger.debug("NotificationJobService isRinging AudioManager.MODE_INVALID = " + AudioManager.MODE_INVALID);
        //Logger.debug("NotificationJobService isRinging AudioManager.MODE_NORMAL = " + AudioManager.MODE_NORMAL);

        final int mode = getAudioManagerMode();
        if (AudioManager.MODE_IN_CALL == mode) {
            Logger.debug("isRinging Ringer: CALL");
        } else if (AudioManager.MODE_IN_COMMUNICATION == mode) {
            Logger.debug("isRinging Ringer: COMMUNICATION");
        } else if (AudioManager.MODE_RINGTONE == mode) {
            Logger.debug("isRinging Ringer: RINGTONE");
        } else {
            Logger.debug("isRinging Ringer: SOMETHING ELSE \\ mode: " + mode);
        }

        return mode;
    }

    private byte filter(StatusBarNotification statusBarNotification) {
        if (notificationTimeGone == null)
            notificationTimeGone = new ArrayMap<>();

        String notificationPackage = statusBarNotification.getPackageName();
        String notificationId = statusBarNotification.getKey();
        Notification notification = statusBarNotification.getNotification();
        String text = "";
        boolean localAllowed = false;
        boolean whitelistedApp = false;

        NotificationCompat.WearableExtender wearableExtender = new NotificationCompat.WearableExtender(notification);
        //List<NotificationCompat.Action> actions = wearableExtender.getActions();

        if (NotificationCompat.isGroupSummary(notification)) {
            //Logger.debug("filter isGroupSummary: " + notificationPackage);
            if (Arrays.binarySearch(APP_WHITELIST, notificationPackage) < 0) {
                Logger.debug("[Marked] Notification marked as FLAG_GROUP_SUMMARY");
                return Constants.FILTER_GROUP;
            } else {
                Logger.debug("[Marked] Notification NOT marked as FLAG_GROUP_SUMMARY because it's whitelisted");
                whitelistedApp = true;
            }
        }

        if ((notification.flags & Notification.FLAG_ONGOING_EVENT) == Notification.FLAG_ONGOING_EVENT) {
            Logger.debug("[Marked] Notification marked as FLAG_ONGOING_EVENT");
            return Constants.FILTER_ONGOING;
        }

        if (NotificationCompat.getLocalOnly(notification)) {
            //Logger.debug("filter: getLocalOnly: " + notificationPackage);
            if ((!Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_LOCAL_ONLY, false) && !whitelistedApp) ||
                    ((Arrays.binarySearch(APP_WHITELIST, notificationPackage) >= 0) && !whitelistedApp)) {
                Logger.debug("[Marked] Notification marked as LOCAL_ONLY");
                return Constants.FILTER_LOCAL;
            } else if (!whitelistedApp) {
                Logger.debug("[Marked] Notification marked as LOCAL_OK");
                localAllowed = true;
            }
        }

        CharSequence bigText = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);
        if (bigText != null)
            text = bigText.toString();

        //Logger.debug("filter: notificationPackage: " + notificationPackage + " \\ text: " + text);

        if (notificationTimeGone.containsKey(notificationId)) {
            String previousText = notificationTimeGone.get(notificationId);
            if ((previousText != null) && (previousText.equals(text)) && (!notificationPackage.equals("com.microsoft.office.outlook"))
                    && ((System.currentTimeMillis() - lastTimeNotificationArrived) < BLOCK_INTERVAL)) {
                Logger.debug("[Marked] Notification blocked as REPEATED (same text)");
                //Logger.debug("notification blocked by key: %s, id: %s, flags: %s, time: %s", notificationId, statusBarNotification.getId(), statusBarNotification.getNotification().flags, (System.currentTimeMillis() - statusBarNotification.getPostTime()));
                return Constants.FILTER_BLOCK;
            } else {
                notificationTimeGone.put(notificationId, text);
                lastTimeNotificationArrived = System.currentTimeMillis();
                Logger.debug("[Marked] Notification marked as allowed.");
                //Logger.debug("filter: allowed1: " + notificationPackage);
                if (localAllowed) return Constants.FILTER_LOCALOK;
                    //else if (whitelistedApp) return returnFilterResult(Constants.FILTER_CONTINUE);
                else return Constants.FILTER_UNGROUP;
            }
        }

        notificationTimeGone.put(notificationId, text);

        if (localAllowed)
            return Constants.FILTER_LOCALOK;

        // Notification passes all checks
        //Logger.debug("[Marked] Notification not marked.");
        return Constants.FILTER_CONTINUE;
    }

    private boolean isNotificationsDisabled() {
        return !Prefs.getBoolean(Constants.PREF_ENABLE_NOTIFICATIONS, Constants.PREF_DEFAULT_ENABLE_NOTIFICATIONS) ||
                (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_WHEN_DND, false) &&
                        Screen.isDNDActive(this, getContentResolver())) ||
                (Prefs.getBoolean(Constants.PREF_DISABLE_NOTIFICATIONS_WHEN_DRIVING, true) &&
                        Screen.isDrivingMode(this));
    }

    private boolean isNotificationsDisabledWhenScreenOn() {
        return !Prefs.getBoolean(Constants.PREF_ENABLE_NOTIFATIONS_WHEN_SCREEN_ON, false)
                && Screen.isInteractive(this);
    }

    private boolean isJobSchedulerEnabled() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATION_SCHEDULER, Constants.PREF_NOTIFICATION_SCHEDULER_DEFAULT);
    }

    private boolean isNotificationsEnabledWhenScreenLocked() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_WHEN_LOCKED, true);
    }

    private boolean isCustomUIEnabled() {
        return Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_CUSTOM_UI, false);
    }

    /*private boolean isStandardDisabled() {
        return Prefs.getBoolean(Constants.PREF_DISABLE_STANDARD_NOTIFICATIONS, false);
    }*/

    private boolean isRingingNotification(byte filterResult, String notificationPackage) {

        final boolean prefs = Prefs.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLE_VOICE_APPS, false);
        final int ring = isRinging();
        return ((filterResult == Constants.FILTER_ONGOING)
                && ((prefs && (ring == AudioManager.MODE_RINGTONE))
                || ((Arrays.binarySearch(VOICE_APP_LIST, notificationPackage) >= 0) && (ring == AudioManager.MODE_NORMAL))
                || ((notificationPackage.contains("skype")) && (ring == AudioManager.MODE_IN_COMMUNICATION))));
    }

    private boolean isMapsNotification(byte filterResult, String notificationPackage) {
        return ((filterResult == Constants.FILTER_ONGOING) && notificationPackage.contains("android.apps.maps") && !Screen.isDrivingMode(this));
    }

    private boolean isPackageAllowed(String packageName) {
        /*
        String packagesJson = Prefs.getString(Constants.PREF_ENABLED_NOTIFICATIONS_PACKAGES, "[]");
        Gson gson = new Gson();
        String[] packagesList = gson.fromJson(packagesJson, String[].class);
        return Arrays.binarySearch(packagesList, packageName) >= 0;
        */
        NotificationPreferencesEntity app = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();
        return app != null;
    }

    private boolean isPackageSilenced(String packageName) {
        NotificationPreferencesEntity app = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();
        return app != null && app.getSilenceUntil() > SilenceApplicationHelper.getCurrentTimeSeconds();
    }

    // Filter notification content
    private boolean isPackageFiltered(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        NotificationPreferencesEntity app = SQLite
                .select()
                .from(NotificationPreferencesEntity.class)
                .where(NotificationPreferencesEntity_Table.packageName.eq(packageName))
                .querySingle();

        // Check if app exists and if it has filters
        if (app == null || app.getFilter() == null || app.getFilter().isEmpty())
            return false;

        // Title text
        String notificationTitle = "";
        CharSequence bigTitle = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TITLE_BIG);
        if (bigTitle != null) {
            notificationTitle = bigTitle.toString();
        } else {
            CharSequence title = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TITLE);
            if (title != null && !title.toString().isEmpty()) {
                notificationTitle = title.toString();
            }
        }

        // Content text
        String notificationText = "";
        CharSequence bigText = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (bigText != null) {
            notificationText = bigText.toString();
        } else {
            CharSequence text = (statusBarNotification.getNotification().extras).getCharSequence(Notification.EXTRA_TEXT);
            if (text != null && !text.toString().isEmpty()) {
                notificationText = text.toString();
            }
        }

        // Get filters
        String[] filters = app.getFilter().split("\\r?\\n");
        // Get filter level (both, title, text)
        int filter_level = app.getFilterLevel();
        for (String filter : filters) {
            //Logger.debug("isPackageFiltered: Checking if '{}' contains '{}'", notificationText, filter);
            if (!filter.isEmpty()) {
                filter = filter.toLowerCase();
                // Check based on filter level / mode
                if (filter_level == Constants.NOTIFICATION_FILTER_BOTH && (notificationTitle.toLowerCase().contains(filter) || notificationText.toLowerCase().contains(filter))) {
                    Logger.debug("isPackageFiltered: Package '{}' filtered because TITLE or CONTENTS ('{}') contains '{}'", packageName, notificationTitle, notificationText, filter);
                    return !app.isWhitelist();
                } else if (filter_level == Constants.NOTIFICATION_FILTER_TITLE && notificationTitle.toLowerCase().contains(filter)) {
                    Logger.debug("isPackageFiltered: Package '{}' filtered because TITLE ('{}') contains '{}'", packageName, notificationTitle, filter);
                    return !app.isWhitelist();
                } else if (filter_level == Constants.NOTIFICATION_FILTER_CONTENTS && notificationText.toLowerCase().contains(filter)) {
                    Logger.debug("isPackageFiltered: Package '{}' filtered because CONTENTS ('{}') contains '{}'", packageName, notificationText, filter);
                    return !app.isWhitelist();
                }
            }
        }
        return app.isWhitelist();
    }

    //Avoid flooding of Notifications log by repeating/ongoing blocked notifications
    private void checkAndLog(String notificationPackage, String notificationTxt, byte filter) {
        //Logger.trace("package: {} filter: {}", notificationPackage, Character.toString((char) (byte) filter));
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTimeRepeatingArrived > REPEATING_BLOCK_INTERVAL || (!notificationTxt.equals(lastNotificationTxt)) || filter != lastFilter) {
            lastTimeRepeatingArrived = currentTime;
            lastNotificationTxt = notificationTxt;
            lastFilter = filter;
            storeForStats(notificationPackage, filter);
        }
    }

    private void storeForStats(String notificationPackage, byte filterResult) {
        try {
            NotificationEntity notificationEntity = new NotificationEntity();
            notificationEntity.setPackageName(notificationPackage);
            notificationEntity.setDate(System.currentTimeMillis());
            notificationEntity.setFilterResult(filterResult);

            FlowManager.getModelAdapter(NotificationEntity.class).insert(notificationEntity);
        } catch (Exception ex) {
            Logger.error(ex, "storeForStats: Failed to store notifications stats");
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void actionToNotificationLocal(ActionToNotificationLocal actionToNotificationLocal) {
        NotificationActionData notificationActionData = actionToNotificationLocal.getNotificationActionData();
        Integer notificationId = notificationActionData.getNotificationId();
        String title = notificationActionData.getTitle();
        System.out.println("ActionToNotificationLocal nId: " + notificationId);
        System.out.println("ActionToNotificationLocal title: " + title);

        Notification.Action[] actions = ActionsHolder.read(notificationId);
        if (actions != null && title != null) {
            Optional<Notification.Action> optionalAction = Arrays.stream(actions).filter(action -> {
                return title.contentEquals(action.title);
            }).findFirst();
            if (optionalAction.isPresent()) {
                try {
                    optionalAction.get().actionIntent.send();
                } catch (PendingIntent.CanceledException e) {
                    e.fillInStackTrace();
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void intentToNotificationLocal(IntentToNotificationLocal intentToNotificationLocal) {
        NotificationIntentData notificationIntentData = intentToNotificationLocal.getNotificationIntentData();
        Integer notificationId = notificationIntentData.getNotificationId();
        String packageName = notificationIntentData.getPackageName();
        System.out.println("IntentToNotificationLocal nId: " + notificationId);
        System.out.println("IntentToNotificationLocal packageName: " + packageName);

        Logger.debug("tryOpenNotification...");
        Boolean status = tryOpenNotification(notificationId, packageName, getApplicationContext());
        Logger.debug("tryOpenNotification: " + status);
    }

    public static Boolean tryOpenNotification(Integer savedSbnId, String packageName, Context context) {
        PendingIntent pendingIntent = PendingIntentHolder.read(savedSbnId);

        if (pendingIntent == null) {
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                Logger.debug("tryOpenNotification: OK 1");
                context.startActivity(launchIntent);
                return true;
            }
            return false;
        } else {
            try {
                Logger.debug("tryOpenNotification: OK 2");
                pendingIntent.send();
                return true;
            } catch (PendingIntent.CanceledException e) {
                Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
                if (launchIntent != null) {
                    Logger.debug("tryOpenNotification: OK 3");
                    context.startActivity(launchIntent);
                    return true;
                }
                e.printStackTrace();
                return false;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void replyToNotificationLocal(ReplyToNotificationLocal replyToNotificationLocal) {
        NotificationReplyData notificationReplyData = replyToNotificationLocal.getNotificationReplyData();
        Integer notificationId = notificationReplyData.getNotificationId();
        String title = notificationReplyData.getTitle();
        String reply = notificationReplyData.getReply();

        Logger.debug("replyToNotificationLocal notificationId: " + notificationId);
        Notification.Action[] actions = ActionsHolder.read(notificationId);
        if (actions != null && title != null) {
            Optional<Notification.Action> optionalAction = Arrays.stream(actions).filter(action -> {
                return title.contentEquals(action.title);
            }).findFirst();
            if (optionalAction.isPresent()) {
                Intent sendIntent = new Intent();
                Bundle msg = new Bundle();

                Notification.Action action = optionalAction.get();
                RemoteInput[] remoteInputs = action.getRemoteInputs();
                for (RemoteInput inputable : remoteInputs) {
                    msg.putCharSequence(inputable.getResultKey(), reply);
                }

                RemoteInput.addResultsToIntent(remoteInputs, sendIntent, msg);

                try {
                    action.actionIntent.send(this, 0, sendIntent);
                } catch (PendingIntent.CanceledException e) {
                    e.fillInStackTrace();
                }
            } else {
                Logger.warn("replyToNotificationLocal Notification action {} not found to reply", reply);
            }
        } else {
            Logger.warn("replyToNotificationLocal Notification {} not found to reply", notificationId);
        }
    }

    private void mapNotification(StatusBarNotification statusBarNotification) {

        final String notificationPackage = statusBarNotification.getPackageName();

        NotificationData notificationData = NotificationFactory.getMapNotification(this, statusBarNotification);

        if (notificationData == null) {
            Logger.error("[Map Notification] Null notification data (pkg: {})", notificationPackage);
            return;
        }

        Logger.debug("[Map Notification] package: {} key: {}", notificationPackage, statusBarNotification.getKey());

        final String txt = notificationData.getText();

        if (!txt.equals(lastTxt) || ((System.currentTimeMillis() - lastTimeNotificationSent) > MAPS_INTERVAL)) {
            // Set vibration
            notificationData.setVibration(getDefaultVibration());
            notificationData.setVibrationAmount(getDefaultVibrationAmount());
            lastTxt = txt;

            TransportService.sendWithTransporterNotifications(Transport.INCOMING_NOTIFICATION, notificationData.toDataBundle(new DataBundle()));

            lastTimeNotificationSent = System.currentTimeMillis();
            storeForStats(notificationPackage, Constants.FILTER_MAPS);
            //Logger.debug("mapNotification maps lastTxt: " + lastTxt);

        } else
            Logger.warn("same text or too soon");
    }


    private void handleCall(StatusBarNotification statusBarNotification, String notificationPackage) {
        Logger.debug("handleCall VoiceCall: " + notificationPackage);
        int mode = 0;
        if (notificationPackage.equals("org.thunderdog.challegram"))
            mode = 1;
        else if (notificationPackage.equals("org.telegram.messenger"))
            mode = 2;
        else if (notificationPackage.contains("skype"))
            mode = 3;
        int counter = 0;

        while (((mode == 0) && (isRinging() == AudioManager.MODE_RINGTONE))
                || ((mode == 1) && (counter < 3))
                || ((mode == 2) && ((counter < 3) && isRinging() != AudioManager.MODE_IN_COMMUNICATION))
                || ((mode == 3) && (counter < 3))) {
            long timeSinceLastNotification = (System.currentTimeMillis() - lastTimeNotificationSent);
            //Logger.debug("NotificationService handleCall timeSinceLastNotification: " + timeSinceLastNotification);

            if (timeSinceLastNotification > VOICE_INTERVAL) {

                counter++;
                NotificationData notificationData = NotificationFactory.fromStatusBarNotification(this, statusBarNotification);
                final String key = statusBarNotification.getKey();
                final PackageManager pm = getApplicationContext().getPackageManager();

                //Logger.debug("NotificationService handleCall notificationPackage: " + notificationPackage);

                ApplicationInfo ai;
                try {
                    ai = pm.getApplicationInfo(notificationPackage, 0);
                } catch (final PackageManager.NameNotFoundException e) {
                    Logger.error(e, "handleCall getApplicationInfo Exception: {}", e.getMessage());
                    ai = null;
                }
                final String applicationName = (String) (ai != null ? pm.getApplicationLabel(ai) : "(unknown)");

                //Logger.debug("NotificationService handleCall applicationName: " + applicationName);

                notificationData.setText(notificationData.getText() + "\n" + applicationName);
                notificationData.setVibration(getDefaultVibration());
                notificationData.setVibrationAmount(getDefaultVibrationAmount());
                notificationData.setHideReplies(true);
                notificationData.setHideButtons(false);
                notificationData.setForceCustom(true);

                //NotificationJobService.sendCustomNotification(this, notificationData);

                /*
                NotificationStore.addCustomNotification(key, notificationData);
                int id = NotificationJobService.NOTIFICATION_POSTED_VOICE;
                int jobId = id + abs((int) (long) (statusBarNotification.getId() % 10000L));

                scheduleJob(id, jobId, key);
                 */

                TransportService.sendWithTransporterNotifications(Transport.INCOMING_NOTIFICATION, notificationData.toDataBundle(new DataBundle()));

                lastTimeNotificationSent = System.currentTimeMillis();

                Logger.debug("handleCall notificationData.getText: " + notificationData.getText());

                final int audioMode = getAudioManagerMode();

                Logger.debug("handleCall audioMode: " + audioMode + " \\ counter: " + counter);

                if (((AudioManager.MODE_RINGTONE != audioMode) && mode == 0) || ((counter == 2) && (mode == 1 || mode == 2 || mode == 3))) {
                    storeForStats(notificationPackage, Constants.FILTER_VOICE);
                }
            } else
                SystemClock.sleep(300);
        }
    }

    private void startPersistentNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String model = PreferenceManager.getDefaultSharedPreferences(this)
                    .getString(Constants.PREF_WATCH_MODEL, "");

            PersistentNotification persistentNotification = new PersistentNotification(this, model);
            Notification notification = persistentNotification.createPersistentNotification();

            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (mNotificationManager != null)
                mNotificationManager.notify(persistentNotification.getNotificationId(), notification);

            startForeground(persistentNotification.getNotificationId(), notification);
        }
    }
}
