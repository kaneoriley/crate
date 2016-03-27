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

package me.oriley.cratesample.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import me.oriley.crate.VideoAsset;
import me.oriley.cratesample.R;

import static java.util.Locale.US;

public class CrateVideoInfoView extends FrameLayout {

    private static final String RESOLUTION_FORMAT = "%d x %d";

    @Bind(R.id.crate_video_info_title)
    TextView mTitleView;

    @Bind(R.id.crate_video_info_resolution)
    TextView mResolutionView;

    @Nullable
    private VideoAsset mAsset;


    public CrateVideoInfoView(@NonNull Context context) {
        this(context, null);
    }

    public CrateVideoInfoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CrateVideoInfoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.crate_video_info_view, this);
        ButterKnife.bind(this);
    }


    @Nullable
    public VideoAsset getAsset() {
        return mAsset;
    }

    public void setAsset(@NonNull VideoAsset asset) {
        if (mAsset != asset) {
            mAsset = asset;
            updateView();
        }
    }

    private void updateView() {
        if (mAsset != null) {
            mTitleView.setText(mAsset.getName());
            mResolutionView.setText(String.format(US, RESOLUTION_FORMAT, mAsset.getWidth(), mAsset.getHeight()));
        } else {
            mTitleView.setText(null);
            mResolutionView.setText(null);
        }
    }
}