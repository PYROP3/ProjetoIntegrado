package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.street.analyzer.R;
import com.street.analyzer.location.MapsActivity;
import com.street.analyzer.serverCommunication.CustomOkHttpClient;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.SLog;

import java.io.IOException;

public class CreateAccountActivity extends AppCompatActivity implements Callback {

    private Context mContext;
    private String TAG = getClass().getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mContext = getApplicationContext();
        SLog.d(TAG, "Create account activity created");
    }

    public void onClickRegisterAccount(View v){
        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();



        if(!customOkHttpClient.sendCreateAccountRequest(mContext, this)){
            Toast.makeText(mContext, "Network not detected"
                    + "\nMake sure you are connected to the internet", Toast.LENGTH_LONG).show();

            SLog.d(TAG, "Can't login, network error");
        }else{
            SLog.d(TAG, "Start request to server");
        }
    }

    @Override
    public void onResponse(Response response) throws IOException {
        if(response.isSuccessful()){
            SLog.d(TAG, "Successfully response");
            SLog.d(TAG, "Response: " + response.body().string());
            startActivity(new Intent(mContext, MapsActivity.class));
        }else {
            //TODO: Handle the response and check what is the error
            SLog.d(TAG, "Response fail not successful response");
        }
    }

    @Override
    public void onFailure(Request request, IOException e) {

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
}
