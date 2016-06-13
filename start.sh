#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

gradle_shadow_jar="$DIR/build/libs/services-python-all.jar"

if [ ! -f "$gradle_shadow_jar" ]; then
    printf "No jar found. Please build the project first.\n" >&2
    exit 99
fi

java -jar "$jar" \
     -t -p -o -c \
     -address tcp://* \
     -registration tcp://*:5004 \
     -configuration tcp://*:5007 \
     -resources 5052 \
     -dyndeps tcp://*:5009
