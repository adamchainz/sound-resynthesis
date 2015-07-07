package adj08.tests;
import java.awt.event.KeyEvent;

import processing.core.PApplet;
import processing.core.PImage;
import adj08.MIDIInstrument;
import adj08.MIDIInstrumentOutputRecorder;
import adj08.MonoWaveformManipulator;
import adj08.Spectrogram;
import ddf.minim.Minim;

public class SpectrogramInstrumentTest extends PApplet {
	private static final long serialVersionUID = -1690835270827018353L;
	Minim minim;
	int windowLength = 512;
	int spectroNo = 0;
	int frameNo = 0;
	int numSamples = 5;
	Spectrogram[] spectrograms = new Spectrogram[numSamples];
	PImage[] spectroImages = new PImage[numSamples];
	float[] spectroPeaks = new float[numSamples];
	float[][] comparisons = new float[numSamples][numSamples];

	public static void main( String[] args ) {
		System.out.println( "HIA" );
		PApplet.main( new String[] { "adj08.tests.SpectrogramInstrumentTest" } );
	}

	public void setup() {
		size( 800, 400, P2D );
		textMode( SCREEN );

		minim = new Minim( this );

		// Test samples
		for ( int i = 0; i < numSamples; i++ ) {

			/*// generate instruments
			MainGenetic g = new MainGenetic();
			try {
				g.getConfiguration();
			} catch ( Exception e ) {
				e.printStackTrace();
			}
			PropertiesStore conf = g.getIndividual();
			MIDIInstrument testInstrument = new DFMSynthInstrument(conf);*/
			MIDIInstrument testInstrument = new DFMTestInstrument();
			MIDIInstrumentOutputRecorder record = new MIDIInstrumentOutputRecorder( testInstrument );

			float[] samplesMono = record.sampleRecordMono( 5.0f, 24 );
			samplesMono = MonoWaveformManipulator.normalize( samplesMono );

			System.out.println( "   samples = " + samplesMono.length );

			// Do analysis..
			spectrograms[i] = Spectrogram.generateFromMonoWaveBuffer( samplesMono, windowLength );

			spectroPeaks[i] = spectrograms[i].getPeak();

			spectroImages[i] = spectrograms[i].generateSpectroImage( this );
		}

		// Comparisons
		for ( int i = 0; i < numSamples; i++ ) {
			for ( int j = 0; j < numSamples; j++ ) {
				comparisons[i][j] = spectrograms[i].compareTo( spectrograms[j] );
			}
		}
	}

	public void draw() {
		// BG
		background( 0 );
		image( spectroImages[spectroNo], 0, 0, this.width, this.height );

		// Draw stats
		stroke( 255 );
		fill( 255 );
		frameNo = (int) ( ( (float) mouseX / (float) this.width ) * spectrograms[spectroNo].getNumFrames() );
		text( "Showing sample " + ( spectroNo + 1 ) + " : inspecting frame " + frameNo + " of "
				+ spectrograms[spectroNo].getNumFrames(), 5, 20 );
		for ( int i = 0; i < numSamples; i++ ) {
			text( "Compared to sample # " + ( i + 1 ) + " score = " + comparisons[spectroNo][i], 5, 40 + 20 * i );
		}

		// Draw frame inspection line
		stroke( color( 0, 255, 0, 127 ) );
		line( mouseX, this.height - 1, mouseX, 0 );

		// Draw frame in red
		float[] frame = spectrograms[spectroNo].getFrame( frameNo );

		stroke( color( 255, 0, 0, 127 ) );

		int whereTo = min( this.width, spectrograms[spectroNo].getWindowLength() / 2 );
		for ( int i = 0; i < this.width; i++ ) {
			int whereAt = (int) ( ( (float) i / (float) this.width ) * frame.length );
			float lineHeight = (float) ( ( frame[whereAt] / spectroPeaks[spectroNo] ) * this.height );
			line( i, this.height - 1, i, this.height - 1 - lineHeight );
		}
	}

	@Override
	public void keyPressed( KeyEvent key ) {
		if ( key.getKeyCode() == KeyEvent.VK_RIGHT ) {
			spectroNo = Math.min( spectroNo + 1, numSamples - 1 );
		} else if ( key.getKeyCode() == KeyEvent.VK_LEFT ) {
			spectroNo = Math.max( spectroNo - 1, 0 );
		}
	}

	public void stop() {
		// always close Minim audio classes when you finish with them
		minim.stop();
		super.stop();
	}

}
