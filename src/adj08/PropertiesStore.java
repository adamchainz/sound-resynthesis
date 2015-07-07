package adj08;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.FloatGene;
import org.jgap.impl.IntegerGene;

public class PropertiesStore implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 7235263519492014858L;

	enum ConstraintType {
		INT, FLOAT
	};

	private HashMap<String, Serializable> properties;
	private HashMap<String, Constraint> constraints;

	public PropertiesStore() {
		properties = new HashMap<String, Serializable>();
		constraints = new HashMap<String, Constraint>();
	}

	public PropertiesStore( PropertiesStore defaults ) {
		this();
		properties.putAll( defaults.properties );
		constraints.putAll( defaults.constraints );
	}

	// Own alternate way of adding defaults to an object if they don't exist...
	public void setDefaults( PropertiesStore defaults ) {

		// Take all non-set real properties from Defaults
		Iterator<String> it1 = defaults.properties.keySet().iterator();
		while ( it1.hasNext() ) {
			String key = (String) it1.next();
			if ( !properties.containsKey( key ) ) {
				properties.put( key, defaults.properties.get( key ) );
			}
		}

		// Take all constraints from Defaults
		constraints.putAll( defaults.constraints );

		// Apply all constraints
		for ( String key : defaults.constraints.keySet() ) {
			if ( properties.containsKey( key ) ) {
				Constraint cons = defaults.constraints.get( key );
				switch ( cons.type ) {
					case FLOAT:
						Float fValue = getFloatProperty( key );
						fValue = constrain( cons, fValue );
						setProperty( key, fValue );
						break;

					case INT:
						Integer iValue = getIntProperty( key );
						iValue = constrain( cons, iValue );
						setProperty( key, iValue );
						break;
				}
			}
		}
	}

	//
	// Set functions
	//
	public void setProperty( String key, float value ) {
		properties.put( key, (Float) value );
	}

	public void setProperty( String key, int value ) {
		properties.put( key, (Integer) value );
	}

	public void setProperty( String key, int value, int min, int max ) {
		constraints.put( key, new Constraint( ConstraintType.INT, min, max ) );
		value = constrain( key, value );
		setProperty( key, value );
	}

	public void setProperty( String key, float value, float min, float max ) {
		constraints.put( key, new Constraint( ConstraintType.FLOAT, min, max ) );
		value = constrain( key, value );
		setProperty( key, value );
	}

	public void randomizeAll() {
		// Get alphabetical list of keys
		List<String> keys = new LinkedList<String>();
		for ( String key : properties.keySet() ) {
			keys.add( key );
		}
		Collections.sort( keys );

		//
		for ( String key : keys ) {
			Constraint c = constraints.get( key );
			if ( c.type == ConstraintType.FLOAT ) {
				float random = (float) Math.random();
				float val = ( (Float) c.min ) + random * ( ( (Float) c.max ) - ( (Float) c.min ) );
				setProperty( key, val );
			} else if ( c.type == ConstraintType.INT ) {
				float random = (float) Math.random();
				int val = Math.round( ( (Integer) c.min ) + random * ( ( (Integer) c.max ) - ( (Integer) c.min ) ) );
				setProperty( key, val );
			}
		}

	}

	//
	// Get functions
	//
	public float getFloatProperty( String key ) {
		Float p = (Float) properties.get( key );
		if ( p == null )
			p = 0.0f;
		return (float) p;
	}

	public int getIntProperty( String key ) {
		Integer p = (Integer) properties.get( key );
		if ( p == null )
			p = 0;
		return (int) p;
	}

	//
	// Get constraints functions
	//
	public float getFloatMax( String key ) {
		Constraint cons = constraints.get( key );
		float max = Float.POSITIVE_INFINITY;
		if ( cons != null ) {
			max = (Float) cons.max;
		}
		return max;
	}

	public float getFloatMin( String key ) {
		Constraint cons = constraints.get( key );
		float min = Float.NEGATIVE_INFINITY;
		if ( cons != null ) {
			min = (Float) cons.min;
		}
		return min;
	}

	public ConstraintType getPropertyType( String key ) {
		Constraint cons = constraints.get( key );
		if (cons != null) {
			return cons.type;
		} else {
			return null;
		}
	}

	//
	// Constraining functions
	//
	private float constrain( Constraint cons, float value ) {
		if ( cons != null ) {
			value = Math.max( value, (Float) cons.min );
			value = Math.min( value, (Float) cons.max );
		}
		return value;
	}

	private int constrain( Constraint cons, int value ) {
		if ( cons != null ) {
			value = Math.max( value, (Integer) cons.min );
			value = Math.min( value, (Integer) cons.max );
		}
		return value;
	}

	private float constrain( String key, float value ) {
		Constraint cons = constraints.get( key );
		return constrain( cons, value );
	}

	private int constrain( String key, int value ) {
		Constraint cons = constraints.get( key );
		return constrain( cons, value );
	}

	//
	// Constraint class
	//
	private class Constraint implements Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -969067071665476078L;
		ConstraintType type;
		Object min, max;

		public Constraint( ConstraintType type, Object min, Object max ) {
			super();
			this.type = type;
			this.min = min;
			this.max = max;
		}
	}

	//
	// Genetic Conversion
	//

	public Gene[] toGeneArray( Configuration conf ) throws InvalidConfigurationException {
		Gene[] genes = new Gene[properties.size()];

		// Get alphabetical list of keys
		List<String> keys = new LinkedList<String>();
		for ( String key : properties.keySet() ) {
			keys.add( key );
		}
		Collections.sort( keys );

		// Turn into genes
		for ( int i = 0; i < genes.length; i++ ) {
			String key = keys.get( i );
			Constraint c = constraints.get( key );
			if ( c == null ) {
				// No constraints ? interesting.. make a float gene with no bounds
				genes[i] = new FloatGene( conf );
			} else {
				// Make float or int gene
				switch ( c.type ) {
					case FLOAT:
						genes[i] = new FloatGene( conf, (Float) c.min, (Float) c.max );
						break;
					case INT:
						genes[i] = new IntegerGene( conf, (Integer) c.min, (Integer) c.max );
						break;
				}
				// Add in string about name
				genes[i].setApplicationData( key );
			}
		}

		return genes;
	}

	//
	// Meta IO functions
	//
	public String toString() {
		String ret = "";
		// Get alphabetical list of keys
		List<String> keys = new LinkedList<String>();
		for ( String key : properties.keySet() ) {
			keys.add( key );
		}
		Collections.sort( keys );

		// Turn into genes
		for ( String key : keys ) {
			ret = ret + key + " = " + properties.get( key ).toString() + "\n";
			// Constraint c = constraints.get( key );
			// switch ( c.type ) {
			// case FLOAT:
			// ret += getFloatProperty( key );
			// break;
			// case INT:
			// ret += getIntProperty( key );
			// break;
			// }
		}

		return ret;
	}

	public static void saveFile( PropertiesStore conf, String filename ) {
		try {
			FileOutputStream fos = new FileOutputStream( filename );
			ObjectOutputStream out = new ObjectOutputStream( fos );
			out.writeObject( conf );
			out.close();
		} catch ( IOException ex ) {
			ex.printStackTrace();
		}
	}

	public static PropertiesStore loadFile( String filename ) {
		PropertiesStore conf = null;
		try {
			FileInputStream fis = new FileInputStream( filename );
			ObjectInputStream in = new ObjectInputStream( fis );
			conf = (PropertiesStore) in.readObject();
			in.close();
		} catch ( IOException ex ) {
			ex.printStackTrace();
		} catch ( ClassNotFoundException ex ) {
			ex.printStackTrace();
		}

		return conf;
	}
}
