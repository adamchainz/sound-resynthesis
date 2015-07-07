package ddf.minim.ugens;

public class SinePlusADSR extends UGen implements TunedUgen, NotedUGen {
	private float frequency = 440.0f;
	private float stepSize = 0.1f;
	private float step = 0.0f;
	private float oneOverSampleRate = 1.0f;
	private ADSRRepeatable ampEnv;
	private UGenInput amplitude;
	private UGenInput frequencyInput;

	public SinePlusADSR( float frequency, float initalAmp, float peakAmp, float attack, float decay, float sustain,
			float release, float releaseAmp ) {
		setFrequency( frequency );

		// Inputs
		amplitude = new UGenInput();
		frequencyInput = new UGenInput();

		// Amp envelope + patching
		Constant c = new Constant( 1.0f );
		ampEnv = new ADSRRepeatable( peakAmp, attack, decay, sustain, release, initalAmp, releaseAmp );
		c.patch( ampEnv );
		ampEnv.patch( amplitude );
	}

	public SinePlusADSR( float frequency, float amp, float attack, float decay, float sustain, float release ) {
		this( frequency, 0.0f, amp, attack, decay, sustain, release, 0.0f );
	}

	public SinePlusADSR( float frequency ) {
		this( frequency, 1.0f, 0.005f, 0.5f, 0.9f, 0.05f );
	}

	public SinePlusADSR( float frequency, float attack, float decay, float sustain, float release ) {
		this( frequency, 1.0f, attack, decay, sustain, release );
	}

	public void sampleRateChanged() {
		oneOverSampleRate = 1 / sampleRate();
		stepSize = frequency * oneOverSampleRate;
	}

	@Override
	public void setFrequency( float freq ) {
		frequency = freq;
		if ( !Float.isInfinite( freq ) && !Float.isNaN( freq ) ) {
			stepSize = frequency * oneOverSampleRate;
		} else {
			stepSize = 0.0f;
		}
	}

	@Override
	public float getFrequency() {
		return frequency;
	}

	@Override
	public void noteOff() {
		ampEnv.noteOff();
	}

	@Override
	public void noteOn() {
		ampEnv.noteOn();
	}

	@Override
	protected void uGenerate( float[] channels ) {
		// if something is plugged into frequency, follow it
		if ( frequencyInput.isPatched() ) {
			setFrequency( frequencyInput.getLastValues()[0] );
		}

		// Calculate Sin Value.
		float val = (float) ( Math.sin( 2 * Math.PI * step ) );

		// Step
		if ( !Float.isInfinite( stepSize ) && !Float.isNaN( stepSize ) )
			step += stepSize;
		step = step % 1.0f;

		// Multiply by amplitude
		float amp = ampEnv.getLastValues()[0];
		val = val * amp;

		// Output
		for ( int i = 0; i < channels.length; i++ ) {
			channels[i] = val;
		}
	}
}
