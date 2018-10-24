package com.example.demo.arcgis;


import java.io.*;

public class FileUtil {

    public static String getCurrentDirPath() {
        String currentDir = new File(".").getAbsolutePath();
        if (currentDir.endsWith("/.")) {
            currentDir = currentDir.substring(0, currentDir.length() - 2);
        }
        return currentDir;
    }

    //to do
    public static String getAbsolutePath(String filepath) {
        String fileAbsolutePath = null;
        if (filepath != null) {
            File t = new File(filepath);
            if (t.exists()) {
                fileAbsolutePath = filepath;
            } else {
                fileAbsolutePath = getCurrentDirPath() + File.separator + filepath;
            }
        }
        return fileAbsolutePath;
    }

    public static void writeStringToFile(String content, String charset, String fileName) {
        if (content != null && charset != null) {
            try {
                byte[] data = content.getBytes(charset);
                writeByteArrayToFile(data, fileName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public static void inputStreamToFile(InputStream inputStream, String filename) {
        try {
            byte[] data = inputStreamToByteArray(inputStream);
            writeByteArrayToFile(data, filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String inputStreamToString(InputStream inputStream, String charset) {
        String content = null;

        try {
            byte[] data = inputStreamToByteArray(inputStream);
            content = new String(data, charset);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return content;
    }

    public static void writeByteArrayToFile(byte[] data, String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!file.exists()) {
            return;
        }

        try {
            FileOutputStream fout = new FileOutputStream(file);
            fout.write(data, 0, data.length);
            fout.flush();
            fout.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte[] inputStreamToByteArray(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int len = 0;
            byte[] buffer = new byte[1024 * 50];
            while ((len = inputStream.read(buffer, 0, buffer.length)) != -1) {
                byteArrayOutputStream.write(buffer, 0, len);
            }
            byteArrayOutputStream.flush();
            byte[] data = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return data;
        }
        return null;
    }

}

