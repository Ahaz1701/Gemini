package myapp.gemini.duplicates;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import myapp.example.duplicates.BuildConfig;
import myapp.gemini.duplicates.Algorithms.DuplicateAudios;
import myapp.gemini.duplicates.Algorithms.DuplicateDocuments;
import myapp.gemini.duplicates.Algorithms.DuplicateImages;
import myapp.gemini.duplicates.Algorithms.DuplicateVideos;
import myapp.example.duplicates.R;

/*
 *
 *  Class that shows the different categories of files
 *
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //Attributes
    private static final Uri foo = Uri.parse("package:" + BuildConfig.APPLICATION_ID);

    private TextView title1, title2;
    private CardView images, videos, audios, documents;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Home interface
        title1 = findViewById(R.id.title1);
        title2 = findViewById(R.id.title2);
        images = findViewById(R.id.duplicateImages);
        videos = findViewById(R.id.duplicateVideos);
        audios = findViewById(R.id.duplicateAudios);
        documents = findViewById(R.id.duplicateFiles);

        dynamicView();
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        //Permission to access all files on external storage
        if(!Environment.isExternalStorageManager())
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, foo));

        else {
            //Different categories of files
            switch (v.getId()) {
                case R.id.duplicateImages:
                    startActivity(new Intent(this, DuplicateImages.class));
                    break;

                case R.id.duplicateVideos:
                    startActivity(new Intent(this, DuplicateVideos.class));
                    break;

                case R.id.duplicateAudios:
                    startActivity(new Intent(this, DuplicateAudios.class));
                    break;

                case R.id.duplicateFiles:
                    startActivity(new Intent(this, DuplicateDocuments.class));
                    break;
            }
        }
    }

    //Start home animation
    private void dynamicView() {
        final float height = getWindowManager().getCurrentWindowMetrics().getBounds().height();

        title1.setY(height/3);
        title1.animate().translationY(0).setDuration(2000).start();

        title2.setY(-height/3);
        title2.animate().translationY(0).setDuration(2000).withEndAction(() -> {
            //Add event listener to all categories
            images.setOnClickListener(this);
            videos.setOnClickListener(this);
            audios.setOnClickListener(this);
            documents.setOnClickListener(this);
        }).start();

        images.setAlpha(0);
        images.setScaleX(0);
        images.setScaleY(0);
        images.animate().scaleX(1).scaleY(1).alpha(1).setDuration(2000).start();

        videos.setAlpha(0);
        videos.setScaleX(0);
        videos.setScaleY(0);
        videos.animate().scaleX(1).scaleY(1).alpha(1).setDuration(2000).start();

        audios.setAlpha(0);
        audios.setScaleX(0);
        audios.setScaleY(0);
        audios.animate().scaleX(1).scaleY(1).alpha(1).setDuration(2000).start();

        documents.setAlpha(0);
        documents.setScaleX(0);
        documents.setScaleY(0);
        documents.animate().scaleX(1).scaleY(1).alpha(1).setDuration(2000).start();
    }
}