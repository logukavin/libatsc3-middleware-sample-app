package org.ngbp.libatsc3.ndk.entities.held;

import android.util.Log;

import androidx.annotation.Nullable;

import org.ngbp.libatsc3.ndk.XmlUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class HeldXmlParser {
    private static final String ENTRY_HELD = "HTMLEntryPackage";

    @Nullable
    public Held parseXML(String xmlPayload) {
        try {
            XmlPullParser parser = XmlUtils.newParser(xmlPayload);

            parser.nextTag();
            parser.require(XmlPullParser.START_TAG, null, "HELD");
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                String name = parser.getName();
                if (name.equals(ENTRY_HELD)) {
                    return readHeld(parser);
                } else {
                    XmlUtils.skip(parser);
                }
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e("HeldXmlParser","exception in parsing: "+e);
        }

        return null;
    }

    private Held readHeld(XmlPullParser parser) throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, ENTRY_HELD);

        Held held = new Held();

        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            String attrName = parser.getAttributeName(i);
            switch (attrName) {
                case "appContextId":
                    held.appContextId = parser.getAttributeValue(i);
                    break;
                case "appRendering":
                    held.appRendering = XmlUtils.strToBool(parser.getAttributeValue(i));
                    break;
                case "bcastEntryPackageUrl":
                    held.bcastEntryPackageUrl = parser.getAttributeValue(i);
                    break;
                case "bcastEntryPageUrl":
                    held.bcastEntryPageUrl = parser.getAttributeValue(i);
                    break;
                case "coupledServices":
                    held.coupledServices = XmlUtils.strToInt(parser.getAttributeValue(i));
                    break;
                default:
                    break;
            }
        }

        return held;
    }
}
