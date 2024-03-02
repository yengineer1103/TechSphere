package com.example.fileshare;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fileshare.adapter.MyEncryptionClass;
import com.example.fileshare.adapter.ShowAllReceivedFiles;
import com.example.fileshare.adapter.ShowUserAdapter;
import com.example.fileshare.adapter.UserRegister;
import com.example.fileshare.model.FileShared;
import com.example.fileshare.model.UserModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity {

    public String username = "";
    List<UserModel> userModelList = new ArrayList<>();
    RecyclerView rv_showAllUsers;
    ShowUserAdapter adapter;

    CircleImageView cv_showAllReceivedFiles, cv_fileReceived;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cv_fileReceived = findViewById(R.id.cv_fileReceived);
        cv_showAllReceivedFiles = findViewById(R.id.cv_showAllReceivedFiles);
        rv_showAllUsers = findViewById(R.id.rv_showAllUsers);
        rv_showAllUsers.setHasFixedSize(true);
        rv_showAllUsers.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        Log.d("TAG1", "username: " + username);
        Toast.makeText(MainActivity.this, "username: " + username, Toast.LENGTH_SHORT).show();

        cv_fileReceived.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });
        cv_showAllReceivedFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUserToReceivedFiles();
            }
        });

        loadUser();

    }

    private void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

        final View customLayout = getLayoutInflater().inflate(R.layout.layout_receive_file, null);
        EditText et_fileCode = customLayout.findViewById(R.id.et_fileCode);

        File file = null;

        Button btn_receive = customLayout.findViewById(R.id.btn_receive);

        builder.setView(customLayout);
        AlertDialog alertDialog = builder.create();

        alertDialog.show();

        btn_receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String keyAES = "AES" + username;
                String keyDES = "DES" + username;

                try {
                    MyEncryptionClass.decryptionAES(file, keyAES);
                    MyEncryptionClass.decryptionDES(file, keyDES);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                ProgressDialog dialog = new ProgressDialog(MainActivity.this);
                dialog.setMessage("Decrypting");

                dialog.show();
                String code = et_fileCode.getText().toString();

                if (code.isEmpty()) {
                    et_fileCode.setError("Empty code");
                    return;
                } else {
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser.getUid() != null) {
                        Log.d("TAG1", "onDataChange: reached here");
                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(UserRegister.FILE_SHARED).child(username);
                        reference.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                                    Log.d("TAG1", "onDataChange: reached here too");
                                    FileShared model = dataSnapshot.getValue(FileShared.class);
                                    if (dataSnapshot.getKey().equals(code)) {
                                        DatabaseReference reference1 = FirebaseDatabase.getInstance().getReference().child(UserRegister.FILE_RECEIVED).child(username);
                                        HashMap<String, String> hashMap = new HashMap<>();
                                        hashMap.put("id", model.getId());
                                        hashMap.put("username", model.getUsername());
                                        hashMap.put("sender", model.getSender());
                                        hashMap.put("filename", model.getFilename());
                                        hashMap.put("fileUrl", model.getFileUrl());

                                        reference1.push().setValue(hashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if (task.isSuccessful()) {
                                                    dialog.dismiss();
                                                    sendUserToReceivedFiles();
                                                    Toast.makeText(MainActivity.this, "File Received Successfully", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                dialog.dismiss();
                                Toast.makeText(MainActivity.this, "Wrong Code", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

            }
        });

    }

    private void sendUserToReceivedFiles() {
        Intent intent = new Intent(MainActivity.this, ShowAllReceivedFiles.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    private void loadUser() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser.getUid() != null) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference().child(UserRegister.RIDER_USERS);
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    userModelList.clear();
                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        UserModel model = dataSnapshot.getValue(UserModel.class);
                        if (!model.getId().equals(firebaseUser.getUid())) {
                            userModelList.add(model);
                        } else {
                            username = model.getUsername();
                        }
                    }
                    adapter = new ShowUserAdapter(MainActivity.this, userModelList, username);
                    rv_showAllUsers.setAdapter(adapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

}