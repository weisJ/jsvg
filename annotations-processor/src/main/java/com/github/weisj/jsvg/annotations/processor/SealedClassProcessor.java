/*
 * MIT License
 *
 * Copyright (c) 2024 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.github.weisj.jsvg.annotations.processor;

import java.util.Map;
import java.util.Set;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.github.weisj.jsvg.annotations.Sealed;
import com.sun.tools.javac.util.List;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class SealedClassProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Types typeUtils = processingEnv.getTypeUtils();

        for (Element type : roundEnv.getElementsAnnotatedWith(Sealed.class)) {
            TypeElement[] permittedSubclasses = getPermittedClasses((TypeElement) type, typeUtils);

            for (Element element : roundEnv.getRootElements()) {
                if (element.getKind() == ElementKind.CLASS || element.getKind() == ElementKind.INTERFACE) {
                    TypeElement classElement = (TypeElement) element;

                    if (isSubtype(classElement, (TypeElement) type, typeUtils)) {
                        if (!isPermitted(permittedSubclasses, classElement)) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("\n");
                            sb.append(classElement.getSimpleName());
                            sb.append(" is not permitted to be a subclass of sealed type ");
                            sb.append(type.getSimpleName());
                            sb.append("\n");
                            sb.append("Permitted subclasses are:");
                            for (TypeElement allowedClass : permittedSubclasses) {
                                sb.append("\n    - ");
                                sb.append(allowedClass.getQualifiedName());
                            }
                            processingEnv.getMessager()
                                    .printMessage(Diagnostic.Kind.ERROR, sb.toString(), element);
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isPermitted(@NotNull TypeElement @NotNull [] permittedSubclasses, @NotNull TypeElement subclass) {
        for (TypeElement allowedClass : permittedSubclasses) {
            if (subclass.getQualifiedName().equals(allowedClass.getQualifiedName())) return true;
        }
        return false;
    }

    private boolean isSubtype(@NotNull TypeElement classElement, @NotNull TypeElement type, @NotNull Types typeUtils) {
        for (TypeMirror iface : classElement.getInterfaces()) {
            if (type.equals(typeUtils.asElement(iface))) return true;
        }
        return type.asType().equals(classElement.getSuperclass());
    }

    public TypeElement[] getPermittedClasses(@NotNull TypeElement element, @NotNull Types typeUtils) {
        AnnotationMirror am = getAnnotationMirror(element, Sealed.class);
        if (am == null) return null;
        AnnotationValue av = getAnnotationValue(am, "permits");
        if (av == null) return null;
        @SuppressWarnings("unchecked") List<AnnotationValue> values = (List<AnnotationValue>) av.getValue();
        TypeElement[] result = new TypeElement[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = (TypeElement) typeUtils.asElement((TypeMirror) values.get(i).getValue());
        }
        return result;
    }

    private @Nullable AnnotationMirror getAnnotationMirror(@NotNull TypeElement typeElement,
            @NotNull Class<?> clazz) {
        String clazzName = clazz.getName();
        for (AnnotationMirror m : typeElement.getAnnotationMirrors()) {
            if (m.getAnnotationType().toString().equals(clazzName)) {
                return m;
            }
        }
        return null;
    }

    private @Nullable AnnotationValue getAnnotationValue(@NotNull AnnotationMirror annotationMirror,
            @NotNull String key) {
        for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror
                .getElementValues().entrySet()) {
            if (entry.getKey().getSimpleName().toString().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
