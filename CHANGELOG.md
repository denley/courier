# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

### 1.2.0 - 2015-09-26
- `@Deliverable` annotated classes can now contain fields that are ArrayLists of other `@Deliverable` annotated classes
- Update Google Play Services version to resolve its breaking changes.

### 1.1.0 - 2015-04-02
- Added `Courier.isWearableApiAvailable` method, to determine wearable API availability.
- Added `Courier.attachMockDataApi`, `Courier.attachMockMessageApi`, and `Courier.attachMockNodeApi` methods to support mocking communication for unit testing.
- `Bitmap` fields are now allowed in `@Deliverable` annotated classes. They are automatically sent and received as an `Asset`.
- Now uses a singleton `GoogleApiClient` for efficiency
- Fixed support for using Courier in nested/inner classes

### 1.0.3 - 2015-03-28
- Now compatible with JDK 7+ and JRE 6+

### 1.0.2 - 2015-03-23
- Now compatible with the `android-apt` plugin

### 1.0.1
- Version skipped due to release build error

### 1.0.0 - 2015-03-12
- Added `Courier.getAssetInputStream` convenience method
- Classes annotated with `@Deliverable` can now contain fields of other classes also annotated with `@Deliverable`

### 0.5.1 - 2015-03-11
- Retention of annotations set to `Class` so that ProGuard can see them
- Inheritance support bug

### 0.5.0 - 2015-03-10
- Added `@BackgroundThread` method annotation, to have method callbacks occur on a background thread.
- Added `@Deliverable` annotation, to create a serializable class
- A target's parent class' annotations are no longer ignored

### 0.4.1 - 2015-03-09
- Added `Courier.deleteData` method to delete items from the `DataApi`
- Added `Courier.getLocalNode` method to retrieve the `Node` representation of the current device
- `ReceiveMessages` and `ReceiveData` annotated methods may now contain a second parameter: A `String` representing the ID of the originating node.

### 0.3.0 - 2015-03-06
- Messages can now be send to a specific node

### 0.2.1 - 2015-03-05
- Only having `@RemoteNodes` annotations now triggers the annotation processor
- Fixed crash when using `@LocalNode` without `ReceiveData`

### 0.2.0 - 2015-03-05
- Added `@LocalNode` annotation to receive the local `Node`
- Added `@RemoteNodes` annotation to receive a list of connected `Node`s

### 0.1.0 - 2015-03-05
- Initial release