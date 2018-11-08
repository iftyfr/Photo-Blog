package com.example.ifty.photoblog;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private FirebaseAuth mAuth;
    FirebaseFirestore firebaseFirestore;
    String user_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth=FirebaseAuth.getInstance();
        firebaseFirestore=FirebaseFirestore.getInstance();
        user_id = mAuth.getCurrentUser().getUid();

        toolbar=findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Photo Blog");


    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser==null){
           sendToLogin();
        }
        firebaseFirestore.collection("Users").document(user_id).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    if (!task.getResult().exists()){
                        Intent intent = new Intent(MainActivity.this,SetupActivity.class);
                        intent.putExtra("intentChecker","first_time");
                        startActivity(intent);
                    }
                }
                else {
                    String error = task.getException().toString();
                    Toast.makeText(MainActivity.this, "RETRIEVE ERROR : "+error, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    public void logOut(MenuItem item) {
        mAuth.signOut();
        sendToLogin();
    }

    private void sendToLogin() {
        Intent intent = new Intent(this,LoginActivity.class);
        finish();
        startActivity(intent);
    }

    public void settingsAcc(MenuItem item) {
        Intent intent = new Intent(this,SetupActivity.class);
        intent.putExtra("intentChecker","not_first_time");
        startActivity(intent);
    }

    public void addNewPost(View view) {
        startActivity(new Intent(this,NewPostActivity.class));
    }
}
