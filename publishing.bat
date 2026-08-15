@echo off
REM release.bat — interactively bump the app version and push a release
REM that uploads to a chosen Google Play track.
REM
REM Flow:
REM   1) enter the new version number (blank = auto-bump the patch number)
REM   2) select the Play Store track (internal / alpha / beta / production)
REM   3) confirm, then update version.properties, commit, and push to main
REM   4) dispatch the "Release" GitHub Actions workflow (.github/workflows/release.yml)
REM
REM NOTE: this intentionally does NOT push a git tag. Pushing a `v*` tag
REM separately triggers the workflow's own `on: push: tags:` path, which
REM always uses play_track=internal (workflow_dispatch inputs aren't visible
REM to tag-push-triggered runs) — that would kick off a second, redundant
REM full build. softprops/action-gh-release creates the `vX.Y.Z` tag itself
REM once this dispatched run reaches the github-release job, so the tag still
REM ends up in the repo; run `git fetch --tags` afterwards to see it locally.

setlocal enabledelayedexpansion

cd /d "%~dp0"

where gh >nul 2>nul
if errorlevel 1 (
    echo ERROR: gh CLI not found. Install it from https://cli.github.com and run "gh auth login".
    exit /b 1
)

if not exist version.properties (
    echo ERROR: version.properties not found. Run this from the repo root.
    exit /b 1
)

REM --- Determine current version ---
for /f "usebackq tokens=1,2 delims==" %%A in ("version.properties") do (
    if "%%A"=="VERSION_NAME" set CURRENT_NAME=%%B
    if "%%A"=="VERSION_CODE" set CURRENT_CODE=%%B
)

if "%CURRENT_NAME%"=="" (
    echo ERROR: could not read VERSION_NAME from version.properties.
    exit /b 1
)

echo Current version: %CURRENT_NAME% (code %CURRENT_CODE%)
echo.

REM --- Ask for the version number ---
set /p NEW_NAME="Enter new version number (blank = auto-bump patch from %CURRENT_NAME%): "
if "%NEW_NAME%"=="" (
    for /f "tokens=1,2,3 delims=." %%a in ("%CURRENT_NAME%") do (
        set MAJOR=%%a
        set MINOR=%%b
        set /a PATCH=%%c+1
    )
    set NEW_NAME=!MAJOR!.!MINOR!.!PATCH!
)
set /a NEW_CODE=%CURRENT_CODE%+1

REM --- Select Play Store track ---
echo.
set PLAY_TRACK=
:ask_track
echo Select Play Store track:
echo   1) internal
echo   2) alpha
echo   3) beta
echo   4) production
set /p TRACK_CHOICE="Enter 1-4: "
if "%TRACK_CHOICE%"=="1" set PLAY_TRACK=internal
if "%TRACK_CHOICE%"=="2" set PLAY_TRACK=alpha
if "%TRACK_CHOICE%"=="3" set PLAY_TRACK=beta
if "%TRACK_CHOICE%"=="4" set PLAY_TRACK=production
if "%PLAY_TRACK%"=="" (
    echo Invalid choice, try again.
    echo.
    goto ask_track
)

REM --- Show what will change, and confirm ---
echo.
echo ============================================
echo   version.properties will change:
echo     VERSION_NAME=%CURRENT_NAME%  -^>  !NEW_NAME!
echo     VERSION_CODE=%CURRENT_CODE%  -^>  !NEW_CODE!
echo   Play Store track: %PLAY_TRACK%
echo ============================================
echo.
set /p CONFIRM="Commit, push to main, and dispatch the Release workflow now? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo Aborted. No changes made.
    goto :end
)

REM --- Update version.properties in place, preserving the header comments ---
powershell -NoProfile -Command ^
  "(Get-Content version.properties -Raw) -replace 'VERSION_NAME=.*', 'VERSION_NAME=!NEW_NAME!' -replace 'VERSION_CODE=.*', 'VERSION_CODE=!NEW_CODE!' | Set-Content version.properties -NoNewline"
if errorlevel 1 (
    echo ERROR: failed to update version.properties.
    exit /b 1
)

REM --- Warn about any other uncommitted changes; this script only commits version.properties ---
git status --porcelain | findstr /v /i "version.properties" > "%TEMP%\dtr_release_status.tmp"
for %%A in ("%TEMP%\dtr_release_status.tmp") do set OTHER_DIRTY_SIZE=%%~zA
del "%TEMP%\dtr_release_status.tmp" >nul 2>nul
if not "%OTHER_DIRTY_SIZE%"=="0" (
    echo NOTE: you have other uncommitted changes besides version.properties.
    echo They will NOT be included in this release commit. Run "git status" to review.
    echo.
)

git add version.properties
git commit -m "Bump to v!NEW_NAME!"
if errorlevel 1 (
    echo ERROR: git commit failed.
    exit /b 1
)

git push origin main
if errorlevel 1 (
    echo ERROR: git push origin main failed.
    exit /b 1
)

echo.
echo Dispatching Release workflow ^(play_track=%PLAY_TRACK%^) for v!NEW_NAME!...
gh workflow run release.yml --ref main -f release_tag=v!NEW_NAME! -f play_track=%PLAY_TRACK%
if errorlevel 1 (
    echo ERROR: gh workflow run failed. The version bump was already pushed to main;
    echo you can retry the dispatch manually with:
    echo   gh workflow run release.yml --ref main -f release_tag=v!NEW_NAME! -f play_track=%PLAY_TRACK%
    exit /b 1
)

echo Waiting for the run to register...
timeout /t 8 /nobreak >nul
gh run list --workflow=release.yml --limit 3

echo.
echo To watch it live, run:
echo   gh run watch --exit-status
echo.
echo Once it completes, run "git fetch --tags" to pull down the v!NEW_NAME! tag
echo that the release job creates on GitHub.

:end
endlocal
