package adj08;

import java.util.Arrays;

import processing.core.PApplet;
import processing.core.PImage;

import ddf.minim.analysis.FFT;
import ddf.minim.analysis.FourierTransform;
import ddf.minim.analysis.WindowFunction;

public class Spectrogram {
	private int numFrames;
	private int windowLength;
	private int frameLength;
	private float overlap = 0.75f;
	private float[][] frames;
	private float peak = 0.0f;
	private WindowFunction windowFunction = FourierTransform.HAMMING;

	private Spectrogram() {

	}

	public static Spectrogram generateFromMonoWaveBuffer( float[] buffer ) {
		return generateFromMonoWaveBuffer( buffer, 16384, 44100 );
	}

	public static Spectrogram generateFromMonoWaveBuffer( float[] buffer, int windowLength ) {
		return generateFromMonoWaveBuffer( buffer, windowLength, 44100 );
	}

	public static Spectrogram generateFromMonoWaveBuffer( float[] buffer, int windowLength, float sampleRate ) {
		// Variables
		Spectrogram spect = new Spectrogram();
		spect.windowLength = windowLength;
		spect.frameLength = spect.windowLength / 2;
		spect.numFrames = (int) Math.ceil( buffer.length / ( spect.windowLength * ( 1 - spect.overlap ) ) );

		// Ready to FFT
		spect.frames = new float[spect.numFrames][spect.frameLength];
		FFT myFFT = new FFT( spect.windowLength, sampleRate );
		myFFT.window( spect.windowFunction );

		// Repeatedly analyze windows of buffer
		for ( int i = 0; i < spect.numFrames; i++ ) {
			// get analysis
			int start = (int) ( i * spect.windowLength * ( 1 - spect.overlap ) );
			int to = start + spect.windowLength;
			float[] window = Arrays.copyOfRange( buffer, start, to );
			myFFT.forward( window );

			// copy spectrum into our spectrogram
			for ( int j = 0; j < spect.frameLength; j++ ) {
				spect.frames[i][j] = myFFT.getBand( j );
				spect.peak = Math.max( spect.frames[i][j], spect.peak );
			}
		}

		// Return
		return spect;
	}

	public float getOverlap() {
		return overlap;
	}

	public void setOverlap( float overlap ) {
		this.overlap = overlap;
	}

	public float[] getFrame( int frameNo ) {
		if ( frameNo >= 0 && frameNo < numFrames )
			return frames[frameNo];
		else
			return null;
	}

	public float getHeightAt( int frameNo, int n ) {
		return frames[frameNo][n];
	}

	public int getNumFrames() {
		return numFrames;
	}

	public int getWindowLength() {
		return windowLength;
	}

	public float getPeak() {
		return peak;
	}

	public PImage generateSpectroImage( PApplet applet ) {
		PImage image = applet.createImage( numFrames, frameLength, PApplet.RGB );
		float sqrtPeak = (float) Math.sqrt( peak );

		for ( int i = 0; i < numFrames; i++ ) {
			for ( int j = 0; j < frameLength; j++ ) {
				float intensity = (float) ( ( Math.sqrt( frames[i][j] ) / sqrtPeak ) * 255 );

				image.pixels[i + ( frameLength - 1 - j ) * numFrames] = applet.color( intensity );
			}
		}
		//
		// for ( int i = 0; i < numFrames; i++ ) {
		// for ( int j = frameLength-1; j >= 0; j-- ) {
		// float intensity = (float) ( ( Math.sqrt( frames[i][j] ) / sqrtPeak ) * 255 );
		// int x = i;
		// int y =
		// image.pixels[i + (frameLength-j) * numFrames] = applet.color( intensity );
		// }
		// }

		return image;
	}

	public double centroidsError( Spectrogram compare ) {
		// Need them to be compatible in window length
		if ( windowLength != compare.getWindowLength() ) {
			System.err.println( "Spectrogram compare called when different window lengths!" );
			return Double.POSITIVE_INFINITY;
		}

		// Compare with self = 0 error
		if ( compare == this ) {
			return 0.0;
		}

		// Compare minimum length sounds' number of frames
		int numFramesToCompare = Math.min( numFrames, compare.getNumFrames() );

		// get Spectral Centroids arrays, limit lengths
		float[] thisSC = spectralCentroids();
		if ( thisSC.length > numFramesToCompare ) {
			thisSC = Arrays.copyOf( thisSC, numFramesToCompare );
		}
		float[] compareSC = compare.spectralCentroids();
		if ( compareSC.length > numFramesToCompare ) {
			compareSC = Arrays.copyOf( compareSC, numFramesToCompare );
		}

		// Make centroid measurement
		double centroidError = 0;
		for ( int i = 0; i < numFramesToCompare; i++ ) {
			centroidError += Math.abs( compareSC[i] - thisSC[i] );
		}

		// Divide by number of frames
		centroidError /= numFramesToCompare;

		return centroidError;
	}

	public double spectralNormError( Spectrogram compare ) {
		// Need them to be compatible in window length
		if ( windowLength != compare.getWindowLength() ) {
			System.err.println( "Spectrogram compare called when different window lengths!" );
			return Double.POSITIVE_INFINITY;
		}

		// Compare with self = 0 error
		if ( compare == this ) {
			return 0.0;
		}

		// Compare minimum length sounds' number of frames
		int numFramesToCompare = Math.min( numFrames, compare.getNumFrames() );

		// Start at 0 and go down.
		double specNormError = 0;
		for ( int frame = 0; frame < numFramesToCompare; frame++ ) {
			for ( int bin = 0; bin < frameLength; bin++ ) {
				// absolute square difference.
				double diff = Math.abs( frames[frame][bin] - compare.frames[frame][bin] );
				double add = Math.pow( diff, 2 );
				specNormError += add;
			}
		}

		// Divide by frame length and number of frames
		specNormError /= frameLength;
		specNormError /= numFramesToCompare;

		return specNormError;
	}

	public float[] spectralCentroids() {
		float[] centroids = new float[numFrames];
		for ( int i = 0; i < numFrames; i++ ) {
			// Do sum and sum*n
			float sum = 0f;
			float sumTimesN = 0f;
			for ( int bin = 0; bin < frameLength; bin++ ) {
				sumTimesN += frames[i][bin] * bin;
				sum += frames[i][bin];
			}

			// Stop division by 0.
			if ( sum > 0 ) {
				centroids[i] = sumTimesN / sum;
			} else {
				centroids[i] = 0;
			}

		}
		return centroids;
	}

	public float compareTo( Spectrogram compare ) {
		// Need them to be compatible in window length
		if ( windowLength != compare.getWindowLength() ) {
			System.err.println( "Spectrogram compare called when different window lengths!" );
			return 0.0f;
		}

		// Compare with self = 1
		if ( compare == this ) {
			return 1.0f;
		}

		// Compare only minimum number of frames
		int numFramesToCompare = Math.min( numFrames, compare.getNumFrames() );

		// Positive scoring
		float score = 0.0f;
		double scorePerPoint = 1.0f / ( numFramesToCompare * windowLength / 2 );

		// Score up
		for ( int i = 0; i < numFramesToCompare; i++ ) {
			// Pre-fetch frame
			float[] compareFrame = compare.getFrame( i );
			for ( int j = 0; j < windowLength / 2; j++ ) {
				// Scoring algorithm
				float scoreThisPoint;
				if ( frames[i][j] == compareFrame[j] ) {
					scoreThisPoint = (float) scorePerPoint;
				} else if ( frames[i][j] > compareFrame[j] ) {
					scoreThisPoint = (float) ( scorePerPoint * ( compareFrame[j] / frames[i][j] ) );
				} else {
					scoreThisPoint = (float) ( scorePerPoint * ( frames[i][j] / compareFrame[j] ) );
				}

				score += scoreThisPoint;
			}
		}

		return score;
	}

	public boolean isSilent() {
		for ( int frame = 0; frame < numFrames; frame++ ) {
			for ( int bin = 0; bin < frameLength; bin++ ) {
				if (frames[frame][bin] != 0f)
					return false;
			}
		}
		return true;
	}
}
