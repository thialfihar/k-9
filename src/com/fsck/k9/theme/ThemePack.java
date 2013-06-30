package com.fsck.k9.theme;

import android.content.ComponentName;

public class ThemePack {
    public static final String INTENT = "org.k9mail.THEME_PACK";

    public static final String ATTR_VERSION = "version";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_AUTHOR = "author";
    public static final String ATTR_ICON_APP = "icon_app";

    public String name;
    public String author;
    public int iconIdentifier;
    public ComponentName componentName;
}
