package com.amazmod.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.amazmod.service.Constants;
import com.amazmod.service.events.ActionNotificationEvent;
import com.amazmod.service.events.IntentNotificationEvent;
import com.amazmod.service.events.ReplyNotificationEvent;

import org.tinylog.Logger;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by edoardotassinari on 25/04/18.
 */

public class NotificationReplyAndActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            Logger.warn("NotificationReplyAndActionReceiver null action, returning...");
            return;
        }

        String action = intent.getAction();
        Logger.debug("NotificationReplyAndActionReceiver action: {}", action);

        if (Constants.INTENT_ACTION_REPLY.equals(action)) {
            String reply = intent.getStringExtra(Constants.EXTRA_REPLY);
            String replyAction = intent.getStringExtra(Constants.EXTRA_ACTION);
            Integer sbnId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_ID, -1);

            EventBus.getDefault().post(new ReplyNotificationEvent(sbnId, replyAction, reply));
            Logger.debug("NotificationReplyAndActionReceiver action: {} \\ notificationKey: {} \\ replyAction: {} \\ reply: {}", action, sbnId, replyAction, reply);

        /* USed for testing purposes only
        Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(vibe != null) {
            vibe.vibrate(100);
            Logger.warn("NotificationReplyReceiver - vibRRRRRRRRRRRRate");
        }
        */
        }
        if (Constants.INTENT_ACTION_ACTION.equals(action)) {
            String title = intent.getStringExtra(Constants.EXTRA_ACTION);
            Integer sbnId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_ID, -1);

            EventBus.getDefault().post(new ActionNotificationEvent(sbnId, title));
            Logger.debug("NotificationReplyAndActionReceiver action: {} \\ notificationKey: {} \\ title: {}", action, sbnId, title);

        /* USed for testing purposes only
        Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(vibe != null) {
            vibe.vibrate(100);
            Logger.warn("NotificationReplyReceiver - vibRRRRRRRRRRRRate");
        }
        */
        }
        if (Constants.INTENT_ACTION_INTENT.equals(action)) {
            String packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE);
            Integer sbnId = intent.getIntExtra(Constants.EXTRA_NOTIFICATION_ID, -1);

            EventBus.getDefault().post(new IntentNotificationEvent(sbnId, packageName));
            Logger.debug("NotificationReplyAndActionReceiver action: {} \\ notificationKey: {} \\ packageName: {}", action, sbnId, packageName);

        /* USed for testing purposes only
        Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(vibe != null) {
            vibe.vibrate(100);
            Logger.warn("NotificationReplyReceiver - vibRRRRRRRRRRRRate");
        }
        */
        }
    }
}
