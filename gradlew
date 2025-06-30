#!/usr/bin/env bash

##############################################################################
##
##  Gradle wrapper script for UNIX
##
##############################################################################

# Determine the Java command to use to start the JVM.
if [ -n ""JAVA_HOME""]; then
    if [ -x ""$JAVA_HOME/jre/sh/java"" ]; then
        # IBM Java on Linux
        JAVA_CMD=""$JAVA_HOME/jre/sh/java""
    else
        JAVA_CMD=""$JAVA_HOME/bin/java""
    fi
    if [ ! -x ""$JAVA_CMD"" ]; then
        die ""ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

If this is a JDK, please verify that $JAVA_HOME/bin/java exists.
If this is a JRE, please verify that $JAVA_HOME/jre/sh/java exists.""
    fi
else
    JAVA_CMD=""java""
fi

# Determine the script directory.
SCRIPT_DIR=""$(dirname ""$0"")""

# Determine the Gradle distribution.
GRADLE_DISTRIBUTION_URL=""https://services.gradle.org/distributions/gradle-8.2-bin.zip""
GRADLE_DISTRIBUTION_SHA256=""a9b0c8e7f6d5b4c3a2b1e0d9c8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9"" # Placeholder, replace with actual SHA256

# Execute Gradle.
exec ""$JAVA_CMD"" -jar ""$SCRIPT_DIR/gradle-wrapper.jar"" ""$@""