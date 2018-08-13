package com.melonfishy.fruitsee;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;

public class EntryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);

        // Placeholder. Will add AsyncTask later.
        ConstraintLayout screen = (ConstraintLayout) findViewById(R.id.cl_screen);
        Activity self = this;
        screen.setOnTouchListener((v, event) -> {
            Intent toCamera = new Intent(self, CameraActivity.class);
            startActivity(toCamera);
            return true;
        });
    }
}
