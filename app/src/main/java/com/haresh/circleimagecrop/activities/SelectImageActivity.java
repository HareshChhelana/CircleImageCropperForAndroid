package com.haresh.circleimagecrop.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.haresh.circleimagecrop.R;
import com.haresh.circleimagecrop.listeners.ImageListener;
import com.haresh.circleimagecrop.utilities.AppUtility;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jp.wasabeef.glide.transformations.CropCircleTransformation;


public class SelectImageActivity extends AppCompatActivity {

    private AppCompatImageView imageViewSelectedImage;
    private AppCompatButton buttonChooseFromGallery;
    private AppCompatButton buttonTakeFromCamera;

    private final int OPEN_CAMERA_FOR_PHOTO = 201;
    private final int OPEN_GALLERY_FOR_PHOTO = 202;
    private final int OPEN_FOR_CROP = 203;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 100;
    private Uri imgPathUri;
    private int selectedOption = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_image);


        imageViewSelectedImage = (AppCompatImageView) findViewById(R.id.image_view_selected);
        buttonChooseFromGallery = (AppCompatButton) findViewById(R.id.button_gallery);
        buttonTakeFromCamera = (AppCompatButton) findViewById(R.id.button_camera);

        initialization();
        setListeners();
    }

    public void initialization() {


    }


    public void setListeners() {
        buttonChooseFromGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedOption = 1;
                if (checkAndRequestPermissions()) {
                    Intent galleryIntent = new Intent();
                    galleryIntent.setType("image/*");
                    galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                    galleryIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    startActivityForResult(Intent.createChooser(galleryIntent, getString(R.string.select_picture)), OPEN_GALLERY_FOR_PHOTO);
                }
            }
        });

        buttonTakeFromCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedOption = 2;
                if (checkAndRequestPermissions()) {
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    cameraIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, setUriForPhoto());
                    startActivityForResult(cameraIntent, OPEN_CAMERA_FOR_PHOTO);
                }
            }
        });
    }

    public Uri setUriForPhoto() {
        File pictureFile = new File(AppUtility.getAppStoragePath(this, "") + "/" + Calendar.getInstance().getTimeInMillis() + ".jpg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            imgPathUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", pictureFile);
        } else {
            imgPathUri = Uri.fromFile(pictureFile);
        }
        return imgPathUri;
    }

    public Uri getImagePathUri() {
        return imgPathUri;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            if (resultCode == RESULT_OK) {
                selectedOption = -1;
                if (requestCode == OPEN_CAMERA_FOR_PHOTO) {
                    AppUtility.resizeImage(SelectImageActivity.this, getImagePathUri(), true, new ImageListener() {
                        @Override
                        public void onImageResize(String path, String uri) {
                            if (path != null && path.trim().length() > 0 && uri != null && uri.trim().length() > 0) {
                                Intent cropIntent = new Intent(SelectImageActivity.this, CropImageActivity.class);
                                cropIntent.putExtra("IMAGE_PATH", path);
                                startActivityForResult(cropIntent, OPEN_FOR_CROP);
                            } else {
                                Toast.makeText(SelectImageActivity.this, getString(R.string.unable_to_get_image), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else if (requestCode == OPEN_GALLERY_FOR_PHOTO) {
                    AppUtility.resizeImage(SelectImageActivity.this, data.getData(), true, new ImageListener() {
                        @Override
                        public void onImageResize(String path, String uri) {
                            if (path != null && path.trim().length() > 0 && uri != null && uri.trim().length() > 0) {
                                Intent cropIntent = new Intent(SelectImageActivity.this, CropImageActivity.class);
                                cropIntent.putExtra("IMAGE_PATH", path);
                                startActivityForResult(cropIntent, OPEN_FOR_CROP);
                            } else {
                                Toast.makeText(SelectImageActivity.this, getString(R.string.unable_to_get_image), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else if (requestCode == OPEN_FOR_CROP) {
                    if (data.getStringExtra("IMAGE_PATH") != null && data.getStringExtra("IMAGE_PATH").trim().length() > 0) {
                        Glide.with(SelectImageActivity.this).load(data.getStringExtra("IMAGE_PATH")).override(350, 350).bitmapTransform(new CropCircleTransformation(this)).into(imageViewSelectedImage);
                    } else {
                        Toast.makeText(SelectImageActivity.this, getString(R.string.unable_to_get_image), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private boolean checkAndRequestPermissions() {
        int externalStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (externalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST_ID_MULTIPLE_PERMISSIONS) {
            if (selectedOption == 1) {
                buttonChooseFromGallery.performClick();
            } else {
                buttonTakeFromCamera.performClick();
            }
        }
    }


}
