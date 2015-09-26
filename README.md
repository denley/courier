[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Courier-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1644)

# Courier
A delivery service for Android Wear. Courier uses `Wearable.DataApi` and `Wearable.MessageApi` to deliver objects between devices simply and cleanly.


Usage
-------

### Build Dependency

Using the jcenter repository, add the following line to the gradle dependencies for your modules. You should add this to both your handheld and wearable modules.
```groovy
compile 'me.denley.courier:courier:1.2.0'
```

### Basic Usage

Simply add `@ReceiveMessages` and `@ReceiveData` annotations to your methods and fields to assign them as callbacks for `MessageApi` and `DataApi` events. Call `Courier.startReceiving(this)` to initialize the listeners and start receiving your callbacks.

```java
public class MainActivity extends Activity  {

    @ReceiveData("/username")
    String loggedInUser;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Courier.startReceiving(this);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        Courier.stopReceiving(this);
    }

    @ReceiveMessages("/incoming_sms")
    void onSmsReceived(SmsDescriptor smsMessage, String nodeId) { // The nodeId parameter is optional
        // ...
    }

}
```

<br/>
On the other device, use `Courier.deliverMessage` and `Courier.deliverData` to easily send data using the `MessageApi` and `DataApi`, respectively

```java
public void onLoginSuccess(String username) {
    Courier.deliverData(this, "/username", username);
}
```


### Checking for Connected Devices

#### Wearable API Status
Often it will be prudent to check whether or not the user has a wearable device paired with their phone. For this, you can use the following method:
```java
// This must be done on a background thread
boolean hasWearableDevice = Courier.isWearableApiAvailable(context);
```

Note: This is not the same as whether or not the watch is in range of the phone ("connected"). It determines whether the user has the `Android Wear` app installed and has paired a wearable device.

If this method returns false, all other method calls to the `Courier` class will simply be ignored, and return `null` where applicable.


#### Connected Devices
You can retrieve a list of connected devices using the `@RemoteNodes` annotation. When devices are connected or disconnected, the callback will be invoked again.
This represents devices that are paired, in range, and ready to send and receive data and messages.

```java
@RemoteNodes
void onConnectionStateChanged(List<Node> connectedNodes) {
    // Do something with the nodes
    // ...
}
```

#### Local Device
Sometimes having the local node can be useful. For example, you might want to ignore data items originating from the same device.

You can retrieve the local node using the `@LocalNode` annotation. This will only be updated once, as it never changes. Alternatively, you can retrieve the local node by calling `Courier.getLocalNode(context)`. This must be done on a background thread.

```java
@LocalNode
Node localNode;
```

Or:

```java
// This must be done on a background thread
localNode = Courier.getLocalNode(context);
```


### Threading

By default, method calls will be made on the main thread. If you want a callback to be made on a background thread, you can use the `@BackgroundThread` annotation on a method, like so:

```java
@BackgroundThread
@ReceiveMessages("/incoming_sms")
void onSmsReceived(SmsDescriptor smsMessage) {
    // ...
}
```

### Object Serialization

To be delivered between devices, objects must be able to be serialized into a byte array. Objects of any class implementing `Serializable` can be delivered. This includes primatives, Strings, and many other classes in the Android API.
For custom classes it is recommended to avoid relying on the `Serializable` interface, as it will restrict your ability to change your data structures in the future.

Instead, annotate your classes with Courier's `@Deliverable` annotation. This will automatically generate methods to convert your objects into a `DataMap` and back again. For example:

```java
@Deliverable
public class SmsDescriptor {

    String sender;
    String messageText;
    long timestamp;

}
```

`@Deliverable` classes support any field types that can be saved into a `DataMap` as well as any other `@Deliverable` or `Seriializable` object types.

`Asset`s can also be included as fields, and should be used for any large blobs of data (anything larger than a few kilobytes). When received, `Asset`s can be opened using the `Courier.getAssetInputStream` method. `@Deliverable` classes can also contain `Bitmap` fields, which will automatically be transferred as `Asset`s.


### WearableListenerService

Often you will want to listen for message and data events outside of your 'Activity' using a [WearableListenerService](https://developer.android.com/training/wearables/data-layer/events.html#Listen).

`Courier` is completely compatible with `WearableListenerService`. In this class your code will look very similar with or without using `Courier`, but `Courier` can help you to unpack any messages/data that were sent using `Courier.deliverMessage` or `Courier.deliverData`.

Example:

```java
@Override public void onMessageReceived(MessageEvent messageEvent) {
    if (messageEvent.getPath().equals("/incoming_sms")) {
        SmsDescriptor mySms = Packager.unpack(this, messageEvent.getData(), SmsDescriptor.class);

        // Do something with the message
        // ...
    }
}

@Override public void onDataChanged(DataEventBuffer dataEvents) {
    for(DataEvent event:dataEvents) {
        if (event.getUri().getPath().equals("/username")) {
            String username = Packager.unpack(this, event.getDataItem(), String.class);

            // Do something with the data
            // ...
        }
    }
}
```

The `Courier.getLocalNode` convenience method can also be useful in a `WearableListenerService`, as you might want to ignore data events that are sent from the local device.

### Miscellaneous

- `@DeliverData` callbacks will be invoked immediately after calling `Courier.startReceiving` (but asynchronously).
- `@DeliverData` callbacks will also be called immediately when the device connects to a device.
- `@DeliverMessage` callbacks will only be invoked at the time that a message is received from the `MessageApi` (they are missed if the device is disconnected).
- If an empty message is sent or if a data item is removed, a `null` object will be passed to the listener. Be sure to check for `null` values!

### Testing

Courier supports using mock implementations of the wearable API for unit testing. Simply call `Courier.attachMockDataApi`, `Courier.attachMockMessageApi`, and `Courier.attachMockNodeApi` to provide your testing API implementations.
Mocked APIs will be called with a null `GoogleApiClient` object.
Your mock APIs should be attached before making any calls to `Courier.startReceiving` and you should not attach new APIs until all targets have called `Courier.stopReceiving`.


### ProGuard Configuration

If you use ProGuard, you will need to add the following lines to your configuration. You will probably need to add this to the configurations for both your handheld and wearable modules.

```
-keep class me.denley.courier.** { *; }
-dontwarn me.denley.courier.compiler.**
-keep class **$$Delivery { *; }
-keep class **DataMapPackager { *; }
-keepclasseswithmembernames class * {
    @me.denley.courier.* <fields>;
}
-keepclasseswithmembernames class * {
    @me.denley.courier.* <methods>;
}
-keep @me.denley.courier.* public class * { *; }
```

License
-------

    Copyright 2015 Denley Bihari

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.