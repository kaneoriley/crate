/*
 * Copyright (C) 2016 Kane O'Riley
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.oriley.cratesample.fragments;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.*;
import android.view.TextureView.SurfaceTextureListener;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import butterknife.BindView;
import me.oriley.crate.Crate;
import me.oriley.crate.VideoAsset;
import me.oriley.cratesample.R;
import me.oriley.cratesample.listeners.DebouncingClickListener;
import me.oriley.cratesample.loaders.CrateVideoLoader;
import me.oriley.cratesample.simple.SimpleAnimatorListener;
import me.oriley.cratesample.simple.SimpleSurfaceTextureListener;
import me.oriley.cratesample.widget.CrateCardViewHolder;
import me.oriley.cratesample.widget.CrateVideoInfoView;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class VideoListFragment extends RecyclerViewFragment {

    @SuppressWarnings("unused")
    private static final String TAG = VideoListFragment.class.getSimpleName();

    private static final int PROGRESS_FADE_ANIM_MILLIS = 300;
    private static final int VIDEO_RESIZE_ANIM_MILLIS = 500;
    private static final int DEBOUNCE_MILLIS = 1000;

    @NonNull
    private final SurfaceTextureListener mListener = new SimpleSurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurface = surface;
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            mSurface = null;
            return false;
        }
    };

    @NonNull
    private final ScheduledExecutorService mExecutorService = Executors.newScheduledThreadPool(1);

    @BindView(R.id.progress_bar_container)
    FrameLayout mProgressBarContainer;

    @BindView(R.id.card_view_container)
    FrameLayout mCardViewContainer;

    @BindView(R.id.card_view)
    CardView mCardView;

    @BindView(R.id.video_view)
    TextureView mTextureView;

    @NonNull
    private CrateVideoLoader mLoader;

    @Nullable
    private SurfaceTexture mSurface;

    @Nullable
    private MediaPlayer mPlayer;

    @Nullable
    private VideoAsset mAsset;

    private int mVideoViewWidth;

    private int mVideoViewHeight;

    private boolean mShowing;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLoader = new CrateVideoLoader(mCrate, 0);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setHasFixedSize(true);
        mTextureView.setSurfaceTextureListener(mListener);
        mShowing = getUserVisibleHint();
        mCardViewContainer.setAlpha(0f);
        updateView();
    }

    @NonNull
    @Override
    public CrateAdapter getAdapter() {
        return new CrateVideoRecyclerAdapter(this, mCrate);
    }

    @NonNull
    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    @Override
    public void onResume() {
        super.onResume();
        final VideoAsset asset = mAsset != null ? mAsset : mCrate.assets.videos.LIST.get(0);

        mCardViewContainer.post(new Runnable() {
            @Override
            public void run() {
                Point point = getDesiredVideoSize(asset.getWidth(), asset.getHeight());
                if (point.x > 0 && point.y > 0) {
                    setCardLayoutParams(point.x, point.y);
                }
                loadVideoIfReady(asset);
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mProgressBarContainer.setAlpha(1f);
        mExecutorService.submit(new Runnable() {
            @Override
            public void run() {
                disposeVideo();
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (mShowing != isVisibleToUser) {
            mShowing = isVisibleToUser;
            updateView();
        }
    }

    public void setMediaPlayer(@NonNull MediaPlayer mediaPlayer) {
        if (mPlayer != mediaPlayer) {
            mPlayer = mediaPlayer;
            if (mSurface != null) {
                mPlayer.setSurface(new Surface(mSurface));
            }
            mCardViewContainer.post(new Runnable() {
                @Override
                public void run() {
                    playVideo(mPlayer);
                }
            });
        }
    }

    private void updateView() {
        if (mRecyclerView == null) {
            // TOO EARLY
            return;
        }

        if (mShowing) {
            Activity activity = getActivity();
            mRecyclerView.setLayoutManager(new LinearLayoutManager(activity));
            mRecyclerView.setAdapter(new CrateVideoRecyclerAdapter(this, mCrate));
        } else {
            mRecyclerView.setLayoutManager(null);
            mRecyclerView.setAdapter(null);
        }
    }

    private void disposeVideo() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }

    private void loadVideoIfReady(@NonNull final VideoAsset asset) {
        disposeVideo();

        if (mProgressBarContainer.getAlpha() > 0f) {
            Log.d(TAG, "Load already in progress, ignoring");
        }

        mAsset = asset;
        Runnable endAction = new Runnable() {
            @Override
            public void run() {
                mLoader.loadInto(VideoListFragment.this, asset);
            }
        };

        if (mCardViewContainer.getAlpha() <= 0) {
            mProgressBarContainer.setAlpha(1f);
            mCardViewContainer.animate()
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .alpha(1f)
                    .setDuration(PROGRESS_FADE_ANIM_MILLIS)
                    .withEndAction(endAction).start();
        } else {
            mProgressBarContainer.animate()
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .alpha(1f)
                    .setDuration(PROGRESS_FADE_ANIM_MILLIS)
                    .withEndAction(endAction).start();
        }
    }

    @NonNull
    private Point getDesiredVideoSize(int videoWidth, int videoHeight) {

        if (mVideoViewWidth <= 0 || mVideoViewHeight <= 0) {
            mVideoViewWidth = mCardViewContainer.getWidth();
            mVideoViewHeight = mCardViewContainer.getHeight();
        }

        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (mVideoViewHeight > (int) (mVideoViewWidth * aspectRatio)) {
            newWidth = mVideoViewWidth;
            newHeight = (int) (mVideoViewWidth * aspectRatio);
        } else {
            newWidth = (int) (mVideoViewHeight / aspectRatio);
            newHeight = mVideoViewHeight;
        }

        return new Point(newWidth, newHeight);
    }

    private void playVideo(@NonNull final MediaPlayer player) {
        int videoWidth = player.getVideoWidth();
        int videoHeight = player.getVideoHeight();

        Point desiredSize = getDesiredVideoSize(videoWidth, videoHeight);
        if (desiredSize.x <= 0 || desiredSize.y <= 0) {
            // TODO: Show error?
            return;
        }

        ViewGroup.LayoutParams params = mCardView.getLayoutParams();
        final int initialWidth = params.width;
        final int initialHeight = params.height;
        final int xDelta = desiredSize.x - initialWidth;
        final int yDelta = desiredSize.y - initialHeight;

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(VIDEO_RESIZE_ANIM_MILLIS);
        animator.setInterpolator(new OvershootInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                Float currentValue = (Float) animation.getAnimatedValue();
                setCardLayoutParams(initialWidth + (int) (xDelta * currentValue),
                        initialHeight + (int) (yDelta * currentValue));
            }
        });
        animator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mExecutorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        player.start();
                    }
                });
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressBarContainer.animate()
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .alpha(0f)
                        .setDuration(PROGRESS_FADE_ANIM_MILLIS)
                        .start();
            }
        });
        animator.start();
    }

    private void setCardLayoutParams(int width, int height) {
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mCardView.getLayoutParams();
        params.width = width;
        params.height = height;
        mCardView.setLayoutParams(params);
    }

    public static final class VideoHolder extends CrateCardViewHolder<VideoAsset, CrateVideoInfoView> {

        VideoHolder(@NonNull CrateVideoInfoView view) {
            super(view);
        }
    }

    private static final class CrateVideoRecyclerAdapter extends CrateAdapter<VideoHolder> {

        @NonNull
        private final WeakReference<VideoListFragment> mFragment;

        @NonNull
        protected final Crate mCrate;

        @NonNull
        private final DebouncingClickListener mClickListener = new DebouncingClickListener(DEBOUNCE_MILLIS) {
            @Override
            public void performClick(@NonNull View view) {
                if (view instanceof CrateVideoInfoView) {
                    VideoAsset asset = ((CrateVideoInfoView) view).getAsset();
                    VideoListFragment fragment = mFragment.get();
                    if (fragment != null && asset != null) {
                        fragment.loadVideoIfReady(asset);
                    }
                }
            }
        };

        CrateVideoRecyclerAdapter(@NonNull VideoListFragment fragment, @NonNull Crate crate) {
            super(crate.assets.videos.LIST.size());
            mFragment = new WeakReference<>(fragment);
            mCrate = crate;
        }

        @Override
        public VideoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.video_list_item, viewGroup, false);
            return new VideoHolder((CrateVideoInfoView) view);
        }

        @Override
        public void onBindViewHolder(@NonNull VideoHolder holder, int position) {
            final VideoAsset asset = mCrate.assets.videos.LIST.get(position);
            holder.view.setAsset(asset);
            holder.view.setOnClickListener(mClickListener);
        }

        @Override
        public int getItemCount() {
            return mCrate.assets.videos.LIST.size();
        }
    }
}
