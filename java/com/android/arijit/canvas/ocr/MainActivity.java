package com.android.arijit.canvas.ocr;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.snackbar.Snackbar;
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

public class MainActivity extends AppCompatActivity {
    private DrawView paint;
    private String TAG = "MainActivity";
    private View card, comSet;
    private Button ok, copy;
    private EditText etXt;
    private Animation reveal, hide;
    private RangeSlider radiusSlider;
    private CheckBox cb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);
        paint = (DrawView) findViewById(R.id.canvas);
        card = findViewById(R.id.ocr_result);
        ok = ((Button) findViewById(R.id.btn_ok));
        copy = ((Button) findViewById(R.id.btn_copy));
        etXt = ((EditText) findViewById(R.id.ocr_xt));
        reveal = AnimationUtils.loadAnimation(this, R.anim.card_reveal);
        hide = AnimationUtils.loadAnimation(this, R.anim.card_hide);
        radiusSlider = (RangeSlider) findViewById(R.id.radius_slider);
        radiusSlider.setValueFrom(0.0f);
        radiusSlider.setValueTo(800.0f);
        comSet = findViewById(R.id.compassset);
        cb = findViewById(R.id.cb);

        checkDownloadLanguageData();

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
//        paint.setmMode(1);
        (findViewById(R.id.compass)).setOnClickListener(v -> {
            paint.setSettingCenter(null);
            if(comSet.getVisibility() == View.GONE)
                comSet.setVisibility(View.VISIBLE);
            else
                comSet.setVisibility(View.GONE);
            cb.setChecked(false);
            paint.setmMode(0);
        });
        ((ImageView) findViewById(R.id.clear)).setOnClickListener(v -> {
            paint.clear();
            if(card.getVisibility()==View.VISIBLE) {
                card.setVisibility(View.GONE);
                card.startAnimation(hide);
            }
            cb.setChecked(false);
            paint.setSettingCenter(false);
            paint.setmMode(0);
            if(comSet.getVisibility() == View.VISIBLE){
                comSet.setVisibility(View.GONE);
            }
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

            DigitalInkRecognizer recognizer = DigitalInkRecognition.
                    getClient(DigitalInkRecognizerOptions.builder(model).build());
            recognizer.recognize(paint.getInk())
                .addOnSuccessListener(new OnSuccessListener<RecognitionResult>() {
                    @Override
                    public void onSuccess(@NonNull  RecognitionResult recognitionResult) {
                        String text = recognitionResult.getCandidates().get(0).getText();
                        Log.i(TAG, "onSuccess: "+text);
                        card.setVisibility(View.VISIBLE);
                        card.startAnimation(reveal);
                        ((EditText) findViewById(R.id.ocr_xt))
                                .setText(text);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "onCreate: "+e.getMessage());
                });
        });

        ok.setOnClickListener(v -> {
            card.setVisibility(View.GONE);
            card.startAnimation(hide);
        });

        copy.setOnClickListener(v -> {
            String fill = etXt.getText().toString();
            copyToClipboard(fill);
            Snackbar.make(v, getResources().getText(R.string.copy_message), Snackbar.LENGTH_SHORT).show();
        });

        radiusSlider.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                paint.setmRadius(value);
            }
        });
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                paint.setmMode(isChecked?1:0);
                paint.setSettingCenter(null);
            }
        });
    }
    private void copyToClipboard(String toCopy){
        ClipboardManager clipboardManager = (ClipboardManager) this.getSystemService(Context.CLIPBOARD_SERVICE);
        String clip = toCopy;
        ClipData clipData = ClipData.newPlainText("scan", clip);
        clipboardManager.setPrimaryClip(clipData);
    }

    private void checkDownloadLanguageData(){
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

        ProgressDialog mProgressDialog;
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setMessage("Getting things Ready...");
        remoteModelManager.isModelDownloaded(model)
                .addOnSuccessListener(new OnSuccessListener<Boolean>() {
                    @Override
                    public void onSuccess(@NonNull Boolean aBoolean) {
                        if(!aBoolean.booleanValue()){
                            mProgressDialog.show();
                            remoteModelManager
                                    .download(model, new DownloadConditions.Builder().build())
                                    .addOnSuccessListener(aVoid -> {
                                        mProgressDialog.dismiss();
                                        Log.i(TAG, "onSuccess: model download");
                                    })
                                    .addOnFailureListener(e -> Log.i(TAG, "onSuccess: fail down"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.i(TAG, "onCreate: "+e.getMessage());
                });
    }


    @Override
    public void onBackPressed() {
        if(card.getVisibility()==View.VISIBLE) {
            card.setVisibility(View.GONE);
            card.startAnimation(hide);
        }
        else
            super.onBackPressed();
    }
}