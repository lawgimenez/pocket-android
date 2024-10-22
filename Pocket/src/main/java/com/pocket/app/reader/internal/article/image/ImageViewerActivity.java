package com.pocket.app.reader.internal.article.image;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.ideashower.readitlater.R;
import com.pocket.app.settings.Theme;
import com.pocket.sdk.api.generated.enums.CxtView;
import com.pocket.sdk.api.generated.thing.Image;
import com.pocket.sdk.util.AbsPocketActivity;
import com.pocket.sync.value.Parceller;
import com.pocket.ui.view.AppBar;
import com.pocket.util.android.drawable.ShadowUtil;

import java.util.ArrayList;
import java.util.List;

public class ImageViewerActivity extends AbsPocketActivity implements ImageViewer.OnImageChangeListener {

	public static final String EXTRA_IMAGES = "com.ideashower.readitlater.extras.images";
	public static final String EXTRA_START_IMAGE_ID =
			"com.ideashower.readitlater.extras.start_image_id";

	private AppBar appbar;
	private ImageViewer viewer;
	private View overlay;
	private View leftArrow;
	private View rightArrow;
	private TextView caption;
	
	private ArrayList<Image> images;
	
	@Override
	public CxtView getActionViewName() {
		return CxtView.READER_IMAGE_VIEWER;
	}
		
	@Override
	protected ActivityAccessRestriction getAccessType() {
		return ActivityAccessRestriction.ALLOWS_GUEST;
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); 
		setContentView(R.layout.activity_image_viewer);

		appbar = findViewById(R.id.appbar);
		appbar.bind()
				.divider(false)
				.onLeftIconClick(v -> finish())
				.leftIcon(com.pocket.ui.R.drawable.ic_pkt_close_x_circle, com.pocket.ui.R.string.ic_close);
		appbar.setBackground(null);

		viewer = findViewById(R.id.image_viewer);

		caption = findViewById(R.id.caption);
		
		leftArrow = findViewById(R.id.arrow_left);
		leftArrow.setOnClickListener(v -> viewer.move(-1));
		
		rightArrow = findViewById(R.id.arrow_right);
		rightArrow.setOnClickListener(v -> viewer.move(1));
		
		viewer.setOnImageChangeListener(this);
		overlay = findViewById(R.id.overlay);
		
		int startImageId = 0;
		Intent intent = getIntent();
		if (intent != null){
			images = Parceller.getList(intent, EXTRA_IMAGES, Image.JSON_CREATOR);
			startImageId = intent.getIntExtra(EXTRA_START_IMAGE_ID, 1);
		}
		if (images == null && savedInstanceState != null){
			images = Parceller.getList(savedInstanceState, EXTRA_IMAGES, Image.JSON_CREATOR);
			startImageId = savedInstanceState.getInt(EXTRA_START_IMAGE_ID, 1);
		}
		if (startImageId < 1) {
			startImageId = 1;
		}
		if (images != null){
			viewer.setImages(images, startImageId);
			
			int arrowVisibility = images.size() > 1 ? View.VISIBLE : View.GONE;
			leftArrow.setVisibility(arrowVisibility);
			rightArrow.setVisibility(arrowVisibility);
			
			updateUI();
			
			showOverlay();
			
			ShadowUtil.setShadowLayer(caption, 8f, 0, -1, ContextCompat.getColor(this, com.pocket.ui.R.color.black)); // this Activity only supports dark mode
		} else {
			finish();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (images != null) {
			Parceller.put(outState, EXTRA_IMAGES, images);
			outState.putInt(EXTRA_START_IMAGE_ID, viewer.getCurrentImageIndex());
		}
		super.onSaveInstanceState(outState);
	}
	
	private void updateUI() {
		Image image = viewer.getCurrentImage();
		String caption = image == null ? null : image.caption;
		this.caption.setText(caption);
		this.caption.setVisibility(caption == null ? View.GONE : View.VISIBLE);
		leftArrow.setEnabled(viewer.canGo(-1));
		rightArrow.setEnabled(viewer.canGo(1));
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (viewer != null) {
			viewer.onDestroy();
		}
	}

	@Override
	public void onChange() {
		updateUI();
	}

	public static void open(Activity activity, List<Image> images, int startingImageId){
		Intent intent = new Intent(activity, ImageViewerActivity.class);
		Parceller.put(intent, ImageViewerActivity.EXTRA_IMAGES, images);
		intent.putExtra(ImageViewerActivity.EXTRA_START_IMAGE_ID, startingImageId);
		activity.startActivity(intent);
	}

	@Override
	protected int getDefaultThemeFlag() {
		return Theme.FLAG_ONLY_DARK;
	}

	@Override
	public void onClick() {
		if (overlay.getVisibility() == View.GONE) {
			showOverlay();
		} else {
			hideOverlay();
		}
	}

	private void showOverlay() {
		overlay.setVisibility(View.VISIBLE);
		overlay.setAlpha(0f);
		overlay.animate().alpha(1f).setListener(null).start();
	}

	private void hideOverlay() {
		overlay.animate().alpha(0f).setListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				overlay.setVisibility(View.GONE);
			}
		}).start();
	}

}
