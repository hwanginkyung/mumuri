package growdy.mumuri.util;

public final class BlurKeyUtil {
    private BlurKeyUtil() {}

    // couples/{coupleId}/{missionId}/UUID_file.jpg  ->  couples/.../blur_UUID_file.jpg
    public static String toBlurKey(String originalKey) {
        if (originalKey == null || originalKey.isBlank()) return originalKey;

        int idx = originalKey.lastIndexOf('/');
        if (idx < 0) return "blur_" + originalKey;

        return originalKey.substring(0, idx + 1)
                + "blur_"
                + originalKey.substring(idx + 1);
    }
}