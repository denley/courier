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

    private static final String INDENT = "    ";
    private static final String INDENT_2 = "        ";


    Set<String> targetClassNames = new LinkedHashSet<>();


    @Override public Set<String> getSupportedAnnotationTypes() {
        final Set<String> types = new LinkedHashSet<>();
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
            throw new IllegalArgumentException("@DataMap only applies to classes");
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
        builder.append("import me.denley.courier.Packager;\n");
        builder.append("import me.denley.courier.Packager.DataPackager;\n");
        builder.append("import com.google.android.gms.wearable.DataMap;\n\n");
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
        builder.append(INDENT).append("public T unpack(DataMap map) {\n");
        builder.append(INDENT_2).append("final T target = (T) new ").append(targetClassName).append("();\n");
        builder.append(INDENT_2).append("unpack(map, target);\n");
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
                    if(targetClassNames.contains(subElement.asType().toString())) {
                        builder.append(INDENT_2).append("map.putDataMap(\"").append(name).append("\", ")
                                .append("Packager.pack(target.").append(name).append("));\n");
                    } else {
                        throw new IllegalArgumentException("Field type ("+subElement.asType().toString()+") is not mappable to a DataMap.");
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
        builder.append(INDENT).append("protected void unpack(DataMap map, T target) {\n");
        if(parentClass!=null) {
            builder.append(INDENT_2).append("super.unpack(map, target);\n");
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
                    if(targetClassNames.contains(subElement.asType().toString())) {
                        builder.append(INDENT_2).append("target.").append(name).append(" = ");
                        builder.append("Packager.unpack(")
                                .append("map.getDataMap(\"").append(name)
                                .append("\"), ")
                                .append(subElement.asType().toString()).append(".class);\n");
                    } else {
                        throw new IllegalArgumentException("Field type ("+subElement.asType().toString()+") is not mappable to a DataMap.");
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
