package com.syscode.attendance;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void invigilatorRoute(View view)
    {
        Intent intent = new Intent(MainActivity.this, invigilator.class);
        startActivity(intent);
    }


    public void adminRoute(View view)
    {
        Intent intent = new Intent(MainActivity.this, AdminLogin.class);
        startActivity(intent);
    }


}
