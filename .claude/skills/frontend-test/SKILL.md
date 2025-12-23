---
name: frontend-test
description: Run the frontend e2e system tests to assess if everything works as expected
allowed-tools: ./gradlew systemTestsChromium, ./gradlew systemTestsCrossBrowser, npm build
---

# Backend Test

This Skill provides the commands to run backend tests, to verify functionality.

## Instructions

1. Go to the webapp folder
2. run `npm build`
3. Fix any issues that arise
4. Go to the project root
5. run `./gradlew systemTestsChromium`
6. fix any issues that arise until it passes
7. run `./gradlew systemTestsCrossBrowser`
8. fix any issues that arise until it passes
