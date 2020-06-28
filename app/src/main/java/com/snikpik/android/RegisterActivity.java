package com.snikpik.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.LinkMovementMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ClickableSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.snikpik.android.helper.UrlUtil;
import com.github.ybq.android.spinkit.SpinKitView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;

import static com.snikpik.android.helper.UrlUtil.SHARED_PREFS_NOTIFICATION_KEY;

public class RegisterActivity extends AppCompatActivity {

    private Button registerButton;
    private EditText emailEditText, passwordEditText;
    private SpinKitView spinKitView;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private TextView termsTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar()!=null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_back_button);
        }

        registerButton = findViewById(R.id.button_signup);
        termsTextView = findViewById(R.id.text_view_terms_conditions);
        emailEditText = findViewById(R.id.edit_text_email);
        passwordEditText = findViewById(R.id.edit_text_password);
        spinKitView = findViewById(R.id.progressBar);
        firebaseAuth = FirebaseAuth.getInstance();
        currentUser = firebaseAuth.getCurrentUser();


        setUpTermsofService();

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!isNetworkAvailable()){
                    Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();
                    return;
                }

                final String email = emailEditText.getText().toString().trim();
                final String password = passwordEditText.getText().toString().trim();


                if (TextUtils.isEmpty(email)||TextUtils.isEmpty(password)){
                    Toast.makeText(RegisterActivity.this, getString(R.string.enter_required_field), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (password.length()<6){
                    Toast.makeText(RegisterActivity.this, getString(R.string.must_be_characters), Toast.LENGTH_SHORT).show();
                    return;
                }



                if (currentUser!=null&&currentUser.isAnonymous()){
                    linkWithCredentials(email, password);
                }else{
                    signUpwithEmailandPassword(email, password);
                }
            }
        });
            }

    private void linkWithCredentials(final String email, final String password){
        spinKitView.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.GONE);

        emailEditText.clearFocus();
        passwordEditText.clearFocus();
        hideKeyboard();

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        currentUser.linkWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        spinKitView.setVisibility(View.GONE);
                        if (task.isSuccessful()){
                            if(firebaseAuth.getCurrentUser()!=null) {
                                updateNotificationKeytoDb();
                                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                finish();

                            }

                        }else{
                            registerButton.setVisibility(View.VISIBLE);
                            if (task.getException() instanceof FirebaseAuthUserCollisionException){
                                emailEditText.setError("Email is already in use");
                                emailEditText.requestFocus();
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                emailEditText.setError("Email is not valid");
                                emailEditText.requestFocus();
                            } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                passwordEditText.setError("Password is weak");
                                passwordEditText.requestFocus();
                            }else{
                                Toast.makeText(RegisterActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                });
    }


    private void signUpwithEmailandPassword(final String email,final String password){

        spinKitView.setVisibility(View.VISIBLE);
        registerButton.setVisibility(View.GONE);

        emailEditText.clearFocus();
        passwordEditText.clearFocus();
        hideKeyboard();

        firebaseAuth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        spinKitView.setVisibility(View.GONE);
                        if (task.isSuccessful()){
                            if(firebaseAuth.getCurrentUser()!=null) {
                                updateNotificationKeytoDb();
                                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                                finish();

                            }

                        }else{

                            registerButton.setVisibility(View.VISIBLE);
                            if (task.getException() instanceof FirebaseAuthUserCollisionException){
                                emailEditText.setError("Email is already in use");
                                emailEditText.requestFocus();
                            } else if (task.getException() instanceof FirebaseAuthInvalidCredentialsException){
                                emailEditText.setError("Email is not valid");
                                emailEditText.requestFocus();
                            } else if (task.getException() instanceof FirebaseAuthWeakPasswordException) {
                                passwordEditText.setError("Password is weak");
                                passwordEditText.requestFocus();
                            }else{
                                Toast.makeText(RegisterActivity.this, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show();
                            }

                        }
                    }
                });
    }
    private void updateNotificationKeytoDb() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_NOTIFICATION_KEY, MODE_PRIVATE);
        String notificationKey = prefs.getString("notification_key",null);
        DatabaseReference usersDbRef = FirebaseDatabase.getInstance().getReference("users");
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        usersDbRef.child(user.getUid()).child("notificationKey").setValue(notificationKey);
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

    public void setUpTermsofService(){
        String text = "By signing up, you agree to the Terms of Service and acknowledge the Privacy Policy.";
        SpannableString spannableString = new SpannableString(text);

        ClickableSpan termsOfService = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(getApplicationContext(),WebViewActivity.class);
                intent.putExtra("url", UrlUtil.termOfServiceUrl);
                startActivity(intent);
            }
        };
        spannableString.setSpan(termsOfService, 32,48, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        ClickableSpan privacyPolicy = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                Intent intent = new Intent(getApplicationContext(),WebViewActivity.class);
                intent.putExtra("url", UrlUtil.privacyPolicyUrl);
                startActivity(intent);
            }
        };
        spannableString.setSpan(privacyPolicy, 69,83, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsTextView.setText(spannableString);
        termsTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(RegisterActivity.this, WelcomeActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(RegisterActivity.this, WelcomeActivity.class));
        finish();
    }

    public void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

}
