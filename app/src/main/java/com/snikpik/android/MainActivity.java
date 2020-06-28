package com.snikpik.android;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.ShortDynamicLink;
import com.snapchat.kit.sdk.SnapCreative;
import com.snapchat.kit.sdk.creative.api.SnapCreativeKitApi;
import com.snapchat.kit.sdk.creative.exceptions.SnapStickerSizeException;
import com.snapchat.kit.sdk.creative.media.SnapMediaFactory;
import com.snapchat.kit.sdk.creative.media.SnapSticker;
import com.snapchat.kit.sdk.creative.models.SnapLiveCameraContent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import static com.snikpik.android.helper.AppUtils.deleteCache;
import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_USER_SETTINGS;


public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private TextView questionTextView, counterTextView, swipeUpTextView;
    private Button startPollButton, showPollResultsButton;
    private EditText pollEditText;
    private FirebaseAuth.AuthStateListener authListener;
    private FirebaseUser currentUser;
    private ImageButton moreChoices, shuffleButton;
    private String pollLink;
    private DatabaseReference userDbRef,resultsDbRef;
    private SpinKitView spinKitProgress;
    private CardView cardView;
    private ValueEventListener userEventListener;
    private File mStickerSwipUp;
    private RelativeLayout stickerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        counterTextView = findViewById(R.id.counter_text_view);
        questionTextView = findViewById(R.id.share_poll_textview);
        startPollButton = findViewById(R.id.start_poll_button);
        shuffleButton = findViewById(R.id.shuffle_button);
        showPollResultsButton = findViewById(R.id.show_explainme_results);
        pollEditText = findViewById(R.id.edit_text_reply_in_snap);
        moreChoices = findViewById(R.id.more_choices_button);
        spinKitProgress = findViewById(R.id.progressBar);
        cardView = findViewById(R.id.cardView);
        currentUser = mAuth.getCurrentUser();
        stickerLayout = findViewById(R.id.main_view_group);
        swipeUpTextView = findViewById(R.id.swipe_up_text_view);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_settings);
        }


        pollEditText.addTextChangedListener(mTextEditorWatcher);

        userDbRef = FirebaseDatabase.getInstance().getReference("users");
        resultsDbRef = FirebaseDatabase.getInstance().getReference("results");

        spinKitProgress.setVisibility(View.VISIBLE);
        userEventListener = userDbRef.child(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            SharedPreferences.Editor edit = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE).edit();
                            if (dataSnapshot.child("pollLink").exists()){
                                edit.putString("pollLink", dataSnapshot.child("pollLink").getValue().toString());
                                edit.apply();
                            }else{
                                generateLink();
                            }
                        }
                        if (dataSnapshot.child("question").exists()){
                                spinKitProgress.setVisibility(View.GONE);
                                cardView.setVisibility(View.VISIBLE);
                                pollEditText.setVisibility(View.GONE);
                                startPollButton.setVisibility(View.GONE);
                                shuffleButton.setVisibility(View.GONE);
                                counterTextView.setVisibility(View.GONE);
                                questionTextView.setText(dataSnapshot.child("question").getValue().toString());
                                questionTextView.setVisibility(View.VISIBLE);
                                showPollResultsButton.setVisibility(View.VISIBLE);
                                moreChoices.setVisibility(View.VISIBLE);

                        }else {
                            spinKitProgress.setVisibility(View.GONE);
                            cardView.setVisibility(View.VISIBLE);
                            pollEditText.setVisibility(View.VISIBLE);
                            pollEditText.getText().clear();
                            questionTextView.setVisibility(View.GONE);
                            startPollButton.setVisibility(View.VISIBLE);
                            shuffleButton.setVisibility(View.VISIBLE);
                            counterTextView.setVisibility(View.VISIBLE);
                            showPollResultsButton.setVisibility(View.GONE);
                            moreChoices.setVisibility(View.GONE);
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });



        authListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    startActivity(new Intent(MainActivity.this,WelcomeActivity.class));
                    finish();
                }
            }
        };

    }

    @Override
    protected void onResume() {
        super.onResume();
        deleteCache(this);
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE);
        int newAnswerCount = prefs.getInt("new_answer_count",0);
        if (newAnswerCount!=0){
            String newAnswersText = getString(R.string.new_answers, newAnswerCount);
            showPollResultsButton.setText(newAnswersText);
        }else{
            showPollResultsButton.setText(getString(R.string.show_results));
        }

        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    @Override
    public void onStart() {
        super.onStart();
        deleteCache(this);
        mAuth.addAuthStateListener(authListener);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mStickerSwipUp = new File(getCacheDir(), "swipeup.png");
        if (!mStickerSwipUp.exists()) {
            try (InputStream inputStream = getAssets().open("swipeup.png")) {
                copyFile(inputStream, mStickerSwipUp);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        userDbRef.child(currentUser.getUid()).removeEventListener(userEventListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (authListener != null) {
            mAuth.removeAuthStateListener(authListener);
        }
    }


    public void generateLink(){

        final String sharelinktext  = "https://snikpik.page.link/?"+
                "link=https://www.snik.com/?invitedby="+currentUser.getUid()+
                "&apn="+ getPackageName()+
                "&st="+"SnikPik"+
                "&sd="+getString(R.string.answer) +
                "&si="+"https://firebasestorage.googleapis.com/v0/b/wonder-ba3cc.appspot.com/o/ic_launcher-web.png?alt=media&token=998d7c51-b483-45db-a9d0-d8317b0d1009";

        // shorten the link
        Task<ShortDynamicLink> shortLinkTask = FirebaseDynamicLinks.getInstance().createDynamicLink()
                .setLongLink(Uri.parse(sharelinktext))  // manually
                .buildShortDynamicLink()
                .addOnCompleteListener(this, new OnCompleteListener<ShortDynamicLink>() {
                    @Override
                    public void onComplete(@NonNull Task<ShortDynamicLink> task) {
                        if (task.isSuccessful()) {

                            Uri shortLink = task.getResult().getShortLink();
                            userDbRef.child(currentUser.getUid()).child("pollLink").setValue(shortLink.toString());

                        } else {
                            Log.e("main", " error "+task.getException() );
                        }
                    }
                });

    }
    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            counterTextView.setText(s.length() +" / 100");
        }

        public void afterTextChanged(Editable s) {
            if (null != pollEditText.getLayout() && pollEditText.getLayout().getLineCount() > 5) {
                pollEditText.getText().delete(pollEditText.getText().length() - 1, pollEditText.getText().length());
            }
        }
    };

    private void updatePolltoDb(){

        if (!isNetworkAvailable()){
            Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();
            return;
        }

        if (pollEditText.getText().toString().trim().equals("")){
            Toast.makeText(this, getString(R.string.please_type_poll_ques), Toast.LENGTH_SHORT).show();
        }else{
            final String pollText = pollEditText.getText().toString().trim();

            userDbRef.child(currentUser.getUid()).child("question").setValue(pollText)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                resultsDbRef.child(currentUser.getUid()).setValue(null);
                                questionTextView.setText(pollText);
                                pollEditText.setVisibility(View.GONE);
                                counterTextView.setVisibility(View.GONE);
                                SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE);
                                pollLink = prefs.getString("pollLink",null);
                                attachToSnap();
                                if (pollLink==null){
                                    generateLink();
                                }
                            }
                        }
                    });

        }
    }

    private void setMoreChoices(){
        // setup the alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        final String[] animals = {getString(R.string.change_poll_question) , getString(R.string.delete_poll) , getString(R.string.copy_poll_link), getString(R.string.attach_to_snap)};
        builder.setItems(animals, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        pollEditText.setVisibility(View.VISIBLE);
                        startPollButton.setVisibility(View.VISIBLE);
                        shuffleButton.setVisibility(View.VISIBLE);
                        counterTextView.setVisibility(View.VISIBLE);
                        questionTextView.setVisibility(View.GONE);
                        pollEditText.setText(questionTextView.getText());
                        showPollResultsButton.setVisibility(View.GONE);
                        moreChoices.setVisibility(View.GONE);
                        break;
                    case 1:
                        userDbRef.child(currentUser.getUid()).child("question").setValue(null);
                        resultsDbRef.child(currentUser.getUid()).setValue(null);
                        break;
                    case 2:
                        copyToClipboard();
                        break;

                    case 3:
                        attachToSnap();
                        break;
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void copyToClipboard(){
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE);
        pollLink = prefs.getString("pollLink",null);
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("link", pollLink);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getString(R.string.poll_link_copied) , Toast.LENGTH_SHORT).show();
    }


    public void attachToSnap(){
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE);
        pollLink = prefs.getString("pollLink",null);

        swipeUpTextView.setTextColor(getResources().getColor(R.color.gainsboro));
        moreChoices.setVisibility(View.GONE);

        Bitmap bitmap = getBitmapFromView(stickerLayout);

        String newSticker = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());

        try {
            FileOutputStream stream = new FileOutputStream(this.getCacheDir() + "/"+newSticker+".png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.flush();
            stream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        File shareSticker = new File(this.getCacheDir(), newSticker+".png");

        if(!appInstalledOrNot()){
            Toast.makeText(this, "Snapchat app is not installed", Toast.LENGTH_SHORT).show();
            return;
        }


        try {

            SnapCreativeKitApi snapCreativeKitApi = SnapCreative.getApi(this);
            SnapMediaFactory snapMediaFactory = SnapCreative.getMediaFactory(this);
            SnapLiveCameraContent snapLiveCameraContent = new SnapLiveCameraContent();
            SnapSticker snapSticker = snapMediaFactory.getSnapStickerFromFile(shareSticker);

            // Height and width~~ ~~in pixels
            snapSticker.setWidth(stickerLayout.getWidth());
            snapSticker.setHeight(stickerLayout.getHeight());

            // Position is specified as a ratio between 0 & 1 to place the center of the sticker
            snapSticker.setPosX(0.5f);
            snapSticker.setPosY(0.5f);

            snapLiveCameraContent.setSnapSticker(snapSticker);
            snapLiveCameraContent.setAttachmentUrl(pollLink);
            //Toast.makeText(this, "Attached to Snap", Toast.LENGTH_SHORT).show();
            snapCreativeKitApi.send(snapLiveCameraContent);
        } catch (SnapStickerSizeException e) {
            Toast.makeText(this, "Error occurred", Toast.LENGTH_SHORT).show();
        }

    }

    public void setRandomQuestion(){
        Random rand = new Random();
        int randomNum = rand.nextInt((10 - 1) + 1) + 1;

        String[] questions = {
                getString(R.string.question1),
                getString(R.string.question2),
                getString(R.string.question3),
                getString(R.string.question4),
                getString(R.string.question5),
                getString(R.string.question6),
                getString(R.string.question7),
                getString(R.string.question8),
                getString(R.string.question9),
                getString(R.string.question10)
        };

        pollEditText.setText(questions[randomNum-1]);
    }

    public void onClick(View v){
        switch (v.getId()){
            case R.id.start_poll_button:
                updatePolltoDb();
                break;

            case R.id.show_explainme_results:
                SharedPreferences.Editor edit = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE).edit();
                edit.putInt("new_answer_count", 0);
                edit.apply();
                startActivity(new Intent(MainActivity.this, ShowResultsActivity.class));
                break;

            case R.id.more_choices_button:
                setMoreChoices();
                break;

            case R.id.shuffle_button:
                setRandomQuestion();
                break;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private Bitmap getBitmapFromView(final View view) {

        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(),Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable =view.getBackground();

        bgDrawable.draw(canvas);

        view.draw(canvas);

        swipeUpTextView.setTextColor(Color.TRANSPARENT);
        moreChoices.setVisibility(View.VISIBLE);

        //return the bitmap
        return returnedBitmap;
    }

    private static void copyFile(InputStream inputStream, File file) throws IOException {
        byte[] buffer = new byte[1024];
        int length;

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

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
