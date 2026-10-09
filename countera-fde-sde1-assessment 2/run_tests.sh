#!/usr/bin/env bash
set -euo pipefail

rm -rf out
mkdir -p out

javac --release 17 -Xlint:all -d out $(find src tests -name '*.java' | sort)
java -ea -cp out com.countera.assessment.AssessmentTests
