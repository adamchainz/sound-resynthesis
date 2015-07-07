package adj08;
import java.util.Arrays;

import ddf.minim.analysis.FFT;
import ddf.minim.analysis.FourierTransform;

public class SpectrogramGenerator {

	private static float lastMax = 0.0f;

	public static float[][] generateFromBuffer( float[] buffer ) {
		return generateFromWaveform( buffer, 16384, 44100 );
	}

	public static float[][] generateFromBuffer( float[] buffer, int windowLength ) {
		return generateFromWaveform( buffer, windowLength, 44100 );
	}

	public static float[][] generateFromWaveform( float[] buffer, int windowLength, float sampleRate ) {
		// Set up variables
		float overlap = 0.75f;
		int numwindows = (int) ( buffer.length / ( windowLength * ( 1 - overlap ) ) );

		// Ready to FFT
		float[][] spectrogram = new float[numwindows][windowLength / 2];
		FFT myFFT = new FFT( windowLength, sampleRate );
		myFFT.window( FourierTransform.HAMMING );

		// lastMax reset
		lastMax = 0.0f;

		// Repeatedly analyze windows of buffer
		for ( int i = 0; i < numwindows; i++ ) {
			// get analysis
			int start = (int) ( i * windowLength * ( 1 - overlap ) );
			int to = start + windowLength;
			float[] window = Arrays.copyOfRange( buffer, start, to );
			myFFT.forward( window );

			// copy spectrum into our spectrogram
			for ( int j = 0; j < windowLength / 2; j++ ) {
				spectrogram[i][j] = myFFT.getBand( j );
				lastMax = Math.max( spectrogram[i][j], lastMax );
			}
		}

		// Return
		return spectrogram;
	}

	public static float getLastMax() {
		return lastMax;
	}
}
