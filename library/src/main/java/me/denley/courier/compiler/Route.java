package me.denley.courier.compiler;

import java.util.LinkedHashSet;
import java.util.Set;

public class Route {

    public final String path;
    public final Set<Recipient> recipients = new LinkedHashSet<>();

    public Route(String path) {
        this.path = path;
    }

    public void writeTo(StringBuilder builder, String indent) {
        builder.append("if (path.equals(\"").append(path).append("\")) {\n");

        for(Recipient recipient:recipients) {
            builder.append(indent);
            builder.append("    ");
            recipient.writeTo(builder);
        }

        builder.append(indent).append("}");
    }

}
