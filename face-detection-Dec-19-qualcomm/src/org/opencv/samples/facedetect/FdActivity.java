package org.opencv.samples.facedetect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.*;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.objdetect.CascadeClassifier;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;


import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;


//public class FdActivity extends Activity implements CvCameraViewListener {
public class FdActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnTouchListener {


    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier faceCascadeClassifier;
    private CascadeClassifier eyeCascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;


    private static final String TAG = "OCVSample::Activity";

    private mView mOpenCvCameraView;

    private List<android.hardware.Camera.Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    initializeOpenCVDependencies();
//                    mOpenCvCameraView.enableView();
//                    mOpenCvCameraView.setOnTouchListener(FdActivity.this);

                    break;
                }

                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies() {

        try {
            // Copy the feature library files here into a temp file so OpenCV can load it
            // download link:
            InputStream is_face = getResources().openRawResource(R.raw.lbpcascade_frontalface);
//            InputStream is_face = getResources().openRawResource(R.raw.haarcascade_frontalface_alt);
//            InputStream is_face = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);
            InputStream is_eye = getResources().openRawResource(R.raw.haarcascade_eye_tree_eyeglasses);

            File CascadeDir = getDir("cascade", Context.MODE_PRIVATE);

//  load feature library here
//            faceCascadeClassifier = new CascadeClassifier(faceCascadeFile.getAbsolutePath());
            File faceCascadeFile = new File(CascadeDir, "lbpcascade_frontalface.xml");
//            File faceCascadeFile = new File(CascadeDir, "haarcascade_frontalface_alt.xml");

//            File faceCascadeFile = new File(CascadeDir, "haarcascade_eye_tree_eyeglasses.xml");
            File eyeCascadeFile = new File(CascadeDir, "haarcascade_eye_tree_eyeglasses.xml");

            FileOutputStream os_face = new FileOutputStream(faceCascadeFile);
            FileOutputStream os_eye = new FileOutputStream(eyeCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is_face.read(buffer)) != -1) {
                os_face.write(buffer, 0, bytesRead);
            }

            while ((bytesRead = is_eye.read(buffer)) != -1) {
                os_eye.write(buffer, 0, bytesRead);
            }

            is_face.close();
            os_face.close();
            is_eye.close();
            os_eye.close();

            // Load the cascade classifier
            faceCascadeClassifier = new CascadeClassifier(faceCascadeFile.getAbsolutePath());
            eyeCascadeClassifier = new CascadeClassifier(eyeCascadeFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // And we are ready to go
        openCvCameraView.enableView();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

//        openCvCameraView = new JavaCameraView(this, -1);
//        setContentView(openCvCameraView);

//        setContentView(R.layout.face_detect_surface_view);

//        mOpenCvCameraView = (mView) findViewById(R.id.fd_activity_surface_view);

//        openCvCameraView.setCameraIndex(openCvCameraView.CAMERA_ID_FRONT);

//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//
//        mOpenCvCameraView.setCvCameraViewListener(this);

//      no resolution changing
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        openCvCameraView = new JavaCameraView(this, -1);
        setContentView(openCvCameraView);
        openCvCameraView.setCameraIndex(openCvCameraView.CAMERA_ID_FRONT);
        openCvCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);

        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
    }


    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        Mat aInputFrame = inputFrame.rgba();

        // samsung phone
//        Core.flip(aInputFrame, aInputFrame, 1);
//        aInputFrame = aInputFrame.t();

        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGBA2RGB);

        MatOfRect faces = new MatOfRect();
        MatOfRect eyes = new MatOfRect();

        // conflict with the android.hardware.Camera.Size  so need the full path here
        // Use the classifier to detect faces
        if (faceCascadeClassifier != null) {
            faceCascadeClassifier.detectMultiScale(grayscaleImage, faces, 1.1, 2, 2,
                    new org.opencv.core.Size(absoluteFaceSize, absoluteFaceSize), new org.opencv.core.Size());
        }

        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();

//        Core.putText(aInputFrame, "#faces: " + Integer.toString( facesArray.length ),
//                new Point(100,100),1,5,new Scalar(255, 0, 0, 255),4);

        for (int i = 0; i < facesArray.length; i++) {
            //draw rectangle for faces
            Core.rectangle(aInputFrame, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0, 255), 3);

            Mat face_area = new Mat(aInputFrame, facesArray[i]);

            // find face first, then detect the eyes within face area
            if (eyeCascadeClassifier != null) {
                eyeCascadeClassifier.detectMultiScale(face_area, eyes, 1.1, 2, 2,
                        new org.opencv.core.Size(absoluteFaceSize, absoluteFaceSize), new org.opencv.core.Size());
            }

            Rect[] eyesArray = eyes.toArray();

//            Core.putText(aInputFrame, "#eyes: " + Integer.toString( eyesArray.length ),
//                    new Point(100,300),1,5,new Scalar(255, 0, 0, 255),4);
            for (int j = 0; j < eyesArray.length; j++) {
                Core.rectangle(aInputFrame,
                        new Point(eyesArray[j].tl().x + facesArray[i].tl().x,
                                eyesArray[j].tl().y + facesArray[i].tl().y),

                        new Point(eyesArray[j].br().x + facesArray[i].tl().x,
                                eyesArray[j].br().y + facesArray[i].tl().y),
                        new Scalar(0, 255, 255, 255), 3);
            }
        }

        //samsung phone
//        aInputFrame = aInputFrame.t();


        int original_width = aInputFrame.width();  //800
        int original_height = aInputFrame.height();  //480
        Mat new_cavas = Mat.zeros(aInputFrame.size(),aInputFrame.type());

//        org.opencv.core.Size sz = new org.opencv.core.Size(100,100);
        org.opencv.core.Size sz = new org.opencv.core.Size(original_height,original_height*original_height/original_width);
//
        Imgproc.resize( aInputFrame, aInputFrame, sz );

//        Core.flip(aInputFrame, aInputFrame, 1);
        aInputFrame = aInputFrame.t();

        aInputFrame.copyTo(new Mat( new_cavas,new Rect(100,0,aInputFrame.cols(),aInputFrame.rows()) ));

        return new_cavas;
    }

//    public boolean onCreateOptionsMenu(Menu menu) {
//        List<String> effects = mOpenCvCameraView.getEffectList();
//
//        if (effects == null) {
//            Log.e(TAG, "Color effects are not supported by device!");
//            return true;
//        }
//
//        mColorEffectsMenu = menu.addSubMenu("Color Effect");
//        mEffectMenuItems = new MenuItem[effects.size()];
//
//        int idx = 0;
//        ListIterator<String> effectItr = effects.listIterator();
//        while(effectItr.hasNext()) {
//            String element = effectItr.next();
//            mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
//            idx++;
//        }
//
//        mResolutionMenu = menu.addSubMenu("Resolution");
//        mResolutionList = mOpenCvCameraView.getResolutionList();
//        mResolutionMenuItems = new MenuItem[mResolutionList.size()];
//
//        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
//        idx = 0;
//        while(resolutionItr.hasNext()) {
//            android.hardware.Camera.Size element = resolutionItr.next();
//            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
//                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
//            idx++;
//        }
//
//        return true;
//    }
//
//
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
//        if (item.getGroupId() == 1) {
//            mOpenCvCameraView.setEffect((String) item.getTitle());
//            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
//        } else if (item.getGroupId() == 2) {
//            int id = item.getItemId();
//            android.hardware.Camera.Size resolution = mResolutionList.get(id);
//            mOpenCvCameraView.setResolution(resolution);
//            resolution = mOpenCvCameraView.getResolution();
//            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
//            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
//        }
//
//        return true;
//    }

    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

//    public boolean onTouch(View v, MotionEvent event) {
//        Log.i(TAG, "onTouch event");
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
//        String currentDateandTime = sdf.format(new Date());
//        String fileName = Environment.getExternalStorageDirectory().getPath() +
//                "/sample_picture_" + currentDateandTime + ".jpg";
//        mOpenCvCameraView.takePicture(fileName);
//        Toast.makeText(this, fileName + " saved", Toast.LENGTH_SHORT).show();
//        return false;
//    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }
}