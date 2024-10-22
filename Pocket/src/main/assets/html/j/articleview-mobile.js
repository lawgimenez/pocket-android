var isAndroid;


var ArticleView = Class.extend(
{
	init : function()
	{		
		this.currentPosition = {};
		
		// Android comm object
		if (typeof PocketAndroidArticleInterface == 'undefined')
			PocketAndroidArticleInterface = false;
			
		isAndroid = !!PocketAndroidArticleInterface;
		
		this.debug = false;
		this.videosLoaded = {};
		this.images = {};
		//this.debug = true; //should be commented out in production
		
	},

	escapefunn : function(incoming, logPrefix) {
	    var verboseLog = logPrefix.length > 0;
	    var value;
	    if (incoming === Object(incoming)) {
	        value = incoming.s;
	        if (verboseLog) {
	            console.log(logPrefix + "obj:" + JSON.stringify(incoming));
	        }
	    } else {
	        value = incoming;
	    }
	    if (verboseLog) {
	        console.log(logPrefix + "len:" + value.length + " val:" + value);
	    }
	    return value.length;
	},

	// native is ready and passes down settings to initialize
	load : function(fontSize, fontType, textAlign, textStyle, lineHeight, scale, formFactor, apiLevel)
	{			
		if (this.loadCalled)
			return;
			
		this.loadCalled = true;
	
		var self = this;
	
		// load the environment settings
		this.scale = scale;
		this.isTablet = formFactor != "phone";
		this.formFactorKey = formFactor;
		if (apiLevel >= 14) {
			$(document.body).toggleClass("isAtLeastICS", true);
		}
		
		this.pagingInsets = {
			topInner : this.isTablet ? 100 : 13,
			btmInner : this.isTablet ? 60 : 13,
		}
		
		// Android setup code
		if (isAndroid)
		{
			// in addition to 'on scroll' fire scroll event every half second
			setInterval(function(){self.scrolled()}, 500);	
            
            // watch for scroll
			document.addEventListener('scroll', function(){self.scrolled()}, false);
		}
		
		// load text settings
		this.newFontSize(fontSize);
		this.newFontType(fontType);
		this.newTextAlign(textAlign);
		this.newLineHeightSetting(lineHeight);
		
		document.body.style.transition = "none"; // prevent animating from white in dark theme
		this.newTextStyle(textStyle);
		this.flushCss(document.body)
		document.body.style.transition = ""; // restore transition

		// Native will call loadCallback or loadHadError when the article data is ready.
	},
	
	flushCss : function(element) {
	  // By reading the offsetHeight property, we are forcing
	  // the browser to flush the pending CSS changes (which it
	  // does to ensure the value obtained is accurate).
	  element.offsetHeight;
	},
		
	// article content was retrieved
	loadCallback : function(content)
	{
		// TODO : 3.0 : If file was missing, handle that correctly
			
		$(document.body).html(content);
		
		// Add empty content to bottom in case an image is at bottom. Otherwise pagination doesn't work on the last page
		$(document.body).append($('<br>&nbsp;'))

		this.footer = $('<div id="article_footer"></div>');
		$(document.body).append(this.footer);
		
		this.onArticleReady();
	},
	
	loadHadError : function()
	{
		PocketAndroidArticleInterface.onError();
	},
	
	// tell native that we're ready and can accept images and videos or whatever it wants to do with us
	onArticleReady : function()
	{
		var self = this;
		
		///// Separate the date and author information
		
		// pull the date out of the cite
		var date = $('.RIL_date');
		date.detach();
		
		// wrap the cite in a span
		var cite = $('#RIL_header cite');
		cite.wrapInner('<span class="ril_byline_content"></span>');
		
		// put the date back in after the span
		cite.append(date);
		
		//////
		
		// Update margins and line heights
		this.adjustDisplayBasedOnFontSize();
			
		// update galleries
		var galleries = document.getElementsByAttribute('isGallery', '1');
		for(var i=0; i<galleries.length; i++)
		{
			var clearer = document.createElement('div');
			clearer.className = 'RIL_clear';
			galleries[i].appendChild(clearer);
		}
		
		// Create gestures to detect fullscreen tap and paging actions
		var gesture = new window.JSTapGestureRecognizer();
		gesture.initWithCallback(this.viewWasTapped);
		gesture.moveTolerance = 5;
		gesture.maximumPressDuration = 300;
		
		var target = new window.JSGestureView(document.body);
		target.addGestureRecognizer(gesture);
		
		// Hack to allow 'full web page view' link in old article views open the web view, this needs to be improved
		$('#RIL_ARTICLE_NOTE a').each(function()
		{
			if (this.href == 'ISRIL:WEB' || this.textContent == 'Full Web Page view')
			{
				var self2 = self;
				this.href = '';
				this.setAttribute('href', '');
				$(this).click(function()
				{
					PocketAndroidArticleInterface.setViewType(2);
					return false;
				});
			}
		});
	
		// make the content visible (now that it has been styled)
		document.body.style.visibility = 'visible';
		
		this.updatePageSwipingDisabledAreas();
		
		$(document.body).addClass("form_factor_" + this.formFactorKey);
		
		PocketAndroidArticleInterface.onReady();
		
		this.onMarginsChanged();
		
		$(window).scroll(function()
		{
			if (self.lastFlipDirection)
			{	
				// we need a timeout because of a race condition in android where the webview internals
				// may have the wrong height when the pagebar is viewable. windowSize will report an old value
				// and the caret position check will likely fail.
				var self_ = self;
				setTimeout(function()
				{
					self_.didFlipPage(true, self_.lastFlipDirection);
					self_.lastFlipDirection = false;
				},50);
				
				clearTimeout(self.safeTO);
			}
		});
		
		this.isReady = true;
	},
	
	// -- //
	
	viewWasTapped : function(gesture)
	{		
		// this = context of gesture recognizer
		if (gesture.state == JSGestureRecognizerStateRecognized) 
		{
			// don't trigger while text selection/highlighting is occuring
			var selection = window.getSelection();
			var selectedText = selection.toString(); 
			if (selectedText && selectedText.length)
			{
				// if the selection is off screen, just ignore it otherwise it'll appear as if fullscreen is no longer working
		    	var range = selection.getRangeAt(0); 
			    var rect = getBoundingClientRectOfRange(range);
			    
			    var viewPortHeight = windowSize()[1];
			    
			    // either the whole thing fits or part of it on screen
			    if ( 
			    	(rect.top >= 0 && rect.top <= viewPortHeight)
			    	||
			    	(rect.bottom >= 0 && rect.bottom <= viewPortHeight)
			        )
			    {
			    	return;
			    }
			}
				
		
			var touch = gesture.translationOrigin;
			var size = windowSize();
			
			var tappedOnElement = elementAtPoint(touch.x,touch.y-scrollY());
			
			if (tappedOnElement && (tappedOnElement.nodeName == 'A' || tappedOnElement.nodeName == 'IMG'))
			{
				return; // don't trigger on links or images
			}
					
			
			// Determine what type of tap this was (center is fullscreen, left/right edges are paging

			// Fullscreen
	        if (touch.x >= size[0]*0.20 && touch.x <= size[0]*0.80)
	        	PocketAndroidArticleInterface.toggleFullscreen();
	        	
	       	/* 
	       	else if (touch.x < size[0]*0.25)
	        	setTimeout(function(){ article.goPreviousPage(); }, 1); // The timeouts prevent a race condition with native touch detection. (Without them, if a link appears in the place of the tap on the next page it will get 'clicked'
				
			else if (touch.x > size[0]*0.75)
				setTimeout(function(){ article.goNextPage(); }, 1);
			*/
		}
	},

	highlightAnnotations : function(annotations) {
		var len = annotations.length;
		pktHighlighter.removeAllHighlights();
		for (var i = 0; i < len; i++) {
			pktHighlighter.highlightAnnotation(annotations[i]);
		}
	},
	
	scrollToAnnotation : function(annotation_id, yInset) {
		var y = $(".highlight[rel=" + annotation_id + "]").offset().top;
		window.scrollTo(0, y-15+yInset);
		PocketAndroidArticleInterface.scrollToPosition(y);
	},

	clearSearchText : function() {
	    $('body').removeSearchTermHighlights();
	},

	searchForText : function(text) {
	    $('body').removeSearchTermHighlights();
	    $('body').highlightSearchTerm(text);
        var count = $('body').find("span.text_search_highlight").length;
	    PocketAndroidArticleInterface.onTextSearch(count);
	},

	scrollToSearchText : function(text, index) {
	    var value = $('body').find("span.text_search_highlight")[index - 1];
	    var position = value.offsetTop;
        PocketAndroidArticleInterface.scrollToPosition(position);
        $('body').setCurrentSelection(index - 1);
    },
	
	smoothScrollToAnnotation : function(annotation, yInset) {
		var y = $(".highlight[rel=" + annotation.annotation_id + "]").offset().top;
		$('html,body').animate({scrollTop: y-15+yInset}, 333);
	},
	
	requestAnnotationPatch : function() {
		var wholeThing = document.createRange();
		wholeThing.selectNodeContents(document.body);
		var originalText = wholeThing.toString();
		
		var selection = window.getSelection().getRangeAt(0);
		var before = document.createRange();
		before.setStart(wholeThing.startContainer, wholeThing.startOffset);
		before.setEnd(selection.startContainer, selection.startOffset);
		var after = document.createRange();
		after.setStart(selection.endContainer, selection.endOffset);
		after.setEnd(wholeThing.endContainer, wholeThing.endOffset);
		var markedUpText = before.toString() +
				"<pkt_tag_annotation>" + selection.toString() + "</pkt_tag_annotation>" +
				after.toString();
		
		var dmp = new diff_match_patch();
		var patch = dmp.patch_toText(dmp.patch_make(originalText, markedUpText));
		PocketAndroidArticleInterface.onRequestedHighlightPatch(patch, selection.toString());
	},
	
	swipedLeft : function()
	{
		this.goNextPage();
	},
	
	swipedRight : function()
	{
		this.goPreviousPage();
	},
	
	isElementAtPointScrollable: function(x, y)
	{
		var element = elementAtPoint(x,y);
		if (!element)
			return false;
		
		var targetPRE;
		if (element.nodeName == 'PRE')
			targetPRE = element;
		else
		{
			var parent = $(element).parents('pre');
			if (parent && parent.length)
				targetPRE = parent[0];
		}
		
		if (!targetPRE)
			return false;

		var scrollWidth = targetPRE.scrollWidth || 0;
		return scrollWidth > $("#RIL_body").width();
	},
	
	/**
	* Page swipes/flips should not trigger if started from within an element
	* that is horizontally scrollable. Otherwise when the user tries to scroll the content
	* it will unintentionally trigger a page flip.
	*
	* To prevent that, this method finds all of these elements and passes their locations up to Java's 
	* swipe gesture detector.
	*
	* This method should be invoked any time something on the page changes that could cause the location of these
	* elements to shift their absolute position on the page.
	*
	*/
	updatePageSwipingDisabledAreas : function()
	{
		var results = [];
		
		var pres = $('pre');
		var lastIndex = pres.length - 1;
		pres.each(function(index, element) {
			if ((element.scrollWidth || 0) <= $("#RIL_body").width()) {
				// Not scrollable
				
			} else {
				var e = $(this);
				var off = e.offset();
				results.push({
					top: off.top,
					bottom: off.top + e.height()
				});
			}
			
			if (index == lastIndex) {
				PocketAndroidArticleInterface.updatePageSwipingDisabledAreas(JSON.stringify(results));
			}
		});
	},

	changedFullscreen : function(isFullscreen)
	{
		this.isFullscreen = isFullscreen;
		
		if (this.paging)
			this.updatePaging();
	},
	
	updateMaxViewHeight : function(h)
	{
		this._maxWebViewHeight = h;
	},
	
	// -- //
	
	topPagingPadding : function(min)
	{
		// Android's webview resizes on fullscreen so there is no inset to account for
		if (isAndroid)
			return this.pagingInsets.topInner;
			
		return (this.isFullscreen && !min ? 0 : (this.isTablet?0:20)) + this.pagingInsets.topInner;
	},
	
	btmPagingPadding : function(min)
	{
		// Android's webview resizes on fullscreen so there is no inset to account for
		if (isAndroid)
			return this.pagingInsets.btmInner;
			
		return (this.isFullscreen && !min ? 0 : (this.isTablet?0:44)) + this.pagingInsets.btmInner;
	},
	
	maxWebViewHeight : function()
	{
		return this._maxWebViewHeight ? this._maxWebViewHeight : (windowSize())[1];
	},
	
	pageHeight : function(min)
	{
		return this.maxWebViewHeight() - this.topPagingPadding(min) - this.btmPagingPadding(min);
	},
	
	// --- //
	
	setPageMode : function(on)
	{		
		//this.log('setPageMode ' + on);
		
		if (!this.isReady)
			return;
	
		var self = this;
		
		var didChange = !!this.paging != !!on;
		this.paging = on;
				
		if (didChange)
		{
			// we need to add extra padding to the bottom of the page so we can view the last page
			if (!this.lastPageSpacer)
			{
				this.lastPageSpacer = $('<div></div>');
				this.lastPageSpacer.css('pointer-events','none');
				$('body').append(this.lastPageSpacer);
			}

			var lastPageSize;
			
			if (this.paging)
			{
				lastPageSize = (windowSize())[1];
				this.lastPageSpacer.css({
					display : 'block',
					height  : lastPageSize
				});
			}	
			else {
				this.lastPageSpacer.css('display', 'none');
				lastPageSize = 0;
			}

			// look for text highlight changes while paging
			clearInterval(this.highlightTO);
			if (this.paging)
			{
				this.highlightTO = setInterval(function()
				{
					self.selectionHasChanged();
				}, 250);
			}
			
			PocketAndroidArticleInterface.setIsPaging(this.paging, lastPageSize);
		}
		
		if (!this.topPageBlocker)
		{
			this.topPageBlocker = $('<div class="page_blocker"></div>');
			this.bottomPageBlocker = $('<div class="page_blocker"></div>');
			
			if (!isAndroid)
			{
				$('body').append(this.topPageBlocker).append(this.bottomPageBlocker);
				
				this.topPageBlocker[0].addEventListener("webkitTransitionEnd", function(){ self.updateBlockerClass(); }, true);
				
				if (!this.setBlockerClassBefore)
					this.updateBlockerClass();
					
			}
		}
		
		this.topPageBlocker.toggleClass('hide', !this.paging);
		this.bottomPageBlocker.toggleClass('hide', !this.paging);
			
		if (this.paging)
		{
			this.updatePaging(true, false, didChange);
		}
		
		if (!this.paging)
		{
			this.topPageBlocker.css('opacity', 0);
			this.bottomPageBlocker.css('opacity', 0);
		}
	},
	
	updateBlockerClass : function()
	{	
		if (isAndroid)
			return;
	
		this.topPageBlocker.toggleClass('allowFade', this.paging);
		this.bottomPageBlocker.toggleClass('allowFade', this.paging);
		
		this.setBlockerClassBefore = true;
	},
	
	blockersShouldBlockTouch : function(block)
	{
		if (isAndroid)
			return;
			
		this.topPageBlocker.toggleClass('passTouch', !block);
		this.bottomPageBlocker.toggleClass('passTouch', !block);
	},
	
	updatePaging : function(justCalc, afterDirection, isStarting)
	{
		//this.log('updatePaging ');
		
		if (!this.paging)
			return;
		
		this.blockersShouldBlockTouch(false);
		
		var windowHeight = (windowSize())[1];
		var pageHeight   = this.pageHeight();
		var header 		 = $('#RIL_header');
		var headerBtmY	 = offsetTop(header[0]) + header.outerHeight(true);
		var docHeight    = $(document).height();
 		var maxY 		 = docHeight - this.lastPageSpacer.height();
 		
 		/*
 		this.log('----');
 		this.log('height ' + windowHeight);
 		this.log('pageHeight ' + pageHeight);
 		this.log('docHeight ' + docHeight);
 		this.log('offset ' + window.pageYOffset);
 		this.log('scrollY ' + scrollY());
 		this.log('headerBtmY ' + headerBtmY);
 		*/
 				
		var currentPageBreak	= this.getPageBreakAtPageY( window.pageYOffset+this.topPagingPadding(), afterDirection=='next' );
		
		//this.log('current : ' 	+ currentPageBreak);
		if (!this.isTablet && currentPageBreak < window.pageYOffset+this.topPagingPadding())
			currentPageBreak	= this.getPageBreakAtPageY( window.pageYOffset+this.topPagingPadding(), afterDirection=='next', true );
		
		//this.log('current : ' 	+ currentPageBreak);
		var previousPageBreak 	= this.getPageBreakAtPageY( currentPageBreak - pageHeight );
		//this.log('previous : ' 	+ previousPageBreak);
		var nextPageBreak 		= this.getPageBreakAtPageY( window.pageYOffset+windowHeight-this.btmPagingPadding() , true );
		//this.log('next : ' 		+ nextPageBreak);
 		
		this.blockersShouldBlockTouch(true);
		
 		// caps 		
 		if (currentPageBreak <= headerBtmY)
 			currentPageBreak = currentPageBreak > this.topPagingPadding() ? headerBtmY : 0;
 		if (previousPageBreak <= headerBtmY)
 			previousPageBreak = previousPageBreak > this.topPagingPadding() ? headerBtmY : 0;
 		if (nextPageBreak > docHeight)
 			nextPageBreak = docHeight;
 		if (nextPageBreak >= maxY)
 			nextPageBreak = maxY;
		 			 		
		this.p = {
			currentPageBreak : currentPageBreak,
			previousPageBreak : previousPageBreak,
			nextPageBreak : nextPageBreak
		}
		
		//this.log('final current : ' 	+ currentPageBreak);
		
		///*
		if (!justCalc)
		{
			if (isAndroid)
			{
				// Because of a hardware acceleration bug where the webview does not update while scrolling
				// we have to have native make these views oh god why android why
				PocketAndroidArticleInterface.placePageBlockers(currentPageBreak, nextPageBreak);
			}
			else
			{
				this.topPageBlocker.css({
					'height' : currentPageBreak + 'px',
					'opacity' : 1
				});
				this.bottomPageBlocker.css({
					'top' : nextPageBreak+'px',
					'opacity' : 1
				});
			}
		}
		//*/
				
		/*
		//Show page breaks : For Debugging
		this.topPageBlocker.css({
			'top' : currentPageBreak+'px',
			'height' : '5px',
			'opacity' : 1,
			'background' : 'red'
		});
		this.bottomPageBlocker.css({
			'top' : nextPageBreak+'px',
			'height' : '5px',
			'opacity' : 1,
			'background' : 'red'
		});
		*/
	},
	
	getPageBreakAtPageY : function(y, goUp, useRectBottom)
	{
		// Because tablets have floated images we need to check both sides and take the lower/higher value
		/*if (this.isTablet)
		{
			// Try the right side of the page (where floated images could be)
			this.foundImage = false;
			var y1 = this.getPageBreakAtPageYAtX(y, goUp, useRectBottom, (windowSize())[0]*0.65);
			var y2 = this.getPageBreakAtPageYAtX(y1, goUp, !goUp, (windowSize())[0]*0.35);
			
			var best;
			if (goUp)
				best = y1 < y2 ? y1 : y2;
			else
				best = y1 > y2 ? y1 : y2;
			
			if (!this.foundImage && y2 != y1)
			{
				// look for an image at the place we are about to send back and make sure there isn't one there
				// ex: y1 misses under image, y2 finds text and moves y up to a place that now runs over an image
				var y3 = this.getPageBreakAtPageYAtX(best, goUp, useRectBottom, (windowSize())[0]*0.65);
				if (y3 != best)
					
			}
			
			return best;
		}
		else*/
			return this.getPageBreakAtPageYAtX(y, goUp, useRectBottom, (windowSize())[0]/2);
	},
	
	getPageBreakAtPageYAtX : function(y, goUp, useRectBottom, x)
	{
		// Find if an element overlaps at this point and find the next place below it that we can cleanly break at.
		// For text nodes, we try to go in between the next line of text.
		// For images, we just go right after the image.
		//this.log('getPageBreakAtPageYAtX ' + y);
		
		var screenY = y-scrollY();
		var element = elementAtPoint( x , screenY);
		if (!element)
			return y; // nothing blocking you, go for it
		
		//this.log('element ' + element.nodeName);
		
		if (element == this.lastPageSpacer[0])
			return y; //the end of the page
			
		var isImgBlock = false;
		var $element = $(element);
		var imgBlock = $element.parents('.RIL_IMG');
		if (imgBlock.length)
		{
			isImgBlock = true;
			element = imgBlock[0];
			$element = imgBlock;
		}
		else if ($element.hasClass('RIL_IMG'))
			isImgBlock = true;
			
		if (isImgBlock)
			this.foundImage = true;
		
		var isVideoBlock = false;
		var videoBlock = $element.parents('.RIL_VIDEO');
		if (videoBlock.length)
		{
			isVideoBlock = true;
			element = videoBlock[0];
			$element = videoBlock;
		}
		else if ($element.hasClass('RIL_VIDEO'))
			isVideoBlock = true;
		
		var newY = y;
		if ($.trim(element.textContent).length && !isImgBlock && !isVideoBlock)
		{
			var range = document.caretRangeFromPoint(x, screenY);
			if (!range)
				return y; // error, text will likely be cut off
				
			var node = range.startContainer;
			
			if (node)
			{
				if (node.id && node.id.match('RIL_IMG_A'))
				{
					element = ($(node).parents())[0];
					return goUp ? offsetTop(element) - 1 : offsetTop(element) + element.offsetHeight + 1;
				}
			
				//this.log('node ' + node.nodeName + ' | ' + node.id + ' | ' + node.className);
				//this.log('tc ' + node.innerHTML);
			}
			
			// if we hit the html or container then nothing is blocking and we can use that space
			// we do this here instead of up by elementAtPoint because elementAtPoint does not to appear to be as accurateand people are like paper dolls.
			if (!this.isInsideArticleBody(node))
				return y;
				
				//this.log('abc');
				
			if (node == this.lastPageSpacer[0])
			{
				//this.log('a');
				return y; //the end of the page
			}
				
			// make sure the target actually has some text
			else if ($.trim(node.textContent).length)
			{
				//this.log('b');
				try
				{
					if (range.startOffset > 0)
						range.setStart(node, range.startOffset-1);
					else
						range.setEnd(node, range.startOffset+1);
				} 
				catch(e){
					return y;
				}
				var rect = getBoundingClientRectOfRange(range);
				if (!rect)
					return y; // error, text will likely be cut off
				
				//this.log('rect edge top/btm : ' + rect.top + ' ' + rect.bottom); 
				//this.log('scrollY ' + scrollY());
				return scrollY() +  (useRectBottom ? rect.bottom : rect.top);
			}
			
			else if (node.offsetTop)
			{
				var oTop = offsetTop(node);
				//this.log('c : ' + oTop + ' | ' + node.offsetTop);
				newY = goUp ? oTop - 1 : oTop + node.offsetHeight + 1;
			}
			
			else
			{	
				//this.log('d');
				return y;
			}
		}
		
		else
		{
			// there is no text in the element so we should just jump to the end (assuming it is an image or video)
			newY = goUp ? offsetTop(element) - 1 : offsetTop(element) + element.offsetHeight + 1;
		}
		
		// We wrap the result in another getPageBreakAtPageY check in case the first landed in some whitespace of a parent object and then we
		// go moved over one of it's children
		//if (y != newY)
		//	return this.getPageBreakAtPageY( newY );
		return newY;
	},
	
	didPageToRealTop : function()
	{
		var self = this;
		this.updatePaging();
		setTimeout(function(){ self.unfreeze(); }, 15);
	},
	
	scrollStarted : function()
	{
		// exit paging mode, user has started to scroll
		this.setPageMode(false);
	},
	
	goNextPage : function()
	{
		this.flipPage('next');
	},
	
	goPreviousPage : function()
	{
		this.flipPage('previous');
	},
	
	flipPage : function(direction)
	{
		//this.log('flipPage ' + direction);
		
		var self = this;
		afterFreeze = function() {
			//this.log('flipPage under freeze ' + direction);
			self.setPageMode(true);
			
			// Get the target scroll position
			var y;
			if (direction == 'next')
				y = self.p.nextPageBreak-self.topPagingPadding();
			else if (direction == 'previous')
				y = self.p.previousPageBreak-self.topPagingPadding();	
				
			if (y < 0 && !isAndroid)
			{
				PocketAndroidArticleInterface.pageToRealTop(); // because javascript cannot handle scrollView insets, we need to have native move us up there
				return; // we wait because of a race condition, this is cleaned up in didPageToRealTop
			}
			
			else if (y < (self.lastPageSpacer.offset()).top)
			{
				if (y < 0)
					y = 0;
			
				self.lastFlipDirection = direction;
				window.scrollTo(0, y);
				
				if (isAndroid && y != window.pageYOffset)
				{
					// due to a race condition we need to wait until we receive the onscroll event before moving on
					// the onscroll event set in articleIsReady will handle calling didFlipPage
					
					// there are cases where the page may not scroll (if they are able to swipe twice at the end of the doc)
					// so we have a timeout safeguard that will unset it if it didn't already
					self.safeTO = setTimeout(function()
					{
						if (article.lastFlipDirection)
						{
							article.log('Freeze Lock : Found to be locked, unfreezing');
							article.didFlipPage(true, article.lastFlipDirection);
							article.lastFlipDirection = false;
						}
					}, 1000);
				}
				else
					self.didFlipPage(true, direction);
			}
			
			else
			{
				self.didFlipPage(false, direction);
			}
			
		};
		if (isAndroid) {
			// Freeze is handled natively in Android and will already be frozen by the time it gets here.
			afterFreeze();
		} else {
			this.freeze(afterFreeze);
		}
	},
	
	didFlipPage : function(didScroll, direction)
	{	
		//this.log('didFlipPage ' + direction);
			
		if (didScroll)
			article.updatePaging(false, direction);
			
		setTimeout(function(){ article.unfreeze(); }, 15);
	},
	
	selectionHasChanged : function()
	{		
		if (!isAndroid)
		{
			var selection = window.getSelection();
			if (!selection || selection.rangeCount <= 0)
				return;
				
			var range = selection.getRangeAt(0);
			if (range)
			{
				var rect = getBoundingClientRectOfRange(range);
				if (rect)
				{
					var y = scrollY();
					
					// does the selection extend outside of the page?		
					if (rect.top + y < this.p.currentPageBreak || rect.bottom + y > this.p.nextPageBreak)
						this.setPageMode(false);
				}		
			}
		}
	},
	
	// --- //
	
	adjustDisplayBasedOnFontSize : function(fontSize)
	{	
		var self = this;
		this.keepScrollPositionWhile(function() {
			self.performAdjustDisplayBasedOnFontSize(fontSize);
		});
	},
	
	performAdjustDisplayBasedOnFontSize : function(fontsize) 
	{
		var fontSize = fontSize || document.body.style.fontSize.replace('px','') * 1;
		
		// Adjusts margins
		margin = PocketAndroidArticleInterface.getHorizontalMargin();
		
		$('#RIL_container').css('margin-left', margin+'px')
			.css('margin-right', margin+'px');
			
		// Adjust alignment -- at large sizes we move to aligned/hypenated to fit more on a single line
		$(document.body).toggleClass('force_left', fontSize > (this.isTablet ? 38 : 20) );
		
		// Line-height adjustment, at big font sizes we reduce the lineHeight a bit
		$(document.body).toggleClass('tighter_line_height', fontSize > 40 );
		
		// If font size is large, we start to make some adjustments to the layout to prevent
		// extremely large headers or strange layout issues
		$(document.body).toggleClass('large_font_size', fontSize > 40 );
		
		if (this.isTablet)
		{
			// decide if the date/authors can be in corners or in two lines
			$(document.body).toggleClass('right_side_date', $('.ril_byline_content').width() + $('.RIL_date').width() + 15 < $('#RIL_container').width());

			// update images to fit
			this.readjustImagesToMatchBodyWidth();
		}
		
		this.onMarginsChanged();
	},
	
	readjustImagesToMatchBodyWidth : function()
	{
		var self = this;
		$('.RIL_IMG').each(function(index, element){
			
			var $ele = $(element);
			self.adjustImage($ele.attr('id').replace('RIL_IMG_', ''));
			
		});
	},
	
	setOverlayPadding : function(top, bottom) {
		$('body')
			.css('padding-top', top + 'px')
			.css('padding-bottom', bottom + 'px');
	},

	setFooterHeight : function(height) {
		var f = this.footer;
		f.height(height);
	},

	setFooterTrackingEnabled : function() {
		var f = this.footer;
		var footerY = -1;
		setInterval(function()
		{
			var y = f.offset().top;
			if (y != footerY) {
				footerY = y;
				PocketAndroidArticleInterface.setFooterPosition(y);
			}
		}, 500);
	},

 	newFontSize : function(i)
	{
		var self = this;
		this.keepScrollPositionWhile(function() 
			{
				document.body.style.fontSize = i + 'px';
				self.curentFontSize = i;
				self.adjustDisplayBasedOnFontSize();
			});
	},
	
	newFontType : function(i)
	{
		this.setBodyAttr('fontType', i);
	},
	
	newTextStyle : function(i)
	{
		this.setBodyAttr('textStyle', i);
	},
	
	newTextAlign : function(textStyle)
	{
		$(document.body).toggleClass('hyphenate', textStyle == 1);
	},

    newLineHeightSetting : function(i)
    {
    	this.setBodyAttr('lineHeightSetting', i);
    },

    newMarginSetting : function(i)
    {
        this.adjustDisplayBasedOnFontSize();
    },

    addCustomCss : function(path)
    {
        var link = document.createElement('link');
        link.setAttribute('rel', 'stylesheet');
        link.setAttribute('type', 'text/css');
        link.setAttribute('href', path);
        document.getElementsByTagName('head')[0].appendChild(link);
    },

	// hack to force webkit to redraw/repaint
	setBodyAttr : function(attr, value)
	{
		document.body.setAttribute(attr, value);
		this.updatePaging();
		/*
		// This causes a lag in updating the font color (so they disappear and a shift in content
		var styleGuy = document.createElement('style');
		document.body.appendChild(styleGuy);
		setTimeout(function()
		{
			if (styleGuy && styleGuy.parentNode)
				styleGuy.parentNode.removeChild(styleGuy)
		},10);*/
	},
	
	/**
	* Runs a function and trys to preserve the scroll/element/node position it was at before running. This is helpful when changing the font size or margins.
	* Will also update paging.
	*
	*/
	keepScrollPositionWhile : function(textChangingFunction)
	{
		if (this.isPerformingTextChange) {
			// Already in the middle of a text change, ok to call function now.
			textChangingFunction();
			
		} else {
			this.isPerformingTextChange = true;
			
			var el;
			try{ el = this.saveScroll(); } catch(e){} // jump back to the same element after the text size changes
			
			textChangingFunction();
			
			if (el) this.scrollIntoViewCompat(el);
			
			this.updatePaging();
			
			this.isPerformingTextChange = false;
		}
	},
	
	onMarginsChanged : function() {
		var width = $('#RIL_body').width();
		if (this.bodyWidth == width) {
			return; // No change
		}
		
		this.bodyWidth = width;
		// TODO actually i don't think we need this anymore? PocketAndroidArticleInterface.onMarginsChanged(width);
		
		// TODO double check if this comment still applies: // We only want to update margins when the width changes otherwise it can cause a jump/scroll during the full screen animation
	},
	
	// -- //
	
	scrolled : function(e) 
	{
		try
		{
			if (this.lastY != scrollY())
			{
				this.saveScroll();
				if (isAndroid) 
				{
					PocketAndroidArticleInterface.onScrollChanged(this.currentPosition.scrollHeight*1, this.currentPosition.innerHeight*1, this.currentPosition.scrolled*1, this.currentPosition.nodeIndex*1, this.currentPosition.section*1, this.currentPosition.page*1);
				}
			}
		}
		catch(e){/*alert(e);*/}
	},
	
	getPosition : function()
	{
		return this.currentPosition.scrollHeight+'|'+
				this.currentPosition.innerHeight+'|'+
				this.currentPosition.scrolled+'|'+
				this.currentPosition.nodeIndex+'|'+
				this.currentPosition.section+'|'+
				this.currentPosition.page;
		
	},
	
	saveScroll : function() 
	{
		try{	
			var win 	= windowSize();
			var y 		= scrollY(); 
			var tY 		= elementFromPointUsesScreen() ? 20 : y;
			var high 	= document.elementFromPoint(win[0]/2, tY);
			var low 	= document.elementFromPoint(win[0]/2, tY+25);
			var iH 		= high.getAttribute('nodeIndex');
			var iL 		= low.getAttribute('nodeIndex');
			
			var e, i;
			if (iH && iH*1 > iL*1) {
				e = high;
				i = iH;
			} else if (iL) {
				e = low;
				i = iL;	
			}
			
			//e.style.background = '#FF0000';
			//alert(e.tagName + ' | ' + e.id); return;
			
			if (i || y < 100) 
			{
				// find page
				var p = 0;
				var pageId = $(e).parents('.RIL_PAGE').attr('id');
				if (pageId) p = pageId.replace('RIL_PAGE_','');
				
				// save position
				var m 	= document.body.scrollHeight;
				var h 	= win[1];
				
				this.currentPosition.scrollHeight = m;
				this.currentPosition.innerHeight = h;
				this.currentPosition.scrolled = y;
				this.currentPosition.nodeIndex = i;
				this.currentPosition.section = (document.body.id=='more'?1:0)
				this.currentPosition.page = p;
				
				this.lastI = i;
			}
			this.lastY = y;
			
			return e;
		}catch(e){  /*alert(e);*/}
	},
	
	scrollToLastI : function()
	{
		this.scrollToNodeIndex(this.lastSection, this.lastI);
	},
	
	scrollToNodeIndex : function(section, page, nodeIndex) 
	{
		try
		{
			this.lastSection = section;
					
			// find first Element with attribute nodeIndex = i (make sure it's inside less or more based on view)
			var i, e, av, arrElements;
			var mode = section == 1 ? 'more' : 'less';
	
			if (page && page > 0 && document.getElementById('RIL_PAGE_'+page))
				arrElements = document.getElementById('RIL_PAGE_'+page).getElementsByTagName('*');
			else
				arrElements = document.getElementById('RIL_'+mode).getElementsByTagName('*');
	
			for(i=0; i<arrElements.length; i++)
			{
				av = arrElements[i].getAttribute('nodeIndex');
				if (av && av == nodeIndex) 
				{
					e = arrElements[i];
					break;
				}
			}
			
			if (e)
			{
				document.body.setAttribute('id', mode);
				this.scrollIntoViewCompat(e);
			}
		}
		
		catch(e){/*alert(e);*/}
	},

	// -- //
	

	didRotate : function()
	{
		this.adjustDisplayBasedOnFontSize();
	},

	// -- //
	
	loadImage : function(id,src,caption,credit)
	{
		var dropSpot = $('#RIL_IMG_'+id);
		if (dropSpot.length == 0 && id == 1) {
			dropSpot = $('<div id="RIL_IMG_'+id+'" class="RIL_IMG"></div>');
			$('#RIL_less').prepend(dropSpot);
		}
		
		// helps eliminate flickering when replacing or rotating images
		dropSpot.css('visibility', 'hidden');
				
		if (caption.length || credit.length) {
			$('#RIL_IMG_'+id).addClass('hasCaption');
		}
		
		var self = this;
		var image = new Image();
		image.onload = function() 
		{		
			// Get and store the image dimensions
			var imageSelf = this;
			var $image = $(image);
			var realImageWidth = article.scale > 1 ? this.width / article.scale : this.width;
			var realImageHeight = article.scale > 1 ? this.height / article.scale : this.height;
			
			self.images[id] = {
				realImageWidth : realImageWidth,
				realImageHeight: realImageHeight
			};

			// replace content
			dropSpot.html('<div class="ril_wrapper"><a id="RIL_IMG_A_'+id+'" href="ISRIL:IMG||'+id+'" class="noLinkStyle"></a></div><div class="ril_caption"><span class="ril_caption_content">'+caption+'</span> <cite>'+(credit?'Photo by: ':'')+'<span class="credit_content">'+credit+'</span></cite></div>');
			
			// Get a reference to the imageWrapper
			var imageWrapper = $('#RIL_IMG_A_'+id);
			var wrapper = dropSpot[0];
			
			// Adjust the image size
			self.adjustImage(id);
			
			self.keepScrollPositionWhile(function() {
				// Add image to page
				imageWrapper.append($image);
				
				// show the image
				$(wrapper).css('visibility', 'visible');
				wrapper.className += ' loaded ';
				
				if (wrapper.parentNode.nodeName == 'A' && !wrapper.parentNode.href.match('ISRIL:')) {
					wrapper.parentNode.href = imageSelf.parentNode.href = 'ISRIL:LINKIMG||'+id+'||'+wrapper.parentNode.href;
				}
				
				self.updatePageSwipingDisabledAreas();
			});
		}
		
		image.src = src;
	},
	
	adjustImage : function(id)
	{
		var self = this;
		this.keepScrollPositionWhile(function() {
			self.performAdjustImage(id);
		});
	},
	
	performAdjustImage : function(id) {		
		var image = this.images[id];
		if (!image) {
			return;
		}
	
		var imageWrapper = $('#RIL_IMG_A_'+id);
			
		// image placement
		// if the image can be wider than the 
		var imageIsLandscape = image.realImageWidth > image.realImageHeight;
		var containerWidth = $('#RIL_container').width();
		var pageWidth = $('body').width();
		
		var newImageWidth = 0;
		var newImageHeight = 0;
		var maxImageUpScaleToFullBleed = 1.8;
		var maxImageUpScaleToSnapMargin = 1.8;
		
		if (!imageIsLandscape || article.isTablet) {
			// Not allowed to scale.
			newImageWidth = image.realImageWidth;
			
		} else if (image.realImageWidth * maxImageUpScaleToFullBleed >= pageWidth) {
			// Allowed to display as full bleed.
			newImageWidth = pageWidth;
			
		} else if (image.realImageWidth * maxImageUpScaleToSnapMargin >= containerWidth) {
			// Close enough to the margin that it should just scale up slightly to snap to it.
			newImageWidth = containerWidth;
		
		} else {
			// Actual image width.
			newImageWidth = image.realImageWidth;
		}
		
		// min width
		if (newImageWidth < 175) {
			newImageWidth = 175;
		}
			
		// max width
		if (this.isTablet && newImageWidth > containerWidth) {
			newImageWidth = containerWidth;
		}
		
		// determine the height based on the width
		newImageHeight = newImageWidth * image.realImageHeight / image.realImageWidth;
		
		//this.log('newImageWidth: ' + newImageWidth)
		
		// See if the image is now too tall
		// We want to make sure that the image+caption does not appear larger than the viewport (for pagination)
		var captionHeight = 0;
		var $caption = imageWrapper.find('.ril_caption');
		if ($caption.length && $caption.text().length) {
			captionHeight = $caption.outerHeight(true);
		}
		var maxImageHeight = (PocketAndroidArticleInterface.getMaxMediaHeight() - this.topPagingPadding(true) - this.btmPagingPadding(true)); 
			
		if (newImageHeight + captionHeight > maxImageHeight) {
			// force the imageHeight to maxHeight and then readjust the width to fit
			newImageHeight = maxImageHeight - captionHeight;
			newImageWidth = newImageHeight * image.realImageWidth / image.realImageHeight;
			
			// if the width sticks outside of the body margins, now force it to that width instead
			if (newImageWidth > containerWidth) {
				newImageWidth = containerWidth;
				newImageHeight = newImageWidth * image.realImageHeight / image.realImageWidth;
			}
		}
		
		// See if the image width will expand the entire width of the page
		var isFullBleed = (newImageWidth >= pageWidth);

		// Set dimensions
		imageWrapper.parent().css({width:newImageWidth, height:newImageHeight>0?newImageHeight:''});
		imageWrapper.parent().toggleClass('notFullBleed', !isFullBleed);
					
		// iPad floating
		/*
		if (article.isTablet){
			if(this.width / $('#RIL_container').width() <= 0.6){
				wrapper.setAttribute('float', 1);
			}
		}
		else if (article.isTablet)
			wrapper.setAttribute('float', 0);
		*/
		
		this.updatePageSwipingDisabledAreas();
	},
	
	isOnline : function() 
	{
		if (PocketAndroidArticleInterface) {
			return PocketAndroidArticleInterface.isConnected();
		
		} else {
			return navigator.onLine;
		}
	},
	
	loadVideo : function(video)
	{
		var self = this;
		var id = video.video_id;
		
		// If the video has already been loaded then do not reload it #2735
		if (this.videosLoaded[id]) {
			return;
		}
		this.videosLoaded[id] = true;
		
		var defaultWidth = $('body').width(); // TODO what is the default? it changes for tablets, also if it is already known we can use that meta data
		var defaultHeight = Math.round(defaultWidth * 0.5625);
		
		// Build a structure to support the full bleed design.
		var element = $("#RIL_VIDEO_"+id);
		var wrapper = $("<div class='ril_wrapper'></div>")
			.css("width", defaultWidth + "px")
			.css("height", defaultHeight + "px");

		var embedId = "RIL_VIDEO_EMBED"+id;	
		var embed = $("<div id='"+embedId+"'></div>")
			.addClass("embed_wrapper");

		wrapper.append(embed);

		// Add it to the layout
		this.saveScroll();
		element.append(wrapper); // Inserts the structure into the layout
		element.append($("<div class='RIL_clear'></div>")); // Clears the full bleed float
		this.scrollToLastI();
		
		var player = new VideoPlayer(video, embedId)
			.setDisplaySize(defaultWidth, defaultHeight)
			.setOnResizeListener(function(newWidth, newHeight) {
				wrapper.css("width", newWidth + "px")
					.css("height", newHeight + "px");
			})
			.load();
		this.videosLoaded[id] = player;

		// TODO this doesn't seem to do anything in the legacy reader, but does work with the new reader
		// TODO behavior seems better without it.  Removing for now
//		$(window).resize(function() {
//			player.setDisplaySize(
//				$('body').width(),
//				PocketAndroidArticleInterface.getMaxMediaHeight());
//		});
	},
	
	requestContentHeight : function()
	{
		PocketAndroidArticleInterface.onRequestedContentHeight(document.body.scrollHeight);
	},
	
	fixLayout : function()
	{
		document.getElementById('RIL_container').style.padding = '0px 0px '+(Math.random()*2)+'px 0px';
	},
	
	/** 
	* scrollIntoView does not work on some Android devices. So we try it and if they didn't actually move, we just do it manually.
	*/
	scrollIntoViewCompat : function(ele)
	{
		var beforeY = scrollY();
		var element = $(ele);
		
		setTimeout(function() {
			ele = element[0];
			var elementY = offsetTop(ele);
			
			window.scrollTo(0, elementY);
			var afterY = scrollY();
		}, 50);
	},
	
	isInsideArticleBody : function(element)
	{	
		var parent = element.parentNode;
		while(parent && parent.nodeName != 'BODY')
		{
			if (parent.id && parent.id.match('RIL_less'))
				return true;
			parent = parent.parentNode;
		}
		return false;
	},
	
	freeze : function(cb)
	{
		this.frozenCallback = cb;
		PocketAndroidArticleInterface.setFrozen(true);
	},
	
	didFreeze : function()
	{
		if (this.frozenCallback)
		{
			this.frozenCallback();
			delete this.frozenCallback;
		}
	},
	
	unfreeze : function()
	{
		PocketAndroidArticleInterface.setFrozen(false);
	},
	
	// -- //
	
	log : function(msg)
	{
		if (this.debug && isAndroid)
			console.log(msg);
	},
	
	// Android TTS Methods
	
	addSpeechNode : function(node, prefix) {
		if (!node || node.length == 0) {
			return;
		}
		
		var nodeIndex = node.attr('nodeIndex');
	    var trimmedText = $.trim(this.getVisibleText(node));
	    if (trimmedText.length == 0) {
	        // We can ignore it, though index the nodeIndex if needed
	        this.indexNodeTo(node, this.listenIndex);
	        return;
	    }
	    
	    if (prefix) {
	    	trimmedText = prefix + trimmedText;
	    }

	    node.attr('listenIndex', this.listenIndex);
	    this.speechNodes.push({
	        listenIndex: this.listenIndex,
	        text: trimmedText,
			nodeIndex: nodeIndex,
			isHeader: node.is(':header')
	    });
	    
	    this.listenIndex++;
	},
	
	getVisibleText : function(node) { // This is probably really slow
		if (node.children().length == 0)
			return node.text();
		
		var text = "";
		var self = this;
		node.contents().each(function(){
			var child = $(this)
			if (!child.is(":hidden")) {
				text = text + self.getVisibleText($(this));
			}
		});	
		
		return text;
	},
	
	checkNode : function(node, hasNodeIndex) {
		var self = this;
	    	    
	    node.contents().each(function() {
	        var child = $(this);
	        var tagName = child.prop("tagName");
	        
	        if (child.attr('pktnolisten') || tagName == "SCRIPT" || tagName == "STYLE") {
	         	// Skip this element and all of its children completely.
	         	
	        } else if (this.nodeType == 3) {
	            // Is TextNode, change to be an element and add.
	            self.addSpeechNode(child.wrap('<span />'));
	            
	        } else if (this.nodeType == 1) {
	       	 	// Is ElementNode
	            if (child.find('[nodeIndex]').length == 0) {
	            	// Has no children that are nodeIndex's
	                self.addSpeechNode(child);
	                
	            } else {
	            	// Has children with nodeIndex's
	            	if (child.attr('nodeIndex') && self.isOneLogicalSpeechBlock(child)) {
	            		// We will include this node as one speech block and not break up its children.
		            	self.addSpeechNode(child);
		            	self.indexAllChildrenTo(child, self.listenIndex - 1); // Loop through all node index children and map them to this node.
		            		
		            } else {
		            	// This element itself will not be a speech node, we will break it up into different speech nodes.
		            	self.indexNodeTo(child, self.listenIndex); // self.listenIndex is the next speech node whatever it is.
		            	self.checkNode(child);
		            }
	            
	            }
	                
	        } else {
	            // Ignore
	        }
	    });
	},
	
	isOneLogicalSpeechBlock : function(parentNode) {
		// If the only nodeIndex child are <a>'s with less than 30 characters each or are just empty nodes, then we should just 
		// include the parentNode as one speechNode and not break it up.
		var isOneBlock = true;
		parentNode.children('[nodeIndex]').each(function() {
			var child = $(this);
			
			var ni = child.attr('nodeIndex');
			var textLength = $.trim(child.text()).length;
			if (textLength == 0)
				return true; // Will be ignored anyways, so no need to break it up.
			
			if (child.is('a') && textLength < 30)
				return true; // It is a link that is short, likely not needing to be broken up.
			
			
			// This node should probably be separated.	
			isOneBlock = false;
			return false; // This breaks the loop
		});
		return isOneBlock;
	},
	
	indexNodeTo : function(node, li) {
		var nodeIndex = node.attr('nodeIndex');
		if (!nodeIndex)
			return;
			
		this.nonSpeechNodeIndexs.push({
			nodeIndex: nodeIndex,
			listenIndex: li
    	});
	}, 
	
	indexAllChildrenTo : function(node, li, indexSelf) {
		if (indexSelf) {
			this.indexNodeTo(node, li);
		}
		
		var self = this;
		node.children('[nodeIndex]').each(function() {
			self.indexAllChildrenTo($(this), li, true);
		});
	},
	
	fetchSpeech : function() {
		if (this.speech) {
			// We already have it parsed and ready to send back
			this.returnSpeech();
			return;	
		}
		
		this.listenIndex = 1;
		this.speechNodes = [];
		this.nonSpeechNodeIndexs = [];
		
		this.addSpeechNode($('#RIL_header h1'));
		this.addSpeechNode($('.RIL_author'), " by ");
		this.addSpeechNode($('.RIL_date'));
		
		this.checkNode($('#RIL_body'));
		
		this.speech = JSON.stringify(this.speechNodes);
		this.nonSpeechIndex = JSON.stringify(this.nonSpeechNodeIndexs);
		
		this.returnSpeech();
	},
	
	returnSpeech : function() {
		PocketAndroidArticleInterface.returnSpeech(this.speech, this.nonSpeechIndex, $('#RIL_header h1').text());
	},
	
	ttsScrollTo : function(elementSelector, index, position, yInset) {
		var node;
		if (index == -1) {
			// Selector points directly to an element
			node = $(elementSelector);
			
		} else {
			// Selector points to a TextNode, we need to find it and turn it into an element
			// First look to see if we have already converted this TextNode to a span
			var converted = $("[listenIndex="+position+"]");
			if (converted.length !== 0) {
				// Already converted
				node = converted
				
			} else {
				// Need to convert to a full element instead of a TextNode
				// elementSelector points to the TextNode's parent
				var textnode = $($(elementSelector).contents().get(index));
				node = textnode.wrap('<span listenIndex='+position+' />');
				// Post this after a delay, otherwise the scroll to method might not get a correct location 
				var self = this;
				setTimeout(function() {	
					self.ttsScrollTo(elementSelector, index, position, yInset);
				},50);
				return;
			}
		}		
		if (node.position()) {
			var y = node.offset().top;
			var bottomY = node.outerHeight + y;
			var windowTop = $(window).scrollTop();
			var windowBottom = windowTop + window.innerHeight;
			$('html,body').animate({scrollTop: y-15+yInset}, 333);
		};
	}
		
});

// -- Functions -- //

function offsetTop(obj)
{
	var oObj = obj;
	
	if (!obj)
		return;

	var curtop = 0;
	if (obj.offsetParent) {
		do {
			//article.log('offsetTop parent ' + obj.nodeName + ' | ' + obj.id + ' | ' + obj.offsetTop);
			if (obj.offsetTop)
				//curtop = obj.offsetTop;
				curtop += obj.offsetTop; // this is how this method normally works but it breaks on Android when trying to get the position of an image. Oddly enough it works if we just take the top offsetParent's offsetTop
		} while (obj = obj.offsetParent);
	}
	
	//article.log('offsetTop : ' + oObj.nodeName + ' | ' + oObj.id + ' | ' + curtop + ' | ' + oObj.offsetTop);
	//article.log('offset tc ' + oObj.innerHTML);
	return curtop;
}

function scrollY() 
{
	var y=0;
	if( document.scrollingElement ) {
		//Standards-compliant behaviour enabled in Android P
		y = document.scrollingElement.scrollTop;
	} else if( typeof( window.pageYOffset ) == 'number' ) {
		//Netscape compliant
		y = window.pageYOffset;
	} else if( document.body && ( document.body.scrollTop ) ) {
		//DOM compliant
		y = document.body.scrollTop;
	} else if( document.documentElement && ( document.documentElement.scrollTop ) ) {
		y = document.documentElement.scrollTop;
	}
	return y;
}

function windowSize() 
{	var h = 0, w=0;
	if( typeof( window.innerWidth ) == 'number' ) {
		//Non-IE
		w = window.innerWidth;
		h = window.innerHeight;
	} else if( document.documentElement && ( document.documentElement.clientHeight ) ) {
		//IE 6+ in 'standards compliant mode'
		w = document.documentElement.clientWidth;
		h = document.documentElement.clientHeight;
	} else if( document.body && ( document.body.clientHeight ) ) {
		//IE 4 compatible
		w = document.body.clientWidth;
		h = document.body.clientHeight;
	}
	return [w,h];
}

var _elementFromPointUsesScreen;
function elementFromPointUsesScreen()
{
	if (_elementFromPointUsesScreen)
		return _elementFromPointUsesScreen;
		
	// Determine if the browser uses relative or page based coordinates
	// http://www.zehnet.de/2010/11/19/document-elementfrompoint-a-jquery-solution/
	var sl;
	var isRelative = true;
	
	if((sl = $(document).scrollTop()) >0)
		isRelative = (document.elementFromPoint(0, sl + $(window).height() -1) == null);
		
	else if((sl = $(document).scrollLeft()) >0)
		isRelative = (document.elementFromPoint(sl + $(window).width() -1, 0) == null);
	
	else
		return true; // we aren't scrolled so it doesn't matter which one you use but we can't calculate yet so do not cache a value
	
	_elementFromPointUsesScreen = isRelative;
	
	return _elementFromPointUsesScreen;
}

function elementAtPoint(x, y)
{
	var yAdjust = elementFromPointUsesScreen() ? 0 : scrollY();
	return document.elementFromPoint(x, yAdjust + y);
}

function firstElementWithAttributeValue(a, v) 
{
	var arrElements = document.getElementsByTagName('*');
	for(var i=0; i<arrElements.length; i++){
		av = arrElements[i].getAttribute(a);
		if (av && av == v) {
			return arrElements[i];
		}
	}
}

document.getElementsByAttribute = function( attrib, value, context_node, tag ) {
		var nodes = [];
		if ( context_node == null )
			context_node = this;
		if ( tag == null ) 
			tag = '*';
		var elems = context_node.getElementsByTagName(tag);
		
		for ( var i = 0; i < elems.length; i += 1 ) {
			if ( value ) {
				if ( elems[i].hasAttribute(attrib) && elems[i].getAttribute(attrib) == value )
					nodes.push(elems[i]);
			} else {
				if ( elems[i].hasAttribute(attrib) )
					nodes.push(elems[i]);
			}
		}
		return nodes;
}

function toTitleCase(str)
{
    return str.replace(/\w\S*/g, function(txt){return txt.charAt(0).toUpperCase() + txt.substr(1).toLowerCase();});
}

function getBoundingClientRectOfRange(range)
{
	var rect = range.getBoundingClientRect();
	return rect;
}

