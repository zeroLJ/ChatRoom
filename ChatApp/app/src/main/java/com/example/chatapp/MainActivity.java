package com.example.chatapp;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    private EditText localhost_et;
    private EditText name_et;
    public static String name = "无名";
    public static String host = "192.168.43.155";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        localhost_et = (EditText) findViewById(R.id.localhost_et);
        name_et = (EditText) findViewById(R.id.name_et);
        host = localhost_et.getText().toString();
        localhost_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                host = localhost_et.getText().toString();
            }
        });
        name_et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                name = name_et.getText().toString();
            }
        });
//        new hostReceiveThread().start();
//        new msgReceiveThread().start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        String[] PERMISSIONS = {Manifest.permission.INTERNET, Manifest.permission.READ_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, PERMISSIONS, 1000);
    }

    public void start(View view) {
        sendHost();
        Intent intent = new Intent(MainActivity.this,ClientActivity.class);
        startActivity(intent);
    }

    public void openServer(View view) {
        Intent intent = new Intent(this, ServerActivity.class);
        startActivity(intent);
    }

    void sendHost(){
        Log.i("ljl","sendhost");
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
//                    Socket s = new Socket("192.168.0.111", 12345);
                    Socket s = new Socket(MainActivity.host, 2000);
                    // outgoing stream redirect to socket
                    OutputStream out = s.getOutputStream();
                    // 注意第二个参数据为true将会自动flush，否则需要需要手动操作out.flush()
                    PrintWriter output = new PrintWriter(out, true);
                    output.println(s.getLocalAddress().getHostAddress());
                    Log.i("ljl",s.getLocalAddress().getHostAddress());
                    s.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
