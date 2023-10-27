/****************************************************************************
 *
 * @file AudioConfigFileParser.java
 * @brief
 *
 * Defines the AudioConfigFileParser class.
 *
 * @author Abalta Technologies, Inc.
 * @date April, 2017
 *
 * @cond Copyright
 *
 * COPYRIGHT 2017 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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
package com.tngdev.weblinkclient.audio;

import com.abaltatech.weblink.core.audioconfig.AudioFormat;
import com.abaltatech.weblink.core.audioconfig.EAudioCodec;
import com.abaltatech.weblink.core.audioconfig.EAudioOutputType;
import com.abaltatech.weblink.core.audioconfig.WLAudioChannelMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class which parses audio channel configuration from an ini file.
 */
public class AudioConfigFileParser {

    protected final InputStream m_input;
    private final int INI_FILE_PARAMETERS_COUNT = 6;

    List<WLAudioChannelMapping> m_channels;
    Map<String, Map<String, String>> m_data;

    /**
     * Default constructor.
     *
     * @param input InputStream pointing to the configuration file
     */
    public AudioConfigFileParser(InputStream input) {
        m_input = input;
        m_channels = new ArrayList<WLAudioChannelMapping>();
        m_data = new HashMap<String, Map<String, String>>();
    }

    /**
     * Parses the configuration file.
     *
     * If there is an error when parsing, an exception containing the parse error will be thrown.
     * @throws IOException
     */
    public void parse() throws IOException {
        InputStreamReader streamReader = new InputStreamReader(m_input);
        BufferedReader bufferedReader = new BufferedReader(streamReader);
        int lineNum = -1;
        String lastSection = "";

        while(bufferedReader.ready()) {
            String line = bufferedReader.readLine();
            lineNum ++;
            if (line != null && line.length() > 0) {
                String trimmedLine = line.trim();

                if (trimmedLine.startsWith("#")) {
                    // Ignore comments
                    continue;
                } else if (trimmedLine.startsWith("[")) {
                    // We are inside a section
                    if (trimmedLine.endsWith("]")) {
                        String section = line.substring(line.indexOf('[') + 1, line.indexOf(']'));
                        m_data.put(section, new HashMap<String, String>());
                        lastSection = section;
                    } else {
                    }
                } else {
                    String[] tokens = null;
                    if (!trimmedLine.contains("\\=")) {
                        tokens = trimmedLine.split("=");
                        if (tokens.length > 2) {
                            // Error, line contains unescaped equals ( = ) sign
                            throw new IOException("Error");
                        }

                        m_data.get(lastSection).put(tokens[0], tokens[1]);
                    } else {

                    }
                }
            }
        }

        Map<String, String> audioChannelsConfig = m_data.get("AudioChannelsConfig");
        if (audioChannelsConfig != null) {
            for (Map.Entry<String, String> entry : audioChannelsConfig.entrySet()) {
                String [] tokens = entry.getValue().split(",");
                if (tokens.length >= INI_FILE_PARAMETERS_COUNT) {
                    int channelID = Integer.parseInt(tokens[0]);
                    EAudioOutputType outputType = EAudioOutputType.valueOf(tokens[1]);
                    EAudioCodec codec = EAudioCodec.valueOf(tokens[2]);
                    int channelCount = Integer.parseInt(tokens[3]);
                    int sampleRate = Integer.parseInt(tokens[4]);
                    int bitsPerChannel = Integer.parseInt(tokens[5]);
                    AudioFormat format = new AudioFormat(codec, channelCount, bitsPerChannel, sampleRate);

                    List<Integer> audioTypes = new ArrayList<Integer>();
                    for (int i = INI_FILE_PARAMETERS_COUNT; i < tokens.length; i++) {
                        audioTypes.add(Integer.parseInt(tokens[i]));
                    }

                    WLAudioChannelMapping mapping = new WLAudioChannelMapping(channelID, format, outputType, audioTypes);
                    m_channels.add(mapping);
                }
            }
        }
    }

    /**
     * Returns a list of all configuration information that was parsed successfully.
     *
     * This method will always return an empty list until {@link AudioConfigFileParser#parse()} is called
     * at least once.
     *
     * @return List of audio channel configuratiion information
     */
    public List<WLAudioChannelMapping> getChannels() {
        return m_channels;
    }
}
