// Docker-specific settings for backend build
// Maintains backend: hierarchy for proper project references
rootProject.name = "flagforge"

include("backend:backend-common")
include("backend:webapp-api")
