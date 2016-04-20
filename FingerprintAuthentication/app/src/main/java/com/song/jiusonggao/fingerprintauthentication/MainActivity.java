package com.song.jiusonggao.fingerprintauthentication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private static int RESULT_LOAD_IMAGE = 100;
    private Button mFingerprintBtn;
    private Button mMinutiaeBtn;
    private TextView mProgressTextView;
    private ImageView mOriginalFingerprint;
    private ImageView mBinaryImage;
    private ImageView mSkeletonImage;
    private ImageView mDirectionImage;
    private ImageView mCoreImage;
    private ImageView mMinutiaeImage;
    private ProgressDialog mProgressDialog;

    private String mImagePath;
    private FingerPrint mFingerPrint;
    private FingerPrint.direction[][] dirMatrix;
    private Point core;
    private int coreRadius;
    private ArrayList<Point> intersections;
    private ArrayList<Point> endPoints;

    private MinutiaeExtractionTask minutiaeExtractionTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        Log.i(TAG, "initView");
        // Button for loading Fingerprints
        mFingerprintBtn = (Button) findViewById(R.id.btn_load_fp);
        mFingerprintBtn.setOnClickListener(this);
        // Button for extracting minutiae
        mMinutiaeBtn = (Button) findViewById(R.id.button_minutiae);
        mMinutiaeBtn.setOnClickListener(this);
        // Progress text view
        mProgressTextView = (TextView) findViewById(R.id.textView_progress);
        // Original Fingerprint image
        mOriginalFingerprint = (ImageView) findViewById(R.id.original_image);
        // Binary image
        mBinaryImage = (ImageView) findViewById(R.id.binary_image);
        // Skeleton image
        mSkeletonImage = (ImageView) findViewById(R.id.skeleton_image);
        // Direction image
        mDirectionImage = (ImageView) findViewById(R.id.direction_image);
        // Core image
        mCoreImage = (ImageView) findViewById(R.id.core_image);
        // Minutiae image
        mMinutiaeImage = (ImageView) findViewById(R.id.minutiae_image);
        // Progress dialog
        mProgressDialog = new ProgressDialog(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_load_fp:
                loadFingerprintFromGallery();
                break;
            case R.id.button_minutiae:
                mProgressDialog.setTitle("Loading");
                mProgressDialog.setMessage("Start minutiae extraction...");
                mProgressDialog.show();
                extractMinutiae();
                break;
            default:
                break;
        }
    }

    private void extractMinutiae(){
        if(mImagePath == null) {
            return;
        } else {
            if(minutiaeExtractionTask != null) {
                minutiaeExtractionTask.cancel(true);
                minutiaeExtractionTask = null;
            }
            minutiaeExtractionTask = new MinutiaeExtractionTask();
            minutiaeExtractionTask.execute(mImagePath);
        }
    }

    private void loadFingerprintFromGallery() {
        Log.i(TAG, "loadFingerprintFromGallery");
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult");
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            mImagePath = cursor.getString(columnIndex);
            cursor.close();
            if(Utilily.shouldAskPermission()) {
             requestReadExternalStoragePermission();
            } else {
                mOriginalFingerprint.setImageBitmap(BitmapFactory.decodeFile(mImagePath));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        Log.i(TAG, "onRequestPermissionsResult");
        super.onRequestPermissionsResult(permsRequestCode, permissions, grantResults);
        switch(permsRequestCode){
            case 200:
                boolean readAcceptted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if(readAcceptted){
                    mOriginalFingerprint.setImageBitmap(BitmapFactory.decodeFile(mImagePath));
                } else {
                    // Permission request denied, nothing to do.
                }
                break;
            default:
                break;
        }
    }

    /**
     * Request read external storage permission for app to load images from gallery.
     */
    private void requestReadExternalStoragePermission() {
        String[] perms = {"android.permission.READ_EXTERNAL_STORAGE"};
        int permsRequestCode = 200;
        requestPermissions(perms, permsRequestCode);
    }

    /**
     * A task to extract minutiaes from a fingerprint image.
     */
    private class MinutiaeExtractionTask extends AsyncTask<String, String, Boolean> {

        @Override
        protected Boolean doInBackground(String... params) {
            String path = params[0];
            mFingerPrint = new FingerPrint(path);
            // Binary local
            publishProgress("Compute Binary");
            mFingerPrint.setColors(Color.BLACK, Color.GREEN);
            mFingerPrint.binarizeLocalMean();
            // Remove noise
            publishProgress("Remove noise");
            mFingerPrint.addBorders(1);
            mFingerPrint.removeNoise();
            mFingerPrint.removeNoise();
            mFingerPrint.removeNoise();
            // Skeletonization
            publishProgress("Skeletonization");
            mFingerPrint.skeletonize();
            // Direction
            publishProgress("Calculate direction");
            dirMatrix = mFingerPrint.getDirections();
            // Core
            publishProgress("Calculate core...");
            core = mFingerPrint.getCore(dirMatrix);
            // Minutiae
            publishProgress("Extract minutiae");
            intersections = mFingerPrint.getMinutiaeIntersections(core, coreRadius);
            endPoints = mFingerPrint.getMinutiaeEndpoints(core, coreRadius);
            return true;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            mProgressDialog.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            mProgressDialog.dismiss();
            if(result) {
                mProgressTextView.setText("success");
            } else {
                mProgressTextView.setText("fail");
            }
            // display results.
            Bitmap skeletonBitmap = mFingerPrint.toBitmap();
            mSkeletonImage.setImageBitmap(skeletonBitmap);
            mDirectionImage.setImageBitmap(mFingerPrint.directionToBufferedImage(dirMatrix));
        }
    }
}
