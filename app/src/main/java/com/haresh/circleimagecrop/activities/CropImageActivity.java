package com.haresh.circleimagecrop.activities;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.circleimagecrop.cropoverlay.edge.Edge;
import com.circleimagecrop.cropoverlay.utils.ImageViewUtil;
import com.circleimagecrop.photoview.PhotoView;
import com.circleimagecrop.photoview.PhotoViewAttacher;
import com.haresh.circleimagecrop.R;
import com.haresh.circleimagecrop.utilities.AppUtility;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;

public class CropImageActivity extends AppCompatActivity {

    private PhotoView photoView;
    private AppCompatButton buttonCancel;
    private AppCompatButton buttonDone;

    private final int IMAGE_MAX_SIZE = 640;
    private float minScale = 1f;
    private final Bitmap.CompressFormat mOutputFormat = Bitmap.CompressFormat.JPEG;

    private ContentResolver mContentResolver;

    private Uri mSaveUri;
    private Uri mImageUri;
    private String mImagePath;
    private File mFileTemp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        photoView = (PhotoView) findViewById(R.id.photoView);
        buttonCancel = (AppCompatButton) findViewById(R.id.button_cancel);
        buttonDone = (AppCompatButton) findViewById(R.id.button_done);

        initialization();
        setListeners();
    }


    public void initialization() {

        photoView.addListener(new PhotoViewAttacher.IGetImageBounds() {
            @Override
            public Rect getImageBounds() {
                return new Rect((int) Edge.LEFT.getCoordinate(), (int) Edge.TOP.getCoordinate(), (int) Edge.RIGHT.getCoordinate(), (int) Edge.BOTTOM.getCoordinate());
            }
        });

        mContentResolver = getContentResolver();
        createTempFile();
        mImagePath = mFileTemp.getAbsolutePath();
        mSaveUri = Uri.fromFile(mFileTemp);
        mImageUri = Uri.fromFile(new File(getIntent().getStringExtra("IMAGE_PATH")));
        init();

    }


    public void setListeners() {
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("IMAGE_PATH", getIntent().getStringExtra("IMAGE_PATH"));
                setResult(RESULT_CANCELED, intent);
                finish();
            }
        });

        buttonDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUploadCroppedImage();
            }
        });
    }


    /**
     * Create temp file.
     */
    public void createTempFile() {
        mFileTemp = new File(AppUtility.getAppStoragePath(this, "Temp"), "Temp_Crop_" + Calendar.getInstance().getTimeInMillis() + ".jpg");
    }


    /**
     * Init.
     */
    public void init() {
        System.gc();
        Bitmap b = getBitmap(mImageUri);
        Drawable bitmap = new BitmapDrawable(getResources(), b);
        int h = bitmap.getIntrinsicHeight();
        int w = bitmap.getIntrinsicWidth();
        final float cropWindowWidth = Edge.getWidth();
        final float cropWindowHeight = Edge.getHeight();
        if (h <= w) {
            //Set the image view height to
            //HACK : Have to add 1f.
            minScale = (cropWindowHeight + 1f) / h;
        } else if (w < h) {
            //HACK : Have to add 1f.
            minScale = (cropWindowWidth + 1f) / w;
        }

        photoView.setMaximumScale(minScale * 3);
        photoView.setMediumScale(minScale * 2);
        photoView.setMinimumScale(minScale);
        photoView.setImageDrawable(bitmap);
        photoView.setScale(minScale);
    }

    /**
     * Save upload cropped image.
     */
    public void saveUploadCroppedImage() {
        boolean saved = saveOutput();
        if (saved) {
            Intent intent = new Intent();
            intent.putExtra("IMAGE_PATH", mImagePath);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(this, R.string.unable_to_crop, Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Gets bitmap.
     *
     * @param uri the uri
     * @return the bitmap
     */
    public Bitmap getBitmap(Uri uri) {
        InputStream in = null;
        Bitmap returnedBitmap = null;
        try {
            in = mContentResolver.openInputStream(uri);
            //Decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int scale = 1;
            if (o.outHeight > IMAGE_MAX_SIZE || o.outWidth > IMAGE_MAX_SIZE) {
                scale = (int) Math.pow(2, (int) Math.round(Math.log(IMAGE_MAX_SIZE / (double) Math.max(o.outHeight, o.outWidth)) / Math.log(0.5)));
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            in = mContentResolver.openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, o2);
            in.close();

            //First check
            ExifInterface ei = new ExifInterface(uri.getPath());
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    returnedBitmap = rotateImage(bitmap, 90);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    returnedBitmap = rotateImage(bitmap, 180);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    returnedBitmap = rotateImage(bitmap, 270);
                    //Free up the memory
                    bitmap.recycle();
                    bitmap = null;
                    break;
                default:
                    returnedBitmap = bitmap;
            }
            return returnedBitmap;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Gets current displayed image.
     *
     * @return the current displayed image
     */
    public Bitmap getCurrentDisplayedImage() {
        System.gc();
        Bitmap result = Bitmap.createBitmap(photoView.getWidth(), photoView.getHeight(), Bitmap.Config.RGB_565);
        Canvas c = new Canvas(result);
        photoView.draw(c);
        return result;
    }


    /**
     * Gets cropped image.
     *
     * @return the cropped image
     */
    public Bitmap getCroppedImage() {

        try {
            Bitmap mCurrentDisplayedBitmap = getCurrentDisplayedImage();
            Rect displayedImageRect = ImageViewUtil.getBitmapRectCenterInside(mCurrentDisplayedBitmap, photoView);
            // Get the scale factor between the actual Bitmap dimensions and the
            // displayed dimensions for width.
            float actualImageWidth = mCurrentDisplayedBitmap.getWidth();
            float displayedImageWidth = displayedImageRect.width();
            float scaleFactorWidth = actualImageWidth / displayedImageWidth;

            // Get the scale factor between the actual Bitmap dimensions and the
            // displayed dimensions for height.
            float actualImageHeight = mCurrentDisplayedBitmap.getHeight();
            float displayedImageHeight = displayedImageRect.height();
            float scaleFactorHeight = actualImageHeight / displayedImageHeight;

            // Get crop window position relative to the displayed image.
            float cropWindowX = Edge.LEFT.getCoordinate() - displayedImageRect.left;
            float cropWindowY = Edge.TOP.getCoordinate() - displayedImageRect.top;
            float cropWindowWidth = Edge.getWidth();
            float cropWindowHeight = Edge.getHeight();

            // Scale the crop window position to the actual size of the Bitmap.
            float actualCropX = cropWindowX * scaleFactorWidth;
            float actualCropY = cropWindowY * scaleFactorHeight;
            float actualCropWidth = cropWindowWidth * scaleFactorWidth;
            float actualCropHeight = cropWindowHeight * scaleFactorHeight;

            // Crop the subset from the original Bitmap.
            System.gc();
            Bitmap croppedBitmap = Bitmap.createBitmap(mCurrentDisplayedBitmap, (int) actualCropX, (int) actualCropY, (int) actualCropWidth, (int) actualCropHeight);
            return croppedBitmap;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Save output boolean.
     *
     * @return the boolean
     */
    public boolean saveOutput() {
        Bitmap croppedImage = getCroppedImage();
        if (croppedImage != null) {
            if (mSaveUri != null) {
                OutputStream outputStream = null;
                try {
                    outputStream = mContentResolver.openOutputStream(mSaveUri);
                    if (outputStream != null) {
                        croppedImage.compress(mOutputFormat, 75, outputStream);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return false;
                } finally {
                    closeSilently(outputStream);
                }
            } else {
                return false;
            }
            croppedImage.recycle();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Close silently.
     *
     * @param c the c
     */
    public void closeSilently(Closeable c) {
        if (c == null) return;
        try {
            c.close();
        } catch (Throwable t) {
            buttonDone.performClick();
        }
    }


    /**
     * Rotate image bitmap.
     *
     * @param source the source
     * @param angle  the angle
     * @return the bitmap
     */
    public Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        System.gc();
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

}
