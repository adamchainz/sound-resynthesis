package adj08;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.FitnessFunction;
import org.jgap.Gene;
import org.jgap.Genotype;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.MinimizingFitnessEvaluator;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;
import org.jgap.event.EventManager;
import org.jgap.impl.EliteTournamentCrossoverer;
import org.jgap.impl.FloatGene;
import org.jgap.impl.GABreeder;
import org.jgap.impl.IntegerGene;
import org.jgap.impl.MutationOperator;
import org.jgap.impl.StockRandomGenerator;

import au.com.bytecode.opencsv.CSVWriter;

public class GeneticSynth extends Configuration {
	/**
	 *
	 */
	private static final long serialVersionUID = 6327085287583505383L;

	// Configuration
	private PropertiesStore conf;
	private int populationSize;
	private int numGenerations;
	private int sampleRate;
	private int windowLength;
	private boolean normalize;
	private String outputDirectory;
	private NaturalSelectorExt selector;

	// Target sound description
	float[] targetSoundBuffer;
	float targetSoundLength;
	int targetMIDINote;
	Spectrogram targetSpectrogram;

	// Statistics
	double[][] stat_fitness;
	double[][] stat_norms;
	double[][] stat_centroids;

	// Population
	private Genotype genotype;

	// Default configuration
	public static PropertiesStore getDefaults() {
		PropertiesStore conf = new PropertiesStore();

		// GA parameters
		conf.setProperty( "PopulationSize", 50, 5, 1000 );
		conf.setProperty( "NumGenerations", 30, 1, 1000 );
		conf.setProperty( "MutationRate", 200, 2, 100000 );
		conf.setProperty( "ElitismPercentage", 0.1f, 0f, 0.9f );
		conf.setProperty( "TournamentSize", 5, 1, 50 );

		// Fitness Function parameters
		conf.setProperty( "TwoPartFitnessWeighting", 0.5f, 0f, 1f );

		// Synth/Audio parameters
		conf.setProperty( "WindowLength", 8192, 512, 16384 );
		conf.setProperty( "SampleRate", 44100, 11025, 200000 );
		conf.setProperty( "Normalize", 1, 0, 1 );

		// Output parameters
		conf.setProperty( "SaveFinalGen", 0, 0, 1 );

		return conf;
	}

	// Constructor
	public GeneticSynth( float[] targetSoundBuffer, int midiNoteNumber ) {
		this( getDefaults(), targetSoundBuffer, midiNoteNumber, "" );
	}

	public GeneticSynth( PropertiesStore a_conf, float[] targetSoundBuffer, int midiNoteNumber, String outputDirectory ) {
		// Super Call
		super( "GeneticSynth", "" );

		// Take arguments
		conf = a_conf;
		conf.setDefaults( getDefaults() );
		sampleRate = conf.getIntProperty( "SampleRate" );
		normalize = conf.getIntProperty( "Normalize" ) == 1;
		windowLength = conf.getIntProperty( "WindowLength" );
		this.outputDirectory = outputDirectory;
		numGenerations = conf.getIntProperty( "NumGenerations" );
		populationSize = conf.getIntProperty( "PopulationSize" );

		// If output directory is not dir or empty, error
		if ( !outputDirectory.equals( "" ) ) {
			File outputDir = new File( outputDirectory );
			if ( !outputDir.isDirectory() ) {
				outputDir.mkdirs();
				// System.err.println( "outputdir is not a directory!" );
				// System.exit( -1 );
			}
			if ( outputDir.list().length > 0 ) {
				System.err.println( "Outputdir is not empty, I can't do this!" );
				System.exit( -1 );
			}
		}

		// Target sound
		this.targetSoundBuffer = targetSoundBuffer;
		if ( normalize )
			MonoWaveformManipulator.normalize( targetSoundBuffer );
		this.targetMIDINote = midiNoteNumber;
		targetSoundLength = ( (float) targetSoundBuffer.length ) / ( (float) sampleRate );
		targetSpectrogram = Spectrogram.generateFromMonoWaveBuffer( targetSoundBuffer, windowLength, sampleRate );

		// Create self as a config.
		try {
			// Setup : vanilla keep pop same size
			setBreeder( new GABreeder() );
			setRandomGenerator( new StockRandomGenerator() );
			setEventManager( new EventManager() );
			setFitnessEvaluator( new MinimizingFitnessEvaluator() );
			setMinimumPopSizePercent( 0 );
			setSelectFromPrevGen( 1.0d );
			setKeepPopulationSizeConstant( true );
			setPopulationSize( populationSize );

			// Selector
			float elitismPercentage = conf.getFloatProperty( "ElitismPercentage" );
			int tournamentSize = conf.getIntProperty( "TournamentSize" );
			selector = new EliteTournamentCrossoverer( this, elitismPercentage, tournamentSize );
			selector.setDoubletteChromosomesAllowed( true );
			addNaturalSelector( selector, false );

			// Other Genetic Operators
			addGeneticOperator( new MutationOperator( this, conf.getIntProperty( "MutationRate" ) ) );

			// Chromosome derived from synthesizer configuration
			Gene[] genes = DFMSynthInstrument.getDefaults().toGeneArray( this );
			Chromosome chromo = new Chromosome( this, genes );
			setSampleChromosome( chromo );

			// Fitness Function
			FitnessFunction myFunc = new TwoPartFitness( conf.getFloatProperty( "TwoPartFitnessWeighting" ) );
			setFitnessFunction( myFunc );

		} catch ( InvalidConfigurationException e ) {
			throw new RuntimeException( "Fatal error: Configuration class could not use its "
					+ "own stock configuration values. This should never happen. "
					+ "Please report this as a bug to the JGAP team." );
		}

		// Statistics
		stat_fitness = new double[numGenerations][populationSize];
		stat_norms = new double[numGenerations][populationSize];
		stat_centroids = new double[numGenerations][populationSize];
	}

	// Step 1 : init
	public void init() throws InvalidConfigurationException {
		genotype = Genotype.randomInitialGenotype( this );
	}

	// Step 2 : runnnn
	@SuppressWarnings( "unchecked" )
	public void run() {
		// System.out.println("fitty " + genotype.getPopulation().getChromosome( 0 ).getFitnessValue());

		// Do all generations
		for ( int gen = 0; gen < numGenerations; gen++ ) {
			System.out.println( "Generation " + gen );
			genotype.evolve();

			// Store stats
			Population pop = genotype.getPopulation();
			for ( int i = 0; i < populationSize; i++ ) {
				IChromosome member = pop.getChromosome( i );
				stat_fitness[gen][i] = member.getFitnessValue();
				HashMap<String, Double> stats = (HashMap<String, Double>) member.getApplicationData();
				if ( stats == null ) {
					stat_norms[gen][i] = Double.NaN;
					stat_centroids[gen][i] = Double.NaN;
				} else {
					stat_norms[gen][i] = stats.get( "norms" );
					stat_centroids[gen][i] = stats.get( "centroids" );
				}
			}

			if ( !outputDirectory.equals( "" ) ) {
				// On the fly saving the best in case terminated
				saveBestWavAndConf( outputDirectory );
			}
		}

		if ( !outputDirectory.equals( "" ) ) {
			System.out.println( "Saving statistics..." );
			// Store everything!
			savePropertiesStore( this.conf, outputDirectory, "this_conf.txt" );
			saveBestWavAndConf( outputDirectory );
			if (conf.getIntProperty( "SaveFinalGen" ) == 1)
				savePopulation( genotype.getPopulation(), outputDirectory + "final_gen/" );
			try {
				saveStatistics( outputDirectory, "stats.csv" );
			} catch ( IOException e ) {
				e.printStackTrace();
				System.exit( -1 );
			}
			System.out.println( "Done." );
		}
	}

	// Conversion functions
	public PropertiesStore getConfForBestIndividual() {
		IChromosome best = genotype.getFittestChromosome();
		return toPropertiesStore( best );
	}

	public Spectrogram getSpectrogramForBestIndividual() {
		PropertiesStore bestConf = getConfForBestIndividual();
		return generateForConf( bestConf );
	}

	public float[] generateWaveformForConf( PropertiesStore conf ) {
		// Make instrument
		DFMSynthInstrument indInstr = new DFMSynthInstrument( conf );

		// Record into wav buffer
		MIDIInstrumentOutputRecorder indRecorder = new MIDIInstrumentOutputRecorder( indInstr, sampleRate );
		float[] record = indRecorder.sampleRecordMono( targetSoundLength, targetMIDINote );
		return record;
	}

	private Spectrogram generateForConf( PropertiesStore conf ) {
		float[] indBuffer = generateWaveformForConf( conf );
		if ( normalize )
			indBuffer = MonoWaveformManipulator.normalize( indBuffer );

		// Spectrogramise buffer
		Spectrogram indSpectrogram = Spectrogram.generateFromMonoWaveBuffer( indBuffer, windowLength, sampleRate );

		// Return spectrogram
		return indSpectrogram;
	}

	private PropertiesStore toPropertiesStore( IChromosome individual ) {
		// Convert an individual to settings the synth can understand
		PropertiesStore settings = new PropertiesStore();

		Gene[] genes = individual.getGenes();

		for ( Gene gene : genes ) {
			// Get the strings from the genes' application data
			String key = (String) gene.getApplicationData();
			if ( key != null ) {
				if ( gene instanceof FloatGene ) {
					float val = ( (FloatGene) gene ).floatValue();
					settings.setProperty( key, val );
				} else if ( gene instanceof IntegerGene ) {
					int val = ( (IntegerGene) gene ).intValue();
					settings.setProperty( key, val );
				}
			}
		}

		return settings;
	}

	// Saving Data functions
	private void saveWavAndConf( IChromosome individual, String targetDir, String targetName ) {
		// Make prop store
		PropertiesStore conf = toPropertiesStore( individual );

		// Make wave
		float[] wave = generateWaveformForConf( conf );
		float amp = MonoWaveformManipulator.getNormalizationFactor( wave );
		wave = MonoWaveformManipulator.normalize( wave );
		MonoWaveformManipulator.writeWav( wave, targetDir + targetName + ".wav" );

		// Normalize!
		conf.setProperty( "Amplitude", amp );

		// Save as txt
		savePropertiesStore( conf, targetDir, targetName + ".txt" );

		// Save as java object
		PropertiesStore.saveFile( conf, targetDir + targetName + ".javaobj" );

	}

	public void saveBestWavAndConf( String targetDir ) {
		IChromosome best = genotype.getFittestChromosome();
		saveWavAndConf( best, targetDir, "best_individual" );
	}

	private void savePropertiesStore( PropertiesStore prop, String targetDir, String targetName ) {
		String confoutput = prop.toString();
		PrintWriter out = null;
		try {
			out = new PrintWriter( targetDir + targetName );
		} catch ( FileNotFoundException e ) {
			System.err.println( "Can't write conf out!" );
			System.exit( -1 );
		}

		out.println( confoutput );
		out.close();
	}

	private void savePopulation( Population pop, String targetDir ) {
		// Touch directory
		File target = new File( targetDir );
		target.mkdirs();

		// Sort pop by error ascending = fitness descending
		List<IChromosome> sortedpop = new Vector<IChromosome>();

		for ( int i = 0; i < pop.size(); i++ ) {
			sortedpop.add( pop.getChromosome( i ) );
		}

		Collections.sort( sortedpop, selector.new FitnessValueComparator() );

		int i = 0;
		for ( Iterator iterator = sortedpop.iterator(); iterator.hasNext(); ) {
			IChromosome member = (IChromosome) iterator.next();

			// Save each member
			saveWavAndConf( member, targetDir, "member" + i );
			i++;
		}

	}

	private void saveStatistics( String outputDirectory, String filename ) throws IOException {
		CSVWriter csvWrite = null;
		csvWrite = new CSVWriter( new FileWriter( outputDirectory + filename ) );

		String[] header = { "Generation", "Min Fitness", "Mean Fitness", "Max Fitness", "Min Norms", "Mean Norms",
				"Max Norms", "Min Centroids", "Mean Centroids", "Max Centroids" };
		csvWrite.writeNext( header );

		for ( int i = 0; i < numGenerations; i++ ) {
			Double minFitness = StatsMan.minDoubleArray( stat_fitness[i] );
			Double meanFitness = StatsMan.meanDoubleArray( stat_fitness[i] );
			Double maxFitness = StatsMan.maxDoubleArray( stat_fitness[i] );

			Double minNorms = StatsMan.minDoubleArray( stat_norms[i] );
			Double meanNorms = StatsMan.meanDoubleArray( stat_norms[i] );
			Double maxNorms = StatsMan.maxDoubleArray( stat_norms[i] );

			Double minCentroids = StatsMan.minDoubleArray( stat_centroids[i] );
			Double meanCentroids = StatsMan.meanDoubleArray( stat_centroids[i] );
			Double maxCentroids = StatsMan.maxDoubleArray( stat_centroids[i] );

			String[] line = new String[10];
			line[0] = Integer.toString( i );
			line[1] = minFitness.toString();
			line[2] = meanFitness.toString();
			line[3] = maxFitness.toString();
			line[4] = minNorms.toString();
			line[5] = meanNorms.toString();
			line[6] = maxNorms.toString();
			line[7] = minCentroids.toString();
			line[8] = meanCentroids.toString();
			line[9] = maxCentroids.toString();

			csvWrite.writeNext( line );
		}

		csvWrite.close();
	}

	// Fitness Class
	private class TwoPartFitness extends FitnessFunction {

		private static final long serialVersionUID = -3752825286026475128L;

		private float weighting;

		public TwoPartFitness( float weighting ) {
			this.weighting = weighting;
		}

		@Override
		protected double evaluate( IChromosome individual ) {
			System.out.print( "Comparing with original.." );

			// Create config based on this chromosome
			PropertiesStore indConf = toPropertiesStore( individual );
			PropertiesStore.saveFile( indConf, "problem_instrument.javaobj" );

			// Make instrument and spectrogram
			Spectrogram indSpectrogram = generateForConf( indConf );

			// Silent sound = Infinite Error!
			if ( indSpectrogram.isSilent() ) {
				System.out.println( " error = INFINITE (silent sound)" );
				return Float.POSITIVE_INFINITY;
			}

			// Get two metrics
			double normError = targetSpectrogram.spectralNormError( indSpectrogram );
			double centroidsError = targetSpectrogram.centroidsError( indSpectrogram );

			// Attach to chromosome
			HashMap<String, Double> stats = new HashMap<String, Double>();
			stats.put( "norms", normError );
			stats.put( "centroids", centroidsError );
			individual.setApplicationData( stats );

			// Return = weighted sum
			double error = weighting * normError + ( 1 - weighting ) * centroidsError;
			System.out.println( " error = " + error );
			if ( Double.isNaN( error ) ) {
				System.err.println( "Fitness function output = NaN error, sorry." );
				System.out.println( indConf.toString() );
				System.exit( -1 );
			}
			return error;
		}

	}

}
