package com.blackboxembedded.WunderLINQ.OTAFirmwareUpdate;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FirmwareXMLParser {
    private static final String ns = null;

    private static final String TAG_ROOT = "firmwares";
    private static final String TAG_KEY = "firmware";
    private static final String TAG_NAME = "name";
    private static final String TAG_VERSION = "version";
    private static final String TAG_DATE = "date";
    private static final String TAG_FILE = "file";
    private static final String TAG_SHASUM = "shasum";
    private static final String TAG_DESCRIPTION = "description";

    // We don't use namespaces

    public List<Entry> parse(InputStream in) throws XmlPullParserException, IOException {
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readFeed(parser);
        } finally {
            in.close();
        }
    }

    private List<Entry> readFeed(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Entry> entries = new ArrayList<Entry>();

        parser.require(XmlPullParser.START_TAG, ns, TAG_ROOT);
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String name = parser.getName();
            // Starts by looking for the entry tag
            if (name.equals(TAG_KEY)) {
                entries.add(readEntry(parser));
            } else {
                skip(parser);
            }
        }
        return entries;
    }

    // This class represents a single firmware entry in the XML feed.
    public static class Entry {
        public final String name;
        public final String version;
        public final String date;
        public final String file;
        public final String shasum;
        public final String description;

        private Entry(String name, String version, String date, String file, String shasum, String description) {
            this.name = name;
            this.version = version;
            this.date = date;
            this.file = file;
            this.shasum = shasum;
            this.description = description;
        }
    }

    // Parses the contents of an entry. If it encounters a title, summary, or link tag, hands them
    // off
    // to their respective &quot;read&quot; methods for processing. Otherwise, skips the tag.
    private Entry readEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_KEY);
        String name = null;
        String version = null;
        String date = null;
        String file = null;
        String shasum = null;
        String description = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagname = parser.getName();
            if (tagname.equals(TAG_NAME)) {
                name = readText(parser);
            } else if (tagname.equals(TAG_VERSION)) {
                version = readText(parser);
            } else if (tagname.equals(TAG_DATE)) {
                date = readText(parser);
            } else if (tagname.equals(TAG_FILE)) {
                file = readText(parser);
            } else if (tagname.equals(TAG_SHASUM)) {
                shasum = readText(parser);
            } else if (tagname.equals(TAG_DESCRIPTION)) {
                description = readText(parser);
            } else {
                skip(parser);
            }
        }
        return new Entry(name, version, date, file, shasum, description);
    }

    // Processes name tags in the feed.
    private String readName(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, ns, TAG_NAME);
        String name = readText(parser);
        parser.require(XmlPullParser.END_TAG, ns, TAG_NAME);
        return name;
    }

    // For the tags title and summary, extracts their text values.
    private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
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
