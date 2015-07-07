package adj08;

import adj08.MIDIInstrument;
import ddf.minim.ugens.UGen;

public class MIDIInstrumentOutputRecorder {
	private MIDIInstrument instrument;
	public UGen audio;
	private int sampleRate;

	public MIDIInstrumentOutputRecorder( MIDIInstrument instr, int sampleRate ) {
		instrument = instr;
		this.sampleRate = sampleRate;
		audio = instrument.getOutputUGen();
	}

	public float[][] sampleRecord( float lengthInSeconds, int noteNumber ) {
		return sampleRecord( lengthInSeconds, noteNumber, 1 );
	}

	public float[][] sampleRecord( float lengthInSeconds, int noteNumber, int numChannels ) {
		int numSamples = (int) ( sampleRate * lengthInSeconds );

		float[] channels = new float[numChannels];
		float[][] output = new float[numSamples][numChannels];

		// Set sample rate for output
		audio.setSampleRate( sampleRate );

		// Tick 50ms of silence to get chain "warmed up" and prevent filter instability
		for ( int i = 0; i < sampleRate * 0.05; i++ ) {
			// Reset channels
			for ( int j = 0; j < channels.length; j++ ) {
				channels[j] = 0.0f;
			}
			audio.tick( channels );
			channels = audio.getLastValues();
		}

		// calculate note off time
		float noteOffTime = lengthInSeconds - instrument.getReleaseTime();
		noteOffTime = Math.max( noteOffTime, instrument.getAttackTime() );
		int noteOffSample = (int) Math.ceil( noteOffTime * sampleRate );

		// send MIDI note on
		instrument.midiNoteOn( noteNumber, 100 );

		// get Audio samples
		for ( int i = 0; i < numSamples; i++ ) {
			// Reset channels
			for ( int j = 0; j < channels.length; j++ ) {
				channels[j] = 0.0f;
			}

			// Turn instrument off if we're at the right sample
			if ( i == noteOffSample ) {
				instrument.midiNoteOff( noteNumber, 100 );
			}

			// Tick chain and put new samples in to output
			audio.tick( channels );
			channels = audio.getLastValues();
			for ( int j = 0; j < numChannels; j++ ) {
				output[i][j] = channels[j];
			}
		}

		return output;
	}

	public float[] sampleRecordMono( float lengthInSeconds, int MIDINoteNumber ) {
		float[][] recording = sampleRecord( lengthInSeconds, MIDINoteNumber );
		float[] mono = MonoWaveformManipulator.toMono( recording );
		return mono;
	}

}
