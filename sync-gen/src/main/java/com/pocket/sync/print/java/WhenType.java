package com.pocket.sync.print.java;

import com.pocket.sync.type.AllowedInCollectionType;
import com.pocket.sync.type.CollectionType;
import com.pocket.sync.type.DefinitionType;
import com.pocket.sync.type.Enum;
import com.pocket.sync.type.Field;
import com.pocket.sync.type.FieldType;
import com.pocket.sync.type.ListType;
import com.pocket.sync.type.MapType;
import com.pocket.sync.type.OpenType;
import com.pocket.sync.type.ReferenceType;
import com.pocket.sync.type.StatefulDefinition;
import com.pocket.sync.type.Thing;
import com.pocket.sync.type.ThingInterface;
import com.pocket.sync.type.Value;
import com.pocket.sync.type.Variety;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.Pair;

/**
 * Whenever code needs to make a decision based on kind of {@link com.pocket.sync.type.FieldType} a value might be, use this class.
 * <p>
 * Why not use language constructs like if, switch, when?
 * <p>
 * If all decisions use this class, it makes it possible to find all cases where we are making type based decisions,
 * otherwise in order to find those you'd have to search for instanceof checks, or references to the {@link com.pocket.sync.type.FieldType} class,
 * other means that won't be as reliable.
 * <p>
 * Being able to find all of these cases is very important anytime we add new types or make significant changes
 * to type rules. This is especially important for code generation, where there might be really specific logic
 * needed based on type.
 * <p>
 * By only using this class when doing type specific logic switching, it makes it easier to find and standardize.
 * <p>
 * The other major benefit is that by using the builder pattern it helps guide you to make sure you are thinking about all the possible cases.
 *
 * <h2>Usage</h2>
 * Use {@link #is(com.pocket.sync.type.FieldType)} if you don't need a return value, {@link #is(com.pocket.sync.type.FieldType, Class)} if you do.
 *
 * Then invoke the various methods to specify what should be done if the type meets that criteria.
 *
 * For example:
 *
 * <pre>
 *     WhenType.is(type)
 *          .collection(c -> println("the type is a collection"))
 *          .value(c -> println("the type is a value"))
 *          .otherwise(c -> println("the type is not a value or collection"))
 * </pre>
 *
 * If you pass in a value, it will print out "type is a value".
 *
 * The ordering of the methods you in invoke on the build is the order in which they will be evaluated, just like an if statement.
 *
 */
public class WhenType {

    /** Begin, with no return value needed.}*/
    public static Handlers is(FieldType type) {
        return new Handlers(type);
    }
    
    /**
     * Begin, with a return value.
     * @param type The type to compare
     * @param handlersReturn The expected return value's class
     */
    public static <R> ReturnHandlers<R> is(FieldType type, Class<R> handlersReturn) {
        return new ReturnHandlers<R>(type, handlersReturn);
    }

    public interface Handler<T> {
        void handle(T type);
    }

    public interface Match {
        boolean match(FieldType type);
    }

    private static class HandlerMatch<H> {
        final Match match;
        final H handler;
        HandlerMatch(Match match, H handler) {
            this.match = match;
            this.handler = handler;
        }
    }

    public static class Handlers {
        private final FieldType type;
        private final List<HandlerMatch<Handler>> handlers = new ArrayList<>();

        protected Handlers(FieldType type) {
            this.type = type;
        }

        public Handlers value(Handler<Value> handler) {
            return putDef(Value.class, handler);
        }

        public Handlers enumm(Handler<Enum> handler) {
            return putDef(Enum.class, handler);
        }

        public Handlers thing(Handler<Thing> handler) {
            return putDef(Thing.class, handler);
        }

        public Handlers list(Handler<ListType> handler) {
            return put(ListType.class, handler);
        }

        public Handlers map(Handler<MapType> handler) {
            return put(MapType.class, handler);
        }
        
        public Handlers variety(Handler<Variety> handler) {
            return putDef(Variety.class, handler);
        }
        
        public Handlers interface_(Handler<ThingInterface> handler) {
            return putDef(ThingInterface.class, handler);
        }

        public Handlers collection(Handler<CollectionType> handler) {
            return put(CollectionType.class, handler);
        }
        
        public Handlers open(Handler<OpenType> handler) {
            return put(OpenType.class, handler);
        }


        /** See {@link WhenType#isIdentifiable(FieldType)} for matching rules */
        public Handlers identifiable(Handler<Thing> handler) {
            return put(WhenType::isIdentifiable, (Handler<ReferenceType<?>>) type -> handler.handle((Thing) type.getDefinition()));
        }

         /** See {@link WhenType#containsIdentifiable(FieldType)}  for matching rules. */
        public Handlers containsIdentifiable(Handler<FieldType> handler) {
            return put(WhenType::containsIdentifiable, handler);
        }

        /** See {@link WhenType#isPotentiallyIdentifiable(FieldType)} for matching rules */
        public Handlers potentiallyIdentifiable(Handler<FieldType> handler) {
            return put(WhenType::isPotentiallyIdentifiable, handler);
        }

        /** See {@link WhenType#containsPotentiallyIdentifiable(FieldType)}  for matching rules */
        public Handlers containsPotentiallyIdentifiable(Handler<FieldType> handler) {
            return put(WhenType::containsPotentiallyIdentifiable, handler);
        }

        public <C extends CollectionType, I extends AllowedInCollectionType> Handlers collectionOfType(Class<C> collection, Class<I> inner, Handler<Pair<C, I>> handler) {
            return put(type ->
                type instanceof CollectionType
                && collection.isAssignableFrom(type.getClass())
                && inner.isAssignableFrom(((CollectionType) type).getInner().getClass())
            , (Handler<?>) type -> handler.handle(new Pair<C, I>((C) type, (I) ((C)type).getInner())));
        }

        public <C extends CollectionType, D extends StatefulDefinition> Handlers collectionOfDefinition(Class<C> collection, Class<D> definition, Handler<Pair<C, D>> handler) {
            return put(type ->
                type instanceof CollectionType
                && collection.isAssignableFrom(type.getClass())
                && ((CollectionType) type).getInner() instanceof DefinitionType
                && definition.isAssignableFrom(  ((DefinitionType) ((CollectionType) type).getInner()) .getDefinition().getClass())
            , (Handler<?>) type -> {
                C c = (C) type;
                DefinitionType<D> t = (DefinitionType<D>) ((CollectionType) type).getInner();
                D d = t.getDefinition();
                handler.handle(new Pair<C, D>(c, d));
            });
        }

        private Handlers put(Match match, Handler handler) {
            handlers.add(new HandlerMatch<>(match, handler));
            return this;
        }

        private <T extends FieldType> Handlers put(Class<T> clazz, Handler<T> handler) {
            return put(type -> clazz.isAssignableFrom(type.getClass()), handler);
        }

        private <D extends StatefulDefinition> Handlers putDef(Class<D> clazz, Handler<D> handler) {
            return put(type -> type instanceof DefinitionType && clazz.isAssignableFrom(((DefinitionType) type).getDefinition().getClass()), (Handler<DefinitionType<D>>) type -> handler.handle(type.getDefinition()));
        }

        public void otherwiseIgnore() {
            otherwise(t -> {});
        }

        /**
         * Ensure that all types are handled, such that if a new type is added later, this will fail it it wasn't handled yet.
         */
        public void otherwiseFail() {
            otherwise(t -> {
                throw new RuntimeException("No type matched for " + type);
            });
        }

        public void otherwise(Handler<FieldType> otherwise) {
            for (HandlerMatch<Handler> h : handlers) {
                if (h.match.match(type)) {
                    h.handler.handle(type);
                    return;
                }
            }
            otherwise.handle(type);
        }

    }




    public interface ReturnHandler<T, R> {
        R handle(T type);
    }

    public static class ReturnHandlers<R> {
        private final FieldType type;
        private final List<HandlerMatch<ReturnHandler>> handlers = new ArrayList<>();

        protected ReturnHandlers(FieldType type, Class<R> handlersReturn) {
            this.type = type;
        }

        public ReturnHandlers<R> value(ReturnHandler<Value, R> handler) {
            return putDef(Value.class, handler);
        }

        public ReturnHandlers<R> enumm(ReturnHandler<Enum, R> handler) {
            return putDef(Enum.class, handler);
        }

        public ReturnHandlers<R> thing(ReturnHandler<Thing, R> handler) {
            return putDef(Thing.class, handler);
        }

        public ReturnHandlers<R> list(ReturnHandler<ListType, R> handler) {
            return put(ListType.class, handler);
        }

        public ReturnHandlers<R> map(ReturnHandler<MapType, R> handler) {
            return put(MapType.class, handler);
        }

        public ReturnHandlers<R> collection(ReturnHandler<CollectionType, R> handler) {
            return put(CollectionType.class, handler);
        }
    
        public ReturnHandlers<R> variety(ReturnHandler<Variety, R> handler) {
            return putDef(Variety.class, handler);
        }
    
        public ReturnHandlers<R> interface_(ReturnHandler<ThingInterface, R> handler) {
            return putDef(ThingInterface.class, handler);
        }


        /** See {@link WhenType#isIdentifiable(FieldType)} for matching rules */
        public ReturnHandlers<R> identifiable(ReturnHandler<Thing, R> handler) {
            return put(WhenType::isIdentifiable, (ReturnHandler<ReferenceType<?>, R>) type -> handler.handle((Thing) type.getDefinition()));
        }

         /** See {@link WhenType#containsIdentifiable(FieldType)}  for matching rules. */
        public ReturnHandlers<R> containsIdentifiable(ReturnHandler<FieldType, R> handler) {
            return put(WhenType::containsIdentifiable, handler);
        }

        /** See {@link WhenType#isPotentiallyIdentifiable(FieldType)} for matching rules */
        public ReturnHandlers<R> potentiallyIdentifiable(ReturnHandler<FieldType, R> handler) {
            return put(WhenType::isPotentiallyIdentifiable, handler);
        }

        /** See {@link WhenType#containsPotentiallyIdentifiable(FieldType)}  for matching rules */
        public ReturnHandlers<R> containsPotentiallyIdentifiable(ReturnHandler<FieldType, R> handler) {
            return put(WhenType::containsPotentiallyIdentifiable, handler);
        }


    
    
        public ReturnHandlers<R> open(ReturnHandler<OpenType, R> handler) {
            return put(OpenType.class, handler);
        }

        public <C extends CollectionType, I extends AllowedInCollectionType> ReturnHandlers<R> collectionOfType(Class<C> collection, Class<I> inner, ReturnHandler<Pair<C, I>, R> handler) {
            return put(type ->
                type instanceof CollectionType
                && collection.isAssignableFrom(type.getClass())
                && inner.isAssignableFrom(((CollectionType) type).getInner().getClass())
            , (ReturnHandler<?, R>) type -> handler.handle(new Pair<C, I>((C) type, (I) ((C)type).getInner())));
        }

        public <C extends CollectionType, D extends StatefulDefinition> ReturnHandlers<R> collectionOfDefinition(Class<C> collection, Class<D> definition, ReturnHandler<Pair<C, D>, R> handler) {
            return put(type ->
                type instanceof CollectionType
                && collection.isAssignableFrom(type.getClass())
                && ((CollectionType) type).getInner() instanceof DefinitionType
                && definition.isAssignableFrom(  ((DefinitionType) ((CollectionType) type).getInner()) .getDefinition().getClass())
            , (ReturnHandler<?, R>) type -> {
                C c = (C) type;
                DefinitionType<D> t = (DefinitionType<D>) ((CollectionType) type).getInner();
                D d = t.getDefinition();
                return handler.handle(new Pair<C, D>(c, d));
            });
        }

        private ReturnHandlers<R> put(Match match, ReturnHandler<?, R> handler) {
            handlers.add(new HandlerMatch<>(match, handler));
            return this;
        }

        private <T> ReturnHandlers<R> put(Class<T> clazz, ReturnHandler<T, R> handler) {
            return put(type -> clazz.isAssignableFrom(type.getClass()), handler);
        }

        private <D extends StatefulDefinition> ReturnHandlers<R> putDef(Class<D> clazz, ReturnHandler<D, R> handler) {
            return put(type -> type instanceof DefinitionType && clazz.isAssignableFrom(((DefinitionType) type).getDefinition().getClass()), (ReturnHandler<DefinitionType<D>, R>) type -> handler.handle(type.getDefinition()));
        }

        /**
         * Ensure that all types are handled, such that if a new type is added later, this will fail it it wasn't handled yet.
         */
        public R otherwiseFail() {
            return otherwise(t -> {
                throw new RuntimeException("No type matched for " + type);
            });
        }

        public R otherwise(ReturnHandler<FieldType, R> otherwise) {
            for (HandlerMatch<ReturnHandler> h : handlers) {
                if (h.match.match(type)) {
                    return (R) h.handler.handle(type);
                }
            }
            return otherwise.handle(type);
        }
    }
    
    /**
     * @return true only if the type is a {@link DefinitionType} of {@link Thing} and that thing's {@link Thing#isIdentifiable()} is true.
     * Any other types, including collections, open types (even interface things!), etc, even if they contain identifiable things will return false.
     * See {@link #containsIdentifiable(FieldType)}, {@link #isPotentiallyIdentifiable(FieldType)} and others for related cases.
     */
    private static boolean isIdentifiable(FieldType type) {
        return WhenType.is(type, boolean.class)
                .open(o -> false)
                .thing(Thing::isIdentifiable)
                .otherwise(t -> false);
    }

    /**
     * @return true if {@link #isIdentifiable(FieldType)} or if this is an open type where at least one of {@link OpenType#compatible()} is identifiable.
     * Be aware that some open types may allow both non-identifiable and identifiable things, so depending on your use case, you may also need to have your code check Thing.isIdentifiable() at runtime as well.
     */
    private static boolean isPotentiallyIdentifiable(FieldType type) {
        return WhenType.is(type, boolean.class)
                .open(o -> {
                    for (Thing t : o.compatible()) {
                        if (t.isIdentifiable()) return true;
                    }
                    return false;
                })
                .thing(Thing::isIdentifiable)
                .otherwise(t -> false);
    }

    /**
     * Returns true if this is a thing that contains, anywhere within its sub/nested fields, a field that matches {@link #isIdentifiable(FieldType)}, regardless of depth.
     * <b>If the type itself is an identifiable, it has no impact on result, only its subfields or inner type is taken into account here.</b>
     * Some examples:
     * <ul>
     *     <li>The type is a thing, and has a field typed as an thing that is identifiable.</li>
     *     <li>The type is a collection of identifiable things.</li>
     *     <li>The type is a thing, and has a field that is a collection of non identifiable things, which has a field that contains an identifiable thing. (just illustrating, that it searches all sub fields)</li>
     * </ul>
     * To be very clear, {@link OpenType#compatible()} is NOT factored in here and do not match. To better handle open types, look at {@link #containsPotentiallyIdentifiable(FieldType)} and related.
     */
    private static boolean containsIdentifiable(FieldType type) {
        return contains(type, WhenType::isIdentifiable, false, false, new HashSet<>());
    }

    /**
     * Same as {@link #containsIdentifiable(FieldType)} but uses {@link #isPotentiallyIdentifiable(FieldType)} instead of {@link #isIdentifiable(FieldType)}.
     */
    public static boolean containsPotentiallyIdentifiable(FieldType type) {
        HashSet<Thing> checked = new HashSet<>();
        return WhenType.is(type, boolean.class)
                .collectionOfType(CollectionType.class, OpenType.class, c -> {
                    for (Thing t : c.component2().compatible()) {
                        if (contains(t, WhenType::isPotentiallyIdentifiable, true, checked)) return true;
                    }
                    return false;
                })
                .open(o -> {
                    for (Thing t : o.compatible()) {
                        if (contains(t, WhenType::isPotentiallyIdentifiable, true, checked)) return true;
                    }
                    return false;
                })
                .otherwise(t -> contains(type, WhenType::isPotentiallyIdentifiable, false, true, new HashSet<>()));
    }

    public static boolean containsPotentiallyIdentifiable(Thing thing) {
        return contains(thing, WhenType::isPotentiallyIdentifiable, true, new HashSet<>());
    }

    /**
     * Returns true if any of this types inner contents matches the 'match'.
     * @param matchThis If true, it will also check the provided `type`. If false it will only check its contents. Passing false here lets you only check the contents and skip the provided type itself when matching.
     */
    private static boolean contains(FieldType type, Match match, boolean matchThis, boolean supportOpenTypes, Set<Thing> checked) {
        if (matchThis && match.match(type)) return true;
        return WhenType.is(type, boolean.class)
                .collection(collection -> contains(collection.getInner(), match, true, supportOpenTypes, checked))
                .open(o -> {
                    if (supportOpenTypes) {
                        for (Thing t : o.compatible()) {
                            if (contains(t, match, true, checked)) return true;
                        }
                    }
                    return false;
                })
                .thing(t -> contains(t, match, supportOpenTypes, checked))
                .otherwise(t -> false);
    }

    /** Returns true if any of this thing's fields (or fields within those fields and so on) contain a match. This thing itself is not considered when matching. */
    private static boolean contains(Thing thing, Match match, boolean supportOpenTypes, Set<Thing> checked) {
        if (!checked.add(thing)) return false; // already checked
        for (Field f : thing.getFields().getAll()) {
            if (contains(f.getType(), match, true, supportOpenTypes, checked)) return true;
        }
        return false;
    }

    /**
     * @return true if one of this thing's identity fields contains directly (or as a child at any depth), a thing that is {@link #isPotentiallyIdentifiable(FieldType)}
     */
    public static boolean potentiallyHasNestedIdentity(Thing thing) {
        if (!thing.isIdentifiable()) return false;
        for (Field field : thing.getFields().getAll()) {
            if (!field.getIdentifying()) continue;
            if (isPotentiallyIdentifiable(field.getType()) || containsPotentiallyIdentifiable(field.getType())) return true;
        }
        return false;
    }

}
