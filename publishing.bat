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
REM   4) create the vX.Y.Z git tag and push it — that is what triggers the
REM      "Release" GitHub Actions workflow (.github/workflows/release.yml) via its
REM      `on: push: tags: 'v*'` path. Then show the run so it can be watched.
REM
REM Why a tag push and not `gh workflow run` (workflow_dispatch): the tag path
REM has the long track record here, needs no gh scopes, and the workflow's
REM baked-in play_track default is already `production` — exactly what a
REM publishing run wants. softprops/action-gh-release attaches the build to the
REM vX.Y.Z tag/release this pushes.

setlocal enabledelayedexpansion

cd /d "%~dp0"

where gh >nul 2>nul
if errorlevel 1 (
    echo WARNING: gh CLI not found — the release will still be triggered by the
    echo tag push, but this script can't show/watch the run. Install it from
    echo https://cli.github.com to get run watching.
    set NO_GH=1
)

if not exist version.properties (
    echo ERROR: version.properties not found. Run this from the repo root.
    pause
    exit /b 1
)

REM --- Determine current version ---
for /f "usebackq tokens=1,2 delims==" %%A in ("version.properties") do (
    if "%%A"=="VERSION_NAME" set CURRENT_NAME=%%B
    if "%%A"=="VERSION_CODE" set CURRENT_CODE=%%B
)

if "%CURRENT_NAME%"=="" (
    echo ERROR: could not read VERSION_NAME from version.properties.
    pause
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
    pause
    exit /b 1
)

REM --- Play Store track: the workflow's own default (production) is used on a
REM     tag push; shown here just so the confirm screen is explicit. ---
set PLAY_TRACK=production

REM --- Bail early if the tag already exists (locally or on origin) ---
git rev-parse -q --verify "refs/tags/v!NEW_NAME!" >nul 2>nul
if not errorlevel 1 (
    echo ERROR: tag v!NEW_NAME! already exists locally. Pick another version, or
    echo delete it with: git tag -d v!NEW_NAME!
    pause
    exit /b 1
)
git ls-remote --exit-code --tags origin "v!NEW_NAME!" >nul 2>nul
if not errorlevel 1 (
    echo ERROR: tag v!NEW_NAME! already exists on origin. Pick another version.
    pause
    exit /b 1
)

REM --- Show what will change, and confirm ---
echo.
echo ============================================
echo   version.properties will change:
echo     VERSION_NAME=%CURRENT_NAME%  -^>  !NEW_NAME!
echo     VERSION_CODE=%CURRENT_CODE%  -^>  !NEW_CODE!
echo   Play Store track: %PLAY_TRACK% (workflow default)
echo   Git tag to push:  v!NEW_NAME!
echo ============================================
echo.
echo   All of the following pending changes will be committed and pushed:
git status --short
echo.
set /p CONFIRM="Bump the version, commit + push ALL of the above to main, then tag and push v!NEW_NAME! to trigger the Release workflow? (y/N): "
if /i not "%CONFIRM%"=="y" (
    echo Aborted. No changes made.
    goto :end
)

REM --- Update version.properties in place, preserving the header comments ---
powershell -NoProfile -Command ^
  "(Get-Content version.properties -Raw) -replace 'VERSION_NAME=.*', 'VERSION_NAME=!NEW_NAME!' -replace 'VERSION_CODE=.*', 'VERSION_CODE=!NEW_CODE!' | Set-Content version.properties -NoNewline"
if errorlevel 1 (
    echo ERROR: failed to update version.properties.
    pause
    exit /b 1
)

REM --- Stage and commit EVERYTHING (version bump + any other pending changes),
REM     so the release is built from exactly what's on disk right now ---
git add -A
if errorlevel 1 (
    echo ERROR: git add failed.
    pause
    exit /b 1
)

git commit -m "Bump to v!NEW_NAME!"
if errorlevel 1 (
    echo ERROR: git commit failed.
    pause
    exit /b 1
)

git push origin main
if errorlevel 1 (
    echo ERROR: git push origin main failed. The commit is local only; fix the
    echo push ^(e.g. pull/rebase^) and re-run, or push manually then tag with:
    echo   git tag v!NEW_NAME! ^&^& git push origin v!NEW_NAME!
    pause
    exit /b 1
)

REM --- Tag the just-pushed commit and push the tag — this triggers release.yml ---
git tag "v!NEW_NAME!"
if errorlevel 1 (
    echo ERROR: git tag v!NEW_NAME! failed. main is already pushed; create and
    echo push the tag manually: git tag v!NEW_NAME! ^&^& git push origin v!NEW_NAME!
    pause
    exit /b 1
)

git push origin "v!NEW_NAME!"
if errorlevel 1 (
    echo ERROR: pushing tag v!NEW_NAME! failed. main is already pushed; retry:
    echo   git push origin v!NEW_NAME!
    pause
    exit /b 1
)

echo.
echo Tag v!NEW_NAME! pushed — the Release workflow should start shortly.

if defined NO_GH goto :done_nogh

echo Waiting for the run to register...
timeout /t 10 /nobreak >nul

set RUN_ID=
for /f "usebackq tokens=* delims=" %%i in (`gh run list --workflow=release.yml --limit 1 --json databaseId --jq ".[0].databaseId"`) do set RUN_ID=%%i

if "%RUN_ID%"=="" (
    echo Could not resolve the run id yet; list it with:
    echo   gh run list --workflow=release.yml --limit 3
    goto :done
)

echo Watching release run %RUN_ID% ^(Ctrl+C to stop watching -- the run keeps going^):
gh run watch %RUN_ID% --exit-status
goto :done

:done_nogh
echo Check progress at: https://github.com/bobadronov/debt-tracker/actions/workflows/release.yml

:done
echo.
echo When it completes, run "git fetch --tags" — the v!NEW_NAME! tag is already
echo on origin; the workflow's github-release job attaches the build to it.

:end
endlocal
pause
