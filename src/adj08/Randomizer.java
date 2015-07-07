package adj08;
import java.util.Random;


public class Randomizer {
	private static Random r;

	public static Random getRandom() {
		if (r==null)
			r = new Random( System.nanoTime() );
		return r;
	}

	public static int getInt( int max ) {
		return getRandom().nextInt( max );
	}

	public static float getFloat() {
		return getRandom().nextFloat();
	}

}
