#!/bin/bash
./gradlew testDebugUnitTest jacocoTestCoverageVerification --continue || true
