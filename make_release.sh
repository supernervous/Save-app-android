#!/usr/bin/env zsh

# from:  http://developer.android.com/tools/publishing/app-signing.html

./gradlew assembleRelease

apk=`find app/build/outputs/apk -name "*.apk"`

jarsigner -verbose -sigalg SHA256withRSA -digestalg SHA-256 -keystore openarchive-release.keystore "${apk}" android

jarsigner -verify -verbose -certs "${apk}"

versions=`ls $ANDROID_HOME/build-tools`
sortedVersions=($(sort <<<"${versions[*]}"))
newest=${sortedVersions[-1]}

"$ANDROID_HOME/build-tools/$newest/zipalign" -v 4 "${apk}" openarchive-release.apk
