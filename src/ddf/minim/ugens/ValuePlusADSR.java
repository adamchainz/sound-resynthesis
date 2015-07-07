package ddf.minim.ugens;

public class ValuePlusADSR extends UGen implements NotedUGen {

	private float defaultV;
	private float envelopeAmount;
	private ADSRRepeatable envelope;
	public UGenInput envelopeIn;
	public UGenInput defaultValue;

	public ValuePlusADSR( float defaultValue, float envelopeAmount, float attack, float decay, float sustain,
			float release ) {
		this.defaultV = defaultValue;
		this.envelopeAmount = envelopeAmount;
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
		float value = defaultV + envelopeAmount * envelopeChannels[0];
		for ( int i = 0; i < channels.length; i++ ) {
			channels[i] = value;
		}
	}

}
