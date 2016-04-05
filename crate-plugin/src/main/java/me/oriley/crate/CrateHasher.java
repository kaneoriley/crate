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

package me.oriley.crate;

import android.support.annotation.NonNull;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class CrateHasher {

    private static final String MD5 = "MD5";

    // http://stackoverflow.com/a/20814872/4516144
    @NonNull
    public static String getActualHash() {

        File currentJavaJarFile = new File(CrateHasher.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        String filepath = currentJavaJarFile.getAbsolutePath();
        StringBuilder sb = new StringBuilder();

        try {
            MessageDigest md = MessageDigest.getInstance(MD5);
            FileInputStream fis = new FileInputStream(filepath);
            byte[] dataBytes = new byte[1024];

            int position;
            while ((position = fis.read(dataBytes)) != -1)
                md.update(dataBytes, 0, position);

            byte[] digestBytes = md.digest();

            for (byte digestByte : digestBytes) {
                sb.append(Integer.toString((digestByte & 0xff) + 0x100, 16).substring(1));
            }
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }
}