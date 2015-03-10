package me.denley.courier.compiler;

import java.util.Set;

import javax.lang.model.element.ElementKind;

public class Recipient {

    public static boolean hasMainThreadReceipient(Set<Recipient> recipients) {
        for(Recipient r:recipients) {
            if(!r.backgroundThread) {
                return true;
            }
        }

        return false;
    }


    public final String recipientName;
    public final String payloadType;

    public final ElementKind deliveryType;
    public final boolean hasNodeParameter;
    public final boolean backgroundThread;


    public Recipient(String name, String payload) {
        this.recipientName = name;
        this.deliveryType = ElementKind.FIELD;
        this.payloadType = payload;
        this.backgroundThread = true;
        this.hasNodeParameter = false;
    }

    public Recipient(String name, String payload, boolean hasNodeParameter, boolean backgroundThread) {
        this.recipientName = name;
        this.deliveryType = ElementKind.METHOD;
        this.payloadType = payload;
        this.hasNodeParameter = hasNodeParameter;
        this.backgroundThread = backgroundThread;
    }

    public void writeDataBindingTo(StringBuilder builder) {
        final String name = "as_"+payloadType.replace(".", "_");
        writeBindingTo(builder, name);
    }

    public void writeLocalNodeBindingTo(StringBuilder builder) {
        writeBindingTo(builder, "localNode");
    }

    public void writeRemoteNodeBindingTo(StringBuilder builder) {
        writeBindingTo(builder, "nodes");
    }

    private void writeBindingTo(StringBuilder builder, String sourceName) {
        builder.append("target.");
        builder.append(recipientName);

        if(deliveryType==ElementKind.METHOD){
            builder.append("(").append(sourceName);
            if(hasNodeParameter) {
                builder.append(", node");
            }
            builder.append(")");
        } else {
            builder.append(" = ").append(sourceName);
        }

        builder.append(";\n");
    }

}