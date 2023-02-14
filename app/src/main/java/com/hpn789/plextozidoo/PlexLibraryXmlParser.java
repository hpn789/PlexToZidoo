package com.hpn789.plextozidoo;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class PlexLibraryXmlParser {

    // We don't use namespaces
    private static final String ns = null;
    private String path = "";
    private String videoKey = "";
    private int duration = 0;

    private final String libraryKey;

    //public List<String> entries = new ArrayList<String>();

    public PlexLibraryXmlParser(String aKey)
    {
        libraryKey = aKey;
    }

    public String parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            readXML(parser);
            return path;
        } finally {
            in.close();
        }
    }

    public String getVideoKey()
    {
        return videoKey;
    }

    public int getDuration()
    {
        return duration;
    }

    private void readXML(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "MediaContainer");
        while (parser.next() != XmlPullParser.END_TAG && path.isEmpty()) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if(name.equals("Video"))
            {
                videoKey = parser.getAttributeValue(null, "ratingKey");

                String durationText = parser.getAttributeValue(null, "duration");
                try
                {
                    duration = Integer.parseInt(durationText);
                }
                catch(NumberFormatException e)
                {
                    duration = 0;
                }

                readVideo(parser);
            }
            else
            {
                skip(parser);
            }
        }
    }

    private void readVideo(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Video");
        while (parser.next() != XmlPullParser.END_TAG && path.isEmpty()) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("Media")) {
                readMedia(parser);
            } else {
                skip(parser);
            }
        }
    }

    private void readMedia(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, "Media");
        while (parser.next() != XmlPullParser.END_TAG && path.isEmpty()) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            if (name.equals("Part")) {
                readPart(parser);
            } else {
                skip(parser);
            }
        }
    }

    // Processes link tags in the feed.
    private void readPart(XmlPullParser parser) throws IOException, XmlPullParserException {
        String keyAttribute = parser.getAttributeValue(null, "key");
        if(keyAttribute.equals(libraryKey))
        {
            path = parser.getAttributeValue(null, "file");
        }
        parser.nextTag();
    }

    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }


}
