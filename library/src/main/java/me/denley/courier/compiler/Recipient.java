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

    public void writeTo(StringBuilder builder) {
        builder.append("target.");
        builder.append(recipientName);

        if(deliveryType==ElementKind.METHOD){
            builder.append("((")
                    .append(payloadType)
                    .append(")unpacked)");
        } else {
            builder.append(" = (")
                    .append(payloadType)
                    .append(")unpacked");
        }

        builder.append(";\n");
    }

}