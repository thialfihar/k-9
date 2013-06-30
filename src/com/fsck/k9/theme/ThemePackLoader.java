package com.fsck.k9.theme;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;

import com.fsck.k9.K9;

import java.util.ArrayList;
import java.util.List;


public class ThemePackLoader {

    private final PackageManager mPackageManager;


    public ThemePackLoader(Context context) {
        mPackageManager = context.getPackageManager();
    }

    public List<ThemePack> loadPacks() {
        List<ThemePack> themePacks = new ArrayList<ThemePack>();

        try {
            List<ResolveInfo> resolveInfos = mPackageManager.queryIntentActivities(
                    new Intent(ThemePack.INTENT), PackageManager.GET_META_DATA);


            for (ResolveInfo resolveInfo : resolveInfos) {
                ComponentName componentName = new ComponentName(
                        resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name);

                Bundle metaData = resolveInfo.activityInfo.metaData;
                if (metaData != null) {
                    int version = metaData.getInt(ThemePack.ATTR_VERSION);
                    if (version >= 1) {
                        //TODO: validate attributes
                        ThemePack themePack = new ThemePack();
                        themePack.componentName = componentName;
                        themePack.name = metaData.getString(ThemePack.ATTR_NAME);
                        themePack.author = metaData.getString(ThemePack.ATTR_AUTHOR);
                        themePack.iconIdentifier = metaData.getInt(ThemePack.ATTR_ICON_APP);

                        themePacks.add(themePack);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Failed to enumerate theme packs", e);
        }

        return themePacks;
    }

    public Drawable getIcon(ComponentName componentName) {
        try {
            ActivityInfo info = mPackageManager.getActivityInfo(
                    componentName, PackageManager.GET_META_DATA);
            int iconIdentifier = info.metaData.getInt(ThemePack.ATTR_ICON_APP);

            Resources resources = mPackageManager.getResourcesForActivity(componentName);
            return resources.getDrawable(iconIdentifier);
        } catch (Exception e) {
            Log.e(K9.LOG_TAG, "Failed to load icon / " + componentName, e);
        }

        return null;
    }
}
