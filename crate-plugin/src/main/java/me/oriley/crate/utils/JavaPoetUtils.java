package me.oriley.crate.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.javapoet.*;
import org.apache.commons.lang.StringUtils;

import javax.lang.model.element.Modifier;

@SuppressWarnings("unused")
public final class JavaPoetUtils {

    private JavaPoetUtils() {
        throw new IllegalAccessError("no instances");
    }

    @NonNull
    public static ParameterSpec createPrimitiveParameter(@NonNull String paramName, @NonNull Class clazz) {
        return ParameterSpec.builder(clazz, paramName)
                .build();
    }

    @NonNull
    public static ParameterSpec createParameter(@NonNull String paramName, @NonNull TypeName typeName, boolean isNullable) {
        return ParameterSpec.builder(typeName, paramName)
                .addAnnotation(createNullabilityAnnotation(isNullable))
                .build();
    }

    @NonNull
    public static ParameterSpec createParameter(@NonNull String paramName, @NonNull Class clazz, boolean isNullable) {
        return ParameterSpec.builder(clazz, paramName)
                .addAnnotation(createNullabilityAnnotation(isNullable))
                .build();
    }

    @NonNull
    public static AnnotationSpec createSuppressWarningAnnotation(@NonNull String value) {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", value).build();
    }

    @NonNull
    public static AnnotationSpec createNullabilityAnnotation(boolean isNullable) {
        return AnnotationSpec.builder(isNullable ? Nullable.class : NonNull.class).build();
    }

    @NonNull
    public static MethodSpec createGetter(@NonNull String fieldName, @NonNull Class clazz, boolean isNullable,
                                           @NonNull Modifier... modifiers) {
        return MethodSpec.methodBuilder("get" + capitalise(fieldName))
                .addModifiers(modifiers)
                .addAnnotation(createNullabilityAnnotation(isNullable))
                .addStatement("return $N", toInstance(fieldName))
                .returns(clazz)
                .build();
    }

    @NonNull
    public static FieldSpec createField(@NonNull ClassName className, boolean isNullable, @NonNull Modifier... modifiers) {
        return FieldSpec.builder(className, toInstance(className.simpleName()))
                .addModifiers(modifiers)
                .addAnnotation(createNullabilityAnnotation(isNullable))
                .build();
    }

    @NonNull
    public static FieldSpec createField(@NonNull String name, @NonNull Class clazz, boolean isNullable,
                                         @NonNull Modifier... modifiers) {
        return FieldSpec.builder(clazz, name)
                .addModifiers(modifiers)
                .addAnnotation(createNullabilityAnnotation(isNullable))
                .build();
    }

    @NonNull
    public static FieldSpec createStringField(@NonNull String name, boolean isNullable, @NonNull Modifier... modifiers) {
        return createField(name, String.class, isNullable, modifiers);
    }

    @NonNull
    public static String toInstance(@NonNull String string) {
        if (StringUtils.isEmpty(string) || string.startsWith("m")) {
            return string;
        } else {
            return "m" + capitalise(string);
        }
    }

    @NonNull
    public static String fromInstance(@NonNull String string) {
        if (StringUtils.isEmpty(string) || !string.startsWith("m")) {
            return string;
        } else {
            String deInstanceName = string.substring(1);
            return deInstanceName.substring(0, 1).toLowerCase() + string.substring(1);
        }
    }

    @NonNull
    public static String capitalise(@NonNull String string) {
        if (StringUtils.isEmpty(string)) {
            return "";
        } else {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }

}
