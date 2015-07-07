package ddf.minim.ugens;

public class Sine extends UGen implements TunedUgen {
	private float amp;
	private float frequency;
	private float stepSize;
	private float step;
	private float oneOverSampleRate;

	public Sine( float frequency, float amp ) {
		this.frequency = frequency;
		this.amp = amp;
		step = 0.0f;
	}

	public Sine( float frequency ) {
		this( frequency, 1.0f );
	}

	public void sampleRateChanged() {
		oneOverSampleRate = 1 / sampleRate();
		stepSize = frequency * oneOverSampleRate;
	}

	@Override
	public void setFrequency( float freq ) {
		frequency = freq;
		stepSize = frequency * oneOverSampleRate;
	}

	@Override
	public float getFrequency() {
		return frequency;
	}

	@Override
	protected void uGenerate( float[] channels ) {
		float val = amp * (float) ( Math.sin( 2 * Math.PI * step ) );
		if (!Float.isInfinite( stepSize ) && !Float.isNaN( stepSize ))
			step += stepSize;
		step = step % 1.0f;
		for ( int i = 0; i < channels.length; i++ ) {
			channels[i] = val;
		}
	}
}
