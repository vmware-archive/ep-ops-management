#! /bin/sh

#
# Copyright (c) 1999, 2006 Tanuki Software Inc.
#
# Java Service Wrapper sh script.  Suitable for starting and stopping
#  wrapped Java applications on UNIX platforms.
#

#-----------------------------------------------------------------------------
# These settings can be modified to fit the needs of your application

# Application
APP_NAME="ep-agent"
APP_LONG_NAME="End Point Operations Management Agent"

# Wrapper
WRAPPER_CMD="../../wrapper/sbin/wrapper"
WRAPPER_CMD_PS="./wrapper-"
WRAPPER_CONF="../../conf/wrapper.conf"

# Priority at which to run the wrapper.  See "man nice" for valid priorities.
#  nice is only used if a priority is specified.
PRIORITY=10

# Location of the pid file.
PIDDIR="../../wrapper"

# If uncommented, causes the Wrapper to be shutdown using an anchor file.
#  When launched with the 'start' command, it will also ignore all INT and
#  TERM signals.
#IGNORE_SIGNALS=true

# Wrapper will start the JVM asynchronously. Your application may have some
#  initialization tasks and it may be desirable to wait a few seconds
#  before returning.  For example, to delay the invocation of following
#  startup scripts.  Setting WAIT_AFTER_STARTUP to a positive number will
#  cause the start command to delay for the indicated period of time 
#  (in seconds).
# 
WAIT_AFTER_STARTUP=3

# If set, the status, start_msg and stop_msg commands will print out detailed
#   state information on the Wrapper and Java processes.
#DETAIL_STATUS=true

# If specified, the Wrapper will be run as the specified user.
# IMPORTANT - Make sure that the user has the required privileges to write
#  the PID file and wrapper.log files.  Failure to be able to write the log
#  file will cause the Wrapper to exit without any way to write out an error
#  message.
# NOTE - This will set the user which is used to run the Wrapper as well as
#  the JVM and is not useful in situations where a privileged resource or
#  port needs to be allocated prior to the user being changed.
#RUN_AS_USER=

# The following two lines are used by the chkconfig command. Change as is
#  appropriate for your application.  They should remain commented.
# chkconfig: 2345 20 80
# description: @app.long.name@
 
# Initialization block for the install_initd and remove_initd scripts used by
#  SUSE linux distributions.
### BEGIN INIT INFO
# Provides: @app.name@
# Required-Start: $local_fs $network $syslog
# Should-Start: 
# Required-Stop:
# Default-Start: 2 3 4 5
# Default-Stop: 0 1 6
# Short-Description: @app.long.name@
# Description: @app.description@
### END INIT INFO

# Do not modify anything beyond this point
#-----------------------------------------------------------------------------

# Make sure permissions are set to owner only 
umask 0077

# Get the fully qualified path to the script
case $0 in
    /*)
        SCRIPT="$0"
        ;;
    *)
        PWD=`pwd`
        SCRIPT="$PWD/$0"
        ;;
esac

# Resolve the true real path without any sym links.
CHANGED=true
while [ "X$CHANGED" != "X" ]
do
    # Change spaces to ":" so the tokens can be parsed.
    SAFESCRIPT=`echo $SCRIPT | sed -e 's; ;:;g'`
    # Get the real path to this script, resolving any symbolic links
    TOKENS=`echo $SAFESCRIPT | sed -e 's;/; ;g'`
    REALPATH=
    for C in $TOKENS; do
        # Change any ":" in the token back to a space.
        C=`echo $C | sed -e 's;:; ;g'`
        REALPATH="$REALPATH/$C"
        # If REALPATH is a sym link, resolve it.  Loop for nested links.
        while [ -h "$REALPATH" ] ; do
            LS="`ls -ld "$REALPATH"`"
            LINK="`expr "$LS" : '.*-> \(.*\)$'`"
            if expr "$LINK" : '/.*' > /dev/null; then
                # LINK is absolute.
                REALPATH="$LINK"
            else
                # LINK is relative.
                REALPATH="`dirname "$REALPATH"`""/$LINK"
            fi
        done
    done

    if [ "$REALPATH" = "$SCRIPT" ]
    then
        CHANGED=""
    else
        SCRIPT="$REALPATH"
    fi
done

# resolve the current HQ Agent bundle home
cd "`dirname "$REALPATH"`/.."

AGENT_BUNDLE_HOME=`pwd`
cd ../..
AGENT_INSTALL_HOME=`pwd`
# invoke the Java Service Wrapper from the wrapper sbin
# directory for compatibility with Windows
cd wrapper/sbin
REALDIR=`pwd`

# ------------- 
# Begin HQ Agent specific logic
# ------------- 
AGENT_BUNDLE_HOME_PROP=agent.bundle.home
AGENT_INSTALL_HOME_PROP=agent.install.home
AGENT_LIB=$AGENT_BUNDLE_HOME/lib
PDK_LIB=$AGENT_BUNDLE_HOME/pdk/lib
# for /proc/net/tcp mirror
SIGAR_PROC_NET=$AGENT_BUNDLE_HOME/tmp

if [ -f "${AGENT_BUNDLE_HOME}"/conf/wrapper-additional.conf ]; then
	rm ${AGENT_BUNDLE_HOME}/conf/wrapper-additional.conf
fi
	
if [ "x${HQ_JAVA_HOME}" != "x" ] ; then
    HQ_JAVA_HOME=${HQ_JAVA_HOME}
elif [ -d "${AGENT_BUNDLE_HOME}"/jre ]; then
    HQ_JAVA_HOME="${AGENT_BUNDLE_HOME}"/jre
    # Just in case
    chmod -R +x "${AGENT_BUNDLE_HOME}"/jre/bin/* > /dev/null 2>&1
elif [ -d "${AGENT_INSTALL_HOME}"/jre ]; then
    HQ_JAVA_HOME="${AGENT_INSTALL_HOME}"/jre
    # Just in case
    chmod -R +x "${AGENT_INSTALL_HOME}"/jre/bin/* > /dev/null 2>&1
elif [ "x$JAVA_HOME" != "x" ] ; then
    HQ_JAVA_HOME=${JAVA_HOME}
else
    case "`uname`" in
    Darwin)
        HQ_JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
        ;;
    *)
        echo "HQ_JAVA_HOME or JAVA_HOME must be set when invoking the agent"
        exit 1
        ;;
    esac
fi

# export environment variables to be picked up by the Java Service Wrapper process
export HQ_JAVA_HOME
export SIGAR_PROC_NET

chmod +x "${AGENT_BUNDLE_HOME}"/pdk/scripts/* > /dev/null 2>&1

HQ_JAVA="${HQ_JAVA_HOME}/bin/java"
# verify that the java command actually exists
if [ ! -f "$HQ_JAVA" ]
then
        echo Invalid Java Home detected at ${HQ_JAVA_HOME}
        exit 1
fi

CLIENT_CLASSPATH="${AGENT_LIB}/dcs-agent-core-${project.version}.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${AGENT_LIB}"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/dcs-tools-common-${project.version}.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/dcs-tools-util-${project.version}.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/dcs-tools-pdk-${project.version}.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/ant-1.7.1.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/commons-logging-1.0.4.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/log4j-1.2.14.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/sigar-${sigar.version}.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/httpclient-4.1.1.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/httpcore-4.1.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/spring-core-3.0.5.RELEASE.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${AGENT_LIB}/dcs-tools-lather-${project.version}.jar"

CLIENT_CLASS=org.hyperic.hq.bizapp.agent.client.AgentClient

CLIENT_CMD="${HQ_JAVA} \
    -Djava.net.preferIPv4Stack=false \
    -D${AGENT_INSTALL_HOME_PROP}=${AGENT_INSTALL_HOME} \
    -D${AGENT_BUNDLE_HOME_PROP}=${AGENT_BUNDLE_HOME} \
    -cp ${CLIENT_CLASSPATH} ${CLIENT_CLASS}"

PING_CMD="${CLIENT_CMD} ping"
SETUP_CMD="${CLIENT_CMD} setup"
STATUS_CMD="${CLIENT_CMD} status"
SETUP_IF_NO_PROVIDER_CMD="${CLIENT_CMD} setup-if-no-provider"
# ------------- 
# End HQ specific logic
# ------------- 

# If the PIDDIR is relative, set its value relative to the full REALPATH to avoid problems if
#  the working directory is later changed.
FIRST_CHAR=`echo $PIDDIR | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    PIDDIR=$REALDIR/$PIDDIR
fi
# Same test for WRAPPER_CMD
FIRST_CHAR=`echo $WRAPPER_CMD | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    WRAPPER_CMD=$REALDIR/$WRAPPER_CMD
fi
# Same test for WRAPPER_CONF
FIRST_CHAR=`echo $WRAPPER_CONF | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    WRAPPER_CONF=$REALDIR/$WRAPPER_CONF
fi

# Process ID
ANCHORFILE="$PIDDIR/$APP_NAME.anchor"
STATUSFILE="$PIDDIR/$APP_NAME.status"
JAVASTATUSFILE="$PIDDIR/$APP_NAME.java.status"
PIDFILE="$PIDDIR/$APP_NAME.pid"
LOCKDIR="/var/lock/subsys"
LOCKFILE="$LOCKDIR/$APP_NAME"
pid=""

# Resolve the location of the 'ps' command
PSEXE="/usr/bin/ps"
if [ ! -x "$PSEXE" ]
then
    PSEXE="/bin/ps"
    if [ ! -x "$PSEXE" ]
    then
        echo "Unable to locate 'ps'."
        echo "Please report this message along with the location of the command on your system."
        exit 1
    fi
fi

# Resolve the os
DIST_OS=`uname -s | tr "[A-Z]" "[a-z]" |tr -d ' '`
DIST_BITS="32"
case "$DIST_OS" in
    'sunos')
        DIST_OS="solaris"
        ;;
    'hp-ux')
        # HP-UX needs the XPG4 version of ps (for -o args)
        DIST_OS="hpux"
        UNIX95=""
        export UNIX95
        ;;
    'hp-ux64')
        # HP-UX needs the XPG4 version of ps (for -o args)
        DIST_OS="hpux"
        UNIX95=""
        export UNIX95
        DIST_BITS="64"
        ;;
    'darwin')
        DIST_OS="macosx"
        ;;
    'unix_sv')
        DIST_OS="unixware"
        ;;
    'aix')
        DIST_BITS=`getconf HARDWARE_BITMODE`
        ;;
esac

# Resolve the architecture
if [ "$DIST_OS" = "macosx" ]
then
    DIST_ARCH="universal"
else
    DIST_ARCH=
    DIST_ARCH=`uname -p 2>/dev/null | tr "[A-Z]" "[a-z]" |tr -d ' '`
    if [ "X$DIST_ARCH" = "X" ]
    then
        DIST_ARCH="unknown"
    fi
    if [ "$DIST_ARCH" = "unknown" ]
    then
        DIST_ARCH=`uname -m 2>/dev/null | tr "[A-Z]" "[a-z]" |tr -d ' '`
    fi

    case "$DIST_ARCH" in
        'athlon' | 'i386' | 'i486' | 'i586' | 'i686')
            DIST_ARCH="x86"
            ;;
        'amd64' | 'x86_64')
            DIST_ARCH="x86"
            DIST_BITS="64"
            ;;            
        'ia32')
            DIST_ARCH="ia"
            ;;
        'ia64' | 'ia64n' | 'ia64w')
            DIST_ARCH="ia"
            DIST_BITS="64"
            ;;            
        'ip27')
            DIST_ARCH="mips"
            ;;
        'power' | 'powerpc' | 'power_pc' | 'ppc64')
            DIST_ARCH="ppc"
            ;;
        'pa_risc' | 'pa-risc')
            DIST_ARCH="parisc"
            ;;
        'sun4u' | 'sparcv9')
            DIST_ARCH="sparc"
            ;;
        '9000/800')
            DIST_ARCH="parisc"
            ;;
    esac
fi

outputFile() {
    if [ -f "$1" ]
    then
        echo "  $1 (Found but not executable.)";
    else
        echo "  $1"
    fi
}

# Decide on the wrapper binary to use.
# First, try out the detected bit version
# If not available, try 32 bits followed by 64 bits.
if [ -x "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-$DIST_BITS" ]
then
  WRAPPER_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-$DIST_BITS"
elif [ -x "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32" ]
then
  WRAPPER_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32"
elif [ -x "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64" ]
then
  WRAPPER_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
else
  if [ ! -x "$WRAPPER_CMD" ]
    then
      echo "Unable to locate any of the following binaries:"
      outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32"
      outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
      outputFile "$WRAPPER_CMD"
      exit 1
  fi
fi

# Build the nice clause
if [ "X$PRIORITY" = "X" ]
then
    CMDNICE=""
else
    CMDNICE="nice -$PRIORITY"
fi

# Build the anchor file clause.
if [ "X$IGNORE_SIGNALS" = "X" ]
then
   ANCHORPROP=
   IGNOREPROP=
else
   ANCHORPROP=wrapper.anchorfile=\"$ANCHORFILE\"
   IGNOREPROP=wrapper.ignore_signals=TRUE
fi

# Build the status file clause.
if [ "X$DETAIL_STATUS" = "X" ]
then
   STATUSPROP=
else
   STATUSPROP="wrapper.statusfile=\"$STATUSFILE\" wrapper.java.statusfile=\"$JAVASTATUSFILE\""
fi

# Build the lock file clause.  Only create a lock file if the lock directory exists on this platform.
LOCKPROP=
if [ -d $LOCKDIR ]
then
    if [ -w $LOCKDIR ]
    then
        LOCKPROP=wrapper.lockfile=\"$LOCKFILE\"
    fi
fi

checkUser() {
    # $1 touchLock flag
    # $2 command

    # Check the configured user.  If necessary rerun this script as the desired user.
    if [ "X$RUN_AS_USER" != "X" ]
    then
        # Resolve the location of the 'id' command
        IDEXE="/usr/xpg4/bin/id"
        if [ ! -x "$IDEXE" ]
        then
            IDEXE="/usr/bin/id"
            if [ ! -x "$IDEXE" ]
            then
                echo "Unable to locate 'id'."
                echo "Please report this message along with the location of the command on your system."
                exit 1
            fi
        fi
    
        if [ "`$IDEXE -u -n`" = "$RUN_AS_USER" ]
        then
            # Already running as the configured user.  Avoid password prompts by not calling su.
            RUN_AS_USER=""
        fi
    fi
    if [ "X$RUN_AS_USER" != "X" ]
    then
        # If LOCKPROP and $RUN_AS_USER are defined then the new user will most likely not be
        # able to create the lock file.  The Wrapper will be able to update this file once it
        # is created but will not be able to delete it on shutdown.  If $2 is defined then
        # the lock file should be created for the current command
        if [ "X$LOCKPROP" != "X" ]
        then
            if [ "X$1" != "X" ]
            then
                # Resolve the primary group 
                RUN_AS_GROUP=`groups $RUN_AS_USER | awk '{print $3}' | tail -1`
                if [ "X$RUN_AS_GROUP" = "X" ]
                then
                    RUN_AS_GROUP=$RUN_AS_USER
                fi
                touch $LOCKFILE
                chown $RUN_AS_USER:$RUN_AS_GROUP $LOCKFILE
            fi
        fi

        # Still want to change users, recurse.  This means that the user will only be
        #  prompted for a password once. Variables shifted by 1
        # 
        # Use "runuser" if this exists.  runuser should be used on RedHat in preference to su.
        #
        if test -f "/sbin/runuser"
        then
            /sbin/runuser - $RUN_AS_USER -c "\"$REALPATH\" $2"
        else
            su - $RUN_AS_USER -c "\"$REALPATH\" $2"
        fi

        # Now that we are the original user again, we may need to clean up the lock file.
        if [ "X$LOCKPROP" != "X" ]
        then
            getpid
            if [ "X$pid" = "X" ]
            then
                # Wrapper is not running so make sure the lock file is deleted.
                if [ -f "$LOCKFILE" ]
                then
                    rm "$LOCKFILE"
                fi
            fi
        fi

        exit 0
    fi
}

getpid() {
    pid=""
    if [ -f "$PIDFILE" ]
    then
        if [ -r "$PIDFILE" ]
        then
            pid=`cat "$PIDFILE"`
            if [ "X$pid" != "X" ]
            then
                # It is possible that 'a' process with the pid exists but that it is not the
                #  correct process.  This can happen in a number of cases, but the most
                #  common is during system startup after an unclean shutdown.
                # The ps statement below looks for the specific wrapper command running as
                #  the pid.  If it is not found then the pid file is considered to be stale.
                case "$DIST_OS" in
                    'macosx')
                        pidtest=`$PSEXE -ww -p $pid -o command | grep "$WRAPPER_CMD_PS" | tail -1`
                        ;;
                    'solaris')
	                    if [ -f "/usr/bin/pargs" ]
	                    then
	                       pidtest=`pargs $pid | grep "$WRAPPER_CMD_PS" | tail -1`
	                    else
	                       case "$PSEXE" in
	                          '/usr/ucb/ps')
	                             pidtest=`$PSEXE -auxww  $pid | grep "$WRAPPER_CMD_PS" | tail -1`
	                             ;;
	                          '/usr/bin/ps')
	                             TRUNCATED_CMD=`$PSEXE -ww -o comm -p $pid | tail -1`
	                             COUNT=`echo $TRUNCATED_CMD | wc -m`
	                             COUNT=`echo ${COUNT}`
	                             COUNT=`expr $COUNT - 1`
	                             TRUNCATED_CMD=`echo $WRAPPER_CMD_PS | cut -c1-$COUNT`
	                             pidtest=`$PSEXE -ww -o comm -p $pid | grep "$TRUNCATED_CMD" | tail -1`
	                             ;;
	                          '/bin/ps')
	                             TRUNCATED_CMD=`$PSEXE -ww -o comm -p $pid | tail -1`
	                             COUNT=`echo $TRUNCATED_CMD | wc -m`
	                             COUNT=`echo ${COUNT}`
	                             COUNT=`expr $COUNT - 1`
	                             TRUNCATED_CMD=`echo $WRAPPER_CMD_PS | cut -c1-$COUNT`
	                             pidtest=`$PSEXE -ww -o comm -p $pid | grep "$TRUNCATED_CMD" | tail -1`
	                             ;;
                              *)
	                             echo "Unsupported ps command $PSEXE"
	                             exit 1
	                             ;;
	                          esac
	                     fi
	                     ;;
                    'hpux')
                        pidtest=`$PSEXE  -p $pid -x -o args | grep "$WRAPPER_CMD_PS" | tail -1`
                        ;;
                    'aix')
                        pidtest=`$PSEXE -p $pid -o args | grep "$WRAPPER_CMD_PS" | tail -1`
                        ;;
                    *)
                        pidtest=`$PSEXE -ww -p $pid -o args | grep "$WRAPPER_CMD_PS" | tail -1`
                        ;;
                esac

                if [ "X$pidtest" = "X" ]
                then
                    # This is a stale pid file.
                    rm -f "$PIDFILE"
                    echo "Removed stale pid file: $PIDFILE"
                    pid=""
                fi
            fi
        else
            echo "Cannot read $PIDFILE."
            exit 1
        fi
    fi
}

getstatus() {
    STATUS=
    if [ -f "$STATUSFILE" ]
    then
        if [ -r "$STATUSFILE" ]
        then
            STATUS=`cat "$STATUSFILE"`
        fi
    fi
    if [ "X$STATUS" = "X" ]
    then
        STATUS="Unknown"
    fi
    
    JAVASTATUS=
    if [ -f "$JAVASTATUSFILE" ]
    then
        if [ -r "$JAVASTATUSFILE" ]
        then
            JAVASTATUS=`cat "$JAVASTATUSFILE"`
        fi
    fi
    if [ "X$JAVASTATUS" = "X" ]
    then
        JAVASTATUS="Unknown"
    fi
}

testpid() {
    # It is possible that 'a' process with the pid exists but that it is not the
    # correct process. This can happen in a number of cases, but the most
    # common is during system startup after an unclean shutdown.
    # The ps statement below looks for the specific wrapper command running as
    # the pid. If it is not found then the pid file is considered to be stale.
    case "$DIST_OS" in
     'solaris')
        case "$PSEXE" in
        '/usr/ucb/ps')
            pid=`$PSEXE  $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
            ;;
        '/usr/bin/ps')
            pid=`$PSEXE -p $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
            ;;
        '/bin/ps')
            pid=`$PSEXE -p $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
            ;;
        *)
            echo "Unsupported ps command $PSEXE"
            exit 1
            ;;
        esac
        ;;
    *)
        pid=`$PSEXE -p $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1` 2>/dev/null
        ;;
    esac
    if [ "X$pid" = "X" ]
    then
        # Process is gone so remove the pid file.
        rm -f "$PIDFILE"
        pid=""
    fi
} 

 
start() {
    echo -n "Starting $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        # The string passed to eval must handles spaces in paths correctly.

	BASE=`pwd`
	WRAPPER_WS_DIR=`dirname $WRAPPER_CMD`
	cd $WRAPPER_WS_DIR
	REL_WRAPPER_EXE=`basename $WRAPPER_CMD`
	REL_WRAPPER_CONF="../../conf/wrapper.conf"
	REL_WRAPPER_PID="../ep-agent.pid"

	COMMAND_LINE="$CMDNICE \"./$REL_WRAPPER_EXE\" \"$REL_WRAPPER_CONF\" wrapper.syslog.ident=\"$APP_NAME\" wrapper.pidfile=\"$REL_WRAPPER_PID\" wrapper.name=\"$APP_NAME\" wrapper.displayname=\"$APP_LONG_NAME\" wrapper.daemonize=TRUE $ANCHORPROP $IGNOREPROP $STATUSPROP $LOCKPROP"
	eval $COMMAND_LINE
        cd $BASE
    else
        echo "$APP_LONG_NAME is already running."
        exit 1
    fi

    # Sleep for a few seconds to allow for intialization if required 
    #  then test to make sure we're still running.
    #
    i=0
    while [ $i -lt $WAIT_AFTER_STARTUP ]
    do
        sleep 1
        echo -n "."
        i=`expr $i + 1`
    done
    if [ $WAIT_AFTER_STARTUP -gt 0 ]
    then
        getpid
        if [ "X$pid" = "X" ]
        then
            echo " WARNING: $APP_LONG_NAME may have failed to start."
            exit 1
        else
            echo " running ($pid)."
        fi
    else 
        echo ""
    fi
    
    ${SETUP_IF_NO_PROVIDER_CMD}
}
 
stopit() {
    echo "Stopping $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."
    else
        if [ "X$IGNORE_SIGNALS" = "X" ]
        then
            # Running so try to stop it.
            kill $pid
            if [ $? -ne 0 ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        else
            rm -f "$ANCHORFILE"
            if [ -f "$ANCHORFILE" ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        fi

        # We can not predict how long it will take for the wrapper to
        #  actually stop as it depends on settings in wrapper.conf.
        #  Loop until it does.
        savepid=$pid
        CNT=0
        TOTCNT=0
        while [ "X$pid" != "X" ]
        do
            # Show a waiting message every 5 seconds.
            if [ "$CNT" -lt "5" ]
            then
                CNT=`expr $CNT + 1`
            else
                echo "Waiting for $APP_LONG_NAME to exit..."
                CNT=0
            fi
            TOTCNT=`expr $TOTCNT + 1`

            sleep 1

            testpid
        done

        pid=$savepid
        testpid
        if [ "X$pid" != "X" ]
        then
            echo "Failed to stop $APP_LONG_NAME."
            exit 1
        else
            echo "Stopped $APP_LONG_NAME."
        fi
    fi
}

status() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME is not running."
        exit 1
    else
        if [ "X$DETAIL_STATUS" = "X" ]
        then
            echo "$APP_LONG_NAME is running (PID:$pid)."
            ${STATUS_CMD}
        else
            getstatus
            echo "$APP_LONG_NAME is running (PID:$pid, Wrapper:$STATUS, Java:$JAVASTATUS)"
            ${STATUS_CMD}
        fi
        exit 0
    fi
}

dump() {
    echo "Dumping $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."
    else
        kill -3 $pid

        if [ $? -ne 0 ]
        then
            echo "Failed to dump $APP_LONG_NAME."
            exit 1
        else
            echo "Dumped $APP_LONG_NAME."
        fi
    fi
}

# Used by HP-UX init scripts.
startmsg() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "Starting $APP_LONG_NAME... (Wrapper:Stopped)"
    else
        if [ "X$DETAIL_STATUS" = "X" ]
        then
            echo "Starting $APP_LONG_NAME... (Wrapper:Running)"
        else
            getstatus
            echo "Starting $APP_LONG_NAME... (Wrapper:$STATUS, Java:$JAVASTATUS)"
        fi
    fi
}

# Used by HP-UX init scripts.
stopmsg() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "Stopping $APP_LONG_NAME... (Wrapper:Stopped)"
    else
        if [ "X$DETAIL_STATUS" = "X" ]
        then
            echo "Stopping $APP_LONG_NAME... (Wrapper:Running)"
        else
            getstatus
            echo "Stopping $APP_LONG_NAME... (Wrapper:$STATUS, Java:$JAVASTATUS)"
        fi
    fi
}


ping() {
    echo Pinging ...
    VAL=`$PING_CMD 2>&1`
    if [ "$?" = "0" ] ; then
        echo 'Success!'
    else
        echo 'Failure!'
        echo $VAL
        exit 1
    fi
}

# Used by HP-UX init scripts.
setup() {
    getpid
    if [ "X$pid" = "X" ]
    then
        start
    fi
    ${SETUP_CMD}
}

lowerCaseArg=`echo ${1} | tr '[A-Z]' '[a-z]'`

case "$lowerCaseArg" in

    'start')
        checkUser touchlock $lowerCaseArg
        start
        ;;

    'stop')
        checkUser "" $lowerCaseArg
        stopit
        ;;

    'restart')
        checkUser touchlock $lowerCaseArg
        stopit
        start
        ;;

    'status')
        checkUser "" $lowerCaseArg
        status
        ;;

    'dump')
        checkUser "" $lowerCaseArg
        dump
        ;;

    'start_msg')
        checkUser "" $lowerCaseArg
        startmsg
        ;;

    'stop_msg')
        checkUser "" $lowerCaseArg
        stopmsg
        ;;

    'ping')
        checkUser "" $lowerCaseArg
        ping
        ;;

    'setup')
        checkUser "" $lowerCaseArg
        setup
        ;;        

    *)
        echo "Usage: $0 { start | stop | restart | status | dump | ping | setup | set-property }"
        exit 1
        ;;
esac

exit 0
