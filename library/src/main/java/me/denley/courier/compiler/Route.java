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
        builder.append(indent).append("    ")
                .append("final Object unpacked = Packager.unpack(data);\n");
        builder.append(indent).append("    ").append("handler.post(new Runnable() {\n");
        builder.append(indent).append("        ").append("public void run() {\n");

        for(Recipient recipient:recipients) {
            builder.append(indent).append("            ");
            recipient.writeTo(builder);
        }

        builder.append(indent).append("        ").append("}\n");
        builder.append(indent).append("    ").append("});\n");
        builder.append(indent).append("}");
    }

}
