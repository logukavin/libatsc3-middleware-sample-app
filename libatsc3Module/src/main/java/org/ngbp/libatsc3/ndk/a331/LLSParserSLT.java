package org.ngbp.libatsc3.ndk.a331;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class LLSParserSLT {

    public ArrayList<Service> parseXML(String myXml) {
        ArrayList<Service> services = new ArrayList<>();
        XmlPullParserFactory parserFactory;
        try {
            parserFactory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = parserFactory.newPullParser();
            //InputStream is = getAssets().open("data.xml");
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(new ByteArrayInputStream(myXml.getBytes()), null);

            processParsing(services, parser);

        } catch (XmlPullParserException | IOException e) {
            Log.e("LLSParserSLT","exception in parsing: "+e);
        }

        return services;
    }

    private void processParsing(ArrayList<Service> services, XmlPullParser parser) throws IOException, XmlPullParserException {
        Service currentService = null;
        BroadcastSvcSignaling broadcastSvcSignaling = null;
        String attrVal;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String eltName = parser.getName();

                if ("Service".equals(eltName)) {
                    currentService = new Service();
                    services.add(currentService);

                    if ((attrVal = parser.getAttributeValue(null, "serviceId")) != null) {
                        currentService.serviceId = Integer.parseInt(attrVal);
                    }
                    if ((attrVal = parser.getAttributeValue(null, "globalServiceID")) != null) {
                        currentService.globalServiceId = attrVal;
                    }
                    if ((attrVal = parser.getAttributeValue(null, "majorChannelNo")) != null) {
                        currentService.majorChannelNo = Integer.parseInt(attrVal);
                    }
                    if ((attrVal = parser.getAttributeValue(null, "minorChannelNo")) != null) {
                        currentService.minorChannelNo = Integer.parseInt(attrVal);
                    }
                    if ((attrVal = parser.getAttributeValue(null, "shortServiceName")) != null) {
                        currentService.shortServiceName = attrVal;
                    }
                } else if ("BroadcastSvcSignaling".equals(eltName)) {
                    if (currentService != null) {
                        broadcastSvcSignaling = new BroadcastSvcSignaling();
                        currentService.broadcastSvcSignalingCollection.add(broadcastSvcSignaling);

                        if ((attrVal = parser.getAttributeValue(null, "slsProtocol")) != null) {
                            broadcastSvcSignaling.slsProtocol = Integer.parseInt(attrVal);
                        }
                        if ((attrVal = parser.getAttributeValue(null, "slsMajorProtocolVersion")) != null) {
                            broadcastSvcSignaling.slsMajorProtocolVersion = Integer.parseInt(attrVal);
                        }
                        if ((attrVal = parser.getAttributeValue(null, "slsMinorProtocolVersion")) != null) {
                            broadcastSvcSignaling.slsMinorProtocolVersion = Integer.parseInt(attrVal);
                        }
                        if ((attrVal = parser.getAttributeValue(null, "slsDestinationIpAddress")) != null) {
                            broadcastSvcSignaling.slsDestinationIpAddress = attrVal;
                        }
                        if ((attrVal = parser.getAttributeValue(null, "slsDestinationUdpPort")) != null) {
                            broadcastSvcSignaling.slsDestinationUdpPort = attrVal;
                        }
                        if ((attrVal = parser.getAttributeValue(null, "slsSourceIpAddress")) != null) {
                            broadcastSvcSignaling.slsSourceIpAddress = attrVal;
                        }
                    }
                }
            }

            eventType = parser.next();
        }

    }
}