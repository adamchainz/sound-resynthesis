# Sound Resynthesis with a Genetic Algorithm

This is my final year project from university, finished in June 2011. It
implements a basic synthesizer that is tuned by a genetic algorithm to
try and match a single-note wave file that it is given.

It's in Java, and I've lost the build scripts. However I can say it was built
with these versions of its dependencies:

* [jgap](http://jgap.sourceforge.net/) version 3.5
* [minim](https://github.com/ddf/Minim) version 2.1 beta
* [processing](https://processing.org/) version unknown, probably the latest
  release from around the start of 2011
* [opencsv](http://opencsv.sourceforge.net/) version 2.3

The main entry point is `adj08/MainMatch.java` but there are also some other
utilities, e.g. to play back dumped `javaobj` files containing generated
synthesizers, in `adj08/tests/` and `adj08/utilities`.

There are some example resyntheses in `demos` - for example,
`oboe-original.wav` is the wav file as given to the system, and
`oboe-resynthesis.wav` is the same note played by the best matched synthesizer
from the genetic algorithm.
