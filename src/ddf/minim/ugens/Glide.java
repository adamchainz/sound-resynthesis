package ddf.minim.ugens;


public class Glide extends UGen {

	// the current size of the step
	private float timeStepSize;

	private float currFreq = 440.0f;
	private float destFreq = 440.0f;
	private float glideTime;

	private float timeFromStart = 0.0f;
	private int numSteps;

	public Glide( float startFreq, float glideTime ) {
		this.glideTime = glideTime;
		restart( startFreq );
	}

	public Glide( float startFreq ) {
		this( startFreq, 0.107f ); // default : 107ms
	}

	public void restart( float freq ) {
		currFreq = freq;
		destFreq = freq;
	}

	public void glideTo( float destFreq ) {
		timeFromStart = 0.0f;
		this.destFreq = destFreq;
		numSteps = 0;
	}

	/**
	 * Use this method to notify the ADSR that the sample rate has changed.
	 */
	@Override
	protected void sampleRateChanged() {
		timeStepSize = 1.0f / ( (float) sampleRate() );
	}

	@Override
	protected void uGenerate( float[] channels ) {
		if ( currFreq != destFreq ) {
			// gliding...
			float timeRemain = ( glideTime - timeFromStart );
			if ( timeRemain < timeStepSize ) {
				currFreq = destFreq;
			} else {
				int stepsRem = (int) ( timeRemain / timeStepSize );
				float freqDiff = destFreq - currFreq;
				currFreq += freqDiff / stepsRem;
				numSteps++;
			}
			timeFromStart += timeStepSize;
		} else if ( numSteps > 0 ) {
			numSteps = 0;
		}
		for ( int i = 0; i < channels.length; i++ ) {
			channels[i] = currFreq;
		}
	}

}
