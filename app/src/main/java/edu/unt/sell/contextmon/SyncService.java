package edu.unt.sell.contextmon;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import org.apache.http.Header;

import edu.unt.sell.contextmon.db.BroadcastDatabaseHelper;


public class SyncService extends IntentService {

    private static final String TAG = SyncService.class.getSimpleName();

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final BroadcastDatabaseHelper database = new BroadcastDatabaseHelper(getApplicationContext());
        AsyncHttpClient client = new SyncHttpClient();
        RequestParams params = new RequestParams();
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        String broadcastJSON = database.composeJSONfromSQLite(isTablet);
        Log.i(TAG, broadcastJSON);
        if (!broadcastJSON.equals("[]")) {
            params.put("broadcasts_json", broadcastJSON);
            String endpoint = "";
            // String endpoint = "http://10.0.3.2/html/contextmon_sync/endpoint.php";
            client.post("", params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.i(TAG, responseBody.toString());
                    database.deleteJustSynced();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    if (statusCode == 404) {
                        Log.d(TAG, "Requested resource not found");
                    } else if (statusCode == 500) {
                        Log.d(TAG, "Something went wrong at the server end");
                    } else {
                        Log.d(TAG, "Unexpected error occurred! Device might not be connected to the Internet");
                    }
                }
            });
        } else {
            Log.i(TAG, "There is no data in the sqlite DB");
        }
    }

}
