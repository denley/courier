package me.denley.courier;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Methods in this class are for use by generated code.
 * Do not use this directly.
 */
@SuppressWarnings("unused")
public class WearableApis {

    /** For use by generated code, do not use */
    public static final int NODE = 0x0001;
    /** For use by generated code, do not use */
    public static final int DATA = 0x0010;
    /** For use by generated code, do not use */
    public static final int MESSAGE = 0x0100;

    /** For use by generated code, do not use */
    @Nullable public static GoogleApiClient googleApiClient = null;

    /** For use by generated code, do not use */
    @NonNull static DataApi DataApi = Wearable.DataApi;

    /** For use by generated code, do not use */
    @NonNull static MessageApi MessageApi = Wearable.MessageApi;

    /** For use by generated code, do not use */
    @NonNull static NodeApi NodeApi = Wearable.NodeApi;

    /** For use by generated code, do not use */
    @NonNull public static DataApi getDataApi() {
        return DataApi;
    }

    /** For use by generated code, do not use */
    @NonNull public static MessageApi getMessageApi() {
        return MessageApi;
    }

    /** For use by generated code, do not use */
    @NonNull public static NodeApi getNodeApi() {
        return NodeApi;
    }

    /** For use by generated code, do not use */
    public static boolean hasMockDataApi() {
        return DataApi!=Wearable.DataApi;
    }

    /** For use by generated code, do not use */
    public static boolean hasMockMessageApi() {
        return MessageApi!=Wearable.MessageApi;
    }

    /** For use by generated code, do not use */
    public static boolean hasMockNodeApi() {
        return NodeApi!=Wearable.NodeApi;
    }

    /** For use by generated code, do not use */
    public static boolean hasAllMockApis() {
        return hasMockDataApi() && hasMockNodeApi() && hasMockMessageApi();
    }

    /** For use by generated code, do not use */
    static void ensureApiClient(final Context context) {
        if(googleApiClient!=null && googleApiClient.isConnected()) {
            return;
        }
        if(WearableApis.hasAllMockApis()) {
            return;
        }

        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();

        final ConnectionResult result = googleApiClient.blockingConnect();

        if(!result.isSuccess()) {
            googleApiClient = null;
        }
    }

    /** For use by generated code, do not use */
    public interface WearableApiRunnable {
        public void run(GoogleApiClient apiClient);
    }

    /** For use by generated code, do not use */
    public static void makeWearableApiCall(final Context context, final int apis, final WearableApiRunnable task) {
        final boolean mockMode =
                ((apis&NODE)==0 || hasMockNodeApi())
                && ((apis&DATA)==0 || hasMockDataApi())
                && ((apis&MESSAGE)==0 || hasMockMessageApi());

        new Thread(){
            public void run() {
                if(mockMode) {
                    task.run(null);
                } else {
                    ensureApiClient(context);
                    if (googleApiClient != null) {
                        task.run(googleApiClient);
                    }
                }
            }
        }.start();
    }

}
