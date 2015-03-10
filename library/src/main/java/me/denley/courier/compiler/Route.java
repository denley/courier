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
        builder.append(indent).append(PostalArea.INDENT)
                .append("final Object unpacked = Packager.unpack(data);\n\n");

        for(Recipient recipient:recipients) {
            if(recipient.backgroundThread) {
                builder.append(indent).append(PostalArea.INDENT);
                recipient.writeDataBindingTo(builder);
            }
        }

        if(Recipient.hasMainThreadReceipient(recipients)) {
            builder.append(indent).append(PostalArea.INDENT).append("handler.post(new Runnable() {\n");
            builder.append(indent).append(PostalArea.INDENT_2).append("public void run() {\n");
            for (Recipient recipient : recipients) {
                if (!recipient.backgroundThread) {
                    builder.append(indent).append(PostalArea.INDENT_3);
                    recipient.writeDataBindingTo(builder);
                }
            }
            builder.append(indent).append(PostalArea.INDENT_2).append("}\n");
            builder.append(indent).append(PostalArea.INDENT).append("});\n");
        }

        builder.append(indent).append("}");
    }

}
