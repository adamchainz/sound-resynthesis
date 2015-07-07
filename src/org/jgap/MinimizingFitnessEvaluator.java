/*
 * This file is adapted from JGAP
 */
package org.jgap;

import org.jgap.util.*;

/**
 * Lower `fitness' = better
 */
public class MinimizingFitnessEvaluator implements FitnessEvaluator, ICloneable, Comparable {
	/**
	 *
	 */
	private static final long serialVersionUID = 5328094652323943229L;

	/**
	 * Compares the first given fitness value with the second and returns true if the first one is better than the
	 * second one. Otherwise returns false
	 *
	 * @param a_fitness_value1
	 *            first fitness value
	 * @param a_fitness_value2
	 *            second fitness value
	 * @return true: first fitness value greater than second
	 */
	public boolean isFitter( final double a_fitness_value1, final double a_fitness_value2 ) {
		return a_fitness_value1 < a_fitness_value2;
	}

	public boolean isFitter( IChromosome a_chrom1, IChromosome a_chrom2 ) {
		return isFitter( a_chrom1.getFitnessValue(), a_chrom2.getFitnessValue() );
	}

	/**
	 * @return deep clone of this instance
	 *
	 * @author Klaus Meffert
	 * @since 3.2
	 */
	public Object clone() {
		return new MinimizingFitnessEvaluator();
	}

	/**
	 * @param a_other
	 *            sic
	 * @return as always
	 *
	 * @author Klaus Meffert
	 * @since 3.2
	 */
	public int compareTo( Object a_other ) {
		if ( a_other.getClass().equals( getClass() ) ) {
			return 0;
		} else {
			return getClass().getName().compareTo( a_other.getClass().getName() );
		}
	}
}
