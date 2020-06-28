package com.snikpik.android;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.snikpik.android.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText currentPassword, newPassword, newPasswordAgain;
    private Button changePassword;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_button);
        }

        currentPassword = findViewById(R.id.edit_text_current_pass);
        newPassword = findViewById(R.id.edit_text_password);
        newPasswordAgain = findViewById(R.id.edit_text_password_again);
        changePassword = findViewById(R.id.button_change_password);
        progressBar = findViewById(R.id.progressBar);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        changePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeUserPassword();
            }
        });
    }

    public void changeUserPassword(){
        final String currentParol = currentPassword.getText().toString().trim();
        final String newParol =  newPassword.getText().toString().trim();
        final String newParolAgain = newPasswordAgain.getText().toString().trim();

        if (TextUtils.isEmpty(currentParol)||TextUtils.isEmpty(newParol)||TextUtils.isEmpty(newParolAgain)){
            Toast.makeText(this, getString(R.string.enter_required_field) , Toast.LENGTH_SHORT).show();
            return;
        }
        if (newParol.length()<6){
            Toast.makeText(ChangePasswordActivity.this, getString(R.string.must_be_characters) , Toast.LENGTH_SHORT).show();
            return;
        }
        if (!newParol.equals(newParolAgain)){
            Toast.makeText(this, getString(R.string.pass_no_match) , Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        changePassword.setText(getString(R.string.changing));
        changePassword.setEnabled(false);
        String email = currentUser.getEmail();
        assert email != null;
        mAuth.signInWithEmailAndPassword(email,currentParol)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if (task.isSuccessful()){
                            currentUser.updatePassword(newParol)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Toast.makeText(ChangePasswordActivity.this, getString(R.string.password_changed) , Toast.LENGTH_SHORT).show();
                                                finish();
                                            } else {
                                                Toast.makeText(ChangePasswordActivity.this, getString(R.string.failed_to_update_password) , Toast.LENGTH_SHORT).show();
                                                progressBar.setVisibility(View.GONE);
                                                changePassword.setText(getString(R.string.change));
                                                changePassword.setEnabled(true);
                                            }
                                        }
                                    });
                        }else{
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ChangePasswordActivity.this, getString(R.string.old_pass_incorrect) , Toast.LENGTH_SHORT).show();
                            changePassword.setEnabled(true);
                            changePassword.setText(getString(R.string.change));
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
