Back to main [README](../README.md)

# Machine setup instructions

## Table of contents

- [Machine setup instructions](#machine-setup-instructions)
- [Install Appium and related drivers & plugins](#install-appium-and-related-drivers--plugins)
- [Start an Android device emulator](#start-an-android-device-emulator)
- [Machine Connectivity Check](#machine-connectivity-check)
  - [Instructions for Windows OS](#instructions-for-windows-os)
  - [Instructions for Linux / OSX OS](#instructions-for-linux--osx-os)

Follow the below steps to get your machine setup ready. These steps are for Selenium-Java based Test Automation. If you are using any other combination, please contact anand.bagmar@essenceoftesting.com
- Install JDK 17 or higher
- Clone this git repo (https://github.com/anandbagmar/getting-started-with-visualtesting) on your laptop
- Open the cloned project in your IDE as a Gradle project. This will automatically download all the dependencies
- Once all dependencies are downloaded, run the following command from command prompt / terminal window:

  **macOS / Linux** (bash/zsh):
  ```bash
  ./gradlew clean test --tests CalculatorTest
  ```

  **Windows** (Command Prompt or PowerShell):
  ```
  gradlew.bat clean test --tests CalculatorTest
  ```

# Install Appium and related drivers & plugins

The NPM package manager can install the Appium service package and all of its related
dependencies and utilities for you.

Run this command from a terminal prompt in the project root directory where the
`package.json` file is stored.
```bash
npm install
```

# Start an Android device emulator

[Create an Android Virtual Device in Android Studio.](https://developer.android.com/studio/run/managing-avds)

[Run an emulator in Andriod Studio.](https://developer.android.com/studio/run/managing-avds#emulator)


# Machine Connectivity Check

Run the following commands on your laptops to ensure connectivity to the Applitools server.
The response status code for each of these methods should be 2xx / 3xx.

## Instructions for Windows OS:

**PowerShell** — run the following commands and note the response status code:
```powershell
curl.exe -I https://eyes.applitools.com
curl.exe -I https://eyesapi.applitools.com
```
(Use `curl.exe` rather than plain `curl` — PowerShell aliases `curl` to
`Invoke-WebRequest`, which takes different parameters. Alternatively, use PowerShell's
native cmdlet:)
```powershell
Invoke-WebRequest -Method GET https://eyes.applitools.com
Invoke-WebRequest -Method GET https://eyesapi.applitools.com
```

**Command Prompt (cmd.exe)** — run the following commands and note the response status code:
```
curl -I https://eyes.applitools.com
curl -I https://eyesapi.applitools.com
```

If you get an error in the console / terminal window with message such as FORBIDDEN / ACCESS DENIED / PROXY ERROR / etc., then try the same commands by providing the proxy details:

NOTE: Based on your network configuration, the `-ProxyCredential` parameter may need to be specified

```powershell
Invoke-WebRequest -Method GET -Proxy <proxy-url> -ProxyCredential (Get-Credential) https://eyes.applitools.com
Invoke-WebRequest -Method GET -Proxy <proxy-url> -ProxyCredential (Get-Credential) https://eyesapi.applitools.com
```

## Instructions for Linux / OSX OS:
Run the following commands in a terminal window and note the response status code:

```bash
curl -I https://eyes.applitools.com
curl -I https://eyesapi.applitools.com
```

If you get an error in the console / terminal window with message such as FORBIDDEN / ACCESS DENIED / PROXY ERROR / etc., then try the same commands by providing the proxy details:

NOTE: Based on your network configuration, the `-x` (proxy) and `-U` (proxy user) parameters may need to be specified
```bash
curl -I -x <proxy-host>:<proxy-port> -U <username>:<password> https://eyes.applitools.com
curl -I -x <proxy-host>:<proxy-port> -U <username>:<password> https://eyesapi.applitools.com
```

If you are still getting an error response, then you will need to get the following URLs whitelisted on your network:
- https://render-wus.applitools.com
- https://eyesapi.applitools.com
- https://eyes.applitools.com
- https://eyespublicwusi0.blob.core.windows.net 


Back to main [README](../README.md)
