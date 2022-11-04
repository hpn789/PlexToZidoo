package com.hpn789.plextozidoo;

public class PlexLibraryInfo {
    private String key;
    private PlexMediaType type;

    public PlexLibraryInfo(String aKey, PlexMediaType aType)
    {
        key=aKey;
        type=aType;
    }

    public PlexMediaType getType() {
        return type;
    }

    public String getKey() {
        return key;
    }
}
