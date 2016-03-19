/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

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

public final class JavaPoetUtils {

    public enum Nullability {
        NONE, NONNULL, NULLABLE
    }

    private JavaPoetUtils() {
        throw new IllegalAccessError("no instances");
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

    @NonNull
    public static FieldSpec createBooleanField(@NonNull String name,
                                               boolean initialValue,
                                               @NonNull Modifier... modifiers) {
        FieldSpec.Builder builder = FieldSpec.builder(boolean.class, asFieldName(name, modifiers))
                .addModifiers(modifiers)
                .initializer("$L", initialValue);
        return builder.build();
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
    public static String capitalise(@NonNull String string) {
        if (StringUtils.isEmpty(string)) {
            return "";
        } else {
            return string.substring(0, 1).toUpperCase() + string.substring(1);
        }
    }

}
