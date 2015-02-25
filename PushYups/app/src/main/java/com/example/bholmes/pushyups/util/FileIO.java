package com.example.bholmes.pushyups.util;

import android.os.Environment;
import android.util.Log;

import com.example.bholmes.pushyups.RepActivity;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by bholmes on 25/02/2015.
 */
public class FileIO {
    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static FileOutputStream getDataFile(String fileName) {
        // Get the directory for the user's public pictures directory.
        File pushYupDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(), "/PushYup");

        if (!pushYupDir.mkdirs()) {
            Log.e(RepActivity.LOG_TAG, "Failed to create PushYup directory");
        } else {
            try {
                File file = new File(pushYupDir, fileName);
                if (file.createNewFile()) {
                    return new FileOutputStream(file);
                } else {
                    Log.e(RepActivity.LOG_TAG, "Couldn't create new file " + fileName);
                }
            } catch (Exception e) {
                Log.e(RepActivity.LOG_TAG, "Couldn't open file output stream: " + e, e);
            }
        }
        return null;
    }

    public static void logToDataFile(FileOutputStream outputStream, String text) {
        try {
            outputStream.write(text.getBytes());
        } catch(Exception e) {
            // Log.e(RepActivity.LOG_TAG, "Error logging data: " + e, e);
        }
    }
}
