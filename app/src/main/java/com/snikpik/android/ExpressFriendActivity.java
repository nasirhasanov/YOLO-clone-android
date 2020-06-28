package com.snikpik.android;

import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.google.firebase.dynamiclinks.PendingDynamicLinkData;

import java.util.HashMap;

public class ExpressFriendActivity extends AppCompatActivity {

    private TextView hiThereTextView,pollTextView, counterTextView;
    private EditText answerEditText;
    private FirebaseAuth mAuth;
    private DatabaseReference userDbRef,pollResultRef, blackListDbRef;
    private Button submitButton, continueButton;
    private FirebaseUser currentUser;
    private String otherUserId, pollAnswer;
    private CardView cardView;
    private SpinKitView spinKitView, sendButtonProgress;
    private ProgressDialog progressDialog;
    private ImageView snikPikIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer_poll);

        mAuth = FirebaseAuth.getInstance();
        hiThereTextView = findViewById(R.id.hi_there_text_view);
        answerEditText = findViewById(R.id.edit_text_reply_in_snap);
        submitButton = findViewById(R.id.button_submit_answer);
        pollTextView = findViewById(R.id.pollText);
        cardView = findViewById(R.id.cardView);
        continueButton = findViewById(R.id.button_continue);
        spinKitView = findViewById(R.id.progressBar);
        sendButtonProgress = findViewById(R.id.send_button_progress);
        counterTextView = findViewById(R.id.counter_text_view);
        snikPikIcon = findViewById(R.id.profile_image);

        userDbRef = FirebaseDatabase.getInstance().getReference("users");
        pollResultRef = FirebaseDatabase.getInstance().getReference("results");
        blackListDbRef = FirebaseDatabase.getInstance().getReference("blacklist");

        currentUser =  mAuth.getCurrentUser();
        answerEditText.addTextChangedListener(mTextEditorWatcher);

        Intent intent = getIntent();
        final Uri pollLink = intent.getData();
        FirebaseDynamicLinks.getInstance()
                .getDynamicLink(getIntent())
                .addOnSuccessListener(this, new OnSuccessListener<PendingDynamicLinkData>() {
                    @Override
                    public void onSuccess(PendingDynamicLinkData pendingDynamicLinkData) {
                        // Get deep link from result (may be null if no link is found)
                        Uri deepLink;
                        if (pendingDynamicLinkData != null) {
                            deepLink = pendingDynamicLinkData.getLink();
                        }else {
                            deepLink = pollLink;
                        }

                        if (deepLink!=null) {
                            otherUserId = deepLink.getQueryParameter("invitedby");
                        }

                        if (currentUser==null){
                            signInAnonymously();
                        }else {
                            checkBlacklistStatus();
                            getQuestionInfo();
                        }
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("error", "getDynamicLink:onFailure", e);
                    }
                });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isNetworkAvailable()){
                    Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();
                    return;
                }

                pollAnswer = answerEditText.getText().toString().trim();

                if (TextUtils.isEmpty(pollAnswer)){
                        Toast.makeText(ExpressFriendActivity.this, getString(R.string.fill_detail) , Toast.LENGTH_SHORT).show();
                    }else{
                        submitButton.setVisibility(View.GONE);
                        sendButtonProgress.setVisibility(View.VISIBLE);
                        hideKeyboard();
                        String pollId = pollResultRef.child(otherUserId).push().getKey();

                        HashMap<String, Object> newMessage = new HashMap<>();
                        newMessage.put("answer",pollAnswer);
                        newMessage.put("timestamp", ServerValue.TIMESTAMP);
                        newMessage.put("device_id", currentUser.getUid());

                        pollResultRef.child(otherUserId).child(pollId)
                                .updateChildren(newMessage)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            submitButton.setVisibility(View.GONE);
                                            sendButtonProgress.setVisibility(View.GONE);
                                            cardView.setVisibility(View.GONE);
                                            hiThereTextView.setVisibility(View.VISIBLE);
                                            continueButton.setVisibility(View.VISIBLE);
                                            ObjectAnimator animation = ObjectAnimator.ofFloat(snikPikIcon, "translationY",   200f);
                                            animation.setDuration(2000);
                                            animation.start();
                                        }else{
                                            submitButton.setVisibility(View.VISIBLE);
                                            Toast.makeText(ExpressFriendActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();

                                        }
                                    }
                                });

                    }
            }
        });

        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ExpressFriendActivity.this, WelcomeActivity.class));
                finish();
            }
        });

    }

    private void getQuestionInfo(){
        spinKitView.setVisibility(View.VISIBLE);
        userDbRef.child(otherUserId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    if (dataSnapshot.child("blocked").child(currentUser.getUid()).exists()) {
                        spinKitView.setVisibility(View.GONE);
                        hiThereTextView.setVisibility(View.VISIBLE);
                        hiThereTextView.setText(getString(R.string.you_cant_reply));
                        continueButton.setVisibility(View.VISIBLE);
                    } else {
                        if (dataSnapshot.child("question").exists()) {
                            spinKitView.setVisibility(View.GONE);
                            cardView.setVisibility(View.VISIBLE);
                            submitButton.setVisibility(View.VISIBLE);
                            String pollText = dataSnapshot.child("question").getValue().toString();
                            pollTextView.setText(pollText);
                            answerEditText.requestFocus();
                        } else {
                            spinKitView.setVisibility(View.GONE);
                            hiThereTextView.setVisibility(View.VISIBLE);
                            hiThereTextView.setText(getString(R.string.poll_not_exist));
                            continueButton.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ExpressFriendActivity.this, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void checkBlacklistStatus(){
        if (currentUser!=null) {
            blackListDbRef.child(currentUser.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        if (dataSnapshot.child("status").getValue().toString().equals("true")) {
                            Intent intent = new Intent(ExpressFriendActivity.this, BlacklistedActivity.class);
                            intent.putExtra("timestamp", dataSnapshot.child("timestamp").getValue().toString());
                            intent.putExtra("interval", dataSnapshot.child("interval").getValue().toString());
                            startActivity(intent);
                            finish();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }
    }

    private final TextWatcher mTextEditorWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            counterTextView.setText(s.length()+" / 120");
        }

        public void afterTextChanged(Editable s) {
            if (null != answerEditText.getLayout() && answerEditText.getLayout().getLineCount() > 6) {
                answerEditText.getText().delete(answerEditText.getText().length() - 1, answerEditText.getText().length());
            }
        }
    };

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void signInAnonymously(){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInAnonymously()
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            currentUser = mAuth.getCurrentUser();
                            progressDialog.dismiss();
                            getQuestionInfo();
                        }
                    }
                });
    }

    public void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
