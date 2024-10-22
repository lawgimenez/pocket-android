package com.pocket.sdk.util.activity;

import android.os.Bundle;

import com.ideashower.readitlater.R;
import com.pocket.ui.view.AppBar;

public class FramedWebViewActivity extends BasicWebViewActivity {

	public static final String EXTRA_TITLE = "com.ideashower.readitlater.extra.title";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AppBar appBar = findViewById(R.id.appbar);
		appBar.bind()
				.withUpArrow()
				.onLeftIconClick(v -> finish())
				.title(getIntent().getStringExtra(EXTRA_TITLE));
	}

	@Override
	protected int getLayoutId() {
		return R.layout.activity_web;
	}

	@Override
	protected int getWebViewResId(){
		return R.id.webview;
	}
}