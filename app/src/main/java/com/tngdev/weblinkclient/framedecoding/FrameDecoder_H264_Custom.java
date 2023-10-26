/****************************************************************************
 *
 * @file FrameDecoder_H264_Custom.java
 * @brief
 *
 * Contains the FrameDecoder_H264_Custom class.
 *
 * @author Abalta Technologies, Inc.
 * @date Jan, 2014
 *
 * @cond Copyright
 *
 * COPYRIGHT 2014 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @endcond
 *****************************************************************************/
package com.tngdev.weblinkclient.framedecoding;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Build.VERSION;

import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.weblink.core.DataBuffer;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblinkclient.compatibility.CodecInfo;
import com.abaltatech.weblinkclient.framedecoding.H264Utils;
import com.abaltatech.weblinkclient.framedecoding.IFrameDecoder;
import com.abaltatech.weblinkclient.framedecoding.IFrameDecoderNotification;
import com.abaltatech.weblinkclient.framedecoding.VideoSurface;
import com.tngdev.weblinkclient.compatibility.WLCompatibilityUtils_Custom;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * An example implementation of a Frame Decoder that uses the Android {@link MediaCodec} to decode
 * the raw video.
 * <p></p>
 * Please note that this is only a sample. It is not guaranteed that this sample will work correctly
 * on all platforms.
 * <p></p>
 * Integrators are encouraged to provide their own implementation that is best suited for the
 * platform.
 */
public class FrameDecoder_H264_Custom implements IFrameDecoder {

    private static final String TAG = "FrameDecoder_H264_Custom";

    /**
     * H264 Video Encoding
     */
    private static final String VIDEO_FORMAT = "video/avc";

    /**
     * Sometimes it takes a bit of time for the decoded video to be rendered on the output
     * surface. To compensate for this, delay returning {@code true} from the
     * {@link #isVideoOutGenerated()} method to avoid seeing a black screen. The decoder will wait
     * to have received, decoded and submitted the specified in this constant number of key-frames
     * before setting the {@link #isVideoOutGenerated()}} flag to {@code true}.
     */
    private static final int KEY_FRAME_DELAY = 2;

    private MediaCodec m_decoder;
    private ByteBuffer[] m_inputBuffers;
    private final int m_dequeInputBufferTimeoutUs = 100000; // [100 milliseconds] In micro seconds.
    private final DataBuffer m_configFrameBits = new DataBuffer();
    private FrameDecodeThread m_frameDecodeThread;
    private IFrameDecoderNotification m_notification;
    private boolean m_decoderStarted = false;
    private boolean m_isVideoOutGenerated = false;

    private long m_numKeyFrameInput = 0;
    private long m_numFrameInput = 0;

    /**
     * Utility class to keep available Madia Codec candidates for H264 decoding
     */
    private static class MediaCodecCandidate {
        private final static String TAG = "MediaCodecCandidate";

        // Codec name
        public String name;

        // Has the codec been tested for usability
        private boolean isTested;

        // Is the codec working (no exceptions during start phase,
        // and at least 1 frame decoded by it successfully).
        // Once the codec's isWorking is set to true errors should not change
        // this field as the codec may just throw errors now and then and a single reset
        // should suffice to take care of those, no need to search another suitable codec because
        // of that
        private boolean isWorking;

        public MediaCodecCandidate(String codecName) {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Created a Media Codec Candidate: " + name);
            name = codecName;
            isTested = false;
            isWorking = false;
        }
    }

    /**
     * Holds a list of media codec candidates.
     * The preferred codec chosen should always be at index 0.
     */
    private static List<MediaCodecCandidate> H264_CODEC_CANDIDATES = new ArrayList<MediaCodecCandidate>();

    /**
     * The MediaCodecCandidate we're currently using
     */
    private static MediaCodecCandidate H264_CODEC_CANDIDATE;

    /*
     * This code collects Media Codecs to be used as a decoder.
     * <p></p>
     * Integrators may use this code to select the Media Codecs to try and use for decoding on the
     * try-fail basis or remove it completely, providing the right decoder for their Platform.
     * <p></p>
     * H264_CODEC_CANDIDATE and H264_CODEC_CANDIDATES are used in the {@link #startDeocidng(IFrameDecoderNotification, int, int, VideoSurface)}.
     * Media Codecs from H264_CODEC_CANDIDATES are tested until a working one is found and stored in H264_CODEC_CANDIDATE.
     */
    static {
        Set<String> codecNames = new HashSet<String>();
        for (int i = 0; i < MediaCodecList.getCodecCount(); ++i) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if(codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(VIDEO_FORMAT)) {
                    MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Media Codec available: " + codecInfo.getName());
                    codecNames.add(codecInfo.getName());
                    if (codecInfo.getName().startsWith("OMX.google")) {
                        H264_CODEC_CANDIDATES.add(new MediaCodecCandidate(codecInfo.getName()));
                    } else {
                        H264_CODEC_CANDIDATES.add(0, new MediaCodecCandidate(codecInfo.getName()));
                    }
                }
            }
        }
        // Try with a compatibility preferred codec first!
        String preferredCodec = findPreferredCodec(codecNames);
        if(preferredCodec != null) {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "We have chosen a codec from the compatibility list as preferred: " + preferredCodec);
            H264_CODEC_CANDIDATES.add(new MediaCodecCandidate(preferredCodec));
        }
        Collections.reverse(H264_CODEC_CANDIDATES);
    }

    @Override
    public boolean startDecoding(IFrameDecoderNotification notification, int width, int height, VideoSurface surface) {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "startDecoding enter");

        boolean result = false;

        if (m_decoder != null) {
            MCSLogger.log(MCSLogger.ELogType.eWarning, TAG,"startDecoding: Decoder is already running!");
        } else {
            if (surface != null) {
                if(!surface.getSurface().isValid()) {
                    MCSLogger.log(MCSLogger.ELogType.eError, TAG,"startDecoding: The surface is invalid!");
                } else {
                    try {
                        if(H264_CODEC_CANDIDATE == null || (H264_CODEC_CANDIDATE.isTested || !H264_CODEC_CANDIDATE.isWorking)) {
                            H264_CODEC_CANDIDATE = getNextMediaCodecCandidate();
                        }
                        if(H264_CODEC_CANDIDATE == null) {
                            MCSLogger.log(MCSLogger.ELogType.eError, TAG, "startDecoding:Fatal error: could not find " +
                                    "a suitable Media Codec to decode the H264 stream!");
                        } else {
                            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "startDecoding: Selected Codec is %s", H264_CODEC_CANDIDATE.name);

                            H264_CODEC_CANDIDATE.isTested = true;

                            String board        = Build.BOARD.toLowerCase(Locale.US);
                            String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.US);
                            String hardware     = Build.HARDWARE.toLowerCase(Locale.US);
                            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "startDecoding: Hardware ID is %s", board + "@" + manufacturer + "@" + hardware);

                            MediaFormat fmt;
                            if (VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                fmt = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
                            } else {
                                fmt = MediaFormat.createVideoFormat(VIDEO_FORMAT, width, height);
                            }

                            CodecInfo codecInfo = WLCompatibilityUtils_Custom.getCodecInfo();
                            if (codecInfo.getInputBufferSize() != null) {
                                fmt.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, codecInfo.getInputBufferSize().intValue());
                            }

                            m_decoder = MediaCodec.createByCodecName(H264_CODEC_CANDIDATE.name);
                            m_decoder.configure(fmt, surface.getSurface(), null, 0);
                            m_decoder.start();
                            m_decoderStarted = true;
                            m_notification = notification;

                            m_inputBuffers = m_decoder.getInputBuffers();
                            m_numKeyFrameInput = 0;
                            m_numFrameInput = 0;
                            m_frameDecodeThread = new FrameDecodeThread();
                            m_frameDecodeThread.start();

                            result = true;
                        }
                    } catch(Exception e) {
                        MCSLogger.log(MCSLogger.eError, TAG, "startDecoding: Exception was raised!");
                        MCSLogger.printStackTrace(TAG, e);
                    }
                }
            } else {
                MCSLogger.log(MCSLogger.ELogType.eError, TAG, "startDecoding: Surface is null!");
            }
        }

        if (notification != null && !result) {
            notification.onDecodingStartFailed();
        }

        // onDecodingStarted() is called when the FrameDecodingThread is up and running

        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "startDecoding exit");

        return result;
    }

    @Override
    public void stopDecoding() {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "stopDecoding: enter");
        boolean result = true;

        boolean wasDecoderStarted;
        MediaCodec decoder;
        FrameDecodeThread frameDecodeThread;
        IFrameDecoderNotification notification;

        synchronized (this) {
            decoder = m_decoder;
            wasDecoderStarted = m_decoderStarted;
            notification = m_notification;
            frameDecodeThread = m_frameDecodeThread;
        }

        if (frameDecodeThread != null && frameDecodeThread.isAlive()) {
            frameDecodeThread.interrupt();
            try {
                frameDecodeThread.join();
            } catch (InterruptedException ex) {
                MCSLogger.log(MCSLogger.ELogType.eError, TAG, "stopDecoding: Exception raised while trying to join the decoder thread: ", ex);
                result = false;
            }
        }
        m_frameDecodeThread = null;

        reset();

        if (decoder != null) {
            try {
                if (wasDecoderStarted) {
                    decoder.flush();
                    decoder.stop();
                }
            } catch(Exception ex) {
                MCSLogger.log(MCSLogger.ELogType.eError, TAG, "stopDecoding: Exception raised while trying to stop the decoder: ", ex);
                if (VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        MCSLogger.log(MCSLogger.ELogType.eError, TAG, "stopDecoding: Trying to reset the decoder.");
                        decoder.reset();
                    } catch (Exception e) {
                        MCSLogger.log(MCSLogger.ELogType.eError, TAG, "stopDecoding: Exception raised while trying to reset the decoder: ", e);
                    }
                }
            }
            try {
                decoder.release();
            } catch (Exception e) {
                result = false;
            }
        }
        m_decoder = null;

        m_decoderStarted = false;

        if (notification != null) {
            if (!result) {
                notification.onDecodingStopFailed();
            } else {
                notification.onDecodingStopped();
            }
        }

        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "stopDecoding: exit");
    }

    @Override
    public void reset() {
        m_isVideoOutGenerated = false;
        m_inputBuffers = null;
        m_numKeyFrameInput = 0;
        m_numFrameInput = 0;
        m_configFrameBits.resize(0);
        m_configFrameBits.reset();
    }

    @Override
    public boolean decodeImage(DataBuffer frameBits) {
        int h264FrameType = H264Utils.getFrameType(frameBits);

        // Store the configuration SPS and PPS data
        if (H264Utils.isConfigFrame(h264FrameType)) {
            m_configFrameBits.addBytes(frameBits.getData(), frameBits.getPos(), frameBits.getSize());
        }

        // Count the number of frames and key-frames (for debugging)
        boolean isKeyFrame = H264Utils.isKeyFrame(h264FrameType);
        if (isKeyFrame) {
            if(m_numKeyFrameInput == 0) {
                MCSLogger.log("Received first key frame.");
            }
            ++m_numKeyFrameInput;
        }
        ++m_numFrameInput;

        // Decode the raw video data
        return decodeFrame(frameBits);
    }

    @Override
    public DataBuffer getConfigFrameBits(){
        return m_configFrameBits;
    }

    /**
     * Return the number of frames that were fed into this decoder.
     * <p></p>
     * Used for debugging purposes.
     * <p></p>
     * @return Total number of frames that were fed into the decoder
     */
    public long getFrameInputCount(){
        return m_numFrameInput;
    }

    /**
     * Return the number of key frames that were fed into this decoder.
     * <p></p>
     * Used for debugging purposes.
     * <p></p>
     * @return Total number of key frames that were fed into the decoder
     */
    public long getKeyFrameInputCount() {
        return m_numKeyFrameInput;
    }


    private boolean decodeFrame(DataBuffer frameBits) {
        boolean result = false;
        try {
            while (!result && m_decoder != null) {
                int inputBufferIndex = m_decoder.dequeueInputBuffer(m_dequeInputBufferTimeoutUs);
                if (inputBufferIndex >= 0) {
                    ByteBuffer buffer = m_inputBuffers[inputBufferIndex];
                    int        size   = frameBits.getSize();
                    buffer.clear();
                    buffer.put(frameBits.getData(), frameBits.getPos(), size);
                    m_decoder.queueInputBuffer(inputBufferIndex, 0, size, 0, 0);
                    result = true;
                }
            }
        } catch(Exception e) {
            MCSLogger.log(MCSLogger.eDebug, TAG, "decodeFrame: Failed with exception!");
            MCSLogger.printStackTrace(TAG, e);

            // Do not notify of error if we already killed the decoder
            if(m_decoder != null) {
                m_notification.onDecodingError();
            }
        }
        return result;
    }

    @Override
    public int getType() {
        return WLTypes.FRAME_ENCODING_H264;
    }

    @Override
    public Bitmap getScreenshot() {
        return null;
    }

    @Override
    public boolean canSkipFrames() {
        // Frame skipping not supported for H264 encoding
        return false;
    }

    @Override
    public boolean isVideoOutGenerated() {
        return m_isVideoOutGenerated;
    }

    @Override
    public void onFrameSkipped(DataBuffer frameBits) {
    }

    /**
     * Helper thread that consumes the output of the Frame Decoder.
     */
    private class FrameDecodeThread extends Thread {

        private long m_startTimestamp; // in milliseconds
        private long m_generateFirstOutputTimeout = 2500; // in milliseconds

        public FrameDecodeThread() {
            setName("FrameDecodeThread");
        }

        @Override
        public void run() {
            MCSLogger.log(MCSLogger.eInfo, TAG, "FrameDecodeThread: START!");
            if(m_notification != null) {
                m_notification.onDecodingStarted();
            }
            m_startTimestamp = System.currentTimeMillis();
            MediaCodec decoder;
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            while (!isInterrupted()) {
                synchronized (FrameDecoder_H264_Custom.this) {
                    decoder = m_decoder;
                }
                if (decoder != null) {
                    int outputBufferIndex;
                    try {
                        outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, m_dequeInputBufferTimeoutUs);
                    } catch (Exception e) {
                        MCSLogger.log(MCSLogger.eError, TAG, "Failed to decode frame!");
                        MCSLogger.printStackTrace(e);
                        if(!interrupted()) {
                            m_notification.onDecodingError();
                        }
                        break;
                    }
                    if(!H264_CODEC_CANDIDATE.isWorking && System.currentTimeMillis() - m_startTimestamp >= m_generateFirstOutputTimeout) {
                        // The codec failed to produce any output in the provided amount of time
                        // so we deduce that it has failed silently -> notify of error and restart
                        // with another codec candidate
                        MCSLogger.log(MCSLogger.ELogType.eError, TAG,
                                String.format("Codec failed to produce output in {%s} milliseconds. Resetting.",
                                        m_generateFirstOutputTimeout));
                        m_notification.onDecodingError();
                        break;
                    }
                    if (outputBufferIndex > 0 && !isInterrupted()) {
                        decoder.releaseOutputBuffer(outputBufferIndex, true);
                        if(!H264_CODEC_CANDIDATE.isWorking) {
                            H264_CODEC_CANDIDATE.isWorking = true;
                            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG,
                                    String.format("Codec produced output in {%s} milliseconds. Chosen as current codec.",
                                            System.currentTimeMillis() - m_startTimestamp));
                        }
                        if (m_numKeyFrameInput >= KEY_FRAME_DELAY) {
                            m_isVideoOutGenerated = true;
                        }
                    }
                }
            }
            MCSLogger.log(MCSLogger.eInfo, TAG, "FrameDecodeThread: FINISH!");
        }
    }

    private MediaCodecCandidate getNextMediaCodecCandidate() {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "getNextMediaCodecCandidate");
        for(MediaCodecCandidate candidate : H264_CODEC_CANDIDATES) {
            if(!candidate.isTested) {
                return candidate;
            }
        }
        // Fallback: If we have tested all available Media Codec options -
        // reset and start over, what else can we do..
        MCSLogger.log(MCSLogger.eWarning, TAG, "Tested all available Media Codecs and none fit up to now. Resetting them.");
        for(MediaCodecCandidate candidate : H264_CODEC_CANDIDATES) {
            candidate.isTested = false;
            candidate.isWorking = false;
        }
        return H264_CODEC_CANDIDATES.get(0);
    }

    /**
     * Helper method that picks the preferred deocder based on the list of avaialable codecs.
     * @param codecNames
     * @return
     */
    static private String findPreferredCodec(Set<String> codecNames) {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "findPreferredCodec");
        CodecInfo codecInfo = WLCompatibilityUtils_Custom.getCodecInfo();
        String preferredCodec = codecInfo.getPreferredCodecName();
        if (preferredCodec != null) {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Found preferred codec name: " + preferredCodec);
            if(codecNames.contains(codecInfo.getPreferredCodecName())) {
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Codec name is part of the available codec names: " + preferredCodec);
                  return preferredCodec;
            }
        }
        return null;
    }
}
