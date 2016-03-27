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

package me.oriley.cratesample.loaders;

import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import me.oriley.crate.Crate;
import me.oriley.crate.VideoAsset;
import me.oriley.crate.loader.AssetLoader;
import me.oriley.cratesample.VideoListFragment;

import java.io.IOException;

public class CrateVideoLoader extends AssetLoader<VideoListFragment, VideoAsset, MediaPlayer> {


    public CrateVideoLoader(@NonNull Crate crate) {
        super(crate);
    }


    @Override
    protected void initialiseTarget(@NonNull VideoListFragment fragment, @NonNull VideoAsset asset) {
        // TODO: Detach current media player?
    }

    @NonNull
    @Override
    protected Result<MediaPlayer> load(@NonNull VideoListFragment fragment, @NonNull VideoAsset asset) {
        MediaPlayer mediaPlayer = new MediaPlayer();

        try {
            AssetFileDescriptor fd = mCrate.openFd(asset);
            mediaPlayer.setLooping(true);
            mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
            fd.close();
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Result<>(mediaPlayer, asset, false);
    }

    @Override
    protected void apply(@NonNull VideoListFragment fragment, @NonNull Result<MediaPlayer> result) {
        if (result.payload != null) {
            fragment.setMediaPlayer(result.payload);
        }
    }
}
