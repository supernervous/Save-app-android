
LibNearbyShare is an Android library that is meant to simplify the discovery of other LibNearbyShare enabled Android devices nearby, and sharing data with them. It attemps to unify all possible nearby network technologies under a simple, clean interface that any developer can implement without too much trouble or overhead.

This project is still under heavy development, and does not yet have a finalized API or implementation. However, as we are doing work to unify and organize code for effectively using Bluetooth, WiFi LAN/NSD sharing, and Wifi P2P Sharing, we felt it is still useful even in its current form.

License
-------

This library is licensed under the LGPLv2.1.  We believe this is compatible
with all reasonable uses, including proprietary software, but let us know if
it provides difficulties for you.  For more info on how that works with Java,
see:

https://www.gnu.org/licenses/lgpl-java.en.html

Using the Library
-------

```java
private void doThatHttpThing() {
  try {
    StrongOkHttpClientBuilder
      .forMaxSecurity(this)
      .build(this);
  }
  catch (Exception e) {
    // do something useful
  }
}
```

Get help
------------------

Do not hesitate to contact us with any questions. The best place to start is our community forums and https://devsq.net. To send a direct message, email support@guardianproject.info

We want your feedback! Please report any problems, bugs or feature requests to our issue tracker on this repo.
