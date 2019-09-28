package com.example.camerax;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.graphics.Matrix;

import java.io.File;

public class MainActivity extends AppCompatActivity {


    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textureView = findViewById(R.id.view_finder);

        if(allPermissionsGranted()){
            startCamera(); //start camera if permission has been granted by user
        } else{
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }


    }

    /*
    @name: startCamera

    @desc:
     */
    private void startCamera() {

        //unbinds all u se cases from the lifecycle and removes them from the CameraX
        CameraX.unbindAll();

        PreviewConfig pConfig = new PreviewConfig.Builder().build();
        Preview preview = new Preview(pConfig);

        preview.setOnPreviewOutputUpdateListener(
                new Preview.OnPreviewOutputUpdateListener() {
                    //to update the surface texture we  have to destroy it first then re-add it
                    @Override
                    public void onUpdated(Preview.PreviewOutput output){
                        ViewGroup parent = (ViewGroup) textureView.getParent();
                        parent.removeView(textureView);
                        parent.addView(textureView, 0);

                        textureView.setSurfaceTexture(output.getSurfaceTexture());
                    }
                });
        /*
        ImageCapture use case.

        @doc: https://developer.android.com/training/camerax/take-photo
         */

        //Getting default rotation from the device.
        int rotation = getWindowManager().getDefaultDisplay().getRotation();

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
                .setTargetRotation(rotation).build();

        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);

//        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                /*
//                READ from external storage
//                WRITE to external storage
//                 */
//                File file = new File(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + System.currentTimeMillis() + ".jpeg");
//                Log.i("MainActivity",file.getAbsolutePath());
//
//            }
//        });

        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ///storage/emulated/0/1569710188867.png
                //content://media/external/images/media/1569710240495.jpeg

                String filePath = Environment.getExternalStorageDirectory()
                        + "/"
                        + System.currentTimeMillis()
                        + ".jpeg";

                File file = new File(filePath);
                Log.i("MainActivity", file.getAbsolutePath());

                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
                    @Override
                    public void onImageSaved(File file) {
                        String msg = "Pic captured at " + file.getAbsolutePath();
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(ImageCapture.UseCaseError useCaseError,String message, Throwable cause) {
                        String msg = "Pic capture failed : " + message;
                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
                        if(cause != null){
                            cause.printStackTrace();
                        }
                    }
                });
            }
        });

        /*
        Turns the camera on and off by binding the preview use case to the activity Lifecycle.
        When the activity starts, the preview is going to start, and the camera starts the stream.
        Vice Versa.

        using both use cases of preview and image capture.
         */
        //bind to lifecycle:
        CameraX.bindToLifecycle(this, preview, imgCap);
    }

    /*
    FLASH CODE
    <com.google.android.material.floatingactionbutton.FloatingActionButton
            android:id="@+id/fab_flash"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            app:fabSize="normal"
            android:src="@drawable/ic_flash"
            android:layout_alignParentBottom="true"
            android:layout_margin="32dp"
            app:backgroundTint="@android:color/white"/>

    fab_flash.setOnClickListener {
            val flashMode = imageCapture?.flashMode
            if(flashMode == FlashMode.ON) imageCapture?.flashMode = FlashMode.OFF
            else imageCapture?.flashMode = FlashMode.ON
        }
     */

    /*
    @name: allPermissionsGranted()
    @desc: true or false to check if AndroidManifest gave camera permission.

    @doc: https://developer.android.com/training/permissions/requesting
     */
    private boolean allPermissionsGranted(){

        for(String permission : REQUIRED_PERMISSIONS) {
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }
    /*
    @name: isExternalStorageWritable
    @desc: Checks if external storage is available to read and write.
     */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)){
            return true;
        }
        return false;
    }

    /*
    @name: isExternalStorageReadable
    @desc: Checks if external storage is available to read.
     */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
            Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
    /*
    @name: isExternalStorageWritable
    @desc: Checks if external storage is available to read and write.
     */
//    private File createImageFile() {
//
//    }

}
