package me.denley.courier.compiler;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import me.denley.courier.Courier;

public class PostalArea {

    public static final String INDENT = "    ";
    public static final String INDENT_2 = "        ";
    public static final String INDENT_3 = "            ";
    public static final String INDENT_4 = "                ";
    public static final String INDENT_5 = "                    ";



    private final String packageName;
    private final String targetClassName;

    public PostalArea(String packageName, String targetClassName) {
        this.packageName = packageName;
        this.targetClassName = targetClassName;
    }

    private final Map<String, Route> dataRoutes = new LinkedHashMap<>();
    private final Map<String, Route> messageRoutes = new LinkedHashMap<>();
    private final Set<Recipient> localNodeRecipients = new LinkedHashSet<>();

    public void addLocalNodeRecipient(Recipient recipient) {
        localNodeRecipients.add(recipient);
    }

    public Route getRoute(final String path, final boolean isData) {
        Route route = (isData?dataRoutes:messageRoutes).get(path);
        if(route==null) {
            route = new Route(path);
            (isData?dataRoutes:messageRoutes).put(path, route);
        }
        return route;
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
        builder.append("import com.google.android.gms.wearable.Wearable;\n");
        builder.append("\n");
        builder.append("import java.util.LinkedHashMap;\n");
        builder.append("import java.util.Map;\n");
        builder.append("\n");
        builder.append("import me.denley.courier.Courier;\n");
        builder.append("import me.denley.courier.Packager;\n");
        builder.append("\n");
        builder.append("import android.os.Handler;\n");
        builder.append("import android.os.Looper;\n");
        builder.append("\n");
    }

    private void writeClassDef(StringBuilder builder) {
        builder.append("public class ")
                .append(targetClassName)
                .append(Courier.CLASS_SUFFIX)
                .append(" <T extends ")
                .append(targetClassName)
                .append("> implements Courier.DeliveryBoy<T> {\n");
        builder.append(INDENT).append("GoogleApiClient apiClient = null;\n");
        builder.append(INDENT).append("Handler handler = new Handler(Looper.getMainLooper());\n\n");

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
        if(!dataRoutes.isEmpty()) {
            writeInitDataListenerMethod(builder);
            writeDeliverDataMethod(builder);
            writeInitDataMethod(builder);
        }
        builder.append("}\n");
    }

    private void writeListenerMaps(StringBuilder builder) {
        if(!messageRoutes.isEmpty()) {
            builder.append(INDENT).append("Map<T, MessageApi.MessageListener> messageListeners = new LinkedHashMap<T, MessageApi.MessageListener>();\n");
        }
        if(!dataRoutes.isEmpty()) {
            builder.append(INDENT).append("Map<T, NodeApi.NodeListener> nodeListeners = new LinkedHashMap<T, NodeApi.NodeListener>();\n");
            builder.append(INDENT).append("Map<T, DataApi.DataListener> dataListeners = new LinkedHashMap<T, DataApi.DataListener>();\n");
        }
        builder.append("\n");
    }

    private void writeStartReceivingMethod(StringBuilder builder) {
        builder.append(INDENT).append("public void startReceiving(final GoogleApiClient apiClient, final T target) {\n");
        builder.append(INDENT_2).append("this.apiClient = apiClient;\n");
        if(!localNodeRecipients.isEmpty()) {
            builder.append(INDENT_2).append("initLocalNodes(target);\n");
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
        builder.append(INDENT_2).append("if(apiClient==null) {\n");
        builder.append(INDENT_3).append("return;\n");
        builder.append(INDENT_2).append("}\n\n");
        if(!messageRoutes.isEmpty()) {
            builder.append(INDENT_2).append("MessageApi.MessageListener ml = messageListeners.remove(target);\n");
            builder.append(INDENT_2).append("if(ml!=null) {\n");
            builder.append(INDENT_3).append("Wearable.MessageApi.removeListener(apiClient, ml);\n");
            builder.append(INDENT_2).append("}\n\n");
        }
        if(!dataRoutes.isEmpty()) {
            builder.append(INDENT_2).append("DataApi.DataListener dl = dataListeners.remove(target);\n");
            builder.append(INDENT_2).append("if(dl!=null) {\n");
            builder.append(INDENT_3).append("Wearable.DataApi.removeListener(apiClient, dl);\n");
            builder.append(INDENT_2).append("}\n\n");

            builder.append(INDENT_2).append("NodeApi.NodeListener nl = nodeListeners.remove(target);\n");
            builder.append(INDENT_2).append("if(nl!=null) {\n");
            builder.append(INDENT_3).append("Wearable.NodeApi.removeListener(apiClient, nl);\n");
            builder.append(INDENT_2).append("}\n\n");
        }
        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitLocalNodesMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initLocalNodes(final T target) {\n");
        builder.append(INDENT_2).append("final Node localNode = Wearable.NodeApi.getLocalNode(apiClient)\n");
        builder.append(INDENT_4).append(".await().getNode();\n\n");
        builder.append(INDENT_2).append("handler.post(new Runnable() {\n");
        builder.append(INDENT_3).append("public void run() {\n");
        for(Recipient localNodeRecipient:localNodeRecipients) {
            builder.append(INDENT_4);
            localNodeRecipient.writeLocalNodeBindingTo(builder);
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
        builder.append(INDENT_2).append("Wearable.MessageApi.addListener(apiClient, ml);\n");

        builder.append(INDENT).append("}\n\n");
    }

    private void writeDeliverMessageMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void deliverMessage(final T target, final MessageEvent message) {\n");
        builder.append(INDENT_2).append("final String path = message.getPath();\n");
        builder.append(INDENT_2).append("final byte[] data = message.getData();\n\n");
        writeDataBindings(builder, messageRoutes);
        builder.append("\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitDataListenerMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initDataListener(final T target) {\n");
        builder.append(INDENT_2).append("final NodeApi.NodeListener nl = new NodeApi.NodeListener() {\n");
        builder.append(INDENT_3).append("@Override public void onPeerConnected(Node node) {\n");
        builder.append(INDENT_4).append("initializeData(target);\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_3).append("@Override public void onPeerDisconnected(Node node) {}\n");
        builder.append(INDENT_2).append("};\n");
        builder.append(INDENT_2).append("nodeListeners.put(target, nl);\n");
        builder.append(INDENT_2).append("Wearable.NodeApi.addListener(apiClient, nl);\n\n");
        builder.append(INDENT_2).append("final DataApi.DataListener dl = new DataApi.DataListener(){\n");
        builder.append(INDENT_3).append("@Override public void onDataChanged(DataEventBuffer dataEvents) {\n");
        builder.append(INDENT_4).append("for(DataEvent event:dataEvents) {\n");
        builder.append(INDENT_5).append("deliverData(target, event.getDataItem());\n");
        builder.append(INDENT_4).append("}\n");
        builder.append(INDENT_3).append("}\n");
        builder.append(INDENT_2).append("};\n");
        builder.append(INDENT_2).append("dataListeners.put(target, dl);\n");
        builder.append(INDENT_2).append("Wearable.DataApi.addListener(apiClient, dl);\n");
        builder.append(INDENT_2).append("initializeData(target);\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeDeliverDataMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void deliverData(final T target, final DataItem item) {\n");
        builder.append(INDENT_2).append("final String path = item.getUri().getPath();\n");
        builder.append(INDENT_2).append("final byte[] data = item.getData();\n\n");
        writeDataBindings(builder, dataRoutes);
        builder.append("\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeInitDataMethod(StringBuilder builder) {
        builder.append(INDENT).append("private void initializeData(T target) {\n");
        builder.append(INDENT_2).append("final DataItemBuffer existingItems = Wearable.DataApi.getDataItems(apiClient).await();\n");
        builder.append(INDENT_2).append("for(DataItem item:existingItems) {\n");
        builder.append(INDENT_3).append("deliverData(target, item);\n");
        builder.append(INDENT_2).append("}\n");
        builder.append(INDENT_2).append("existingItems.release();\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeDataBindings (StringBuilder builder, Map<String, Route> routes) {
        builder.append(INDENT_2);

        boolean startedIfBlock = false;
        for(Map.Entry<String, Route> entry : routes.entrySet()) {
            if(startedIfBlock) {
                builder.append(" else ");
            }
            startedIfBlock = true;

            entry.getValue().writeTo(builder, INDENT_2);
        }
    }

}
