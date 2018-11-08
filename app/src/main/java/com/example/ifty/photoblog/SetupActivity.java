package com.example.ifty.photoblog;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.constraintlayout.widget.Constraints;
import androidx.core.app.ActivityCompat;
import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private static final int IMAGE_CHOOSE_CODE = 1;
    private Uri mainImageUri=null;
    private CircleImageView userImage;
    private EditText userName;
    private Button accSaveBtn;
    private StorageReference mStorageRef;
    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private ProgressBar setupProgressBar;
    private Uri defaultUri;
    private String user_id;
    private Boolean isChanged;
    private Uri notChangedUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        String intentChecker=getIntent().getStringExtra("intentChecker");
        if (intentChecker.equals("first_time")){
            isChanged=true;
        }
        else if (intentChecker.equals("not_first_time")){
            isChanged=false;
        }

        userImage=findViewById(R.id.setup_user_image);
        userName=findViewById(R.id.user_name);
        accSaveBtn=findViewById(R.id.setup_btn);
        setupProgressBar=findViewById(R.id.setupProgressBar);

        setupProgressBar.setVisibility(View.VISIBLE);
        accSaveBtn.setEnabled(false);

        defaultUri = Uri.parse("android.resource://com.example.ifty.photoblog/drawable/default_user_image");
        try {
            InputStream stream = getContentResolver().openInputStream(defaultUri);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        mAuth=FirebaseAuth.getInstance();
        firebaseFirestore=FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        user_id=mAuth.getCurrentUser().getUid();

        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    if (task.getResult().exists()){
                        Toast.makeText(SetupActivity.this, "exist", Toast.LENGTH_SHORT).show();

                        String name = task.getResult().getString("name");
                        String image = task.getResult().getString("image");
                        notChangedUri=Uri.parse(image);

                        userName.setText(name);

                        RequestOptions placeHolderRequest = new RequestOptions();
                        placeHolderRequest.placeholder(R.drawable.default_user_image);

                        Glide.with(SetupActivity.this).applyDefaultRequestOptions(placeHolderRequest).load(image).into(userImage);
                    }
                    else {
                        Toast.makeText(SetupActivity.this, "don't exist", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    String error = task.getException().toString();
                    Toast.makeText(SetupActivity.this, "RETRIEVE ERROR : "+error, Toast.LENGTH_SHORT).show();
                }
                setupProgressBar.setVisibility(View.INVISIBLE);
                accSaveBtn.setEnabled(true);
            }
        });
    }

    public void deployImsge(View view) {
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if (ActivityCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
               bringImage();
            }
            else {
                requestForPermission(Manifest.permission.READ_EXTERNAL_STORAGE,"Storage",IMAGE_CHOOSE_CODE);
            }
        }
        else {
            bringImage();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==IMAGE_CHOOSE_CODE){
            if (grantResults.length>0 && grantResults[0] ==PackageManager.PERMISSION_GRANTED){
                bringImage();
            }
            else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void requestForPermission(final String permissionType, String permissionText, final int REQUEST_CODE) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,permissionType)){
            new AlertDialog.Builder(this)
                    .setTitle("Permission Needed")
                    .setMessage("To make a "+permissionText+" this permission is needed.")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(SetupActivity.this,new String[] {permissionType},REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }
        else {
            ActivityCompat.requestPermissions(this,new String[] {permissionType},REQUEST_CODE);
        }
    }

    private void bringImage() {
        userImage.getScaleType();
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(SetupActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                mainImageUri = result.getUri();

                isChanged=true;

              /*  int[] location = new int[2];
                userImage.getLocationInWindow(location);

                int left = location[0];
                int right = location[1];*/
             //   userImage.setLayoutParams(new Constraints.LayoutParams(userImage.getWidth(), userImage.getHeight()));



              /*  ConstraintSet cs = new ConstraintSet();
                ConstraintLayout layout = (ConstraintLayout) findViewById(R.id.setupActivity);
                cs.clone(layout);
                cs.setVerticalBias(R.id.setup_user_image, 0.25f);
                cs.applyTo(layout);*/


                /*Constraints.LayoutParams layoutParams=new Constraints.LayoutParams(userImage.getWidth(), userImage.getHeight());
                layoutParams.setMargins(40,40, 40, 0);
                userImage.setLayoutParams(layoutParams);*/


                ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) userImage.getLayoutParams();
                params.horizontalBias=1;
                params.width=userImage.getWidth();
                params.height=userImage.getHeight();
                params.topToBottom=R.id.account_setup_toolbar;
                params.topMargin=110;
                userImage.requestLayout();



     /*           ConstraintSet constraintSet = new ConstraintSet();
                constraintSet.clone(constraintLayout);
                constraintSet.connect(R.id.imageView,ConstraintSet.RIGHT,R.id.check_answer2,ConstraintSet.RIGHT,0);
                constraintSet.connect(R.id.imageView,ConstraintSet.TOP,R.id.check_answer2,ConstraintSet.TOP,0);
                constraintSet.applyTo(constraintLayout);*/



                userImage.setImageURI(mainImageUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    public void accSettingsSave(View view) {
        final String user_name = userName.getText().toString();
        if (!TextUtils.isEmpty(user_name)){
            if (isChanged){
                setupProgressBar.setVisibility(View.VISIBLE);
                user_id = mAuth.getCurrentUser().getUid();
                final StorageReference photo_path= mStorageRef.child("profile_image").child(user_id+".jpg");
                if (mainImageUri==null){
                    mainImageUri=defaultUri;
                }

                photo_path.putFile(mainImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        if (taskSnapshot.getTask().isSuccessful()){
                            //When the image has successfully uploaded, get its download URL
                            photo_path.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    Uri dlUri = uri;
                                    storeToFirestore(dlUri,user_name);
                                }
                            });
                        }
                        else {
                            String error = taskSnapshot.getTask().getException().toString();
                            Toast.makeText(SetupActivity.this, "IMAGE ERROR : "+error, Toast.LENGTH_SHORT).show();
                            setupProgressBar.setVisibility(View.INVISIBLE);
                        }

                    }
                });
            }
            else {
                storeToFirestore(notChangedUri,user_name);
            }

        }
        else {
            Toast.makeText(this, "User name is required.", Toast.LENGTH_SHORT).show();
        }
    }

    private void storeToFirestore(Uri dlUri,String user_name) {
        Map<String, String> userMap = new HashMap<>();
        userMap.put("name",user_name);
        userMap.put("image",dlUri.toString());

        firebaseFirestore.collection("Users").document(user_id).set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(Task<Void> task) {
                if (task.isSuccessful()){
                    Toast.makeText(SetupActivity.this, "Settings Updated Successfully!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SetupActivity.this,MainActivity.class));
                    finish();
                }
                else {
                    String error = task.getException().toString();
                    Toast.makeText(SetupActivity.this, "FireStore ERROR : "+error, Toast.LENGTH_SHORT).show();
                    setupProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

}
