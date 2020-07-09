package org.ngbp.libatsc3.ndk;

import android.util.Xml;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public final class XmlUtils {
    private XmlUtils() {
    }

    public static XmlPullParser newParser(@NonNull String cmlPayload) throws XmlPullParserException {
        XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(new ByteArrayInputStream(cmlPayload.getBytes()), null);

        return parser;
    }

    public static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
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

    public static boolean strToBool(String value) {
        return Boolean.parseBoolean(value);
    }

    public static int strToInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }

        return -1;
    }
}
