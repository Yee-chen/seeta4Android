package cn.edu.zstu.facedetection.Detection;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.YuvImage;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Chenlei on 2017/04/28.
 */
public class FaceUtil {
	public final static String TAG = "FaceUtil";
	private static String SAVE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/FaceDetection/";
	private static File folder;

	/**
	 * 在指定画布上将人脸框出来
	 *
	 * @param canvas 给定的画布
	 * @param face 需要绘制的人脸信息
	 */
	static public void drawRect(Canvas canvas, Rect face) {

		if(canvas == null) {
			return;
		}

		Paint paint = new Paint();
		paint.setStyle(Style.STROKE);
		paint.setColor(Color.rgb(255, 203, 15));
		paint.setStrokeWidth(2);
//		canvas.drawOval(new RectF(face.bound.left, face.bound.top, face.bound.right, face.bound.bottom), paint);
		canvas.drawRect(new RectF(face.left, face.top, face.right, face.bottom), paint);
	}

	/**
	 * 获取保存路径
	 * @return
	 */
	public static String getFacePath(){
		String path;
		folder = new File(SAVE_PATH);
		if (!folder.exists()) {
			folder = new File(SAVE_PATH);
		}
		folder = new File(SAVE_PATH + "Face/");
		if (!folder.exists()) {
			if (!folder.mkdirs()) {
				folder = new File(SAVE_PATH + "Face/");
			}
		}

		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		Date curDate = new Date(System.currentTimeMillis());
		String timeStr = formatter.format(curDate);
		path = SAVE_PATH + "Face/" + ("FACE" + timeStr + ".jpg");
		return path;
	}

	/**
	 * 获取保存路径
	 * @return
	 */
	public static String getImagePath() {
		folder = new File(SAVE_PATH);
		if (!folder.exists()) {
			if (!folder.mkdirs()) {
				folder = new File(SAVE_PATH);
			}
		}
		folder = new File(SAVE_PATH + "Image/");
		if (!folder.exists()) {
			if (!folder.mkdirs()) {
				folder = new File(SAVE_PATH + "Image/");
			}
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
		Date curDate = new Date(System.currentTimeMillis());
		String timeStr = formatter.format(curDate);
		return SAVE_PATH + "Image/" + "IMAGE" + timeStr + ".jpg";
	}

	/**
	 * 保存Bitmap至本地
	 * @param bmp
	 */
	public static void saveBitmapToFile(Bitmap bmp){
		String file_path = getFacePath();
		File file = new File(file_path);
		FileOutputStream fOut;
		try {
			fOut = new FileOutputStream(file);
			bmp.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
			fOut.flush();
			fOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 保存byte[]至本地
	 * @param src  image byte[]
	 * @param w    width
	 * @param h    height
	 */
	public static void saveByteToFile(byte[] src, int w, int h) {
		String path = getImagePath();
		File file = new File(path);
		FileOutputStream fOut;
		YuvImage image = new YuvImage(src, ImageFormat.NV21, w, h, null);
		if (image != null) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			image.compressToJpeg(new Rect(0, 0, w, h), 80, stream);
			Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
			try {
				fOut = new FileOutputStream(file);
				bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
				fOut.flush();
				fOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
