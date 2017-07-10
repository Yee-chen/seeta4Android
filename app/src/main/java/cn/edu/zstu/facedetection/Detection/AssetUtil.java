package cn.edu.zstu.facedetection.Detection;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Chenlei on 2017/4/8.
 */
public class AssetUtil {

    private static final String TAG = "AssetUtil";
    private static String mFileName = "seeta_frontal.bin";

    public static void copyAssetToCache(Context context) {
        File cacheFile = new File(context.getFilesDir().getPath() + "/" + mFileName);
        if (!cacheFile.exists()) {
            int count, buffer_len = 2048;
            byte[] data = new byte[buffer_len];
            AssetManager assetManager = context.getAssets();
            try {
                InputStream srcIS = assetManager.open(mFileName);

                // Copy the file from the assets subsystem to the filesystem
                FileOutputStream destOS = new FileOutputStream(cacheFile);
                // Copy the file content in bytes
                while ((count = srcIS.read(data, 0, buffer_len)) != -1) {
                    destOS.write(data, 0, count);
                }
                // Close the two files
                srcIS.close();
                destOS.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, String.valueOf(e));
            }
        }
    }
}
