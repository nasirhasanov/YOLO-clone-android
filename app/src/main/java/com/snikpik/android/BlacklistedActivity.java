package com.snikpik.android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.concurrent.TimeUnit;

public class BlacklistedActivity extends AppCompatActivity {
    private TextView timeIntervalTv, infoTextView;
    private ImageView blackWhiteFox;
    private Button closeButton;
    private ProgressBar progressBar;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklisted);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        timeIntervalTv = findViewById(R.id.time_interval);
        infoTextView = findViewById(R.id.info_unblock);
        blackWhiteFox = findViewById(R.id.black_white_fox);
        progressBar = findViewById(R.id.progressBar);
        closeButton = findViewById(R.id.button_close);


        Intent intent = getIntent();
        final String blackListedTimeStamp = intent.getStringExtra("timestamp");
        final String interval = intent.getStringExtra("interval");


        progressBar.setVisibility(View.VISIBLE);
        FirebaseFunctions.getInstance().getHttpsCallable("getTime")
                .call().addOnSuccessListener(new OnSuccessListener<HttpsCallableResult>() {
            @Override
            public void onSuccess(HttpsCallableResult httpsCallableResult) {
                long timestampNow = (long) httpsCallableResult.getData();
                long timestampBlocked = Long.valueOf(blackListedTimeStamp);

                long diffInMs = timestampNow - timestampBlocked;
                long diffInSec = TimeUnit.MILLISECONDS.toSeconds(diffInMs);

                switch (interval){
                    case "permanent":
                        progressBar.setVisibility(View.GONE);
                        infoTextView.setVisibility(View.VISIBLE);
                        blackWhiteFox.setVisibility(View.VISIBLE);
                        timeIntervalTv.setVisibility(View.VISIBLE);
                        timeIntervalTv.setText(getString(R.string.permanent));
                        closeButton.setVisibility(View.VISIBLE);
                        break;
                    case "24":
                        if (diffInSec/3600>=24){
                            unblockUser();
                        }else{
                            progressBar.setVisibility(View.GONE);
                            infoTextView.setVisibility(View.VISIBLE);
                            blackWhiteFox.setVisibility(View.VISIBLE);
                            timeIntervalTv.setVisibility(View.VISIBLE);
                            timeIntervalTv.setText(getString(R.string.twenty_four));
                            closeButton.setVisibility(View.VISIBLE);

                        }break;

                    case "48":
                        if (diffInSec/3600>=48){
                            unblockUser();
                        }else{
                            progressBar.setVisibility(View.GONE);
                            infoTextView.setVisibility(View.VISIBLE);
                            blackWhiteFox.setVisibility(View.VISIBLE);
                            timeIntervalTv.setVisibility(View.VISIBLE);
                            timeIntervalTv.setText(getString(R.string.forty_eight));
                            closeButton.setVisibility(View.VISIBLE);

                        }break;

                }

            }


            private void unblockUser() {
                DatabaseReference blackListDbRef = FirebaseDatabase.getInstance().getReference("blacklist");
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser!=null){
                    blackListDbRef.child(currentUser.getUid()).child("status").setValue("false")
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    Toast.makeText(BlacklistedActivity.this, "Unblocked! Try again", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(BlacklistedActivity.this, MainActivity.class));
                                    finish();
                                }
                            });
                }



            }
        });


        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}
