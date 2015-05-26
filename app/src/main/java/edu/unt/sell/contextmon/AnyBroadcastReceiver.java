package edu.unt.sell.contextmon;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

import edu.unt.sell.contextmon.contentprovider.BroadcastContentProvider;
import edu.unt.sell.contextmon.db.BroadcastTable;

import static android.os.SystemClock.elapsedRealtime;

public class AnyBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = AnyBroadcastReceiver.class.getSimpleName();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    public static final String EXTRA_ACTION = "EXTRA_ACTION";
    public static final String EXTRA_EXTRAS = "EXTRA_EXTRAS";

    public AnyBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context pContext, Intent pIntent) {
        if (pIntent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            pContext.startService(new Intent(pContext, BroadcastMonitoringService.class));

            // set the syncService alarm
            Intent syncIntent = new Intent(pContext, SyncService.class);
            AlarmManager alarmManager = (AlarmManager) pContext.getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getService(pContext.getApplicationContext(), 0, syncIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            // long interval = 14400000;
            long interval = 10000;
            long triggerTime = elapsedRealtime() + interval;
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerTime, AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);
            // alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, triggerTime, interval, pendingIntent);
        }

        Log.d(TAG, "Received broadcast intent is: " + pIntent.toString());
        String action = pIntent.getAction();
        String extrasString = getExtrasString(pIntent);

        // you might need to create an API service to get description of intent instead of just
        // saving the intent action and extras string

        saveReceivedBroadcastDetails(pContext, action, extrasString);

        // BroadcastMonitoringService.showNotification(pContext, action);
    }

    private void saveReceivedBroadcastDetails(Context pContext, String pAction, String pExtrasString) {
        ContentValues values = new ContentValues();
        values.put(BroadcastTable.COLUMN_ACTION, pAction);
        values.put(BroadcastTable.COLUMN_EXTRAS, pExtrasString);
        values.put(BroadcastTable.COLUMN_TIMESTAMP, dateFormat.format(Calendar.getInstance().getTime()));

        pContext.getContentResolver().insert(BroadcastContentProvider.CONTENT_URI, values);
        Log.i(TAG, "Saved broadcast: Action: " + pAction + ", Extras: " + pExtrasString + ", Time: "
            + dateFormat.format(Calendar.getInstance().getTime()));
    }

    private String getExtrasString(Intent pIntent){
        String extrasString = "";
        Bundle extras = pIntent.getExtras();
        try {
            if (extras != null) {
                Set<String> keySet = extras.keySet();
                for (String key: keySet) {
                    try {
                        String extraValue = pIntent.getExtras().get(key).toString();
                        extrasString += key + ": " + extraValue + "\n";
                    } catch (Exception e) {
                        Log.d(TAG, "Exception 2 in getExtrasString(): " + e.toString());
                        extrasString += key + ": Exception:" + e.getMessage() + "\n";
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception in getExtrasString(): " + e.toString());
            extrasString += "Exception:" + e.getMessage() + "\n";
        }
        Log.d(TAG, "extras=" + extrasString);
        return extrasString;
    }
}
