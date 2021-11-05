package myapp.gemini.duplicates.Algorithms;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import myapp.gemini.duplicates.MainObjects.File;

/*
 *
 * Class that finds all duplicate videos
 *
 */
public class DuplicateVideos extends Duplicate {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        runParallelTask("Videos");
    }

    @Override
    protected void checkDuplicateFiles() {
        final String[] projection = new String[]{
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.DATE_MODIFIED,
                MediaStore.MediaColumns.RELATIVE_PATH,
                MediaStore.MediaColumns.DISPLAY_NAME
        };

        //Get all videos
        try (Cursor cursor = getApplicationContext().getContentResolver().query(
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL),
                projection,
                null,
                null,
                null
        )) {
            //Get all file attributes
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                int size = cursor.getInt(sizeColumn);
                String date = cursor.getString(dateColumn);
                String path = cursor.getString(pathColumn);
                String name = cursor.getString(nameColumn);

                Uri contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL), id);

                //First part of algorithm: group files by size
                filesBySize.put(size, new File(id, contentUri, date, name, size, path));
            }

            //Second part of algorithm
            getDuplicateFileBySmallHash(filesBySize);
            //Last part of algorithm
            getDuplicateFileByFullHash(filesSmallHash);
        }
    }
}