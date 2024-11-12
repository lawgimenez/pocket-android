# Pocket App

This module is Pocket's flagship Android app. https://play.google.com/store/apps/details?id=com.ideashower.readitlater.pro

The main packages are:

* `com.pocket.app` Android components that power the app. Activities, Services, etc. If you are looking for a specific part of the app, jump in here.
* `com.pocket.sdk` Pocket tools and components that have shared use thorough-out the entire app.
* `com.pocket.util` Java and Android utilities, tools and helpers that aren't specific to Pocket.

Note there is also `com.pocket.sdk2` which may host some experiments or refactors during the sdk / syncing refactor project.

In addition to diving into the package structure, here are some additional jumping off points for core functionality:

#### Pocket API

A great starting point for getting an intro to the concepts in entire, cross-platform Pocket platform is [The Spec](https://github.com/Pocket/spec).

Working with Pocket's v3 API.

The main package is `com.pocket.sdk.api`. If you haven't yet, be sure to view [The Spec](https://github.com/Pocket/spec).

All communication with Pocket's api is through the sync engine. See [sync](/sync), [sync-pocket](/sync-pocket)
and all the related modules.


#### Offline Items / Image & File Caching

All resource downloading and caching is managed with the `com.pocket.sdk.offline` package. Managed by `Assets`, and `OfflineDownloading`.

Images can be obtained and resized with the `Image` class.

#### Async

See `AppThreads` component.

#### Account Management

The logged in user is managed through `Pocket.user()`.

#### Views

All collection views are powered by `DataSourceView`.
