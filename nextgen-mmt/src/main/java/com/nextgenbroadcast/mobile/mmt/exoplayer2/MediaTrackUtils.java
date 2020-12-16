package com.nextgenbroadcast.mobile.mmt.exoplayer2;

import android.util.Log;
import android.util.Pair;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.ParserException;
import com.google.android.exoplayer2.extractor.ExtractorOutput;
import com.google.android.exoplayer2.extractor.TrackOutput;
import com.google.android.exoplayer2.util.CodecSpecificDataUtil;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.util.Collections;

public final class MediaTrackUtils {
    public static final String TAG = MediaTrackUtils.class.getSimpleName();

    public MediaTrackUtils() {
    }

    public static TrackOutput createVideoOutput(ExtractorOutput output, int id, int videoType, int videoWidth, int videoHeight, float videoFrameRate, byte[] initData) {
        TrackOutput trackOutput = null;
        if (videoType == Util.getIntegerCodeForString("hev1")) {
            trackOutput = output.track(id, C.TRACK_TYPE_VIDEO);
            trackOutput.format(
                    Format.createVideoSampleFormat(
                            null,
                            MimeTypes.VIDEO_H265,
                            null,
                            Format.NO_VALUE,
                            Format.NO_VALUE,
                            videoWidth,
                            videoHeight,
                            videoFrameRate,
                            Collections.singletonList(initData),
                            null)
            );
        }

        return trackOutput;
    }

    public static TrackOutput createAudioOutput(ExtractorOutput output, int id, int audioType, int audioChannelCount, int audioSampleRate) {
        Format format = null;
        if (audioType == Util.getIntegerCodeForString("mp4a")) {
            //jjustman-2020-11-30 - work in progress
            //MediaFormat audioMediaFormat = CodecAACSpecificData.BuildAACCodecSpecificData(MediaCodecInfo.CodecProfileLevel.AACObjectLC, audioSampleRate, audioChannelCount);
            /* jjustman-2020-11-30 - needs special audio codec specific metadata */
            // 11 b0
            /*
               byte[] audioSpecificConfig = new byte[data.bytesLeft()];
                data.readBytes(audioSpecificConfig, 0, audioSpecificConfig.length);
                Pair<Integer, Integer> audioParams = CodecSpecificDataUtil.parseAacAudioSpecificConfig(
                audioSpecificConfig);
                Format format = Format.createAudioSampleFormat(null, MimeTypes.AUDIO_AAC, null,
                Format.NO_VALUE, Format.NO_VALUE, audioParams.second, audioParams.first,
                Collections.singletonList(audioSpecificConfig), null, 0, null);
             */
            //audioMediaFormat.getByteBuffer("csd-0").array();

            byte[] audioSpecificConfig = CodecSpecificDataUtil.buildAacLcAudioSpecificConfig(48000, 6);
            Pair<Integer, Integer> audioParams;
            try {
                audioParams = CodecSpecificDataUtil.parseAacAudioSpecificConfig(audioSpecificConfig);
            } catch (ParserException e) {
                Log.d(TAG, "Can't parse audio params", e);
                return null;
            }
/*
                        Format.createAudioContainerFormat(
                            null,
                            null,
                            MimeTypes.AUDIO_MP4,
                            MimeTypes.AUDIO_AAC,
                            "mp4a.40.2",
                            null,
                            128000,
                            audioParams.second,
                            audioParams.first,
                            null,
                            C.SELECTION_FLAG_DEFAULT,
                            C.ROLE_FLAG_MAIN,
                            "eng")
*/
            format = Format.createAudioSampleFormat(
                    null,
                    MimeTypes.AUDIO_AAC,
                    "mp4a.40.2",
                    128000,
                    Format.NO_VALUE,
                    audioParams.second,
                    audioParams.first,
                    Collections.singletonList(audioSpecificConfig),
                    null,
                    C.SELECTION_FLAG_DEFAULT,
                    "en");
        } else if (audioType == Util.getIntegerCodeForString("xhe1")) {
            format = Format.createAudioSampleFormat(
                    null,
                    MimeTypes.AUDIO_MP4,
                    null,
                    Format.NO_VALUE,
                    Format.NO_VALUE,
                    audioChannelCount,
                    audioSampleRate,
                    null,
                    null,
                    Format.NO_VALUE,
                    null);
        } else if (audioType == Util.getIntegerCodeForString("ac-4")) {
            format = Format.createAudioSampleFormat(
                    null,
                    MimeTypes.AUDIO_AC4,
                    null,
                    Format.NO_VALUE,
                    Format.NO_VALUE,
                    audioChannelCount,
                    audioSampleRate,
                    null,
                    null,
                    Format.NO_VALUE,
                    null);
        }

        TrackOutput trackOutput = null;
        if (format != null) {
            trackOutput = output.track(id, C.TRACK_TYPE_AUDIO);
            trackOutput.format(format);
        }

        return trackOutput;
    }

    public static TrackOutput createTextOutput(ExtractorOutput output, int id, int textType) {
        TrackOutput trackOutput = null;
        if (textType == Util.getIntegerCodeForString("stpp")) {
            trackOutput = output.track(id, C.TRACK_TYPE_TEXT);
            trackOutput.format(
                    Format.createTextSampleFormat(
                            null,
                            MimeTypes.APPLICATION_TTML,
                            0,
                            null)
            );
        }

        return trackOutput;
    }

}
