package me.denley.courier.compiler;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class PostalArea {

    public static final String INDENT = "    ";
    public static final String INDENT_2 = "        ";
    public static final String INDENT_3 = "            ";
    public static final String INDENT_4 = "                ";
    public static final String INDENT_5 = "                    ";
    public static final String INDENT_6 = "                        ";



    private final String packageName;
    private final String targetClassName;
    private String parentClass = null;

    public PostalArea(String packageName, String targetClassName) {
        this.packageName = packageName;
        this.targetClassName = targetClassName;
    }

    private final Map<String, Route> dataRoutes = new LinkedHashMap<String, Route>();
    private final Map<String, Route> messageRoutes = new LinkedHashMap<String, Route>();
    private final Set<Recipient> localNodeRecipients = new LinkedHashSet<Recipient>();
    private final Set<Recipient> remoteNodeRecipients = new LinkedHashSet<Recipient>();

    public void addLocalNodeRecipient(Recipient recipient) {
        localNodeRecipients.add(recipient);
    }

    public void addRemoteNodeRecipient(Recipient recipient) {
        remoteNodeRecipients.add(recipient);
    }

    public Route getRoute(final String path, final boolean isData) {
        Route route = (isData?dataRoutes:messageRoutes).get(path);
        if(route==null) {
            route = new Route(path);
            (isData?dataRoutes:messageRoutes).put(path, route);
        }
        return route;
    }

    public void setParent(String parentClass) {
        this.parentClass = parentClass;
    }

    public String getTargetClassName() {
        return packageName + "." + targetClassName;
    }

    public String writeJava() {
        final StringBuilder builder = new StringBuilder();
        builder.append("package ").append(packageName).append(";\n\n");
        writeImports(builder);
        writeClassDef(builder);
        return builder.toString();
    }

    private void writeImports(StringBuilder builder) {
        builder.append("import com.google.android.gms.common.api.GoogleApiClient;\n");
        builder.append("import com.google.android.gms.wearable.DataApi;\n");
        builder.append("import com.google.android.gms.wearable.DataEvent;\n");
        builder.append("import com.google.android.gms.wearable.DataEventBuffer;\n");
        builder.append("import com.google.android.gms.wearable.DataItem;\n");
        builder.append("import com.google.android.gms.wearable.DataItemBuffer;\n");
        builder.append("import com.google.android.gms.wearable.MessageApi;\n");
        builder.append("import com.google.android.gms.wearable.MessageEvent;\n");
        builder.append("import com.google.android.gms.wearable.Node;\n");
        builder.append("import com.google.android.gms.wearable.NodeApi;\n");
        builder.append("\n");
        builder.append("import java.util.LinkedHashMap;\n");
        builder.append("import java.util.Map;\n");
        builder.append("import java.util.List;\n");
        builder.append("\n");
        builder.append("import me.denley.courier.Courier;\n");
        builder.append("import me.denley.courier.Packager;\n");
        builder.append("import me.denley.courier.WearableApis;\n");
        builder.append("\n");
        builder.append("import android.os.Handler;\n");
        builder.append("import android.os.Looper;\n");
        builder.append("import android.content.Context;\n");
        builder.append("\n");
        builder.append("import static me.denley.courier.WearableApis.NODE;\n");
        builder.append("import static me.denley.courier.WearableApis.DATA;\n");
        builder.append("import static me.denley.courier.WearableApis.MESSAGE;\n");
        builder.append("\n");
    }

    private void writeClassDef(StringBuilder builder) {
        builder.append("public class ").append(targetClassName).append(Processor.CLASS_SUFFIX)
                .append("<T extends ").append(targetClassName.replace('$','.')).append(">");

        if(parentClass==null) {
            builder.append(" implements Courier.DeliveryBoy<T>");
        } else {
            builder.append(" extends ").append(parentClass).append("<T>");
        }

        builder.append(" {\n");
        builder.append(INDENT).append("private Context context;\n");
        builder.append(INDENT).append("private Handler handler = new Handler(Looper.getMainLooper());\n\n");

        writeListenerMaps(builder);
        writeStartReceivingMethod(builder);
        writeStopReceivingMethod(builder);
        if(!localNodeRecipients.isEmpty()) {
            writeInitLocalNodesMethod(builder);
        }
        if(!messageRoutes.isEmpty()) {
            writeInitMessageListenerMethod(builder);
            writeDeliverMessageMethod(builder);
        }
        if(!remoteNodeRecipients.isEmpty() || !dataRoutes.isEmpty()) {
            writeInitNodeListenerMethod(builder);
            writeDeliverRemoteNodesMethod(builder);
        }
        if(!dataRoutes.isEmpty()) {
            writeInitDataListenerMethod(builder);
            writeDeliverDataMethod(builder);
            writeInitDataMethod(builder);
        }
        builder.append("}\n");
    }

    private void writeListenerMaps(StringBuilder builder) {
        if(!messageRoutes.isEmpty()) {
            builder.append(INDENT).append("private Map<T, MessageApi.MessageListener> messageListeners = new LinkedHashMap<T, MessageApi.MessageListener>();\n");
        }
        if(!dataRoutes.isEmpty()) {
            builder.append(INDENT).append("private Map<T, DataApi.DataListener> dataListeners = new LinkedHashMap<T, DataApi.DataListener>();\n");
        }
        if(!remoteNodeRecipients.isEmpty() || !dataRoutes.isEmpty()) {
            builder.append(INDENT).append("private Map<T, NodeApi.NodeListener> nodeListeners = new LinkedHashMap<T, NodeApi.NodeListener>();\n");
        }
        builder.append("\n");
    }

    private void writeStartReceivingMethod(StringBuilder builder) {
        builder.append(INDENT).append("public void startReceiving(final Context context, final T target) {\n");
        if(parentClass!=null) {
            builder.append(INDENT_2).append("super.startReceiving(context, target);\n");
        }

        builder.append(INDENT_2).append("this.context = context;\n");
        if(!localNodeRecipients.isEmpty()) {
            builder.append(INDENT_2).append("initLocalNodes(target);\n");
        }
        if(!remoteNodeRecipients.isEmpty() || !dataRoutes.isEmpty()) {
            builder.append(INDENT_2).append("initNodeListener(target);\n");
        }
        if(!messageRoutes.isEmpty()) {
            builder.append(INDENT_2).append("initMessageListener(target);\n");
        }
        if(!dataRoutes.isEmpty()) {
            builder.append(INDENT_2).append("initDataListener(target);\n");
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void writeStopReceivingMethod(StringBuilder builder) {
        builder.append(INDENT).append("public void stopReceiving(T target) {\n");
        if(parentClass!=null) {
            builder.append(INDENT_2).append("super.stopReceiving(target);\n");
        }
        builder.append(INDENT_2).append("GoogleApiClient apiClient = WearableApis.googleApiClient;\n");
        builder.append(INDENT_2).append("if(apiClient==null) {\n");
        builder.append(INDENT_3).append("return;\n");
        builder.append(INDENT_2).append("}\n\n");

        if(!messageRoutes.isEmpty()) {
            builder.append(INDENT_2).append("MessageApi.MessageListener ml = messageListeners.remove(target);\n");
            builder.append(INDENT_2).append("if(ml!=null) {\n");
            builder.append(INDENT_3).append("WearableApis.getMessageApi().removeListener(apiClient, ml);\n");
            builder.append(INDENT_2).append("}\n\n");
        }
        if(!dataRoutes.isEmpty()) {
            builder.append(INDENT_2).append("DataApi.DataListener dl = dataListeners.remove(target);\n");
            builder.append(INDENT_2).append("if(dl!=null) {\n");
            builder.append(INDENT_3).append("WearableApis.getDataApi().removeListener(apiClient, dl);\n");
            builder.append(INDENT_2).append("}\n\n");
        }
        if(!remoteNodeRecipients.isEmpty() || !dataRoutes.isEmpty()) {
            builder.append(INDENT_2).append("NodeApi.NodeListener nl = nodeListeners.remove(target);\n");
            builder.append(INDENT_2).append("if(nl!=null) {\n");
            builder.append(INDENT_3).append("WearableApis.getNodeApi().removeListener(apiClient, nl);\n");
            builder.append(INDENT_2).append("}\n\n");
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitLocalNodesMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initLocalNodes(final T target) {\n");
        builder.append(INDENT_2).append("WearableApis.makeWearableApiCall(context, NODE, new WearableApis.WearableApiRunnable() {\n");
        builder.append(INDENT_3).append("public void run(GoogleApiClient apiClient){\n");
        builder.append(INDENT_4).append("final Node localNode = WearableApis.getNodeApi().getLocalNode(apiClient).await().getNode();\n");


        for(Recipient localNodeRecipient:localNodeRecipients) {
            if(localNodeRecipient.backgroundThread) {
                builder.append(INDENT_4);
                localNodeRecipient.writeLocalNodeBindingTo(builder);
            }
        }

        if(Recipient.hasMainThreadReceipient(localNodeRecipients)) {
            builder.append(INDENT_4).append("handler.post(new Runnable() {\n");
            builder.append(INDENT_5).append("public void run() {\n");
            for (Recipient localNodeRecipient : localNodeRecipients) {
                if (!localNodeRecipient.backgroundThread) {
                    builder.append(INDENT_6);
                    localNodeRecipient.writeLocalNodeBindingTo(builder);
                }
            }
            builder.append(INDENT_5).append("}\n");
            builder.append(INDENT_4).append("});\n");
        }


        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("});\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitMessageListenerMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initMessageListener(final T target) {\n");
        builder.append(INDENT_2).append("final MessageApi.MessageListener ml = new MessageApi.MessageListener() {\n");
        builder.append(INDENT_3).append("@Override public void onMessageReceived(MessageEvent messageEvent) {\n");
        builder.append(INDENT_4).append("deliverMessage(target, messageEvent);\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("};\n\n");

        builder.append(INDENT_2).append("messageListeners.put(target, ml);\n");
        builder.append(INDENT_2).append("WearableApis.makeWearableApiCall(context, MESSAGE, new WearableApis.WearableApiRunnable() {\n");
        builder.append(INDENT_3).append("public void run(GoogleApiClient apiClient){\n");
        builder.append(INDENT_4).append("WearableApis.getMessageApi().addListener(apiClient, ml);\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("});\n");

        builder.append(INDENT).append("}\n\n");
    }

    private void writeDeliverMessageMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void deliverMessage(final T target, final MessageEvent message) {\n");
        builder.append(INDENT_2).append("final String path = message.getPath();\n");
        builder.append(INDENT_2).append("final byte[] data = message.getData();\n");
        builder.append(INDENT_2).append("final String node = message.getSourceNodeId();\n\n");
        writeDataBindings(builder, messageRoutes, "data");
        builder.append("\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitNodeListenerMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initNodeListener(final T target) {\n");
        builder.append(INDENT_2).append("final NodeApi.NodeListener nl = new NodeApi.NodeListener() {\n");
        builder.append(INDENT_3).append("@Override public void onPeerConnected(Node node) {\n");
        builder.append(INDENT_4).append("deliverRemoteNodes(target);\n");
        if(!dataRoutes.isEmpty()) {
            builder.append(INDENT_4).append("initializeData(target);\n");
        }
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_3).append("@Override public void onPeerDisconnected(Node node) {\n");
        builder.append(INDENT_4).append("deliverRemoteNodes(target);\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("};\n");
        builder.append(INDENT_2).append("nodeListeners.put(target, nl);\n");

        builder.append(INDENT_2).append("WearableApis.makeWearableApiCall(context, NODE, new WearableApis.WearableApiRunnable() {\n");
        builder.append(INDENT_3).append("public void run(GoogleApiClient apiClient){\n");
        builder.append(INDENT_4).append("WearableApis.getNodeApi().addListener(apiClient, nl);\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("});\n");

        if(!remoteNodeRecipients.isEmpty()) {
            builder.append(INDENT_2).append("deliverRemoteNodes(target);\n");
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void writeDeliverRemoteNodesMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void deliverRemoteNodes(final T target) {\n");

        builder.append(INDENT_2).append("WearableApis.makeWearableApiCall(context, NODE, new WearableApis.WearableApiRunnable() {\n");
        builder.append(INDENT_3).append("public void run(GoogleApiClient apiClient){\n");

        builder.append(INDENT_4).append("final List<Node> nodes = WearableApis.getNodeApi().getConnectedNodes(apiClient).await().getNodes();\n\n");

        for(Recipient localNodeRecipient:remoteNodeRecipients) {
            if(localNodeRecipient.backgroundThread) {
                builder.append(INDENT_4);
                localNodeRecipient.writeRemoteNodeBindingTo(builder);
            }
        }

        if(Recipient.hasMainThreadReceipient(remoteNodeRecipients)) {
            builder.append(INDENT_4).append("handler.post(new Runnable() {\n");
            builder.append(INDENT_5).append("public void run() {\n");
            for (Recipient localNodeRecipient : remoteNodeRecipients) {
                if (!localNodeRecipient.backgroundThread) {
                    builder.append(INDENT_6);
                    localNodeRecipient.writeRemoteNodeBindingTo(builder);
                }
            }
            builder.append(INDENT_5).append("}\n");
            builder.append(INDENT_4).append("});\n");
        }

        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("});\n");

        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitDataListenerMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initDataListener(final T target) {\n");
        builder.append(INDENT_2).append("final DataApi.DataListener dl = new DataApi.DataListener(){\n");
        builder.append(INDENT_3).append("@Override public void onDataChanged(DataEventBuffer dataEvents) {\n");
        builder.append(INDENT_4).append("for(DataEvent event:dataEvents) {\n");
        builder.append(INDENT_5).append("deliverData(target, event.getDataItem());\n");
        builder.append(INDENT_4).append("}\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("};\n");
        builder.append(INDENT_2).append("dataListeners.put(target, dl);\n");
        builder.append(INDENT_2).append("WearableApis.makeWearableApiCall(context, DATA, new WearableApis.WearableApiRunnable() {\n");
        builder.append(INDENT_3).append("public void run(GoogleApiClient apiClient){\n");
        builder.append(INDENT_4).append("WearableApis.getDataApi().addListener(apiClient, dl);\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("});\n");
        builder.append(INDENT_2).append("initializeData(target);\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeDeliverDataMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void deliverData(final T target, final DataItem item) {\n");
        builder.append(INDENT_2).append("final String path = item.getUri().getPath();\n");
        builder.append(INDENT_2).append("final byte[] data = item.getData();\n");
        builder.append(INDENT_2).append("final String node = item.getUri().getHost();\n\n");

        writeDataBindings(builder, dataRoutes, "item");
        builder.append("\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitDataMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initializeData(final T target) {\n");
        builder.append(INDENT_2).append("WearableApis.makeWearableApiCall(context, DATA, new WearableApis.WearableApiRunnable() {\n");
        builder.append(INDENT_3).append("public void run(GoogleApiClient apiClient){\n");

        builder.append(INDENT_4).append("final DataItemBuffer existingItems = WearableApis.getDataApi().getDataItems(apiClient).await();\n");
        builder.append(INDENT_4).append("for(DataItem item:existingItems) {\n");
        builder.append(INDENT_5).append("deliverData(target, item);\n");
        builder.append(INDENT_4).append("}\n");
        builder.append(INDENT_4).append("existingItems.release();\n");

        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("});\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeDataBindings (StringBuilder builder, Map<String, Route> routes, String dataVariable) {
        builder.append(INDENT_2);

        boolean startedIfBlock = false;
        for(Map.Entry<String, Route> entry : routes.entrySet()) {
            if(startedIfBlock) {
                builder.append(" else ");
            }
            startedIfBlock = true;

            entry.getValue().writeTo(builder, INDENT_2, dataVariable);
        }
    }

}
