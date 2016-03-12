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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

import static java.util.Locale.US;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;

public final class CrateGenerator {

    private static final String CRATE_HASH = CrateHasher.getActualHash();

    private static final String OTF_EXTENSION = "otf";
    private static final String TTF_EXTENSION = "ttf";

    private static int sCurrentDepth;

    public static void writeJava(@Nonnull String crateOutputFile,
                                 @Nonnull String variantAssetDir,
                                 @Nonnull String packageName) {
        sCurrentDepth = 4;

        File file = new File(variantAssetDir);
        if (!file.exists() || !file.isDirectory()) {
            return;
        }

        File javaFile = new File(crateOutputFile);
        if (javaFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            javaFile.delete();
        }

        //noinspection ResultOfMethodCallIgnored
        javaFile.getParentFile().mkdirs();
        if (!javaFile.getParentFile().exists() || !javaFile.getParentFile().isDirectory()) {
            throw new IllegalStateException("Crate: Output dir for " + crateOutputFile + " does not exist!");
        }

        // TODO: Use JavaPoet?
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(crateOutputFile, "UTF-8");
            writer.println("// " + CRATE_HASH + " -- DO NOT EDIT THIS LINE\n");
            writer.println("package " + packageName + ";\n");
            writer.println("import android.content.Context;");
            writer.println("import android.content.res.AssetManager;");
            writer.println("import android.support.annotation.NonNull;");
            writer.println("import android.support.annotation.Nullable;");
            writer.println("import java.io.IOException;");
            writer.println("import java.io.InputStream;");
            writer.println("import java.util.Arrays;");
            writer.println("import java.util.List;\n");

            writer.println("@SuppressWarnings(\"unused\")");
            writer.println("public final class Crate {\n");

            writer.println("    @NonNull");
            writer.println("    private final Context mContext;\n");

            writer.println("    @Nullable");
            writer.println("    private AssetManager mManager;\n");

            writer.println("    public Crate(@NonNull Context context) {");
            writer.println("        mContext = context.getApplicationContext();");
            writer.println("    }\n");

            writer.println("    @NonNull");
            writer.println("    public InputStream get(@NonNull Asset asset) throws IOException {");
            writer.println("        return getManager().open(asset.mPath);");
            writer.println("    }\n");

            writer.println("    @NonNull");
            writer.println("    public InputStream get(@NonNull Asset asset, int mode) throws IOException {");
            writer.println("        return getManager().open(asset.mPath, mode);");
            writer.println("    }\n");

            writer.println("    @NonNull");
            writer.println("    private AssetManager getManager() {");
            writer.println("        if (mManager == null) {");
            writer.println("            mManager = mContext.getAssets();");
            writer.println("        }");
            writer.println("        return mManager;");
            writer.println("    }\n");

            writer.println("    @NonNull");
            writer.println("    public void close() {");
            writer.println("        if (mManager != null) {");
            writer.println("            mManager.close();");
            writer.println("            mManager = null;");
            writer.println("        }");
            writer.println("    }\n");

            writer.println("    public static class Asset {\n");

            writer.println("        @NonNull");
            writer.println("        private final String mPath;\n");

            writer.println("        @NonNull");
            writer.println("        private final String mName;\n");

            writer.println("        private Asset(@NonNull String path, @NonNull String name) {");
            writer.println("            mPath = path;");
            writer.println("            mName = name;");
            writer.println("        }\n");

            writer.println("        @NonNull");
            writer.println("        public String getPath() {");
            writer.println("            return mPath;");
            writer.println("        }\n");

            writer.println("        @NonNull");
            writer.println("        public String getName() {");
            writer.println("            return mName;");
            writer.println("        }\n");
            writer.println("    }\n");

            writer.println("    public static final class FontAsset extends Asset {\n");

            writer.println("        @NonNull");
            writer.println("        private final String mFontName;\n");

            writer.println("        private FontAsset(@NonNull String path, @NonNull String name, @NonNull String fontName) {");
            writer.println("            super(path, name);");
            writer.println("            mFontName = fontName;");
            writer.println("        }\n");

            writer.println("        @NonNull");
            writer.println("        public String getFontName() {");
            writer.println("            return mFontName;");
            writer.println("        }\n");
            writer.println("    }\n");

            listFiles(writer, file, variantAssetDir, true);

            writer.println("}");
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Crate: IOException when generating class");
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static void listFiles(@Nonnull PrintWriter writer,
                                  @Nonnull File directory,
                                  @Nonnull String variantAssetDir,
                                  boolean root) {
        if (!root) {
            writer.println("" + getIndent() + "public static final class " + directory.getName() + " {\n");
            sCurrentDepth += 4;
        }

        List<File> files = getFileList(directory);
        List<Asset> assets = new ArrayList<>();
        boolean isFontFolder = true;

        for (File file : files) {
            if (file.isDirectory()) {
                listFiles(writer, file, variantAssetDir, false);
            } else {
                String fileName = file.getName();
                String assetName = sanitiseFilename(fileName).toUpperCase(US);
                String filePath = file.getPath().replace(variantAssetDir + "/", "");

                String fileExtension = getFileExtension(fileName);
                Asset asset;
                if (equalsIgnoreCase(fileExtension, TTF_EXTENSION) || equalsIgnoreCase(fileExtension, OTF_EXTENSION)) {
                    String fontName = getFontName(file.getPath());
                    asset = new FontAsset(filePath, assetName, fontName != null ? fontName : fileName);
                } else {
                    isFontFolder = false;
                    asset = new Asset(filePath, assetName);
                }
                assets.add(asset);

                String className = asset.getClass().getSimpleName();
                String fontName = (asset instanceof FontAsset ? (", \"" + ((FontAsset) asset).getFontName() + "\"") : "");
                writer.println(getIndent() + "public static final " + className + " " + assetName + " = new " +
                        className + "(\"" + filePath + "\", \"" + fileName + "\"" + fontName + ");");
            }
        }

        if (!assets.isEmpty()) {
            String listClass = isFontFolder ? "FontAsset" : "Asset";
            writer.println("\n" + getIndent() + "public static final List<" + listClass +
                    "> LIST = Arrays.asList(\n" + getIndent() + "        " +
                    Joiner.on(",\n" + getIndent() + "        ").join(Iterables.transform(assets,
                            new Function<Asset, String>() {
                                @Override
                                public String apply(Asset asset) {
                                    return asset != null ? asset.getName() : null;
                                }
                            })) + " );");
        }

        if (!root) {
            sCurrentDepth -= 4;
            writer.println("\n" + getIndent() + "}\n");
        }
    }

    public static boolean isCrateHashValid(@Nonnull String crateOutputFile) {
        File file = new File(crateOutputFile);
        return file.exists() && file.isFile() && CrateHasher.isHashValid(file, CRATE_HASH);
    }

    @Nonnull
    private static String getIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sCurrentDepth; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    @Nonnull
    private static List<File> getFileList(@Nonnull File directory) {
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
    private static String getFontName(@Nonnull String filePath) {
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

    @Nonnull
    private static String getFileExtension(@Nonnull String fileName) {
        String extension = "";
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            extension = fileName.substring(i + 1);
        }
        return extension;
    }

    @Nonnull
    private static String sanitiseFilename(@Nonnull String fileName) {
        return fileName.replace(" ", "_").replace("-", "_").replace(".", "_");
    }

    @SuppressWarnings("unused")
    private static class Asset {

        @Nonnull
        private final String mPath;

        @Nonnull
        private final String mName;

        private Asset(@Nonnull String path, @Nonnull String name) {
            mPath = path;
            mName = name;
        }

        @Nonnull
        public String getPath() {
            return mPath;
        }

        @Nonnull
        public String getName() {
            return mName;
        }
    }

    @SuppressWarnings("unused")
    private static final class FontAsset extends Asset {

        @Nonnull
        private final String mFontName;

        private FontAsset(@Nonnull String path, @Nonnull String name, @Nonnull String fontName) {
            super(path, name);
            mFontName = fontName;
        }

        @Nonnull
        public String getFontName() {
            return mFontName;
        }
    }
}