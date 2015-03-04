package me.denley.courier.compiler;

import java.io.IOException;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import me.denley.courier.Courier;
import me.denley.courier.ReceiveData;
import me.denley.courier.ReceiveMessages;

public class Processor extends javax.annotation.processing.AbstractProcessor {

    private Map<TypeElement, PostalArea> postalAreaMap = new LinkedHashMap<>();

    private PostalArea getPostalArea(TypeElement enclosingElement) {
        PostalArea area = postalAreaMap.get(enclosingElement);
        if(area==null) {
            final String packageName = processingEnv
                    .getElementUtils()
                    .getPackageOf(enclosingElement)
                    .getQualifiedName()
                    .toString();
            final String targetClassName = enclosingElement
                    .getQualifiedName()
                    .toString()
                    .substring(packageName.length() + 1)
                    .replace('.', '$');

            area = new PostalArea(packageName, targetClassName);
            postalAreaMap.put(enclosingElement, area);
        }
        return area;
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        final Set<String> set = new LinkedHashSet<>();
        set.add(ReceiveData.class.getName());
        set.add(ReceiveMessages.class.getName());
        return set;
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        postalAreaMap = new LinkedHashMap<>();
        processReceiveDataAnnotations(roundEnv);
        processReceiveMessagesAnnotations(roundEnv);
        writeClasses();
        return true;
    }

    private void processReceiveDataAnnotations(RoundEnvironment roundEnv) {
        for(Element element:roundEnv.getElementsAnnotatedWith(ReceiveData.class)) {
            final String path = element.getAnnotation(ReceiveData.class).value();
            processElementOrFail(element, path, true);
        }
    }

    private void processReceiveMessagesAnnotations(RoundEnvironment roundEnv) {
        for(Element element:roundEnv.getElementsAnnotatedWith(ReceiveMessages.class)) {
            final String path = element.getAnnotation(ReceiveMessages.class).value();
            processElementOrFail(element, path, false);
        }
    }

    private void processElementOrFail(Element element, String path, boolean data) {
        try {
            processElement(element, path, data);
        } catch (IllegalArgumentException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
        }
    }

    private void processElement(Element element, String path, boolean data) {
        if(element.getEnclosingElement().getKind()!= ElementKind.CLASS) {
            throw new IllegalArgumentException("Annotation can only apply to class fields and methods.");
        }

        switch(element.getKind()){
            case METHOD:
                processMethodElement(element, path, data);
                break;
            case FIELD:
                processFieldElement(element, path, data);
                break;
            default:
                throw new IllegalArgumentException("Delivery must be made to a method or field");
        }
    }

    private void processFieldElement(Element element, String path, boolean data) {
        final Set<Modifier> modifiers = element.getModifiers();
        if(modifiers.contains(Modifier.PRIVATE)
                || modifiers.contains(Modifier.STATIC)
                || modifiers.contains(Modifier.FINAL)) {
            throw new IllegalArgumentException("Annotated fields must not be private, static, nor final");
        }


        final String name = element.getSimpleName().toString();
        final String payload = element.asType().toString();
        final Recipient recipient = new Recipient(name, ElementKind.FIELD, payload);

        final PostalArea area = getPostalArea((TypeElement) element.getEnclosingElement());
        final Route route = area.getRoute(path, data);
        route.recipients.add(recipient);
    }

    private void processMethodElement(Element element, String path, boolean data) {
        final Set<Modifier> modifiers = element.getModifiers();
        final ExecutableElement executableElement = (ExecutableElement) element;
        final List<? extends VariableElement> params = executableElement.getParameters();

        if(modifiers.contains(Modifier.PRIVATE)
                || modifiers.contains(Modifier.STATIC)) {
            throw new IllegalArgumentException("Annotated methods must not be private or static");
        } else if (params.size() != 1) {
            throw new IllegalArgumentException("Annotated methods must have a single parameter");
        }

        final String name = element.getSimpleName().toString();
        final String payload = params.get(0).asType().toString();
        final Recipient recipient = new Recipient(name, ElementKind.METHOD, payload);

        final PostalArea area = getPostalArea((TypeElement) element.getEnclosingElement());
        final Route route = area.getRoute(path, data);
        route.recipients.add(recipient);
    }

    private void writeClasses() {
        for(TypeElement element : postalAreaMap.keySet()) {
            final PostalArea area = postalAreaMap.get(element);

            try {
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                        area.getTargetClassName() + Courier.CLASS_SUFFIX,
                        element);
                Writer writer = jfo.openWriter();
                writer.write(area.writeJava());
                writer.flush();
                writer.close();
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
            }
        }
    }


}
