package adj08;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;


import processing.core.PApplet;

public class MIDICompKeyboard implements KeyListener {
	// milliseconds of delay an off event will experience; necessary due to refiring
	final static int NOTE_OFF_DELAY = 20;

	final static int NO_NOTE = -1;
	final static int DOWN_OCTAVE = -3;
	final static int UP_OCTAVE = -2;

	PApplet app;
	KeyDetail[] keys;
	int base = 60;
	MIDIInstrument instrument;

	public MIDICompKeyboard( PApplet app, MIDIInstrument instrument ) {
		// Store values
		this.app = app;
		this.instrument = instrument;

		// Prepare ourselves to receive key information
		app.addKeyListener( this );

		// Store our array of which keys are active
		keys = new KeyDetail[127];
		for ( int i = 0; i < keys.length; i++ ) {
			keys[i] = new KeyDetail();
		}
	}
	
	public void stop() {
		app.removeKeyListener( this );
	}

	public void keyPressed( KeyEvent key ) {
		int n = getN( key );

		if ( n >= 0 ) {
			// Figure out which key this really means (octave control)
			int keyNum = base + n;
			// Turn it on!
			if ( keyNum <= 126 && !keys[keyNum].on ) {
				// if it is "justoff" - cancel the pending timer that will turn it off
				if ( keys[keyNum].justoff ) {
					keys[keyNum].timer.cancel();
					keys[keyNum].timer = null;
				} else {
					// else turn it on and dispatch event
					instrument.midiNoteOn( keyNum, 127 );
				}
				keys[keyNum].on = true;
				keys[keyNum].justoff = false;
			}
		} else if ( n == DOWN_OCTAVE ) {
			if ( base >= 12 ) {
				// Move down an octave
				base -= 12;
				// Turn off all notes in previous octave
				for ( int i = base + 12; i <= base + 26; i++ ) {
					if ( i <= 126 && keys[i].on )
						keyOff( i );
				}
			}
		} else if ( n == UP_OCTAVE ) {
			if ( base <= 115 )
				base += 12;
			// Turn off all notes in previous octave
			for ( int i = base - 12; i <= base + 2; i++ ) {
				if ( i >= 0 && keys[i].on )
					keyOff( i );
			}
		}

	}

	public void keyReleased( KeyEvent key ) {
		int n = getN( key );

		if ( n >= 0 ) {
			// Figure out which key this really means (octave control)
			int keyNum = base + n;
			keyOff( keyNum );
		}
	}

	private void keyOff( int keyNum ) {
		// Turn it off!
		if ( keyNum <= 126 && keys[keyNum].on ) {
			keys[keyNum].on = false;
			keys[keyNum].justoff = true;
			// make thread that will turn it off in 1ms
			keys[keyNum].timer = new Timer();
			keys[keyNum].timer.schedule( new KeyOffTask( keyNum ), NOTE_OFF_DELAY );
		}
	}

	private int getN( KeyEvent key ) {
		int n = -1;
		switch ( key.getKeyCode() ) {
			// Notes of the piano
			case KeyEvent.VK_A:
				n = 0;
				break;
			case KeyEvent.VK_W:
				n = 1;
				break;
			case KeyEvent.VK_S:
				n = 2;
				break;
			case KeyEvent.VK_E:
				n = 3;
				break;
			case KeyEvent.VK_D:
				n = 4;
				break;
			case KeyEvent.VK_F:
				n = 5;
				break;
			case KeyEvent.VK_T:
				n = 6;
				break;
			case KeyEvent.VK_G:
				n = 7;
				break;
			case KeyEvent.VK_Y:
				n = 8;
				break;
			case KeyEvent.VK_H:
				n = 9;
				break;
			case KeyEvent.VK_U:
				n = 10;
				break;
			case KeyEvent.VK_J:
				n = 11;
				break;
			case KeyEvent.VK_K:
				n = 12;
				break;
			case KeyEvent.VK_O:
				n = 13;
				break;
			case KeyEvent.VK_L:
				n = 14;
				break;

			// OCTAVE CHANGES
			case KeyEvent.VK_Z:
				n = DOWN_OCTAVE;
				break;
			case KeyEvent.VK_X:
				n = UP_OCTAVE;
				break;
		}
		return n;
	}

	@Override
	public void keyTyped( KeyEvent arg0 ) {
		// nothing.
		return;
	}

	private class KeyDetail {
		boolean on = false;
		boolean justoff = false;
		Timer timer;
	}

	private class KeyOffTask extends TimerTask {
		int keyNum;

		public KeyOffTask( int keyNum ) {
			this.keyNum = keyNum;
		}

		@Override
		public void run() {
			instrument.midiNoteOff( keyNum, 127 );
			keys[keyNum].justoff = false;
		}
	}

}
