// Generate TypeScript client from OpenAPI spec
tasks.register<Exec>("generateTypeScriptClient") {
  group = "openapi"
  description = "Generate TypeScript API client from OpenAPI spec"

  workingDir = file("${project.rootDir}")  // Run from project root
  commandLine(
    "bash", "-c",
    "npx ng-openapi-gen --input contracts/webapp_api.json --output webapp/src/app/api/generated"
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
