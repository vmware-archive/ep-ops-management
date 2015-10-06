@echo off
setlocal

if "%OS%"=="Windows_NT" goto nt
echo This script only works with NT-based versions of Windows.
goto :eof

:nt
rem
rem Find the application home.
rem
rem %~dp0 is location of current script under NT
set _AGENTHOME=%~dp0..\

rem Define the install log and log the time
rem Find the requested command. we would like to log only the important operations.
rem Define variables for loggin.
set _AGENTOPERATIONSLOGFOLDER=%_AGENTHOME%log
if not exist "%_AGENTOPERATIONSLOGFOLDER%" (
      mkdir "%_AGENTOPERATIONSLOGFOLDER%"
)
set _AGENTOPERATIONSLOGFILE=%_AGENTOPERATIONSLOGFOLDER%\agent.operations.log
For /f "tokens=2-4 delims=/ " %%a in ('date /t') do (set mydate=%%c-%%a-%%b)
For /f "tokens=1-2 delims=/:" %%a in ('time /t') do (set mytime=%%a:%%b)


rem Create header of execution: 2 lines, command, date_time.
echo Logs can be found at: %_AGENTOPERATIONSLOGFILE%
echo ============================== >> "%_AGENTOPERATIONSLOGFILE%"
echo ============================== >> "%_AGENTOPERATIONSLOGFILE%"
echo Command called: %1 at %mydate%_%mytime% >> "%_AGENTOPERATIONSLOGFILE%"


echo cd to AGENTHOME: %_AGENTHOME% >> "%_AGENTOPERATIONSLOGFILE%"
cd %_AGENTHOME%

set ROLLBACK_PROPS=conf\rollback.properties
set AGENT_BUNDLE_PROP=set.HQ_AGENT_BUNDLE

rem look for the agent bundle property in the rollback properties file >> "%_AGENTOPERATIONSLOGFILE%")
rem and invoke the bundle hq-agent.bat script
echo Looping over records in %ROLLBACK_PROPS%, searching for value of %AGENT_BUNDLE_PROP%  >> "%_AGENTOPERATIONSLOGFILE%"
for /F "delims== tokens=1*" %%i in (%ROLLBACK_PROPS%) do (
  if "%%i"=="%AGENT_BUNDLE_PROP%" (
    echo Found: %AGENT_BUNDLE_PROP%=%%j  >> "%_AGENTOPERATIONSLOGFILE%"
    echo Looking for internal file: bundles\%%j\bin\ep-agent.bat >> "%_AGENTOPERATIONSLOGFILE%"
    if not EXIST bundles\%%j\bin\ep-agent.bat (
      echo Failed to find bundle script "%_AGENTHOME%bundles\%%j\bin\ep-agent.bat". >> "%_AGENTOPERATIONSLOGFILE%"
      echo Failed to find bundle script "%_AGENTHOME%bundles\%%j\bin\ep-agent.bat".
    ) else (
      echo Found internal file: bundles\%%j\bin\ep-agent.bat >> "%_AGENTOPERATIONSLOGFILE%"
      if /i "%1"=="set-property" (
        echo call bundles\%%j\bin\hq-agent-nowrapper.bat %* >> "%_AGENTOPERATIONSLOGFILE%"
        call bundles\%%j\bin\hq-agent-nowrapper.bat %*
      ) else (
        echo call bundles\%%j\bin\ep-agent.bat %* >> "%_AGENTOPERATIONSLOGFILE%"
        call bundles\%%j\bin\ep-agent.bat %*
      )
    )
  )
)
