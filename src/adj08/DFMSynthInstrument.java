package adj08;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import adj08.MIDIInstrument;
import ddf.minim.ugens.ADSRRepeatable;
import ddf.minim.ugens.Frequency;
import ddf.minim.ugens.Glide;
import ddf.minim.ugens.MoogFilter;
import ddf.minim.ugens.ZeroHzFMCarrier;
import ddf.minim.ugens.ValuePlusADSRPercentage;
import ddf.minim.ugens.ValuePlusADSR;
import ddf.minim.ugens.Summer;
import ddf.minim.ugens.UGen;

public class DFMSynthInstrument implements MIDIInstrument {
	// Constants
	private static final float MIN_ADR = 0.0f;
	private static final float MAX_ADR = 2.0f;

	// Static vars
	private static PropertiesStore defaults;

	// Vars
	Glide glide;
	List<ZeroHzFMCarrier> carriers;
	Summer carrierSum;
	ADSRRepeatable ampEnv;
	ValuePlusADSR filterEnv;
	ValuePlusADSRPercentage pitchEnv;
	MoogFilter filter;
	UGen out;
	List<Integer> noteStack;

	public static PropertiesStore getDefaults() {
		// Straight away return if already set
		if ( defaults != null )
			return defaults;

		// Otherwise set defaults + min/max ranges, then return
		defaults = new PropertiesStore();
		defaults.setProperty( "AttackOn", 1, 0, 1 );
		defaults.setProperty( "Attack", 0.25f, MIN_ADR, MAX_ADR );
		defaults.setProperty( "Decay", 1.0f, MIN_ADR, MAX_ADR );
		defaults.setProperty( "AmpEnvS", 0.8f, 0.0f, 1.0f );
		defaults.setProperty( "ReleaseOn", 1, 0, 1 );
		defaults.setProperty( "Release", 0.05f, MIN_ADR, MAX_ADR );

		defaults.setProperty( "PitchEnvOn", 0, 0, 1 );
		defaults.setProperty( "PitchEnvAmt", 0.0f, -50.0f, 100.0f ); // +/- 1 octave
		// defaults.setProperty( "PitchEnvA", 0.05f, MIN_ADR, MAX_ADR );
		// defaults.setProperty( "PitchEnvD", 0.05f, MIN_ADR, MAX_ADR );

		defaults.setProperty( "NumCarriers", 2, 1, 5 );
		for ( int i = 1; i <= 5; i++ ) {
			// Each carrier has an amplitude
			defaults.setProperty( "Carrier" + i + "Amp", 1.0f, 0.0f, 1.0f );

			// Set up each carrier's default osc set up
			defaults.setProperty( "Carrier" + i + "NumOscs", 2, 1, 2 );
			for ( int j = 1; j <= 2; j++ ) {
				defaults.setProperty( "Carrier" + i + "Osc" + j + "Ratio", 1.0f, 0.25f, 8.0f );
				defaults.setProperty( "Carrier" + i + "Osc" + j + "Amp", 1.0f, 0.0f, 10.0f );

				// defaults.setProperty( "Carrier" + i + "Osc" + j + "IntialAmp", 0.0f, 0.0f, 1.0f );
				// defaults.setProperty( "Carrier" + i + "Osc" + j + "PeakAmp", 1.0f, 0.0f, 1.0f );
				defaults.setProperty( "Carrier" + i + "Osc" + j + "S", 1.0f, 0.0f, 1.0f );
				// defaults.setProperty( "Carrier" + i + "Osc" + j + "ReleaseAmp", 0.0f, 0.0f, 1.0f );
				defaults.setProperty( "Carrier" + i + "Osc" + j + "AmpModel", 1, 1, 3 );
			}
		}

		defaults.setProperty( "FilterFreq", 1500.0f, 30.0f, 20000.0f );
		defaults.setProperty( "FilterRes", 0.5f, 0.1f, 0.5f );

		defaults.setProperty( "FilterEnvOn", 1, 0, 1 );
		defaults.setProperty( "FilterEnvAmt", 0.0f, -10000.0f, 10000.0f );
		defaults.setProperty( "FilterEnvS", 1.0f, 0.0f, 1.0f );

		return defaults;
	}

	// Constructors
	public DFMSynthInstrument() {
		this( null, null );
	}

	public DFMSynthInstrument( UGen out ) {
		this( out, null );
	}

	public DFMSynthInstrument( PropertiesStore settings ) {
		this( null, settings );
	}

	public DFMSynthInstrument( UGen outgen, PropertiesStore settings ) {
		// Param defaults
		if ( outgen == null ) {
			out = new Summer();
		} else {
			out = outgen;
		}

		if ( settings == null ) {
			settings = DFMSynthInstrument.getDefaults();
		} else {
			settings.setDefaults( DFMSynthInstrument.getDefaults() );
		}

		// Glide
		glide = new Glide( 100, 0.107f );

		// Amp Env
		float attack, release;
		if ( settings.getIntProperty( "AttackOn" ) == 1 ) {
			attack = settings.getFloatProperty( "Attack" );
		} else {
			attack = 0f;
		}
		float decay = settings.getFloatProperty( "Decay" );
		float ampEnvS = settings.getFloatProperty( "AmpEnvS" );
		if ( settings.getIntProperty( "ReleaseOn" ) == 1 ) {
			release = settings.getFloatProperty( "Release" );
		} else {
			release = 0f;
		}

		// Renormalizing Amplitude
		float amplitude = settings.getFloatProperty( "Amplitude" );
		if ( amplitude == 0.0 ) {
			amplitude = 0.25f;
		} else {
			amplitude *= 0.25f;
		}

		ampEnv = new ADSRRepeatable( amplitude, attack, decay, ampEnvS, release );

		// Pitch Env
		float pitchEnvAmt = settings.getFloatProperty( "PitchEnvAmt" );
		if ( settings.getIntProperty( "PitchEnvOn" ) == 0 ) {
			pitchEnvAmt = 0f;
		}
		float pitchEnvS = 0.0f;
		float pitchEnvR = 0.0f;

		pitchEnv = new ValuePlusADSRPercentage( 100.0f, pitchEnvAmt, attack, decay, pitchEnvS, pitchEnvR );
		glide.patch( pitchEnv.defaultValue );

		// Carriers
		int numCarriers = settings.getIntProperty( "NumCarriers" );
		carriers = new ArrayList<ZeroHzFMCarrier>();
		carrierSum = new Summer();
		carrierSum.patch( ampEnv );

		for ( int i = 1; i <= numCarriers; i++ ) {
			float carrierAmp = settings.getFloatProperty( "Carrier" + i + "Amp" );
			ZeroHzFMCarrier carrier = new ZeroHzFMCarrier( carrierAmp );
			pitchEnv.patch( carrier.frequency );
			carrier.patch( carrierSum );
			carriers.add( carrier );

			// Oscillators=modulators for carrier
			int numOscs = settings.getIntProperty( "Carrier" + i + "NumOscs" );
			for ( int j = 1; j <= numOscs; j++ ) {
				// Get properties for this oscillator
				float oscAmp = settings.getFloatProperty( "Carrier" + i + "Osc" + j + "Amp" );
				float oscRatio = settings.getFloatProperty( "Carrier" + i + "Osc" + j + "Ratio" );

				float oscSustain = settings.getFloatProperty( "Carrier" + i + "Osc" + j + "S" );
				int oscAmpModel = settings.getIntProperty( "Carrier" + i + "Osc" + j + "AmpModel" );
				float oscAmpBase, oscAmpPeak;
				if ( oscAmpModel == 2 ) {
					oscAmpBase = 1f;
					oscAmpPeak = 0f;
				} else if ( oscAmpModel == 3 ) {
					oscAmpBase = 1f;
					oscAmpPeak = 1f;
				} else {
					oscAmpBase = 0f;
					oscAmpPeak = 1f;
				}

				// Set and add
				carrier.addFModulator( oscAmp, oscRatio, oscAmpBase, oscAmpPeak, attack, decay, oscSustain, release,
						oscAmpBase );
			}
		}

		// Filter + Envelope
		float filterFreq = settings.getFloatProperty( "FilterFreq" );
		float filterRes = settings.getFloatProperty( "FilterRes" );
		filter = new MoogFilter( filterFreq, filterRes ); // frequency gets properly patched below

		float filterEnvAmt;
		if ( settings.getIntProperty( "FilterEnvOn" ) == 1 ) {
			filterEnvAmt = settings.getFloatProperty( "FilterEnvAmt" );
		} else {
			filterEnvAmt = 0f;
		}
		float filterEnvS = settings.getFloatProperty( "FilterEnvS" );

		filterEnv = new ValuePlusADSR( filterFreq, filterEnvAmt, attack, decay, filterEnvS, release );
		filterEnv.patch( filter.frequency );

		// Output patching
		filter.patch( out );
		ampEnv.patch( filter.audio );
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
			for ( ZeroHzFMCarrier carrier : carriers ) {
				carrier.noteOn();
			}
			ampEnv.noteOn();
			filterEnv.noteOn();
			pitchEnv.noteOn();
			//ampEnv.patchAndUnpatchAfterRelease( filter.audio );
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
			for ( ZeroHzFMCarrier carrier : carriers ) {
				carrier.noteOff();
			}
			ampEnv.noteOff();
			filterEnv.noteOff();
			pitchEnv.noteOff();
		} else if ( currNote == midiNote ) {
			// note removed was currently playing one
			glide.glideTo( Frequency.ofMidiNote( noteStack.get( 0 ) ).asHz() );
		}
	}

	public UGen getOutputUGen() {
		return out;
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
