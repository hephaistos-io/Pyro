// Generate TypeScript client from OpenAPI spec
tasks.register<Exec>("generateTypeScriptClient") {
  group = "openapi"
  description = "Generate TypeScript API client from OpenAPI spec"

  workingDir = file("${project.projectDir}")  // Run from webapp directory
  commandLine(
    "bash", "-c",
    "npx ng-openapi-gen --config ng-openapi-gen.json"
  )

  inputs.file("${project.rootDir}/contracts/webapp_api.json")
  outputs.dir("${project.projectDir}/src/app/api/generated")
}

// Clean generated code
tasks.register<Delete>("cleanGenerated") {
  group = "openapi"
  description = "Delete generated TypeScript API client"

  delete("src/app/api/generated")
}

tasks.register<Exec>("bootRun") {
  group = "application"
  description = "Run dev server"

  commandLine(
    "bash", "-c",
    "ng serve"
  )
}

tasks.register<Exec>("npmBuild") {
  group = "build"
  description = "Build the Angular webapp for production"

  workingDir = file("${project.projectDir}")
  commandLine("bash", "-c", "npm run build")
}
