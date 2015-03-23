package me.denley.courier.compiler;

import java.util.LinkedHashSet;
import java.util.Set;

class Route {

    public final String path;
    public final Set<Recipient> recipients = new LinkedHashSet<>();

    public Route(String path) {
        this.path = path;
    }

    public void writeTo(StringBuilder builder, String indent, String dataVariable) {
        builder.append("if (path.equals(\"").append(path).append("\")) {\n");

        for(String type:getTargetTypes()) {
            final String name = "as_"+type.replace(".", "_");
            builder.append(indent).append(PostalArea.INDENT).append("final ")
                    .append(type).append(" ").append(name)
                    .append(" = Packager.unpack(")
                    .append(dataVariable).append(", ")
                    .append(type).append(".class")
                    .append(");\n\n");
        }

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

    private Set<String> getTargetTypes() {
        final Set<String> types = new LinkedHashSet<>();
        for(Recipient recipient:recipients) {
            if(!types.contains(recipient.payloadType)) {
                types.add(recipient.payloadType);
            }
        }
        return types;
    }

}
