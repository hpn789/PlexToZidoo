package com.hpn789.plextozidoo;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PlexXmlParser {

    // We don't use namespaces
    private static final String ns = null;
    private List<PlexLibraryInfo> infos = new ArrayList<>();

    private List<String> titles;

    //public List<String> entries = new ArrayList<String>();

    public PlexXmlParser(List<String> aTitles)
    {
        titles=aTitles;
    }

    public List<PlexLibraryInfo> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            infos.clear();
            readXML(parser);
            return infos;
        } finally {
            in.close();
        }
    }

    private void readXML(XmlPullParser parser) throws XmlPullParserException, IOException {

        parser.require(XmlPullParser.START_TAG, ns, "MediaContainer");
        while (parser.next() != XmlPullParser.END_DOCUMENT && infos.size()<titles.size()) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals("Directory")) {
                readDirectory(parser);
            } else {
                skip(parser);
            }
        }
    }

    private void readDirectory(XmlPullParser parser) throws XmlPullParserException, IOException {

        String titleValue = parser.getAttributeValue(null, "title");
        if(titles.contains(titleValue))
        {
            String key = parser.getAttributeValue(null, "key");
            String type = parser.getAttributeValue(null, "type");
            String viewOffsetText = parser.getAttributeValue(null, "viewOffset");
            int viewOffset;
            try
            {
                viewOffset = Integer.parseInt(viewOffsetText);
            }
            catch(NumberFormatException e)
            {
                viewOffset=0;
            }
            for(PlexMediaType mt : PlexMediaType.values())
            {
                if(mt.name.equals(type))
                {
                    infos.add(new PlexLibraryInfo(key, mt, viewOffset));
                }
            }
        }
        //parser.nextTag();
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
