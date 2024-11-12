# Sync Engine's Parser Library

## Packages / Features

* [com.pocket.sync.parse](src/commonMain/kotlin/com/pocket/sync/parse) For reading/parsing/validating schema/spec files.
* [com.pocket.sync.type](src/commonMain/kotlin/com/pocket/sync/type) Modelling of syncs as classes so you can work with them.
* [com.pocket.sync.print.figment](src/commonMain/kotlin/com/pocket/sync/print/figment) Reprinting back out to figment.

Also see Pocket's [Android Repo](https://github.com/Pocket/Android) for sync engine code generation.

### Syntax Updates

Adding new figment syntax features is a fairly common task, here are some general tips for doing so:
* Add the new property to Data.kt
* Add the new property to Definition.kt
* Handle the property in Syntax.kt
* Add to the FigmentPrinter.kt
* Add to SyntaxTests and ReprintTest cases as needed

## Debugging

Create a runtime config in IntelliJ that you can run in debug mode.

There is a prebuilt template included in this project in [.idea/runConfigurations](.idea/runConfigurations) that should have already loaded into your IDE automatically. In the configs dropdown, look for "Debuggable - validate".
Make a copy of that template and modify the command line args as needed for what you want to debug. Then run it in debug mode.
