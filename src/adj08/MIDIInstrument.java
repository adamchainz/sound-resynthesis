package adj08;

import ddf.minim.ugens.UGen;

public interface MIDIInstrument {

	/*
	 * When the instrument is told to turn a note on
	 *
	 * @param midiNote The midi note number (0-127)
	 * @param velocity The midi velocity value (0-127)
	 */
	public void midiNoteOn( int midiNote, int velocity );

	public void midiNoteOff( int midiNote, int aftertouch );

	public UGen getOutputUGen();

	public float getAttackTime();

	public float getReleaseTime();

}
