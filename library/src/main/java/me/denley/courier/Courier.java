package me.denley.courier;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public final class Courier {

    public static final String CLASS_SUFFIX = "$$Delivery";
    private static final Map<Class, DeliveryBoy> DELIVERY_STAFF = new LinkedHashMap<>();

    /** For use by generated code. Don't use this. */
    public interface DeliveryBoy<T> {
        public void startReceiving(GoogleApiClient apiClient, T target);
        public void stopReceiving(T target);
    }

    private interface WearableApiTask {
        public void run(GoogleApiClient apiClient);
    }


    public static void deliverData(final Context context, final String path, final Object data) {
        makeWearableApiCall(context, new WearableApiTask() {
            @Override public void run(GoogleApiClient apiClient) {
                final PutDataRequest request = Packager.pack(path, data);
                Wearable.DataApi.putDataItem(apiClient, request);
            }
        });
    }

    public static void deliverMessage(final Context context, final String path, final Object data) {
        makeWearableApiCall(context, new WearableApiTask() {
            @Override public void run(GoogleApiClient apiClient) {
                final byte[] bytes = Packager.pack(data);

                final List<Node> nodes = Wearable.NodeApi.getConnectedNodes(apiClient).await().getNodes();
                for (Node node : nodes) {
                    Wearable.MessageApi.sendMessage(apiClient, node.getId(), path, bytes);
                }
            }
        });
    }

    public static void deliverMessage(final Context context, final String path, final String destinationNodeId, final Object data) {
        makeWearableApiCall(context, new WearableApiTask() {
            @Override public void run(GoogleApiClient apiClient) {
                final byte[] bytes = Packager.pack(data);
                Wearable.MessageApi.sendMessage(apiClient, destinationNodeId, path, bytes);
            }
        });
    }

    public static void deleteData(final Context context, final String path) {
        deleteData(context, path, null);
    }

    public static void deleteData(final Context context, final String path, final String nodeId) {
        makeWearableApiCall(context, new WearableApiTask(){
            @Override public void run(GoogleApiClient apiClient) {
                final Uri.Builder uri = new Uri.Builder();
                uri.scheme("wear");
                uri.encodedPath(path);
                if(nodeId!=null) {
                    uri.encodedAuthority(nodeId);
                }

                Wearable.DataApi.deleteDataItems(apiClient, uri.build());
            }
        });
    }

    public static Node getLocalNode(final Context context) {
        if(Looper.myLooper()==Looper.getMainLooper()) {
            throw new IllegalStateException("getLocalNode can not be called from the UI thread");
        }

        final GoogleApiClient apiClient = new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();

        final ConnectionResult result = apiClient.blockingConnect();

        if(result.isSuccess()) {
            return Wearable.NodeApi.getLocalNode(apiClient).await().getNode();
        } else {
            return null;
        }
    }

    public static <T> void startReceiving(final Context context, final T target) {
        final DeliveryBoy<T> messenger = findDeliveryBoy(target);

        makeWearableApiCall(context, new WearableApiTask() {
            @Override public void run(GoogleApiClient apiClient) {
                messenger.startReceiving(apiClient, target);
            }
        });
    }

    public static <T> void stopReceiving(final T target) {
        final DeliveryBoy<T> messenger = findDeliveryBoy(target);
        messenger.stopReceiving(target);
    }


    public static void startReceiving(final Activity target) {
        startReceiving(target, target);
    }

    public static void startReceiving(final Fragment target) {
        startReceiving(target.getActivity(), target);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void startReceiving(final android.app.Fragment target) {
        startReceiving(target.getActivity(), target);
    }

    public static void startReceiving(final View target) {
        startReceiving(target.getContext(), target);
    }

    public static void startReceiving(final Dialog target) {
        startReceiving(target.getContext(), target);
    }

    public static void startReceiving(final Service target) {
        startReceiving(target, target);
    }

    private static void makeWearableApiCall(final Context context, final WearableApiTask task) {
        new Thread(){
            public void run() {
                final GoogleApiClient apiClient = new GoogleApiClient.Builder(context)
                        .addApi(Wearable.API)
                        .build();

                final ConnectionResult result = apiClient.blockingConnect();

                if(result.isSuccess()) {
                    task.run(apiClient);
                }
            }
        }.start();
    }

    @SuppressWarnings("unchecked")
    private static <T> DeliveryBoy<T> findDeliveryBoy(T target) {
        final Class targetClass = target.getClass();

        DeliveryBoy<T> messenger = DELIVERY_STAFF.get(targetClass);
        if(messenger!=null) {
            return messenger;
        }

        try {
            final String messengerClassName = targetClass.getName() + CLASS_SUFFIX;
            final Class messengerClass = Class.forName(messengerClassName);
            messenger = (DeliveryBoy<T>)messengerClass.newInstance();
        }catch (Exception e) {
            throw new IllegalStateException("Courier not found for "+targetClass.getName()+". Missing annotations?");
        }

        DELIVERY_STAFF.put(targetClass, messenger);
        return messenger;
    }


    // Don't allow instantiation
    private Courier(){}

}
