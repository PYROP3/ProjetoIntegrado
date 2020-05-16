package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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

import java.io.IOException;

public class CreateAccountActivity extends AppCompatActivity implements Callback {

    private Context mContext;
    private String TAG = Constants.TAG;

    //TODO: Get email, password and name from user and let he choose profile picture
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mContext = getApplicationContext();
        Log.d(TAG, "Create account activity created");
    }

    public void onClickRegisterAccount(View v){
        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();

        if(!customOkHttpClient.sendCreateAccountRequest(mContext, this)){
            Toast.makeText(mContext, "Network not detected"
                    + "\nMake sure you are connected to the internet", Toast.LENGTH_LONG).show();

            Log.d(TAG, "Can't login, network error");
        }else{
            Log.d(TAG, "Start request to server");
        }
    }

    @Override
    public void onResponse(Response response) throws IOException {
        if(response.isSuccessful()){
            Log.d(TAG, "Successfully response");
            Log.d(TAG, "Response: " + response.body().string());
            startActivity(new Intent(mContext, MapsActivity.class));
        }else {
            //TODO: Handle the response and check what is the error
            Log.d(TAG, "Response fail not successful response");
        }
    }

    @Override
    public void onFailure(Request request, IOException e) {

    }
}
