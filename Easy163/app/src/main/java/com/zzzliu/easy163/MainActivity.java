package com.zzzliu.easy163;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button bt_start = findViewById(R.id.start);
        bt_start.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Server.getInstance().start();
                }
            }

        );
    }
}
