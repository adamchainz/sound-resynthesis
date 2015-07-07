package ddf.minim.ugens;

public class ValuePlusADSRPercentage extends UGen implements NotedUGen {

	private float defaultV;
	private float envelopePercentage;
	private ADSRRepeatable envelope;
	public UGenInput envelopeIn;
	public UGenInput defaultValue;

	public ValuePlusADSRPercentage( float defaultValue, float envelopePercentage, float attack, float decay, float sustain,
			float release ) {
		this.defaultV = defaultValue;
		this.envelopePercentage = envelopePercentage;
		this.envelope = new ADSRRepeatable( 1.0f, attack, decay, sustain, release );
		Constant c = new Constant( 1.0f );
		c.patch( envelope );
		envelopeIn = new UGenInput( InputType.CONTROL );
		envelope.patch( envelopeIn );
		this.defaultValue = new UGenInput( InputType.CONTROL );
	}

	@Override
	public void noteOn() {
		envelope.noteOn();
	}

	@Override
	public void noteOff() {
		envelope.noteOff();
	}

	@Override
	protected void uGenerate( float[] channels ) {
		if ( defaultValue.isPatched() ) {
			defaultV = defaultValue.getLastValues()[0];
		}
		float[] envelopeChannels = envelope.getLastValues();
		float value = defaultV * (100 + envelopePercentage * envelopeChannels[0])/100;
		for ( int i = 0; i < channels.length; i++ ) {
			channels[i] = value;
		}
	}

}
