package myapp.gemini.duplicates.MainObjects;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import myapp.example.duplicates.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/*
 *
 * Class that represents a file details
 *
 */

public class FileDetails extends Activity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.file_details);

        //File details
        String fileType = getIntent().getStringExtra("fileType");
        String filePath = getIntent().getStringExtra("filePath");
        String fileSize = getIntent().getStringExtra("fileSize");
        String fileDate = getIntent().getStringExtra("fileDate");

        Uri thumbnailFile = null;
        int thumbnailError = 0;

        switch (fileType) {
            case "Images":
                thumbnailFile = Uri.parse(getIntent().getStringExtra("fileUri"));
                thumbnailError = R.drawable.ic_baseline_image_24;
                break;

            case "Videos":
                thumbnailFile = Uri.parse(getIntent().getStringExtra("fileUri"));
                thumbnailError = R.drawable.ic_baseline_videocam_24;
                break;

            case "Audios":
                thumbnailError = R.drawable.ic_baseline_audiotrack_24;
                break;

            case "Others":
                thumbnailError = R.drawable.ic_baseline_article_24;
                break;
        }

        //File details view
        ImageView fileView = findViewById(R.id.file);
        TextView name = findViewById(R.id.name);
        TextView size = findViewById(R.id.size);
        TextView date = findViewById(R.id.date);

        Glide.with(this).load(thumbnailFile).fitCenter().error(thumbnailError).into(fileView);
        name.setText(filePath);
        size.setText("File size: " + fileSize);
        date.setText("Last modified date: " + new SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a", Locale.ENGLISH).format(new Date(Long.parseLong(fileDate)*1000)));
    }
}
