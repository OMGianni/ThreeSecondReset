#!/bin/sh
#
# Gradle startup script for UN*X
#
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME="`cd "\`dirname "$0"\`"; pwd`"
MAX_FD="maximum"
warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }
if $darwin; then :; else
  MAX_FD_LIMIT=`ulimit -H -n`
  if [ $? -eq 0 ] ; then
    if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
      MAX_FD="$MAX_FD_LIMIT"
    fi
    ulimit -n $MAX_FD
  fi
fi
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_EXE=java
exec "$JAVA_EXE"  -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
