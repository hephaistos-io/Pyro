// Root build file for FlagForge
// Contains Docker Compose convenience tasks

// Docker Compose tasks for development environment
// Note: Docker must be in the system PATH for these tasks to work

tasks.register<Exec>("dockerUp") {
    description = "Start all containers with Docker Compose"
    group = "docker"
    workingDir = projectDir
    environment("PATH", System.getenv("PATH"))
    commandLine("docker", "compose", "up", "-d")
}

tasks.register<Exec>("dockerDown") {
    description = "Stop all containers"
    group = "docker"
    workingDir = projectDir
    environment("PATH", System.getenv("PATH"))
    commandLine("docker", "compose", "down")
}

tasks.register<Exec>("dockerBuild") {
    description = "Build all Docker images"
    group = "docker"
    workingDir = projectDir
    environment("PATH", System.getenv("PATH"))
    commandLine("docker", "compose", "build")
}

tasks.register<Exec>("dockerLogs") {
    description = "Show container logs (follow mode)"
    group = "docker"
    workingDir = projectDir
    environment("PATH", System.getenv("PATH"))
    commandLine("docker", "compose", "logs", "-f")
}

tasks.register<Exec>("dockerClean") {
    description = "Remove containers, networks, and volumes"
    group = "docker"
    workingDir = projectDir
    environment("PATH", System.getenv("PATH"))
    commandLine("docker", "compose", "down", "-v", "--rmi", "local")
}

tasks.register("dockerRestart") {
    description = "Restart all containers"
    group = "docker"
    dependsOn("dockerDown")
    finalizedBy("dockerUp")
}

tasks.register<Exec>("dockerPs") {
    description = "Show running containers status"
    group = "docker"
    workingDir = projectDir
    environment("PATH", System.getenv("PATH"))
    commandLine("docker", "compose", "ps")
}

tasks.register<Exec>("dockerResetDb") {
    description = "Reset the PostgreSQL database volume (stops containers, deletes volume, restarts)"
    group = "docker"
    workingDir = projectDir
    environment("PATH", System.getenv("PATH"))
    commandLine(
        "sh",
        "-c",
        "docker compose down && docker volume rm flagforge-pgdata 2>/dev/null || true && docker compose up -d"
    )
}
