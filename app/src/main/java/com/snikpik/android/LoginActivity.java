package com.snikpik.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_NOTIFICATION_KEY;
import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_USER_SETTINGS;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private Button loginButton;
    private SpinKitView spinKitView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_button);
        }


        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        loginButton = findViewById(R.id.button_login);
        emailEditText = findViewById(R.id.edit_text_email);
        emailEditText.requestFocus();
        passwordEditText = findViewById(R.id.edit_text_password);
        spinKitView = findViewById(R.id.progressBar);


        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isNetworkAvailable()){
                    Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();
                    return;
                }


                final String email = emailEditText.getText().toString().trim();
                final String password = passwordEditText.getText().toString().trim();


                if (TextUtils.isEmpty(email)||TextUtils.isEmpty(password)){
                    Toast.makeText(LoginActivity.this, getString(R.string.enter_required_field), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length()<6){
                    Toast.makeText(LoginActivity.this, getString(R.string.must_be_characters) , Toast.LENGTH_SHORT).show();
                    return;
                }

                spinKitView.setVisibility(View.VISIBLE);
                hideKeyboard();
                emailEditText.clearFocus();
                passwordEditText.clearFocus();
                loginButton.setVisibility(View.GONE);
                mAuth.signInWithEmailAndPassword(email,password)
                        .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                spinKitView.setVisibility(View.GONE);
                                loginButton.setVisibility(View.VISIBLE);
                                if (task.isSuccessful()){
                                    updateNotificationKeytoDb();
                                    startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                    finish();
                                }else{
                                    String errorCode = ((FirebaseAuthException) task.getException()).getErrorCode();

                                    switch (errorCode){
                                        case  "ERROR_WRONG_PASSWORD":
                                               passwordEditText.setError("Password is incorrect");
                                               passwordEditText.requestFocus();
                                               break;

                                        case   "ERROR_USER_NOT_FOUND":
                                               emailEditText.setError("User not found");
                                               emailEditText.requestFocus();
                                               break;

                                        default:
                                            Toast.makeText(LoginActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                                            break;





                                    }
                                }
                            }
                        });
            }
        });
            }

    private void updateNotificationKeytoDb() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NOTIFICATION_KEY, MODE_PRIVATE);
        String notificationKey = prefs.getString("notification_key",null);
        DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference("users");
        usersDbRef.child(mAuth.getCurrentUser().getUid()).child("notificationKey").setValue(notificationKey);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Check if user is signed in (non-null).
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && !currentUser.isAnonymous()) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void showHide(View view) {
        if(view.getId()==R.id.show_hide_button){

            if(passwordEditText.getTransformationMethod().equals(PasswordTransformationMethod.getInstance())){
                ((ImageView)(view)).setImageResource(R.drawable.ic_show_password);

                //Show Password
                passwordEditText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                passwordEditText.setSelection(passwordEditText.getText().length());

            }
            else{
                ((ImageView)(view)).setImageResource(R.drawable.ic_hide_password);

                //Hide Password
                passwordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                passwordEditText.setSelection(passwordEditText.getText().length());

            }
        }
    }

    public void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(LoginActivity.this, WelcomeActivity.class));
        finish();
    }

}