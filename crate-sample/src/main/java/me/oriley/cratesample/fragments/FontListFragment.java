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

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import me.oriley.crate.Crate;
import me.oriley.crate.FontAsset;
import me.oriley.cratesample.R;
import me.oriley.cratesample.loaders.CrateFontLoader;
import me.oriley.cratesample.widget.CrateCardViewHolder;
import me.oriley.cratesample.widget.CrateFontView;

public final class FontListFragment extends RecyclerViewFragment {

    @NonNull
    @Override
    public CrateAdapter getAdapter() {
        return new FontRecyclerAdapter(mCrate);
    }

    @NonNull
    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getActivity());
    }

    public static final class FontHolder extends CrateCardViewHolder<FontAsset, CrateFontView> {

        FontHolder(@NonNull CrateFontView view) {
            super(view);
        }
    }

    private static final class FontRecyclerAdapter extends CrateAdapter<FontHolder> {

        @NonNull
        private final Crate mCrate;

        @NonNull
        private final CrateFontLoader mLoader;


        FontRecyclerAdapter(@NonNull Crate crate) {
            super(crate.assets.fonts.LIST.size());
            mCrate = crate;
            mLoader = new CrateFontLoader(mCrate, 0);
        }


        @Override
        public FontHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            CrateFontView view = (CrateFontView) LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.font_view_item, viewGroup, false);
            return new FontHolder(view);
        }

        @Override
        public void onBindViewHolder(FontHolder holder, int position) {
            FontAsset fontAsset = mCrate.assets.fonts.LIST.get(position % mActualSize);
            mLoader.loadInto(holder, fontAsset, 50 + (int) (100 * getFlingScale()));
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }
}
