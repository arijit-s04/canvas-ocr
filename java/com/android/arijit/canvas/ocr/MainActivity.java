package com.android.arijit.canvas.ocr;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.common.MlKitException;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.vision.digitalink.DigitalInkRecognition;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer;
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions;
import com.google.mlkit.vision.digitalink.Ink;
import com.google.mlkit.vision.digitalink.RecognitionResult;

import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private DrawView paint;
    private String TAG = "MainActivity";
    private ProgressDialog mProgressDialog;
    private View card;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        paint = (DrawView) findViewById(R.id.canvas);
        card = findViewById(R.id.ocr_result);


        ViewTreeObserver vto = paint.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                paint.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int width = paint.getMeasuredWidth();
                int height = paint.getMeasuredHeight();
                paint.init(height, width);
            }
        });

        ((ImageView) findViewById(R.id.clear)).setOnClickListener(v -> {
            paint.clear();
            if(card.getVisibility()==View.VISIBLE)
                card.setVisibility(View.INVISIBLE);
        });

        ((ImageView)findViewById(R.id.ocr)).setOnClickListener(v -> {
            Ink ink = paint.getInk();
            Log.i(TAG, "onCreate: null "+(ink==null));
            // Specify the recognition model for a language
            DigitalInkRecognitionModelIdentifier modelIdentifier;
            try {
                modelIdentifier =
                        DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US");
            } catch (MlKitException e) {
                modelIdentifier = null;
            }
            if (modelIdentifier == null) {}

            DigitalInkRecognitionModel model = DigitalInkRecognitionModel.builder(modelIdentifier).build();
            RemoteModelManager remoteModelManager = RemoteModelManager.getInstance();
            remoteModelManager.isModelDownloaded(model)
                    .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                        @Override
                        public void onSuccess(@NonNull Boolean aBoolean) {
                            if(!aBoolean.booleanValue()){
                                remoteModelManager
                                        .download(model, new DownloadConditions.Builder().build())
                                        .addOnSuccessListener(aVoid -> {
                                            Log.i(TAG, "onSuccess: model download");
                                        })
                                        .addOnFailureListener(e -> Log.i(TAG, "onSuccess: fail down"));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.i(TAG, "onCreate: "+e.getMessage());
                    });

            DigitalInkRecognizer recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model).build());
            recognizer.recognize(paint.getInk())
                    .addOnSuccessListener(new OnSuccessListener<RecognitionResult>() {
                        @Override
                        public void onSuccess(@NonNull  RecognitionResult recognitionResult) {
                            String text = recognitionResult.getCandidates().get(0).getText();
                            Log.i(TAG, "onSuccess: "+text);
                            card.setVisibility(View.VISIBLE);
                            ((EditText) findViewById(R.id.ocr_xt))
                                    .setText(text);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.i(TAG, "onCreate: "+e.getMessage());
                    });
        });

        ((ImageView) findViewById(R.id.save)).setOnClickListener(v -> {
            Bitmap bmp = paint.save();

            // opening a OutputStream to write into the file
            OutputStream imageOutStream = null;

            ContentValues cv = new ContentValues();

            // name of the file
            cv.put(MediaStore.Images.Media.DISPLAY_NAME, "drawing.png");

            // type of the file
            cv.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            // location of the file to be saved
            cv.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            // get the Uri of the file which is to be created in the storage
            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv);
            try {
                // open the output stream with the above uri
                imageOutStream = getContentResolver().openOutputStream(uri);

                // this method writes the files in storage
                bmp.compress(Bitmap.CompressFormat.PNG, 100, imageOutStream);

                // close the output stream after use
                imageOutStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if(card.getVisibility()==View.VISIBLE)
            card.setVisibility(View.INVISIBLE);
        else
            super.onBackPressed();
    }
}