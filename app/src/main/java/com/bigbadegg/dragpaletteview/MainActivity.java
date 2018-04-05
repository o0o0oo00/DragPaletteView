package com.bigbadegg.dragpaletteview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DragPaletteView imageView = findViewById(R.id.dragpv);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.woman);
        imageView.setImageBitmap(bitmap);
    }
}
