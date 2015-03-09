package me.denley.courier.compiler;

import javax.lang.model.element.ElementKind;

public class Recipient {

    public final String recipientName;
    public final String payloadType;

    public final ElementKind deliveryType;
    public boolean hasNodeParameter = false;


    public Recipient(String name, ElementKind type, String payload) {
        this.recipientName = name;
        this.deliveryType = type;
        this.payloadType = payload;
    }

    public Recipient(String name, ElementKind type, String payload, boolean hasNodeParameter) {
        this.recipientName = name;
        this.deliveryType = type;
        this.payloadType = payload;
        this.hasNodeParameter = hasNodeParameter;
    }

    public void writeDataBindingTo(StringBuilder builder) {
        writeBindingTo(builder, "("+payloadType+")unpacked");
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