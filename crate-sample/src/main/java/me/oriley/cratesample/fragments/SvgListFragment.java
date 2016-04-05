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
import me.oriley.crate.SvgAsset;
import me.oriley.cratesample.R;
import me.oriley.cratesample.loaders.CrateSvgLoader;
import me.oriley.cratesample.widget.CrateCardViewHolder;
import me.oriley.cratesample.widget.CrateSvgView;

public final class SvgListFragment extends RecyclerViewFragment {

    private int mSvgColumnCount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Resources res = getResources();
        mSvgColumnCount = res.getInteger(R.integer.svg_column_count);
    }

    @NonNull
    @Override
    public CrateAdapter getAdapter() {
        return new SvgRecyclerAdapter(mCrate);
    }

    @NonNull
    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new GridLayoutManager(getActivity(), mSvgColumnCount);
    }

    public static final class SvgHolder extends CrateCardViewHolder<SvgAsset, CrateSvgView> {

        SvgHolder(@NonNull CrateSvgView view) {
            super(view);
        }
    }

    private static final class SvgRecyclerAdapter extends CrateAdapter<SvgHolder> {

        @NonNull
        protected final Crate mCrate;

        @NonNull
        private final CrateSvgLoader mLoader;

        protected final int mActualSize;


        SvgRecyclerAdapter(@NonNull Crate crate) {
            super(crate.assets.svgs.LIST.size());
            mCrate = crate;
            mLoader = new CrateSvgLoader(crate, 0);
            mActualSize = mCrate.assets.svgs.LIST.size();
        }


        @Override
        public SvgHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.svg_view_item, viewGroup, false);
            return new SvgHolder((CrateSvgView) view);
        }

        @Override
        public void onBindViewHolder(@NonNull SvgHolder holder, int position) {
            SvgAsset svgAsset = mCrate.assets.svgs.LIST.get(position % mActualSize);
            mLoader.loadInto(holder, svgAsset, 150 + (int) (200 * getFlingScale()));
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }

        @Override
        public void dispose() {
            mLoader.dispose();
        }
    }
}
