# Change Log
All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](http://semver.org/).

## 1.0.2 - 2015-03-23
### Fixed
- Now compatible with the `android-apt` plugin

## 1.0.1
- Version skipped due to release build error

## 1.0.0 - 2015-03-12
### Added
- `Courier.getAssetInputStream` convenience method

### Fixed
- Classes annotated with `@Deliverable` can now contain fields of other classes also annotated with `@Deliverable`

## 0.5.1 - 2015-03-11
### Fixed
- Retention of annotations set to `Class` so that ProGuard can see them
- Inheritance support bug

## 0.5.0 - 2015-03-10
### Added
- `@BackgroundThread` method annotation, to have method callbacks occur on a background thread.
- `@Deliverable` annotation, to create a serializable class

### Fixed
- A target's parent class' annotations are no longer ignored

## 0.4.1 - 2015-03-09
### Added
- `Courier.deleteData` method to delete items from the `DataApi`
- `Courier.getLocalNode` method to retrieve the `Node` representation of the current device

### Changed
- `ReceiveMessages` and `ReceiveData` annotated methods may now contain a second parameter: A `String` representing the ID of the originating node.

## 0.3.0 - 2015-03-06
### Added
- Messages can now be send to a specific node

## 0.2.1 - 2015-03-05
### Fixed
- Only having `@RemoteNodes` annotations now triggers the annotation processor
- Fixed crash when using `@LocalNode` without `ReceiveData`

## 0.2.0 - 2015-03-05
### Added
- `@LocalNode` annotation to receive the local `Node`
- `@RemoteNodes` annotation to receive a list of connected `Node`s

## 0.1.0 - 2015-03-05
### Added
- Initial release