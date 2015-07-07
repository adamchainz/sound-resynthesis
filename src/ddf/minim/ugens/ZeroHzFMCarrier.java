package ddf.minim.ugens;

import java.util.ArrayList;
import java.util.List;

import adj08.Randomizer;

public class ZeroHzFMCarrier extends UGen implements TunedUgen, NotedUGen {

	/**
	 * Patch to this to control the frequency of the oscillator with another UGen.
	 */
	public UGenInput frequency;

	// Frequency
	private float freq = 440.0f;

	// Amplitude
	private float amplitude;

	// The modulator sources
	private List<UGenModulator> sources;

	public ZeroHzFMCarrier( float freq, float i1, float i1r, float i2, float i2r ) {
		this();
		// Add the two fm modulators we have been given
		addFModulator( i1, i1r );
		addFModulator( i2, i2r );

		// Frequency setup
		setFrequency( freq );
	}

	public ZeroHzFMCarrier( float amp ) {
		this();
		amplitude = amp;
	}

	// empty multifmosc..
	public ZeroHzFMCarrier() {
		// Set up ourselves
		this.frequency = new UGenInput( InputType.CONTROL );
		sources = new ArrayList<UGenModulator>( 2 );
		amplitude = 1.0f;
	}

	/*
	 * Generate a random multi osc : 1 to many different random sines
	 */
	public static ZeroHzFMCarrier RandomMultiFMOsc() {
		ZeroHzFMCarrier ret = new ZeroHzFMCarrier();

		// Random number of sin modulators
		int numSub = Randomizer.getRandom().nextInt( 5 ) + 1;
		for ( int i = 0; i < numSub; i++ ) {
			// With random values
			float a = Randomizer.getFloat() * 10;
			int r = Randomizer.getInt( 9 ) + 1;
			ret.addFModulator( a, r );
		}
		return ret;
	}

	public void addFModulator( float amp, float ratio ) {
		// fill in default adsr values
		addFModulator( amp, ratio, 0.005f, 0.5f, 0.9f, 0.05f );
	}

	public void addFModulator( float amp, float ratio, float attack, float decay, float sustain, float release ) {
		// Bound ratio to one of the valid values
		ratio = boundRatio( ratio );

		// Make osc
		UGen m = new SinePlusADSR( ratio * freq, attack, decay, sustain, release );
		m.sampleRateChanged();

		// Add to our FM sources
		UGenModulator u = new UGenModulator( m, ratio, amp );
		sources.add( u );
	}

	public void addFModulator( float amp, float ratio, float initialAmp, float peakAmp, float attack, float decay,
			float sustain, float release, float releaseAmp ) {
		// Bound ratio to one of the valid values
		ratio = boundRatio( ratio );

		// Make osc
		UGen m = new SinePlusADSR( ratio * freq, initialAmp, peakAmp, attack, decay, sustain, release, releaseAmp );
		m.sampleRateChanged();

		// Add to our FM sources
		UGenModulator u = new UGenModulator( m, ratio, amp );
		sources.add( u );
	}

	private float boundRatio( float ratio ) {
		float[] validRatios = { 0.25f, 0.5f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f };
		float bestdistance = Float.POSITIVE_INFINITY;
		int bestno = 0;
		for ( int i = 0; i < validRatios.length; i++ ) {
			float dist = Math.abs( ratio - validRatios[i] );
			if ( dist < bestdistance ) {
				bestno = i;
				bestdistance = dist;
			}
		}
		return validRatios[bestno];
	}

	protected void sampleRateChanged() {
		for ( UGenModulator u : sources ) {
			u.ugen.sampleRateChanged();
		}
	}

	public void setFrequency( float hz ) {
		if ( freq != hz ) {
			freq = hz;
			for ( UGenModulator u : sources ) {
				// Re-tune our tuned Ugens
				if ( u.tuned ) {
					( (TunedUgen) u.ugen ).setFrequency( u.ratio * freq );
				}
			}
		}
	}

	public float getFrequency() {
		return freq;
	}

	public void noteOn() {
		for ( UGenModulator u : sources ) {
			// Note-on our noted UGens
			if ( u.noted ) {
				( (NotedUGen) u.ugen ).noteOn();
			}
		}
	}

	public void noteOff() {
		for ( UGenModulator u : sources ) {
			// Note-off our noted UGens
			if ( u.noted ) {
				( (NotedUGen) u.ugen ).noteOff();
			}
		}
	}

	@Override
	protected void uGenerate( float[] channels ) {

		// if something is plugged into frequency
		if ( frequency.isPatched() ) {
			setFrequency( frequency.getLastValues()[0] );
		}

		// Calculate where our oscillators are at
		float sum = 0;

		//System.out.println(" New source sum ");
		for ( UGenModulator u : sources ) {
			// u.ugen.uGenerate( testChannels ); // generate into test channels array
			float[] testChannels = u.ugen.getLastValues();
			//if ( Math.abs(testChannels[0]) < 0.001 ) {
			//	System.err.println("0 source");
			//}
			sum += testChannels[0] * u.amp; // add into our sin summer
		}

		// Do 0Hz Frequency mod, scale by amplitude
		float value = (float) Math.sin( sum ) * amplitude;

		// Set output
		for ( int i = 0; i < channels.length; i++ ) {
			channels[i] = value;
		}
	}

	// Storage for all the details of a UGen modulating our sin signal
	private class UGenModulator {
		UGen ugen;
		UGenInput input;
		float ratio;
		float amp;
		boolean tuned;
		boolean noted;

		public UGenModulator( UGen ugen, float ratio, float amp ) {
			super();
			this.ugen = ugen;
			this.ratio = ratio;
			this.amp = amp;
			this.tuned = ( ugen instanceof TunedUgen );
			this.noted = ( ugen instanceof NotedUGen );

			this.input = new UGenInput( InputType.AUDIO );
			ugen.patch( input );
		}

	}

}
