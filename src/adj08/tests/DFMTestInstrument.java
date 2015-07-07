package adj08.tests;
import java.util.LinkedList;
import java.util.List;

import adj08.MIDIInstrument;
import ddf.minim.ugens.ADSRRepeatable;
import ddf.minim.ugens.Frequency;
import ddf.minim.ugens.Glide;
import ddf.minim.ugens.MoogFilter;
import ddf.minim.ugens.ZeroHzFMCarrier;
import ddf.minim.ugens.Summer;
import ddf.minim.ugens.UGen;
import ddf.minim.ugens.ValuePlusADSR;

public class DFMTestInstrument implements MIDIInstrument {
	Glide glide;
	ZeroHzFMCarrier osc;
	ADSRRepeatable ampEnv;
	ValuePlusADSR filterEnv, pitchEnv;
	MoogFilter filter;
	UGen out;
	List<Integer> noteStack;

	public DFMTestInstrument() {
		this(null);
		out = new Summer();
		filter.patch( out );
	}

	public DFMTestInstrument( UGen out ) {
		glide = new Glide( 100, 0.107f );
		ampEnv = new ADSRRepeatable( 0.25f, 1f, 0.0f, 1.0f, 0.05f );
		pitchEnv = new ValuePlusADSR( 100.0f, 00f, 0.005f, 0.05f, 0.0f, 0.0f );
		glide.patch( pitchEnv.defaultValue );

		for ( int i = 0; i <= 3; i++ ) {
			osc = ZeroHzFMCarrier.RandomMultiFMOsc();
			pitchEnv.patch( osc.frequency );
			osc.patch( ampEnv );
		}
		filter = new MoogFilter( 15000f, 0.5f );
		filterEnv = new ValuePlusADSR( 9000.0f, 400f, 1.5f, 1.5f, 0.0f, 1.0f );
		filterEnv.patch(filter.frequency);
		if (out != null)
			filter.patch( out );
		this.out = out;
		noteStack = new LinkedList<Integer>();
	}

	@Override
	public void midiNoteOn( int midiNote, int velocity ) {
		// Values
		noteStack.add( 0, midiNote );
		float freq = Frequency.ofMidiNote( midiNote ).asHz();

		if ( noteStack.size() == 1 ) {
			// Start from scratch
			glide.restart( freq );
			osc.noteOn();
			ampEnv.noteOn();
			filterEnv.noteOn();
			pitchEnv.noteOn();
			ampEnv.patchAndUnpatchAfterRelease( filter.audio );
		} else {
			// Glide
			glide.glideTo( freq );
		}
	}

	@Override
	public void midiNoteOff( int midiNote, int aftertouch ) {
		// current note is one at top of stack
		int currNote = noteStack.get( 0 );
		// remove midiNote
		noteStack.remove( new Integer( midiNote ) );
		if ( noteStack.size() == 0 ) {
			// off
			osc.noteOff();
			ampEnv.noteOff();
			filterEnv.noteOff();
			pitchEnv.noteOn();
		} else if ( currNote == midiNote ) {
			// note removed was currently playing one
			glide.glideTo( Frequency.ofMidiNote( noteStack.get( 0 ) ).asHz() );
		}
	}

	public UGen getOutputUGen() {
		return this.out;
	}

	@Override
	public float getAttackTime() {
		return ampEnv.getAttackTime();
	}

	@Override
	public float getReleaseTime() {
		return ampEnv.getReleaseTime();
	}

}
