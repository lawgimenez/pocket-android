package com.pocket.sdk.api.thing;

import com.pocket.sdk.api.generated.enums.VideoType;
import com.pocket.sdk.api.generated.thing.Video;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tools for working with {@link Video}
 */
public class VideoUtil {
	
	private static Pattern sYouTubePattern;
	private static Pattern sVimeoPattern;
		
	public static Video convertUrl(String url) {
		if (url == null) {
			return null;
		}
		Matcher youtube = getYouTubePattern().matcher(url);
		if (youtube.find()) {
			String vid = youtube.group(3);
			return new Video.Builder()
					.src(makeYouTubeUrl(vid))
					.video_id(1)
					.vid(vid)
					.type(VideoType.YOUTUBE)
					.build();
		}
		Matcher vimeo = getVimeoPattern().matcher(url);
		if (vimeo.find()) {
			return new Video.Builder()
					.src(url)
					.video_id(1)
					.vid(vimeo.group(2))
					.type(VideoType.VIMEO_LINK)
					.build();
		}
		return null;
	}
		
	/**
	 * If this video is {@link VideoType#IFRAME}, this method will check to see if the iframe source ({@link Video#type}) is actually pointing to a known format
	 * like youtube or vimeo, and if so, it will upgrade this {@link VideoUtil} to the more specific type.
	 * <p>
	 * If it was able to change to a better type, the returned instance will have a different {@link Video#type} and {@link Video#vid}.
	 */
	public static Video upgradeType(Video video) {
		Video converted = video.type == VideoType.IFRAME ? convertUrl(video.src) : null;
		if (converted != null) {
			return video.builder()
					.vid(converted.vid)
					.type(converted.type)
					.build();
		} else {
			return video;
		}
	}
	
	private static Pattern getYouTubePattern() { // FINISH support youtu.be and vnd.youtube: in regex
		if (sYouTubePattern == null) {
			// Matches youtube's /embed/, /watch?v=, and /v/ formats
			sYouTubePattern = Pattern.compile("(?i)youtube.([a-z]{1,10})(.*?/watch\\?.*v=|/v/|/embed/)([a-z0-9\\_-]*)", Pattern.CASE_INSENSITIVE);
		}
		return sYouTubePattern;
	}
	
	private static Pattern getVimeoPattern() {
		if (sVimeoPattern == null) {
			sVimeoPattern = Pattern.compile("(?i)vimeo.com/(m/)?([0-9]*)", Pattern.CASE_INSENSITIVE);
		}
		return sVimeoPattern;
	}

	public static String makeYouTubeUrl(String id){
		return "http://www.youtube.com/watch?v=".concat(id);
	}
	
}
