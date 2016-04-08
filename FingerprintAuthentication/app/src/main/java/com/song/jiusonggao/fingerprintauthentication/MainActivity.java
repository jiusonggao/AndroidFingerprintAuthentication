package com.song.jiusonggao.fingerprintauthentication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "MainActivity";
    private static int RESULT_LOAD_IMAGE = 100;
    private Button mFingerprintBtn;
    private ImageView mOriginalFingerprint;
    private String mImagePath;
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
        // Original Fingerprint image
        mOriginalFingerprint = (ImageView) findViewById(R.id.original_image);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_load_fp:
                loadFingerprintFromGallery();
                break;
            default:
                break;
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
                    //
                }
                break;
            default:
                break;
        }

    }

    private void requestReadExternalStoragePermission() {
        Log.i(TAG, "requestReadExternalStoragePermission");
        String[] perms = {"android.permission.READ_EXTERNAL_STORAGE"};
        int permsRequestCode = 200;
        requestPermissions(perms, permsRequestCode);
    }
}
