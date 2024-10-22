package com.pocket.sdk.api.spec;

import com.pocket.sdk.api.generated.PocketDerives;
import com.pocket.sdk.api.generated.enums.AdzerkPlacementName;
import com.pocket.sdk.api.generated.enums.ItemContentType;
import com.pocket.sdk.api.generated.enums.ItemSortKey;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.ItemStatusKey;
import com.pocket.sdk.api.generated.enums.PostService;
import com.pocket.sdk.api.generated.enums.PremiumFeature;
import com.pocket.sdk.api.generated.enums.SharedItemStatus;
import com.pocket.sdk.api.generated.enums.Videoness;
import com.pocket.sdk.api.generated.thing.AcEmail;
import com.pocket.sdk.api.generated.thing.AdzerkDecision;
import com.pocket.sdk.api.generated.thing.AdzerkSpoc;
import com.pocket.sdk.api.generated.thing.AdzerkSpocs;
import com.pocket.sdk.api.generated.thing.AutoCompleteEmails;
import com.pocket.sdk.api.generated.thing.ConnectedAccounts;
import com.pocket.sdk.api.generated.thing.Feed;
import com.pocket.sdk.api.generated.thing.FeedItem;
import com.pocket.sdk.api.generated.thing.Fetch;
import com.pocket.sdk.api.generated.thing.Friend;
import com.pocket.sdk.api.generated.thing.Friends;
import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sdk.api.generated.thing.GetAdzerkDecisions;
import com.pocket.sdk.api.generated.thing.GetLikes;
import com.pocket.sdk.api.generated.thing.GetProfileFeed;
import com.pocket.sdk.api.generated.thing.GetReposts;
import com.pocket.sdk.api.generated.thing.GetUnleashAssignments;
import com.pocket.sdk.api.generated.thing.Group;
import com.pocket.sdk.api.generated.thing.Groups;
import com.pocket.sdk.api.generated.thing.Guid;
import com.pocket.sdk.api.generated.thing.HiddenSpoc;
import com.pocket.sdk.api.generated.thing.HiddenSpocs;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.ListCounts;
import com.pocket.sdk.api.generated.thing.ListenSettings;
import com.pocket.sdk.api.generated.thing.LocalItems;
import com.pocket.sdk.api.generated.thing.LoginInfo;
import com.pocket.sdk.api.generated.thing.Position;
import com.pocket.sdk.api.generated.thing.Post;
import com.pocket.sdk.api.generated.thing.Profile;
import com.pocket.sdk.api.generated.thing.RecentFriends;
import com.pocket.sdk.api.generated.thing.RecentSearches;
import com.pocket.sdk.api.generated.thing.Saves;
import com.pocket.sdk.api.generated.thing.SearchMatch;
import com.pocket.sdk.api.generated.thing.SearchQuery;
import com.pocket.sdk.api.generated.thing.SharedItem;
import com.pocket.sdk.api.generated.thing.SyncState;
import com.pocket.sdk.api.generated.thing.Tag;
import com.pocket.sdk.api.generated.thing.Tags;
import com.pocket.sdk.api.generated.thing.Unleash;
import com.pocket.sdk.api.generated.thing.UnleashAssignment;
import com.pocket.sdk.api.generated.thing.UserFollow;
import com.pocket.sdk.api.thing.ItemUtil;
import com.pocket.sdk.api.thing.ItemUtil2Kt;
import com.pocket.sdk.api.thing.TagUtil;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sync.source.subscribe.Changes;
import com.pocket.sync.space.Change;
import com.pocket.sync.space.Diff;
import com.pocket.sync.space.SelectorHelper;
import com.pocket.sync.space.Space;
import com.pocket.sync.space.mutable.MutableSpace;
import com.pocket.sync.thing.Thing;
import com.pocket.util.java.DomainUtils;
import com.pocket.util.java.Safe;
import com.pocket.util.java.function.Function;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static kotlin.collections.CollectionsKt.emptyList;
import static kotlin.collections.CollectionsKt.firstOrNull;
import static kotlin.collections.CollectionsKt.map;
import static kotlin.collections.CollectionsKt.removeAll;
import static kotlin.collections.CollectionsKt.subtract;
import static kotlin.jvm.internal.Intrinsics.areEqual;

/**
 * Deriving a field is mostly opt-in, otherwise it relies on syncing with v3 to fill in those values.
 * To setup support for deriving a field or thing locally, override its specific method in this class.
 */
public class Deriver extends PocketDerives {
	
	Deriver() {
		super( // These are the only things we'll try to fully derive locally, the rest we'll only try to derive/rederive specific fields
				Saves.THING_TYPE,
				LocalItems.THING_TYPE,
				ListCounts.THING_TYPE);
	}
	
	@Override
	public PocketSpec spec() {
		return (PocketSpec) super.spec();
	}
	
	/**
	 * @return a copy of the list (or null if already null) that has filtered out:
	 * <ul>
	 * <li> any where {FeedItem.reported} = true, {FeedItem.post.deleted} = true or {FeedItem.post.original_post.deleted} = true </li>
	 * <li> If {Account.premium_features} contains {PremiumFeature.ad_free}, any where {FeedItem.impression_info} is non null (meaning it is a spoc) </li>
	 * </ul>
	 */
	private List<FeedItem> cleanFeed(List<FeedItem> feed, Space.Selector selector) {
		if (feed != null) {
			List<FeedItem> clean = new ArrayList<>(feed);
			Iterator<FeedItem> it = clean.iterator();
			while (it.hasNext()) {
				FeedItem feedItem = it.next();
				if (isReported(feedItem)) {
					it.remove();
				} else if (isDeleted(feedItem)) {
					it.remove();
				}
			}
			return clean;
		} else {
			return null;
		}
	}
	
	/**
	 * Like {@link #cleanFeed(List, Space.Selector)}, but also removes posts from profiles user 
	 * stopped following.
	 */
	private List<FeedItem> cleanRecommendations(List<FeedItem> feed, Space.Selector selector) {
		if (feed != null) {
			List<FeedItem> clean = cleanFeed(feed, selector);
			Iterator<FeedItem> it = clean.iterator();
			while (it.hasNext()) {
				FeedItem feedItem = it.next();
				if (notFollowingAuthor(feedItem)) {
					it.remove();
				}
			}
			return clean;
		} else {
			return null;
		}
	}
	
	private boolean isReported(FeedItem feedItem) {
		return feedItem.reported != null && feedItem.reported;
	}
	
	private boolean isDeleted(FeedItem feedItem) {
		return (Safe.getBoolean(() -> feedItem.post.deleted))
				|| (Safe.getBoolean(() -> feedItem.post.original_post.deleted));
	}
	
	private boolean notFollowingAuthor(FeedItem feedItem) {
		return feedItem.post != null && !feedItem.post.profile.is_following;
	}
	
	/** @return a copy of the list (or null if already null) that has filtered out any deleted posts. */
	private List<Post> cleanPosts(List<Post> posts) {
		if (posts != null) {
			List<Post> clean = new ArrayList<>(posts);
			Iterator<Post> it = clean.iterator();
			while (it.hasNext()) {
				Post post = it.next();
				if (post.deleted != null && post.deleted) {
					it.remove();
					// TODO what about post.original_post.deleted?
				}
			}
			return clean;
		} else {
			return null;
		}
	}
	
	@Override
	public Group derive__Item__badge(Item t, Diff diff, Space.Selector selector) {
		if (selector == null || t.badge_group_id == null) return t.badge; // Nothing to look up or we can't
		if (t.badge != null && t.badge.group_id.equals(t.badge_group_id)) return t.badge; // Already found
		Group found = selector.get(new Group.Builder().group_id(t.badge_group_id).build());
		return found != null ? found : t.badge; // If not found, keep existing value
	}

	/** {@inheritDoc} */
	@Override public String derive__Item__domain(Item t, Diff diff, Space.Selector selector) {
		if (t.display_url != null) {
			return DomainUtils.cleanHostFromUrl(t.display_url.url);
		}

		return super.derive__Item__domain(t, diff, selector);
	}

	@Override
	public List<Post> derive__Item__posts(Item t, Diff diff, Space.Selector selector) {
		return cleanPosts(t.posts);
	}

	@Override
	public Integer derive__Profile__follow_count(Profile t, Diff diff, Space.Selector selector) {
		// Note: This assumes that UserFollow's will only be imprinted when their status is changing. If a UserFollow is imprinted for some other reason it could be seen as a following change by this logic. If we find that use case comes up this will need to be reconsidered and perhaps handled in the follow_user action instead.
		int followCountChange =
				// The number of new follows
				diff.find(Changes.of(UserFollow.class, i -> i.user_id.equals(t.uid)).when((before, after) -> after.status && (before == null || !before.status))).size()
				-
				// The number of unfollows
				diff.find(Changes.of(UserFollow.class, i -> i.user_id.equals(t.uid)).when((before, after) -> !after.status && (before == null || before.status))).size();
		return Safe.value(t.follow_count) + followCountChange;
	}

	@Override
	public Integer derive__Profile__follower_count(Profile t, Diff diff, Space.Selector selector) {
		// Note: This assumes that UserFollow's will only be imprinted when their status is changing. If a UserFollow is imprinted for some other reason it could be seen as a following change by this logic. If we find that use case comes up this will need to be reconsidered and perhaps handled in the follow_user action instead.
		int followerCountChange =
				// The number of new follows
				diff.find(Changes.of(UserFollow.class, i -> i.follow_user_id.equals(t.uid)).when((before, after) -> after.status && (before == null || !before.status))).size()
				-
				// The number of unfollows
				diff.find(Changes.of(UserFollow.class, i -> i.follow_user_id.equals(t.uid)).when((before, after) -> !after.status && (before == null || before.status))).size();
		return Safe.value(t.follower_count) + followerCountChange;
	}

	@Override
	public Boolean derive__Profile__is_following(Profile t, Diff diff, Space.Selector selector) {
		String selfUid = Safe.get(() -> selector.get(spec().things().loginInfo().build()).account.user_id);
		if (selfUid != null) {
			Change<UserFollow> followChange = diff.find(new UserFollow.Builder().follow_user_id(t.uid).user_id(selfUid).build());
			if (followChange != null) return followChange.latest.status;
		}
		return t.is_following;
	}
	
	@Override
	public List<FeedItem> derive__Feed__feed(Feed t, Diff diff, Space.Selector selector) {
		return cleanRecommendations(t.feed, selector);
	}
	
	@Override
	public Get get(Get t, Collection<String> reactions, Diff diff, Space.Selector selector) {
		// We'll always require that the remote derives it originally, we won't attempt to derive it locally
		if (t == null) return t;
		
		// Locally we will only attempt to rederive cases where the get is being used as an items query.
		
		// Here are cases that resembles a "sync" usage or obtaining of non-item info
		// We won't attempt to derive or rederive these at all, they must be handled remotely.
		if (t.declared.changes_since
			|| t.declared.include_account
			|| t.declared.sposts
			|| t.declared.taglist
			|| t.declared.include_notification_status
			|| t.declared.premium
			|| t.declared.include_connected_accounts
			|| t.declared.listCount
			|| t.declared.listen
			|| t.declared.since_m
			|| t.declared.forceposts
			|| t.declared.forcetweetupgrade
			|| t.declared.forcevideolength
			|| t.declared.forceannotations
			|| t.declared.force70upgrade
			|| t.declared.forcemails
			|| t.declared.forcerediscovery
			|| t.declared.forcetaglist
			|| t.declared.forceaccount
			|| t.declared.forcepremium
			|| t.declared.forceconnectedaccounts
			|| t.declared.forceListCount
			|| t.declared.forcesettings) {
			return t;
		} else if (t.declared.item) {
			// We don't have a use case for this right now, so not supporting it yet.
			return t;
		} else if (t.list == null) {
			// Not sure what this case would be, but we'll have nothing to do if this is the case.
			return t;
		}
		
		boolean isStateSupported = false;
			// TODO if we want to enable locally deriving Favorites, Archive, etc we'd need a way (possible local action?) to
			// inform this that we "know" how to derive certain cases. For example, inform the deriver that we know all item's
			// that have a status of UNREAD. Or if we started caching the Archive, that it knows how to derive ARCHIVE or maybe just the
			// first page of the ARCHIVE or something.  With this currently set to false, the else {} block below will never run.
			// Leaving it in for now in case it is something we want to use in the future.
		boolean sortSupported
				 = t.sort == null
				|| t.sort == ItemSortKey.NEWEST
				|| t.sort == ItemSortKey.OLDEST
				|| t.sort == ItemSortKey.ANNOTATION;
		boolean isPremiumSearch
				 = (!StringUtils.isBlank(t.search) && Safe.getBoolean(() -> getLoginInfo(selector).account.premium_features.contains(PremiumFeature.PREMIUM_SEARCH)))
				|| (t.search_matches != null && !t.search_matches.isEmpty());
		boolean hasPreviousPages
				= SublistUtil.hasPreviousPages(t, selector);
		if (!isStateSupported || !sortSupported || isPremiumSearch || !hasPreviousPages) {
			/*
				We don't have enough information to perfectly rederive this locally.
				There are a lot of potential changes we can't properly calculate or would be complex to calculate.
				
				For example:
				* If an item changes such that it should be now appear in this list, it may not be clear where to insert it.
				* If an item is removed from this list and then later readded, it will have lost its sort_id and search_matches and we won't be able to restore them without having kept track of it somehow.
				* If an item changes such that it should be reordered, if we don't know how to sort, we can't make that change
				* If a change should cause one or many items to move between pages or fill in non-empty spots in pages, that can be complex to calculate, or impossible depending on what pages we have available locally. (page here means using offset and count to get a subset of the list)
	
				So the best we can do is filter out and remove items that no longer match the query,
				and try to detect the cases where something changed that could have potentially caused a change here,
				and then flag that this instance needs to be synced with the remote to make sure it is correct.
			*/
			
			// Define what we are able to check, in terms of whether an item belongs in this list or not:
			// This isn't complete, since there are some aspects we can't check like search, offset or count.
			Changes.ThingMatch<Item> match = item -> {
				if (!ItemUtil.matchesStatusKey(item, t.state)) return false;
				if (t.favorite != null && t.favorite != Safe.value(item.favorite)) return false;
				if (t.tag != null && !ItemUtil.matchesTag(item, t.tag)) return false;
				if (t.declared.contentType && !ItemUtil.matchesContentType(item, t.contentType, null)) return false;
				if (t.hasAnnotations != null && t.hasAnnotations != (item.annotations != null && !item.annotations.isEmpty())) return false;
				if (t.shared != null && t.shared != ItemUtil.hasShareOf(item, SharedItemStatus.ACCEPTED)) return false;
				if (ItemUtil2Kt.shouldRemove(item, t.filters)) return false;
				return true;
			};
			
			// Remove any items that don't match, and remove their sort_id and search_matches entries.
			List<Item> list = new ArrayList<>(t.list.size());
			List<Integer> sort_ids = t.sort_ids != null ? new ArrayList<>(t.sort_ids.size()) : null;
			List<SearchMatch> search_matches = t.search_matches != null ? new ArrayList<>(t.search_matches.size()) : null;
			for (int i = 0, len = t.list.size(); i < len; i++) {
				Item item = t.list.get(i);
				if (match.matches(item)) {
					list.add(item);
					if (sort_ids != null) sort_ids.add(t.sort_ids.get(i));
					if (search_matches != null) search_matches.add(t.search_matches.get(i));
				}
			}
			if (list.size() != t.list.size() // If the we made changes. We have to flag in this case as well because techincally we might need to move some items between pages if an item is removed.
				|| !diff.find(Changes.of(Item.class).when((before, after) -> match.matches(after) && (before == null || !match.matches(before)))).isEmpty()) { // This line means: If any changed items now match this but didn't before.
				
				Get.Builder builder = t.builder().list(list);
				if (sort_ids != null) builder.sort_ids(sort_ids);
				if (search_matches != null) builder.search_matches(search_matches);
				
				// Flag this as needed a remote sync to be 100% sure it is correct.
				selector.addInvalid(t);
				
				return builder.build();
			} else {
				return t;
			}
			
		} else {
			// The remaining cases should be able to be derived locally.
			// To share code, we can use the saves deriving logic
			Saves.Builder query = new Saves.Builder();
			if (t.declared.state) query.state(t.state);
			if (t.declared.search) query.search(t.search);
			if (t.declared.favorite) query.favorite(t.favorite);
			if (t.declared.tag) query.tag(t.tag);
			if (t.declared.contentType) query.contentType(t.contentType);
			if (t.declared.hasAnnotations) query.hasAnnotations(t.hasAnnotations);
			if (t.declared.shared) query.shared(t.shared);
			if (t.declared.sort) query.sort(t.sort);
			if (t.declared.count) query.count(t.count);
			if (t.declared.offset) query.offset(t.offset);
			Saves result = derive(query.build(), selector);
			// Note: For now not going to support meta extras like .groups unless have a use case for them
			
			Get.Builder updated = t.builder()
					.list(result.list);
			
			if (t.declared.sort_ids) {
				int size = result.list.size();
				List<Integer> sort_ids = new ArrayList<>(size);
				int offset = Safe.value(t.offset);
				for (int i = 0; i < size; i++) {
					sort_ids.add(offset+i);
				}
				updated.sort_ids(sort_ids);
			}
			return updated.build();
		}
	}

	@Override
	public List<Item> derive__LocalItems__items(LocalItems t, Diff diff, Space.Selector selector) {
		return select(selector, t.items)
				.mutable(s -> {

					final ItemStatusKey statusKey = t.status != null ? t.status : ItemStatusKey.UNREAD;

					if (t.items == null || diff == null) {
						// First derive, or diff is not available, use Saves to calculate it cleanly
						Saves.Builder builder = new Saves.Builder().state(statusKey).sort(ItemSortKey.NEWEST);
						if (Safe.value(t.max) > 0) builder.count(t.max);
						List<Item> items = derive__Saves__list(builder.build(), null, selector);
						return items;

					} else {
						// Re-derive, we can optimize and try to do the minimum work
						List<Item> items = Safe.nonNullCopy(t.items);
						for (Change<Item> change : diff.find(Changes.of(Item.class))) {
							Item now = change.latest;
							Item before = change.previous;
							// We can make assumptions about whether this item is already in this list based on its previous status value.
							if (before == null) {
								if (ItemUtil.matchesStatusKey(now, statusKey)) items.add(now);
							} else if ((ItemUtil.matchesStatusKey(before, statusKey)) != (ItemUtil.matchesStatusKey(now, statusKey))) {
								if (ItemUtil.matchesStatusKey(now, statusKey)) {
									items.add(now);
								} else {
									items.remove(now);
								}
							} else {
								// Based on reactives, this is just a sort order change, which will be handled below as needed
							}
						}
						if (Safe.value(t.max) > 0) {
							// Only bother with the cost of sorting and trimming if we actually need to trim something
							if (items.size() > t.max) {
								ItemUtil.sort(items, ItemSortKey.NEWEST);
								items = SublistUtil.applyOffsetCount(items, 0, t.max);
							}
						}
						return items;
					}
				}).query();
	}

	@Override
	public List<Item> derive__Saves__list(Saves thing, Diff diff, Space.Selector selector) {
		return select(selector, thing.list)
				.mutable(s -> {
					// TODO Optimizations:
					// 1. If already derived once, we could look at the diff, and if the item changes aren't ones that would add/remove or re-sort, then nothing will change in the list here, so we could just return the current list and avoid all this work.
					// 2. Looks like 80% of this time is spent sorting. Maybe there is a way to avoid getting a randomized list every time, so sorting can be faster
					List<Item> items = s.getOfType(Item.THING_TYPE, Item.class);
					Iterator<Item> it = items.iterator();
					while (it.hasNext()) {
						Item i = it.next();
						if (!ItemUtil.matchesStatusKey(i, thing.state)) {
							it.remove();
							continue;
						}
						if (thing.group_id != null && !thing.group_id.equals(i.badge_group_id)) {
							it.remove();
							continue;
						}
						if (thing.minWordCount != null && Safe.value(i.word_count) < thing.minWordCount) {
							it.remove();
							continue;
						}
						if (thing.maxWordCount != null && Safe.value(i.word_count) > thing.maxWordCount) {
							it.remove();
							continue;
						}
						if (thing.minTimeSpent != null) {
							boolean meetsCriteria = false;
							if (i.positions != null) {
								for (Position p : i.positions.values()) {
									if (Safe.value(p.time_spent) >= thing.minTimeSpent) {
										meetsCriteria = true;
										break;
									}
								}
							}
							if (!meetsCriteria) {
								it.remove();
								continue;
							}
						}
						if (thing.maxScrolled != null) {
							if (i.positions != null) {
								boolean over = false;
								for (Position p : i.positions.values()) {
									if (Safe.value(p.percent) > thing.maxScrolled) {
										over = true;
										break;
									}
								}
								if (over) {
									it.remove();
									continue;
								}
							}
						}
						if (thing.favorite != null && Safe.value(i.favorite) != thing.favorite) {
							it.remove();
							continue;
						}
						if (thing.hasAnnotations != null && (i.annotations != null && !i.annotations.isEmpty()) != thing.hasAnnotations) {
							it.remove();
							continue;
						}
						if (!StringUtils.isBlank(thing.search)) {
							if (!StringUtils.containsIgnoreCase(i.given_title, thing.search)
								&& !StringUtils.containsIgnoreCase(i.resolved_title, thing.search)
								&& !StringUtils.containsIgnoreCase(UrlString.asString(i.resolved_url), thing.search)
								&& !StringUtils.containsIgnoreCase(UrlString.asString(i.given_url), thing.search)) {
									it.remove();
									continue;
							}
						}
						if (thing.tag != null && !ItemUtil.matchesTag(i, thing.tag)) {
							it.remove();
							continue;
						}
						if (thing.contentType != null && !ItemUtil.matchesContentType(i, thing.contentType, thing.contentType == ItemContentType.LISTENABLE ? selector.get(new ListenSettings.Builder().build()) : null)) {
							it.remove();
							continue;
						}
						if (thing.is_article != null && Safe.value(i.is_article) != thing.is_article) {
							it.remove();
							continue;
						}
						if (thing.shared != null) {
							if (ItemUtil.hasShareOf(i, SharedItemStatus.ACCEPTED) != thing.shared) {
								it.remove();
								continue;
							}
						}
						if (thing.added_since != null && Timestamp.get(i.time_added) < thing.added_since.value) {
							it.remove();
							continue;
						}
						if (thing.archived_since != null && Timestamp.get(i.time_read) < thing.archived_since.value) {
							it.remove();
							continue;
						}
						if (thing.item_id != null && !thing.item_id.equals(i.item_id)) {
							it.remove();
							continue;
						}
						if (thing.host != null && !thing.host.equalsIgnoreCase(DomainUtils.getHost(i.id_url.url))) {
							it.remove();
							continue;
						}
						if (thing.downloadable != null && ItemUtil.downloadables(i, thing.downloadable, Safe.value(thing.downloadable_retries)).isEmpty()) {
							it.remove();
							continue;
						}
						if (ItemUtil2Kt.shouldRemove(i, thing.filters)) {
							it.remove();
							continue;
						}
					}
					
					if (!ItemUtil.sort(items, thing.sort)) {
						throw new RuntimeException("unsupported sort " + thing.sort);
					}
					
					items = SublistUtil.applyOffsetCount(items, thing.offset, thing.count);
					return items;
				})
				.query();
	}
	
	public List<FeedItem> derive__GetProfileFeed__feed(GetProfileFeed t, Diff diff, Space.Selector selector) {
		return cleanFeed(t.feed, selector);
	}
	
	/** Same as {@link #fieldOnGetAndFetch(Diff, Function, Function, Function, Function)} but only searches for {@link Get}s */
	private <R> List<R> fieldOnGet(Diff diff, Function<Get,Boolean> getDeclared, Function<Get,R> getValue) {
		return fieldOnGetAndFetch(diff, getDeclared, getValue, null, null);
	}
	
	/**
	 * Looks at the diff for any {@link Get} or {@link Fetch} that are new, and didn't have previous values (as expected for part of a sync)
	 * and extracts a field from them if this field was declared.  This returns a list of those extracted values, for all get and fetches found in the diff, with the
	 * latest (as ordered by their `since` value) as the first in the list. If none were found in the diff, this returns an empty list.
	 * @param diff The diff to look in
	 * @param getDeclared A lambda that returns if this field is declared in a {@link Get} or null not to search gets
	 * @param getValue A lambda that returns this field's value from a {@link Get}
	 * @param fetchDeclared A lambda that returns if this field is declared in a {@link Fetch} or null not to search fetches
	 * @param fetchValue A lambda that returns this field's value from a {@link Fetch}
	 */
	private <R> List<R> fieldOnGetAndFetch(Diff diff, Function<Get,Boolean> getDeclared, Function<Get,R> getValue, Function<Fetch,Boolean> fetchDeclared, Function<Fetch,R> fetchValue) {
		Set<Get> gets = getDeclared != null ? diff.currentValues(Changes.of(Get.class, getDeclared::apply)) : null;
		Set<Fetch> fetches = fetchDeclared != null ? diff.currentValues(Changes.of(Fetch.class, fetchDeclared::apply)) : null;
		List<Thing> found = new ArrayList<>();
		if (gets != null) found.addAll(gets);
		if (fetches != null) found.addAll(fetches);
		Collections.sort(found, (o1, o2) -> {
			Timestamp t1 = o1 instanceof Get ? ((Get) o1).since : ((Fetch) o1).since;
			Timestamp t2 = o2 instanceof Get ? ((Get) o2).since : ((Fetch) o2).since;
			return ObjectUtils.compare(t2, t1);
		});
		List<R> values = new ArrayList<>();
		for (Thing t : found) {
			values.add(t instanceof Get ? getValue.apply((Get)t) : fetchValue.apply((Fetch)t));
		}
		return values;
	}
	
	/** Same as {@link #latestFieldOnGetAndFetch(Object, Diff, Function, Function, Function, Function)}  but only searches for {@link Get}s */
	private <R> R latestFieldOnGet(R def, Diff diff, Function<Get,Boolean> getDeclared, Function<Get,R> getValue) {
		return latestFieldOnGetAndFetch(def, diff, getDeclared, getValue, null, null);
	}
	
	/**
	 * Same as {@link #fieldOnGetAndFetch(Diff, Function, Function, Function, Function)} but returns the first value or if none found, returns `def`
	 * @param def what to return if there are no matching get/fetches in the diff.
	 */
	private <R> R latestFieldOnGetAndFetch(R def, Diff diff, Function<Get,Boolean> getDeclared, Function<Get,R> getValue, Function<Fetch,Boolean> fetchDeclared, Function<Fetch,R> fetchValue) {
		List<R> r = fieldOnGetAndFetch(diff, getDeclared, getValue, fetchDeclared, fetchValue);
		return r != null && !r.isEmpty() ? r.get(0) : def;
	}
	
	@Override
	public Integer derive__Fetch__remaining_items(Fetch t, Diff diff, Space.Selector selector) {
		if (t.chunk != null) return t.remaining_chunks;
		if (t.total == null || t.passthrough == null || t.passthrough.firstChunkSize == null) return 0;
		return t.total - t.passthrough.firstChunkSize;
	}
	
	@Override
	public Integer derive__Fetch__remaining_chunks(Fetch t, Diff diff, Space.Selector selector) {
		if (t.chunk != null) return t.remaining_chunks;
		if (t.remaining_items == null || t.passthrough == null || t.passthrough.firstChunkSize == null) return t.remaining_chunks;
		if (t.remaining_items <= 0 || t.passthrough.fetchChunkSize == 0) return 0;
		return (int) Math.ceil(t.remaining_items / (float) t.passthrough.fetchChunkSize);
	}
	
	@Override
	public Friend derive__SharedItem__friend(SharedItem t, Diff diff, Space.Selector selector) {
		if (selector == null || t.from_friend_id == null) return t.friend; // Nothing to look up or can't
		if (t.friend != null && t.friend.friend_id.equals(t.from_friend_id)) return t.friend; // Already found
		Friend found = selector.get(new Friend.Builder().friend_id(t.from_friend_id).build());
		return found != null ? found : t.friend; // If not found, keep existing value
	}
	
	@Override
	public GetLikes getLikes(GetLikes t, Collection<String> reactions, Diff diff, Space.Selector selector) {
		// There are some changes we can react to and update locally but handling them correctly, especially with paging will bit of careful work
		// Given the use case here is pretty small, for now   t feels ok to just flag as needing a remote update.
		// Can come back and implement this if we find that we want to keep getLikes offline or we have a case where users might expect this to update more quickly
		selector.addInvalid(t);
		return t;
	}
	
	@Override
	public GetReposts getReposts(GetReposts t, Collection<String> reactions, Diff diff, Space.Selector selector) {
		// There are some changes we can react to and update locally but handling them correctly, especially with paging will bit of careful work
		// Given the use case here is pretty small, for now it feels ok to just flag as needing a remote update.
		// Can come back and implement this if we find that we want to keep getReposts offline or we have a case where users might expect this to update more quickly
		selector.addInvalid(t);
		return t;
	}
	
	@Override
	public Integer derive__ListCounts__unread(ListCounts t, Diff diff, Space.Selector selector) {
		if (!Safe.value(t.local)) return t.unread;
		return select(selector, t.unread)
				.mutable(map -> countOf(map, Item.THING_TYPE, Item.class, item -> item.status == ItemStatus.UNREAD))
				.query();
	}
	
	@Override
	public Integer derive__ListCounts__unread_articles(ListCounts t, Diff diff, Space.Selector selector) {
		if (!Safe.value(t.local)) return t.unread_articles;
		return select(selector, t.unread_articles)
				.mutable(map -> countOf(map, Item.THING_TYPE, Item.class, item -> item.status == ItemStatus.UNREAD && Safe.value(item.is_article)))
				.query();
	}
	
	@Override
	public Integer derive__ListCounts__unread_shared_to_me(ListCounts t, Diff diff, Space.Selector selector) {
		return t.unread_shared_to_me; // TODO This requires a bunch of joins, we aren't using this yet so can come back and implement it if and when we need it.
	}
	
	@Override
	public Integer derive__ListCounts__unread_untagged(ListCounts t, Diff diff, Space.Selector selector) {
		return t.unread_untagged; // TODO This requires a bunch of joins, we aren't using this yet so can come back and implement it if and when we need it.
	}
	
	@Override
	public Integer derive__ListCounts__unread_videos(ListCounts t, Diff diff, Space.Selector selector) {
		if (!Safe.value(t.local)) return t.unread_videos;
		return select(selector, t.unread_videos)
				.mutable(map -> countOf(map, Item.THING_TYPE, Item.class, item -> item.status == ItemStatus.UNREAD && (item.has_video == Videoness.HAS_VIDEOS || item.has_video == Videoness.IS_VIDEO)))
				.query();
	}
	
	private static <T extends Thing> int countOf(MutableSpace.Selector space, String type, Class<T> clazz, Changes.ThingMatch<T> match) {
		int count = 0;
		for (T t : space.getOfType(type, clazz)) {
			if (match.matches(t)) count++;
		}
		return count;
	}
	
	@Override
	public Integer derive__ListCounts__archived(ListCounts t, Diff diff, Space.Selector selector) {
		if (!Safe.value(t.local)) return t.archived;
		return select(selector, t.archived)
				.mutable(map -> countOf(map, Item.THING_TYPE, Item.class, item -> item.status == ItemStatus.ARCHIVED))
				.query();
	}
	
	@Override
	public Integer derive__ListCounts__favorites(ListCounts t, Diff diff, Space.Selector selector) {
		if (!Safe.value(t.local)) return t.favorites;
		return select(selector, t.favorites)
				.mutable(map -> countOf(map, Item.THING_TYPE, Item.class, item -> Safe.value(item.favorite)))
				.query();
	}
	
	@Override
	public Integer derive__ListCounts__highlights(ListCounts t, Diff diff, Space.Selector selector) {
		return t.highlights; // TODO This requires a bunch of joins, we aren't using this yet so can come back and implement it if and when we need it.
	}
	
	private <T> SelectorHelper<T> select(Space.Selector selector, T currentValue) {
		return new SelectorHelper<>(selector, currentValue);
	}
	
	@Override
	public String derive__LoginInfo__guid(LoginInfo t, Diff diff, Space.Selector selector) {
		Change<Guid> change = diff != null ? diff.find(spec().things().guid().build()) : null;
		return change != null ? change.latest.guid : t.guid;
	}
	
	@Override
	public List<AcEmail> derive__AutoCompleteEmails__auto_complete_emails(AutoCompleteEmails t, Diff diff, Space.Selector selector) {
		List<AcEmail> emails = latestFieldOnGetAndFetch(t != null ? t.auto_complete_emails : null, diff, g -> g.declared.auto_complete_emails, g -> g.auto_complete_emails, f -> f.declared.auto_complete_emails, f -> f.auto_complete_emails);
		Set<AcEmail> newEmails = diff.currentValues(Changes.of(AcEmail.class).when((before, after) -> before == null));
		if (!newEmails.isEmpty()) {
			emails = Safe.nonNullCopy(emails);
			emails.addAll(newEmails);
		}
		return emails;
	}
	
	@Override
	public List<PostService> derive__ConnectedAccounts__connectedAccounts(ConnectedAccounts t, Diff diff, Space.Selector selector) {
		return latestFieldOnGet(t != null ? t.connectedAccounts : null, diff, g -> g.declared.connectedAccounts, g -> g.connectedAccounts);
	}
	
	@Override
	public List<Friend> derive__RecentFriends__recent_friends(RecentFriends t, Diff diff, Space.Selector selector) {
		return latestFieldOnGetAndFetch(t != null ? t.recent_friends : null,diff, g -> g.declared.recent_friends, g -> g.recent_friends, f -> f.declared.recent_friends, f -> f.recent_friends);
	}
	
	@Override
	public List<Friend> derive__Friends__friends(Friends t, Diff diff, Space.Selector selector) {
		List<List<Friend>> changes = fieldOnGetAndFetch(diff, g -> g.declared.friends, g -> g.friends, f -> f.declared.friends, f -> f.friends);
		if (!changes.isEmpty()) {
			Set<Friend> updated = new HashSet<>();
			if (t.friends != null) updated.addAll(t.friends);
			for (List<Friend> change : changes) {
				if (change != null) updated.addAll(change);
			}
			return new ArrayList<>(updated);
		}
		return t.friends;
	}
	
	@Override
	public List<Group> derive__Groups__groups(Groups t, Diff diff, Space.Selector selector) {
		List<List<Group>> changes = fieldOnGet(diff, g -> g.declared.since, g -> g.groups);
		if (!changes.isEmpty()) {
			Set<Group> updated = new HashSet<>();
			if (t.groups != null) updated.addAll(t.groups);
			for (List<Group> change : changes) {
				if (change != null) updated.addAll(change);
			}
			return new ArrayList<>(updated);
		}
		return t.groups;
	}
	
	@Override
	public List<SearchQuery> derive__RecentSearches__searches(RecentSearches t, Diff diff, Space.Selector selector) {
		return latestFieldOnGet(t != null ? t.searches : null, diff, g -> g.declared.recent_searches, g -> g.recent_searches);
	}
	
	@Override
	public Boolean derive__AdzerkDecision__removeSponsoredLabel(AdzerkDecision t, Diff diff, Space.Selector selector) {
		return Safe.getBoolean(() -> t.contents.get(0).data.ctRemoveSponsorLabel.equals("true"));
	}

	@Override
	public List<AdzerkSpoc> derive__AdzerkSpocs__spocs(AdzerkSpocs t,
			Diff diff,
			Space.Selector selector) {
		// If user's {Account.premium_features} contains {PremiumFeature.ad_free} then set to empty list.
		boolean adFree = Safe.getBoolean(() -> {
			LoginInfo loginInfo = selector.get(new LoginInfo.Builder().build());
			return loginInfo.account.premium_features.contains(PremiumFeature.AD_FREE);
		});
		if (adFree) {
			return emptyList();
		}

		// If {getAdzerkDecisions.placement.divName} == {.name} then clear the list
		// and for each {AdzerkDecision} in {getAdzerkDecisions.decisions} add an {AdzerkSpoc} with:
		//   * {AdzerkSpoc.decision} set to the {AdzerkDecision}.
		//   * {AdzerkSpoc.placement} set to {getAdzerkDecisions.placement}.
		//   * For {AdzerkSpoc.item}:
		//     * Find or create an {Item} with {Item.given_url} = {AdzerkDecision.url}.
		//     * If {Item.given_title} is undeclared set it to {AdzerkDecision.title}
		//     * If {Item.display_thumbnail} is undeclared set {Item.top_image_url} to {AdzerkDecision.thumbnail}
		//   * {AdzerkSpoc.valid_until} set to {getAdzerkDecisions.received_at} + 24 hours.
		var response = findAdzerkApiResponse(diff, t.name);
		if (response != null) {
			long validUntilInMillis = response.received_at.millis() + TimeUnit.HOURS.toMillis(24);
			Timestamp validUntil = Timestamp.fromMillis(validUntilInMillis);

			var decisions = Safe.nonNullCopy(response.decisions);
			var spocs = new ArrayList<AdzerkSpoc>(decisions.size());
			for (AdzerkDecision decision : decisions) {
				Item id = new Item.Builder().given_url(decision.url).build();
				Item found = selector.get(id);
				Item item = found != null ? found : id;
				Item.Builder builder = item.builder();

				if (!item.declared.given_title) {
					builder.given_title(decision.title);
				}
				if (!item.declared.display_thumbnail) {
					builder.top_image_url(decision.thumbnail);
				}

				spocs.add(
						new AdzerkSpoc.Builder()
								.decision(decision)
								.placement(response.placement)
								.item(builder.build())
								.valid_until(validUntil)
								.build()
				);
			}
			return spocs;
		}

		// Otherwise don't update.
		return super.derive__AdzerkSpocs__spocs(t, diff, selector);
	}

	private static GetAdzerkDecisions findAdzerkApiResponse(@Nullable Diff diff,
			AdzerkPlacementName placementName) {
		if (diff == null) diff = new Diff();

		var responses = diff.currentValues(Changes.of(GetAdzerkDecisions.class)
				.when((__, after) -> after.placement != null &&
						placementName.equals(after.placement.divName)));
		return responses != null ? firstOrNull(responses) : null;
	}
	
	@Override
	public List<HiddenSpoc> derive__HiddenSpocs__spocs(HiddenSpocs t,
			Diff diff,
			Space.Selector selector) {
		ArrayList<HiddenSpoc> list = new ArrayList<>(t.spocs);
		
		list.addAll(diff.currentValues(Changes.of(HiddenSpoc.class)));
		
		int maxSize = 100;
		if (list.size() > maxSize) {
			Collections.sort(list, 
					(l, r) -> -Long.compare(l.time_hidden.unixSeconds, r.time_hidden.unixSeconds));
			return list.subList(0, maxSize);
		} else {
			return list;
		}
	}

	@Override
	public SyncState syncState(SyncState thing, Collection<String> reactions, Diff diff, Space.Selector selector) {
		SyncState.Builder b = thing.builder();
		Set<Get> gets = diff.currentValues(Changes.of(Get.class));
		Set<Fetch> fetches = diff.currentValues(Changes.of(Fetch.class));
		List<Thing> found = new ArrayList<>();
		if (gets != null) found.addAll(gets);
		if (fetches != null) found.addAll(fetches);
		Collections.sort(found, (o1, o2) -> {
			Timestamp t1 = o1 instanceof Get ? ((Get) o1).since : ((Fetch) o1).since;
			Timestamp t2 = o2 instanceof Get ? ((Get) o2).since : ((Fetch) o2).since;
			return Long.compare(t1 != null ?  t1.value : 0, t2 != null ? t2.value : 0);
		});
		
		for (Thing t : found) {
			if (t instanceof Fetch) {
				Fetch f = (Fetch) t;
				if (f.declared.since) b.since(f.since);
			} else {
				Get g = (Get) t;
				if (g.declared.since) b.since(g.since);
			}
		}
		return b.build();
	}
	
	/**
	 * <pre>
	 * ~ Set from {get.tags}
	 * ~ If a client has all items that are archived and unread, it can calculate this by looking for all possible tags in {Item.tags}
	 * ~ Sort ASC
	 * </pre>
	 */
	@Override
	public List<String> derive__Tags__tags(Tags t, Diff diff, Space.Selector selector) {
		List<String> tags = null;
		List<String> latestTagsOnGet = latestFieldOnGet(t != null ? t.tags : null, diff, g -> g.declared.tags, g -> g.tags);
		if (latestTagsOnGet != null) {
			tags = new ArrayList<>(latestTagsOnGet);
		}
		
		// After the first get also find any new tags. We don't have enough data locally to know
		// when we can remove a tag from this list, but we can add new ones.
		if (tags != null) {
			for (Change<Item> change : diff.find(Changes.of(Item.class))) {
				TagUtil.addAll(change.latest.tags, tags);
			}
		}
		
		if (tags != null) Collections.sort(tags);
		return tags;
	}

	/**
	 * <pre>
	 * ~ If there's a new tag added to {.tags} or an existing tag added to {Item.tags},
	 * ~ add it at the start of the list.
	 * ~ If it was already in the list, move it to the start instead.
	 * ~ If a tag was removed from {.tags} remove it from the list.
	 * </pre>
	 */
	
	@Override
	public List<Tag> derive__Tags__recentlyUsed(Tags t, Diff diff, Space.Selector selector) {
		List<Tag> recentTags = Safe.nonNullCopy(t.recentlyUsed);

		for (Change<Tags> change : diff.find(Changes.of(Tags.class))) {
			if (change.latest == null || change.latest.tags == null ||
					change.previous == null || change.previous.tags == null) {
				continue;
			}

			// If there's a new tag added to {.tags} add it at the start of the list.
			Collection<String> added = subtract(change.latest.tags, change.previous.tags);
			if (!added.isEmpty()) {
				List<Tag> newTags = map(added, name -> new Tag.Builder().tag(name).build());
				// If it was already in the list, move it to the start instead.
				recentTags.removeAll(newTags);
				recentTags.addAll(0, newTags);
			}

			// If a tag was removed from {.tags} remove it from the list.
			Collection<String> removed = subtract(change.previous.tags, change.latest.tags);
			removeAll(recentTags, it -> removed.contains(it.tag));
		}

		// If an existing tag is added to {Item.tags}, add it at the start of the list.
		for (Change<Item> change : diff.find(Changes.of(Item.class))) {
			if (change.latest == null || change.latest.tags == null || change.previous == null) {
				continue;
			}
			
			List<Tag> newTags = Safe.nonNullCopy(change.latest.tags);
			if (change.previous.tags != null) {
				newTags.removeAll(change.previous.tags);
			}
			// If it was already in the list, move it to the start instead.
			recentTags.removeAll(newTags);
			recentTags.addAll(0, newTags);
		}

		return recentTags;
	}
	
	/**
	 * <pre>
	 * ~ If {guid.guid} changed, reset the value to an empty map.
	 * ~ If {LoginInfo.account.user_id} changed and wasn't previously null, reset the value to an empty map.
	 * ~ Otherwise convert {getUnleashAssignments.assignments.assignments} to a map from {UnleashAssignment.name} to {UnleashAssignment}.
	 * </pre>
	 */
	@Override
	public Map<String, UnleashAssignment> derive__Unleash__current_assignments(Unleash t,
			Diff diff,
			Space.Selector selector) {

		// If guid changed, reset the value to an empty list.
		for (Change<Guid> change : diff.find(Changes.of(Guid.class))) {
			if (!areEqual(Safe.get(() -> change.latest.guid),
					Safe.get(() -> change.previous.guid))) {
				return Collections.emptyMap();
			}
		}

		// If user id changed and wasn't previously null, reset the value to an empty list.
		for (Change<LoginInfo> change : diff.find(Changes.of(LoginInfo.class))) {
			String previousId = Safe.get(() -> change.previous.account.user_id);
			if (previousId != null &&
					!previousId.equals(Safe.get(() -> change.latest.account.user_id))) {
				return Collections.emptyMap();
			}
		}

		// Otherwise convert {getUnleashAssignments.assignments.assignments}
		// to a map from {UnleashAssignment.name} to {UnleashAssignment}.
		for (GetUnleashAssignments v : diff.currentValues(Changes.of(GetUnleashAssignments.class))) {
			HashMap<String, UnleashAssignment> assignments = new HashMap<>(v.assignments.assignments.size());
			for (UnleashAssignment assignment : v.assignments.assignments) {
				assignments.put(assignment.name, assignment);
			}
			return assignments;
		}

		// Not expected, but if no changes found just keep the current value.
		return super.derive__Unleash__current_assignments(t, diff, selector);
	}

	private static LoginInfo getLoginInfo(Space.Selector selector) {
		return selector.get(new LoginInfo.Builder().build());
	}
}
