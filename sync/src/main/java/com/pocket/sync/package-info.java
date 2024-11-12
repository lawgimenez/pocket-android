/**
 * A Java/Android implementation of "Sync".
 * See the Sync docs for more details.
 * Note: This package is for Sync's general implementation, not Pocket's implementation of Sync. For that see {@link com.pocket.sdk}.
 *
 * <h2>How to create a new sync engine</h2>
 * The sync engine works with things and actions abstractly and just needs a {@link com.pocket.sync.spec.Spec} with your API defined to get setup.
 * <p>
 * It is designed to be used with code generation. The sync-gen module (together with sync-parser) will
 * generate a {@link com.pocket.sync.spec.Spec} for you based on your schema.
 * <p>
 * To get started:
 * <ol>
 *     <li>Read sync.md to understand the ideas and organization underlying "Sync"</li>
 *     <li>Define your API in GraphQL files</li>
 *     <li>Run code generation (See the {@code com.pocket.sync.print.java} package in the sync-gen module) TODO document how to have it part of the build process.</li>
 *     <li>Create your spec class as a subclass as the _BaseSpec that sync-gen generated for you. (Where this is depends on your generation configuration)</li>
 *     <li>In your spec, implement any action or derive methods that could not be automatically generated.</li>
 *     <li>Choose an existing {@link com.pocket.sync.source.Source} implementation (or create a new one if needed) and pass in your {@link com.pocket.sync.spec.Spec}.</li>
 * </ol>
 * That completes the initial setup. Now you can use your {@link com.pocket.sync.source.Source} to power your app!
 * If you want a reference implementation, see Pocket's at {@link com.pocket.sdk}.
 *
 * <h2>Updating your API</h2>
 * Once you have this setup, here is how you update your API.
 * The goal here is that you can make changes to your API in a matter of seconds or a few short minutes.
 * <ol>
 *     <li>Add, remove or update definitions in your schema files</li>
 *     <li>Re-run code generation</li>
 *     <li>In your spec, implement any new action or derive methods that could not be automatically generated.</li>
 * </ol>
 *
 * <h2>Adding or changing sync engine features</h2>
 * It is important to consider what layer of abstraction your change belongs in.
 * See the sync docs (sync.md) section on making changes. The broad strokes are:
 * <ol>
 *     <li>Figure out where the change best fits. Can it be a thing or action, or is it really a sync engine change?</li>
 *     <li>Spec out the underlying concepts of your change in the sync docs (sync.md)</li>
 *     <li>If needed, make modifications to the sync-parser</li>
 *     <li>If needed, make modifications to the code generation</li>
 *     <li>Make modifications in this library</li>
 *     <li>Update implementations to handle this change</li>
 * </ol>
 *
 * TODO include example code in these docs.
 */
package com.pocket.sync;