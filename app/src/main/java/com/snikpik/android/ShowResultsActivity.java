package com.snikpik.android;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.snikpik.android.adapter.ExpresserAdapter;
import com.snikpik.android.model.PollData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShowResultsActivity extends AppCompatActivity {
    private List<PollData> pollData;
    private RecyclerView rv;
    private ExpresserAdapter adapter;
    private DatabaseReference pollResultRef;
    private ImageView foxBlackWhite;
    private TextView noAnswerTextView;
    private FirebaseUser currentUser;
    private SpinKitView spinKitView;
    private ValueEventListener pollEventListener;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        setContentView(R.layout.activity_show_results);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_button);
        }

        foxBlackWhite = findViewById(R.id.fox_image_view);
        noAnswerTextView = findViewById(R.id.no_answers_yet);
        spinKitView = findViewById(R.id.progressBar);

        rv = findViewById(R.id.recycler);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));
        pollData = new ArrayList<>();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        pollResultRef = FirebaseDatabase.getInstance().getReference("results");

        fetchAnswers();

    }

    public void fetchAnswers() {
        spinKitView.setVisibility(View.VISIBLE);
        pollEventListener = pollResultRef.child(currentUser.getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()){
                            pollData.clear();
                            spinKitView.setVisibility(View.GONE);
                            for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                                PollData data = new PollData();
                                data.setAnswer(String.valueOf(snapshot.child("answer").getValue()));
                                data.setTimestamp(String.valueOf(snapshot.child("timestamp").getValue()));
                                data.setAnswerId(snapshot.getKey());
                                data.setDevice_id(String.valueOf(snapshot.child("device_id").getValue()));
                                pollData.add(data);
                                adapter = new ExpresserAdapter(pollData,ShowResultsActivity.this);
                                rv.setAdapter(adapter);
                            }
                            Collections.reverse(pollData);
                            adapter.notifyDataSetChanged();
                        } else{
                            spinKitView.setVisibility(View.GONE);
                            foxBlackWhite.setVisibility(View.VISIBLE);
                            noAnswerTextView.setVisibility(View.VISIBLE);
                            rv.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ShowResultsActivity.this, getString(R.string.error_occurred) , Toast.LENGTH_SHORT).show();
                    }
                });
    }

        @Override
    protected void onDestroy() {
        super.onDestroy();
        pollResultRef.child(currentUser.getUid()).removeEventListener(pollEventListener);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

}
