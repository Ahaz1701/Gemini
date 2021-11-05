package myapp.gemini.duplicates.Algorithms;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentSender;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import myapp.gemini.duplicates.MainObjects.File;
import myapp.gemini.duplicates.MediaStoreAdapter;
import myapp.example.duplicates.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

/**
 * Abstract class that finds all duplicate files
 */
public abstract class Duplicate extends Activity implements PopupMenu.OnMenuItemClickListener, View.OnClickListener, SwipeRefreshLayout.OnRefreshListener {

    //Attributes

    //File container
    protected final Multimap<Integer, File> filesBySize = HashMultimap.create();
    protected final Multimap<String, File> filesSmallHash = HashMultimap.create();
    protected final Multimap<String, File> filesFullHash = HashMultimap.create();

    //XML elements
    protected CardView menuBar, removeBar;
    protected RelativeLayout menuView, emptyView, removeView;
    protected TextView mediaFiles, textEmptyView, removeFiles;
    protected CheckBox selectAllFiles;
    protected RecyclerView recyclerView;
    protected MediaStoreAdapter duplicateFilesListAdapter;
    protected SwipeRefreshLayout refreshLayout;
    protected CircularProgressIndicator progressBar;

    protected final int DELETE_REQUEST = 1;
    protected String filesType;

    //Duplicate file container
    protected List<File> duplicateFiles = new ArrayList<>();
    protected List<Uri> currentSelectedFiles;

    //Find all duplicate files
    protected abstract void checkDuplicateFiles();

    //Hashes (SHA-1) the files (part or all) by chunks
    protected String hashFile(File file, boolean firstChunk) {
        StringBuilder hexString = new StringBuilder();
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            ContentResolver resolver = getApplicationContext().getContentResolver();
            BufferedInputStream is = new BufferedInputStream(resolver.openInputStream(file.getUri()));
            DigestInputStream dis = new DigestInputStream(is, md);
            byte[] buffer = new byte[8192];
            if (firstChunk)
                dis.read(buffer, 0, buffer.length);
            else
                while (dis.read(buffer, 0, buffer.length) != -1) ;

            byte[] digest = md.digest();
            for (byte b : digest) hexString.append(Integer.toHexString(0xFF & b));
        } catch (NoSuchAlgorithmException | IOException ignored) {
        }
        return hexString.toString();
    }

    //Group files by "small hash": hash only first 1kB of the file
    protected void getDuplicateFileBySmallHash(Multimap<Integer, File> files) {
        files.keySet().stream().filter(key -> !(files.get(key).size() < 2)).forEachOrdered(key -> {
            files.get(key).forEach(file -> {
                String hashFile = hashFile(file, true);
                filesSmallHash.put(hashFile, file);
            });
        });
    }

    //Group files by "full hash": hash the file fully
    protected void getDuplicateFileByFullHash(Multimap<String, File> files) {
        files.keySet().stream().filter(key -> !(files.get(key).size() < 2)).forEachOrdered(key -> {
            files.get(key).forEach(file -> {
                String hashFile = hashFile(file, false);
                if (filesFullHash.get(hashFile).isEmpty())
                    filesFullHash.put(hashFile, file);

                //Duplicate file container
                else
                    duplicateFiles.add(file);
            });
        });
    }

    //Run two parallel threads: background task and load view
    protected void runParallelTask(String filesType) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        this.filesType = filesType;

        //Load view
        loadingView();

        executorService.execute(() -> {
            //Background task
            checkDuplicateFiles();

            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                buildView(filesType);
            });
        });
    }

    //Load view
    @SuppressLint("SetTextI18n")
    protected void loadingView() {
        setContentView(R.layout.duplicate_files);

        //XML elements view
        removeView = findViewById(R.id.removeView);
        menuView = findViewById(R.id.menuView);
        menuBar = findViewById(R.id.menuBar);
        removeBar = findViewById(R.id.removeBar);
        emptyView = findViewById(R.id.emptyView);
        mediaFiles = findViewById(R.id.mediaFiles);
        selectAllFiles = findViewById(R.id.selectAllFiles);
        recyclerView = findViewById(R.id.recyclerView);
        textEmptyView = findViewById(R.id.textEmptyView);
        removeFiles = findViewById(R.id.removeFiles);
        refreshLayout = findViewById(R.id.refresh);
        progressBar = findViewById(R.id.progressBar);

        emptyView.setVisibility(View.GONE);
        selectAllFiles.setVisibility(View.GONE);
        removeBar.setVisibility(View.GONE);

        final int height = getWindowManager().getCurrentWindowMetrics().getBounds().height();
        progressBar.setIndicatorSize(height / 4);
        progressBar.setTrackThickness(height / 90);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        removeFiles.setEnabled(false);

        //Custom view depending file type
        mediaFiles.setText(filesType);

        int color = 0;

        switch (filesType) {
            case "Images":
                color = Color.rgb(255, 215, 0);
                break;

            case "Videos":
                color = Color.rgb(225, 0, 0);
                break;

            case "Audios":
                color = Color.rgb(0, 187, 0);
                break;

            case "Others":
                filesType = "Files";
                color = Color.rgb(0, 80, 255);
                break;
        }

        textEmptyView.setText(textEmptyView.getText() + " " + filesType.toLowerCase());
        menuView.setBackgroundColor(color);
        removeView.setBackgroundColor(color);
        progressBar.setIndicatorColor(color);
    }

    //Add Onclick event and file thumbnail
    protected void buildView(String filesType) {
        selectAllFiles.setOnClickListener(this);
        removeFiles.setOnClickListener(this);
        refreshLayout.setOnRefreshListener(this);

        duplicateFilesListAdapter = new MediaStoreAdapter(duplicateFiles, this, filesType, () -> {
            updateVariables();
            selectAllFiles.setChecked(currentSelectedFiles.size() == duplicateFiles.size());
        });

        //Update the view depending checkduplicate() function result
        updateView();

        recyclerView.setAdapter(duplicateFilesListAdapter);
    }

    //Onclick event
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            //Select all files
            case R.id.selectAllFiles:
                duplicateFilesListAdapter.selectAllFiles(selectAllFiles.isChecked());
                updateVariables();
                break;

            //Remove selected files
            case R.id.removeFiles:
                try {
                    removeSelectedFiles();
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    //Refresh the activity
    @Override
    public void onRefresh() {
        finish();
        overridePendingTransition(0, 0);
        startActivity(getIntent());
        overridePendingTransition(0, 0);
        refreshLayout.setRefreshing(false);
    }

    //Update the view depending checkduplciate() result
    protected void updateView() {
        if (duplicateFiles.isEmpty()) {
            selectAllFiles.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            removeBar.setVisibility(View.GONE);
        } else {
            selectAllFiles.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            removeBar.setVisibility(View.VISIBLE);
        }
    }

    //Update variables depending checkduplicate() result
    @SuppressLint("SetTextI18n")
    protected void updateVariables() {
        removeFiles.setText(setSizeToRemove(duplicateFilesListAdapter.getSelectedFilesSize()));
        currentSelectedFiles = duplicateFilesListAdapter.getCurrentSelectedFiles();
        selectAllFiles.setText("Selected: " + currentSelectedFiles.size());
        removeFiles.setEnabled(!currentSelectedFiles.isEmpty());
    }

    //Remove selected duplicate files (only media files)
    protected void removeSelectedFiles() throws IntentSender.SendIntentException {
        PendingIntent removePI = MediaStore.createDeleteRequest(getContentResolver(), currentSelectedFiles);
        startIntentSenderForResult(removePI.getIntentSender(), DELETE_REQUEST, null, 0, 0, 0, null);
    }

    //Clear variable after deleting files
    protected void clearVariables() {
        //Show information Toast
        String message = currentSelectedFiles.size() > 1 ? "You deleted " + currentSelectedFiles.size() + " " + filesType.toLowerCase() : "You deleted 1 " + filesType.substring(0, filesType.length() - 1).toLowerCase();
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();

        duplicateFilesListAdapter.removeFiles();
        duplicateFiles = duplicateFilesListAdapter.getDuplicateFiles();
        updateVariables();
        updateView();
    }

    //To find out if the user agrees to delete the duplicate file(s)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent date) {
        if (requestCode == DELETE_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                clearVariables();
            }
        }
    }

    //Menu that displays different categories of files
    public void mediaMenu(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.media_menu);
        popupMenu.show();
    }

    //To change of file categories
    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        finish();

        switch (item.getItemId()) {
            case R.id.Images:
                startActivity(new Intent(this, DuplicateImages.class));
                return true;

            case R.id.Videos:
                startActivity(new Intent(this, DuplicateVideos.class));
                return true;

            case R.id.Audios:
                startActivity(new Intent(this, DuplicateAudios.class));
                return true;

            case R.id.Documents:
                startActivity(new Intent(this, DuplicateDocuments.class));
                return true;

            default:
                return false;
        }
    }

    //Display free space to gain after deletion
    @SuppressLint("DefaultLocale")
    protected static String setSizeToRemove(long bytes) {
        if (bytes == 0)
            return "Remove";

        if (-1000 < bytes && bytes < 1000)
            return "Free " + bytes + " B";

        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return "Free " + String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}