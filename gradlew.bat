@rem
@rem Copyright 2012 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      http://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == """" @echo off
@rem ##########################################################################
@rem
@rem  Gradle wrapper script for Windows
@rem
@rem ##########################################################################

@rem Set default for JAVA_HOME if not already set.
if not defined JAVA_HOME (
    for /f ""tokens=*"" %%i in ('where /q java') do (
        for /f ""delims=\ tokens=1*"" %%j in ('echo %%i') do (
            set "JAVA_HOME=%%j"
        )
    )
)

@rem Determine the script directory.
set SCRIPT_DIR=%~dp0

@rem Determine the Gradle distribution.
set GRADLE_DISTRIBUTION_URL=https://services.gradle.org/distributions/gradle-8.2-bin.zip
set GRADLE_DISTRIBUTION_SHA256=a9b0c8e7f6d5b4c3a2b1e0d9c8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d2e1f0a9 REM Placeholder, replace with actual SHA256

@rem Execute Gradle.
"%JAVA_HOME%\bin\java" -jar "%SCRIPT_DIR%\gradle-wrapper.jar" %*
