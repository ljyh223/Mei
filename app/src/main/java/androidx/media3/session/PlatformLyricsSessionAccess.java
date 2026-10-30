package androidx.media3.session;

import androidx.media3.common.util.UnstableApi;

/**
 * Accesses the existing platform session without reflection or a duplicate session.
 * Media3 1.11.1 exposes a public token, but no public metadata writer. Keep this
 * version-dependent access isolated and check it when upgrading media3-session.
 */
@UnstableApi
public final class PlatformLyricsSessionAccess {
    private PlatformLyricsSessionAccess() {}

    public static android.media.session.MediaSession get(MediaSession session) {
        return (android.media.session.MediaSession) session.getImpl()
                .getMediaSessionLegacyStub().getSessionCompat().getMediaSession();
    }
}
