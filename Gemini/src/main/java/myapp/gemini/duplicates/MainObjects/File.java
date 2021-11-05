package myapp.gemini.duplicates.MainObjects;

import android.net.Uri;

/*
 *
 * Class that represents a file
 *
 */

public class File {

    ///Attributes
    private final long id;
    private final Uri uri;
    private final String date, name, path;
    private final int size;

    ///Constructor
    public File(long id, Uri uri, String date, String name, int size, String path) {
        this.id = id;
        this.uri = uri;
        this.date = date;
        this.name = name;
        this.size = size;
        this.path = path;
    }

    ///Getters
    public long getId() { return id; }

    public Uri getUri() { return uri; }

    public String getDate() { return date; }

    public String getName() { return name; }

    public int getSize() { return size; }

    public String getPath() { return path; }
}
