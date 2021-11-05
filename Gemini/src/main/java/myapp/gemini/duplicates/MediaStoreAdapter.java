package myapp.gemini.duplicates;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import myapp.gemini.duplicates.MainObjects.File;
import myapp.gemini.duplicates.MainObjects.FileDetails;
import myapp.example.duplicates.R;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.List;

/*
 *
 * RecyclerView Adapter
 * Allow to display the thumbnail of files
 *
 */
public class MediaStoreAdapter extends RecyclerView.Adapter<MediaStoreAdapter.ViewHolder> {

    //Interface that creates an Onclick event
    public interface OnItemCheckListener {
        void onItemCheck();
    }

    //Attributes
    private final String filesType;
    private final List<File> duplicateFiles;
    private final List<File> currentSelectedFiles = new ArrayList<>();
    private long selectedFilesSize = 0;

    private final OnItemCheckListener onItemClick;
    private final Context context;

    //Constructor
    public MediaStoreAdapter(List<File> duplicateFiles, Context context, String filesType, @NonNull OnItemCheckListener onItemClick) {
        this.duplicateFiles = duplicateFiles;
        this.context = context;
        this.onItemClick = onItemClick;
        this.filesType = filesType;
    }

    //Getters
    public long getSelectedFilesSize() {
        return selectedFilesSize;
    }

    public List<Uri> getCurrentSelectedFiles() {
        List<Uri> selectedUriFiles = new ArrayList<>();
        for(File file : currentSelectedFiles)
            selectedUriFiles.add(file.getUri());
        return selectedUriFiles;
    }

    public List<File> getDuplicateFiles() {
        return duplicateFiles;
    }

    //Select all files
    public void selectAllFiles(boolean selectAllFiles) {
        currentSelectedFiles.clear();
        selectedFilesSize = 0;
        if (selectAllFiles) {
            for (File file : duplicateFiles) {
                currentSelectedFiles.add(file);
                selectedFilesSize += file.getSize();
            }
        }
        notifyDataSetChanged();
    }

    //Remove selected files
    public void removeFiles() {
        for (File file : currentSelectedFiles) {
            int position = duplicateFiles.indexOf(file);
            duplicateFiles.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, getItemCount());
        }
        currentSelectedFiles.clear();
        selectedFilesSize = 0;
    }

    //Class View where files are displayed
    public static class ViewHolder extends RecyclerView.ViewHolder {

        //Attributes
        private final ImageView thumbnail;
        private final CheckBox checkBox;
        private final TextView openFully;

        //Constructor
        public ViewHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.thumbnail);
            checkBox = view.findViewById(R.id.checkbox);
            openFully = view.findViewById(R.id.openFully);
        }

        //Onclick event
        public void setOnclickListener(View.OnClickListener onClickListener) {
            itemView.setOnClickListener(onClickListener);
        }
    }

    //Create the View
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file, parent, false);
        return new ViewHolder(view);
    }

    //Manage user actions and view files thumbnails
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final File file = duplicateFiles.get(position);

        holder.checkBox.setChecked(currentSelectedFiles.contains(file));
        holder.checkBox.setText(humanReadableByteCountSI(file.getSize()));

        Uri thumbnailFile = null;
        int thumbnailError = 0;

        //Thumbnail are different depending on the file type
        switch (filesType) {
            case "Images":
                thumbnailFile = file.getUri();
                thumbnailError = R.drawable.ic_baseline_image_24;
                break;

            case "Videos":
                thumbnailFile = file.getUri();
                thumbnailError = R.drawable.ic_baseline_videocam_24;
                break;

            case "Audios":
                thumbnailError = R.drawable.ic_baseline_audiotrack_24;
                break;

            case "Others":
                thumbnailError = R.drawable.ic_baseline_article_24;
                break;
        }

        //Display thumbnail
        Glide.with(context).load(thumbnailFile).centerCrop().error(thumbnailError).into(holder.thumbnail);

        //Onclick events
        holder.checkBox.setOnClickListener(v -> {
            notifyItemChanged(position);
            if(holder.checkBox.isChecked()) {
                currentSelectedFiles.add(file);
                selectedFilesSize += file.getSize();
            }
            else {
                currentSelectedFiles.remove(file);
                selectedFilesSize -= file.getSize();
            }
            onItemClick.onItemCheck();
        });

        holder.setOnclickListener(v -> {
            holder.checkBox.setChecked(!holder.checkBox.isChecked());
            notifyItemChanged(position);
            if(holder.checkBox.isChecked()) {
                currentSelectedFiles.add(file);
                selectedFilesSize += file.getSize();
            }
            else {
                currentSelectedFiles.remove(file);
                selectedFilesSize -= file.getSize();
            }
            onItemClick.onItemCheck();
        });

        holder.openFully.setOnClickListener(v -> {
            Intent intent = new Intent(context, FileDetails.class);
            intent.putExtra("filePath", file.getPath() + file.getName());
            intent.putExtra("fileUri", file.getUri().toString());
            intent.putExtra("fileSize", humanReadableByteCountSI(file.getSize()));
            intent.putExtra("fileDate", file.getDate());
            intent.putExtra("fileType", filesType);
            context.startActivity(intent);
        });
    }

    //Getter
    @Override
    public int getItemCount() {
        return duplicateFiles.size();
    }

    //Convert file size to human readable file size
    @SuppressLint("DefaultLocale")
    private static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000)
            return bytes + " B";

        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
    }
}