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

import com.google.common.base.Joiner;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static java.util.Locale.US;

public final class CrateGenerator {

    @Nonnull
    private final String mJavaFilePath;

    @Nonnull
    private final String mAssetDir;

    @Nonnull
    private final String mPackageName;

    private int mDepth;

    CrateGenerator(@Nonnull String javaFilePath, @Nonnull String assetDir, @Nonnull String packageName) {
        mJavaFilePath = javaFilePath;
        mAssetDir = assetDir;
        mPackageName = packageName;
        mDepth = 4;
    }

    public void writeJava() {
        File file = new File(mAssetDir);
        if (!file.exists() || !file.isDirectory()) {
            return;
        }

        File javaFile = new File(mJavaFilePath);
        if (javaFile.exists()) {
            //noinspection ResultOfMethodCallIgnored
            javaFile.delete();
        }

        //noinspection ResultOfMethodCallIgnored
        javaFile.mkdirs();
        if (!javaFile.getParentFile().exists() || !javaFile.getParentFile().isDirectory()) {
            throw new IllegalStateException("Crate: Output dir for " + mJavaFilePath + " does not exist!");
        }

        // TODO: Use JavaPoet?
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(mJavaFilePath + "/Crate.java", "UTF-8");
            writer.println("package " + mPackageName + ";\n");
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

            writer.println("    public static final class Asset {\n");

            writer.println("        @NonNull");
            writer.println("        private final String mPath;\n");
            writer.println("        @NonNull");
            writer.println("        private final String mName;\n");

            writer.println("        private Asset(@NonNull String path, @NonNull String name) {");
            writer.println("            mPath = path;");
            writer.println("            mName = name;");
            writer.println("        }");

            writer.println("        @NonNull");
            writer.println("        public String getPath() {");
            writer.println("            return mPath;");
            writer.println("        }");

            writer.println("        @NonNull");
            writer.println("        public String getName() {");
            writer.println("            return mName;");
            writer.println("        }");
            writer.println("    }\n");

            listFiles(writer, file, true);

            writer.println("}");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private void listFiles(@Nonnull PrintWriter writer, @Nonnull File directory, boolean root) {
        if (!root) {
            writer.println("" + getIndent() + "public static final class " + directory.getName() + " {\n");
            mDepth += 4;
        }

        List<File> files = getFileList(directory);
        List<String> assetNames = new ArrayList<>();

        for (File file : files) {
            if (file.isDirectory()) {
                listFiles(writer, file, false);
            } else {
                String fileName = file.getName();
                String assetName = sanitiseFilename(fileName).toUpperCase(US);
                String filePath = file.getPath().replace(mAssetDir + "/", "");
                writer.println(getIndent() + "public static final Asset " + assetName + " = new Asset(\""
                        + filePath + "\", \"" + fileName + "\");");
                assetNames.add(assetName);
            }
        }

        if (!assetNames.isEmpty()) {
            writer.println("\n" + getIndent() + "public static final List<Asset> LIST = Arrays.asList(\n" + getIndent() + "        " +
                    Joiner.on(",\n" + getIndent() + "        ").join(assetNames) + " );");
        }

        if (!root) {
            mDepth -= 4;
            writer.println("\n" + getIndent() + "}\n");
        }
    }

    @Nonnull
    private String getIndent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mDepth; i++) {
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

    @Nonnull
    private static String sanitiseFilename(@Nonnull String fileName) {
        return fileName.replace(" ", "_").replace("-", "_").replace(".", "_");
    }
}