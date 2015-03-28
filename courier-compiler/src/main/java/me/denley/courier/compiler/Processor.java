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
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import me.denley.courier.BackgroundThread;
import me.denley.courier.LocalNode;
import me.denley.courier.ReceiveData;
import me.denley.courier.ReceiveMessages;
import me.denley.courier.RemoteNodes;

public class Processor extends javax.annotation.processing.AbstractProcessor {

    static final String CLASS_SUFFIX = "$$Delivery";

    private static final String NODE_CLASS = "com.google.android.gms.wearable.Node";



    private Map<TypeElement, PostalArea> postalAreaMap = new LinkedHashMap<TypeElement, PostalArea>();
    Set<String> targetClassNames = new LinkedHashSet<String>();

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
            targetClassNames.add(enclosingElement.toString());
        }
        return area;
    }

    @Override public Set<String> getSupportedAnnotationTypes() {
        final Set<String> set = new LinkedHashSet<String>();
        set.add(ReceiveData.class.getName());
        set.add(ReceiveMessages.class.getName());
        set.add(LocalNode.class.getName());
        set.add(RemoteNodes.class.getName());
        set.add(BackgroundThread.class.getName());
        return set;
    }

    @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        postalAreaMap = new LinkedHashMap<TypeElement, PostalArea>();
        verifyBackgroundThreadAnnotations(roundEnv);

        processReceiveDataAnnotations(roundEnv);
        processReceiveMessagesAnnotations(roundEnv);
        processLocalNodeAnnotations(roundEnv);
        processRemoteNodeAnnotations(roundEnv);

        processParents();

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

    private void processRemoteNodeAnnotations(RoundEnvironment roundEnv) {
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
                } else if (parameters.size() > 2
                        || (parameters.size() < 1 && (annotationClass==ReceiveMessages.class || annotationClass==ReceiveData.class))) {
                    throw new IllegalArgumentException("Incorrect number of parameters for method.");
                } else if (parameters.size()==2
                        && (annotationClass==ReceiveMessages.class || annotationClass==ReceiveData.class)
                        && !parameters.get(1).asType().toString().equals(String.class.getName())) {
                    throw new IllegalArgumentException("The second parameter must be a String (represents the source node ID)");
                } else if(annotationClass==LocalNode.class
                        && !parameters.get(0).asType().toString().equals(NODE_CLASS)) {
                    throw new IllegalArgumentException("@LocalNode annotated method must have a parameter that is a "+NODE_CLASS);
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
                } else if(annotationClass==LocalNode.class && !element.asType().toString().equalsIgnoreCase(NODE_CLASS)) {
                    throw new IllegalArgumentException("@LocalNode annotated field must be a "+NODE_CLASS);
                } else if(annotationClass==RemoteNodes.class
                        && !element.asType().toString().equalsIgnoreCase("java.util.List<com.google.android.gms.wearable.Node>")) {
                    throw new IllegalArgumentException("@RemoteNodes annotated field must be a List<Node>");
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
            return new Recipient(name, payload);
        } else {
            final ExecutableElement executableElement = (ExecutableElement) element;
            final List<? extends VariableElement> params = executableElement.getParameters();
            final String name = element.getSimpleName().toString();
            final String payload = params.get(0).asType().toString();

            final boolean backgroundThread = element.getAnnotation(BackgroundThread.class)!=null;

            return new Recipient(name, payload, params.size()>1, backgroundThread);
        }
    }

    private void verifyBackgroundThreadAnnotations(RoundEnvironment roundEnv) {
        for(Element element:roundEnv.getElementsAnnotatedWith(BackgroundThread.class)) {
            verifyBackgroundThreadElementOrFail(element);
        }
    }

    private void verifyBackgroundThreadElementOrFail(Element element) {
        try {
            verifyBackgroundThreadElement(element);
        } catch(IllegalArgumentException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage(), element);
        }
    }

    private void verifyBackgroundThreadElement(Element element) {
        if(element.getKind()!=ElementKind.METHOD) {
            throw new IllegalArgumentException("@BackgroundThread may only be used on methods");
        } else if (element.getAnnotation(ReceiveData.class)==null
                && element.getAnnotation(ReceiveMessages.class)==null
                && element.getAnnotation(RemoteNodes.class)==null
                && element.getAnnotation(LocalNode.class)==null
                ) {
            throw new IllegalArgumentException("@BackgroundThread must be used with @ReceiveData, @ReceiveMessages, @RemoteNodes, or @LocalNode");
        }
    }

    private void processParents() {
        // Try to find a parent injector for each injector.
        for (Map.Entry<TypeElement, PostalArea> entry : postalAreaMap.entrySet()) {
            String parentClassFqcn = findParentFqcn(entry.getKey());
            if (parentClassFqcn != null) {
                entry.getValue().setParent(parentClassFqcn + CLASS_SUFFIX);
            }
        }
    }

    private String findParentFqcn(TypeElement typeElement) {
        TypeMirror type;
        while (true) {
            type = typeElement.getSuperclass();
            if (type.getKind() == TypeKind.NONE) {
                return null;
            }
            typeElement = (TypeElement) ((DeclaredType) type).asElement();
            if (targetClassNames.contains(typeElement.toString())) {
                final String packageName = processingEnv
                        .getElementUtils()
                        .getPackageOf(typeElement)
                        .getQualifiedName()
                        .toString();
                final String targetClassName = typeElement
                        .getQualifiedName()
                        .toString()
                        .substring(packageName.length() + 1)
                        .replace('.', '$');
                return packageName + "." + targetClassName;
            }
        }
    }

    private void writeClasses() {
        for(TypeElement element : postalAreaMap.keySet()) {
            final PostalArea area = postalAreaMap.get(element);

            try {
                JavaFileObject jfo = processingEnv.getFiler().createSourceFile(
                        area.getTargetClassName() + CLASS_SUFFIX,
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
