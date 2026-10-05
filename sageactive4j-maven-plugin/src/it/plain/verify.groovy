def log = new File(basedir, 'build.log').text
assert log.contains("Detected framework 'plain'")
assert !log.contains('Add the dependency')

assert !new File(basedir, 'src/main/resources/sageactive4j.properties').exists()
// Compiled at --release 8 by the `compile` goal that ran after `init`.
assert new File(basedir, 'target/classes/sageactive4j/SageActiveExample.class').isFile()
