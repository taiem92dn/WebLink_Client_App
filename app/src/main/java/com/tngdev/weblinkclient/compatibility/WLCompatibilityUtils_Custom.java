/****************************************************************************
 *
 * @file WLCompatibilityUtils_Custom.java
 * @brief
 *
 * WLCompatibilityUtils_Custom class implementation.
 *
 * @author Abalta Technologies, Inc.
 * @date March, 2014
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
package com.tngdev.weblinkclient.compatibility;

import android.os.Build;

import com.abaltatech.weblinkclient.compatibility.CodecInfo;

import java.util.HashMap;
import java.util.Locale;


/**
 * Helper class that returns specific {@link CodecInfo} for the current device. If there
 * is no device specific information, the default CodecInfo is returned.   
 * 
 */
public class WLCompatibilityUtils_Custom
{
    private static final String                     m_hardwareID;
    private static final HashMap<String, CodecInfo> m_deviceCodecInfo;
    private static final CodecInfo					m_defaultCodecInfo = new CodecInfo();
    
    static {
        String board        = Build.BOARD.toLowerCase(Locale.US);
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.US);
        String hardware     = Build.HARDWARE.toLowerCase(Locale.US);
        m_hardwareID = board + "@" + manufacturer + "@" + hardware;
        //---
        
        // Init the device specific codec info objects.
        HashMap<String, CodecInfo> deviceCodecInfo = new HashMap<String, CodecInfo>();
        /**
         * Examples
         */
        deviceCodecInfo.put("smdk4x12@samsung@smdk4x12", new CodecInfo("OMX.SEC.avc.dec"));              // Samsung Galaxy S3
        
        deviceCodecInfo.put("flo@asus@flo",              new CodecInfo("OMX.qcom.video.decoder.avc"));   // Nexus 7, 2013
        
        deviceCodecInfo.put("holiday@htc@holiday",       new CodecInfo("OMX.qcom.video.decoder.avc"));   // HTC Vivid
        
        /**
         * Example
         */
        deviceCodecInfo.put("unknown@atc@autochipsac83xx", new CodecInfo("OMX.mtk.video.decoder.avc",null,1000,1000));

        deviceCodecInfo.put("unknown@alps@mt6735", new CodecInfo("OMX.MTK.VIDEO.DECODER.AVC",null,1000,1000));

        //deviceCodecInfo.put("unknown@atc@autochipsac83xx", new CodecInfo("OMX.mtk.video.decoder.avc"));
        //deviceCodecInfo.put("unknown@atc@autochipsac83xx", new CodecInfo("OMX.mtkwfd.video.decoder.avc"));
        //deviceCodecInfo.put("unknown@atc@autochipsac83xx", new CodecInfo("OMX.google.h264.decoder", 1024*1024*128, 0));
        
        m_deviceCodecInfo = deviceCodecInfo;
        
    }
    
    /**
     * Returns a string that uniquely identifies the current hardware. It's a combination of board, 
     * manufacturer and hardware identifiers returned by Android.
     * 
     * @return The hadrware ID string
     */
    public static String getHardwareID() {
        return m_hardwareID;
    }
    
   
    /**
     * Returns the {@link CodecInfo} that is best suitable for the current hardware.
     * 
     * @return CodecInfo object.
     */
    public static CodecInfo getCodecInfo() {
    	if (m_deviceCodecInfo.containsKey( m_hardwareID )) {
    		// There is specific codec information for the current device, use it.
    		return m_deviceCodecInfo.get(m_hardwareID);
    	}
    	return m_defaultCodecInfo; // Use the default codec.
    }
}
