## UriIO Android library

Android library for the [UriIO Ephemeral URL API](https://api.uriio.com/api)

The library takes care of creating and refreshing Eddystone-URL beacons, when the broadcasted URL is no longer valid server-side.

- [About ephemeral URLs](#ephemeral-urls)
   * [Register a redirected URL](#registering-a-redirected-long-url)
   * [Update destination URL](#updating-the-target-url)


### Ephemeral URLs

An ephemeral URL broadcasts an Eddystone-URL beacon, but it can dynamically change the beacon-advertised URL (and even the target URL).

UriIO is a cloud service for redirecting to timestamp-authenticated URLs, and requires an API key, which you can get by visiting the link below.

[Read more about the UriIO API and why it is secure and anti-spoofable.](https://uriio.com)

### Setup

1. Add the library to your application-level **build.gradle**

   ```groovy
   dependencies {
      ...
      compile 'com.uriio:uriio-android:1.0.1'
   }
   ```

2. Initialize the library in the `onCreate()` of your Application, or Activity, or Service:

   ```java
   Uriio.initialize(this);
   ```

  Note: you don't need to also call `Beacons.initialize()` since it's called for you.

3. Add your API key in your app's `strings.xml` and update your `AndroidManifest.xml` to reference it, inside the `<application>` tag:
 
   ```xml
      <meta-data android:name="com.uriio.apiKey" android:value="@string/uriio_api_key" />
   ```

### Registering a redirected long URL

The snippet below registers a new "long" URL destination and creates an Ephemeral URL beacon for it.
The `timeToLive` is in seconds; use 0 for an initially non-ephemeral URL. If non-zero,
the UriIO server will invalidate every beacon-advertised URL after it expires using a 404.
You can change the TTL at any time after an URL is registered


```java
String url = 'https://github.com/uriio/beacons-android';
int beaconTTL = 300;

Uriio.registerUrlAndAdvertise(url, beaconTTL, new Callback<EphemeralURL>() {
   @Override
   public void onResult(EphemeralURL beacon, Throwable error) {
      if (null != result) {
         // yey, URL registered and beacon created for it
         // you can modify the beacon here, but it might restart if you change TTL, TX power, or mode
         beacon.edit().setName("My first beacon").apply();
      }
      else {
         handleError(error);  // registration failed for whatever reason
      }
   }
});
```

If you'd like to customize the created beacon before it first starts, then do the registration
only, and add the beacon yourself after you modified its initial properties:

```java
String url = 'https://github.com/uriio/beacons-android';

Uriio.registerUrl(url, new Callback<UrlResource>() {
   @Override
   public void onResult(UrlResource result, Throwable error) {
      if (null != result) {
         // URL registered! Create a beacon for it
         EphemeralURL beacon = Uriio.createBeacon(result, beaconTimeToLive);
         beacon.edit()
                 .setAdvertiseMode(mode).setAdvertiseTxPower(txPowerLevel)
                 .setName(name)
                 .apply();
         // Add the beacon to the store and start it
         Beacons.add(beacon);
      }
      else {
         handleError(error);  // registration failed for whatever reason
      }
   }
});
```

The library will call the specific APIs for issuing periodically new short URLs, and recreate the Eddystone-URL beacon, according to the timeToLive property. If the TTL is zero, the beacon's URL remains the same.

### Updating the target URL

To update the target URL (with or without the need to change other beacon properties), use:

```java
// beacon is a EphemeralURL instance
Uriio().updateUrl(beacon, url, new Callback<EphemeralURL>() {
   @Override
   public void onResult(EphemeralURL beacon, Throwable error) {
      if (null != result) {
         // target URL was updated. From now on, the server will redirect to the new URL.
         // the updated beacon is the same instance you provided
      }
      else {
         showError(error);
      }
   }
});
```

### Interacting with Eddystone-URL broadcasted beacons

Use the usual strategies explained in the [Android BLE library](https://github.com/uriio/beacons-android). The broadcasted beacons are instances
of Eddystone-URL beacons, with some extra properties to allow refreshing their URLs when needed.

### Extract URL registration token

You can extract the *URL identifier* and *URL token* (the details needed to edit the server resource or issue new beacon URLs),
either after registration, or when listing `Beacons` that are instances of the subtype `EphemeralURL` class.
