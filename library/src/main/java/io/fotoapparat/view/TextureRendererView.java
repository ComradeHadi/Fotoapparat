package io.fotoapparat.view;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.TextureView;
import android.widget.FrameLayout;

import java.util.concurrent.CountDownLatch;

import io.fotoapparat.hardware.CameraDevice;
import io.fotoapparat.parameter.RendererParameters;
import io.fotoapparat.parameter.Size;

/**
 * Uses {@link android.view.TextureView} as an output for camera.
 */
class TextureRendererView extends FrameLayout implements CameraRenderer {

	private final CountDownLatch textureLatch = new CountDownLatch(1);

	private SurfaceTexture surfaceTexture;
	private TextureView textureView;

	private Size previewSize = null;

	public TextureRendererView(@NonNull Context context) {
		super(context);

		init();
	}

	public TextureRendererView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);

		init();
	}

	public TextureRendererView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		init();
	}

	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	public TextureRendererView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);

		init();
	}

	private void init() {
		textureView = new TextureView(getContext());
		tryToInitializeSurfaceTexture(textureView);

		addView(textureView);
	}

	private void tryToInitializeSurfaceTexture(TextureView textureView) {
		surfaceTexture = textureView.getSurfaceTexture();
		if (surfaceTexture == null) {
			textureView.setSurfaceTextureListener(new TextureListener());
		}
	}

	@Override
	public void attachCamera(CameraDevice camera) {
		awaitSurfaceTexture();
		updateLayout(camera);

		camera.setDisplaySurface(textureView);
	}

	private void updateLayout(CameraDevice camera) {
		final Size previewSize = toPreviewSize(
				camera.getRendererParameters()
		);

		post(new Runnable() {
			@Override
			public void run() {
				TextureRendererView.this.previewSize = previewSize;

				requestLayout();
			}
		});
	}

	private Size toPreviewSize(RendererParameters rendererParameters) {
		return rendererParameters.frameRotation == 0 || rendererParameters.frameRotation == 180
				? rendererParameters.previewSize
				: rendererParameters.previewSize.flip();
	}

	private void awaitSurfaceTexture() {
		if (surfaceTexture != null) {
			return;
		}

		try {
			textureLatch.await();
		} catch (InterruptedException e) {
			// Do nothing
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		if (previewSize == null) {
			super.onLayout(changed, left, top, right, bottom);
			return;
		}

		final float scale = Math.max(
				getMeasuredWidth() / (float) previewSize.width,
				getMeasuredHeight() / (float) previewSize.height
		);

		final int width = (int) (previewSize.width * scale);
		final int height = (int) (previewSize.height * scale);

		final int extraX = Math.max(0, width - getMeasuredWidth());
		final int extraY = Math.max(0, height - getMeasuredHeight());

		getChildAt(0).layout(
				-extraX / 2,
				-extraY / 2,
				width - (extraX / 2),
				height - (extraY / 2)
		);
	}

	private class TextureListener implements TextureView.SurfaceTextureListener {

		@Override
		public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
			surfaceTexture = surface;
			textureLatch.countDown();
		}

		@Override
		public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
			// Do nothing
		}

		@Override
		public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
			// Do nothing

			return true;
		}

		@Override
		public void onSurfaceTextureUpdated(SurfaceTexture surface) {
			// Do nothing
		}
	}

}
