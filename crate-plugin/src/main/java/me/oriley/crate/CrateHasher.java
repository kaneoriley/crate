package me.oriley.crate;

import android.support.annotation.NonNull;

import org.apache.commons.lang.StringUtils;

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

    public static boolean isHashValid(@NonNull File crateOutputFile, @NonNull String currentHash) {
        if (StringUtils.isEmpty(currentHash)) {
            return false;
        }

        try {
            FileReader reader = new FileReader(crateOutputFile);
            BufferedReader input = new BufferedReader(reader);
            String firstLine = input.readLine();
            input.close();
            reader.close();

            return firstLine != null && firstLine.contains(currentHash);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}