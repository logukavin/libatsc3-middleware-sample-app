package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model;

public class CaptionDisplay {
    public String msgType;
    public cta708 cta708;
    public imsc1 imsc1;

    public class cta708 {
        public String characterColor;
        public double characterOpacity;
        public int characterSize;
        public String fontStyle;
        public String backgroundColor;
        public int backgroundOpacity;
        public String characterEdge;
        public String characterEdgeColor;
        public String windowColor;
        public int windowOpacity;
    }

    public class imsc1 {
        public String region_textAlign;
        public String content_fontWeight;
    }
}
