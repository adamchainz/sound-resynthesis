package ddf.minim.ugens;
import ddf.minim.ugens.UGen;

// Moog 24 dB/oct resonant lowpass VCF
// References: CSound source code, Stilson/Smith CCRMA paper.
// Modified by paul.kellett@maxim.abel.co.uk July 2000
// Java implementation by Damien Di Fede September 2010

public class MoogFilter extends UGen {
	public UGenInput audio;
	public UGenInput frequency;
	private float freq;
	public UGenInput resonance;
	private float res;

	private float coeff[][]; // filter buffers (beware denormals!)

	private float constrain( float number, float min, float max ) {
		return Math.max( min, Math.min( number, max ) );
	}

	public MoogFilter( float frequencyInHz, float normalizedResonance ) {
		audio = new UGenInput( InputType.AUDIO );
		frequency = new UGenInput( InputType.CONTROL );
		resonance = new UGenInput( InputType.CONTROL );

		freq = frequencyInHz;
		res = constrain( normalizedResonance, 0.f, 1.f );

		// Working in stereo
	    coeff = new float[2][5];
	}

	protected void uGenerate( float[] channels ) {
		// Get freq and res from patching if necessary
		if ( frequency.isPatched() ) {
			freq = constrain( frequency.getLastValues()[0], 1.0f, (float) ( sampleRate()/2.0 ) );
		}
		if ( resonance.isPatched() ) {
			res = constrain( resonance.getLastValues()[0], 0.f, 1.f );
		}
		// Set coefficients given frequency & resonance [0.0...1.0]
		float t1, t2; // temporary buffers
		float normFreq = freq / ( sampleRate() * 0.5f );
		float rez = res;

		float q = 1.0f - normFreq;
		float p = normFreq + 0.8f * normFreq * q;
		float f = p + p - 1.0f;
		q = rez * ( 1.0f + 0.5f * q * ( 1.0f - q + 5.6f * q * q ) );

		if ( audio == null || !audio.isPatched() ) {
			for ( int i = 0; i < channels.length; i++ ) {
				channels[i] = 0.0f;
			}
			return;
		}

		float[] input = audio.getLastValues();

		for ( int i = 0; i < channels.length; ++i ) {
			// Filter (in [-1.0...+1.0])
			float[] b = coeff[i];
			float in = input[i];

			in -= q * b[4]; // feedback

			t1 = b[1];
			b[1] = ( in + b[0] ) * p - b[1] * f;

			t2 = b[2];
			b[2] = ( b[1] + t1 ) * p - b[2] * f;

			t1 = b[3];
			b[3] = ( b[2] + t2 ) * p - b[3] * f;

			b[4] = ( b[3] + t1 ) * p - b[4] * f;
			b[4] = b[4] - b[4] * b[4] * b[4] * 0.166667f; // clipping

			b[0] = in;

			channels[i] = b[4];
		}
	}
}
