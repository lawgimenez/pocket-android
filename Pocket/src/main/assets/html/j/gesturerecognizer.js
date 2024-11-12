/*  JSGestureRecognizer, Version 1.0
 *  (c) 2011 Takashi Okamoto - BuzaMoto
 *
 *  JSGestureRecognizer is a JavaScript implementation of 
 *  UIGestureRecognizer on iOS
 *
 *  http://developer.apple.com/library/ios/#documentation/EventHandling/Conceptual/EventHandlingiPhoneOS/GestureRecognizers/GestureRecognizers.html
 *
 *  Source: https://github.com/mud/JSGestureRecognizer
 *  THIS FILE CONTAINS RIL MODIFICATIONS, DO NOT UPDATE WITHOUT DIFFING AGAINST 1.0
 *--------------------------------------------------------------------------*/


var JSGestureRecognizerStatePossible   = 'JSGestureRecognizer:possible',
    JSGestureRecognizerStateBegan      = 'JSGestureRecognizer:began',
    JSGestureRecognizerStateChanged    = 'JSGestureRecognizer:changed',
    JSGestureRecognizerStateEnded      = 'JSGestureRecognizer:ended',
    JSGestureRecognizerStateCancelled  = 'JSGestureRecognizer:cancelled',
    JSGestureRecognizerStateFailed     = 'JSGestureRecognizer:failed',
    JSGestureRecognizerStateRecognized = JSGestureRecognizerStateEnded;

var JSTouchStart     = 'touchstart',
    JSTouchMove      = 'touchmove',
    JSTouchEnd       = 'touchend',
    JSTouchCancelled = 'touchcancelled',
    JSGestureStart   = 'gesturestart',
    JSGestureChange  = 'gesturechange',
    JSGestureEnd     = 'gestureend';

var JSSwipeGestureRecognizerDirectionRight = 1 << 0,
    JSSwipeGestureRecognizerDirectionLeft  = 1 << 1,
    JSSwipeGestureRecognizerDirectionUp    = 1 << 2,
    JSSwipeGestureRecognizerDirectionDown  = 1 << 3;(function(w) {
  
  var MobileSafari = (function() {
    return /Apple.*Mobile/.test(navigator.userAgent);
  })();

  if (!MobileSafari) {
      JSTouchStart     = 'mousedown',
      JSTouchMove      = 'mousemove',
      JSTouchEnd       = 'mouseup',
      JSTouchCancelled = 'mouseup',
      JSGestureStart   = 'mousedown',
      JSGestureChange  = 'mousemove',
      JSGestureEnd     = 'mouseup';
  }

  var Framework = (function() {
    return {
      Prototype: (typeof w.Prototype != 'undefined'),
      jQuery:    (typeof w.jQuery != 'undefined')
    }
  })();
  
  // class definition using Prototype
  if (Framework.Prototype) {
    
  } else if (Framework.jQuery) {
    // add Mobile Safari touch properties to event object
    // Removed 10/14/19 when upgrading to JQuery 3.4.1, as event.props was removed in JQuery 3.0.0, and this doesn't seem to apply to Android devices anyway
    // https://jquery.com/upgrade-guide/3.0/#breaking-change-jquery-event-props-and-jquery-event-fixhooks-removed
//    $.each(['rotation', 'scale', 'touches', 'targetTouches'], function(i, propName) {
//      if ($.inArray(propName, $.event.props) < 0) {
//        $.event.props.push(propName);
//      }
//    });
    Function.prototype.bind = function(context) {
      return jQuery.proxy(this, context);
    }
  } else {
    throw new Error("Required Dependency: you need to include either Prototype.js or jQuery.");
  }

  window.isDescendentOf = function(target, parent){
	while(target != undefined && target != parent){
		target = target.parentNode;
	}
	return (target == parent);
  }
  
  // scope Class here so that it doesn't redefine Prototype's Class definition.
  // we're using John Resig's class inheritance, which is nice and library independent.
  // http://ejohn.org/blog/simple-javascript-inheritance/
  // LOCAL: We include this already.
  // var Class = function(){};
  // (function(){ var initializing = false, fnTest = /xyz/.test(function(){xyz;}) ? /\b_super\b/ : /.*/; Class.extend = function(prop) { var _super = this.prototype; initializing = true; var prototype = new this(); initializing = false; for (var name in prop) { prototype[name] = typeof prop[name] == "function" && typeof _super[name] == "function" && fnTest.test(prop[name]) ? (function(name, fn){ return function() { var tmp = this._super; this._super = _super[name]; var ret = fn.apply(this, arguments); this._super = tmp; return ret; }; })(name, prop[name]) : prop[name]; } function Class() { if ( !initializing && this.init ) this.init.apply(this, arguments); } Class.prototype = prototype; Class.constructor = Class; Class.extend = arguments.callee; return Class; };})();

  // -- Event extension -------------------------------------------------------
  if (!MobileSafari) {
    Event.prototype.allTouches = function() {
      var touches = [this];
      if (this.altKey) {
        touches.push(this);
      }
      return touches;
    }
	window.getAllTouches = function(ev){
      var touches = [ev];
      if (ev.altKey) {
        touches.push(ev);
      }
      return touches;
	}
  } else {
    Event.prototype.allTouches = function() {
      return this.targetTouches;
    }
	window.getAllTouches = function(ev){
		return ev.targetTouches;
	}
  }// -- Abstract Class: JSTouchRecognizer -------------------------------------
var JSTouchRecognizer = Class.extend({
	addsSelectedClass: false,
  enabled: true,
  initWithCallback: function(callback) {
    if (typeof callback == 'function') {
      this.callback = callback;
    } else {
      throw new Error("Callback must be set otherwise this won't do anything!");
    }
  },
  
  verbosify: function(name){
	  app.log("Making gesture recognizer verbose for " + name);
	  var self = this;
	  var makeDebugger = function(type){
		  return function(){
			  if(self.state != JSGestureRecognizerStateFailed){
			  	app.log("" + type + " for " + name + ": " + self.state);
			  }
		  }
	  };
	  this.observe(this.target, JSTouchStart    , makeDebugger("TouchStart"));
	  this.observe(this.target, JSTouchMove     , makeDebugger("TouchMove"));
	  this.observe(this.target, JSTouchEnd      , makeDebugger("TouchEnd"));
	  this.observe(this.target, JSTouchCancelled, makeDebugger("TouchCancel"));
  },
  
  // sets the target and sets up recognizer by running initRecognizer()
  setTarget: function(_target) {
    var target = (_target.view) ? _target.view : _target;
    if (target !== null && this.target != target) {
      this.target = target;
      this.view = _target;

      this.initRecognizer();
    }
  },
  
  toString: function() {
    return "JSTouchRecognizer";
  },
  
  // -- Subclass Implementation Methods ---------------------------------------
  // initRecognizer - called when target and action are set (init)
  initRecognizer: function() {
    if (this.target === null) {
      throw new Error("this.target is null, must be a DOM element.");
    }

    if(!JSTouchRecognizer.allRecognizers){
      JSTouchRecognizer.allRecognizers = [];
    }

    JSTouchRecognizer.allRecognizers.push(this);

    this.reset();
    this.state = JSGestureRecognizerStatePossible;

    this.touchmoveHandler = this.touchmove.bind(this);
    this.touchendHandler = this.touchend.bind(this);

    this.observe(this.target, JSTouchStart, this.touchstart.bind(this));
    this.observe(this.target, JSGestureRecognizerStatePossible, this.possible.bind(this));
    this.observe(this.target, JSGestureRecognizerStateBegan, this.began.bind(this));
    this.observe(this.target, JSGestureRecognizerStateEnded, this.ended.bind(this));
    this.observe(this.target, JSGestureRecognizerStateCancelled, this.cancelled.bind(this));
    this.observe(this.target, JSGestureRecognizerStateFailed, this.failed.bind(this));
    this.observe(this.target, JSGestureRecognizerStateChanged, this.changed.bind(this));
  },
  
  reset: function() {
	this.stopObserving(this.target, JSTouchStart);
    this.stopObserving(this.target, JSGestureRecognizerStatePossible);
    this.stopObserving(this.target, JSGestureRecognizerStateBegan);
    this.stopObserving(this.target, JSGestureRecognizerStateEnded);
    this.stopObserving(this.target, JSGestureRecognizerStateCancelled);
    this.stopObserving(this.target, JSGestureRecognizerStateFailed);
    this.stopObserving(this.target, JSGestureRecognizerStateChanged);
},
  
  
  // -- Touch Events ----------------------------------------------------------
  touchstart: function(event, obj) {
    if (this.target && window.isDescendentOf(event.target, this.target)) {
      this.addObservers();
      this.fire(this.target, JSGestureRecognizerStatePossible, this);
    }
  },
  
  touchmove: function(event) {},
  touchend: function(event) {},
  touchcancelled: function(event) {},
  
  
  // -- Event Handlers --------------------------------------------------------
  possible: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStatePossible;
      if (this.callback && this.enabled) {
        this.callback(this);
      }
    }
  },
  
  began: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStateBegan;
      if (this.callback && this.enabled) {
        this.callback(this);
      }
    }
  },
  
  ended: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this && this.state != JSGestureRecognizerStateEnded) {
      this.state = JSGestureRecognizerStateEnded;
      this.removeObservers();
      this.reset();

      if (this.callback && this.enabled) {
        this.callback(this);
      }
    }
  },
  
  cancelled: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStateCancelled;
      if (this.callback && this.enabled) {
        this.callback(this);
      }
      this.removeObservers();
      this.reset();
    }
  },
  
  failed: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStateFailed;
      if (this.callback && this.enabled) {
        this.callback(this);
      }
      this.removeObservers();
      this.reset();
    }
  },
  
  changed: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStateChanged;
      if (this.callback && this.enabled) {
        this.callback(this);
      }
    }
  },
  
  addObservers: function() {
    this.observe(document, JSTouchMove, this.touchmoveHandler);
    this.observe(document, JSTouchEnd, this.touchendHandler);
  },
  
  removeObservers: function() {
    this.stopObserving(document, JSTouchMove, this.touchmoveHandler);
    this.stopObserving(document, JSTouchEnd, this.touchendHandler);
  },

  preventIfNotSynthesized: function(cb){
    return function(ev){
      if((!JSTouchRecognizer.preventNaturalTouch || ev.originalEvent._synthesized) && cb){
        return cb(ev);
      }
    };
  },
  
  addSelectedClass: function(target, eventName){
	  if(this.enabled && (!this.addsSelectedClass || (this.addsSelectedClassOnlyToSubTarget && !this.subTarget))) return;
			
		var targetToUse = this.getTarget();
	  
    	if((eventName == JSGestureRecognizerStateBegan || eventName == JSGestureRecognizerStateChanged) && !this.trackingSelected){
			this.trackingSelected = true;
			var self = this;
			
			var addClassFunc = function(){
				if(self.trackingSelected){
					targetToUse.addClass("selected");
				}
			}
			
			if (this.shouldNotDelaySelectClass) // for buttons we highlight immediately 
				addClassFunc();
			else
				setTimeout(addClassFunc, 100);			
				
		}else if(eventName == JSGestureRecognizerStateRecognized){
			targetToUse.addClass("selected");
			this.trackingSelected = true;
		}else{
			if (targetToUse.hasClass('selected')){
				targetToUse.removeClass("selected"); // we should only call this if have to as it causes lag when scrolling (due to restyling/repainting)
			}
			this.trackingSelected = false;
		}
  },
  
  getTarget: function(){
  	return this.subTarget ? this.subTarget : $(this.target);
  },
  
  // -- Utility methods -------------------------------------------------------
  // make this section library independent
  fire: function(target, eventName, obj) {
	  this.addSelectedClass(target, eventName); // THIS MUST ALWAYS GO BEFORE THE EVENT TRIGGER. IT CANNOT GO AFTER. THIS IS EXTREMELY IMPORTANT.

      if (Framework.Prototype) {
        Event.fire(target, eventName, obj);
      } else if (Framework.jQuery) {
        $(target).trigger(eventName, obj);
      }
  },
  
  observe: function(target, eventName, handler) {
    if (Framework.Prototype) {
      target.observe(eventName, handler);
    } else if (Framework.jQuery) {
      $(target).bind(eventName, handler);
    }
  },
  
  stopObserving: function(target, eventName, handler) {
    if (Framework.Prototype) {
      target.stopObserving(eventName, handler);
    } else if (Framework.jQuery) {
      $(target).unbind(eventName, handler);
    }
  },
  
  getEventPoint: function(event) {
    if (MobileSafari)
      return { x: event.targetTouches[0].pageX, y: event.targetTouches[0].pageY };
    if (Framework.Prototype) return Event.pointer(event);
    if (Framework.jQuery) return { x: event.pageX, y: event.pageY };
  }
});

// -- Class Methods -----------------------------------------------------------
JSTouchRecognizer.addGestureRecognizer = function(target, gestureRecognizer) {
  gestureRecognizer.setTarget(target);
}
// -- Abstract Class: JSGestureRecognizer -------------------------------------
var JSGestureRecognizer = JSTouchRecognizer.extend({
  numberOfTouchesAllowed:  1,
  
  toString: function() {
    return "JSGestureRecognizer";
  },
  
  // -- Subclass Implementation Methods ---------------------------------------
  // initRecognizer - called when target and action are set (init)
  initRecognizer: function() {
    this._super();

    this.gesturechangeHandler = this.gesturechange.bind(this);
    this.gestureendHandler = this.gestureend.bind(this);

    this.observe(this.target, JSTouchStart, this.gesturestart.bind(this));
  },
	reset: function(){
	    this.observe(this.target, JSTouchStart);
	},
  
  
  // -- Gesture Events --------------------------------------------------------
  gesturestart: function(event) {
    if (this.target && window.isDescendentOf(event.target, this.target) && event.touches.length <= this.numberOfTouchesAllowed) {
      this.addGestureObservers();
      this.fire(this.target, JSGestureRecognizerStatePossible, this);
    }
  },
  
  gesturechange: function(event) { },
  gestureend: function(event) {
    if (this.target && window.isDescendentOf(event.target, this.target)) {
      this.fire(this.target, JSGestureRecognizerStateEnded, this);
    }
  },
  
  
  // -- Event Handlers --------------------------------------------------------
  ended: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStateEnded;
      if (this.callback) {
        this.callback(this);
      }
      this.removeObservers();
      this.removeGestureObservers();
      this.reset();
    }
  },
  
  cancelled: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStateCancelled;
      if (this.callback) {
        this.callback(this);
      }
      this.removeObservers();
      this.removeGestureObservers();
      this.reset();
    }
  },
  
  failed: function(event, memo) {
    if (!event.memo) event.memo = memo;
    if (event.memo == this) {
      this.state = JSGestureRecognizerStateFailed;
      if (this.callback) {
        this.callback(this);
      }
      this.removeObservers();
      this.removeGestureObservers();
      this.reset();
    }
  },
  
  addGestureObservers: function() {
    this.observe(document, JSGestureChange, this.gesturechangeHandler);
    this.observe(document, JSGestureEnd, this.gestureendHandler);
  },
  
  removeGestureObservers: function() {
    this.stopObserving(document, JSGestureChange, this.gesturechangeHandler);
    this.stopObserving(document, JSGestureEnd, this.gestureendHandler);
  }
});
var JSTapGestureRecognizer = JSTouchRecognizer.extend({
  addsSelectedClass:       true,
  numberOfTapsRequired:    1,
  numberOfTouchesRequired: 1,
  moveTolerance:           5,
  cancelIfMoved:		   false, // moveTolerance only works if this is enabled
  maximumPressDuration:    0xFFFFFFFF, // taps should never time out
  blockScrollingOnFail:    false,  
  
  toString: function() {
    return "JSTapGestureRecognizer";
  },
  
  touchstart: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
    	
      this.lastEvent = event;
//      event.preventDefault();
      this._super(event);
      this.numberOfTouches = window.getAllTouches(event).length;
      this.distance = 0;
      this.translationOrigin = this.getEventPoint(event);
      
      if (this.maximumPressDuration && this.numberOfTouchesRequired == window.getAllTouches(event).length) 
      {
      	this.startTime = new Date();
      }

      this.fire(this.target, JSGestureRecognizerStateBegan, this);
    }
  },
  
  touchmove: function(event) {
    // move events fire even if there's no move on desktop browsers
    // the idea of a "tap" with mouse should ignore movement anyway...
//    if (window.isDescendentOf(event.target, this.target) && MobileSafari) {
//    }

      this.lastEvent = event;
      
	if (event.touches.length > this.numberOfTouchesAllowed)
	{	
		if(this.blockScrollingOnFail){
     		//event.preventDefault();
		}
		this.fire(this.target, JSGestureRecognizerStateFailed, this);
		return;
	}

    var p = this.getEventPoint(event);
    var dx = p.x - this.translationOrigin.x,
        dy = p.y - this.translationOrigin.y;
    this.distance = Math.sqrt(dx*dx + dy*dy);
    if (this.distance > this.moveTolerance) 
    {
    	if (this.cancelIfMoved)
			this.fire(this.target, JSGestureRecognizerStateFailed, this);

		if(this.blockScrollingOnFail)
		{
			// event.preventDefault();
		}
		
      this.removeObservers();
      this.fire(this.target, JSGestureRecognizerStateFailed, this);
    }
  },
  
  touchend: function(event) {
      this.lastEvent = event;
      
    if (window.isDescendentOf(event.target, this.getTarget()[0]) && (this.state == JSGestureRecognizerStatePossible || this.state == JSGestureRecognizerStateBegan)) {    	
      if (this.numberOfTouches == this.numberOfTouchesRequired) {
        this._super(event);
        this.taps++;
		
		// make sure we haven't exceeded the maxPress time
		if (this.startTime)
		{
			var timeSpent = (new Date()).getTime() - this.startTime.getTime();
			if ( timeSpent > this.maximumPressDuration)
			{
          		this.fire(this.target, JSGestureRecognizerStateFailed, this);				
          		return;
			}
		}
		
		if (this.taps == this.numberOfTapsRequired) {
      this.addSelectedClass(this.target, JSGestureRecognizerStateRecognized);
      setTimeout(function(){
        this.fire(this.target, JSGestureRecognizerStateRecognized, this);
      }.bind(this), 1)
		  //event.preventDefault(); // commenting out because this breaks deselecting of text in the reader [#1118]
        } else {
        	if (this.recognizerTimer) {
          		window.clearTimeout(this.recognizerTimer);
          		this.recognizerTimer = null;
        	}
        	this.recognizerTimer = window.setTimeout(function() {
          		if (this.taps == this.numberOfTapsRequired) {
                this.addSelectedClass(this.target, JSGestureRecognizerStateRecognized);
                setTimeout(function(){
                  this.fire(this.target, JSGestureRecognizerStateRecognized, this);
                }.bind(this), 1)
          		} else {
            		this.fire(this.target, JSGestureRecognizerStateFailed, this);
          		}
        	}.bind(this), JSTapGestureRecognizer.TapTimeout);
      	}
    } else {
      this.fire(this.target, JSGestureRecognizerStateFailed, this);
    }
	}
  },
  
  reset: function() {
    this.taps = 0;
    this.recognizerTimer = null;
	this.trackingSelected = false;
	if ($(this.target).hasClass('selected'))
		$(this.target).removeClass("selected"); // we should only call this if have to as it causes lag when scrolling (due to restyling/repainting)
  }
});

JSTapGestureRecognizer.TapTimeout = 500;
var JSLongPressGestureRecognizer = JSGestureRecognizer.extend({
  addsSelectedClass:       true,
  minimumPressDuration:    400,
  numberOfTouchesRequired: 1,
  numberOfTapsRequired:    1, // not sure how this works with multiple taps
  allowableMovement:       10, // currently we ignore this
  
  init: function(){
	  if(app.metadata.longPressTimeout){
		  app.log("DEVICE SPECIFIC LONG PRESS TIMEOUT " + app.metadata.longPressTimeout);
		  this.minimumPressDuration = app.metadata.longPressTimeout;
	  }
  },
  
  toString: function() {
    return "JSLongPressGestureRecognizer";
  },
  
  touchstart: function(event) {
      this.lastEvent = event;
      
  	if (this.maximumPressDuration < this.minimumPressDuration)
  		this.maximumPressDuration = false; // remove the default max detection
  
    if (window.isDescendentOf(event.target, this.target)) {
//      event.preventDefault();
      this._super(event);
      if (this.numberOfTouchesRequired == window.getAllTouches(event).length) {
        this.recognizerTimer = window.setTimeout(function() {
          this.fire(this.target, JSGestureRecognizerStateRecognized, this);
	      blockScrollingUntilReleased();
        }.bind(this), this.minimumPressDuration);
      }

      this.fire(this.target, JSGestureRecognizerStateBegan, this);
    }
  },
  
  touchmove: function(event) {
      this.lastEvent = event;
      
    if (window.isDescendentOf(event.target, this.target) && MobileSafari && (this.state == JSGestureRecognizerStatePossible || this.state == JSGestureRecognizerStateBegan)) {
      this.fire(this.target, JSGestureRecognizerStateFailed, this);
	}
  },
  
  touchend: function(event) {
      this.lastEvent = event;
      
    if (window.isDescendentOf(event.target, this.target)) {
      event.preventDefault(); 
      this.fire(this.target, JSGestureRecognizerStateFailed, this);
    }
  },
  
  gesturestart: function(event) {
      this.lastEvent = event;
      
    if (window.isDescendentOf(event.target, this.target)) {
      this.fire(this.target, JSGestureRecognizerStateFailed);
    }
  },
  
  reset: function() {
    if (this.recognizerTimer) {
      window.clearTimeout(this.recognizerTimer);
    }
    this.recognizerTimer = null;
  }
});
var JSPanGestureRecognizer = JSGestureRecognizer.extend({
  maximumNumberOfTouches: 100000,
  minimumNumberOfTouches: 1,
  
  toString: function() {
    return "JSPanGestureRecognizer";
  },
  
  touchstart: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
      this._super(event);
      var allTouches = window.getAllTouches(event);
      if (allTouches.length > this.maximumNumberOfTouches ||
          allTouches.length < this.minimumNumberOfTouches) {
			  app.log("bailing touchstart");
        this.touchend(event);
      }
    }
  },
  
  touchmove: function(event) {
    var allTouches = window.getAllTouches(event);
    if (allTouches.length >= this.minimumNumberOfTouches &&
        allTouches.length <= this.maximumNumberOfTouches) {
      if (MobileSafari) {
        if (!isDescendentOf(event.target, this.target)) {
		  app.log("bailing touchmove " + event.target);
          this.touchend(event);
          return;
        }
      }
      event.preventDefault();
      if (this.beganRecognizer == false) {
        this.fire(this.target, JSGestureRecognizerStateBegan, this);
        this.beganRecognizer = true;
        this.translationOrigin = this.getEventPoint(event);
      } else {
        this.fire(this.target, JSGestureRecognizerStateChanged, this);
        var p = this.getEventPoint(event);
        
        this.velocity.x = p.x - this.translation.x;
        this.velocity.y = p.y - this.translation.y;

		app.log("translation.x = " + this.translation.x + " (" + p.x + " - " + this.translationOrigin.x + ") = " + (this.translation.x + (p.x - this.translationOrigin.x)));
        this.translation.x = p.x - this.translationOrigin.x;
        this.translation.y = p.y - this.translationOrigin.y;
      }
    }
  },
  
  touchend: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
      this._super(event);
      if (this.beganRecognizer) {
        this.fire(this.target, JSGestureRecognizerStateEnded, this);
      } else {
		  app.log("bailing touchend");
        this.fire(this.target, JSGestureRecognizerStateFailed, this);
      }
    }
  },
  
  gesturestart: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
      var allTouches = window.getAllTouches(event);
      if (allTouches.length > this.maximumNumberOfTouches) {
        this.fire(this.target, JSGestureRecognizerStateFailed, this);
      }
    }
  },
  
  reset: function() {
    this.beganRecognizer = false;
    this.translation = { x: 0, y: 0 };
    this.velocity = { x: 0, y: 0 };
  },
  
  setTranslation: function(translation) {
    if (typeof translation.x != 'undefined' &&
        typeof translation.y != 'undefined') {
      this.translation = { x: translation.x, y: translation.y };
    }
  }
});
var JSPinchGestureRecognizer = JSGestureRecognizer.extend({
  toString: function() {
    return "JSPinchGestureRecognizer";
  },
  
  gesturestart: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
      var allTouches = window.getAllTouches(event);
      if (allTouches.length == 2) {
        event.preventDefault();
        this._super(event);
      }
    }
  },
  
  gesturechange: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
      event.preventDefault();
      if (this.beganRecognizer == false) {
        this.fire(this.target, JSGestureRecognizerStateBegan, this);
        this.beganRecognizer = true;
      } else {
        this.fire(this.target, JSGestureRecognizerStateChanged, this);
        this.velocity = event.scale / this.scale;
        this.scale *= event.scale;
      }
    }
  },
  
  // seems like if this isn't included jQuery doesn't run gestureend
  gestureend: function(event) {
    this._super(event);
  },
  
  reset: function() {
    this.beganRecognizer = false;
    this.scale = 1;
    this.velocity = 0;
  },
  
  setScale: function(scale) {
    if (typeof scale == 'number') {
      this.scale = scale;
    }
  }
});
var JSRotationGestureRecognizer = JSGestureRecognizer.extend({
  toString: function() {
    return "JSRotationGestureRecognizer";
  },
  
  gesturestart: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
      var allTouches = window.getAllTouches(event);
      if (allTouches.length == 2) {
        event.preventDefault();
        this._super(event);
      }
    }
  },
  
  gesturechange: function(event) {
    if (window.isDescendentOf(event.target, this.target)) {
      event.preventDefault();
      if (this.beganRecognizer == false) {
        this.fire(this.target, JSGestureRecognizerStateBegan, this);
        this.beganRecognizer = true;
      } else {
        this.fire(this.target, JSGestureRecognizerStateChanged, this);
        this.velocity = event.rotation - this.rotation;
        this.rotation += event.rotation;
      }
    }
  },
  
  // seems like if this isn't included jQuery doesn't run gestureend
  gestureend: function(event) {
    this._super(event);
  },
  
  reset: function() {
    this.beganRecognizer = false;
    this.rotation = 0;
    this.velocity = 0;
  },
  
  setRotation: function(rot) {
    if (typeof rot == 'number') {
      this.rotation = rot;
    }
  }
});
var JSSwipeGestureRecognizer = JSGestureRecognizer.extend({
  numberOfTouchesRequired: 1,
  direction:               JSSwipeGestureRecognizerDirectionRight,
  minimumDistance:         50,
  maximumAlternateDistance:20,
  
  toString: function() {
    return "JSSwipeGestureRecognizer";
  },
  
  lastSwipeDirection: function(){
      if ((this.direction & JSSwipeGestureRecognizerDirectionRight) && (this.distance.x > this.minimumDistance)) {
		  return JSSwipeGestureRecognizerDirectionRight;
	  }else if((this.direction & JSSwipeGestureRecognizerDirectionLeft) && (this.distance.x < -this.minimumDistance)){
		  return JSSwipeGestureRecognizerDirectionLeft;
	  }else if((this.direction & JSSwipeGestureRecognizerDirectionDown) && (this.distance.y > this.minimumDistance)){
		  return JSSwipeGestureRecognizerDirectionDown;
	  }else if((this.direction & JSSwipeGestureRecognizerDirectionUp) && (this.distance.y < -this.minimumDistance)){
		  return JSSwipeGestureRecognizerDirectionUp;
	  }else{
		  return undefined;
	  }
  },
  
  touchstart: function(event) {
      this.lastEvent = event;
      
    var allTouches = window.getAllTouches(event);
    if ((window.isDescendentOf(event.target, this.target) && this.numberOfTouchesRequired == allTouches.length) && this.numberOfTouchesRequired == allTouches.length) {
//        event.preventDefault();
        this._super(event);
		this.startingScrollY = window.pageYOffset; // track starting scroll position for scroll check in touchmove
        this.startingPos = { x: allTouches[0].pageX, y: allTouches[0].pageY };
        this.distance = { x: 0, y: 0 };
        this.didStart = true; // prevents the swipe from occuring if the gesture was added while the finger was still down
      this.fire(this.target, JSGestureRecognizerStateBegan, this);
    } else {
      this.fire(this.target, JSGestureRecognizerStateFailed, this);
    }
  },
  
  touchmove: function(event) {
      this.lastEvent = event;
      
    if (window.isDescendentOf(event.target, this.target) && (this.state == JSGestureRecognizerStatePossible || this.state == JSGestureRecognizerStateBegan) && event.touches.length <= this.numberOfTouchesAllowed && this.didStart)
    {
    
    	// If the scroll position has changed it means they have started scrolling and therefore the gesture should not longer be able to fire.
    	// A swipe motion is fast and direct enough that if it is not successful before the scroll view has taken over the touch we should stop trying to process it.
    	// This logic is taken from the swipe gesture recognizer that was built into the RIL 2.0 iPhone app
    	if (this.startingScrollY != window.pageYOffset)
    	{
			this.fire(this.target, JSGestureRecognizerStateFailed, this);
    		return;
    	}
    
      var allTouches = window.getAllTouches(event);
      this.distance.x = allTouches[0].pageX - this.startingPos.x;
      this.distance.y = allTouches[0].pageY - this.startingPos.y;

      if (this.direction & JSSwipeGestureRecognizerDirectionRight) {
	    if (Math.abs(this.distance.y) > this.maximumAlternateDistance){
          this.fire(this.target, JSGestureRecognizerStateFailed, this);
		}else 
        if (this.distance.x > this.minimumDistance) {
          this.fire(this.target, JSGestureRecognizerStateRecognized, this);
		  event.preventDefault();
        }
      }

      if (this.direction & JSSwipeGestureRecognizerDirectionLeft) {
	    if (Math.abs(this.distance.y) > this.maximumAlternateDistance){
          this.fire(this.target, JSGestureRecognizerStateFailed, this);
		}else 
        if (this.distance.x < -this.minimumDistance) {
          this.fire(this.target, JSGestureRecognizerStateRecognized, this);
		  event.preventDefault();
        }
      }

      if (this.direction & JSSwipeGestureRecognizerDirectionUp) {
	    if (Math.abs(this.distance.x) > this.maximumAlternateDistance){
          this.fire(this.target, JSGestureRecognizerStateFailed, this);
		}else 
        if (this.distance.y < -this.minimumDistance) {
          this.fire(this.target, JSGestureRecognizerStateRecognized, this);
		  event.preventDefault();
        }
      }

      if (this.direction & JSSwipeGestureRecognizerDirectionDown) {
	    if (Math.abs(this.distance.x) > this.maximumAlternateDistance){
          this.fire(this.target, JSGestureRecognizerStateFailed, this);
		}else 
        if (this.distance.y > this.minimumDistance) {
          this.fire(this.target, JSGestureRecognizerStateRecognized, this);
		  event.preventDefault();
        }
      }
    }else if(this.state == JSGestureRecognizerStateEnded){
	  event.preventDefault();
	}
  },

  touchend: function(event) {
      this.lastEvent = event;
      this.didStart = false;
      
    if (window.isDescendentOf(event.target, this.target)) {
      this.fire(this.target, JSGestureRecognizerStateFailed, this);
    }
  }
});
var JSGestureView = Class.extend({
  init: function(element) {
    this.view = (typeof element == 'string') ? document.getElementById(element) : element;
    this.scale = 1;
    this.rotation = this.x = this.y = this.z = 0;
    // Nate 2011-11-25 : Commenting this out because it causes a flicker in the reader view, not sure why it is needed:
    //this.setTransform({}); // forces mobile safari to set object as accelerated.
  },
  
  setTransform: function(obj) {
    this._x        = this.getDefined(obj.x,        this._x,        this.x);
    this._y        = this.getDefined(obj.y,        this._y,        this.y);
    this._z        = this.getDefined(obj.z,        this._z,        this.z);
    this._scale    = this.getDefined(obj.scale,    this._scale,    this.scale);
    this._rotation = this.getDefined(obj.rotation, this._rotation, this.rotation);
    this.view.style.webkitTransform = 'translate3d('+
      this._x+'px, '+this._y+'px, '+this._z+') '+
      'scale('+this._scale+') '+
      'rotate('+this._rotation+'deg)';
  },
  
  addGestureRecognizer: function(recognizer) {
    JSTouchRecognizer.addGestureRecognizer(this, recognizer);
  },
  
  getDefined: function() {
    for (var i = 0; i < arguments.length; i++) {
      if (typeof arguments[i] != 'undefined') return arguments[i];
    }
    return arguments[arguments.length-1];
  }
});

JSGestureView.viewForTarget = function(target){
	var key = "JSGestureView.viewForTarget";
	var view = $.data(target, key);
	if(!view){
		view = new JSGestureView(target);
		$.data(target, key, view);
	}
	return view;
};

  w.JSTouchRecognizer            = JSTouchRecognizer;
  w.JSGestureRecognizer          = JSGestureRecognizer;
  w.JSLongPressGestureRecognizer = JSLongPressGestureRecognizer;
  w.JSPanGestureRecognizer       = JSPanGestureRecognizer;
  w.JSPinchGestureRecognizer     = JSPinchGestureRecognizer;
  w.JSRotationGestureRecognizer  = JSRotationGestureRecognizer;
  w.JSSwipeGestureRecognizer     = JSSwipeGestureRecognizer;
  w.JSTapGestureRecognizer       = JSTapGestureRecognizer;
  
  w.JSGestureView       = JSGestureView;
})(window);


// this prevents scrolling until the user lifts their finger
// used after a gesture is triggered (like swipe or long press)
var haveCreatedBlockEvents = false;
var gestureBlockScrolling = false;
function blockScrollingUntilReleased()
{
	if (!haveCreatedBlockEvents)
	{
		var element = $('#container');
		
		element.bind("touchmove", function(e)
		{
			if (gestureBlockScrolling)
				e.preventDefault();
		});
		element.bind("touchend", function(e){		gestureBlockScrolling = false;	});
		element.bind("touchcancelled", function(e){	gestureBlockScrolling = false;	});
	}

	gestureBlockScrolling = true;
}

(function($){  
	$.fn.tap = function(handler){
		return this.each(function(){
			var $this = $(this);
			$this.bind('tap', handler);
			
			var tapGestureRecognizer = $.data(this, "shared-JSTapGestureRecognizer");
			if(!tapGestureRecognizer){
				tapGestureRecognizer = new JSTapGestureRecognizer();
				tapGestureRecognizer.cancelIfMoved = true;
				tapGestureRecognizer.shouldNotDelaySelectClass = true;
				tapGestureRecognizer.initWithCallback(function(gesture){
					if(gesture.state == JSGestureRecognizerStateRecognized){
						$this.trigger("tap", [gesture]);
					}
				});
				$.data(this, "shared-JSTapGestureRecognizer", tapGestureRecognizer);
				
				var view = JSGestureView.viewForTarget(this);
				view.addGestureRecognizer(tapGestureRecognizer);
			}
		});
	};
})(jQuery);