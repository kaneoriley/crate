// https://github.com/svn2github/filebot

package me.oriley.crate.mediainfo;

@SuppressWarnings("unused")
public class MediaInfoException extends RuntimeException {

	public MediaInfoException(String message) {
		super(message);
	}

	public MediaInfoException(LinkageError e) {
		super(getLinkageErrorMessage(e), e);
	}

	private static String getLinkageErrorMessage(LinkageError e) {
		String name = System.mapLibraryName("mediainfo");
		String arch = System.getProperty("os.arch");
		return String.format("Unable to load %s native library %s: %s", arch, name, e.getMessage());
	}
}
