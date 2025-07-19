#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

#*****************************************************************************
#
#   Gradle start up script for UN*X
#
#*****************************************************************************

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/" >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass any JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

# OS specific support.  $var _must_ be set to either true or false.
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN*) cygwin=true ;;
  Darwin*) darwin=true ;;
  MINGW*) msys=true ;;
  NONSTOP*) nonstop=true ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if $cygwin ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Attempt to find java
if [ -z "$JAVA_HOME" ] ; then
    if $darwin ; then
        if [ -x '/usr/libexec/java_home' ] ; then
            JAVA_HOME=`/usr/libexec/java_home`
        elif [ -d "/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home" ]; then
            JAVA_HOME="/System/Library/Frameworks/JavaVM.framework/Versions/CurrentJDK/Home"
        fi
    else
        java_path=`which java 2>/dev/null`
        if [ -n "$java_path" ] ; then
            java_path=`readlink -f "$java_path" 2>/dev/null`
            if [ -n "$java_path" ] ; then
                JAVA_HOME=`dirname "$java_path" 2>/dev/null`
                JAVA_HOME=`dirname "$JAVA_HOME" 2>/dev/null`
            fi
        fi
    fi
fi

# If we still don't have a JAVA_HOME, try to use a default
if [ -z "$JAVA_HOME" ] ; then
    # If we are on mac, adopt openjdk
    if $darwin ; then
        if [ -d "/opt/homebrew/opt/openjdk" ] ; then
            JAVA_HOME="/opt/homebrew/opt/openjdk"
        fi
    fi
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

if [ -z "$JAVA_HOME" ] ; then
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH." >&2
    echo "" >&2
    echo "Please set the JAVA_HOME variable in your environment to match the" >&2
    echo "location of your Java installation." >&2
    exit 1
fi

# Set directory to APP_HOME
cd "$APP_HOME"

# Read wrapper properties
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
PROPERTIES_FILE="gradle/wrapper/gradle-wrapper.properties"
if [ -f "$PROPERTIES_FILE" ] ; then
    while IFS='=' read -r key value || [ -n "$key" ]; do
        case "$key" in
            distributionUrl)
                GRADLE_DISTRIBUTION_URL="$value"
                ;;
        esac
    done < "$PROPERTIES_FILE"
fi

# Download Gradle distribution if not available
if [ -n "$GRADLE_DISTRIBUTION_URL" ] ; then
    GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
    GRADLE_WRAPPED_DIST_PATH="$GRADLE_USER_HOME/wrapper/dists"
    GRADLE_DIST_FLAVOR="bin" # "bin" or "all"
    if echo "$GRADLE_DISTRIBUTION_URL" | grep "all.zip" > /dev/null; then
        GRADLE_DIST_FLAVOR="all"
    fi
    GRADLE_DIST_NAME=`basename "$GRADLE_DISTRIBUTION_URL"`
    GRADLE_DIST_NAME_WITHOUT_EXT=${GRADLE_DIST_NAME%.zip}
    GRADLE_DIST_PATH="$GRADLE_WRAPPED_DIST_PATH/$GRADLE_DIST_NAME_WITHOUT_EXT"
    if [ ! -d "$GRADLE_DIST_PATH" ] ; then
        echo "Downloading $GRADLE_DISTRIBUTION_URL"
        mkdir -p "$GRADLE_WRAPPED_DIST_PATH"
        if [ `which curl` ]; then
            curl -L -o "$GRADLE_WRAPPED_DIST_PATH/$GRADLE_DIST_NAME" "$GRADLE_DISTRIBUTION_URL"
        elif [ `which wget` ]; then
            wget -O "$GRADLE_WRAPPED_DIST_PATH/$GRADLE_DIST_NAME" "$GRADLE_DISTRIBUTION_URL"
        else
            echo "ERROR: Neither curl nor wget is available to download gradle distribution." >&2
            exit 1
        fi
        unzip -d "$GRADLE_WRAPPED_DIST_PATH" "$GRADLE_WRAPPED_DIST_PATH/$GRADLE_DIST_NAME"
    fi
fi

# Collect all arguments for the java command, following the shell quoting rules
#
# (The code below was inspired by the quoting implementation in the "ant" shell script.)
#
# Assuming the following variables are set:
#
# - OPTS_VAR_NAME: the name of the variable containing the options
# - EXTRA_ARGS_VAR_NAME: the name of the variable containing additional arguments
# - a variable with the name of the value of OPTS_VAR_NAME: the options
# - a variable with the name of the value of EXTRA_ARGS_VAR_NAME: the additional arguments
#
# The final command will be stored in the variable 'CMD'.
#
collect_java_args() {
    local OPTS_VAR_NAME=$1
    local EXTRA_ARGS_VAR_NAME=$2
    local CMD=""
    local _OPTS_VAR_NAME_VALUE=`eval echo '$'$OPTS_VAR_NAME`
    local _EXTRA_ARGS_VAR_NAME_VALUE=`eval echo '$'$EXTRA_ARGS_VAR_NAME`

    for arg in `eval echo $_OPTS_VAR_NAME_VALUE`; do
        # if the arg contains a space, then wrap it in quotes
        case "$arg" in
            *\"* | *\'\* | *\ * )
                arg="\"$arg\""
                ;;
        esac
        CMD="$CMD $arg"
    done

    for arg in `eval echo $_EXTRA_ARGS_VAR_NAME_VALUE`; do
        # if the arg contains a space, then wrap it in quotes
        case "$arg" in
            *\"* | *\'\* | *\ * )
                arg="\"$arg\""
                ;;
        esac
        CMD="$CMD $arg"
    done
    echo "$CMD"
}

# Collect all arguments for the java command
JAVA_ARGS=`collect_java_args DEFAULT_JVM_OPTS JAVA_OPTS`
GRADLE_ARGS=`collect_java_args GRADLE_OPTS GRADLE_OPTS`

# Execute Gradle
eval exec "\"$JAVA_HOME/bin/java\"" $JAVA_ARGS -cp "\"$APP_HOME/$WRAPPER_JAR\"" org.gradle.wrapper.GradleWrapperMain $GRADLE_ARGS "\"$@\""