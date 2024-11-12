package com.pocket.sdk.util.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pocket.sdk.Pocket;
import com.pocket.sdk.api.generated.PocketRemoteStyle;
import com.pocket.sync.source.PendingResult;
import com.pocket.sync.source.Source;
import com.pocket.sync.source.result.SyncException;
import com.pocket.sync.source.subscribe.Subscription;
import com.pocket.sync.thing.Thing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * A {@link DataSourceCache} backed by syncing and subscribing a {@link Thing} with {@link Source}.
 *
 * <h3>Usage</h3>
 * To use, you need to help describe what thing to sync, how to get a list of objects from that thing, how to handle paging and making subsets, etc.
 * To create an instance, use the builder pattern starting with {@link #from(Pocket)} to help guide you through these pieces.
 *
 * <h3>Implementation</h3>
 * This takes a Thing and breaks it into {@link Page}s using {@link SubsetApply#subset(Thing, Subset)}.
 * To load each page it will use Pocket.sync() and then subscribe to it as well to keep it up to date.
 * As pages are loaded/updated, it creates the full list by looping through each page, invoking {@link CollectionGet#collectionFrom(Thing)} and appending the results into one list.
 *
 * <h2>Dependencies</h2>
 * This supports adding additional things that you need loaded into memory before you can display the list. See {@link BuilderStep4#dependsOn}.
 *
 * <h2>Diffing</h2>
 * Automatically sets up a {@link FastOrDieDiffer} that is suitable for working with {@link Thing}s. If you want more refined diffing you can setup your own with {@link #setDiffer(FastOrDieDiffer)}.
 *
 * <h2>Thread Safety</h2>
 * This assumes (but doesn't enforce to avoid performance penalties of checking) all calls to it are on the ui thread.
 * It also assumes that the {@link Pocket.Config#publisher} publishes {@link com.pocket.sync.source.subscribe.Subscriber#onUpdate(Thing)} callbacks to the ui thread.
 * If that is not the case, there will be issues.
 */
public class SyncCache<C, T extends Thing> extends AbsDataSourceCache<C> {
	
	public static final int DEFAULT_PAGE_SIZE = 30;
	
	private final T identity;
	private final PagingStrategy<C> paging;
	private final SubsetApply<T> subsetApply;
	private final CollectionGet<C, T> collection;
	private final Pocket source;
	private final int pageSize;
	private final List<Merge<C, T>> merges;
	private final List<Page<C,T>> pages = new ArrayList<>();
	private final Map<Thing, Subscription> subs = new HashMap<>();
	private final Map<Thing, Object> dependsOn = new HashMap<>();

	public boolean forceRemote = false;
	
	public static class Config<C, T extends Thing>  {
		private final T identity;
		private final PagingStrategy<C> paging;
		private final int pageSize;
		private final SubsetApply<T> subsetApply;
		private final CollectionGet<C, T> collection;
		private final Pocket source;
		private final List<Thing> dependsOn = new ArrayList<>();
		private final List<Merge<C ,T>> merges = new ArrayList<>();
		public boolean diffUtilEnabled = true;
		
		public Config(
				T identity,
				PagingStrategy<C> paging,
				SubsetApply<T> subsetApply,
				CollectionGet<C, T> collection,
				Pocket source
		) {
			this.pageSize = DEFAULT_PAGE_SIZE;
			this.identity = identity;
			this.paging = paging;
			this.subsetApply = subsetApply;
			this.collection = collection;
			this.source = source;
		}
	}

	private PendingResult<T, SyncException> pendingRefresh;
	
	public SyncCache(Config<C, T> config) {
		super();
		this.pageSize = config.pageSize;
		this.identity = config.identity;
		this.paging = config.paging;
		this.subsetApply = config.subsetApply;
		this.collection = config.collection;
		this.source = config.source;
		this.merges = config.merges;
		for (Thing t : config.dependsOn) {
			this.dependsOn.put(t, null);
		}
		if (config.diffUtilEnabled) {
			setDiffer(new FastOrDieDiffer<>(
					(FastOrDieDiffer.IdDiff<C>) Object::equals,
					// Since Thing's use IDENTITY comparisons this should be good for checking ids
					(oldItem, newItem) -> {
						if (oldItem instanceof Thing) {
							return ((Thing) oldItem).equals(Thing.Equality.STATE, newItem);
						} else { // We aren't forcing a sync cache to use Things as the list data type, so fallback to this logic if not
							return oldItem.equals(newItem);
						}
					},
					true
			));
		}
	}
	
	/** Retrieve the current value of a dependency. See {@link BuilderStep4#dependsOn}. */
	public <D extends Thing> D dependency(D thing) {
		Object s = dependsOn.get(thing);
		if (s instanceof Thing && thing.getClass().isAssignableFrom(s.getClass())) return (D) s;
		return null;
	}

	/** Create an empty page instance of what the next page should be (identity wise) based on the current config. */
	private Page<C, T> nextPage() {
		if (paging != null) {
			Subset subset = new Subset(paging.getNextPageOffset(getList()), pageSize);
			T pageIdentity = subsetApply.subset(identity, subset);
			return new Page<>(subset, pageIdentity);
		} else {
			return new Page<>(null, identity);
		}
	}
	
	@Override
	protected void doLoadFirstPage() {
		clearData(); // This can also be called when retrying an empty list, so make sure we clear out all data and subscriptions and retry clean
		/*
			This status map will contain the status of each thing we must load before we can display a result.
			Its key will be the thing we are loading, its value will be its status, one of:
				null means loading
				Thing means loaded
				SyncException means error
		 */
		HashMap<Thing, Object> status = new HashMap<>();
		for (Thing t : dependsOn.keySet()) {
			status.put(t, null);
		}
		Page<C, T> first = nextPage();
		status.put(first.identity, null);
		
		// This will run after each thing is loaded to check if we are ready to display a result.
		// We'll wait until all have some result before we do anything.
		Runnable check = () -> {
			Throwable error = null;
			for (Map.Entry<Thing, Object> result : status.entrySet()) {
				Object r = result.getValue();
				if (r == null) {
					// Something is still loading, wait for next check
					return;
				} else if (r instanceof Throwable) {
					if (error instanceof SyncException) {
						// An error occurred, prioritize grabbing the page's error if there is more than one.
						SyncException syncException = (SyncException) error;
						if (!syncException.result.t.equals(first.identity)) {
							error = (Throwable) r;
						}
					} else if (error == null) {
						error = (Throwable) r;
					}
				} else if (r instanceof Thing) {
					// Loaded
				} else {
					throw new RuntimeException("unexpected result " + r);
				}
			}
			// Ok we have a result, otherwise it would have stopped above
			if (error != null) {
				clearData();
				Throwable e = error;
				setError(new Error() {
							 @Override public void retry() { loadFirstPage(); }
							 @Override public Throwable getError() { return e;}
						 }, LoadState.INITIAL_ERROR);
			} else {
				// Show the list
				pages.add(first);
				invalidateList();
			}
		};
		
		// Bind this callback to the first page plus and dependencies, this does the initial load plus any future updates
		for (Thing t : new HashSet<>(status.keySet())) {
			// Also stop any subscription this replaces
			Subscription.stop(subs.put(t, source.bind(forceRemote, t, u -> {
				Throwable error = null;
				if (u.equals(first.identity)) {
					try {
						updatePage(first, (T) u);
					} catch (Throwable e) {
						error = e;
					}
				} else {
					dependsOn.put(u, u);
				}
				if (pages.isEmpty()) {
					// First time
					status.put(t, error != null ? error : u);
					check.run();
				} else {
					// Update
					invalidateList();
				}
				
			}, (e, sub) -> {
				status.put(t, e);
				check.run();
			})));
		}
	}
	
	@Override
	protected void doLoadNextPage() {
		Page<C, T> next = nextPage();
		Subscription.stop(subs.put(next.identity, source.bind(forceRemote, next.identity, u -> { // The Subscription.stop() ensures if this ends up replacing an existing subscription it is stopped properly.
			if (!pages.contains(next)) pages.add(next);
			try {
				updatePage(next, u);
			} catch (Throwable t) {
				setError(
						new Error() {
							@Override public void retry() { loadNextPage(); }
							@Override public Throwable getError() { return t; }
						},
						LoadState.LOADED_APPEND_ERROR
				);
			}
			invalidateList();
		},
		(e, sub) -> {
			sub.stop();
			subs.remove(next.identity);
			setError(new Error() {
				@Override public void retry() { loadNextPage(); }
				@Override public Throwable getError() { return e;}
			}, LoadState.LOADED_APPEND_ERROR);
		})));
	}
	
	private void updatePage(Page<C, T> page, T updated) {
		if (paging != null) paging.onDataReturned(page.subset);
		page.data.clear();
		
		List<C> data = collection.collectionFrom(updated);
		if (data != null) page.data.addAll(data);
	}

	/**
	 * Invalidates the current list and notifies any listeners of changes in Item state.
	 */
	private void invalidateList() {
		// Merge all pages together into a single list
		List<C> list = new ArrayList<>();
		for (Page<C, T> page : pages) {
			list.addAll(page.data);
		}
		
		// Check if paging is completed
		boolean isPagingComplete;
		Page<C,T> last = pages.isEmpty() ? null : pages.get(pages.size()-1);
		if (last == null) {
			isPagingComplete = false;
		} else if (paging == null) {
			isPagingComplete = true;
		} else {
			isPagingComplete = paging.isPagingComplete(last.subset, last.data);
		}
		
		for (Merge<C ,T> merge : merges) {
			merge.merge(this, list, isPagingComplete);
		}
		
		// Apply this new list
		setList(list, isPagingComplete);
	}
	
	@Override
	protected void doRefresh() {
		/*
			Refreshes keep the existing data and only replace it if it is successful.
			Since we are already bound to the first page, all we do is syncRemote it, if there are changes, we'll pick them up automatically.
			If successful, we'll trim off the pages other than the first.
			To keep things simple for now, dependencies are a best attempt refresh and don't have an impact on error or success here.
		 */
		Page<C,T> firstPage = pages.get(0);
		if (firstPage.identity.remote().style == PocketRemoteStyle.LOCAL) {
			pendingRefresh = source.sync(firstPage.identity);
		} else {
			pendingRefresh = source.syncRemote(firstPage.identity);
		}
		pendingRefresh
			.onSuccess(p -> {
				// Our subscription should pick it up any changes
				// Clear all but the first page so they are re-paged as needed
				pages.clear();
				pages.add(firstPage);
				invalidateList();
			})
			.onFailure(e -> setError(new Error() {
				@Override public void retry() { refresh(); }
				@Override public Throwable getError() { return e;}
			}, LoadState.LOADED_REFRESH_ERROR))
			.onComplete(() -> pendingRefresh = null);

		
		for (Thing t : dependsOn.keySet()) {
			if (t.remote().style != PocketRemoteStyle.LOCAL) {
				source.syncRemote(t);
			}
			// If it is local, we should get updates automatically already, so no need to refresh
		}
	}
	
	private void clearData() {
		for (Subscription sub : subs.values()) {
			sub.stop();
		}
		subs.clear();
		pages.clear();
		if (pendingRefresh != null) {
			pendingRefresh.abandon();
			pendingRefresh = null;
		}
		if (paging != null) paging.onReset();
		for (Thing t : new HashSet<>(dependsOn.keySet())) {
			dependsOn.put(t, null);
		}
	}
	
	@Override
	public void resetData() {
		super.resetData();
		clearData();
	}

	public Thing identity() {
		return identity;
	}
	
	/** Describes how to apply a subset to a thing. */
	public interface SubsetApply<T extends Thing> {
		/** @return A new instance of T with the subset applied to it */
		T subset(T identity, Subset subset);
	}
	
	public static class Subset {
		public final int offset;
		public final int count;
		public Subset(int offset, int count) {
			this.offset = offset;
			this.count = count;
		}
	}
	
	private static class Page<C, T extends Thing> {
		public final Subset subset;
		public final T identity;
		public final List<C> data = new ArrayList<>();
		
		private Page(Subset subset, T identity) {
			this.subset = subset;
			this.identity = identity;
		}
		
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Page<?, ?> page = (Page<?, ?>) o;
			return identity.equals(page.identity);
		}
		
		@Override
		public int hashCode() {
			return identity.hashCode();
		}
	}
	
	/** Describes how to convert a thing to a list of objects. */
	public interface CollectionGet<C, T extends Thing> {
		/** @return the list of objects this thing represents. */
		List<C> collectionFrom(T thing);
	}
	
	/**
	 * Calculates the offset to use when requesting the next page of data from the endpoint.
	 * @see PagingStrategies for pre-made common ones.
	 */
	public interface PagingStrategy<C> {
		int getNextPageOffset(List<C> data);
		
		/**
		 * Results have been returned from an endpoint.
		 */
		void onDataReturned(Subset subset);
		
		/**
		 * The {@link DataSourceCache} was reset.
		 */
		void onReset();
		
		boolean isPagingComplete(Subset subset, List<C> lastPage);
	}
	
	public static class PagingStrategies {
		/**
		 * Returns the next index/position that we don't have, based on number
		 * of items in the data.
		 */
		public static class NextPosition<C> implements PagingStrategy<C> {
			@Override
			public int getNextPageOffset(List<C> data) {
				return data.size();
			}
			
			@Override
			public void onDataReturned(Subset subset) {}
			
			@Override
			public void onReset() {}
			
			@Override
			public boolean isPagingComplete(Subset subset, List<C> lastPage) {
				return lastPage.size() < subset.count;
			}
		}
		
		/**
		 * Remembers the last requested offset, and returns the next one,
		 * regardless of how many items are in the data set currently.
		 */
		public static class NextPage<C> implements PagingStrategy<C> {
			
			private int next = 0;
			
			@Override
			public int getNextPageOffset(List<C> data) {
				return next;
			}
			
			@Override
			public void onDataReturned(Subset subset) {
				next = subset.offset + subset.count;
			}
			
			@Override
			public void onReset() {
				next = 0;
			}
			
			@Override
			public boolean isPagingComplete(Subset subset, List<C> lastPage) {
				return lastPage.isEmpty();
			}
		}
	}
	
	/** Describes how to merge extra dependencies into the main list of objects. */
	public interface Merge<C, T extends Thing> {
		/**
		 * Modifies the list by merging in some extra objects.
		 *
		 * @param cache available to grab a {@link SyncCache#dependency(Thing)}
		 * @param list list to merge an extra object or objects into
		 * @param isPagingComplete provided to help deciding if desired merging position might
		 * become available later
		 */
		void merge(SyncCache<C, T> cache, List<C> list, boolean isPagingComplete);
		
		/**
		 * Returns a list of things this merge depends on, so it can be encapsulated in a single
		 * object and a single call to {@link SyncCache.BuilderStep4#merge(Merge)}.
		 */
		@Nullable List<Thing> dependencies();
		
		/**
		 * Start configuring merging a single element into the list by specifying how to get or 
		 * create the element.
		 */
		static <C, E extends C, T extends Thing> SingleElementMerge.Builder<C, E, T> singleElement(
				SingleElementMerge.Element<C, E, T> element) {
			return new SingleElementMerge.Builder<>(element);
		}
	}
	
	/**
	 * Standard merging logic when adding only a single element to the list.
	 * 
	 * @param <C> Collection element type of the sync cache
	 * @param <E> Element type this merges in
	 * @param <T> Thing that backs the primary list content of the sync cache
	 */
	public static class SingleElementMerge<C, E extends C, T extends Thing> implements Merge<C, T> {
		private final Element<C, E, T> element;
		private final Placement<C> placement;
		private final FailedListener<E> listener;
		private final List<Thing> dependencies;
		private E current;
		
		SingleElementMerge(@NonNull Element<C, E, T> element,
				@NonNull Placement<C> placement,
				@Nullable FailedListener<E> listener,
				@Nullable List<Thing> dependencies) {
			this.element = element;
			this.placement = placement;
			this.listener = listener;
			this.dependencies = dependencies;
		}
		
		@Override
		public void merge(SyncCache<C, T> cache, List<C> list, boolean isPagingComplete) {
			E element = this.element.get(cache, list);
			
			// Remove if needed
			if (element == null || !element.equals(current)) {
				// If there is no more element, or if it is a different one, clear the old one out.
				current = null;
				placement.setAfter(null);
			}
			
			// Insert if needed
			if (element != null) {
				if (current != null) {
					// Same one, but needs to be reinserted into the data set at the natural order it 
					// would be as the item data below changes.
					int position = list.size();
					for (int i = 0, listSize = list.size(); i < listSize; i++) {
						C obj = list.get(i);
						if (placement.wasAfter(obj, i)) continue;
						
						position = i;
						break;
					}
					
					if (position > list.size()) {
						// It no longer fits, add to end.
						position = list.size();
					}
					current = element;
					list.add(position, element);
					
				} else {
					// New element, use its starting position
					int position = placement.startingPosition();
					if (position >= list.size()) {
						// Position isn't available yet
						if (isPagingComplete) {
							// Fail
							if (listener != null) listener.onMergeFailed(element);
						} else {
							// Wait until paging is complete to see if it will fit later
						}
					} else {
						placement.setAfter(list.get(position));
						current = element;
						list.add(position, element);
					}
				}
			}
		}
		
		@Override public List<Thing> dependencies() {
			return dependencies;
		}
		
		/** Describes how to get or create the element to merge into the list. */
		public interface Element<C, E extends C, T extends Thing> {
			E get(SyncCache<C, T> cache, List<C> list);
		}
		
		/** Describes where to place an element with relation to other objects in the list */
		public interface Placement<C> {
			/**
			 * What position to start from when inserting for the first time.
			 */
			int startingPosition();
			
			/**
			 * When previously merged into the list, was the SPOC after the given obj or position.
			 * Use obj or position according to your logic.
			 */
			boolean wasAfter(C obj, int position);
			
			/**
			 * Remember position of a new SPOC.
			 */
			void setAfter(@Nullable C obj);
		}
		
		/**
		 * Notifies if a merge failed, i.e. when it wasn't possible to merge the element at the 
		 * specified {@link Placement}.
		 */
		public interface FailedListener<O> {
			void onMergeFailed(O obj);
		}
		
		public static class Builder<C, E extends C, T extends Thing> {
			private final Element<C, E, T> element;
			private Placement<C> placement;
			private FailedListener<E> listener;
			private List<Thing> dependencies;
			
			/**
			 * Start building by specifying how to get or create the element to merge into the list.
			 */
			public Builder(Element<C, E, T> element) {
				this.element = element;
			}
			
			/** Set a custom placement implementation. */
			public Builder<C, E, T> placement(Placement<C> placement) {
				this.placement = placement;
				return this;
			}
			
			/**
			 * Place the element at the starting position and try to keep it place relative to 
			 * other objects in the list using the comparator.
			 */
			public Builder<C, E, T> relativePlacement(int startingPosition, Comparator<C> comparator) {
				placement = new Placement<C>() {
					C current;
					
					@Override public int startingPosition() {
						return startingPosition;
					}
					
					@Override public boolean wasAfter(C obj, int position) {
						return comparator.compare(current, obj) < 0;
					}
					
					@Override public void setAfter(@Nullable C obj) {
						current = obj;
					}
				};
				return this;
			}
			
			/** Place the element always at the specified position. */
			public Builder<C, E, T> staticPlacement(int position) {
				placement = new Placement<C>() {
					@Override public int startingPosition() {
						return position;
					}
					
					@Override public boolean wasAfter(Object obj, int position) {
						return position < startingPosition();
					}
					
					@Override public void setAfter(@Nullable Object obj) {
						// Nothing to do here, move along.
					}
				};
				return this;
			}
			
			/**
			 * Set a listener that is called if the merge fails, i.e. it's not possible to merge 
			 * the element at the requested placement.
			 */
			public Builder<C, E, T> onFailure(FailedListener<E> listener) {
				this.listener = listener;
				return this;
			}
			
			/** Add a thing this merge depends on. */
			public Builder<C, E, T> dependsOn(Thing thing) {
				if (dependencies == null) {
					dependencies = new LinkedList<>();
				}
				
				dependencies.add(thing);
				return this;
			}
			
			public SingleElementMerge<C, E, T> build() {
				if (placement == null) {
					staticPlacement(0);
				}
				return new SingleElementMerge<>(element, placement, listener, dependencies);
			}
		}
	}
	
	/**
	 * Create a new builder.
	 * @param source The source to sync and subscribe from.
	 * @return The next builder step.
	 */
	public static Builder from(Pocket source) {
		return new Builder(source);
	}
	public static class Builder {
		Pocket source;
		private Builder(Pocket source) {
			this.source = source;
		}
		/**
		 * The thing to sync and subscribe to.
		 * @param identity The thing, only the identity fields are needed.
		 * @return The next builder step.
		 */
		public <T extends Thing> BuilderStep2<T> sync(T identity) {
			return new BuilderStep2<>(identity, source);
		}
	}
	public static class BuilderStep2<T extends Thing> {
		private final T t;
		private final Pocket source;
		private BuilderStep2(T t, Pocket source) {
			this.t = t;
			this.source = source;
		}
		/**
		 * How to convert the thing to a list of data objects to display.
		 * @return The next builder step.
		 */
		public <C> BuilderStep3<C,T> display(CollectionGet<C, T> collection) {
			return new BuilderStep3<>(this, collection);
		}
	}
	public static class BuilderStep3<C, T extends Thing> {
		private final BuilderStep2<T> b;
		private final CollectionGet<C, T> collection;
		private BuilderStep3(BuilderStep2<T> b, CollectionGet<C, T> collection) {
			this.b = b;
			this.collection = collection;
		}
		/**
		 * Page by using a {@link PagingStrategies.NextPage} strategy and describe how to apply a subset (count and offset) to the thing.
		 * @return The next builder step.
		 */
		public BuilderStep4<C,T> pageByOffset(SubsetApply<T> subsetApply) {
			return new BuilderStep4<>(this, new PagingStrategies.NextPage<>(), subsetApply);
		}
		/**
		 * Page by using a {@link PagingStrategies.NextPosition} strategy and describe how to apply a subset (count and offset) to the thing.
		 * @return The next builder step.
		 */
		public BuilderStep4<C,T> pageByPosition(SubsetApply<T> subsetApply) {
			return new BuilderStep4<>(this, new PagingStrategies.NextPosition<>(), subsetApply);
		}
		/**
		 * Do not page, use the identity as is, without modifying or paging.
		 * @return The next builder step.
		 */
		public BuilderStep4<C,T> noPaging() {
			return new BuilderStep4<>(this, null, null);
		}
	}
	public static class BuilderStep4<C, T extends Thing> {
		private final Config<C, T> config;
		
		private BuilderStep4(BuilderStep3<C, T> b, PagingStrategy<C> paging, SubsetApply<T> subsetApply) {
			this.config = new Config<>(b.b.t, paging, subsetApply, b.collection, b.b.source);
		}
		
		/**
		 * Adds an additional Thing that must be loaded before {@link #setList(List, boolean)} is invoked.
		 * Can later be retrieved with {@link SyncCache#dependency(Thing)} anytime after {@link #setList(List, boolean)} has been called.
		 */
		public BuilderStep4<C, T> dependsOn(Thing t) {
			config.dependsOn.add(t);
			return this;
		}
		
		/** Optionally adds logic to merge some extra objects to the list of display objects. */
		public BuilderStep4<C, T> merge(Merge<C, T> merge) {
			List<Thing> dependencies = merge.dependencies();
			if (dependencies != null) {
				config.dependsOn.addAll(dependencies);
			}
			config.merges.add(merge);
			return this;
		}

		/**
		 * A DataSourceCache probably shouldn't be doing the diffs.  The diffs should happen
		 * in the RecyclerView.Adapter instead.  Most places in the app use the
		 * DataSourceCache's diff, so I'm adding this property to disable it.
		 */
		public BuilderStep4<C, T> disableDiffUtil() {
			config.diffUtilEnabled = false;
			return this;
		}
		
		/** @return A config with these settings. */
		public Config<C,T> config() {
			Config<C, T> tmp = new Config<>(config.identity, config.paging, config.subsetApply, config.collection, config.source);
			tmp.dependsOn.addAll(config.dependsOn);
			tmp.merges.addAll(config.merges);
			tmp.diffUtilEnabled = config.diffUtilEnabled;
			return tmp;
		}
		/** @return A cache with these settings. */
		public SyncCache<C,T> build() {
			return new SyncCache<>(config());
		}
	}
	
}
