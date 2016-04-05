// https://github.com/svn2github/filebot

package me.oriley.crate.mediainfo;

import java.io.File;
import java.io.FileFilter;

@SuppressWarnings("unused")
public class MediaDurationFilter implements FileFilter {

	private final MediaInfo mediaInfo = new MediaInfo();

	private final long min;
	private final long max;
	private final boolean acceptByDefault;

	public MediaDurationFilter(long min) {
		this(min, Long.MAX_VALUE, false);
	}

	public MediaDurationFilter(long min, long max, boolean acceptByDefault) {
		this.min = min;
		this.max = max;
		this.acceptByDefault = acceptByDefault;
	}

	public long getDuration(File file) {
		synchronized (mediaInfo) {
			try {
				String duration = mediaInfo.open(file).get(MediaInfo.StreamKind.General, 0, "Duration");
				return Long.parseLong(duration);
			} catch (Exception e) {
				// WTF
			}
		}
		return -1;
	}

	@Override
	public boolean accept(File file) {
		long d = getDuration(file);
		if (d >= 0) {
			return d >= min && d <= max;
		}
		return acceptByDefault;
	}
}
