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

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import me.oriley.crate.Asset;

public class CrateCardViewHolder<A extends Asset, T extends TaggedCardView<A>> extends RecyclerView.ViewHolder {

    @NonNull
    public T view;

    public boolean attached;

    public boolean loaded;

    public CrateCardViewHolder(@NonNull T view) {
        super(view);
        this.view = view;
    }

    public void attach() {
        attached = true;
        animateIfReady();
    }

    public void detach() {
        attached = false;
        clearAnimation();
    }

    public void reset() {
        attached = false;
        loaded = false;
        clearAnimation();
    }

    public void initialise(@Nullable A asset) {
        loaded = false;
        view.initialise(asset);
    }

    public void clearAnimation() {
        view.clearCardAnimation();
    }

    public void animateIfReady() {
        clearAnimation();
        if (attached && loaded) {
            view.animateCard();
        }
    }
}
