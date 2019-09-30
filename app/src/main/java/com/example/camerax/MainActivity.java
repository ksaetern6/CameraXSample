package com.example.camerax;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageAnalysisConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;


import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
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
import android.widget.ImageButton;
import android.widget.Toast;
import android.graphics.Matrix;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.vision.Frame;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {


    private int REQUEST_CODE_PERMISSIONS = 101;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    TextureView textureView;

//    CameraView cameraView;
//    ImageButton imgCapture = findViewById(R.id.imgCapture);

    //==============================================================================================
    // [FIREBASE]
    FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(
                    FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build();
    FirebaseVisionBarcodeDetector detector;
    //==============================================================================================
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
//==================================================================================================
        ImageAnalysisConfig config =
                new ImageAnalysisConfig.Builder()
//                        .setTargetResolution(new Size(1280, 720))
//                        .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
                        .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis(config);

        imageAnalysis.setAnalyzer(
                new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(ImageProxy imageProxy, int degrees) {
                        if (imageProxy == null || imageProxy.getImage() == null) {
                            return;
                        }
                        Image mediaImage = imageProxy.getImage();
                        int rotation = degreesToFirebaseRotation(degrees);
                        FirebaseVisionImage image =
                                FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

                        detector = FirebaseVision.getInstance()
                                .getVisionBarcodeDetector(options);

                        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
                                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                                    @Override
                                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                                        processResult(firebaseVisionBarcodes);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                });
//==================================================================================================
        /*
        ImageCapture use case.

        @doc: https://developer.android.com/training/camerax/take-photo
         */

//        //Getting default rotation from the device.
//        int rotation = getWindowManager().getDefaultDisplay().getRotation();
//
//        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder()
//                .setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
//                .setTargetRotation(rotation).build();
//
//        final ImageCapture imgCap = new ImageCapture(imageCaptureConfig);
//
////        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////                /*
////                READ from external storage
////                WRITE to external storage
////                 */
////                File file = new File(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + System.currentTimeMillis() + ".jpeg");
////                Log.i("MainActivity",file.getAbsolutePath());
////
////            }
////        });
//
//        findViewById(R.id.imgCapture).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                ///storage/emulated/0/1569710188867.png
//                //content://media/external/images/media/1569710240495.jpeg
//
//                String filePath = Environment.getExternalStorageDirectory()
//                        + "/"
//                        + System.currentTimeMillis()
//                        + ".jpeg";
//
//                File file = new File(filePath);
//                Log.i("MainActivity", file.getAbsolutePath());
//
//                imgCap.takePicture(file, new ImageCapture.OnImageSavedListener() {
//                    @Override
//                    public void onImageSaved(File file) {
//                        String msg = "Pic captured at " + file.getAbsolutePath();
//                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
//                        FirebaseVisionImage image = captureFirebaseImage(getApplicationContext(),file);
//
//                        detector = FirebaseVision.getInstance()
//                                .getVisionBarcodeDetector(options);
//
//                        Task<List<FirebaseVisionBarcode>> result = detector.detectInImage(image)
//                                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
//                                    @Override
//                                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
//                                        processResult(firebaseVisionBarcodes);
//                                    }
//                                })
//                                .addOnFailureListener(new OnFailureListener() {
//                                    @Override
//                                    public void onFailure(@NonNull Exception e) {
//                                        Toast.makeText(MainActivity.this, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                        Log.d("Main", "END");
//                    }
//
//                    @Override
//                    public void onError(ImageCapture.UseCaseError useCaseError,String message, Throwable cause) {
//                        String msg = "Pic capture failed : " + message;
//                        Toast.makeText(getBaseContext(), msg,Toast.LENGTH_LONG).show();
//                        if(cause != null){
//                            cause.printStackTrace();
//                        }
//                    }
//                });
//            }
//        });

        /*
        Turns the camera on and off by binding the preview use case to the activity Lifecycle.
        When the activity starts, the preview is going to start, and the camera starts the stream.
        Vice Versa.

        using both use cases of preview and image capture.
         */
        //bind to lifecycle:
        CameraX.bindToLifecycle(this, preview, imageAnalysis); //imgCap


    }

    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        //Log.d("Main", "processResult working");
        for(FirebaseVisionBarcode barcode: firebaseVisionBarcodes) {
//            Rect bounds = barcode.getBoundingBox();
//            Point[] corners = barcode.getCornerPoints();
//
//            String rawValue = barcode.getRawValue();
            int valueType = barcode.getValueType();

            switch (valueType) {
                case FirebaseVisionBarcode.TYPE_TEXT:
                {
                    String msg = barcode.getRawValue();

                    Log.d("Main", "search here " + msg);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(msg)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    break;
                }
            }

        }
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

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }


}
