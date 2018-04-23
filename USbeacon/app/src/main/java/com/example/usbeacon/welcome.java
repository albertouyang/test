package com.example.usbeacon;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class welcome extends AppCompatActivity {

    Button market;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        //
        android.support.v7.app.ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        market = (Button)findViewById(R.id.market);
        market.setOnClickListener(GoInMarket);
    }

    public View.OnClickListener GoInMarket = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(welcome.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    };
}
