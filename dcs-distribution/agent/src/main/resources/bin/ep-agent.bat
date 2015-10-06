@echo off
setlocal

rem Copyright (c) 1999, 2006 Tanuki Software Inc.
rem
rem Java Service Wrapper command based script
rem

rem Check if has been requested to run in silent mode, i.e. no user input required/accepted
for %%v in (%*) do (
                if "%%v" == "--silent" (
                                set SILENT_MODE="True"
                )
)

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _REALPATH=%~dp0..\..\..\wrapper\sbin

set AGENT_INSTALL_HOME=%~dp0..\..\..
set AGENT_BUNDLE_HOME=%~dp0..

echo ------------------------------ >> "%_AGENTOPERATIONSLOGFILE%"
echo Command called: %1 at %mydate%_%mytime% >> "%_AGENTOPERATIONSLOGFILE%"

rem
rem Detect HQ_JAVA_HOME
rem
echo cd to REALPATH : %_REALPATH% >> "%_AGENTOPERATIONSLOGFILE%"
cd %_REALPATH%

if not "%HQ_JAVA_HOME%"=="" goto gothqjava

if EXIST "%AGENT_BUNDLE_HOME%"\jre (
    echo Using AGENT_BUNDLE_HOME for jre: set HQ_JAVA_HOME=%AGENT_BUNDLE_HOME%\jre >> "%_AGENTOPERATIONSLOGFILE%"
    set HQ_JAVA_HOME=%AGENT_BUNDLE_HOME%\jre
    goto gotjava
)

if EXIST "%AGENT_INSTALL_HOME%"\jre (
    echo Using AGENT_INSTALL_HOME for jre: set HQ_JAVA_HOME=%AGENT_INSTALL_HOME%\jre >> "%_AGENTOPERATIONSLOGFILE%"
    set HQ_JAVA_HOME=%AGENT_INSTALL_HOME%\jre
    goto gotjava
)

:nojava
  echo :nojava - HQ_JAVA_HOME must be set when invoking the agent >> "%_AGENTOPERATIONSLOGFILE%"
  echo HQ_JAVA_HOME must be set when invoking the agent
goto :eof

:gothqjava
if not EXIST "%HQ_JAVA_HOME%" (
  echo :gothqjava - HQ_JAVA_HOME must be set to a valid directory >> "%_AGENTOPERATIONSLOGFILE%"
  echo HQ_JAVA_HOME must be set to a valid directory
  goto :eof
) else (
  echo Using HQ_JAVA_HOME for jre: set HQ_JAVA_HOME=%HQ_JAVA_HOME% >> "%_AGENTOPERATIONSLOGFILE%"
  set HQ_JAVA_HOME=%HQ_JAVA_HOME%
)

:gotjava
rem Decide on the wrapper binary.
set _WRAPPER_BASE=wrapper
set _WRAPPER_EXE=%_REALPATH%\%_WRAPPER_BASE%-windows-x86-32.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_REALPATH%\%_WRAPPER_BASE%-windows-x86-64.exe
if exist "%_WRAPPER_EXE%" goto validate
set _WRAPPER_EXE=%_REALPATH%\%_WRAPPER_BASE%.exe
if exist "%_WRAPPER_EXE%" goto validate

echo Unable to locate a Wrapper executable using any of the following names: >> "%_AGENTOPERATIONSLOGFILE%"
echo %_REALPATH%\%_WRAPPER_BASE%-windows-x86-32.exe >> "%_AGENTOPERATIONSLOGFILE%"
echo %_REALPATH%\%_WRAPPER_BASE%-windows-x86-64.exe >> "%_AGENTOPERATIONSLOGFILE%"
echo %_REALPATH%\%_WRAPPER_BASE%.exe >> "%_AGENTOPERATIONSLOGFILE%"

echo Unable to locate a Wrapper executable using any of the following names:
echo %_REALPATH%\%_WRAPPER_BASE%-windows-x86-32.exe
echo %_REALPATH%\%_WRAPPER_BASE%-windows-x86-64.exe
echo %_REALPATH%\%_WRAPPER_BASE%.exe
if "%SILENT_MODE%" == "" pause
goto :eof

:validate
echo Using WRAPPER_EXE: %_WRAPPER_EXE% >> "%_AGENTOPERATIONSLOGFILE%"
rem Find the requested command.
for /F %%v in ('echo %1^|findstr /I "^start$ ^stop$ ^restart$ ^install$ ^remove$ ^query$ ^ping$ ^setup"') do call :exec set COMMAND=%%v

if "%COMMAND%" == "" (
    echo Usage: %0 { start : stop : restart : install : remove : query : ping : setup : set-property }
    if "%SILENT_MODE%" == "" pause
    goto :eof
) else (
    shift
)

rem
rem Find the wrapper.conf
rem
:conf
set _WRAPPER_CONF="%AGENT_INSTALL_HOME%\conf\wrapper.conf"

rem
rem Set some HQ properties
rem
set AGENT_BUNDLE_HOME_PROP=agent.bundle.home
set AGENT_INSTALL_HOME_PROP=agent.install.home
set AGENT_LIB=%AGENT_BUNDLE_HOME%\lib
set PDK_LIB=%AGENT_BUNDLE_HOME%\pdk\lib


set CLIENT_CLASSPATH=%AGENT_LIB%\dcs-agent-core-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%AGENT_LIB%
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\dcs-tools-common-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\dcs-tools-util-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\dcs-tools-pdk-${project.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\ant-1.7.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\commons-logging-1.0.4.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\log4j-1.2.14.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\sigar-${sigar.version}.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\httpclient-4.1.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\httpcore-4.1.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\spring-core-3.0.5.RELEASE.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%PDK_LIB%\commons-codec-1.2.jar
set CLIENT_CLASSPATH=%CLIENT_CLASSPATH%;%AGENT_LIB%\dcs-tools-lather-${project.version}.jar

set CLIENT_CLASS=org.hyperic.hq.bizapp.agent.client.AgentClient

set CLIENT_CMD="%HQ_JAVA_HOME%\bin\java" -Djava.net.preferIPv4Stack=false -D%AGENT_INSTALL_HOME_PROP%="%AGENT_INSTALL_HOME%" -D%AGENT_BUNDLE_HOME_PROP%="%AGENT_BUNDLE_HOME%" -cp "%CLIENT_CLASSPATH%" %CLIENT_CLASS%

set PING_CMD=%CLIENT_CMD% ping
set SETUP_CMD=%CLIENT_CMD% setup
set SETUP_IF_NO_PROVIDER_CMD=%CLIENT_CMD% setup-if-no-provider
set wrapper_update1=set.HQ_JAVA_HOME=%HQ_JAVA_HOME%

rem
rem Run the application.
rem At runtime, the current directory will be that of wrapper.exe
rem
echo call :%COMMAND% >> "%_AGENTOPERATIONSLOGFILE%"
call :%COMMAND%
set COMMAND_ERROR_LEVEL=%ERRORLEVEL%
if %COMMAND_ERROR_LEVEL% NEQ 0 (
    if "%SILENT_MODE%" == "" pause
)
goto :eof

:remove
echo "%_WRAPPER_EXE%" -r %_WRAPPER_CONF% "%wrapper_update1%" >> "%_AGENTOPERATIONSLOGFILE%"
"%_WRAPPER_EXE%" -r %_WRAPPER_CONF% "%wrapper_update1%"
goto :eof

:start
echo "%_WRAPPER_EXE%" -r %_WRAPPER_CONF% "%wrapper_update1%" >> "%_AGENTOPERATIONSLOGFILE%"
"%_WRAPPER_EXE%" -t %_WRAPPER_CONF% "%wrapper_update1%"
set START_ERROR_LEVEL=%ERRORLEVEL%
if %START_ERROR_LEVEL% NEQ 0 (
    rem if "%SILENT_MODE%" == "" pause
    goto :eof
)
ping -3 XXX 127.0.0.1 >nul
call :setup-if-no-provider
goto :eof

:stop
echo "%_WRAPPER_EXE%" -p %_WRAPPER_CONF% "%wrapper_update1%" >> "%_AGENTOPERATIONSLOGFILE%"
"%_WRAPPER_EXE%" -p %_WRAPPER_CONF% "%wrapper_update1%"
goto :eof

:install
echo "%_WRAPPER_EXE%" -i %_WRAPPER_CONF% "%wrapper_update1%" >> "%_AGENTOPERATIONSLOGFILE%"
"%_WRAPPER_EXE%" -i %_WRAPPER_CONF% "%wrapper_update1%"
if not errorlevel 1 goto :hardenfolder
goto :eof

:query
"%_WRAPPER_EXE%" -q %_WRAPPER_CONF% "%wrapper_update1%"
goto :eof

:restart
echo :restart >> "%_AGENTOPERATIONSLOGFILE%"
call :stop
if %ERRORLEVEL% EQU 0 ( call :start )
goto :eof

:exec
%*
goto :eof

:hardenfolder
set targetFolder=%AGENT_INSTALL_HOME%
if not exist "%targetFolder%" goto :nodir
if not exist %windir%\System32\icacls.exe goto :nofile

echo Setting restrictive folder permissions on %targetFolder%
echo --------------------------------------------------------------------------
echo.
echo This process preserves any custom permissions that were granted to this folder and subfolders
echo.
FOR /F "delims=" %%i IN ('whoami') DO set currentUser=%%i
echo currentUser="%currentUser%" >> "%_AGENTOPERATIONSLOGFILE%"

REM This should be always first, since /inheritance:r removes all permissions and we may get access denied after we run it!
echo Granting the current user full control over the agent root folder and subfolders >> "%_AGENTOPERATIONSLOGFILE%"
echo icacls "%targetFolder%" /grant "%currentUser%":"(OI)(CI)F /t /c /q" >> "%_AGENTOPERATIONSLOGFILE%"
echo Granting the current user full control over the agent root folder and subfolders
icacls "%targetFolder%" /grant "%currentUser%":(OI)(CI)F /t /c /q >> "%_AGENTOPERATIONSLOGFILE%"
if %ERRORLEVEL% NEQ 0 (
    echo Failed! ERRORLEVEL:%ERRORLEVEL% >> "%_AGENTOPERATIONSLOGFILE%"
    echo Failed with error level %ERRORLEVEL%
)

echo Removing permission inheritance from parent folders >> "%_AGENTOPERATIONSLOGFILE%"
echo icacls "%targetFolder%" /inheritance:r /c /q >> "%_AGENTOPERATIONSLOGFILE%"
echo Removing permission inheritance from parent folders
icacls "%targetFolder%" /inheritance:r /c /q >> "%_AGENTOPERATIONSLOGFILE%"
if %ERRORLEVEL% NEQ 0 (
    echo Failed! ERRORLEVEL:%ERRORLEVEL% >> "%_AGENTOPERATIONSLOGFILE%"
    echo Failed with error level %ERRORLEVEL%
)

echo Setting current user as owner of the agent root folder and subfolders >> "%_AGENTOPERATIONSLOGFILE%"
echo icacls "%targetFolder%" /setowner "%currentUser%" /t /c /q >> "%_AGENTOPERATIONSLOGFILE%"
echo Setting current user as owner of the agent root folder and subfolders
icacls "%targetFolder%" /setowner "%currentUser%" /t /c /q >> "%_AGENTOPERATIONSLOGFILE%"
if %ERRORLEVEL% NEQ 0 (
    echo Failed! ERRORLEVEL:%ERRORLEVEL% >> "%_AGENTOPERATIONSLOGFILE%"
    echo Failed with error level %ERRORLEVEL%
)

echo Granting the Administrators group full control over the agent root folder and subfolders >> "%_AGENTOPERATIONSLOGFILE%"
echo icacls "%targetFolder%" /grant *S-1-5-32-544:"(OI)(CI)F /t /c /q" >> "%_AGENTOPERATIONSLOGFILE%"
echo Granting the Administrators group full control over the agent root folder and subfolders
icacls "%targetFolder%" /grant *S-1-5-32-544:(OI)(CI)F /t /c /q >> "%_AGENTOPERATIONSLOGFILE%"
if %ERRORLEVEL% NEQ 0 (
    echo Failed! ERRORLEVEL:%ERRORLEVEL% >> "%_AGENTOPERATIONSLOGFILE%"
    echo Failed with error level %ERRORLEVEL%
)

echo Granting the SYSTEM user full control over the agent root folder and subfolders >> "%_AGENTOPERATIONSLOGFILE%"
echo Granting the SYSTEM user full control over the agent root folder and subfolders
icacls "%targetFolder%" /grant SYSTEM:(OI)(CI)F /t /c /q >> "%_AGENTOPERATIONSLOGFILE%"
if %ERRORLEVEL% NEQ 0 (
    echo Failed! ERRORLEVEL:%ERRORLEVEL% >> "%_AGENTOPERATIONSLOGFILE%"
    echo Failed with error level %ERRORLEVEL%
)
goto :eof

:nofile
echo Cannot find icacls.exe in %windir%\System32 folder. >> "%_AGENTOPERATIONSLOGFILE%"
echo Cannot find icacls.exe in %windir%\System32 folder.

:nodir
echo Cannot set restrictive folder permissions on %targetFolder%. >> "%_AGENTOPERATIONSLOGFILE%"
echo Cannot set restrictive folder permissions on %targetFolder%.
goto :eof


rem
rem HQ Agent specific commands
rem

:ping
%PING_CMD%
IF %ERRORLEVEL% NEQ 0 (echo Ping failed!) else echo Ping success!
goto :eof

:setup
if %ERRORLEVEL% EQU 0 ( call :start )
echo %SETUP_CMD% >> "%_AGENTOPERATIONSLOGFILE%"
%SETUP_CMD%
goto :eof

:setup-if-no-provider
echo %SETUP_IF_NO_PROVIDER_CMD% >> "%_AGENTOPERATIONSLOGFILE%"
%SETUP_IF_NO_PROVIDER_CMD%
goto :eof
