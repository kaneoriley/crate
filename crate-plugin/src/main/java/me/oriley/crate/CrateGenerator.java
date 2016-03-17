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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Locale.US;
import static javax.lang.model.element.Modifier.*;
import static me.oriley.crate.utils.JavaPoetUtils.*;
import static me.oriley.crate.utils.JavaPoetUtils.Nullability.NONNULL;

public final class CrateGenerator {

    private enum FolderClass {
        NONE, FONT, IMAGE, SVG, ASSET
    }

    // Deprecated options
    private static final String PACKAGE_NAME = CrateGenerator.class.getPackage().getName();
    private static final String CLASS_NAME = "CrateDictionary";
    private static final boolean STATIC_MODE = false;

    private static final String CRATE_HASH = CrateHasher.getActualHash();
    private static final String ASSETS = "assets";
    private static final String DEBUG = "debug";
    private static final String CLASS = "Class";

    private static final List<String> FONT_TYPES = Arrays.asList("application/x-font-otf", "application/x-font-ttf");
    private static final List<String> IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png", "image/pjpeg", "image/gif", "image/bmp", "image/x-windows-bmp", "image/webp");
    private static final List<String> SVG_TYPES = Arrays.asList("image/svg+xml", "image/svg+xml-compressed");

    private static final Logger log = LoggerFactory.getLogger(CrateGenerator.class.getSimpleName());

    @NonNull
    private final String mBaseOutputDir;

    @NonNull
    private final String mVariantAssetDir;

    private final boolean mDebugLogging;

    public CrateGenerator(@NonNull String baseOutputDir,
                          @NonNull String variantAssetDir,
                          boolean debugLogging) {
        mBaseOutputDir = baseOutputDir;
        mVariantAssetDir = variantAssetDir;
        mDebugLogging = debugLogging;
        log("CrateGenerator constructed\n" +
                "    Output: " + mBaseOutputDir + "\n" +
                "    Asset: " + mVariantAssetDir + "\n" +
                "    Package: " + PACKAGE_NAME + "\n" +
                "    Class: " + CLASS_NAME + "\n" +
                "    Static: " + STATIC_MODE + "\n" +
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
            brewJava(variantDir, mVariantAssetDir, PACKAGE_NAME).writeTo(new File(mBaseOutputDir));
        } catch (IOException e) {
            logError("Failed to generate java", e, true);
        }

        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        log("Time to build was " + lengthMillis + "ms");
    }

    public boolean isCrateHashValid() {
        String crateOutputFile = mBaseOutputDir + '/' + PACKAGE_NAME.replace('.', '/') + "/" + CLASS_NAME + ".java";
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

        TypeSpec.Builder builder = TypeSpec.classBuilder(CLASS_NAME)
                .addModifiers(PUBLIC, FINAL)
                .addAnnotation(createSuppressWarningAnnotation("unused"));

        builder.addField(createBooleanField(DEBUG, mDebugLogging));

        TreeMap<String, Asset> allAssets = new TreeMap<>();
        listFiles(allAssets, builder, "", variantDir, variantAssetDir, true);

        JavaFile.Builder javaBuilder = JavaFile.builder(packageName, builder.build())
                .indent("    ");

        for (String comment : getComments()) {
            javaBuilder.addFileComment(comment + "\n");
        }

        return javaBuilder.build();
    }

    @NonNull
    private String[] getComments() {
        return new String[]{CRATE_HASH, "Package: " + PACKAGE_NAME, "Class: " + CLASS_NAME, "Static: " + STATIC_MODE,
                "Debug: " + mDebugLogging};
    }

    private void listFiles(@NonNull TreeMap<String, Asset> allAssets,
                           @NonNull TypeSpec.Builder parentBuilder,
                           @NonNull String classPathString,
                           @NonNull File directory,
                           @NonNull String variantAssetDir,
                           boolean root) {

        String rootName = root ? ASSETS : directory.getName();
        TypeSpec.Builder builder = TypeSpec.classBuilder(capitalise(rootName + CLASS))
                .addModifiers(PUBLIC, STATIC, FINAL);

        List<File> files = getFileList(directory);
        TreeMap<String, Asset> assetMap = new TreeMap<>();
        FolderClass folderClass = FolderClass.NONE;

        for (File file : files) {
            if (file.isDirectory()) {
                listFiles(allAssets, builder, classPathString + file.getName() + ".", file, variantAssetDir, false);
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

                String contentType = getContentType(file);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                String filePath = file.getPath().replace(variantAssetDir + "/", "");
                AssetHolder asset;

                if (FONT_TYPES.contains(contentType)) {
                    folderClass = checkFolderClass(folderClass, FolderClass.FONT);
                    String fontName = getFontName(file.getPath());
                    asset = new FontAssetHolder(fieldName, filePath, fileName, fontName != null ? fontName : fileName);
                    builder.addField(createFontAssetField((FontAssetHolder) asset));
                } else if (IMAGE_TYPES.contains(contentType)) {
                    folderClass = checkFolderClass(folderClass, FolderClass.IMAGE);
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

                    asset = new ImageAssetHolder(fieldName, filePath, fileName, width, height);
                    builder.addField(createImageAssetField((ImageAssetHolder) asset));
                } else if (SVG_TYPES.contains(contentType)) {
                    folderClass = checkFolderClass(folderClass, FolderClass.SVG);
                    asset = new SvgAssetHolder(fieldName, filePath, fileName);
                    builder.addField(createSvgAssetField((SvgAssetHolder) asset));
                } else {
                    folderClass = FolderClass.ASSET;
                    asset = new AssetHolder(fieldName, filePath, fileName);
                    builder.addField(createAssetField(asset));
                }
                assetMap.put(fieldName, asset);
                allAssets.put(classPathString + fieldName, asset);
            }
        }

        if (!assetMap.isEmpty()) {
            TypeName elementType = TypeVariableName.get(getFolderClass(folderClass));
            TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), elementType);
            builder.addField(createListField(listType, "LIST", assetMap));
        }

        if (root && !allAssets.isEmpty()) {
            TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class),
                    TypeVariableName.get(Asset.class));
            builder.addField(createListField(listType, "FULL_LIST", allAssets));
        }

        parentBuilder.addType(builder.build());
        parentBuilder.addField(createNonStaticClassField(rootName));
    }

    @NonNull
    private FolderClass checkFolderClass(@NonNull FolderClass original, @NonNull FolderClass current) {
        if (original == FolderClass.NONE) {
            return current;
        } else if (original != current) {
            return FolderClass.ASSET;
        } else {
            return original;
        }
    }

    @NonNull
    private Class getFolderClass(@NonNull FolderClass folderAssetClass) {
        switch (folderAssetClass) {
            case NONE:
            case ASSET:
            default:
                return Asset.class;
            case FONT:
                return FontAsset.class;
            case IMAGE:
                return ImageAsset.class;
            case SVG:
                return SvgAsset.class;
        }
    }

    @Nullable
    private String getContentType(@NonNull File file) {
        try {
            return Files.probeContentType(file.toPath());
        } catch (IOException e) {
            return null;
        }
    }

    @NonNull
    private FieldSpec createListField(@NonNull TypeName typeName,
                                      @NonNull String fieldName,
                                      @NonNull Map<String, Asset> assets) {
        FieldSpec.Builder builder = FieldSpec.builder(typeName, fieldName)
                .addModifiers(PUBLIC, FINAL);

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
        TypeName typeName = TypeVariableName.get(capitalise(rootName + CLASS));
        FieldSpec.Builder builder = FieldSpec.builder(typeName, rootName)
                .addModifiers(PUBLIC, FINAL)
                .initializer("new $T()", typeName);
        addNullability(builder, NONNULL);
        return builder.build();
    }

    @NonNull
    private FieldSpec createAssetField(@NonNull AssetHolder asset) {
        FieldSpec.Builder builder = FieldSpec.builder(Asset.class, asset.mFieldName)
                .addModifiers(PUBLIC, FINAL);
        asset.addInitialiser(builder);
        return builder.build();
    }

    @NonNull
    private FieldSpec createFontAssetField(@NonNull FontAssetHolder asset) {
        FieldSpec.Builder builder = FieldSpec.builder(FontAsset.class, asset.mFieldName)
                .addModifiers(PUBLIC, FINAL);
        asset.addInitialiser(builder);
        return builder.build();
    }

    @NonNull
    private FieldSpec createImageAssetField(@NonNull ImageAssetHolder asset) {
        FieldSpec.Builder builder = FieldSpec.builder(ImageAsset.class, asset.mFieldName)
                .addModifiers(PUBLIC, FINAL);
        asset.addInitialiser(builder);
        return builder.build();
    }

    @NonNull
    private FieldSpec createSvgAssetField(@NonNull SvgAssetHolder asset) {
        FieldSpec.Builder builder = FieldSpec.builder(SvgAsset.class, asset.mFieldName)
                .addModifiers(PUBLIC, FINAL);
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
    private static class AssetHolder extends Asset {

        @NonNull
        final String mFieldName;

        private AssetHolder(@NonNull String fieldName,
                            @NonNull String path,
                            @NonNull String name) {
            super(path, name);
            mFieldName = fieldName;
        }

        public void addInitialiser(@NonNull FieldSpec.Builder builder) {
            builder.initializer("new $T($S, $S)", Asset.class, mPath, mName);
        }
    }

    @SuppressWarnings("unused")
    private static final class FontAssetHolder extends AssetHolder {

        @NonNull
        final String mFontName;

        private FontAssetHolder(@NonNull String fieldName,
                                @NonNull String path,
                                @NonNull String name,
                                @NonNull String fontName) {
            super(fieldName, path, name);
            mFontName = fontName;
        }

        public void addInitialiser(@NonNull FieldSpec.Builder builder) {
            builder.initializer("new $T($S, $S, $S)", FontAsset.class, mPath, mName, mFontName);
        }
    }

    @SuppressWarnings("unused")
    private static final class ImageAssetHolder extends AssetHolder {

        final int mWidth;

        final int mHeight;

        private ImageAssetHolder(@NonNull String fieldName,
                                 @NonNull String path,
                                 @NonNull String name,
                                 int width,
                                 int height) {
            super(fieldName, path, name);
            mWidth = width;
            mHeight = height;
        }

        public void addInitialiser(@NonNull FieldSpec.Builder builder) {
            builder.initializer("new $T($S, $S, $L, $L)", ImageAsset.class, mPath, mName, mWidth, mHeight);
        }
    }

    @SuppressWarnings("unused")
    private static final class SvgAssetHolder extends AssetHolder {

        private SvgAssetHolder(@NonNull String fieldName,
                               @NonNull String path,
                               @NonNull String name) {
            super(fieldName, path, name);
        }

        public void addInitialiser(@NonNull FieldSpec.Builder builder) {
            builder.initializer("new $T($S, $S)", SvgAsset.class, mPath, mName);
        }
    }
}