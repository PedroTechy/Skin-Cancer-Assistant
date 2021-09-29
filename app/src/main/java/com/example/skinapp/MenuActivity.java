package com.example.skinapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MenuActivity extends AppCompatActivity {

    private Button newAnalyse, history;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        newAnalyse = findViewById(R.id.newAnalyse);
        history = findViewById(R.id.history);


        newAnalyse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent analyseIntent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(analyseIntent);
            }
        });

        history.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent historyIntent = new Intent(getApplicationContext(), history.class);
                startActivity(historyIntent);
            }
        });

    }




}