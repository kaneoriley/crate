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

import javax.lang.model.element.Modifier;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Locale.US;
import static javax.lang.model.element.Modifier.*;
import static me.oriley.crate.utils.JavaPoetUtils.*;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

public final class CrateGenerator {

    private static final String DEFAULT_CLASS_NAME = "Crate";
    private static final String CRATE_HASH = CrateHasher.getActualHash();
    private static final String ASSETS = "assets";

    private static final ClassName ASSETMANAGER_CLASS = ClassName.get("android.content.res", "AssetManager");
    private static final ClassName CONTEXT_CLASS = ClassName.get("android.content", "Context");

    private static final String OTF_EXTENSION = "otf";
    private static final String TTF_EXTENSION = "ttf";

    private static final Logger log = LoggerFactory.getLogger(CrateGenerator.class.getSimpleName());

    @NonNull
    private final String mBaseOutputDir;

    @NonNull
    private final String mVariantAssetDir;

    @NonNull
    private final String mPackageName;

    @NonNull
    private final String mClassName;

    private final boolean mStaticFields;

    private final boolean mDebugLogging;

    public CrateGenerator(@NonNull String baseOutputDir,
                          @NonNull String variantAssetDir,
                          @NonNull String packageName,
                          @Nullable String className,
                          boolean staticFields,
                          boolean debugLogging) {
        mBaseOutputDir = baseOutputDir;
        mVariantAssetDir = variantAssetDir;
        mPackageName = packageName;
        mClassName = className != null ? className : DEFAULT_CLASS_NAME;
        mStaticFields = staticFields;
        mDebugLogging = debugLogging;
        log("CrateGenerator constructed\n" +
                "    Output: " + mBaseOutputDir + "\n" +
                "    Asset: " + mVariantAssetDir + "\n" +
                "    Package: " + mPackageName + "\n" +
                "    Class: " + mClassName + "\n" +
                "    Static: " + mStaticFields + "\n" +
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
                .addAnnotation(createSuppressWarningAnnotation("unused"));

        builder.addField(createField(CONTEXT_CLASS, false, PRIVATE, FINAL))
                .addField(createField(ASSETMANAGER_CLASS, true, PRIVATE));

        TreeMap<String, Asset> allAssets = new TreeMap<>();
        listFiles(allAssets, builder, ASSETS, variantDir, variantAssetDir, true);

        if (!allAssets.isEmpty()) {
            TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class),
                    TypeVariableName.get(Asset.getTypeName()));
            builder.addField(createListField(listType, "FULL_LIST", allAssets));
        }

        String asset = "asset";
        TypeName assetType = TypeVariableName.get(Asset.getTypeName());
        builder.addMethod(createCrateConstructor())
                .addMethod(createInputStreamMethod(asset, assetType, false, PUBLIC))
                .addMethod(createInputStreamMethod(asset, assetType, true, PUBLIC))
                .addMethod(createGetManagerMethod())
                .addMethod(createCloseManagerMethod());

        JavaFile.Builder javaBuilder = JavaFile.builder(packageName, builder.build())
                .indent("    ");

        for (String comment : getComments()) {
            javaBuilder.addFileComment(comment + "\n");
        }

        return javaBuilder.build();
    }

    @NonNull
    private String[] getComments() {
        return new String[] { CRATE_HASH, "Package: " + mPackageName, "Class: " + mClassName, "Static: " + mStaticFields};
    }

    private void listFiles(@NonNull TreeMap<String, Asset> allAssets,
                           @NonNull TypeSpec.Builder parentBuilder,
                           @NonNull String classPathString,
                           @NonNull File directory,
                           @NonNull String variantAssetDir,
                           boolean root) {

        String rootName = root ? ASSETS : directory.getName();
        TypeSpec.Builder builder = TypeSpec.classBuilder(makeClassName(rootName))
                .addModifiers(PUBLIC, STATIC, FINAL);

        List<File> files = getFileList(directory);
        TreeMap<String, Asset> assetMap = new TreeMap<>();
        boolean isFontFolder = true;

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

                String fileExtension = getFileExtension(fileName);
                Asset asset;
                if (equalsIgnoreCase(fileExtension, TTF_EXTENSION) || equalsIgnoreCase(fileExtension, OTF_EXTENSION)) {
                    String fontName = getFontName(file.getPath());
                    asset = new FontAsset(fieldName, filePath, fileName, fontName != null ? fontName : fileName);
                    builder.addField(createFontAssetField((FontAsset) asset));
                } else {
                    isFontFolder = false;
                    asset = new Asset(fieldName, filePath, fileName);
                    builder.addField(createAssetField(asset));
                }
                assetMap.put(fieldName, asset);
                allAssets.put(classPathString + "." + fieldName, asset);
            }
        }

        if (!assetMap.isEmpty()) {
            TypeName elementType = TypeVariableName.get(isFontFolder ? FontAsset.getTypeName() : Asset.getTypeName());
            TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), elementType);
            builder.addField(createListField(listType, "LIST", assetMap));
        }
        parentBuilder.addType(builder.build());

        if (!mStaticFields) {
            parentBuilder.addField(createNonStaticClassField(rootName));
        }
    }

    @NonNull
    private String makeClassName(@NonNull String rootClassName) {
        if (mStaticFields) {
            return rootClassName;
        } else {
            return capitalise(rootClassName + "Class");
        }
    }

    @NonNull
    private TypeSpec createAssetClass() {
        String[] fields = Asset.getFields();

        TypeSpec.Builder builder = TypeSpec.classBuilder(Asset.getTypeName())
                .addModifiers(PUBLIC, STATIC)
                .addMethod(createConstructor(fields));

        for (String field : fields) {
            builder.addField(createStringField(toInstance(field), false, PRIVATE, FINAL))
                    .addMethod(createGetter(field, String.class, false, PUBLIC));
        }

        return builder.build();
    }

    @NonNull
    private TypeSpec createFontAssetClass() {
        TypeSpec.Builder builder = TypeSpec.classBuilder(FontAsset.getTypeName())
                .addModifiers(PUBLIC, STATIC)
                .superclass(TypeVariableName.get(Asset.getTypeName()));

        String[] fields = FontAsset.getFields();
        for (String field : fields) {
            builder.addField(createStringField(toInstance(field), false, PRIVATE, FINAL))
                    .addMethod(createGetter(field, String.class, false, PUBLIC));
        }

        builder.addMethod(createSubConstructor(Asset.getFields(), fields));
        return builder.build();
    }

    @NonNull
    private MethodSpec createCrateConstructor() {
        String context = "context";

        return MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(createParameter(context, CONTEXT_CLASS, false))
                .addStatement("$N = $N.getApplicationContext()", toInstance(context), context)
                .build();
    }

    @NonNull
    private MethodSpec createConstructor(@NonNull String... fields) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        for (String field : fields) {
            builder.addParameter(createParameter(field, String.class, false))
                    .addStatement("$N = $N", toInstance(field), field);
        }

        return builder.build();
    }

    @NonNull
    private MethodSpec createInputStreamMethod(@NonNull String paramName,
                                               @NonNull TypeName typeName,
                                               boolean mode,
                                               @NonNull Modifier... modifiers) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("get");

        builder.addModifiers(modifiers)
                .addAnnotation(createNullabilityAnnotation(false))
                .addParameter(createParameter(paramName, typeName, false))
                .addException(IOException.class)
                .returns(InputStream.class);

        String modeName = "mode";
        if (mode) {
            builder.addParameter(createPrimitiveParameter(modeName, int.class))
                    .addStatement("return getManager().open($N.mPath, $N)", paramName, modeName);
        } else {
            builder.addStatement("return getManager().open($N.mPath)", paramName);
        }

        return builder.build();
    }

    @NonNull
    private MethodSpec createGetManagerMethod() {
        return MethodSpec.methodBuilder("getManager")
                .addModifiers(PRIVATE)
                .addAnnotation(createNullabilityAnnotation(false))
                .beginControlFlow("if (mAssetManager == null)")
                .addStatement("mAssetManager = mContext.getAssets()")
                .endControlFlow()
                .addStatement("return mAssetManager")
                .returns(ASSETMANAGER_CLASS)
                .build();
    }

    @NonNull
    private MethodSpec createCloseManagerMethod() {
        return MethodSpec.methodBuilder("close")
                .addModifiers(PUBLIC)
                .beginControlFlow("if (mAssetManager != null)")
                .addStatement("mAssetManager.close()")
                .addStatement("mAssetManager = null")
                .endControlFlow()
                .build();
    }

    @NonNull
    private MethodSpec createSubConstructor(@NonNull String[] parentFields, @NonNull String[] fields) {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PRIVATE);

        builder.addStatement("super(" + Joiner.on(", ").join(parentFields) + ")");

        for (String field : parentFields) {
            builder.addParameter(createParameter(field, String.class, false));
        }

        for (String field : fields) {
            builder.addParameter(createParameter(field, String.class, false))
                    .addStatement("$N = $N", toInstance(field), field);
        }

        return builder.build();
    }

    @NonNull
    private FieldSpec createListField(@NonNull TypeName typeName,
                                      @NonNull String fieldName,
                                      @NonNull Map<String, Asset> assets) {
        FieldSpec.Builder builder = FieldSpec.builder(typeName, fieldName);

        if (mStaticFields) {
            builder.addModifiers(PUBLIC, STATIC, FINAL);
        } else {
            builder.addModifiers(PUBLIC, FINAL);
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
        TypeName typeName = TypeVariableName.get(makeClassName(rootName));
        return FieldSpec.builder(typeName, rootName)
                .addModifiers(PUBLIC, FINAL)
                .initializer("new $T()", typeName)
                .build();
    }

    @NonNull
    private FieldSpec createAssetField(@NonNull Asset asset) {
        FieldSpec.Builder builder = FieldSpec.builder(TypeVariableName.get(Asset.getTypeName()), asset.getFieldName());

        if (mStaticFields) {
            builder.addModifiers(PUBLIC, STATIC, FINAL);
        } else {
            builder.addModifiers(PUBLIC, FINAL);
        }

        asset.addInitialiser(builder);
        return builder.build();
    }

    @NonNull
    private FieldSpec createFontAssetField(@NonNull FontAsset asset) {
        FieldSpec.Builder builder = FieldSpec.builder(TypeVariableName.get(FontAsset.getTypeName()), asset.getFieldName());

        if (mStaticFields) {
            builder.addModifiers(PUBLIC, STATIC, FINAL);
        } else {
            builder.addModifiers(PUBLIC, FINAL);
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
}