package adj08.tests;

import java.awt.event.KeyEvent;

import processing.core.PApplet;
import adj08.MIDICompKeyboard;
import adj08.MIDIInstrument;
import ddf.minim.AudioOutput;
import ddf.minim.Minim;

public class InstrumentTest extends PApplet {

	public static void main( String[] args ) {
		PApplet.main( new String[] { "adj08.tests.InstrumentTest" } );
	}

	Minim minim = null;
	TestSummer sum;
	AudioOutput out;
	MIDICompKeyboard keyboard;
	MIDIInstrument instr;

	@Override
	public void setup() {
		// initialize the drawing window
		size( 512, 200, P2D );
		smooth();

		initStuff();

	}

	private void initStuff() {
		if (minim != null) {
			sum.unpatch( out );
			out.close();
			keyboard.stop();
			minim.stop();
			minim = null;
		}
		// initialize the minim, out, and recorder objects
		minim = new Minim( this );
		out = minim.getLineOut( Minim.STEREO, 1024 );
		sum = new TestSummer();
		sum.patch( out );

		// Make ma instrument!
		instr = new DFMTestInstrument( sum );

		// Make my midi keyboard
		keyboard = new MIDICompKeyboard( this, instr );
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
	public void keyPressed(KeyEvent key) {
		// Quit when Ctrl-Q pressed
		if (key.getKeyCode() == KeyEvent.VK_Q && key.isControlDown()) {
			System.exit( 0 );
		} else if (key.getKeyCode() == KeyEvent.VK_F1) {
			initStuff();
		} else if (key.getKeyCode() == KeyEvent.VK_F2) {
			sum.flipDebug();
		}
	}

}
