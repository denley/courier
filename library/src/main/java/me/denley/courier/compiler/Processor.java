package me.denley.courier.compiler;

import com.google.android.gms.wearable.Node;

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
import me.denley.courier.LocalNode;
import me.denley.courier.ReceiveData;
import me.denley.courier.ReceiveMessages;
import me.denley.courier.RemoteNodes;

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
        set.add(LocalNode.class.getName());
        return set;
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        postalAreaMap = new LinkedHashMap<>();
        processReceiveDataAnnotations(roundEnv);
        processReceiveMessagesAnnotations(roundEnv);
        processLocalNodeAnnotations(roundEnv);
        processRemotelNodeAnnotations(roundEnv);
        writeClasses();
        return true;
    }

    private void processReceiveDataAnnotations(RoundEnvironment roundEnv) {
        for(Element element:roundEnv.getElementsAnnotatedWith(ReceiveData.class)) {
            final String path = element.getAnnotation(ReceiveData.class).value();
            processElementOrFail(element, path, ReceiveData.class);
        }
    }

    private void processReceiveMessagesAnnotations(RoundEnvironment roundEnv) {
        for(Element element:roundEnv.getElementsAnnotatedWith(ReceiveMessages.class)) {
            final String path = element.getAnnotation(ReceiveMessages.class).value();
            processElementOrFail(element, path, ReceiveMessages.class);
        }
    }

    private void processLocalNodeAnnotations(RoundEnvironment roundEnv) {
        for(Element element:roundEnv.getElementsAnnotatedWith(LocalNode.class)) {
            processElementOrFail(element, null, LocalNode.class);
        }
    }

    private void processRemotelNodeAnnotations(RoundEnvironment roundEnv) {
        for(Element element:roundEnv.getElementsAnnotatedWith(RemoteNodes.class)) {
            processElementOrFail(element, null, RemoteNodes.class);
        }
    }

    private void processElementOrFail(Element element, String path, Class annotationClass) {
        try {
            processElement(element, path, annotationClass);
        } catch (IllegalArgumentException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
        }
    }

    private void processElement(Element element, String path, Class annotationClass) {
        checkForErrors(element, annotationClass);

        final PostalArea area = getPostalArea((TypeElement) element.getEnclosingElement());
        final Recipient recipient = createRecipient(element);

        if(annotationClass==LocalNode.class) {
            area.addLocalNodeRecipient(recipient);
        } else if (annotationClass==RemoteNodes.class) {
            area.addRemoteNodeRecipient(recipient);
        } else {
            final Route route = area.getRoute(path, annotationClass==ReceiveData.class);
            route.recipients.add(recipient);
        }
    }

    private void checkForErrors(Element element, Class annotationClass) {
        if(element.getEnclosingElement().getKind()!= ElementKind.CLASS) {
            throw new IllegalArgumentException("Annotation can only apply to class fields and methods.");
        }

        final Set<Modifier> modifiers = element.getModifiers();

        switch(element.getKind()){
            case METHOD:
                List<? extends VariableElement> parameters = ((ExecutableElement) element).getParameters();
                if(modifiers.contains(Modifier.PRIVATE)
                        || modifiers.contains(Modifier.STATIC)) {
                    throw new IllegalArgumentException("Annotated methods must not be private or static");
                } else if (parameters.size() != 1) {
                    throw new IllegalArgumentException("Annotated methods must have a single parameter");
                } else if(annotationClass==LocalNode.class
                        && !parameters.get(0).asType().toString().equals(Node.class.getName())) {
                    throw new IllegalArgumentException("@LocalNode annotated method must have a parameter that is a "+Node.class.getName());
                } else if(annotationClass==RemoteNodes.class
                        && !parameters.get(0).asType().toString().equals("java.util.List<com.google.android.gms.wearable.Node>")) {
                    throw new IllegalArgumentException("@RemoteNode annotated method must have a parameter that is a List<Node>");
                }
                break;
            case FIELD:
                if(modifiers.contains(Modifier.PRIVATE)
                        || modifiers.contains(Modifier.STATIC)
                        || modifiers.contains(Modifier.FINAL)) {
                    throw new IllegalArgumentException("Annotated fields must not be private, static, nor final");
                } else if(annotationClass==LocalNode.class && !element.asType().toString().equalsIgnoreCase(Node.class.getName())) {
                    throw new IllegalArgumentException("@LocalNode annotated field must be a "+Node.class.getName());
                } else if(annotationClass==RemoteNodes.class
                        && !element.asType().toString().equalsIgnoreCase("java.util.List<com.google.android.gms.wearable.Node>")) {
                    throw new IllegalArgumentException("@LocalNode annotated field must be a List<Node>");
                }
                break;
            default:
                throw new IllegalArgumentException("Delivery must be made to a method or field");
        }
    }

    private Recipient createRecipient(Element element) {
        if(element.getKind().isField()) {
            final String name = element.getSimpleName().toString();
            final String payload = element.asType().toString();
            return new Recipient(name, ElementKind.FIELD, payload);
        } else {
            final ExecutableElement executableElement = (ExecutableElement) element;
            final List<? extends VariableElement> params = executableElement.getParameters();
            final String name = element.getSimpleName().toString();
            final String payload = params.get(0).asType().toString();
            return new Recipient(name, ElementKind.METHOD, payload);
        }
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
