package com.song.jiusonggao.fingerprintauthentication;

import android.os.Build;

/**
 * Created by juisong.gao on 4/7/16.
 */
public class Utilily {
    public static boolean shouldAskPermission(){
        return(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1);
    }
}
