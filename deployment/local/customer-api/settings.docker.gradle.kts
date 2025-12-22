// Docker-specific settings for customer-api build
// Maintains backend: hierarchy for proper project references
rootProject.name = "flagforge"

include("backend:backend-common")
include("backend:customer-api")
