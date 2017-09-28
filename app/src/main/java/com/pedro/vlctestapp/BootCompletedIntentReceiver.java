package com.pedro.vlctestapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Vinal on 18/09/2017.
 */

// This class is implemented to start the Service listner when device is booted.
public class BootCompletedIntentReceiver extends BroadcastReceiver {
@Override
public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
        Intent pushIntent = new Intent(context, ServiceListner.class);
        context.startService(pushIntent);
        }


        }
        }
