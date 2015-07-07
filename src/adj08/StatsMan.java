package adj08;

import java.util.List;

public class StatsMan {
	public static double meanDoubleList( List<Double> list ) {
		double sum = 0d;
		for ( double d : list ) {
			sum += d;
		}
		return sum / list.size();
	}

	public static double meanDoubleArray( double[] array ) {
		double sum = 0d;
		int num = array.length;
		for ( int i = 0; i < array.length; i++ ) {
			if (Double.isNaN( array[i] ) || Double.isInfinite( array[i] ))
				num--;
			else
				sum += array[i];
		}
		return sum / num;
	}

	public static double maxDoubleArray( double[] array ) {
		double max = Double.NEGATIVE_INFINITY;
		for ( int i = 0; i < array.length; i++ ) {
			if ( !Double.isNaN( array[i] ) && array[i] > max )
				max = array[i];
		}
		return max;
	}

	public static double minDoubleArray( double[] array ) {
		double min = Double.POSITIVE_INFINITY;
		for ( int i = 0; i < array.length; i++ ) {
			if ( !Double.isNaN( array[i] ) && array[i] < min )
				min = array[i];
		}
		return min;
	}
}
