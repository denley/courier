package me.denley.courier;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.util.LinkedHashMap;
import java.util.Map;

public class Delivery<T extends Object> implements Courier.DeliveryBoy<T> {
    GoogleApiClient apiClient = null;

    Map<T, MessageApi.MessageListener> messageListeners = new LinkedHashMap<>();
    Map<T, NodeApi.NodeListener> nodeListeners = new LinkedHashMap<>();
    Map<T, DataApi.DataListener> dataListeners = new LinkedHashMap<>();

    public void startReceiving(final GoogleApiClient apiClient, final T target) {
        this.apiClient = apiClient;
        initMessageListener(target);
        initDataListener(target);
    }

    public void stopReceiving(T target) {
        if(apiClient==null) {
            return;
        }

        MessageApi.MessageListener ml = messageListeners.remove(target);
        if(ml!=null) {
            Wearable.MessageApi.removeListener(apiClient, ml);
        }

        DataApi.DataListener dl = dataListeners.remove(target);
        if(dl!=null) {
            Wearable.DataApi.removeListener(apiClient, dl);
        }

        NodeApi.NodeListener nl = nodeListeners.remove(target);
        if(nl!=null) {
            Wearable.NodeApi.removeListener(apiClient, nl);
        }
    }



    private void initMessageListener(final T target) {
        final MessageApi.MessageListener ml = new MessageApi.MessageListener() {
            @Override public void onMessageReceived(MessageEvent messageEvent) {
                deliverMessage(target, messageEvent);
            }
        };
        messageListeners.put(target, ml);
        Wearable.MessageApi.addListener(apiClient, ml);
    }

    private void deliverMessage(T target, MessageEvent message) {
        final String path = message.getPath();
        final byte[] data = message.getData();


    }



    private void initDataListener(final T target) {
        final NodeApi.NodeListener nl = new NodeApi.NodeListener() {
            @Override public void onPeerConnected(Node node) {
                initializeData(target);
            }
            @Override public void onPeerDisconnected(Node node) {}
        };
        nodeListeners.put(target, nl);
        Wearable.NodeApi.addListener(apiClient, nl);

        final DataApi.DataListener dl = new DataApi.DataListener(){
            @Override public void onDataChanged(DataEventBuffer dataEvents) {
                for(DataEvent event:dataEvents) {
                    deliverData(target, event.getDataItem());
                }
            }
        };
        dataListeners.put(target, dl);
        Wearable.DataApi.addListener(apiClient, dl);
        initializeData(target);
    }

    private void deliverData(T target, DataItem item) {
        final String path = item.getUri().getPath();
        final byte[] data = item.getData();


    }



    private void initializeData(T target) {
        final Node localNode = Wearable.NodeApi.getLocalNode(apiClient).await().getNode();
        final DataItemBuffer existingItems = Wearable.DataApi.getDataItems(apiClient).await();
        for(DataItem item:existingItems) {
            if(!item.getUri().getHost().equals(localNode.getId())) {
                deliverData(target, item);
            }
        }
        existingItems.release();
    }

}

