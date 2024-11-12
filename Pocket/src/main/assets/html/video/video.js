var VIDEO_YOUTUBE = 1;
var VIDEO_VIMEO_LINK = 2;
var VIDEO_VIMEO_MOOGALOOP = 3;
var VIDEO_VIMEO_IFRAME = 4;
var VIDEO_HTML5 = 5;
var VIDEO_FLASH = 6;
var VIDEO_IFRAME = 7;
var VIDEO_BRIGHTCOVE = 8;

var isYoutubeApiReady = false;
var isYoutubeApiInstalling = false;
var pendingYoutubeVideos = [];
var PocketAndroidVideoInterface = null;

var VideoPlayer = Class.extend({

	/**
	*	Create a new VideoPlayer. If flash is to be supported swfobject.js is required to be imported in the <head>
	*
	*	To style, include video.css and to modify, you can override those styles.
	*
	*	To use, call setDisplaySize(width,height) and load() to setup. You can also call setOnReadyListener() if you want
	*	to receive a callback when the player is loaded and ready to use.
	*
	*	v: The video object containing info about the video.
	*	elementId: The ID of the element that this video should be inserted into.
	*/
	init : function(v, elementId) {
		var self = this;
		
		this.video = v;
		
		this.embedParentId = elementId;
		this.embedParent = $("#" + elementId);
		this.embedParent.addClass("video_box");
		
		this.placeholder = $("<div>&nbsp;</div>"); // Just an empty placeholder to hold the space until the real embed is loaded.
		this.embedParent.append(this.placeholder);
		
		// Declaring other instance vars
		this.embed = false; // The $element that is the video embed
		this.onReadyListener = false;
		this.onResizeListener = false;
		this.sourceRatio; // The width/height ratio of the source video size. May be 0 while it is unknown. In most cases, should be set by the time onEmbedReady is invoked.
		this.displaySizeRequested; // The requested size from setDisplaySize()
		
		if (this.video.width > 0 && this.video.height > 0) {
			if (this.video.width * 2 < this.video.height) {
				// This is weirdly shaped... let's ignore parser provided size ratio for now to avoid weird layout issues.
			} else {
				this.setSourceRatio(this.video.width / this.video.height);
			}
		}
	},
	
	/**
	* Sets the bounding area for the video to fit into. The video will fit into this space. At least one dimension will match.
	* The other dimension may be adjusted to match the ratio of the video size.
	*
	* The VideoPlayer instance is returned for chaining init calls.
	*/
	setDisplaySize : function(width, height) {
		this.displaySizeRequested = {width:width, height:height};
		this.onDimensionsChanged();
		return this;
	},
	
	/**
	* Sets a callback to be invoked when the video is loaded and ready to play.
	*
	* It will callback with one parameter called "controller" which will allow you to control the video player.
	* To see the available methods see the VideoVideoControllerTemplate.
	*
	* Note: This is currently under development and may return null if the video type doesn't support it yet.
	*		Also, the controllers may not support all of the methods yet either.
	*
	* The VideoPlayer instance is returned for chaining init calls.
	*/
	setOnReadyListener : function(onReadyListener) {
		this.onReadyListener = onReadyListener;
		return this;
	},
	
	/**
	* Sets a callback to be invoked when the video changes its size. Invoked with (width, height) as parameters.
	*
	* The VideoPlayer instance is returned for chaining init calls.
	*/
	setOnResizeListener : function(onResizeListener) {
		this.onResizeListener = onResizeListener;
		return this;
	},
	
	/**
	* Start loading the video.
	*
	* The VideoPlayer instance is returned for chaining init calls.
	*/
	load : function() {
		if (this.isOnline()) {
			this.loadEmbed();
			
		} else {
			// Show play icon and wait for a tap/click to retry or display error.
			 
			var self = this;
			this.embedParent.toggleClass("video_box_play_icon", true);
			
			this.embedParent.click(function() {
				if (self.isOnline()) {
					// Retry load
					self.embedParent.unbind('click'); // Remove this click handler for now to avoid duplicates added later.
					self.embedParent.toggleClass("video_box_play_icon", false);
					self.load();
					
				} else {
					// Display offline video error message to user
					self.showOfflineVideoDialog();
				}
			});
		}
		return this;
	},
	
	/** Private. Loads the correct video player based on type */
	loadEmbed : function() {
		var type = this.video.type;
		if (type == VIDEO_YOUTUBE) {
			this.loadYoutubeEmbed();	
			
		} else if (type == VIDEO_VIMEO_LINK || type == VIDEO_VIMEO_IFRAME || type == VIDEO_VIMEO_MOOGALOOP) {
			this.loadVimeoEmbed();
		
		} else if (type == VIDEO_HTML5) {
			this.loadHTML5Embed();
		
		} else if (type == VIDEO_FLASH) {
			this.loadFlashEmbed();
			
		} else if (type == VIDEO_IFRAME || type == VIDEO_BRIGHTCOVE) {
			this.loadIFrameEmbed(); 
			
		} else {
			console.log("unknown type " + type);
		}
	},
	
	/** Private. Installs the Youtube API and invokes onYouTubeIframeAPIReady() when it is setup. */
	installYoutubeApi : function() {
		if (isYoutubeApiReady || isYoutubeApiInstalling) {
			return; // Don't need to install it
		}
		isYoutubeApiInstalling = true;
		var tag = document.createElement('script');
		tag.src = "https://www.youtube.com/iframe_api";
		var firstScriptTag = document.getElementsByTagName('script')[0];
		firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);
	},
	
	/** Private. Loads the video in a Youtube player */
	loadYoutubeEmbed : function() {
		var self = this;
		if (!isYoutubeApiReady) {
			if (!isYoutubeApiInstalling) {
				self.installYoutubeApi();
			}
			pendingYoutubeVideos.push(this);
			return; // Will retry the load after the Youtube API is loaded.
		}
		
		// Youtube API is loaded and ready to use.
		
		var id = this.embedParentId+"_typlayer";
		this.embed = $('<div></div>') // The player will replace this, it is only temporary.
			.attr("id", id);
		
		// Create a temporary div to wrap the iframe so we can hide it while it is loading. Otherwise it breaks out of the parent layout as it loads.	
		var iframeWrapper = $('<div></div>');
		iframeWrapper.append(this.embed);
		this.embedParent.append(iframeWrapper);
		iframeWrapper.css('visibility', 'hidden');
		
		var youtubePlayer = new YT.Player(id, {
			videoId: self.video.vid,
			playerVars: {
	            showinfo: 0 ,
	            modestbranding: 1,
	            theme: (PocketAndroidVideoInterface != null && PocketAndroidVideoInterface.isDarkTheme()) ? "dark" : "light",
	            controls: 2
	        },
			events: {
		    	'onReady': function() {
		    		iframeWrapper.css('visibility', 'visible');
		    		self.embed = $("#" + id); // The player replaced our placeholder div with the actual embed, so get the new element.
		    		self.setSourceRatio(self.embed.width() / self.embed.height());
					self.onEmbedReady(new YoutubeController(youtubePlayer));
		    	}
			}
		});
	},
	
	/** Private. Loads the video in a Vimeo player */
	loadVimeoEmbed : function () {
		var video = document.createElement('iframe');
        video.setAttribute('width', this.displaySizeRequested.width);
        video.setAttribute('height', this.displaySizeRequested.height);
        video.setAttribute('frameborder', 0);
        video.setAttribute('src', "http://player.vimeo.com/video/" + this.video.vid + "?title=0&amp;byline=0&amp;portrait=0");

		this.embed = $(video.outerHTML);
		this.embedParent.append(this.embed);
		
		/*
		* Work around for an issue with HTML5 Vimeo on Android.
		* Android Honeycomb+ plays html5 video inline, but will expand the video to fill 100% width and height.
		* So as soon as the iframe has loaded, we pass the content of the iframe up to Android to parse.
		* Then it will call back down and size the iframe to the correct dimensions. 
		*/
		var self = this;
		this.embed.on('load', function() {

		    if (PocketAndroidVideoInterface != null) {
                var content;
                try {
                    var doc = self.embed[0].contentDocument || self.embed[0].contentWindow.document;
                    content = doc.body.innerHTML;
                } catch (e) {
                    console.log("error fetching iframe contents " + e);
                }
                if (content) {
                    var response = jQuery.parseJSON(PocketAndroidVideoInterface.onHTML5VideoReady(content, self.video.type));
                    if (response) {
                        self.setSourceRatio(response.width / response.height);
                    }
                }
            }
			// If we can't obtain the source ratio it will just use the displaySize as is.
			
			self.onEmbedReady();
		});
	},

	/** Private. Loads the video in a HTML5 player */
	loadHTML5Embed : function () {
		var video = document.createElement('video');
        video.setAttribute('width', this.displaySizeRequested.width);
        video.setAttribute('height', this.displaySizeRequested.height);
        video.setAttribute('controls', '');
        if (this.autoplay) {
            video.setAttribute('autoplay', '');
        }
        var src = document.createElement('source');
        src.setAttribute('src', this.video.src);
        video.innerHTML = src.outerHTML;

		this.embed = $(video.outerHTML);
		this.embedParent.append(this.embed);
		
		this.onEmbedReady();
	},
	
	/** Private. Loads the video in a Flash player. If flash is not supported, the video box will show a play icon and display an message when tapped.  */
	loadFlashEmbed : function () {
		if (!swfobject) {
			// The swfobject.js isn't loaded. Flash support must not be expected.
			this.onFlashNotSupported();
			return;
		}
	
		var self = this;
		var width = this.displaySizeRequested.width;
		var height = this.displaySizeRequested.height;
		var src = this.video.src;
		
		var embedId = "FLASH_VIDEO_" + this.video.id;
		this.embed = $("<div id='" + embedId + "'></div>");
		this.embedParent.append(this.embed);
		
		var params = { allowScriptAccess: "always" };
		swfobject.embedSWF(src, embedId, width, height, "8", null, null, params, null, function(e) {
			if (e && e.success) {
				self.onEmbedReady();
				
			} else {
				// Flash not available, just show play button and have the click display a message.
				self.onFlashNotSupported();
			}
		});
	},
	
	onFlashNotSupported : function () {
		this.embedParent.toggleClass("video_box_play_icon", true);
		if (PocketAndroidVideoInterface != null) {
            this.embedParent.click(function() {
                PocketAndroidVideoInterface.showFlashVideoNotSupportedDialog();
            });
        }
	},
	
	/** Private. Loads the video in an iFrame */
	loadIFrameEmbed : function () {
		var video = document.createElement('iframe');
        video.setAttribute('width', this.displaySizeRequested.width);
        video.setAttribute('height', this.displaySizeRequested.height);
        video.setAttribute('frameborder', 0);
        video.setAttribute('src', this.video.src);
		
		this.embed = $(video.outerHTML);
		this.embedParent.append(this.embed);
		
		this.onEmbedReady();
	},
	
	/** Private. Invoke when the video player has been loaded and is ready to play.
	*
	*	controller: An video player specific instance resembling VideoControllerTemplate.
	 */
	onEmbedReady : function (controller) {
		if (this.onReadyListener) { 
			this.onReadyListener(controller);
		}
		this.embedParent.toggleClass("video_box_playing", true);
		this.embedParent.toggleClass("video_box_play_icon", false);
		this.placeholder.hide();
		this.onDimensionsChanged();
	},
	
	/* Private. Set the width/height ratio for the source video. */
	setSourceRatio : function(ratio) {
		this.sourceRatio = ratio;
		this.onDimensionsChanged();
	},
	
	/** Private. Updates the video to make sure it fits within all of the various bounds and limitations correctly. Call this when things change that affect the video size */
	onDimensionsChanged : function() {
		if (!this.displaySizeRequested) {
			return; // No size set yet.
		}
	
		var width = this.displaySizeRequested.width;
		var height = this.displaySizeRequested.height;
		
		var sourceRatio = this.sourceRatio;
		var displayRatio = width / height;
		
		if (sourceRatio > 0) {
			// Adjust video to fit within bounds
			
			// First fill width and adjust height to match ratio.
			var adjustedHeight = width / sourceRatio;
			if (adjustedHeight <= height) {
				// Fits
				height = adjustedHeight;
			} else {
				// Doesn't fit into bounds by fitting width, use requested height and scale width to match ratio
				width = height * sourceRatio;
			}
		}	
			
		var view; // Either the actual embed or the placeholder element
		if (this.embed) {
			// There is an embed available, size that and hide any visible placeholder.
			view = this.embed;
			this.placeholder.hide();
			
		} else {
			// Embed isn't loaded yet, so just size the placeholder
			view = this.placeholder;
		}
		
		if (view.width() == width && view.height() == height) {
			return; // No change, ignore.
		}
		
		// Set the new dimensions
		view.width(width);
		view.height(height);
		
		this.embedParent.width(width);
		this.embedParent.height(height);
		
		if (this.onResizeListener) {
			this.onResizeListener(width, height);
		}
	},
	
	/** Remove this video from the DOM and stop playback */
	kill : function() { // TODO still needed?
		this.embedParent.html('');
		// TODO should also cancel anything pending or at least have those things fail gracefully when they return.
	},
	
	/** Returns true if the Android device is connected to the internet */
	isOnline : function() {
		if (PocketAndroidVideoInterface != null) {
		    return PocketAndroidVideoInterface.isConnected();
        } else {
            return true;
        }
	},
	
	/** Displays a native Android dialog explaining why videos are not available while offline. */
	showOfflineVideoDialog : function() {
		if (PocketAndroidVideoInterface != null) {
		    PocketAndroidVideoInterface.showOfflineVideoDialog();
		}
	}
	
});

/**
* This is a template for a controller returned in the callback set through VideoPlayer.setOnReadyListener.
* All video types should try to support these methods if possible.
*/
var VideoControllerTemplate = Class.extend({

	init : function() {
		
	},
	
	play : function() {
		// Play the video
	},
	
	pause : function() {
		// Pause the video
	},
	
	setFullscreen : function(fullscreen) {
		if (fullscreen) {
			// Go fullscreen
		} else {
			// Come out of fullscreen if it is
		} 
	},
	
	getPosition : function() {
		return 0; // Return the current playback position in seconds 
	},
	
	getLength : function() {
		return 0; // Return the length of the video in seconds
	}
	
});

var YoutubeController = Class.extend({

	init : function(youtubePlayer) {
		this.youtubePlayer = youtubePlayer;
	},
	
	play : function() {
		this.youtubePlayer.playVideo();
	},
	
	pause : function() {
		this.youtubePlayer.pauseVideo();
	},
	
	setFullscreen : function(fullscreen) {
		if (fullscreen) {
			// TODO Go fullscreen
		} else {
			// TODO Come out of fullscreen if it is
		} 
	},
	
	getPosition : function() {
		return 0; // TODO Return the current playback position in seconds 
	},
	
	getLength : function() {
		return 0; // TODO Return the length of the video in seconds
	}
	
});

function onYouTubeIframeAPIReady() {
	isYoutubeApiReady = true;
	var len = window.pendingYoutubeVideos.length;
	for (var i = 0; i < len; i++) {
		window.pendingYoutubeVideos[i].loadEmbed(); // Retry the load
	}
	window.pendingYoutubeVideos = []; // Clear
}
