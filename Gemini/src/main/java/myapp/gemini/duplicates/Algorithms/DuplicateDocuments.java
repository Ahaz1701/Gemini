package myapp.gemini.duplicates.Algorithms;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import myapp.gemini.duplicates.MainObjects.File;
import myapp.example.duplicates.R;

/*
 *
 * Class that finds all duplicate non media files
 *
 */

public class DuplicateDocuments extends Duplicate {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        runParallelTask("Others");
    }

    @Override
    protected void checkDuplicateFiles() {
        final String[] projection = new String[]{
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                MediaStore.Files.FileColumns.RELATIVE_PATH,
                MediaStore.Files.FileColumns.DISPLAY_NAME
        };

        //Mime type of non media files
        String selectionArgs = "application/pdf";
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + " IN('" + selectionArgs + "')" + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'application/vnd%'" + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'application/x%'" + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'application/octet-stream'" + " OR " + MediaStore.Files.FileColumns.MIME_TYPE + " LIKE 'text/%'";

        //Get all non media files
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                projection,
                selection,
                null,
                null
        )) {
            //Get all file attributes
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                int size = cursor.getInt(sizeColumn);
                String date = cursor.getString(dateColumn);
                String path = cursor.getString(pathColumn);
                String name = cursor.getString(nameColumn);

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), id);

                //First part of algorithm: group files by size
                filesBySize.put(size, new File(id, contentUri, date, name, size, path));
            }

            //Second part of algorithm
            getDuplicateFileBySmallHash(filesBySize);
            //Last part of algorithm
            getDuplicateFileByFullHash(filesSmallHash);
        }
    }

    /*
     *
     * Cannot delete non media files with MediaStore API
     * Use of contentResolver to delete files
     * Displaying an alert box
     *
     */
    @Override
    protected void removeSelectedFiles() {
        String nbFiles = currentSelectedFiles.size()>1? currentSelectedFiles.size() + " files":"this file";

        @SuppressLint("UseCompatLoadingForDrawables") AlertDialog dialog = new AlertDialog.Builder(DuplicateDocuments.this)
                .setTitle("Allow Gemini to delete " + nbFiles)
                .setCancelable(false)
                .setIcon(getDrawable(R.drawable.ic_baseline_article_24))
                .setPositiveButton("Allow", (positive, which) -> {
                    for(Uri uri : currentSelectedFiles)
                        getContentResolver().delete(uri, null, null);

                    clearVariables();
                    positive.dismiss();
                })
                .setNegativeButton("Deny", (negative, which) -> negative.dismiss())
                .create();

        dialog.show();
    }
}