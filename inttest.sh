#!/bin/bash

source ./testsecrets/source_secrets.sh

./gradlew clean test -Dtestcontainer.fxint.dim.disable=false --no-daemon --info