# Analytics

This module contains and implements `Tracker` and `TrackerConfig` interfaces that app modules
can use to setup sending analytic events to Snowplow collector. It relies on Snowplow support in
`pocket-sync`.

UI library modules that shouldn't have access to full analytic APIs, but want to support more
automatic triggering and building of analytics events can depend on `analytics-api` and implement
tracking helper interfaces from there.
