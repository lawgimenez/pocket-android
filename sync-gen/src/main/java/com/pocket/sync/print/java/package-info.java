/**
 * Generate Things, Actions, and other code needed for a sync engine implementation (the com.pocket.sync library).
 * See the sync and figment docs for more details (figment/spec/sync.md).
 * <p>
 * Assuming you already have your schema files, then create a {@link com.pocket.sync.print.java.Config} to give the generator
 * the info it needs about your implementation and then create a {@link com.pocket.sync.print.java.Generator} and run it.
 * <p>
 * The following will be generated for you:
 * <ul>
 *     <li>A class for each {@link com.pocket.sync.type.Thing}. See {@link com.pocket.sync.print.java.ThingGenerator}</li>
 *     <li>A class for each {@link com.pocket.sync.type.Action}. See {@link com.pocket.sync.print.java.ActionGenerator}</li>
 *     <li>A class for each {@link com.pocket.sync.type.Enum}. See {@link com.pocket.sync.print.java.EnumGenerator}</li>
 *     <li>A Modeller class to help work with {@link com.pocket.sync.type.Value}s. See {@link com.pocket.sync.print.java.ModellerGenerator}</li>
 *     <li>A Spec.Actions implementation that will have a method and builder for all actions. See {@link com.pocket.sync.print.java.ActionsSpecGenerator}</li>
 *     <li>A Spec.Things implementation that will have a method and builder for all things. See {@link com.pocket.sync.print.java.ThingsSpecGenerator}</li>
 *     <li>A helper class your Spec can use to apply actions, called an Applier. See {@link com.pocket.sync.print.java.ActionApplierGenerator}</li>
 *     <li>A helper class your Spec can use to derive things, called a Derives. See {@link com.pocket.sync.print.java.DerivedSpecGenerator}</li>
 * </ul>
 *
 * All will be output to the directory and package specified by your {@link com.pocket.sync.print.java.Config}.
 *
 * @see com.pocket.sync.print.java.pocket.AndroidClassGenerator for Pocket's generator
 */
package com.pocket.sync.print.java;