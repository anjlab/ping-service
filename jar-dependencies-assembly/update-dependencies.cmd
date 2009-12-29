ECHO off

SET cwd=%CD%

SET PINGSERVICE_WEBINF_LIB=%cwd%\..\ping-service\war\WEB-INF\lib
SET DEPENDENCIES_DIR=%cwd%\target\jar-dependencies-assembly-1.0-SNAPSHOT\WEB-INF\lib

ECHO ************************************************************************
ECHO *
ECHO * Getting recent (snapshot) dependencies...
ECHO *

CALL mvn clean package

ECHO ************************************************************************
ECHO *
ECHO * Updating jars in web app...
ECHO *

CD %PINGSERVICE_WEBINF_LIB%

FOR %%i IN (tapestry*.jar) DO DEL %%i

COPY /Y %DEPENDENCIES_DIR%\*.jar %PINGSERVICE_WEBINF_LIB%

ECHO ************************************************************************
ECHO *
ECHO * Updating filelist.txt file...
ECHO *

CD %PINGSERVICE_WEBINF_LIB%

CALL create-filelist.cmd

CD %cwd%