/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package me.oriley.crate;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import com.squareup.javapoet.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.lang.model.element.Modifier;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Locale.US;
import static javax.lang.model.element.Modifier.*;
import static me.oriley.crate.utils.JavaPoetUtils.*;
import static me.oriley.crate.utils.JavaPoetUtils.Nullability.NONE;
import static me.oriley.crate.utils.JavaPoetUtils.Nullability.NONNULL;
import static me.oriley.crate.utils.JavaPoetUtils.Nullability.NULLABLE;

public final class CrateGenerator {

    private static final String CRATE = "Crate";
    private static final String CRATE_HASH = CrateHasher.getActualHash();
    private static final String ASSETS = "assets";
    private static final String CONTEXT = "context";
    private static final String BITMAP = "bitmap";
    private static final String TYPEFACE = "typeface";
    private static final String OPEN = "open";
    private static final String GET_BITMAP = "getBitmap";
    private static final String GET_TYPEFACE = "getTypeface";
    private static final String THIS = "this";
    private static final String CLEAR = "clear";
    private static final String STREAM = "stream";
    private static final String KEY = "key";
    private static final String DEBUG = "DEBUG";

    private static final String BITMAP_CACHE = "bitmapCache";
    private static final String TYPEFACE_CACHE = "typefaceCache";
    private static final String ASSET_MANAGER = "AssetManager";

    private static final ClassName BITMAP_FACTORY_CLASS = ClassName.get("android.graphics", "BitmapFactory");
    private static final ClassName LOG_CLASS = ClassName.get("android.util", "Log");
    private static final ClassName ASSETMANAGER_CLASS = ClassName.get("android.content.res", ASSET_MANAGER);
    private static final ClassName BITMAP_CLASS = ClassName.get("android.graphics", capitalise(BITMAP));
    private static final ClassName CONTEXT_CLASS = ClassName.get("android.content", capitalise(CONTEXT));
    private static final ClassName TYPEFACE_CLASS = ClassName.get("android.graphics", capitalise(TYPEFACE));

    private static final List<String> FONT_EXTENSIONS = Arrays.asList("otf", "ttf");
    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "gif", "png");

    private static final Logger log = LoggerFactory.getLogger(CrateGenerator.class.getSimpleName());

    @NonNull
    private final String mBaseOutputDir;

    @NonNull
    private final String mVariantAssetDir;

    @NonNull
    private final String mPackageName;

    @NonNull
    private final String mClassName;

    private final boolean mStaticMode;

    private final boolean mDebugLogging;

    public CrateGenerator(@NonNull String baseOutputDir,
                          @NonNull String variantAssetDir,
                          @NonNull String packageName,
                          @Nullable String className,
                          boolean staticMode,
                          boolean debugLogging) {
        mBaseOutputDir = baseOutputDir;
        mVariantAssetDir = variantAssetDir;
        mPackageName = packageName;
        mClassName = className != null ? className : CRATE;
        mStaticMode = staticMode;
        mDebugLogging = debugLogging;
        log("CrateGenerator constructed\n" +
                "    Output: " + mBaseOutputDir + "\n" +
                "    Asset: " + mVariantAssetDir + "\n" +
                "    Package: " + mPackageName + "\n" +
                "    Class: " + mClassName + "\n" +
                "    Static: " + mStaticMode + "\n" +
                "    Logging: " + mDebugLogging);
    }

    public void buildCrate() {
        long startNanos = System.nanoTime();
        File variantDir = new File(mVariantAssetDir);
        if (!variantDir.exists() || !variantDir.isDirectory()) {
            log("Asset directory does not exist, aborting");
            return;
        }

        try {
            brewJava(variantDir, mVariantAssetDir, mPackageName).writeTo(new File(mBaseOutputDir));
        } catch (IOException e) {
            logError("Failed to generate java", e, true);
        }

        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        log("Time to build was " + lengthMillis + "ms");
    }

    public boolean isCrateHashValid() {
        String crateOutputFile = mBaseOutputDir + '/' + mPackageName.replace('.', '/') + "/" + mClassName + ".java";
        long startNanos = System.nanoTime();
        File file = new File(crateOutputFile);

        boolean returnValue = false;
        if (!file.exists()) {
            log("File " + crateOutputFile + " doesn't exist, hash invalid");
        } else if (!file.isFile()) {
            log("File " + crateOutputFile + " is not a file (?), hash invalid");
        } else {
            returnValue = isFileValid(file, getComments());
        }

        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        log("Hash check took " + lengthMillis + "ms, was valid: " + returnValue);
        return returnValue;
    }

    private boolean isFileValid(@NonNull File crateOutputFile, @NonNull String[] comments) {
        if (comments.length <= 0) {
            return false;
        }

        boolean isValid = true;
        try {
            FileReader reader = new FileReader(crateOutputFile);
            BufferedReader input = new BufferedReader(reader);

            for (String comment : comments) {
                String fileLine = input.readLine();
                if (fileLine == null || comment == null || !StringUtils.contains(fileLine, comment)) {
                    log("Aborting, comment: " + comment + ", fileLine: " + fileLine);
                    isValid = false;
                    break;
                } else {
                    log("Line valid, comment: " + comment + ", fileLine: " + fileLine);
                }
            }

            input.close();
            reader.close();
        } catch (IOException e) {
            logError("Error parsing file", e, false);
            isValid = false;
        }

        log("File check result -- isValid ? " + isValid);
        return isValid;
    }

    private void validateNonStatic() {
        if (mStaticMode) {
            String message = "Dont call non static spec creator in static mode";
            logError(message, new Exception(), true);
        }
    }

    private void logError(@NonNull String message, @NonNull Throwable error, boolean throwError) {
        log.error("Crate: " + message, error);
        if (throwError) {
            throw new IllegalStateException("Crate: Fatal Exception");
        }
    }

    private void log(@NonNull String message) {
        if (mDebugLogging) {
            log.warn("Crate: " + message);
        }
    }

    @NonNull
    private JavaFile brewJava(@NonNull File variantDir,
                              @NonNull String variantAssetDir,
                              @NonNull String packageName) {

        TypeSpec.Builder builder = TypeSpec.classBuilder(mClassName)
                .addModifiers(PUBLIC, FINAL)
                .addType(createAssetClass())
                .addType(createFontAssetClass())
                .addType(createImageAssetClass())
                .addAnnotation(createSuppressWarningAnnotation("unused"));

        builder.addField(createBooleanField(DEBUG, mDebugLogging, PRIVATE, STATIC, FINAL))
                .addField(createField(ASSETMANAGER_CLASS, NONNULL, PRIVATE, FINAL))
                .addField(createCacheMapField(TYPEFACE_CACHE, TYPEFACE_CLASS))
                .addField(createCacheMapField(BITMAP_CACHE, BITMAP_CLASS));

        TreeMap<String, Asset> allAssets = new TreeMap<>();
        listFiles(allAssets, builder, ASSETS, variantDir, variantAssetDir, true);

        if (!allAssets.isEmpty()) {
            TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class),
                    TypeVariableName.get(Asset.getTypeName()));
            builder.addField(createListField(listType, "FULL_LIST", allAssets));
        }

        builder.addMethod(createCrateConstructor())
                .addMethod(createInputStreamMethod(false))
                .addMethod(createInputStreamMethod(true))
                .addMethod(createBitmapMethod())
                .addMethod(createTypefaceMethod())
                .addMethod(createBitmapClearMethod())

        JavaFile.Builder javaBuilder = JavaFile.builder(packageName, builder.build())
                .indent("    ");

        for (String comment : getComments()) {
            javaBuilder.addFileComment(comment + "\n");
        }

        return javaBuilder.build();
    }

    @NonNull
    private String[] getComments() {
        return new String[]{CRATE_HASH, "Package: " + mPackageName, "Class: " + mClassName, "Static: " + mStaticMode};
    }

    private void listFiles(@NonNull TreeMap<String, Asset> allAssets,
                           @NonNull TypeSpec.Builder parentBuilder,
                           @NonNull String classPathString,
                           @NonNull File directory,
                           @NonNull String variantAssetDir,
                           boolean root) {

        String rootName = root ? ASSETS : directory.getName();
        TypeSpec.Builder builder = TypeSpec.classBuilder(makeClassName(rootName))
                .addModifiers(PUBLIC, FINAL);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        }

        List<File> files = getFileList(directory);
        TreeMap<String, Asset> assetMap = new TreeMap<>();
        boolean isFontFolder = true;
        boolean isImageFolder = true;

        for (File file : files) {
            if (file.isDirectory()) {
                listFiles(allAssets, builder, classPathString + "." + file.getName(), file, variantAssetDir, false);
            } else {
                String fileName = file.getName();
                String fieldName = sanitiseFieldName(fileName).toUpperCase(US);

                if (assetMap.containsKey(fieldName)) {
                    String baseFieldName = fieldName + "_";
                    int counter = 0;
                    while (assetMap.containsKey(fieldName)) {
                        fieldName = baseFieldName + counter;
                    }
                }

                String filePath = file.getPath().replace(variantAssetDir + "/", "");

                String fileExtension = getFileExtension(fileName).toLowerCase(US);
                Asset asset;
                if (FONT_EXTENSIONS.contains(fileExtension)) {
                    isImageFolder = false;
                    String fontName = getFontName(file.getPath());
                    asset = new FontAsset(fieldName, filePath, fileName, fontName != null ? fontName : fileName);
                    builder.addField(createFontAssetField((FontAsset) asset));
                } else if (IMAGE_EXTENSIONS.contains(fileExtension)) {
                    isFontFolder = false;

                    int width = 0;
                    int height = 0;
                    try {
                        BufferedImage image = ImageIO.read(file);
                        if (image != null) {
                            width = image.getWidth();
                            height = image.getHeight();
                        }
                    } catch (IOException e) {
                        logError("Error parsing image: " + file.getPath(), e, false);
                    }

                    asset = new ImageAsset(fieldName, filePath, fileName, width, height);
                    builder.addField(createImageAssetField((ImageAsset) asset));
                } else {
                    isFontFolder = false;
                    isImageFolder = false;
                    asset = new Asset(fieldName, filePath, fileName);
                    builder.addField(createAssetField(asset));
                }
                assetMap.put(fieldName, asset);
                allAssets.put(classPathString + "." + fieldName, asset);
            }
        }

        if (!assetMap.isEmpty()) {
            TypeName elementType = TypeVariableName.get(isFontFolder ? FontAsset.getTypeName() :
                    isImageFolder ? ImageAsset.getTypeName() : Asset.getTypeName());
            TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), elementType);
            builder.addField(createListField(listType, "LIST", assetMap));
        }
        parentBuilder.addType(builder.build());

        if (!mStaticMode) {
            parentBuilder.addField(createNonStaticClassField(rootName));
        }
    }

    @NonNull
    private String makeClassName(@NonNull String rootClassName) {
        if (mStaticMode) {
            return rootClassName;
        } else {
            return capitalise(rootClassName + "Class");
        }
    }

    @NonNull
    private TypeSpec createAssetClass() {
        String[] fields = Asset.getFields();

        TypeSpec.Builder builder = TypeSpec.classBuilder(Asset.getTypeName())
                .addMethod(createConstructor(fields))
                .addModifiers(PUBLIC);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        } else {
            builder.addMethod(createNonStaticInputStreamMethod(false))
                    .addMethod(createNonStaticInputStreamMethod(true));
        }

        for (String field : fields) {
            builder.addField(createStringField(field, NONNULL, FINAL))
                    .addMethod(createGetter(field, String.class, NONNULL, PUBLIC));
        }

        return builder.build();
    }

    private void addAssetSuperConstructor(@NonNull MethodSpec.Builder builder) {
        String[] fields = Asset.getFields();
        builder.addStatement("super(" + Joiner.on(", ").join(fields) + ")");
        for (String field : fields) {
            builder.addParameter(createParameter(field, String.class, NONNULL));
        }
    }

    @NonNull
    private MethodSpec createNonStaticInputStreamMethod(boolean mode) {
        validateNonStatic();

        MethodSpec.Builder builder = MethodSpec.methodBuilder(OPEN)
                .addModifiers(PUBLIC)
                .addException(IOException.class)
                .returns(InputStream.class);
        addNullability(builder, NONNULL);

        String modeName = "mode";
        if (mode) {
            builder.addParameter(createPrimitiveParameter(modeName, int.class))
                    .addStatement("return $N.$N.$N($N, $N)", mClassName, THIS, OPEN, THIS, modeName);
        } else {
            builder.addStatement("return $N.$N.$N($N)", mClassName, THIS, OPEN, THIS);
        }

        return builder.build();
    }

    @NonNull
    private TypeSpec createFontAssetClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(FontAsset.getTypeName())
                .superclass(TypeVariableName.get(Asset.getTypeName()))
                .addModifiers(PUBLIC);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        } else {
            builder.addMethod(createNonStaticTypefaceMethod());
        }

        String[] fields = FontAsset.getFields();
        for (String field : fields) {
            builder.addField(createStringField(field, NONNULL, FINAL))
                    .addMethod(createGetter(field, String.class, NONNULL, PUBLIC));
        }

        builder.addMethod(createFontAssetConstructor());
        return builder.build();
    }

    @NonNull
    private MethodSpec createFontAssetConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
        addAssetSuperConstructor(builder);

        for (String field : FontAsset.getFields()) {
            builder.addParameter(createParameter(field, String.class, NONNULL))
                    .addStatement("$N = $N", asFieldName(field), field);
        }

        return builder.build();
    }

    @NonNull
    private MethodSpec createNonStaticTypefaceMethod() {
        validateNonStatic();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(GET_TYPEFACE)
                .addModifiers(PUBLIC)
                .addStatement("return $N.$N.$N($N)", mClassName, THIS, GET_TYPEFACE, THIS)
                .returns(TYPEFACE_CLASS);
        addNullability(builder, NULLABLE);
        return builder.build();
    }

    @NonNull
    private TypeSpec createImageAssetClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(ImageAsset.getTypeName())
                .superclass(TypeVariableName.get(Asset.getTypeName()))
                .addModifiers(PUBLIC);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        } else {
            builder.addMethod(createNonStaticBitmapMethod());
        }

        String[] fields = ImageAsset.getFields();
        for (String field : fields) {
            builder.addField(createIntField(field, FINAL))
                    .addMethod(createGetter(field, int.class, NONE, PUBLIC));
        }

        builder.addMethod(createImageAssetConstructor());
        return builder.build();
    }

    @NonNull
    private MethodSpec createNonStaticBitmapMethod() {
        validateNonStatic();
        MethodSpec.Builder builder = MethodSpec.methodBuilder(GET_BITMAP)
                .addModifiers(PUBLIC)
                .addStatement("return $N.$N.$N($N)", mClassName, THIS, GET_BITMAP, THIS)
                .returns(BITMAP_CLASS);
        addNullability(builder, NULLABLE);
        return builder.build();
    }

    @NonNull
    private MethodSpec createImageAssetConstructor() {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);
        addAssetSuperConstructor(builder);

        for (String field : ImageAsset.getFields()) {
            builder.addParameter(createParameter(field, int.class, NONE))
                    .addStatement("$N = $N", asFieldName(field), field);
        }

        return builder.build();
    }

    @NonNull
    private MethodSpec createCrateConstructor() {
        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(createParameter(CONTEXT, CONTEXT_CLASS, NONNULL))
                .addStatement("$N = $N.getApplicationContext().getAssets()", asFieldName(ASSET_MANAGER), CONTEXT)
                .build();
    }

    @NonNull
    private MethodSpec createConstructor(@NonNull String... fields) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        for (String field : fields) {
            builder.addParameter(createParameter(field, String.class, NONNULL))
                    .addStatement("$N = $N", asFieldName(field), field);
        }

        return builder.build();
    }

    private FieldSpec createCacheMapField(@NonNull String name, @NonNull ClassName entryClass) {
        TypeName mapType = ParameterizedTypeName.get(ClassName.get(HashMap.class), ClassName.get(String.class), entryClass);
        FieldSpec.Builder builder = FieldSpec.builder(mapType, asFieldName(name))
                .addModifiers(PRIVATE, FINAL)
                .initializer(CodeBlock.builder().add("new $T()", mapType).build());
        addNullability(builder, NONNULL);
        return builder.build();
    }

    @NonNull
    private MethodSpec createBitmapMethod() {
        String imageAsset = "imageAsset";
        String cacheField = asFieldName(BITMAP_CACHE);

        TypeName assetType = TypeVariableName.get(ImageAsset.getTypeName());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(GET_BITMAP)
                .addModifiers(mStaticMode ? PUBLIC : PRIVATE)
                .addParameter(createParameter(imageAsset, assetType, NONNULL))
                .addCode(CodeBlock.builder()
                        .addStatement("$T $N = $N.mPath", String.class, KEY, imageAsset)
                        .beginControlFlow("if ($N.containsKey($N))", cacheField, KEY)
                        .addStatement("$T $N = $N.get($N)", BITMAP_CLASS, BITMAP, cacheField, KEY)
                        .beginControlFlow("if ($N != null && !$N.isRecycled())", BITMAP, BITMAP)
                        .addStatement("if ($N) $T.d($S, \"Using cached bitmap for key: \" + $N)", DEBUG, LOG_CLASS, mClassName, KEY)
                        .addStatement("return $N", BITMAP)
                        .nextControlFlow("else")
                        .addStatement("if ($N) $T.d($S, \"Ejecting recycled bitmap for key: \" + $N)", DEBUG, LOG_CLASS, mClassName, KEY)
                        .addStatement("$N.remove($N)", cacheField, KEY)
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("$T $N = null", BITMAP_CLASS, BITMAP)
                        .beginControlFlow("try")
                        .addStatement("$T $N = open($N)", InputStream.class, STREAM, imageAsset)
                        .beginControlFlow("try")
                        .addStatement("$N = $T.decodeStream($N)", BITMAP, BITMAP_FACTORY_CLASS, STREAM)
                        .nextControlFlow("finally")
                        .addStatement("$N.close()", STREAM)
                        .endControlFlow()
                        .nextControlFlow("catch ($T e)", IOException.class)
                        .addStatement("$T.e($S, \"Failed to load bitmap for key: \" + $N, e)", LOG_CLASS, mClassName, KEY)
                        .addStatement("e.printStackTrace()")
                        .nextControlFlow("finally")
                        .beginControlFlow("if ($N != null)", BITMAP)
                        .addStatement("if ($N) $T.d($S, \"Bitmap loaded for key: \" + $N)", DEBUG, LOG_CLASS, mClassName, KEY)
                        .addStatement("$N.put($N, $N)", cacheField, KEY, BITMAP)
                        .endControlFlow()
                        .addStatement("return $N", BITMAP)
                        .endControlFlow()
                        .build())
                .returns(BITMAP_CLASS);
        addNullability(builder, NULLABLE);
        return builder.build();
    }

    @NonNull
    private MethodSpec createTypefaceMethod() {
        String fontAsset = "fontAsset";
        String cacheField = asFieldName(TYPEFACE_CACHE);

        TypeName assetType = TypeVariableName.get(FontAsset.getTypeName());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(GET_TYPEFACE)
                .addModifiers(mStaticMode ? PUBLIC : PRIVATE)
                .addParameter(createParameter(fontAsset, assetType, NONNULL))
                .addCode(CodeBlock.builder()
                        .addStatement("$T $N = $N.mPath", String.class, KEY, fontAsset)
                        .beginControlFlow("if ($N.containsKey($N))", cacheField, KEY)
                        .addStatement("return $N.get($N)", cacheField, KEY)
                        .endControlFlow()
                        .addStatement("$T $N = null", TYPEFACE_CLASS, TYPEFACE)
                        .beginControlFlow("try")
                        .addStatement("$N = $T.createFromAsset($N, $N)", TYPEFACE, TYPEFACE_CLASS, asFieldName(ASSET_MANAGER), KEY)
                        .nextControlFlow("catch ($T e)", RuntimeException.class)
                        .addStatement("$T.e($S, \"Failed to load typeface for key: \" + $N, e)", LOG_CLASS, mClassName, KEY)
                        .addStatement("e.printStackTrace()")
                        .nextControlFlow("finally")
                        .beginControlFlow("if ($N != null)", TYPEFACE)
                        .addStatement("$N.put($N, $N)", cacheField, KEY, TYPEFACE)
                        .endControlFlow()
                        .addStatement("return $N", TYPEFACE)
                        .endControlFlow()
                        .build())
                .returns(TYPEFACE_CLASS);
        addNullability(builder, NULLABLE);
        return builder.build();
    }

    @NonNull
    private MethodSpec createInputStreamMethod(boolean mode) {
        String asset = "asset";
        TypeName assetType = TypeVariableName.get(Asset.getTypeName());
        MethodSpec.Builder builder = MethodSpec.methodBuilder(OPEN)
                .addModifiers(mStaticMode ? PUBLIC : PRIVATE)
                .addParameter(createParameter("asset", assetType, NONNULL))
                .addException(IOException.class)
                .returns(InputStream.class);
        addNullability(builder, NONNULL);

        String modeName = "mode";
        if (mode) {
            builder.addParameter(createPrimitiveParameter(modeName, int.class))
                    .addStatement("return $N.open($N.mPath, $N)", asFieldName(ASSET_MANAGER), asset, modeName);
        } else {
            builder.addStatement("return $N.open($N.mPath)", asFieldName(ASSET_MANAGER), asset);
        }

        return builder.build();
    }

    @NonNull
    private MethodSpec createBitmapClearMethod() {
        return MethodSpec.methodBuilder(CLEAR + capitalise(BITMAP_CACHE))
                .addModifiers(PUBLIC)
                .beginControlFlow("for ($T $N : $N.values())", BITMAP_CLASS, BITMAP, asFieldName(BITMAP_CACHE))
                .addStatement("$N.recycle()", BITMAP)
                .endControlFlow()
                .addStatement("$N.clear()", asFieldName(BITMAP_CACHE))
                .build();
    }

    @NonNull
    private FieldSpec createListField(@NonNull TypeName typeName,
                                      @NonNull String fieldName,
                                      @NonNull Map<String, Asset> assets) {
        FieldSpec.Builder builder = FieldSpec.builder(typeName, fieldName)
                .addModifiers(PUBLIC, FINAL);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        }

        return builder.initializer(CodeBlock.builder()
                .add("$T.unmodifiableList($T.asList(", Collections.class, Arrays.class)
                .add(Joiner.on(", ").join(Iterators.transform(assets.entrySet().iterator(),
                        new Function<Map.Entry<String, Asset>, String>() {
                            @Override
                            public String apply(Map.Entry<String, Asset> entry) {
                                return entry != null ? entry.getKey() : null;
                            }
                        })) + "))")
                .build())
                .build();
    }

    @NonNull
    private FieldSpec createNonStaticClassField(@NonNull String rootName) {
        validateNonStatic();

        TypeName typeName = TypeVariableName.get(makeClassName(rootName));
        FieldSpec.Builder builder = FieldSpec.builder(typeName, rootName)
                .addModifiers(PUBLIC, FINAL)
                .initializer("new $T()", typeName);
        addNullability(builder, NONNULL);
        return builder.build();
    }

    @NonNull
    private FieldSpec createAssetField(@NonNull Asset asset) {
        FieldSpec.Builder builder = FieldSpec.builder(TypeVariableName.get(Asset.getTypeName()), asset.getFieldName())
                .addModifiers(PUBLIC, FINAL);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        }

        asset.addInitialiser(builder);
        return builder.build();
    }

    @NonNull
    private FieldSpec createFontAssetField(@NonNull FontAsset asset) {
        FieldSpec.Builder builder = FieldSpec.builder(TypeVariableName.get(FontAsset.getTypeName()), asset.getFieldName())
                .addModifiers(PUBLIC, FINAL);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        }

        asset.addInitialiser(builder);
        return builder.build();
    }

    @NonNull
    private FieldSpec createImageAssetField(@NonNull ImageAsset asset) {
        FieldSpec.Builder builder = FieldSpec.builder(TypeVariableName.get(ImageAsset.getTypeName()), asset.getFieldName())
                .addModifiers(PUBLIC, FINAL);

        if (mStaticMode) {
            builder.addModifiers(STATIC);
        }

        asset.addInitialiser(builder);
        return builder.build();
    }

    @NonNull
    private static List<File> getFileList(@NonNull File directory) {
        if (!directory.exists() || !directory.isDirectory()) {
            throw new IllegalArgumentException("Crate: Invalid file passed: " + directory.getAbsolutePath());
        }

        List<File> files = new LinkedList<>();

        File[] fileArray = directory.listFiles();
        if (fileArray != null) {
            Arrays.sort(fileArray, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    boolean o1Directory = o1.isDirectory();
                    boolean o2Directory = o2.isDirectory();

                    if ((o1Directory && o2Directory) || (!o1Directory && !o2Directory)) {
                        return o1.getName().compareToIgnoreCase(o2.getName());
                    } else {
                        return o1Directory ? -1 : 1;
                    }
                }
            });

            Collections.addAll(files, fileArray);
        }

        return files;
    }

    @Nullable
    private static String getFontName(@NonNull String filePath) {
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            inputStream.close();
            return font.getName();
        } catch (FontFormatException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    private static String getFileExtension(@NonNull String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    @NonNull
    private static String sanitiseFieldName(@NonNull String fileName) {
        // JavaPoet doesn't like the dollar signs so we remove them too
        char[] charArray = fileName.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            if (!Character.isJavaIdentifierPart(charArray[i]) || charArray[i] == '$') {
                charArray[i] = '_';
            }
        }

        if (!Character.isJavaIdentifierStart(charArray[0]) || charArray[0] == '$') {
            return "_" + new String(charArray);
        } else {
            return new String(charArray);
        }
    }

    @SuppressWarnings("unused")
    private static class Asset {

        @NonNull
        final String mFieldName;

        @NonNull
        final String mPath;

        @NonNull
        final String mName;

        private Asset(@NonNull String fieldName, @NonNull String path, @NonNull String name) {
            mFieldName = fieldName;
            mPath = path;
            mName = name;
        }

        @NonNull
        public String getFieldName() {
            return mFieldName;
        }

        @NonNull
        public String getPath() {
            return mPath;
        }

        @NonNull
        public String getName() {
            return mName;
        }

        public void addInitialiser(@NonNull FieldSpec.Builder builder) {
            builder.initializer("new $N($S, $S)", getTypeName(), mPath, mName);
        }

        @NonNull
        public static String getTypeName() {
            return Asset.class.getSimpleName();
        }

        @NonNull
        public static String[] getFields() {
            return new String[]{"path", "name"};
        }
    }

    @SuppressWarnings("unused")
    private static final class FontAsset extends Asset {

        @NonNull
        final String mFontName;

        private FontAsset(@NonNull String fieldName,
                          @NonNull String path,
                          @NonNull String name,
                          @NonNull String fontName) {
            super(fieldName, path, name);
            mFontName = fontName;
        }

        @NonNull
        public String getFontName() {
            return mFontName;
        }

        public void addInitialiser(@NonNull FieldSpec.Builder builder) {
            builder.initializer("new $N($S, $S, $S)", getTypeName(), mPath, mName, mFontName);
        }

        @NonNull
        public static String getTypeName() {
            return FontAsset.class.getSimpleName();
        }

        @NonNull
        public static String[] getFields() {
            return new String[]{"fontName"};
        }
    }

    @SuppressWarnings("unused")
    private static final class ImageAsset extends Asset {

        final int mWidth;

        final int mHeight;

        private ImageAsset(@NonNull String fieldName,
                           @NonNull String path,
                           @NonNull String name,
                           int width,
                           int height) {
            super(fieldName, path, name);
            mWidth = width;
            mHeight = height;
        }

        public int getWidth() {
            return mWidth;
        }

        public int getHeight() {
            return mHeight;
        }

        public void addInitialiser(@NonNull FieldSpec.Builder builder) {
            builder.initializer("new $N($S, $S, $L, $L)", getTypeName(), mPath, mName, mWidth, mHeight);
        }

        @NonNull
        public static String getTypeName() {
            return ImageAsset.class.getSimpleName();
        }

        @NonNull
        public static String[] getFields() {
            return new String[]{"width", "height"};
        }
    }
}