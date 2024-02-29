# App Integrity

The purpose of the integrity api is check with the google play store that the device and application have not been tampered with.

An application on a rooted device can completely bypass encryption amongst other scary stuff like modifying the application.

`Themis` (greek god of protection) is the code name for a small API to validate a token with play integrity API.

## Setup

1. Configure API properties in `$HOME/.gradle/gradle.properties`

   ```properties
    oaCloudProjectNumber=123456789L # the cloud project with play integrity api enabled
    oaThemisDomain="themis.ryan-059.workers.dev" # the api to validate with
    oaThemisApiKey="XXXXX" # the API key for themis (get from themis)
    oaThemisIntegrityToken=""  # the integrity token to verify (must match on themis config)
   ```

## Flow

1. Request an integrity token from google API. This token must be decrypted by a backend.
2. Request the verdict from a backend. Currently setup on a cloudflare worker.
3. Take action on verdict. Either a dialog to download the app, or to block the application from running.

## Improvements

1. Frequency

There is documentation that the google api calls can be periodic and have to stay within the limits of allowed api calls.

For this reason is probably better to run as a background service, that runs at first boot and very periodically, with exponential backoff.

2. Whitelisting / Blacklisting

By including device/user information to themis, we can allow or deny certain use cases.
