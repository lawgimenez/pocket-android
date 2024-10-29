# Sync Engine Code Generation

Creates a library your [sync engine](../sync) can use to generate code from schema.

The main class is [Generator.java](src/main/java/com/pocket/sync/print/java/Generator.java).

## Usage

The general usage pattern is:

One time setup:
1. Create a config and a generator class. See [Pocket's Code Generator](src/main/java/com/pocket/sync/print/java/pocket/AndroidClassGenerator.java) for an example.
2. Create a gradle task that will produce a jar for your class and copy it into your sync engine module. See the `pocketGenJarPublish` task in [build.gradle](build.gradle) for an example.
3. Setup a gradle code generation task in your sync engine module, that will use this jar to run the generator. See [../sync-pocket/build.gradle] for an example.

Any time you modify this libraries generator code:
1. Run your gradle task to regenerate your jar and copy it over to your module.

Now anytime you update your schema or rebuild your sync engine, it will regenerate the classes for you.

## Making Changes

1. Make changes to the generation code as needed.
2. Re-run your publishing gradle task (described above in the usage section).

*Tip*: Use the `Republish sync-gen Libraries` runtime configuration that should automatically be included in your IDE to run step 2 easily and quickly.

## Debugging

Debug the `Republish sync-gen Libraries` runtime configuration.
