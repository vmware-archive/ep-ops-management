=====================================================================================                  

     End Point Operations Management Agent, version 6.x, build ${BUILD_NUMBER} 

=====================================================================================


Starting and Stopping the End Point Operations Management Agent on Windows:

On Windows, you have the choice of starting and stopping the End Point Operations
Management Agent from the Windows Service Manager or from a Windows Command Prompt. 
The Service Manager is recommended for starting the End Point Operations Management Agent 
because services remain running even if you logout of the Windows user session 
that you're logged-in to when starting the server.

-------------------------------------------------------------------------------------
To start the End Point Operations Management Agent from the Windows Service Manager:

1. Start the services application from the Windows Administrative Tools menu or
   Windows Control Panel depending on the version of Windows you're running.
   
2. Select the End Point Operations Management Agent service.

3. Click the start icon or click the start link.


-------------------------------------------------------------------------------------
To stop the End Point Operations Management Agent from the Windows Service Manager:

1. Start the services application from the Windows Administrative Tools menu or
   Windows Control Panel depending on the version of Windows you're running.
   
2. Select the End Point Operations Management Agent service.

3. Click the stop icon or click the stop link.


-------------------------------------------------------------------------------------
To start the End Point Operations Management Agent from the Windows Command Prompt:

1. Open a Windows Command Prompt Window

2. Execute the following command:
	 bin\ep-agent.bat start

Note: Logging out of the Windows user session will shutdown the End Point 
	  Operations Management Agent. You should start the End Point Operations Management
	  Agent from the Windows Service Manager, if you want the End Point Operations 
          Management Agent to run after you logout of your Windows user session.


-------------------------------------------------------------------------------------
To stop the End Point Operations Management Agent from the Windows Command Prompt:

1. Open a Windows Command Prompt Window

2. Execute the following command:
	 bin\ep-agent.bat stop


=====================================================================================


Starting and Stopping the End Point Operations Management Agent on Linux / UNIX:

-------------------------------------------------------------------------------------
To start the End Point Operations Management Agent, execute this command:

 ep-agent.sh start


-------------------------------------------------------------------------------------
To stop the End Point Operations Management Agent, execute this command:

 ep-agent.sh stop

 
=====================================================================================


Reading the End Point Operations Management Agent Log Files
-------------------------------------------------------------------------------------
The End Point Operations Management Agent log files are located in the 'log' 
sub-directory where the End Point Operations Management Agent is installed. 
The active log file is always named 'agent.log'. The log is a text file and 
can be opened and read with any text editor or the Windows Notepad application.
