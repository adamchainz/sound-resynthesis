package adj08.utilities;

import java.awt.event.MouseEvent;

import processing.core.PApplet;
import processing.core.PImage;
import adj08.DFMSynthInstrument;
import adj08.MIDIInstrument;
import adj08.MIDIInstrumentOutputRecorder;
import adj08.MonoWaveformManipulator;
import adj08.PropertiesStore;
import adj08.Spectrogram;
import ddf.minim.Minim;

public class SpectrogramViewer extends PApplet {
	/**
	 *
	 */
	private static final long serialVersionUID = 5252559852480576809L;

	private static final int WINDOW_LENGTH = 1024;
	private static final float INSTRUMENT_LENGTH = 2.5f; // seconds
	private static final int MIDI_NOTE = 48;

	Minim minim;
	int spectroNo = 0;
	int frameNo = 0;
	float[] samples;
	Spectrogram spectrogram;
	PImage spectroImage;

	private boolean mouseOverApplet;

	private float spectroPeak;

	public static void main( String[] args ) {
		PApplet.main( new String[] { "adj08.utilities.SpectrogramViewer" } );
	}

	public void setup() {
		size( 800, 400, P2D );
		textMode( SCREEN );

		minim = new Minim( this );

		String filename = "testData/test4piano.wav";

		if ( filename.endsWith( "wav" ) ) {
			samples = MonoWaveformManipulator.readWav( filename );
		} else if ( filename.endsWith( "javaobj" ) ) {
			// Load + construct instrument
			PropertiesStore conf = PropertiesStore.loadFile( filename );
			MIDIInstrument instr = new DFMSynthInstrument( conf );

			// Record into wav buffer
			MIDIInstrumentOutputRecorder recorder = new MIDIInstrumentOutputRecorder( instr, 44100 );
			samples = recorder.sampleRecordMono( INSTRUMENT_LENGTH, MIDI_NOTE );
			MonoWaveformManipulator.writeWav( samples, filename+".wav" );
		} else {
			System.out.println( "Unknown filetype" );
			System.exit( -1 );
		}

		// Do analysis..
		spectrogram = Spectrogram.generateFromMonoWaveBuffer( samples, WINDOW_LENGTH );
		spectroImage = spectrogram.generateSpectroImage( this );
		spectroPeak = spectrogram.getPeak();
	}

	public void draw() {
		// BG
		background( 0 );
		image( spectroImage, 0, 0, this.width, this.height );

		if ( mouseOverApplet ) {
			// Draw frame inspection line
			stroke( color( 0, 255, 0, 127 ) );
			line( mouseX, this.height - 1, mouseX, 0 );

			// Draw frame in red
			frameNo = (int) ( ( (float) mouseX / (float) this.width ) * spectrogram.getNumFrames() );
			float[] frame = spectrogram.getFrame( frameNo );

			stroke( color( 255, 0, 0, 127 ) );

			// int whereTo = min( this.width, spectrograms[spectroNo].getWindowLength() / 2 );
			for ( int i = 0; i < this.width; i++ ) {
				int whereAt = (int) ( ( (float) i / (float) this.width ) * frame.length );
				float lineHeight = (float) ( ( frame[whereAt] / spectroPeak ) * this.height );
				line( i, this.height - 1, i, this.height - 1 - lineHeight );
			}
		}
	}

	public void mouseEntered( MouseEvent mouseEvent ) {
		mouseOverApplet = true;
	}

	public void mouseExited( MouseEvent mouseEvent ) {
		mouseOverApplet = false;
	}

	public void stop() {
		// always close Minim audio classes when you finish with them
		minim.stop();
		super.stop();
	}

}
