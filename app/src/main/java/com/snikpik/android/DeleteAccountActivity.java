package com.snikpik.android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.snikpik.android.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_USER_SETTINGS;


public class DeleteAccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userDbRef, resultsDbRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delete_account);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_button);
        }

        Button deleteAccountButton = findViewById(R.id.button_delete_account);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        userDbRef = FirebaseDatabase.getInstance().getReference("users");
        resultsDbRef = FirebaseDatabase.getInstance().getReference("results");

        deleteAccountButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteUserAccount();
            }
        });
    }

    public void deleteUserAccount(){
     userDbRef.child(currentUser.getUid()).removeValue();
     resultsDbRef.child(currentUser.getUid()).removeValue();

        currentUser.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()){
                    SharedPreferences settings = getSharedPreferences(SHARED_PREFS_USER_SETTINGS, MODE_PRIVATE);
                    settings.edit().clear().apply();
                    Toast.makeText(DeleteAccountActivity.this, getString(R.string.account_deleted) , Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    startActivity(new Intent(DeleteAccountActivity.this, WelcomeActivity.class));
                    finish();
                }
            }
        });
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
