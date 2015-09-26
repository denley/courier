package me.denley.courier;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static me.denley.courier.WearableApis.DATA;
import static me.denley.courier.WearableApis.MESSAGE;
import static me.denley.courier.WearableApis.NODE;

/**
 * This class is used to start and stop receiving callbacks on annotated fields and methods.
 * This is done with the {@link #startReceiving} and {@link #stopReceiving} methods.
 *
 * This class also contains convenience methods for sending messages and data through the wearable API.
 */
@SuppressWarnings("unused")
public final class Courier {

    private static final Map<Class, DeliveryBoy> DELIVERY_STAFF = new LinkedHashMap<Class, DeliveryBoy>();

    /** For use by generated code. Don't use this. */
    public interface DeliveryBoy<T> {
        void startReceiving(Context context, T target);
        void stopReceiving(T target);
    }


    /**
     * Determines whether or not the Wearable API is available to communicate with a paired device. If this method
     * returns false then all other methods in this class will silently fail (and {@link #getLocalNode} and {@link #getAssetInputStream} will return null).
     *
     * For the most part, this will return true if the user has the "Android Wear" app installed and has used it to pair a watch, and false otherwise.
     *
     * Note: This does not correspond to the connected state of the user's watch. If the user has a paired watch, but that watch
     * is out of range, this will still return true.
     *
     * This method must not be called from the main thread.
     *
     * @param context   The The Context used to connect to the wearable API.
     * @return          True, if the Wearable API is available, false otherwise.
     */
    public static boolean isWearableApiAvailable(final Context context) {
        if(Looper.myLooper()==Looper.getMainLooper()) {
            throw new IllegalStateException("isWearableApiAvailable can not be called from the UI thread");
        }
        if(WearableApis.hasAllMockApis()) {
            return true;
        }

        WearableApis.ensureApiClient(context);
        return WearableApis.googleApiClient!=null;
    }

    /**
     * Puts the given object to the specified path in the Wearable.DataApi.
     *
     * This can be called safely from any thread (it will occur asynchronously).
     *
     * @param context   The Context used to connect to the wearable API.
     * @param path      The path on which to place the data.
     * @param data      The object to serialize and send to the wearable API on the given path.
     */
    public static void deliverData(final Context context, final String path, final Object data) {
        WearableApis.makeWearableApiCall(context, DATA, new WearableApis.WearableApiRunnable() {
            @Override public void run(GoogleApiClient apiClient) {
                final PutDataRequest request = Packager.pack(path, data);
                WearableApis.DataApi.putDataItem(apiClient, request);
            }
        });
    }

    /**
     * Sends the given object as a message to all other connected devices
     *
     * This can be called safely from any thread (it will occur asynchronously).
     *
     * @param context   The Context used to connect to the wearable API.
     * @param path      The path on which to send the message.
     * @param data      The object to serialize and send.
     */
    public static void deliverMessage(final Context context, final String path, final Object data) {
        WearableApis.makeWearableApiCall(context, MESSAGE | NODE, new WearableApis.WearableApiRunnable() {
            @Override public void run(GoogleApiClient apiClient) {
                final byte[] bytes = Packager.packBytes(data);

                final List<Node> nodes = WearableApis.NodeApi.getConnectedNodes(apiClient).await().getNodes();
                for (Node node : nodes) {
                    WearableApis.MessageApi.sendMessage(apiClient, node.getId(), path, bytes);
                }
            }
        });
    }

    /**
     * Sends the given object as a message to a single connected device. If the destination device is
     * not connected, this message will not be sent.
     *
     * This can be called safely from any thread (it will occur asynchronously).
     *
     * @param context   The Context used to connect to the wearable API.
     * @param path      The path on which to send the message.
     * @param destinationNodeId The ID of the destination node.
     * @param data      The object to serialize and send.
     */
    public static void deliverMessage(final Context context, final String path, final String destinationNodeId, final Object data) {
        WearableApis.makeWearableApiCall(context, MESSAGE, new WearableApis.WearableApiRunnable() {
            @Override public void run(GoogleApiClient apiClient) {
                final byte[] bytes = Packager.packBytes(data);
                WearableApis.MessageApi.sendMessage(apiClient, destinationNodeId, path, bytes);
            }
        });
    }

    /**
     * Deletes all data items on the given path.
     *
     * This can be called safely from any thread (it will occur asynchronously).
     *
     * @param context The Context used to connect to the wearable API.
     * @param path The path on which to delete every data item.
     */
    public static void deleteData(final Context context, final String path) {
        deleteData(context, path, null);
    }

    /**
     * Deletes a single data item on the given path from the given node. If the target node
     * is disconnected, this will occur next time the node is connected to this device.
     *
     * This can be called safely from any thread (it will occur asynchronously).
     *
     * @param context The Context used to connect to the wearable API.
     * @param path The path on which to delete the data item
     * @param nodeId The node that created the data item to be removed.
     */
    public static void deleteData(final Context context, final String path, final String nodeId) {
        WearableApis.makeWearableApiCall(context, DATA, new WearableApis.WearableApiRunnable() {
            @Override public void run(GoogleApiClient apiClient) {
                final Uri.Builder uri = new Uri.Builder();
                uri.scheme("wear");
                uri.encodedPath(path);
                if (nodeId != null) {
                    uri.encodedAuthority(nodeId);
                }

                WearableApis.DataApi.deleteDataItems(apiClient, uri.build());
            }
        });
    }

    /**
     * Retrieves and returns a Node representing this device. This must not be called
     * on the main thread.
     *
     * @param context The Context used to connect to the wearable API.
     * @return a Node representing this device, or null if the Wearable API is unavailable.
     */
    @Nullable public static Node getLocalNode(final Context context) {
        if(Looper.myLooper()==Looper.getMainLooper()) {
            throw new IllegalStateException("getLocalNode can not be called from the UI thread");
        }

        if(WearableApis.hasMockNodeApi()) {
            return WearableApis.NodeApi.getLocalNode(null).await().getNode();
        }

        WearableApis.ensureApiClient(context);
        if (WearableApis.googleApiClient != null) {
            return WearableApis.NodeApi.getLocalNode(WearableApis.googleApiClient).await().getNode();
        } else {
            return null;
        }
    }

    /**
     * Retrieves and returns an InputStream for reading the data from an Asset. This must not be called
     * on the main thread.
     *
     * @param context The Context used to connect to the wearable API.
     * @param asset The asset to open a stream for.
     * @return An InputStream containing the data for the given asset, or null if the Wearable API is unavailable.
     */
    @Nullable public static InputStream getAssetInputStream(final Context context, final Asset asset) {
        if(Looper.myLooper()==Looper.getMainLooper()) {
            throw new IllegalStateException("getAssetInputStream can not be called from the UI thread");
        }

        if(WearableApis.hasMockDataApi()) {
            return WearableApis.DataApi.getFdForAsset(null, asset).await().getInputStream();
        }

        WearableApis.ensureApiClient(context);
        if(WearableApis.googleApiClient!=null) {
            return WearableApis.DataApi.getFdForAsset(WearableApis.googleApiClient, asset).await().getInputStream();
        } else {
            return null;
        }
    }

    /**
     * Starts receiving message, data, an device connection events on a target object.
     * Be sure to call {@link #stopReceiving} when you no longer want to receive updates.
     *
     * @param context The Context used to connect to the wearable API.
     * @param target The object to start receiving callbacks/bindings.
     */
    public static <T> void startReceiving(final Context context, final T target) {
        final DeliveryBoy<T> messenger = findDeliveryBoy(target.getClass());
        messenger.startReceiving(context, target);
    }

    /**
     * Starts receiving message, data, an device connection events on a target object.
     * Be sure to call {@link #stopReceiving} when you no longer want to receive updates.
     *
     * @param target The object to start receiving callbacks/bindings.
     */
    public static void startReceiving(final Activity target) {
        startReceiving(target, target);
    }

    /**
     * Starts receiving message, data, an device connection events on a target object.
     * Be sure to call {@link #stopReceiving} when you no longer want to receive updates.
     *
     * @param target The object to start receiving callbacks/bindings.
     */
    public static void startReceiving(final Fragment target) {
        startReceiving(target.getActivity(), target);
    }

    /**
     * Starts receiving message, data, an device connection events on a target object.
     * Be sure to call {@link #stopReceiving} when you no longer want to receive updates.
     *
     * @param target The object to start receiving callbacks/bindings.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void startReceiving(final android.app.Fragment target) {
        startReceiving(target.getActivity(), target);
    }

    /**
     * Starts receiving message, data, an device connection events on a target object.
     * Be sure to call {@link #stopReceiving} when you no longer want to receive updates.
     *
     * @param target The object to start receiving callbacks/bindings.
     */
    public static void startReceiving(final View target) {
        startReceiving(target.getContext(), target);
    }

    /**
     * Starts receiving message, data, an device connection events on a target object.
     * Be sure to call {@link #stopReceiving} when you no longer want to receive updates.
     *
     * @param target The object to start receiving callbacks/bindings.
     */
    public static void startReceiving(final Dialog target) {
        startReceiving(target.getContext(), target);
    }

    /**
     * Starts receiving message, data, an device connection events on a target object.
     * Be sure to call {@link #stopReceiving} when you no longer want to receive updates.
     *
     * @param target The object to start receiving callbacks/bindings.
     */
    public static void startReceiving(final Service target) {
        startReceiving(target, target);
    }

    /**
     * Stops receiving message, data, an device connection events on a target object.
     * Since callbacks are made asynchronously, it is possible that callbacks may occur
     * for a short time after this method is called.
     *
     * @param target The object to start receiving callbacks/bindings.
     */
    public static <T> void stopReceiving(final T target) {
        final DeliveryBoy<T> messenger = findDeliveryBoy(target.getClass());
        messenger.stopReceiving(target);
    }


    private static <T> DeliveryBoy<T> findDeliveryBoy(Class targetClass) {
        return findDeliveryBoy(targetClass, targetClass);
    }

    @SuppressWarnings("unchecked")
    private static <T> DeliveryBoy<T> findDeliveryBoy(Class targetClass, Class actualClass) {
        DeliveryBoy<T> messenger = DELIVERY_STAFF.get(targetClass);
        if(messenger!=null) {
            return messenger;
        }

        try {
            final String messengerClassName = targetClass.getName() + "$$Delivery";
            final Class messengerClass = Class.forName(messengerClassName);
            messenger = (DeliveryBoy<T>)messengerClass.newInstance();
        }catch (Exception e) {
            Class superClass = targetClass.getSuperclass();
            if(superClass==Object.class) {
                throw new IllegalStateException("Courier not found for "+actualClass.getName()+". Missing annotations?");
            } else {
                messenger = findDeliveryBoy(superClass, actualClass);
            }
        }

        DELIVERY_STAFF.put(targetClass, messenger);
        return messenger;
    }

    /**
     * Attach a mock DataApi for testing.
     *
     * @param mockDataApi A custom DataApi implementation to use instead of Wearable.DataApi, or null to revert back to Wearable.DataApi
     */
    public static void attachMockDataApi(@Nullable final DataApi mockDataApi) {
        if(mockDataApi==null) {
            WearableApis.DataApi = Wearable.DataApi;
        } else {
            WearableApis.DataApi = mockDataApi;
        }
    }

    /**
     * Attach a mock MessageApi for testing.
     *
     * @param mockMessageApi A custom MessageApi implementation to use instead of Wearable.MessageApi, or null to revert back to Wearable.MessageApi
     */
    public static void attachMockMessageApi(@Nullable final MessageApi mockMessageApi) {
        if(mockMessageApi==null) {
            WearableApis.MessageApi = Wearable.MessageApi;
        } else {
            WearableApis.MessageApi = mockMessageApi;
        }
    }

    /**
     * Attach a mock NodeApi for testing.
     *
     * @param mockNodeApi A custom NodeApi implementation to use instead of Wearable.NodeApi, or null to revert back to Wearable.NodeApi
     */
    public static void attachMockNodeApi(@Nullable final NodeApi mockNodeApi) {
        if(mockNodeApi==null) {
            WearableApis.NodeApi = Wearable.NodeApi;
        } else {
            WearableApis.NodeApi = mockNodeApi;
        }
    }

    // Don't allow instantiation
    private Courier(){}

}
