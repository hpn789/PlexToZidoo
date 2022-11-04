package com.hpn789.plextozidoo;

public class PlexLibraryInfo {
    private String key;
    private PlexMediaType type;
    private int viewOffset;

    public PlexLibraryInfo(String aKey, PlexMediaType aType, int aViewOffset)
    {
        key=aKey;
        type=aType;
        viewOffset=aViewOffset;
    }

    public PlexMediaType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }

    public int getViewOffset() {
        return viewOffset;
    }
}
