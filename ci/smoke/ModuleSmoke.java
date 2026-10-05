import io.github.josemodi97.sageactive4j.SageActive4jClient;

/**
 * Checks that sageactive4j-core loads as a real named module with the Java 11
 * descriptor, then runs {@link Smoke} through that module boundary.
 *
 * Usage: java --module-path <jar> --add-modules io.github.josemodi97.sageactive4j.core -cp <dir> ModuleSmoke
 */
public class ModuleSmoke {

    public static void main(String[] args) throws Exception {
        Module core = SageActive4jClient.class.getModule();
        if (!core.isNamed()) {
            throw new AssertionError("Expected sageactive4j-core to be a NAMED module on the module path");
        }
        boolean requiresHttp = core.getDescriptor().requires().stream()
                .anyMatch(r -> r.name().equals("java.net.http"));
        if (!requiresHttp) {
            throw new AssertionError("Expected the Java 11+ module descriptor to be selected");
        }
        System.out.println("Named module " + core.getName() + " " + core.getDescriptor().exports());
        Smoke.main(new String[0]);
    }
}
