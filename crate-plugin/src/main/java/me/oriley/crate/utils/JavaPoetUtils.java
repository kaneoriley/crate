package me.oriley.crate.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.squareup.javapoet.*;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nonnull;
import javax.lang.model.element.Modifier;

import java.util.Arrays;
import java.util.List;

import static java.util.Locale.US;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.STATIC;
import static me.oriley.crate.utils.JavaPoetUtils.Nullability.NONE;
import static me.oriley.crate.utils.JavaPoetUtils.Nullability.NULLABLE;

@SuppressWarnings("unused")
public final class JavaPoetUtils {

    public enum Nullability {
        NONE, NONNULL, NULLABLE
    }

    private JavaPoetUtils() {
        throw new IllegalAccessError("no instances");
    }

    @NonNull
    public static ParameterSpec createPrimitiveParameter(@NonNull String paramName, @NonNull Class clazz) {
        return ParameterSpec.builder(clazz, paramName).build();
    }

    @NonNull
    public static ParameterSpec createParameter(@NonNull String paramName,
                                                @NonNull TypeName typeName,
                                                @NonNull Nullability nullability) {
        ParameterSpec.Builder builder = ParameterSpec.builder(typeName, paramName);
        addNullability(builder, nullability);
        return builder.build();
    }

    @NonNull
    public static ParameterSpec createParameter(@NonNull String paramName,
                                                @NonNull Class clazz,
                                                @NonNull Nullability nullability) {
        ParameterSpec.Builder builder = ParameterSpec.builder(clazz, paramName);
        addNullability(builder, nullability);
        return builder.build();
    }

    @NonNull
    public static AnnotationSpec createSuppressWarningAnnotation(@NonNull String value) {
        return AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", value).build();
    }

    public static void addNullability(@NonNull FieldSpec.Builder builder, @NonNull Nullability nullability) {
        if (nullability != NONE) {
            builder.addAnnotation(
                    AnnotationSpec.builder(nullability == NULLABLE ? Nullable.class : NonNull.class).build());
        }
    }

    public static void addNullability(@NonNull MethodSpec.Builder builder, @NonNull Nullability nullability) {
        if (nullability != NONE) {
            builder.addAnnotation(
                    AnnotationSpec.builder(nullability == NULLABLE ? Nullable.class : NonNull.class).build());
        }
    }

    public static void addNullability(@NonNull ParameterSpec.Builder builder, @NonNull Nullability nullability) {
        if (nullability != NONE) {
            builder.addAnnotation(
                    AnnotationSpec.builder(nullability == NULLABLE ? Nullable.class : NonNull.class).build());
        }
    }

    @NonNull
    public static MethodSpec createGetter(@NonNull String fieldName,
                                          @NonNull Class clazz,
                                          @NonNull Nullability nullability,
                                          @NonNull Modifier... modifiers) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("get" + capitalise(fieldName))
                .addModifiers(modifiers)
                .addStatement("return $N", asFieldName(fieldName, modifiers))
                .returns(clazz);
        addNullability(builder, nullability);
        return builder.build();
    }

    @NonNull
    public static FieldSpec createField(@NonNull ClassName className,
                                        @NonNull Nullability nullability,
                                        @NonNull Modifier... modifiers) {
        FieldSpec.Builder builder = FieldSpec.builder(className, asFieldName(className.simpleName(), modifiers))
                .addModifiers(modifiers);
        addNullability(builder, nullability);
        return builder.build();
    }

    @NonNull
    public static FieldSpec createField(@NonNull String name,
                                        @NonNull Class clazz,
                                        @NonNull Nullability nullability,
                                         @NonNull Modifier... modifiers) {
        FieldSpec.Builder builder = FieldSpec.builder(clazz, asFieldName(name, modifiers))
                .addModifiers(modifiers);
        addNullability(builder, nullability);
        return builder.build();
    }

    @NonNull
    public static FieldSpec createBooleanField(@NonNull String name,
                                               boolean initialValue,
                                               @NonNull Modifier... modifiers) {
        FieldSpec.Builder builder = FieldSpec.builder(boolean.class, asFieldName(name, modifiers))
                .addModifiers(modifiers)
                .initializer("$L", initialValue);
        return builder.build();
    }

    @NonNull
    public static FieldSpec createIntField(@NonNull String name, @NonNull Modifier... modifiers) {
        return createField(name, int.class, NONE, modifiers);
    }

    @NonNull
    public static FieldSpec createStringField(@NonNull String name,
                                              @NonNull Nullability nullability,
                                              @NonNull Modifier... modifiers) {
        return createField(asFieldName(name, modifiers), String.class, nullability, modifiers);
    }

    @Nonnull
    public static String asFieldName(@NonNull String string, @NonNull Modifier... modifiers) {
        List<Modifier> modifierList = Arrays.asList(modifiers);
        if (modifierList.contains(STATIC) && modifierList.contains(FINAL)) {
            return string.toUpperCase(US);
        } else if (modifierList.contains(STATIC)) {
            return addPrefix(string, "s");
        } else {
            return addPrefix(string, "m");
        }
    }

    @NonNull
    public static String addPrefix(@NonNull String string, @NonNull String prefix) {
        if (StringUtils.isEmpty(string) || string.startsWith(prefix)) {
            return string;
        } else {
            return prefix + capitalise(string);
        }
    }

    @NonNull
    public static String removePrefix(@NonNull String string, @NonNull String prefix) {
        if (StringUtils.isEmpty(string) || !string.startsWith(prefix)) {
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
