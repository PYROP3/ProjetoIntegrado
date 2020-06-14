package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.street.analyzer.R;

public class WelcomeActivity extends AppCompatActivity {

    private Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        mContext = getApplicationContext();
    }
    public void onClickStartUserActivity(View v){
        startActivity(new Intent(mContext, LoginActivity.class));
    }

    public void onClickRegister(View v){
        startActivity(new Intent(mContext, CreateAccountActivity.class));
    }
}
