@echo off
REM publishing.bat — interactively bump the app version and push a release
REM that uploads to the Google Play production track.
REM
REM Flow:
REM   1) enter the new VERSION_NAME (blank = auto-bump the patch number)
REM   2) enter the new VERSION_CODE (blank = auto-bump +1 from the current one;
REM      ask in case you already bumped it by hand in version.properties)
REM   3) confirm, then bump version.properties and commit + push ALL pending
REM      changes to main in one "Bump to vX.Y.Z" commit
REM   4) dispatch the "Release" GitHub Actions workflow (.github/workflows/release.yml)
REM      with play_track=production, and show the run so it can be watched in Actions
REM
REM NOTE: this intentionally does NOT push a git tag. Pushing a `v*` tag
REM separately triggers the workflow's own `on: push: tags:` path, which
REM uses the play_track default baked into release.yml (production) — that
REM would kick off a second, redundant full build. softprops/action-gh-release
REM creates the `vX.Y.Z` tag itself once this dispatched run reaches the
REM github-release job, so the tag still ends up in the repo; run
REM `git fetch --tags` afterwards to see it locally.

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

REM --- Ask for the version code (may already be bumped by hand) ---
set /a AUTO_CODE=%CURRENT_CODE%+1
set NEW_CODE=
set /p NEW_CODE="Enter new VERSION_CODE (blank = auto-bump %CURRENT_CODE% to !AUTO_CODE!): "
if "!NEW_CODE!"=="" set NEW_CODE=!AUTO_CODE!
echo !NEW_CODE!| findstr /r "^[0-9][0-9]*$" >nul
if errorlevel 1 (
    echo ERROR: VERSION_CODE must be a positive integer.
    exit /b 1
)

REM --- Play Store track: always production for a publishing run ---
set PLAY_TRACK=production

REM --- Show what will change, and confirm ---
echo.
echo ============================================
echo   version.properties will change:
echo     VERSION_NAME=%CURRENT_NAME%  -^>  !NEW_NAME!
echo     VERSION_CODE=%CURRENT_CODE%  -^>  !NEW_CODE!
echo   Play Store track: %PLAY_TRACK%
echo ============================================
echo.
echo   All of the following pending changes will be committed and pushed:
git status --short
echo.
set /p CONFIRM="Bump the version, commit + push ALL of the above to main, and dispatch the Release workflow now? (y/N): "
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

REM --- Stage and commit EVERYTHING (version bump + any other pending changes),
REM     so the release is built from exactly what's on disk right now ---
git add -A
if errorlevel 1 (
    echo ERROR: git add failed.
    exit /b 1
)

git commit -m "Bump to v!NEW_NAME!"
if errorlevel 1 (
    echo ERROR: git commit failed.
    exit /b 1
)

git push origin main
if errorlevel 1 (
    echo ERROR: git push origin main failed. The commit is local only; fix the
    echo push (e.g. pull/rebase) and re-run, or push manually then dispatch with:
    echo   gh workflow run release.yml --ref main -f release_tag=v!NEW_NAME! -f play_track=%PLAY_TRACK%
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

set RUN_ID=
for /f "usebackq tokens=* delims=" %%i in (`gh run list --workflow=release.yml --limit 1 --json databaseId --jq ".[0].databaseId"`) do set RUN_ID=%%i

if "%RUN_ID%"=="" (
    echo Could not resolve the run id; listing recent runs instead:
    gh run list --workflow=release.yml --limit 3
    echo Watch it with: gh run watch --exit-status
) else (
    echo Watching release run %RUN_ID% ^(Ctrl+C to stop watching -- the run keeps going^):
    gh run watch %RUN_ID% --exit-status
)

echo.
echo Once it completes, run "git fetch --tags" to pull down the v!NEW_NAME! tag
echo that the release job creates on GitHub.

:end
endlocal
