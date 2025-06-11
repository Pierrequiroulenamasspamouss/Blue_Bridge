@echo off
setlocal

:: Define source and destination paths
set "SOURCE=Server"
set "DEST=server.zip"

:: Go to the base directory (CurrentDirectory)
cd /d "%~dp0"

:: Delete old zip if it exists
if exist "%DEST%" del "%DEST%"

:: Create a temporary folder for the files to be zipped
set "TEMP_FOLDER=%temp%\server_zip_temp"
if exist "%TEMP_FOLDER%" rmdir /s /q "%TEMP_FOLDER%"
mkdir "%TEMP_FOLDER%"
echo server.zip created in %cd%
echo server.zip created in %cd%

:: Copy all files and folders except the excluded SQLite files
xcopy "%SOURCE%\APK" "%TEMP_FOLDER%\APK" /E /I /Y
xcopy "%SOURCE%\assets" "%TEMP_FOLDER%\assets" /E /I /Y
xcopy "%SOURCE%\config" "%TEMP_FOLDER%\config" /E /I /Y

:: Copy data folder excluding specific SQLite files
mkdir "%TEMP_FOLDER%\data"
for /f "delims=" %%F in ('dir /b /a-d "%SOURCE%\data\*" ^| findstr /v /i "users.sqlite deviceTokens.sqlite wells.sqlite"') do (
    copy "%SOURCE%\data\%%F" "%TEMP_FOLDER%\data\%%F" /Y
)

xcopy "%SOURCE%\html" "%TEMP_FOLDER%\html" /E /I /Y
xcopy "%SOURCE%\middleware" "%TEMP_FOLDER%\middleware" /E /I /Y
xcopy "%SOURCE%\models" "%TEMP_FOLDER%\models" /E /I /Y
xcopy "%SOURCE%\routes" "%TEMP_FOLDER%\routes" /E /I /Y
xcopy "%SOURCE%\scripts" "%TEMP_FOLDER%\scripts" /E /I /Y
xcopy "%SOURCE%\services" "%TEMP_FOLDER%\services" /E /I /Y
xcopy "%SOURCE%\ssl" "%TEMP_FOLDER%\ssl" /E /I /Y

:: Copy individual files
copy "%SOURCE%\.env" "%TEMP_FOLDER%\" /Y
copy "%SOURCE%\.greenlockrc" "%TEMP_FOLDER%\" /Y
copy "%SOURCE%\install.sh" "%TEMP_FOLDER%\" /Y
copy "%SOURCE%\package.json" "%TEMP_FOLDER%\" /Y
copy "%SOURCE%\README.md" "%TEMP_FOLDER%\" /Y
copy "%SOURCE%\routeExplorer.js" "%TEMP_FOLDER%\" /Y
copy "%SOURCE%\server.bat" "%TEMP_FOLDER%\" /Y
copy "%SOURCE%\server.js" "%TEMP_FOLDER%\" /Y

:: Create the zip file using PowerShell
powershell -Command "Compress-Archive -Path '%TEMP_FOLDER%\*' -DestinationPath '%DEST%' -Force"

:: Clean up temporary folder
rmdir /s /q "%TEMP_FOLDER%"

echo server.zip created in %cd%

