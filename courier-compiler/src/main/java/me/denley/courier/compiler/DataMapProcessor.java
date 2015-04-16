package me.denley.courier.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import me.denley.courier.Deliverable;

public class DataMapProcessor extends AbstractProcessor {

    public static final String CLASS_SUFFIX = "$$DataMapPackager";

    private static final String BITMAP = "android.graphics.Bitmap";

    private static final String INDENT = "    ";
    private static final String INDENT_2 = "        ";
    private static final String INDENT_3 = "            ";

    Set<String> targetClassNames = new LinkedHashSet<String>();


    @Override public Set<String> getSupportedAnnotationTypes() {
        final Set<String> types = new LinkedHashSet<String>();
        types.add(Deliverable.class.getName());
        return types;
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processDataMapElements(roundEnv);
        return true;
    }

    private void processDataMapElements(RoundEnvironment roundEnv) {
        final Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Deliverable.class);
        for(Element element:annotatedElements) {
            targetClassNames.add(element.toString());
        }
        for(Element element:annotatedElements) {
            processDataMapElementOrFail(element);
        }
    }

    private void processDataMapElementOrFail(Element element) {
        try {
            processDataMapElement(element);
        } catch (IllegalArgumentException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
        }
    }

    private void processDataMapElement(Element element) {
        checkForErrors(element);
        writeClassForElement((TypeElement) element);
    }

    private void checkForErrors(Element element) {
        if(element.getKind()!=ElementKind.CLASS) {
            throw new IllegalArgumentException("@Deliverable only applies to classes");
        }
    }

    private void writeClassForElement(TypeElement element) {
        try {
            JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                    getClassOfElement(element) + CLASS_SUFFIX,
                    element);
            Writer writer = jfo.openWriter();

            final StringBuilder builder = new StringBuilder();
            writeClass(builder, element);
            writer.write(builder.toString());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
        }
    }

    private String getParentClassName(TypeElement typeElement) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (targetClassNames.contains(typeElement.toString())) {
                return getClassOfElement(typeElement);
            }
        }
    }

    private String getClassOfElement(TypeElement element) {
        final String packageName = getPackageName(element);
        final String targetClassName = element
                .getQualifiedName()
                .toString()
                .substring(packageName.length() + 1)
                .replace('.', '$');
        return packageName + "." + targetClassName;
    }

    private String getPackageName(TypeElement element) {
        return processingEnv
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
    }



    private void writeClass(StringBuilder builder, TypeElement element) {
        final String parentClass = getParentClassName(element);

        builder.append("package ").append(getPackageName(element)).append(";\n\n");
        builder.append("import me.denley.courier.Courier;\n");
        builder.append("import me.denley.courier.Packager;\n");
        builder.append("import me.denley.courier.Packager.DataPackager;\n");
        builder.append("import com.google.android.gms.wearable.DataMap;\n");
        builder.append("import com.google.android.gms.wearable.Asset;\n");
        builder.append("import android.content.Context;\n");
        builder.append("import android.graphics.Bitmap;\n");
        builder.append("import android.graphics.BitmapFactory;\n");
        builder.append("import java.io.InputStream;\n");
        builder.append("import java.io.ByteArrayOutputStream;\n\n");

        writeClassDef(builder, element, parentClass);
    }

    private void writeClassDef(StringBuilder builder, TypeElement element, String parentClass) {
        final String targetClassName = element.getSimpleName().toString();

        builder.append("public class ").append(targetClassName).append(CLASS_SUFFIX)
                .append("<T extends ").append(targetClassName).append("> ");

        if(parentClass==null) {
            builder.append("implements DataPackager<T>");
        } else {
            builder.append("extends ").append(parentClass).append(CLASS_SUFFIX).append("<T>");
        }
        builder.append(" {\n\n");

        if(parentClass==null) {
            writePackInterfaceMethod(builder);
        }
        writeUnpackInterfaceMethod(builder, targetClassName);

        writePackImplMethod(builder, element, parentClass);
        writeUnpackImplMethod(builder, element, parentClass);

        builder.append("}\n");
    }

    private void writePackInterfaceMethod(StringBuilder builder) {
        builder.append(INDENT).append("public DataMap pack(T target) {\n");
        builder.append(INDENT_2).append("final DataMap map = new DataMap();\n");
        builder.append(INDENT_2).append("pack(target, map);\n");
        builder.append(INDENT_2).append("return map;\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writeUnpackInterfaceMethod(StringBuilder builder, String targetClassName) {
        builder.append(INDENT).append("public T unpack(Context context, DataMap map) {\n");
        builder.append(INDENT_2).append("final T target = (T) new ").append(targetClassName).append("();\n");
        builder.append(INDENT_2).append("unpack(context, map, target);\n");
        builder.append(INDENT_2).append("return target;\n");
        builder.append(INDENT).append("}\n\n");
    }

    private void writePackImplMethod(StringBuilder builder, TypeElement element, String parentClass) {
        if(parentClass!=null) {
            builder.append(INDENT).append("@Override\n");
        }
        builder.append(INDENT).append("public void pack(T target, DataMap map) {\n");
        if(parentClass!=null) {
            builder.append(INDENT_2).append("super.pack(target, map);\n");
        }

        for(Element subElement:element.getEnclosedElements()) {
            final Set<Modifier> modifiers = subElement.getModifiers();
            final String name = subElement.getSimpleName().toString();

            if(subElement.getKind()==ElementKind.FIELD
                    && !modifiers.contains(Modifier.PRIVATE)
                    && !modifiers.contains(Modifier.STATIC)
                    && !modifiers.contains(Modifier.FINAL)
                    ) {

                final DataMapElementType elementType = DataMapElementType.getElementType(subElement);
                if(elementType==null) {
                    final String fieldType = subElement.asType().toString();
                    if(targetClassNames.contains(fieldType)) {
                        builder.append(INDENT_2).append("map.putDataMap(\"").append(name).append("\", ")
                                .append("Packager.pack(target.").append(name).append("));\n");
                    } else if (fieldType.equals(BITMAP)) {
                        builder.append(INDENT_2).append("if(target.").append(name).append("!=null) {\n");
                        builder.append(INDENT_3).append("final ByteArrayOutputStream ").append(name).append("ByteArrayOutputStream = new ByteArrayOutputStream();\n");
                        builder.append(INDENT_3).append("target.").append(name).append(".compress(Bitmap.CompressFormat.PNG, 100, ").append(name).append("ByteArrayOutputStream);\n");
                        builder.append(INDENT_3).append("final Asset ").append(name).append("Asset = Asset.createFromBytes(").append(name).append("ByteArrayOutputStream.toByteArray());\n");
                        builder.append(INDENT_3).append("map.putAsset(\"").append(name).append("\", ").append(name).append("Asset);\n");
                        builder.append(INDENT_2).append("}\n");
                    } else {
                        // Bad field type, show an error linking to this specific element
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Field type not supported ("+fieldType+").", subElement);
                    }
                } else {
                    builder.append(INDENT_2).append("map.").append(elementType.putMethod)
                            .append("(\"").append(name).append("\", target.").append(name).append(");\n");
                }
            }
        }

        builder.append(INDENT).append("}\n\n");
    }

    private void writeUnpackImplMethod(StringBuilder builder, TypeElement element, String parentClass) {
        if(parentClass!=null) {
            builder.append(INDENT).append("@Override\n");
        }
        builder.append(INDENT).append("protected void unpack(Context context, DataMap map, T target) {\n");
        if(parentClass!=null) {
            builder.append(INDENT_2).append("super.unpack(context, map, target);\n");
        }

        for(Element subElement:element.getEnclosedElements()) {
            final Set<Modifier> modifiers = subElement.getModifiers();
            final String name = subElement.getSimpleName().toString();

            if(subElement.getKind()==ElementKind.FIELD
                    && !modifiers.contains(Modifier.PRIVATE)
                    && !modifiers.contains(Modifier.STATIC)
                    && !modifiers.contains(Modifier.FINAL)
                    ) {

                final DataMapElementType elementType = DataMapElementType.getElementType(subElement);
                if(elementType==null) {
                    final String fieldType = subElement.asType().toString();

                    if(targetClassNames.contains(fieldType)) {
                        builder.append(INDENT_2).append("target.").append(name).append(" = ");
                        builder.append("Packager.unpack(")
                                .append("map.getDataMap(\"").append(name)
                                .append("\"), ")
                                .append(fieldType).append(".class);\n");
                    } else if (fieldType.equals(BITMAP)) {
                        builder.append(INDENT_2).append("final Asset ").append(name).append("Asset = map.getAsset(\"").append(name).append("\");\n");
                        builder.append(INDENT_2).append("if(").append(name).append("Asset!=null && context!=null) {\n");
                        builder.append(INDENT_3).append("final InputStream in = Courier.getAssetInputStream(context, ").append(name).append("Asset);\n");
                        builder.append(INDENT_3).append("target.").append(name).append(" = BitmapFactory.decodeStream(in);\n");
                        builder.append(INDENT_2).append("}\n");
                    } else {
                        // Bad field type, show an error linking to this specific element
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Field type not supported ("+fieldType+").", subElement);
                    }
                } else {
                    builder.append(INDENT_2).append("target.").append(name).append(" = ");
                    builder.append("map.").append(elementType.getMethod)
                            .append("(\"").append(name).append("\");\n");
                }
            }
        }

        builder.append(INDENT).append("}\n\n");
    }



}
