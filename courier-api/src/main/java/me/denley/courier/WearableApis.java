package me.denley.courier;

import android.support.annotation.NonNull;

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

}
