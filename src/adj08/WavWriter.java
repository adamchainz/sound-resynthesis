package adj08;


import java.io.*;
import javax.sound.sampled.*;

/*

<This Java Class is modified from one of the jMusic API version 1.5, March 2004.>

Copyright (C) 2000 Andrew Sorensen & Andrew Brown

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or any
later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

*/

public class WavWriter {

    public WavWriter(float[] sampleData, String fileName) {
        this(sampleData, fileName, 2, 44100, 16);
    }

    public WavWriter(float[] sampleData, String fileName, int channels,
                        int sampleRate, int sampleSizeInBits) {
    	// Properties
        int duration = sampleData.length;
        int sampleSize = sampleSizeInBits / 8;
        AudioFileFormat.Type fileType = null;
        boolean bigEndian;

        // Choose file type from file name
        if (fileName.endsWith(".au")) {
            fileType = AudioFileFormat.Type.AU;
            bigEndian = true;
        } else if (fileName.endsWith(".wav")) {
            fileType = AudioFileFormat.Type.WAVE;
            bigEndian = false;
        } else if (fileName.endsWith(".aif") || fileName.endsWith(".aiff")) {
            fileType = AudioFileFormat.Type.AIFF;
            bigEndian = true;
        } else { // default
            fileName = fileName + ".au";
            bigEndian = true;
        }

        // Prepare for output
        File file = new File(fileName);

        // Convert floats to bytes
        byte[] tmp = new byte[sampleData.length * sampleSize];
        for(int i=0; i<sampleData.length; i++) {
            int ival = -1;
            switch(sampleSize) {
                case 1: // 8 bit
                    tmp[i] = new Float(sampleData[i] * (float)Byte.MAX_VALUE).byteValue();
                    break;
                case 2: // 16 bit
                    short sval = new Float(sampleData[i] * (float)Short.MAX_VALUE).shortValue();
                    if(bigEndian) {
                        tmp[i*2] = (byte) ((sval & 0x0000ff00) >> 8);
                        tmp[i*2+1] = (byte) (sval & 0x000000ff);
                    } else {
                        tmp[i*2] = (byte) (sval & 0x000000ff);
                        tmp[i*2+1] = (byte) ((sval & 0x0000ff00) >> 8);
                    }
                    break;
                case 3: // 24 bit
                    ival = new Float(sampleData[i] * (float)8388608).intValue();
                    if(bigEndian) {
                        tmp[i*3] = (byte) ((ival & 0x00ff0000) >> (8 * 2));
                        tmp[i*3+1] = (byte) ((ival & 0x0000ff00) >> 8);
                        tmp[i*3+2] = (byte) (ival & 0x000000ff);
                    } else {
                        tmp[i*3] = (byte) (ival & 0x000000ff);
                        tmp[i*3+1] = (byte) ((ival & 0x0000ff00) >> 8);
                        tmp[i*3+2] = (byte) ((ival & 0x00ff0000) >> (8 * 2));
                    }
                    break;
                case 4: // 32 bit
                    ival = new Float(sampleData[i] * (float)Integer.MAX_VALUE).intValue();
                    if(bigEndian) {
                        tmp[i*4] = (byte) ((ival & 0xff000000) >> (8 * 3));
                        tmp[i*4+1] = (byte) ((ival & 0x00ff0000) >> (8 * 2));
                        tmp[i*4+2] = (byte) ((ival & 0x0000ff00) >> 8);
                        tmp[i*4+3] = (byte) (ival & 0x000000ff);
                    } else {
                        tmp[i*4] = (byte) (ival & 0x000000ff);
                        tmp[i*4+1] = (byte) ((ival & 0x0000ff00) >> 8);
                        tmp[i*4+2] = (byte) ((ival & 0x00ff0000) >> (8 * 2));
                        tmp[i*4+3] = (byte) ((ival & 0xff000000) >> (8 * 3));
                    }
                    break;
                default:
                    System.err.println("jMusic AudioFileOut error: " +
                                       sampleSizeInBits +
                                       " bit audio output file format not supported, sorry :(");
                    System.exit(0); // ugly but necessary.
            }
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(tmp);

        // Specify file format
        AudioFormat format = new AudioFormat(sampleRate, sampleSizeInBits, channels, true, bigEndian);
        AudioInputStream ais = new AudioInputStream(bis, format, duration / channels);

        // Write out
        try {
        AudioSystem.write(ais, fileType, file);
        } catch (IOException ioe) {
            System.out.println("error writing audio file.");
        }
    }
}
