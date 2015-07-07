package adj08.tests;

import adj08.MIDIInstrument;
import adj08.MIDIInstrumentOutputRecorder;
import adj08.MonoWaveformManipulator;
import adj08.Spectrogram;

public class OutputRecorderTest {
	public static void main( String[] args ) {
		MIDIInstrument testInstrument;
		testInstrument = new DFMTestInstrument();
		MIDIInstrumentOutputRecorder record = new MIDIInstrumentOutputRecorder( testInstrument );

		float[] outputMono = record.sampleRecordMono( 5.0f, 60 );

		MonoWaveformManipulator.writeWav( outputMono, "test.wav", 44100, 32 );

		Spectrogram spec = Spectrogram.generateFromMonoWaveBuffer( outputMono, 512 );

	}
}
