package adj08.tests;

import java.awt.event.KeyEvent;

import processing.core.PApplet;
import adj08.DFMSynthInstrument;
import adj08.MIDICompKeyboard;
import adj08.MIDIInstrument;
import adj08.PropertiesStore;
import adj08.Randomizer;
import ddf.minim.AudioOutput;
import ddf.minim.Minim;
import ddf.minim.ugens.Summer;

public class RealInstrumentTest extends PApplet {

	/**
	 *
	 */
	private static final long serialVersionUID = -3559150955564503431L;

	public static String filename = "best_individual.javaobj";

	public static void main( String[] args ) {
		PApplet.main( new String[] { "adj08.tests.RealInstrumentTest" } );
		if ( args.length > 0 )
			filename = args[0];
	}

	Minim minim;
	Summer sum;
	AudioOutput out;
	MIDICompKeyboard keyboard;
	MIDIInstrument instr;

	@Override
	public void setup() {
		// initialize the drawing window
		size( 512, 200, P2D );
		smooth();

		// initialize the minim, out, and recorder objects
		minim = new Minim( this );
		out = minim.getLineOut( Minim.MONO, 1024 );

		// Make ma instrument!
		PropertiesStore conf;
		// String filename = "best_individual.javaobj";

		if ( filename.endsWith( "javaobj" ) ) {
			conf = PropertiesStore.loadFile( filename );
			// conf.setProperty( "FilterRes", 0.5f );
			// conf.setProperty( "PitchEnvAmt", 0.0f );

			// System.out.println( "PitchEnvAmt = " + conf.getFloatProperty( "PitchEnvAmt" ) );
			// System.out.println( "FRes = " + conf.getFloatProperty( "FilterRes" ) );
			// System.out.println( "Amplitude = " + conf.getFloatProperty( "Amplitude" ) );
			// System.out.println( "FilterEnvAmt = " + conf.getFloatProperty( "FilterEnvAmt" ) );
		} else if ( filename.equals( "random" ) ) {
			conf = loadRandomInstrument();
		} else {
			conf = loadFiddleInstrument();
		}

		instr = new DFMSynthInstrument( conf );
		instr.getOutputUGen().patch( out );

		// Make my midi keyboard
		keyboard = new MIDICompKeyboard( this, instr );

	}

	private PropertiesStore loadFiddleInstrument() {
		PropertiesStore conf = new PropertiesStore( DFMSynthInstrument.getDefaults() );
		conf.setProperty( "AmpEnvA", 0.005f );
		conf.setProperty( "AmpEnvS", 0.5f );
		float fMin = conf.getFloatMin( "FilterFreq" ), fMax = conf.getFloatMax( "FilterFreq" );
		float fFreq = fMin + ( Randomizer.getFloat() * ( fMax - fMin ) );
		conf.setProperty( "FilterFreq", fFreq );
		conf.setProperty( "FilterRes", 0.6f );
		conf.setProperty( "FilterEnvAmt", 3500.0f );
		conf.setProperty( "FilterEnvA", 0.005f );
		conf.setProperty( "FilterEnvD", 0.8f );
		conf.setProperty( "FilterEnvS", 0.0f );

		conf.setProperty( "PitchEnvAmt", 100.0f );
		conf.setProperty( "PitchEnvA", 0.05f );
		conf.setProperty( "PitchEnvD", 0.44f );

		conf.setProperty( "NumCarriers", 4 );

		conf.setProperty( "Carrier1Osc1Amp", 3.0f );
		conf.setProperty( "Carrier1Osc1Ratio", 7.0f );
		conf.setProperty( "Carrier1Osc1A", 1.0f );
		conf.setProperty( "Carrier1Osc2Amp", 2.0f );
		conf.setProperty( "Carrier1Osc2Ratio", 4.0f );
		conf.setProperty( "Carrier1Osc2A", 1.0f );

		conf.setProperty( "Carrier2Amp", 0.0f );

		conf.setProperty( "Carrier2Osc1Amp", 10.0f );
		conf.setProperty( "Carrier2Osc1S", 0.0f );
		conf.setProperty( "Carrier2Osc2Amp", 10.0f );
		conf.setProperty( "Carrier2Osc2S", 0.0f );
		conf.setProperty( "Carrier2Osc1Ratio", 3.0f );
		conf.setProperty( "Carrier2Osc2Ratio", 0.5f );
		return conf;
	}

	private PropertiesStore loadRandomInstrument() {
		PropertiesStore conf = new PropertiesStore( DFMSynthInstrument.getDefaults() );
		conf.randomizeAll();
		System.out.println( conf.toString() );
		return conf;
	}

	public void draw() {
		// erase the window to black
		background( 0 );
		// draw using a white stroke
		stroke( 255 );
		// draw the waveforms
		for ( int i = 0; i < out.bufferSize() - 1; i++ ) {
			// find the x position of each buffer value
			float x1 = map( i, 0, out.bufferSize(), 0, width );
			float x2 = map( i + 1, 0, out.bufferSize(), 0, width );
			// draw a line from one buffer position to the next for both channels
			line( x1, 50 + out.left.get( i ) * 50, x2, 50 + out.left.get( i + 1 ) * 50 );
			line( x1, 150 + out.right.get( i ) * 50, x2, 150 + out.right.get( i + 1 ) * 50 );
		}
	}

	// Overall key handling
	public void keyPressed( KeyEvent key ) {
		// Quit when Ctrl-Q pressed
		if ( key.getKeyCode() == KeyEvent.VK_Q && key.isControlDown() ) {
			System.exit( 0 );
		}
	}

}
