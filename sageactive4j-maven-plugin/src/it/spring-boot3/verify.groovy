def log = new File(basedir, 'build.log').text
assert log.contains("Detected framework 'spring-boot3'")
// The starter is already declared, so the plugin must not ask for it.
assert !log.contains('Add the dependency')

assert new File(basedir, 'src/main/resources/sageactive4j.properties').isFile()
// Compiled by the `compile` goal that ran after `init`.
assert new File(basedir, 'target/classes/sageactive4j/SageController.class').isFile()
