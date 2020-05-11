# cordova-plugin-gpay

Cordova plugin for Master Card - Google Pay integration

## Notes
This plugin only supports Android.

This plugin will add these dependencies to your build.gradle file:

    com.google.android.gms:play-services-wallet:15.0.0
    com.android.support:support-v13:27.+
    com.android.support:appcompat-v7:27.1.0
    

## Installation
    cordova plugin add cordova-plugin-gpay

## Quick Example

Run below on onDeviceReady function

```javascript
  GooglePay.isReadyToPay().then(function() {
    GooglePay.requestPayment(1000, 'AED').then(function(token) {
      alert(token);
    }).catch(function(err) {
      alert(err);
    });
  }).catch(function(err) {
    alert(err);
  });

```

## Usage
  This plugin puts the functions into `window.GooglePay` .
  All functions return a promise.

```javascript
GooglePay.isReadyToPay()
```
-  Used to test if the appropriate payment method is available on the current device.
-  Resolves if appropriate payment method is available
-  Rejects if not, or if it encounters an error

```javascript
GooglePay.requestPayment(totalPrice, currency)
```

-  Initiates the payment journey for the user to complete.
-  `totalPrice` must be a string representation of the total price - e.g. for £10.78, it would be 10.78
-  `currency` must be a valid ISO 4217 currency code for the transaction
-  Resolves when the journey is complete, with the payment gateway token
-  Rejects if an error occurs

## Switch to PRODUCTION

You need to request for production access before test on production environment. Carefully go through the [CHECK LIST](https://developers.google.com/pay/api/android/guides/test-and-deploy/integration-checklist) mentioned in this [LINK](https://developers.google.com/pay/api/android/guides/test-and-deploy/integration-checklist) and [REQUEST PRODUCTION ACCESS](https://services.google.com/fb/forms/googlepayAPIenable)

## Why this error after switching to PRODUCTION?

#### Request Failed

Make sure you check the APK on a real device and not on an emulator.


