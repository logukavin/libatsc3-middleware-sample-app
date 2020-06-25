package org.ngbp.jsonrpc4jtestharness.rpc.receiverQueryApi.model;

public class AudioAccessibilityPref {
    public VideoDescriptionService videoDescriptionService;
    public AudioEIService    audioEIService;
    public class VideoDescriptionService{
        public boolean enabled;
        public String language;
    }
    public class AudioEIService{
        public boolean enabled;
        public String language;
    }
}
