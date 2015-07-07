package adj08.utilities;

import adj08.PropertiesStore;

public class InstrumentInspector {
	public static void main( String[] args ) {
		String filename = "testData/it2-results/test6sine.javaobj";//"best_individual.javaobj";
		PropertiesStore conf = PropertiesStore.loadFile( filename );
		System.out.println(conf.toString());
	}
}
