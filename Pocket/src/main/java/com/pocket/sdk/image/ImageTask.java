package com.pocket.sdk.image;

import com.pocket.util.android.thread.TaskRunnable;

/**
 * A {@link TaskRunnable} that prioritizes requests so that images that the user is most likely looking at
 * get prioritized over requests that are for caching or may no longer be needed such as having gone off-screen.
 */
abstract class ImageTask extends TaskRunnable {
	
	private final Image.Request request;
	
	ImageTask(Image.Request request) {
		this.request = request;
	}
	
	@Override
	public int getPriority() {
		if (request.callback == null || !request.returnBitmap) {
			return TaskRunnable.PRIORITY_LOW;
			
		} else if (request.callback.isImageRequestStillValid(request)) {
			return TaskRunnable.PRIORITY_HIGH;
			
		} else {
			return TaskRunnable.PRIORITY_NORMAL;
		}
	}
	
}