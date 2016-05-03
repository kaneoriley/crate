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

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import me.oriley.crate.Crate;
import me.oriley.cratesample.CrateApplication;
import me.oriley.cratesample.R;
import me.oriley.cratesample.widget.CrateCardViewHolder;
import me.oriley.cratesample.widget.CrateRecyclerView;

public abstract class RecyclerViewFragment extends BaseFragment {

    private static final String KEY_LAYOUT_MANAGER_STATE = "layoutManagerState";

    @BindView(R.id.recycler_view)
    CrateRecyclerView mRecyclerView;

    @Nullable
    private CrateAdapter mAdapter;

    @Nullable
    private RecyclerView.LayoutManager mLayoutManager;

    @NonNull
    final Crate mCrate = CrateApplication.getCrate();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recycler_view, container, false);
    }

    @CallSuper
    @Override
    public void onViewCreated(View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setFriction(0.7f);

        mLayoutManager = getLayoutManager();
        mRecyclerView.setLayoutManager(mLayoutManager);
        if (savedInstanceState != null) {
            Parcelable layoutState = savedInstanceState.getParcelable(KEY_LAYOUT_MANAGER_STATE);
            if (mLayoutManager != null) {
                mLayoutManager.onRestoreInstanceState(layoutState);
            }
        }

        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                mAdapter = getAdapter();
                if (mRecyclerView != null) {
                    mRecyclerView.setAdapter(mAdapter);
                    if (savedInstanceState == null && mAdapter.getItemCount() == Integer.MAX_VALUE) {
                        // For fake infinite scrolling lists, move to centre
                        mRecyclerView.scrollToPosition(Integer.MAX_VALUE / 2);
                    }
                }
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mLayoutManager != null) {
            outState.putParcelable(KEY_LAYOUT_MANAGER_STATE, mLayoutManager.onSaveInstanceState());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        if (mAdapter != null) {
            mAdapter.dispose();
            mAdapter = null;
        }

        mRecyclerView.setAdapter(null);
        mRecyclerView.setLayoutManager(null);
        super.onDestroyView();
    }

    @NonNull
    public abstract CrateAdapter getAdapter();

    @NonNull
    public abstract RecyclerView.LayoutManager getLayoutManager();

    protected abstract static class CrateAdapter<H extends CrateCardViewHolder> extends RecyclerView.Adapter<H> {

        protected final int mActualSize;

        @Nullable
        private CrateRecyclerView mRecyclerView;


        CrateAdapter(int actualSize) {
            mActualSize = actualSize;
            setHasStableIds(true);
        }


        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            if (recyclerView instanceof CrateRecyclerView) {
                mRecyclerView = (CrateRecyclerView) recyclerView;
            } else {
                throw new IllegalStateException("Must use CrateRecyclerView");
            }
        }

        @Override
        public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
            super.onDetachedFromRecyclerView(recyclerView);
            mRecyclerView = null;
        }

        @Override
        public void onViewAttachedToWindow(H holder) {
            super.onViewAttachedToWindow(holder);
            holder.attach();
        }

        @Override
        public boolean onFailedToRecycleView(H holder) {
            holder.reset();
            return true;
        }

        @Override
        public void onViewDetachedFromWindow(H holder) {
            super.onViewDetachedFromWindow(holder);
            holder.detach();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        protected float getFlingScale() {
            return mRecyclerView != null ? mRecyclerView.getFlingScale() : 0f;
        }

        public void dispose() {
            // Override if required
        }
    }
}
