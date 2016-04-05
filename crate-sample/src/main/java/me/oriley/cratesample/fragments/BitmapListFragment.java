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

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import me.oriley.crate.Crate;
import me.oriley.crate.ImageAsset;
import me.oriley.cratesample.R;
import me.oriley.cratesample.loaders.CrateBitmapLoader;
import me.oriley.cratesample.widget.CrateBitmapView;
import me.oriley.cratesample.widget.CrateCardViewHolder;

public final class BitmapListFragment extends RecyclerViewFragment {

    private int mBitmapColumnCount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        mBitmapColumnCount = res.getInteger(R.integer.bitmap_column_count);
    }

    @NonNull
    @Override
    public CrateAdapter getAdapter() {
        return new BitmapRecyclerAdapter(mCrate);
    }

    @NonNull
    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new GridLayoutManager(getActivity(), mBitmapColumnCount);
    }

    public static final class BitmapHolder extends CrateCardViewHolder<ImageAsset, CrateBitmapView> {

        BitmapHolder(@NonNull CrateBitmapView view) {
            super(view);
        }
    }

    private static final class BitmapRecyclerAdapter extends CrateAdapter<BitmapHolder> {

        @NonNull
        protected final Crate mCrate;

        @NonNull
        private final CrateBitmapLoader mLoader;


        BitmapRecyclerAdapter(@NonNull Crate crate) {
            super(crate.assets.images.LIST.size());
            mCrate = crate;
            mLoader = new CrateBitmapLoader(crate, 0);
        }


        @Override
        public BitmapHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.bitmap_view_item, viewGroup, false);
            return new BitmapHolder((CrateBitmapView) view);
        }

        @Override
        public void onBindViewHolder(@NonNull BitmapHolder holder, int position) {
            ImageAsset imageAsset = mCrate.assets.images.LIST.get(position % mActualSize);
            mLoader.loadInto(holder, imageAsset, 100 + (int) (150 * getFlingScale()));
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }
}
