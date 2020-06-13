package com.street.analyzer.wakeUp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.street.analyzer.R;
import com.street.analyzer.serverCommunication.CustomOkHttpClient;
import com.street.analyzer.utils.Constants;
import com.street.analyzer.utils.JsonParser;
import com.street.analyzer.utils.SLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class CreateAccountActivity extends AppCompatActivity implements Callback {

    private Context mContext;
    private String TAG = getClass().getSimpleName();

    private TextView mTvEmail;
    private TextView mTvName;
    private TextView mTvPassword;
    private TextView mTvConfirmPassword;
    private ImageView mImgPicture;

    //TODO: Use this variable to create a button to remove user photo after he has already inserted one
    private Boolean isImageChanged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        mTvName = findViewById(R.id.txtName);
        mTvEmail = findViewById(R.id.txtEmail);
        mTvPassword = findViewById(R.id.txtPassword);
        mTvConfirmPassword = findViewById(R.id.txtConfirmPassword);

        mImgPicture = (ImageView)findViewById(R.id.profilePicture);
        Bitmap image = BitmapFactory.decodeResource(getResources(), R.drawable.user_icon);
        mImgPicture.setImageBitmap(image);

        isImageChanged = false;

        mContext = getApplicationContext();
        SLog.d(TAG, "Create account activity created");
        loadingBarStatus(false);
    }

    public void onClickRegisterAccount(View v){
        loadingBarStatus(true);

        CustomOkHttpClient customOkHttpClient = new CustomOkHttpClient();

        String email = mTvEmail.getText().toString();
        String name = mTvName.getText().toString();
        String password = mTvPassword.getText().toString();
        String confirmPassword = mTvConfirmPassword.getText().toString();

        if(!isInputValid(email, name, password, confirmPassword)) {
            loadingBarStatus(false);
            return;
        }

        if(!customOkHttpClient.sendCreateAccountRequest(mContext, this, email, name, password)){
            loadingBarStatus(false);
            Toast.makeText(mContext, "Network not detected"
                    + "\nMake sure you are connected to the internet", Toast.LENGTH_LONG).show();

            SLog.d(TAG, "Can't login, network error");
        }else{
            SLog.d(TAG, "Start request to server");
        }
    }

    @Override
    public void onResponse(Response response) throws IOException {
        loadingBarStatus(false);

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
        loadingBarStatus(false);

        //TODO: Handle the Http-Ok Error
        SLog.d(TAG, "Response fail onFailure");
    }

    public void onClickReturn(View v){
        finish();
    }

    public void onClickAddPicture(View v) {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);

        // Find the data location
        File pictureDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String pictureDirectoryPath = pictureDirectory.getPath();
        // Uri representation
        Uri data = Uri.parse(pictureDirectoryPath);

        // Get all image types
        photoPickerIntent.setDataAndType(data, "image/*");

        // Invoke activyResult and getting the photo back
        startActivityForResult(photoPickerIntent, Constants.IMAGE_GALLERY_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == Constants.IMAGE_GALLERY_REQUEST) {
                // The images's address on the SD Card.
                Uri imageUri = data.getData();

                // Stream to read the image data from the SD Card.
                InputStream inputStream;

                // Input stream, based on the URI of the image
                try {
                    inputStream = getContentResolver().openInputStream(imageUri);

                    // Get a bitmap from the stream
                    Bitmap image = BitmapFactory.decodeStream(inputStream);

                    // Show the image to the user
                    mImgPicture.setImageBitmap(image);
                    isImageChanged = true;

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Unable to open image", Toast.LENGTH_LONG).show();
                }

            }
        }
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
            msg.append("Invalid name, your name must have at least " + Constants.MIN_NAME_LENGTH + " characters!\n");
        }

        //Password validation
        if(TextUtils.isEmpty(password) || password.length() < Constants.MIN_PASSWORD_LENGTH){
            valid = false;
            msg.append("Invalid password, your password must have at least " + Constants.MIN_PASSWORD_LENGTH + " characters!\n");
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
        finish();
    }

    private void loadingBarStatus(final Boolean status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loadingPanel).setVisibility((status)?View.VISIBLE:View.GONE);
                mTvName.setEnabled(!status);
                mTvEmail.setEnabled(!status);
                mImgPicture.setEnabled(!status);
                mTvPassword.setEnabled(!status);
                mTvConfirmPassword.setEnabled(!status);
            }
        });
    }
}
