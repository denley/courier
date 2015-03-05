package me.denley.courier.compiler;

import javax.lang.model.element.ElementKind;

public class Recipient {

    public final String recipientName;
    public final ElementKind deliveryType;
    public final String payloadType;

    public Recipient(String name, ElementKind type, String payload) {
        this.recipientName = name;
        this.deliveryType = type;
        this.payloadType = payload;
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
            builder.append("(").append(sourceName).append(")");
        } else {
            builder.append(" = ").append(sourceName);
        }

        builder.append(";\n");
    }

}