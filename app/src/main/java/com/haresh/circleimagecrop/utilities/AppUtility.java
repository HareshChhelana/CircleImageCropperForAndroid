package com.haresh.circleimagecrop.utilities;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.haresh.circleimagecrop.R;
import com.haresh.circleimagecrop.listeners.ImageListener;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Calendar;


public class AppUtility {

    public static String getAppStoragePath(Context context, String dir) {
        String path;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            path = Environment.getExternalStorageDirectory() + "/" + context.getString(R.string.app_name);
        } else {
            path = context.getFilesDir() + "/" + context.getString(R.string.app_name);
        }

        String directory = +dir.trim().length() > 0 ? ("/" + dir) : "";
        File file = new File(path + directory);
        if (!file.exists()) file.mkdirs();

        return file.getAbsolutePath();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }

        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            String url;
            if (isGooglePhotosUri(uri)) {
                url = getDataColumnWithAuthority(context, uri, null, null);
                return getDataColumn(context, Uri.parse(url), null, null);
            }

            return getDataColumn(context, uri, null, null);

        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                if (cursor.getString(column_index) != null) {
                    return cursor.getString(column_index);
                } else {

                }
            }
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    /**
     * Function for fixing synced folder image picking bug
     **/
    public static String getDataColumnWithAuthority(Context context, Uri uri, String selection, String[] selectionArgs) {
        Bitmap bitmap = null;
        InputStream is = null;
        if (uri.getAuthority() != null) {
            try {
                is = context.getContentResolver().openInputStream(uri);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Bitmap bmp = BitmapFactory.decodeStream(is);
            return getImageUri(context, bmp).toString();
        }

        return null;
    }

    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return uri.getAuthority() != null;
    }


    public static void resizeImage(final Context context, final Uri uri, final boolean isNeedToScalDown, final ImageListener listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return getRightAngleImage(context, getPath(context, uri), isNeedToScalDown);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return "";
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (s != null && s.trim().length() > 0) {
                    listener.onImageResize(s, Uri.fromFile(new File(s)).toString());
                } else {
                    listener.onImageResize(null, null);
                }
            }
        }.execute();
    }

    public static void resizeImage(final Context context, final String path, final boolean isNeedToScalDown, final ImageListener listener) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... voids) {
                try {
                    return getRightAngleImage(context, path, isNeedToScalDown);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return "";
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (s != null && s.trim().length() > 0) {
                    listener.onImageResize(s, Uri.fromFile(new File(s)).toString());
                } else {
                    listener.onImageResize(null, null);
                }
            }
        }.execute();
    }

    public static String getRightAngleImage(Context context, String photoPath, boolean isNeedToScalDown) {

        try {
            ExifInterface ei = new ExifInterface(photoPath);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            int degree = 0;

            switch (orientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    degree = 0;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
                case ExifInterface.ORIENTATION_UNDEFINED:
                    degree = 0;
                    break;
                default:
                    degree = 90;
            }

            return rotateImage(context, degree, photoPath, isNeedToScalDown);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return photoPath;
    }


    private static String rotateImage(Context context, int degree, String path, boolean isNeedToScalDown) {
        String imagePath = "";
        try {
            Bitmap bitmap = getBitmap(path);
            if (bitmap != null) {
                if (degree > 0 && bitmap.getWidth() > bitmap.getHeight()) {
                    Matrix matrix = new Matrix();
                    matrix.setRotate(degree);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                }
                Bitmap scaled;
                if (isNeedToScalDown) {
                    scaled = scaleDown(bitmap, 800, true);
                } else {
                    scaled = scaleDown(bitmap, 1024, true);
                }
                imagePath = getAppStoragePath(context, "") + "/temp_" + Calendar.getInstance().getTimeInMillis() + ".jpg";
                FileOutputStream out = new FileOutputStream(new File(imagePath));
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out);
                out.flush();
                out.close();
                scaled.recycle();
                bitmap.recycle();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imagePath;
    }

    private static Bitmap getBitmap(String path) {
        File f = new File(path);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            return BitmapFactory.decodeStream(new FileInputStream(f), null, options);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Bitmap scaleDown(Bitmap realImage, float maxImageSize, boolean filter) {
        Bitmap newBitmap;
        if (realImage.getWidth() > maxImageSize || realImage.getHeight() > maxImageSize) {
            float ratio = Math.min(maxImageSize / realImage.getWidth(), maxImageSize / realImage.getHeight());
            int width = Math.round(ratio * realImage.getWidth());
            int height = Math.round(ratio * realImage.getHeight());
            newBitmap = Bitmap.createScaledBitmap(realImage, width, height, filter);
        } else {
            newBitmap = Bitmap.createScaledBitmap(realImage, realImage.getWidth(), realImage.getHeight(), filter);
        }
        return newBitmap;
    }
}
