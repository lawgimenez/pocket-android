# Analytics API

A small set interfaces that the `analytics` module uses to observe the views to detect when to
fire events and to query for data needed to build these events.

Mostly `analytics` just tries to use available `android.view.View` APIs where possible.
But sometimes it needs this extra support when the existing APIs can't give it what it needs.

This is meant as a small dependency for UI modules, that don't need (and shouldn't have) access
to the full API of the `analytics` module, but want to add some support, so that tracking works
more out of the box without extensive configuration.
