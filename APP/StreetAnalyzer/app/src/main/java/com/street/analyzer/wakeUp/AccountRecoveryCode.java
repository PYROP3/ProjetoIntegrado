package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.street.analyzer.R;

public class AccountRecoveryCode extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_recovery_code);
    }

    public void onClickReturn(View v){
        finish();
    }
}
