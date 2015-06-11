package edu.unt.sell.contextmon.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by SELL-1 on 3/25/2015.
 */
public class BroadcastDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = BroadcastDatabaseHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "broadcasts.db";
    private static final int DATABASE_VERSION = 1;
    private Context mContext;

    // list of ids that are being synced, store them here so that they can be deleted
    // after successful sync
    public ArrayList<Integer> mSyncList;

    public BroadcastDatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
        mSyncList = new ArrayList<Integer>();
    }

    // this method is called during creation of the database
    @Override
    public void onCreate(SQLiteDatabase database){
        BroadcastTable.onCreate(database);
    }

    // this method is called during an upgrade of the database
    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
        BroadcastTable.onUpgrade(database, oldVersion, newVersion);
    }

    // compose JSON out of SQLite records in addition to device id and android version
    public String composeJSONfromSQLite(boolean isTablet) {
        String deviceId = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
        String androidVersion = Build.VERSION.RELEASE;
        ArrayList<HashMap<String, String>> broadcastList;
        broadcastList = new ArrayList<HashMap<String, String>>();
        String selectQuery = "SELECT * FROM broadcasts";
        SQLiteDatabase database = this.getWritableDatabase();
        Cursor cursor = database.rawQuery(selectQuery, null);
        if (cursor.moveToFirst()) {
            do {
                HashMap<String, String> map = new HashMap<String, String>();
                map.put("_id", cursor.getString(0));
                map.put("action", cursor.getString(1));
                map.put("extras", cursor.getString(2));
                map.put("timestamp", cursor.getString(3));
                map.put("device_id", deviceId);
                map.put("android_version", androidVersion);
                if (isTablet) {
                    map.put("is_tablet", "1");
                } else {
                    map.put("is_tablet", "0");
                }
                broadcastList.add(map);

                mSyncList.add(Integer.parseInt(cursor.getString(0)));
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();
        Gson gson = new GsonBuilder().create();
        String jsonString = gson.toJson(broadcastList);
        Log.i(TAG, "Here is the json data: " + jsonString);
        return jsonString;
    }

    // delete all entries that have just been synced from the sqlite database
    // takes in an ArrayList of ids
    public void deleteJustSynced() {
        // generate a string that serves as the list of IDs in the query's IN clause
        StringBuffer inClauseBuffer = new StringBuffer();
        int syncListSize = mSyncList.size();
        for (int i=0; i<syncListSize; i++) {
            inClauseBuffer.append(Integer.toString(mSyncList.get(i)));
            if (i != syncListSize - 1) {
                inClauseBuffer.append(",");
            }
        }

        StringBuffer deleteQuery = new StringBuffer();
        deleteQuery.append("DELETE FROM broadcasts WHERE _id IN (");
        deleteQuery.append(inClauseBuffer);
        deleteQuery.append(")");
        String deleteQueryStr = deleteQuery.toString();
        Log.i(TAG, deleteQueryStr);
        SQLiteDatabase database = this.getWritableDatabase();
        database.execSQL(deleteQueryStr);
        database.close();
    }
}
