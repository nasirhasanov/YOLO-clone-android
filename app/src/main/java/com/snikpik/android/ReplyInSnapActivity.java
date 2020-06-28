package com.snikpik.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;


import com.snapchat.kit.sdk.SnapCreative;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitApi;
import com.snapchat.kit.sdk.creative.exceptions.SnapStickerSizeException;
import com.snapchat.kit.sdk.creative.media.SnapMediaFactory;
import com.snapchat.kit.sdk.creative.media.SnapSticker;
import com.snapchat.kit.sdk.creative.models.SnapLiveCameraContent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.snikpik.android.helper.AppUtils.deleteCache;
import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_USER_SETTINGS;

public class ReplyInSnapActivity extends AppCompatActivity {

    private EditText replySnapEdittext;
    private RelativeLayout dialogView;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_reply_with_snap);

        TextView anonymousTextView = findViewById(R.id.anonymText);
        Button replySnapButton = findViewById(R.id.button_reply_in_snap);
        dialogView = findViewById(R.id.dialog_alert);
        ConstraintLayout replyLayout = findViewById(R.id.reply_layout);
        replySnapEdittext = findViewById(R.id.edit_text_reply_in_snap);
        replySnapEdittext.addTextChangedListener(mTextEditorWatcher);
        replySnapEdittext.requestFocus();

        Intent intent = getIntent();
        String anonymousText = intent.getStringExtra("anonymousText");
        anonymousTextView.setText(anonymousText);

        deleteCache(this);


        replySnapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (TextUtils.isEmpty(replySnapEdittext.getText().toString().trim())){
                    Toast.makeText(ReplyInSnapActivity.this, getString(R.string.fill_detail) , Toast.LENGTH_SHORT).show();
                }else{
                    replySnapEdittext.clearFocus();
                    Bitmap bitmap = getBitmapFromView(dialogView);
                    shareReplySticker(bitmap);
                }
            }
        });


        replyLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, android.R.anim.fade_out);
            }
        });

    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        deleteCache(this);
    }

    private void shareReplySticker(Bitmap bitmap) {

        String newSticker = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

        try {
            FileOutputStream stream = new FileOutputStream(this.getCacheDir() + "/"+newSticker+".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        File replySticker = new File(this.getCacheDir(), newSticker+".png");

        if (replySticker.exists()) {

            try {
                if(!appInstalledOrNot()){
                    Toast.makeText(this, "Snapchat app is not installed", Toast.LENGTH_SHORT).show();
                    return;
                }

                final SnapCreativeKitApi snapCreativeKitApi = SnapCreative.getApi(this);
                final SnapMediaFactory snapMediaFactory = SnapCreative.getMediaFactory(this);
                SnapLiveCameraContent snapLiveCameraContent = new SnapLiveCameraContent();
                final SnapSticker snapSticker = snapMediaFactory.getSnapStickerFromFile(replySticker);
                snapSticker.setWidth(dialogView.getWidth());
                snapSticker.setHeight(dialogView.getHeight());
                snapSticker.setPosX(0.5f);
                snapSticker.setPosY(0.5f);

                SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE);
                String pollLink = prefs.getString("pollLink",null);

                snapLiveCameraContent.setAttachmentUrl(pollLink);
                snapLiveCameraContent.setSnapSticker(snapSticker);
                //Toast.makeText(this, "Attached to Snap", Toast.LENGTH_SHORT).show();
                snapCreativeKitApi.send(snapLiveCameraContent);

            } catch (SnapStickerSizeException e) {
                Toast.makeText(this, "Error occurred", Toast.LENGTH_SHORT).show();
            }


        }
    }

    private  Bitmap getBitmapFromView(final View view) {

        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable =view.getBackground();

        bgDrawable.draw(canvas);

        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
           // counterTextView.setText(String.valueOf(s.length())+" / 100");
        }

        public void afterTextChanged(Editable s) {
            if (null != replySnapEdittext.getLayout() && replySnapEdittext.getLayout().getLineCount() > 4) {
                replySnapEdittext.getText().delete(replySnapEdittext.getText().length() - 1, replySnapEdittext.getText().length());
            }
        }
    };
    private boolean appInstalledOrNot() {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo("com.snapchat.android", PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(this, getString(R.string.error_occurred), Toast.LENGTH_SHORT).show();
        }
        return false;
    }
}
