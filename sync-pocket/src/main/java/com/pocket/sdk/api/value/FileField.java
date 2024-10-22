package com.pocket.sdk.api.value;

import java.io.File;

/**
 *
 */
public class FileField {

    /** The full absolute file path */
    public final String path;

    public FileField(File file) {
        this(file.getAbsolutePath());
    }
    
    public FileField(String path) {
        this.path = path;
    }
    
    public File asFile() {
        return new File(path);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        FileField fileField = (FileField) o;
    
        return path != null ? path.equals(fileField.path) : fileField.path == null;
    }
    
    @Override
    public int hashCode() {
        return path != null ? path.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return path;
    }
}
