package cityu.jasontang.comicviewer;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final int OPEN_REQUEST_CODE = 2;
//    private static final int READ_REQUEST_CODE = 3;
//    private TextView textView;
//    private Uri selectedPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Check permission for Android 6.0+ */
        if (Build.VERSION.SDK_INT >= 23) {
            if (!checkPermission()) {
                requestPermissions(); // Code for permission
            }
        }
    }

    /* Check for Write Permission */
    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return (result == PackageManager.PERMISSION_GRANTED);
    }

    /* Request Write Permission if not permitted */
    private void requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Need Permission to read images.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    /* Check permission request result */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Result", "Permission Granted.");
                } else {
                    Log.e("Result", "Permission Denied.");
                }
                break;
        }
    }

    /* Button for file browser */
    public void openFile(View view)
    {
        /* Start the SAF file browser */
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, OPEN_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {

        /* On selecting a folder from openFile() */
        if (requestCode == OPEN_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (resultData != null) {
                    //Retrieve the folder Uri
                    Uri currentUri = resultData.getData();
                    Intent intent = new Intent(this, ReaderActivity.class);

                    //Pass Uri to ReaderActivity
                    intent.putExtra("selectedPath", currentUri);
                    startActivity(intent);
                }
            }
        }
    }
}
