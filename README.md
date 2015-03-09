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


Build Configuration
-------

Using the jcenter repository, add the following line to the gradle dependencies for your module.
```groovy
compile 'me.denley.courier:courier:0.4.1'
```

Details
-------

- All callbacks are made on the main thread.
- `@DeliverData` callbacks will be invoked immediately after calling `Courier.startReceiving`, but asynchronously.
- `@DeliverData` callbacks will also be called immediately when the device connects to another device.
- `@DeliverMessage` callbacks will only be invoked at the time that a message is received from the `MessageApi` (they are missed if the device is disconnected).
- Transferred objects are serialized in a raw form (objects must implement java's ``Serializable` interface). This is high priority issue as it limits forward compatibility. It will be addressed before the first stable release.
- If an empty message is sent or if a data item is removed, a `null` object will be passed to the listener. Be sure to check for `null` values.

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