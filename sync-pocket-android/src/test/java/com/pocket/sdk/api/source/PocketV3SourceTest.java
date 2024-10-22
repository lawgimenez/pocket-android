package com.pocket.sdk.api.source;

import com.pocket.sdk.api.endpoint.AppInfo;
import com.pocket.sdk.api.endpoint.Credentials;
import com.pocket.sdk.api.endpoint.DeviceInfo;
import com.pocket.sdk.api.generated.action.Acctchange;
import com.pocket.sdk.api.generated.action.Add;
import com.pocket.sdk.api.generated.action.AddAlias;
import com.pocket.sdk.api.generated.action.AddAnnotation;
import com.pocket.sdk.api.generated.action.Archive;
import com.pocket.sdk.api.generated.action.Delete;
import com.pocket.sdk.api.generated.action.DeleteAlias;
import com.pocket.sdk.api.generated.action.DeleteAnnotation;
import com.pocket.sdk.api.generated.action.Favorite;
import com.pocket.sdk.api.generated.action.FollowAllUsers;
import com.pocket.sdk.api.generated.action.FollowUser;
import com.pocket.sdk.api.generated.action.PostDelete;
import com.pocket.sdk.api.generated.action.PostLike;
import com.pocket.sdk.api.generated.action.PostRemoveLike;
import com.pocket.sdk.api.generated.action.PostRemoveRepost;
import com.pocket.sdk.api.generated.action.Readd;
import com.pocket.sdk.api.generated.action.Scrolled;
import com.pocket.sdk.api.generated.action.SharePost;
import com.pocket.sdk.api.generated.action.TagDelete;
import com.pocket.sdk.api.generated.action.TagRename;
import com.pocket.sdk.api.generated.action.TagsAdd;
import com.pocket.sdk.api.generated.action.TagsClear;
import com.pocket.sdk.api.generated.action.TagsRemove;
import com.pocket.sdk.api.generated.action.TagsReplace;
import com.pocket.sdk.api.generated.action.UndoArchive;
import com.pocket.sdk.api.generated.action.UndoDelete;
import com.pocket.sdk.api.generated.action.Unfavorite;
import com.pocket.sdk.api.generated.action.UnfollowUser;
import com.pocket.sdk.api.generated.enums.ItemSortKey;
import com.pocket.sdk.api.generated.enums.ItemStatus;
import com.pocket.sdk.api.generated.enums.ItemStatusKey;
import com.pocket.sdk.api.generated.enums.PositionType;
import com.pocket.sdk.api.generated.enums.PostChannel;
import com.pocket.sdk.api.generated.enums.SuggestionsType;
import com.pocket.sdk.api.generated.thing.Account;
import com.pocket.sdk.api.generated.thing.Annotation;
import com.pocket.sdk.api.generated.thing.EmailAlias;
import com.pocket.sdk.api.generated.thing.FeedItem;
import com.pocket.sdk.api.generated.thing.Get;
import com.pocket.sdk.api.generated.thing.GetFollowers;
import com.pocket.sdk.api.generated.thing.GetFollowing;
import com.pocket.sdk.api.generated.thing.GetItem;
import com.pocket.sdk.api.generated.thing.GetLikes;
import com.pocket.sdk.api.generated.thing.GetPost;
import com.pocket.sdk.api.generated.thing.GetProfile;
import com.pocket.sdk.api.generated.thing.GetProfileFeed;
import com.pocket.sdk.api.generated.thing.GetReposts;
import com.pocket.sdk.api.generated.thing.GetSuggestedFollows;
import com.pocket.sdk.api.generated.thing.Getuser;
import com.pocket.sdk.api.generated.thing.Guid;
import com.pocket.sdk.api.generated.thing.Item;
import com.pocket.sdk.api.generated.thing.OAuthAuthorize;
import com.pocket.sdk.api.generated.thing.Position;
import com.pocket.sdk.api.generated.thing.Signup;
import com.pocket.sdk.api.generated.thing.Tag;
import com.pocket.sdk.api.generated.thing.ValidateEmail;
import com.pocket.sdk.api.value.AccessToken;
import com.pocket.sdk.api.value.EmailString;
import com.pocket.sdk.api.value.Password;
import com.pocket.sdk.api.value.Timestamp;
import com.pocket.sdk.api.value.UrlString;
import com.pocket.sdk.network.EclecticOkHttpClient;
import com.pocket.sync.source.result.SyncException;
import com.pocket.util.java.Logs;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import okhttp3.OkHttpClient;

/**
 * Tests all endpoint based Things and all Actions on a {@link PocketRemoteSource}.
 * Ensures they successfully connect, send, receive and parse the expected data from the Pocket API.
 *
 * While a more strictly unit test would likely recreate a guid and account for each individual test,
 * that seems like it would be excessive on our API, creating a lot of accounts every time we ran this class.
 * So up front we create a guid and some accounts to use for all unit tests.
 *
 *
 */
public class PocketV3SourceTest {
	
	private static final boolean ENABLED = false;
	
	public static final AppInfo APP_IDENTITY = new AppInfo("5513-8646141fb5902c766272e74d", "Pocket", "Free", "6.7.0.0", "Google", "Google");
	public static final DeviceInfo DEVICE_IDENTITY = new DeviceInfo("Test", "Test", "Test", "Test", "Test", "mobile", "en-us", null);
	
	private static class User {
		public final String email;
		public final String guid;
		public final String accessToken;
		public final String password;
		public final String firstName;
		public final String uid;
		private User(String email, String guid, AccessToken accessToken, String password, String firstName, String uid) {
			this.email = email;
			this.guid = guid;
			this.accessToken = accessToken.value;
			this.password = password;
			this.firstName = firstName;
			this.uid = uid;
		}
	}
	
	private PocketRemoteSource src;
	
	@Before
	public void setUp() {
		src = new PocketRemoteSource(new EclecticOkHttpClient(new OkHttpClient()))
				.setCredentials(new Credentials(null, null, DEVICE_IDENTITY, APP_IDENTITY));
	}
	
	private PocketRemoteSource asNoOne() {
		return src.setCredentials(new Credentials(null, null, DEVICE_IDENTITY, APP_IDENTITY));
	}
	
	private PocketRemoteSource asGuid(User user) {
		return asGuid(user.guid);
	}
	
	private PocketRemoteSource asGuid(String guid) {
		return src.setCredentials(new Credentials(null, guid, DEVICE_IDENTITY, APP_IDENTITY));
	}
	
	private PocketRemoteSource asUser(User user) {
		return src.setCredentials(new Credentials(user.accessToken, user.guid, DEVICE_IDENTITY, APP_IDENTITY));
	}
	
	@Test
	public void everything() throws Exception {
		if (!ENABLED) return;
		
		Logs.logger(Logs.SOUT);
		
		// Functions
		validateEmail();
		// TODO shorten
		
		User alice = signup("alice");
		User bob = signup("bob");
		asNoOne();

		// Account
		guid();
		login(alice);
		// TODO oauth/request_meta
		// TODO oauth/approve_access
		getuser(alice);
		acctchange(alice);
		addAlias(alice);
		deleteAlias(alice);
		setAvatar(alice);
		// TODO friendFindersync
		// TODO registerSocialToken
		// TODO checkFeatures

		// Item
		asUser(alice);
		UrlString url = new UrlString("https://getpocket.com/"); // Note: specifically choose a url whose resolved_url is the same to simplify later assertions.
		add(url);
		String itemId = findItem(src.sync(new Get.Builder().build()).list, url).item_id;
		// getItem
		Assert.assertEquals(url, src.sync(new GetItem.Builder().item_id(itemId).build()).item.get(itemId).given_url);
		favorite(url);
		unfavorite(url);
		archive(url);
		readd(url);
		delete(url);
		undo_delete(url);
		archive(url);
		undo_archive(url);
		tags_add(url, "a", "b", "c");
		tags_replace(url, "d", "e", "f");
		tag_delete("d");
		tag_rename("e", "e2");
		tags_remove(url, "e2");
		tags_clear(url);
		Annotation annotation = new Annotation.Builder().annotation_id(UUID.randomUUID().toString()).quote("quote").patch("patch").version(2).build();
		add_annotation(url, annotation);
		delete_annotation(url, annotation);
		scrolled(url);
		
		// getItemAudio
//		src.sync(new GetItemAudio.IdBuilder().accent_locale("en-us").itemId(itemId))


		// Following
		
		// follow_user
		asUser(alice);
		src.sync(null, new FollowUser.Builder()
				.time(Timestamp.now())
				.user_list(Arrays.asList(bob.uid))
				.build());
		Assert.assertEquals(src.sync(new GetFollowing.Builder().profile_key(alice.uid).build()).profiles.get(0).uid, bob.uid);
		Assert.assertEquals(src.sync(new GetFollowers.Builder().profile_key(bob.uid).build()).profiles.get(0).uid, alice.uid);
		Assert.assertEquals(src.sync(new GetProfile.Builder().profile_key(alice.uid).build()).profile.follow_count.intValue(), 1);
		
		// unfollow_user
		asUser(alice);
		src.sync(null, new UnfollowUser.Builder()
				.time(Timestamp.now())
				.user_list(Arrays.asList(bob.uid))
				.build());
		Assert.assertTrue(src.sync(new GetFollowing.Builder().profile_key(alice.uid).build()).profiles.isEmpty());
		Assert.assertTrue(src.sync(new GetFollowers.Builder().profile_key(bob.uid).build()).profiles.isEmpty());
		Assert.assertEquals(src.sync(new GetProfile.Builder().profile_key(alice.uid).build()).profile.follow_count.intValue(), 0);
		
		// follow_all_users
		asUser(alice);
		// getSuggestedFollows
		GetSuggestedFollows suggestionsBefore = src.sync(new GetSuggestedFollows.Builder().version("2").social_service(SuggestionsType.POCKET).build());
		src.sync(null, new FollowAllUsers.Builder()
				.time(Timestamp.now())
				.social_service(SuggestionsType.POCKET)
				.build());
		Assert.assertTrue(src.sync(new GetFollowing.Builder().profile_key(alice.uid).build()).profiles.size() >= suggestionsBefore.suggested_follows.size());
		
		// share_post TODO there are a few variants we should test
		asUser(alice);
		src.sync(null, new SharePost.Builder()
				.time(Timestamp.now())
				.url(url)
				.channels(Arrays.asList(PostChannel.PUBLIC_PROFILE))
				.build());
		FeedItem post = src.sync(new GetProfileFeed.Builder().profile_key(alice.uid).build()).feed.get(0);
		Assert.assertEquals(post.item.given_url, url);
		
		// post_like
		asUser(bob);
		src.sync(null, new PostLike.Builder()
				.time(Timestamp.now())
				.post_id(post.post.post_id)
				.build());
		Assert.assertEquals(src.sync(new GetLikes.Builder().post_id(post.post.post_id).build()).profiles.get(0).uid, bob.uid);
		Assert.assertEquals(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.like_count.count.intValue(), 1);
		Assert.assertTrue(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.like_status);
		
		// post_remove_like
		asUser(bob);
		src.sync(null, new PostRemoveLike.Builder()
				.time(Timestamp.now())
				.post_id(post.post.post_id)
				.build());
		Assert.assertTrue(src.sync(new GetLikes.Builder().post_id(post.post.post_id).build()).profiles.isEmpty());
		Assert.assertEquals(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.like_count.count.intValue(), 0);
		Assert.assertFalse(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.like_status);
		
		// share_post  as repost variant
		asUser(bob);
		src.sync(null, new SharePost.Builder()
				.time(Timestamp.now())
				.url(url)
				.comment("comment")
				.original_post_id(post.post.post_id)
				.channels(Arrays.asList(PostChannel.PUBLIC_PROFILE))
				.build());
		FeedItem repost = src.sync(new GetProfileFeed.Builder().profile_key(bob.uid).build()).feed.get(0);
		Assert.assertNotNull(repost);
		Assert.assertEquals(repost.post.original_post.post_id, post.post.post_id);
		Assert.assertEquals(src.sync(new GetReposts.Builder().post_id(post.post.post_id).build()).profiles.get(0).uid, bob.uid);
		Assert.assertEquals(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.repost_count.count.intValue(), 1);
		Assert.assertTrue(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.repost_status);
		
		// post_remove_repost
		asUser(bob);
		src.sync(null, new PostRemoveRepost.Builder()
				.time(Timestamp.now())
				.post_id(post.post.post_id)
				.build());
		assertEmpty(src.sync(new GetProfileFeed.Builder().profile_key(bob.uid).build()).feed);
		assertEmpty(src.sync(new GetReposts.Builder().post_id(post.post.post_id).build()).profiles);
		Assert.assertEquals(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.repost_count.count.intValue(), 0);
		Assert.assertFalse(src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()).post.post.repost_status);
		
		// post_delete
		asUser(alice);
		src.sync(null, new PostDelete.Builder()
				.time(Timestamp.now())
				.post_id(post.post.post_id)
				.build());
		assertEmpty(src.sync(new GetProfileFeed.Builder().profile_key(alice.uid).build()).feed);
		assertThrowsException(() -> src.sync(new GetPost.Builder().profile_key(alice.uid).post_id(post.post.post_id).build()));
		
		
		// Send to Friend
		// TODO requires accounts with confirmed emails, will need to setup some accounts to use for this
		// asUser(alice);
		// shared_to();
		// share_added
		// share_ignored
		
		// Premium
		// TODO requires a premium account, will need to do some setup for this
		// recent_search
		// purchase_status
		// suggested_tags
		// refresh_library
		
		
/*



		Getting
https://text.getpocket.com/v3beta/mobile
feed
fetch
get


getMessage
getNotifications

https://text.getpocket.com/v3beta/loadWebCache
send
send_guid

		Not sure how to test
reportArticleView
resendEmailConfirmation
email
deregister_gcm
register_gcm
purchase
		 */
	}
	
	private void assertEmpty(Collection c) {
		Assert.assertTrue(c == null || c.isEmpty());
	}
	
	interface ThrowingRunnable {
		void run() throws Throwable;
	}
	
	private void assertThrowsException(ThrowingRunnable r) {
		try {
			r.run();
			Assert.fail();
		} catch (Throwable t) {}
	}
	
	public void guid() throws Exception {
		asNoOne();
		Guid guid = src.sync(new Guid.Builder().build());
		Assert.assertNotNull(guid.guid);
	}
	
	public void validateEmail() throws Exception {
		asNoOne();
		Assert.assertNotNull(src.sync(new ValidateEmail.Builder().email("valid@getpocket.com").build()));
		assertThrowsException(() -> src.sync(new ValidateEmail.Builder().email("not a valid email address").build()));
	}
	
	public User signup(String firstName) throws SyncException {
		asNoOne();
		String guid = src.sync(new Guid.Builder().build()).guid;
		
		asGuid(guid);
		String email = "android-pkt-unit-test+" + System.currentTimeMillis() + "@readitlater.com";
		String password = "123456";
		Signup signup = src.sync(new Signup.Builder()
				.email(new EmailString(email))
				.first_name(firstName)
				.password(new Password(password))
				.source("email")
				.get_access_token(true)
				.include_account(true)
				.build());
		Assert.assertNotNull(signup.access_token);
		Assert.assertNotNull(signup.account.user_id);
		Assert.assertEquals(signup.account.email, email);
		Assert.assertEquals(signup.account.first_name, firstName);
		
		System.out.println("====");
		System.out.println("Created User");
		System.out.println("firstName : " + firstName);
		System.out.println("email : " + email);
		System.out.println("password : " + password);
		System.out.println("accessToken : " + signup.access_token);
		System.out.println("guid : " + guid);
		System.out.println("uid : " + signup.account.user_id);
		System.out.println("====");
		
		return new User(email, guid, signup.access_token, password, firstName, signup.account.user_id);
	}
	
	public void login(User user) throws Exception {
		asGuid(user);
		
		OAuthAuthorize login = src.sync(new OAuthAuthorize.Builder()
				.username(user.email)
				.password(new Password(user.password))
				.grant_type("credentials")
				.include_account(true)
				.build());
		Assert.assertNotNull(login.access_token);
		Assert.assertEquals(login.account.email, user.email);
		Assert.assertEquals(login.account.first_name, user.firstName);
		Assert.assertEquals(login.account.user_id, user.uid);
	}
	
	public void getuser(User user) throws Exception {
		asUser(user);
		
		Getuser getuser = src.sync(new Getuser.Builder().hash("9dJDjsla49la").build());
		Assert.assertEquals(getuser.user.email, user.email);
		Assert.assertEquals(getuser.user.first_name, user.firstName);
		Assert.assertEquals(getuser.user.user_id, user.uid);
	}
	
	public void acctchange(User user) throws Exception {
		asUser(user);
		
		String newfirstName = "first";
		String newpassword = "654321";
		
		src.sync(null, new Acctchange.Builder()
				.newfirst_name(newfirstName)
				.newpassword(new Password(newpassword))
				.password(new Password(user.password))
				.time(Timestamp.now())
				.build());
		
		Account account = src.sync(new Getuser.Builder().build()).user;
		
		Assert.assertEquals(account.first_name, newfirstName);
		
		// Reset user back to default state
		src.sync(null, new Acctchange.Builder()
				.newfirst_name(user.firstName)
				.newpassword(new Password(user.password))
				.password(new Password(newpassword))
				.build());
		
		account = src.sync(new Getuser.Builder().build()).user;
		Assert.assertEquals(account.first_name, user.firstName);
	}
	
	public void addAlias(User user) throws Exception {
		asUser(user);
		
		String alias = new StringBuilder(user.email).insert(user.email.indexOf("@"), "_a").toString();
		src.sync(null, new AddAlias.Builder()
				.email(alias)
				.time(Timestamp.now())
				.build());
		
		boolean found = false;
		List<EmailAlias> aliases = src.sync(new Getuser.Builder().build()).user.aliases;
		for (EmailAlias a : aliases) {
			if (a.email.value.equals(alias)) found = true;
		}
		Assert.assertTrue(found);
	}
	
	public void deleteAlias(User user) throws Exception {
		asUser(user);
		
		String alias = new StringBuilder(user.email).insert(user.email.indexOf("@"), "_a").toString();
		src.sync(null, new DeleteAlias.Builder()
				.email(alias)
				.time(Timestamp.now())
				.build());
		
		boolean found = false;
		List<EmailAlias> aliases = src.sync(new Getuser.Builder().build()).user.aliases;
		for (EmailAlias a : aliases) {
			if (a.email.value.equals(alias)) found = true;
		}
		Assert.assertFalse(found);
	}
	
	public void setAvatar(User user) throws Exception {
		asUser(user);
		
		// TODO
	}
	
	public void add(UrlString url) throws Exception {
		src.sync(null, new Add.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().build());
		assertItem(url, get, i -> i.status == ItemStatus.UNREAD);
	}
	
	public void favorite(UrlString url) throws Exception {
		long now = System.currentTimeMillis();
		src.sync(null, new Favorite.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		System.out.println("FAVROTE " + (System.currentTimeMillis()-now));
		
		Get get = src.sync(new Get.Builder().build());
		assertItem(url, get, i -> i.favorite != null && i.favorite);
	}
	
	public void unfavorite(UrlString url) throws Exception {
		src.sync(null, new Unfavorite.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().build());
		assertItem(url, get, i -> i.favorite == null || !i.favorite);
	}
	
	public void archive(UrlString url) throws Exception {
		src.sync(null, new Archive.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.ARCHIVE).build());
		assertItem(url, get, i -> i.status == ItemStatus.ARCHIVED);
	}
	
	public void readd(UrlString url) throws Exception {
		src.sync(null, new Readd.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.UNREAD).build());
		assertItem(url, get, i -> i.status == ItemStatus.UNREAD);
	}
	
	public void delete(UrlString url) throws Exception {
		src.sync(null, new Delete.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		List<Item> myList = src.sync(new Get.Builder().state(ItemStatusKey.ALL).build()).list;
		for (Item i : myList) {
			if (i.given_url.equals(url)) Assert.fail();
		}
	}
	
	public void undo_delete(UrlString url) throws Exception {
		src.sync(null, new UndoDelete.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.UNREAD).build());
		assertItem(url, get, i -> i.status == ItemStatus.UNREAD);
	}
	
	public void undo_archive(UrlString url) throws Exception {
		src.sync(null, new UndoArchive.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.UNREAD).build());
		assertItem(url, get, i -> i.status == ItemStatus.UNREAD);
	}
	
	public void tags_add(UrlString url, String... tags) throws Exception {
		src.sync(null, new TagsAdd.Builder()
				.time(Timestamp.now())
				.tags(Arrays.asList(tags))
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.ALL).include_item_tags(1).taglist(1).build());
		Assert.assertTrue(get.tags.containsAll(Arrays.asList(tags)));
		assertItem(url, get, i -> i.tags.containsAll(asTagsList(tags)));
	}
	
	public void tags_replace(UrlString url, String... tags) throws Exception {
		src.sync(null, new TagsReplace.Builder()
				.time(Timestamp.now())
				.tags(Arrays.asList(tags))
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.ALL).include_item_tags(1).taglist(1).build());
		assertItem(url, get, i -> tags.length == i.tags.size() && i.tags.containsAll(asTagsList(tags)));
	}
	
	public void tags_remove(UrlString url, String... tags) throws Exception {
		src.sync(null, new TagsRemove.Builder()
				.time(Timestamp.now())
				.tags(Arrays.asList(tags))
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.ALL).include_item_tags(1).build());
		assertItem(url, get, i -> {
			for (Tag t : i.tags) {
				if (ArrayUtils.contains(tags, t.tag)) return false;
			}
			return true;
		});
	}
	
	public void tags_clear(UrlString url) throws Exception {
		src.sync(null, new TagsClear.Builder()
				.time(Timestamp.now())
				.url(url)
				.build());
		
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.ALL).include_item_tags(1).taglist(1).build());
		assertItem(url, get, i -> i.tags == null || i.tags.isEmpty());
	}
	
	public void tag_delete(String tag) throws Exception {
		src.sync(null, new TagDelete.Builder()
				.time(Timestamp.now())
				.tag(tag)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.ALL).include_item_tags(1).taglist(1).build());
		
		Assert.assertFalse(get.tags.contains(tag));
		
		Assert.assertTrue(
				src.sync(new Get.Builder()
				.state(ItemStatusKey.ALL)
				.tag(tag)
				.build())
				.list.isEmpty());
	}
	
	public void tag_rename(String from, String to) throws Exception {
		src.sync(null, new TagRename.Builder()
				.time(Timestamp.now())
				.old_tag(from)
				.new_tag(to)
				.build());
		
		Get get = src.sync(new Get.Builder().state(ItemStatusKey.ALL).include_item_tags(1).taglist(1).build());
		
		Assert.assertFalse(get.tags.contains(from));
		Assert.assertTrue(get.tags.contains(to));
		
		Assert.assertTrue(
				src.sync(new Get.Builder()
						.state(ItemStatusKey.ALL)
						.tag(from)
						.build())
						.list.isEmpty());
		
		Assert.assertFalse(
				src.sync(new Get.Builder()
						.state(ItemStatusKey.ALL)
						.tag(to)
						.build())
						.list.isEmpty());
	}
	
	public void add_annotation(UrlString url, Annotation annotation) throws Exception {
		src.sync(null, new AddAnnotation.Builder()
				.annotation_id(annotation.annotation_id)
				.annotation(annotation)
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().annotations(1).build());
		assertItem(url, get, i -> i.annotations.contains(annotation));
		
		Get annotations = src.sync(new Get.Builder().hasAnnotations(true).sort(ItemSortKey.ANNOTATION).count(1).offset(0).build());
		assertItem(url, annotations.list, i -> i.annotations.contains(annotation));
		Assert.assertEquals(annotations.list.get(0).annotations.get(0).annotation_id, annotation.annotation_id);
	}
	
	public void delete_annotation(UrlString url, Annotation annotation) throws Exception {
		src.sync(null, new DeleteAnnotation.Builder()
				.annotation_id(annotation.annotation_id)
				.time(Timestamp.now())
				.url(url)
				.build());
		
		Get get = src.sync(new Get.Builder().annotations(1).build());
		assertItem(url, get, i -> i.annotations == null || !i.annotations.contains(annotation));
		
		Get annotations = src.sync(new Get.Builder().hasAnnotations(true).sort(ItemSortKey.ANNOTATION).count(1).offset(0).build());
		assertEmpty(annotations.list);
	}
	
	public void scrolled(UrlString url) throws Exception {
		Timestamp now = Timestamp.now();
		
		Position article = new Position.Builder()
				.node_index(4)
				.page(1)
				.percent(50)
				.section(0)
				.time_spent(14)
				.view(PositionType.ARTICLE)
				.build();
		
		Position web = new Position.Builder()
				.node_index(0)
				.page(0)
				.percent(45)
				.section(0) 
				.time_spent(14)
				.view(PositionType.WEB)
				.build();
		
		src.sync(null, new Scrolled.Builder()
						.node_index(article.node_index)
						.page(article.page)
						.percent(article.percent)
						.section(article.section)
						.time_spent(article.time_spent)
						.time_updated(now)
						.view(article.view)
						.time(now)
						.url(url)
						.build(),
				new Scrolled.Builder()
						.node_index(web.node_index)
						.page(web.page)
						.percent(web.percent)
						.section(web.section)
						.time_spent(web.time_spent)
						.time_updated(now)
						.view(web.view)
						.time(now)
						.url(url)
						.build());
		
		Get get = src.sync(new Get.Builder().positions(1).build());
		assertItem(url, get, i -> i.positions.get(article.view.toString()).equals(article) && i.positions.get(web.view.toString()).equals(web));
	}
	
	private void assertItem(UrlString url, Get get, ItemAssertion ia) throws SyncException {
		assertItem(url, get.list, ia);
	}
	
	private void assertItem(UrlString url, List<Item> items, ItemAssertion ia) throws SyncException {
		Assert.assertTrue(ia.itemMeetsExpectation(findItem(items, url)));
	}
	
	private Item findItem(List<Item> items, UrlString url)  {
		for (Item i : items) {
			if (i.given_url.equals(url)) {
				return i;
			}
		}
		Assert.fail("item not found");
		return null;
	}
	
	interface ItemAssertion {
		boolean itemMeetsExpectation(Item item);
	}
	
	private static List<Tag> asTagsList(String... tags) {
		List<Tag> list = new ArrayList<>(tags.length);
		for (String tag : tags) {
			list.add(new Tag.Builder().tag(tag).build());
		}
		return list;
	}
	
	public void follow_user(User follow) throws SyncException {
	
	}
	
	
}