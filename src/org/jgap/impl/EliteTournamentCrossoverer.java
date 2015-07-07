package org.jgap.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.ICompositeGene;
import org.jgap.InvalidConfigurationException;
import org.jgap.NaturalSelectorExt;
import org.jgap.Population;
import org.jgap.RandomGenerator;

public class EliteTournamentCrossoverer extends NaturalSelectorExt {
	/**
	 * Control the selectionism
	 */
	private float elitismRate;
	private int tournamentSize;

	/**
	 * Stores the chromosomes to be taken into account for selection
	 */
	private List<IChromosome> m_chromosomes;

	/**
	 * Indicated whether the list of added chromosomes needs sorting
	 */
	private boolean m_needsSorting;

	/**
	 * Comparator that is only concerned about fitness values
	 */
	private FitnessValueComparator m_fitnessValueComparator;

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private RandomGenerator random;

	public EliteTournamentCrossoverer( Configuration aConfig ) throws InvalidConfigurationException {
		this( aConfig, 0.1f, 5 );
	}

	public EliteTournamentCrossoverer( Configuration aConfig, float elitismRate, int tournamentSize )
			throws InvalidConfigurationException {
		super( aConfig );
		m_chromosomes = new Vector<IChromosome>();
		m_needsSorting = false;
		m_fitnessValueComparator = new FitnessValueComparator();
		random = getConfiguration().getRandomGenerator();
		this.elitismRate = elitismRate;
		this.tournamentSize = tournamentSize;
	}

	@Override
	protected void add( IChromosome aChromosomeToAdd ) {
		m_chromosomes.add( aChromosomeToAdd );
		m_needsSorting = true;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public void selectChromosomes( int a_howManyToSelect, Population a_destPop ) {
		// Calculate number to put in pop
		int canBeSelected;
		if ( a_howManyToSelect > m_chromosomes.size() ) {
			canBeSelected = m_chromosomes.size();
		} else {
			canBeSelected = a_howManyToSelect;
		}

		// Sort the collection of chromosomes previously added for evaluation.
		// Only do this if necessary.
		if ( m_needsSorting ) {
			Collections.sort( m_chromosomes, m_fitnessValueComparator );
			m_needsSorting = false;
		}

		// Select the elites to just copy over
		int eliteNumber = (int)  (a_howManyToSelect * elitismRate);
		eliteNumber = Math.min( eliteNumber, canBeSelected );
		for ( int i = 0; i < eliteNumber; i++ ) {
			IChromosome elite = (IChromosome) m_chromosomes.get( i ).clone();
			a_destPop.addChromosome( elite  );
		}

		// Fill up the rest with tournaments
		int missing = a_howManyToSelect - eliteNumber;
		doTournaments( a_destPop, missing );
	}

	private void doTournaments( Population a_toPop, int missing ) {
		for ( int i = 0; i < missing; i++ ) {
			// Selections
			IChromosome parent1 = doTournament( tournamentSize );
			IChromosome parent2 = doTournament( tournamentSize );

			// Cloning
			parent1 = (IChromosome) parent1.clone();
			parent2 = (IChromosome) parent2.clone();

			// Creating baby
			a_toPop.addChromosome( doCrossover( parent1, parent2 ) );
		}
	}

	private IChromosome doTournament( int a_tournamentSize ) {
		// Pick 5 randomly
		IChromosome[] contestants = new IChromosome[a_tournamentSize];
		for ( int i = 0; i < a_tournamentSize; i++ ) {
			int randomC = random.nextInt( m_chromosomes.size() );
			contestants[i] = (IChromosome) m_chromosomes.get( randomC );
		}
		// Find best
		int best = 0;
		for ( int i = 1; i < a_tournamentSize; i++ ) {
			if ( m_fitnessValueComparator.compare( contestants[best], contestants[i] ) == 1 )
				best = i;
		}
		return contestants[best];
	}

	// 2-point crossoverer
	protected IChromosome doCrossover( IChromosome a_firstMate, IChromosome a_secondMate ) {

		// 1 in 2 chance of selecting parent2 first instead
		if ( random.nextBoolean() ) {
			IChromosome temp = a_firstMate;
			a_firstMate = a_secondMate;
			a_secondMate = temp;
		}

		// Turn into genes
		Gene[] firstGenes = a_firstMate.getGenes();
		Gene[] secondGenes = a_secondMate.getGenes();
		int locus = random.nextInt( firstGenes.length );
		int crossoverend = locus + random.nextInt( firstGenes.length - locus );

		// Swap the genes
		Gene gene1;
		Gene gene2;
		Object firstAllele;
		for ( int j = locus; j < crossoverend; j++ ) {
			// Make a distinction for ICompositeGene for the first gene
			int index = 0;
			if ( firstGenes[j] instanceof ICompositeGene ) {
				// Randomly determine gene to be considered
				index = random.nextInt( firstGenes[j].size() );
				gene1 = ( (ICompositeGene) firstGenes[j] ).geneAt( index );
			} else {
				gene1 = firstGenes[j];
			}
			// Make a distinction for the second gene if CompositeGene
			if ( secondGenes[j] instanceof ICompositeGene ) {
				gene2 = ( (ICompositeGene) secondGenes[j] ).geneAt( index );
			} else {
				gene2 = secondGenes[j];
			}
			firstAllele = gene1.getAllele();
			gene1.setAllele( gene2.getAllele() );
			gene2.setAllele( firstAllele );
		}

		// Clear child's Fitness and app data because we're a new baby!
		a_firstMate.setApplicationData( null );
		a_firstMate.setFitnessValueDirectly( -1d );

		return a_firstMate;
	}

	@Override
	public void empty() {
		m_chromosomes.clear();
		m_needsSorting = false;
	}

	@Override
	public boolean returnsUniqueChromosomes() {
		return false;
	}

}
