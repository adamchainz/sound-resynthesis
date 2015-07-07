package adj08;

import java.io.FileInputStream;

import org.jgap.InvalidConfigurationException;

public class MainMatch {

	public static void main( String[] args ) {

		// Configure genetic algorithm
		PropertiesStore conf = GeneticSynth.getDefaults();

		// Take arguments from commandline
		String targetWav = "";
		int midiNote = -1;
		String outputDirectory = "";
		for ( String arg : args ) {
			String[] split = arg.split( "=", 2 );

			// Two essential parameters
			if ( split[0].equals( "target" ) ) {
				targetWav = split[1];
			} else if ( split[0].equals( "midinote" ) ) {
				midiNote = Integer.parseInt( split[1] );
			} else if ( split[0].equals( "output" ) ) {
				outputDirectory = split[1];
			} else {
				// Store in conf!
				PropertiesStore.ConstraintType type = conf.getPropertyType( split[0] );
				if ( type != null ) {
					switch ( type ) {
						case INT:
							conf.setProperty( split[0], Integer.parseInt( split[1] ) );
							break;
						case FLOAT:
							conf.setProperty( split[0], Float.parseFloat( split[1] ) );
							break;
						default:
							break;
					}
				}
			}
		}

		// Error if necessary parameters not specified.
		if ( midiNote == -1 ) {
			System.err.println( "No midi note specified!" );
			System.exit( -1 );
		} else if ( targetWav.equals( "" ) ) {
			System.err.println( "No target wav file specified!" );
			System.exit( -1 );
		} else if ( outputDirectory.equals( "" ) ) {
			System.err.println( "No output directory specified - no output will be stored!" );
		}

		// Load wav file
		float[] wavBuffer = MonoWaveformManipulator.readWav( targetWav );
		if ( wavBuffer == null ) {
			System.err.println( "Cannot read wav file" );
			System.exit( -1 );
		}

		// Create genetic algorithm
		GeneticSynth gSynth = null;
		try {
			gSynth = new GeneticSynth( conf, wavBuffer, midiNote, outputDirectory );
			gSynth.init();
		} catch ( InvalidConfigurationException e1 ) {
			e1.printStackTrace();
			System.exit( -1 );
		}

		// Run run run!
		gSynth.run();

		// Save best individual for inspection
		gSynth.saveBestWavAndConf( "./" );
	}
}
