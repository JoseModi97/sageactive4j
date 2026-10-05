rootProject.name = "sageactive4j"

include("sageactive4j-core")
include("sageactive4j-servlet")
include("sageactive4j-jakarta")
include("sageactive4j-spring-boot2-starter")
include("sageactive4j-spring-boot3-starter")
include("sageactive4j-bom")

// The CLI is added here when Phase 4 lands (PLAN.md §8). Build-tool plugins
// are standalone builds, not subprojects (PLAN.md §5.4).
