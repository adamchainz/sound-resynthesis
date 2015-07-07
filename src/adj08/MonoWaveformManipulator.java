package adj08;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

public class MonoWaveformManipulator {

	public static float[] toMono( float[] monoRecording ) {
		// Dummy method for accidental calls
		return monoRecording;
	}

	public static float[] toMono( float[][] multiChannelRecording ) {
		// Turns a multi-channel waveform into a mono one.
		float[] output = new float[multiChannelRecording.length];
		for ( int i = 0; i < multiChannelRecording.length; i++ ) {
			float sum = 0.0f;
			for ( int j = 0; j < multiChannelRecording[i].length; j++ ) {
				sum += multiChannelRecording[i][j];
			}
			output[i] = sum / multiChannelRecording[i].length;
		}

		return output;
	}

	public static float[] normalize( float[] waveform ) {
		float normFactor = getNormalizationFactor( waveform );

		for ( int i = 0; i < waveform.length; i++ ) {
			if ( Double.isNaN( waveform[i] ) )
				System.err.println( "NaN in waveform!" );
			waveform[i] = waveform[i] * normFactor;
		}

		return waveform;
	}

	public static float getNormalizationFactor( float[] waveform ) {
		float max = Float.NEGATIVE_INFINITY;
		for ( int i = 0; i < waveform.length; i++ ) {
			max = Math.max( max, Math.abs( waveform[i] ) );
		}
		if ( Float.isInfinite( max ) ) {
			System.err.println( " Infinite in waveform ! " );
			return 1.0f;
		} else if ( max == 0f ) {
			return 1.0f;
		} else {
			return ( 1.0f / max );
		}
	}

	public static float[] readWav( String filename ) {
		/* Adapted from jMusic API version 1.5, licensed under GPL */
		float[] samples = null;
		try {
			// Lots of input
			File file = new File( filename );
			AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat( file );
			AudioFormat format = fileFormat.getFormat();
			boolean bigEndian = format.isBigEndian();
			int channels = format.getChannels();
			int sampleRate = (int) format.getSampleRate();
			int duration = fileFormat.getFrameLength() * channels;
			int sampleSize = ( format.getSampleSizeInBits() ) / 8;

			// Read in file's bytes
			AudioInputStream is = AudioSystem.getAudioInputStream( file );
			byte[] tmp = new byte[(int) duration * sampleSize];
			is.read( tmp );
			is.close();

			// Convert to floats
			ByteArrayInputStream bis = new ByteArrayInputStream( tmp );
			samples = new float[duration];
			byte[] sampleWord = new byte[sampleSize];
			for ( int i = 0; i < duration; i++ ) {
				if ( bis.read( sampleWord ) == -1 ) {
					// this.finished = true;
					System.out.println( "Ran out of samples to read" );
				} else {
					samples[i] = getFloat( sampleWord, bigEndian, sampleSize );
				}
				bis.close();
			}

			// Mix to Mono!
			if ( channels > 1 ) {
				samples = monoMixLinear( samples, channels );
			}
		} catch ( UnsupportedAudioFileException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch ( IOException e ) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return samples;
	}

	private static float[] monoMixLinear( float[] samples, int channels ) {
		float[] newSamples = new float[samples.length / channels];
		for ( int i = 0; i < newSamples.length; i++ ) {
			float sum = 0.0f;
			for ( int j = 0; j < channels; j++ ) {
				sum += samples[i * channels + j];
			}
			newSamples[i] = sum / channels;
		}
		// TODO Auto-generated method stub
		return null;
	}

	private static float getFloat( byte[] b, boolean bigEndian, int sampleSize ) {
		/* Adapted from jMusic API version 1.5, licensed under GPL */
		float sample = 0.0f;
		int ret = 0;
		int length = b.length;
		for ( int i = 0; i < b.length; i++, length-- ) {
			ret |= ( (int) ( b[i] & 0xFF ) << ( ( ( ( bigEndian ) ? length : ( i + 1 ) ) * 8 ) - 8 ) );
		}
		switch ( sampleSize ) {
			case 1:
				if ( ret > 0x7F ) {
					ret = ~ret;
					ret &= 0x7F;
					ret = ~ret + 1;
				}
				sample = (float) ( (float) ret / (float) Byte.MAX_VALUE );
				break;
			case 2:
				if ( ret > 0x7FFF ) {
					ret = ~ret;
					ret &= 0x7FFF;
					ret = ~ret + 1;
				}
				sample = (float) ( (float) ret / (float) Short.MAX_VALUE );
				break;
			case 3:
				if ( ret > 0x7FFFFF ) {
					ret = ~ret;
					ret &= 0x7FFFFF;
					ret = ~ret + 1;
				}
				sample = (float) ( (float) ret / 8388608f );
				break;
			case 4:
				sample = (float) ( (double) ret / (double) Integer.MAX_VALUE );
				break;
			default:
				System.err.println( "Format not accepted" );
		}
		return sample;
	}

	public static boolean writeWav( float[] waveform, String filename ) {
		return writeWav( waveform, filename, 44100, 16 );

	}

	public static boolean writeWav( float[] waveform, String filename, int sampleRate, int sampleSizeInBits ) {
		/* Adapted from jMusic API version 1.5, licensed under GPL */
		// Properties
		int channels = 1;
		int duration = waveform.length;
		int sampleSize = sampleSizeInBits / 8;
		AudioFileFormat.Type fileType = null;
		boolean bigEndian;

		// Choose file type from file name
		if ( filename.endsWith( ".au" ) ) {
			fileType = AudioFileFormat.Type.AU;
			bigEndian = true;
		} else if ( filename.endsWith( ".wav" ) ) {
			fileType = AudioFileFormat.Type.WAVE;
			bigEndian = false;
		} else if ( filename.endsWith( ".aif" ) || filename.endsWith( ".aiff" ) ) {
			fileType = AudioFileFormat.Type.AIFF;
			bigEndian = true;
		} else { // default
			filename = filename + ".au";
			bigEndian = true;
		}

		// Prepare for output
		File file = new File( filename );

		// Convert floats to bytes
		byte[] tmp = new byte[waveform.length * sampleSize];
		for ( int i = 0; i < waveform.length; i++ ) {
			int ival = -1;
			switch ( sampleSize ) {
				case 1: // 8 bit
					tmp[i] = new Float( waveform[i] * (float) Byte.MAX_VALUE ).byteValue();
					break;
				case 2: // 16 bit
					short sval = new Float( waveform[i] * (float) Short.MAX_VALUE ).shortValue();
					if ( bigEndian ) {
						tmp[i * 2] = (byte) ( ( sval & 0x0000ff00 ) >> 8 );
						tmp[i * 2 + 1] = (byte) ( sval & 0x000000ff );
					} else {
						tmp[i * 2] = (byte) ( sval & 0x000000ff );
						tmp[i * 2 + 1] = (byte) ( ( sval & 0x0000ff00 ) >> 8 );
					}
					break;
				case 3: // 24 bit
					ival = new Float( waveform[i] * (float) 8388608 ).intValue();
					if ( bigEndian ) {
						tmp[i * 3] = (byte) ( ( ival & 0x00ff0000 ) >> ( 8 * 2 ) );
						tmp[i * 3 + 1] = (byte) ( ( ival & 0x0000ff00 ) >> 8 );
						tmp[i * 3 + 2] = (byte) ( ival & 0x000000ff );
					} else {
						tmp[i * 3] = (byte) ( ival & 0x000000ff );
						tmp[i * 3 + 1] = (byte) ( ( ival & 0x0000ff00 ) >> 8 );
						tmp[i * 3 + 2] = (byte) ( ( ival & 0x00ff0000 ) >> ( 8 * 2 ) );
					}
					break;
				case 4: // 32 bit
					ival = new Float( waveform[i] * (float) Integer.MAX_VALUE ).intValue();
					if ( bigEndian ) {
						tmp[i * 4] = (byte) ( ( ival & 0xff000000 ) >> ( 8 * 3 ) );
						tmp[i * 4 + 1] = (byte) ( ( ival & 0x00ff0000 ) >> ( 8 * 2 ) );
						tmp[i * 4 + 2] = (byte) ( ( ival & 0x0000ff00 ) >> 8 );
						tmp[i * 4 + 3] = (byte) ( ival & 0x000000ff );
					} else {
						tmp[i * 4] = (byte) ( ival & 0x000000ff );
						tmp[i * 4 + 1] = (byte) ( ( ival & 0x0000ff00 ) >> 8 );
						tmp[i * 4 + 2] = (byte) ( ( ival & 0x00ff0000 ) >> ( 8 * 2 ) );
						tmp[i * 4 + 3] = (byte) ( ( ival & 0xff000000 ) >> ( 8 * 3 ) );
					}
					break;
				default:
					System.err.println( "jMusic AudioFileOut error: " + sampleSizeInBits
							+ " bit audio output file format not supported, sorry :(" );
					System.exit( 0 ); // ugly but necessary.
			}
		}
		ByteArrayInputStream bis = new ByteArrayInputStream( tmp );

		// Specify file format
		AudioFormat format = new AudioFormat( sampleRate, sampleSizeInBits, channels, true, bigEndian );
		AudioInputStream ais = new AudioInputStream( bis, format, duration / channels );

		// Write out
		try {
			AudioSystem.write( ais, fileType, file );
		} catch ( IOException ioe ) {
			System.out.println( "error writing audio file." );
			return false;
		}

		return true;
	}

}
