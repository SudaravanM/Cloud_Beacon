package com.example.firebasefileupload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.dynamiclinks.DynamicLink;
//import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
//import com.google.firebase.dynamiclinks.PendingDynamicLinkData;
//import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URLEncoder;

public class MainActivity extends Activity implements View.OnClickListener,
        DialogInterface.OnClickListener {

    static final int HANDLE_URL = 1;

    static final int HANDLE_ERROR = 2;

    private final Handler mHandler = new Handler(this);

    private String mUrl;

    private TextView mTextUrl;

    private TextView mTextOriginalUrl;

    private String mTinyUrl;

    Button selectFile, upload;
    TextView notification;
    Uri pdfUri; // uri are actually URLs that are meant for local storage

    FirebaseStorage storage; // used for uploading files
    FirebaseDatabase database; // used to store URLs of uploaded files
    ProgressDialog progressDialog;

    void handleError(Throwable throwable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (throwable instanceof MalformedURLException) {
            builder.setMessage(R.string.message_invalid_url);
        } else {
            // TODO: User-friendly error messages
            builder.setMessage(String.valueOf(throwable));
        }
        builder.setPositiveButton(R.string.button_ok, this);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        storage = FirebaseStorage.getInstance(); // return an object of Firebase Storage
        database = FirebaseDatabase.getInstance(); // return an object of Firebase Database

        selectFile = findViewById(R.id.selectFile);
        upload = findViewById(R.id.upload);
        notification = findViewById(R.id.notification);
        mTextUrl = (TextView) findViewById(R.id.text_url);
        mTextOriginalUrl = (TextView) findViewById(R.id.text_original_url);

//        FirebaseDynamicLinks.getInstance()
//                .getDynamicLink(getIntent())
//                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
//                    @Override
//                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
//                        Uri deepLink = null;
//                        if(pendingDynamicLinkData != null) {
//                            deepLink=pendingDynamicLinkData.getLink();
//                        }
//                    }
//                })
//                .addOnFailureListener(this, new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        //Log.w(TAG, "getDynamicLink:onFailure",e);
//                    }
//                });

        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    selectPdf();
                } else
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 9);

            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (pdfUri != null) // the user has selected the file
                    uploadFile(pdfUri);
                else
                    Toast.makeText(MainActivity.this, "Select a file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onClick(DialogInterface dialog, int which) {
        // Close activity after acknowledging error message.
        // It generally won't be difficult for the user to retry the request
        // from the activity the launched the original Intent.
        finish();
    }

    private void uploadFile(Uri pdfUri) {

        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Uploading file......");
        progressDialog.setProgress(0);
        progressDialog.show();

        final String fileName = System.currentTimeMillis() + "";
        StorageReference storageReference = storage.getReference(); // returns root path

        storageReference.child("Uploads").child(fileName).putFile(pdfUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        Task<Uri> url = taskSnapshot.getStorage().getDownloadUrl(); // return the url of uploaded file
                        //store the url in realtime database
                        DatabaseReference reference = database.getReference(); // return the path to root
                        while (!url.isSuccessful()) ;
                        Uri durl = url.getResult();
                        final String link = String.valueOf(durl);// 'link' has final URL stored in it
                        System.out.println(link);
                        mUrl = link;
                        mTextOriginalUrl.setText(mUrl);
                        shorten();


                        reference.child(fileName).setValue(link).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {

                                if (task.isSuccessful()) {
                                    Toast.makeText(MainActivity.this, "File successfully uploaded", Toast.LENGTH_SHORT).show();
                                } else
                                    Toast.makeText(MainActivity.this, "File not successfully uploaded", Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                Toast.makeText(MainActivity.this, "File not successfully uploaded", Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {

                // track the progress of our upload
                int currentProgress = (int) (100 * taskSnapshot.getBytesTransferred() / taskSnapshot.getTotalByteCount());
                progressDialog.setProgress(currentProgress);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 9 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            selectPdf();
        } else
            Toast.makeText(MainActivity.this, "Please provide permission..", Toast.LENGTH_SHORT).show();
    }

    private void selectPdf() {

        // to offer user to select a file using file manager

        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT); // to fetch files
        startActivityForResult(intent, 86);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        //check whether user has selected a file or not

        if (requestCode == 86 && resultCode == RESULT_OK && data != null) {
            pdfUri = data.getData(); // return the uri of selected file
            notification.setText("A file is selected : " + data.getData().getLastPathSegment());

        } else {
            Toast.makeText(MainActivity.this, "Please select a file", Toast.LENGTH_SHORT).show();
        }


    /*DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("https://www.example.com/"))
            .setDomainUriPrefix("https://example.page.link")
            // Open links with this app on Android
            .setAndroidParameters(new DynamicLink.AndroidParameters.Builder().build())
            // Open links with com.example.ios on iOS
            .setIosParameters(new DynamicLink.IosParameters.Builder("com.example.ios").build())
            .buildDynamicLink();

    Uri dynamicLinkUri = dynamicLink.getUri(); */

//    Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
//            .setLink(Uri.parse(address))
//            .setDomainUriPrefix("https://miniserver.page.link")
//            // Set parameters
//            // ...
//            .buildShortDynamicLink()
//            .addOnCompleteListener(this, new OnCompleteListener<ShortDynamicLink>() {
//                @Override
//                public void onComplete(@NonNull Task<ShortDynamicLink> task) {
//                    if (task.isSuccessful()) {
//                        // Short link created
//                        Uri shortLink = task.getResult().getShortLink();
//                        Uri flowchartLink = task.getResult().getPreviewLink();
//                        small=String.valueOf(shortLink); // shortened link
//                        System.out.println(small);
//
//                    } else {
//                        // Error
//                        // ...
//                    }
//                }
//            });
    }

    @Override
    public void onClick(View view) {

    }

    public void shorten() {
        if (!Util.isValidUrl(mUrl)) {
            sendError(new MalformedURLException());
            return;
        }
        try {
            HttpClient client = new DefaultHttpClient();
            String urlTemplate = "http://tinyurl.com/api-create.php?url=%s";
            String uri = String.format(urlTemplate, URLEncoder.encode(mUrl,"utf-8"));
            HttpGet request = new HttpGet(uri);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            InputStream in = entity.getContent();
            try {
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    // TODO: Support other encodings
                    String enc = "utf-8";
                    Reader reader = new InputStreamReader(in, enc);
                    BufferedReader bufferedReader = new BufferedReader(reader);
                    String tinyUrl = bufferedReader.readLine();
                    if (tinyUrl != null) {
                        sendUrl(tinyUrl);
                    } else {
                        throw new IOException("empty response");
                    }
                } else {
                    String errorTemplate = "unexpected response: %d";
                    String msg = String.format(errorTemplate, statusCode);
                    throw new IOException(msg);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            sendError(e);
        } catch (RuntimeException e) {
            sendError(e);
        } catch (Error e) {
            sendError(e);
        }

    }

    private void sendUrl(String url) {
        mHandler.obtainMessage(HANDLE_URL, url).sendToTarget();
    }

    private void sendError(Throwable t) {
        mHandler.obtainMessage(HANDLE_ERROR, t).sendToTarget();
    }

    public boolean handleMessage(Message msg) {
        if (isFinishing()) {
            return false;
        }
        switch (msg.what) {
            case HANDLE_URL:
                String url = (String) msg.obj;
                handleUrl(url);
                return true;
            case HANDLE_ERROR:
                Throwable t = (Throwable) msg.obj;
                handleError(t);
                return true;
            default:
                return false;
        }
    }

    void handleUrl(String url) {
        mTinyUrl = url;
        mTextUrl.setText(url);
        System.out.println(mTinyUrl);
        //mTextUrl.setVisibility(View.VISIBLE);
    }
}
