package eu.midnightdust.picturesign.util;

import net.irisshaders.iris.api.v0.IrisApi;

public class IrisCompat {
    public static boolean isShaderPackInUse() {
        return IrisApi.getInstance().isShaderPackInUse();
    }
}
