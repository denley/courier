[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-Courier-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1644)

# Courier
A delivery service for Android Wear. Courier uses `Wearable.DataApi` and `Wearable.MessageApi` to deliver objects between devices simply and cleanly.


Usage
-------

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


### Nodes (Connected Devices)

You can retrieve a list of connected devices using the `@RemoteNodes` annotation. When devices are connected or disconnected, the callback will be invoked again.

```java
@RemoteNodes
void onConnectionStateChanged(List<Node> connectedNodes) {
    // Do something with the nodes
    // ...
}
```

You can also retrieve the local node using the `@LocalNode` annotation. This will only be updated once, as it never changes.

```java
@LocalNode
Node localNode;
```

Alternatively, you can retrieve the local node by calling `Courier.getLocalNode(context)`. This must be done on a background thread.

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

To be delivered between devices, objects must be serialized into a byte array. This can be done in two ways:

1. Annotate your class with the `@Deliverable` interface. This will generate a utility class that will convert your object into a `DataMap` (and back again).
2. Have your class implement java's `Serializable` interface. This restricts your ability to change the class`s structure in the future. As such, it is not recommended. However, this means that you can send raw primitives (or Strings, etc.) as messages or data using `Courier`.

Example:

```java
@Deliverable
public class SmsDescriptor {

    String sender;
    String messageText;
    long timestamp;

}
```

This `DataMap` serialization process supports arbitrary object types as fields, as long as the object's class is also annotated with `@Deliverable` or implements `Serializable`.
`Asset`s can also be included as fields. Though this can only be used with the DataApi (not the MessageApi). For convenience, `Asset`s can be opened using the `Courier.getAssetInputStream` method.

### WearableListenerService

Often you will want to listen for message and data events outside of your 'Activity' using a [WearableListenerService](https://developer.android.com/training/wearables/data-layer/events.html#Listen).

`Courier` is completely compatible with `WearableListenerService`. In this class your code will look very similar with or without using `Courier`, but `Courier` can help you to unpack any messages/data that were sent using `Courier.deliverMessage` or `Courier.deliverData`.

Example:

```java
@Override public void onMessageReceived(@NonNull MessageEvent messageEvent) {
    if (messageEvent.getPath().equals("/incoming_sms")) {
        SmsDescriptor mySms = Packager.unpack(messageEvent.getData(), SmsDescriptor.class);

        // Do something with the message
        // ...
    }
}

@Override public void onDataChanged(DataEventBuffer dataEvents) {
    for(DataEvent event:dataEvents) {
        if (event.getUri().getPath().equals("/username")) {
            String username = Packager.unpack(event.getDataItem(), String.class);

            // Do something with the data
            // ...
        }
    }
}
```

The `Courier.getLocalNode` convenience method can also be useful in a `WearableListenerService`, as you might want to ignore data events that are sent from the local device.

Build Configuration
-------

Using the jcenter repository, add the following line to the gradle dependencies for your module.
```groovy
compile 'me.denley.courier:courier:1.0.0'
```

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
-keep @me.denley.courier.* public class *
```

Details
-------

- `@DeliverData` callbacks will be invoked immediately after calling `Courier.startReceiving` (but asynchronously).
- `@DeliverData` callbacks will also be called immediately when the device connects to a device.
- `@DeliverMessage` callbacks will only be invoked at the time that a message is received from the `MessageApi` (they are missed if the device is disconnected).
- If an empty message is sent or if a data item is removed, a `null` object will be passed to the listener. Be sure to check for `null` values!

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