package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.street.analyzer.R;
import com.street.analyzer.serverCommunication.CustomOkHttpClient;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.JsonParser;
import com.street.analyzer.utils.SLog;

import java.io.IOException;

public class CreateAccountActivity extends AppCompatActivity implements Callback {

    private Context mContext;
    private String TAG = getClass().getSimpleName();

    private TextView mTvEmail;
    private TextView mTvName;
    private TextView mTvPassword;
    private TextView mTvConfirmPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mTvEmail = findViewById(R.id.txtEmail);
        mTvName = findViewById(R.id.txtName);
        mTvPassword = findViewById(R.id.txtPassword);
        mTvConfirmPassword = findViewById(R.id.txtConfirmPassword);

        mContext = getApplicationContext();
        SLog.d(TAG, "Create account activity created");
    }

    public void onClickRegisterAccount(View v){
        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();

        String email = mTvEmail.getText().toString();
        String name = mTvName.getText().toString();
        String password = mTvPassword.getText().toString();
        String confirmPassword = mTvConfirmPassword.getText().toString();

        if(!isInputValid(email, name, password, confirmPassword))
            return;

        if(!customOkHttpClient.sendCreateAccountRequest(mContext, this, email, name, password)){
            Toast.makeText(mContext, "Network not detected"
                    + "\nMake sure you are connected to the internet", Toast.LENGTH_LONG).show();

            SLog.d(TAG, "Can't login, network error");
        }else{
            SLog.d(TAG, "Start request to server");
        }
    }

    @Override
    public void onResponse(Response response) throws IOException {
        SLog.d(TAG, "Response received!");
        if(response.isSuccessful()){
            SLog.d(TAG, "HTTP - Ok");
            if(JsonParser.isResponseSuccessful(response.body().string())) {
                SLog.d(TAG, "Successfully response");
                notifyUser();
            }else {
                //TODO: Handle server response code error
                SLog.d(TAG, "Server send response WTF");
            }
        }else {
            //TODO: Handle the Http-Ok Error
            SLog.d(TAG, "Response fail not successful response");
        }
    }

    @Override
    public void onFailure(Request request, IOException e) {
        //TODO: Handle the Http-Ok Error
        SLog.d(TAG, "Response fail onFailure");
    }

    public void onClickReturn(View v){
        finish();
    }

    public void onClickAddPicture(View v){
        //TODO: Get a picture from the user gallery and save it

        // PLACE HOLDER (IT SHOULD BE REMOVED ANYTIME SOON)
        AlertDialog.Builder dlgAlert  = new AlertDialog.Builder(this);
        dlgAlert.setMessage("This option is not available yet. Please, try again later.");
        dlgAlert.setTitle("Warning");
        dlgAlert.setPositiveButton(Html.fromHtml("<font color='#9BDE7A'>OK</font>"), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int arg1) {

            }
        });
        dlgAlert.setCancelable(true);
        dlgAlert.create().show();
    }

    private boolean isInputValid(String email, String name, String password, String confirmPassword){
        StringBuilder msg = new StringBuilder();
        boolean valid = true;

        //Email validation
        if(TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            valid = false;
            msg.append("Invalid email!\n");
        }

        //Name validation
        if(TextUtils.isEmpty(name) || name.length() < Constants.MIN_NAME_LENGTH){
            valid = false;
            msg.append("Invalid name, your name must have more than " + Constants.MIN_NAME_LENGTH + " characters!\n");
        }

        //Password validation
        if(TextUtils.isEmpty(password) || password.length() < Constants.MIN_PASSWORD_LENGTH){
            valid = false;
            msg.append("Invalid password, your password must have more than " + Constants.MIN_PASSWORD_LENGTH + " characters!\n");
        }else{
            if(!password.equals(confirmPassword)){
                valid = false;
                msg.append("Passwords don't match!");
            }
        }

        if(!valid) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(this);
            dlgAlert.setTitle("Input not valid!");
            dlgAlert.setMessage(msg);
            dlgAlert.setPositiveButton(Html.fromHtml("<font color='#9BDE7A'>OK</font>"), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int arg1) {

                }
            });
            dlgAlert.setCancelable(true);
            dlgAlert.create().show();
        }
        return valid;
    }

    private void notifyUser(){
        Intent intent = new Intent(mContext, LoginActivity.class);
        intent.putExtra(Constants.EXTRA_CREATE_ACCOUNT, 0);
        startActivity(intent);
    }
}
