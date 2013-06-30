package com.fsck.k9.activity.setup;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.K9Activity;
import com.fsck.k9.theme.ThemePack;
import com.fsck.k9.theme.ThemePackLoader;

import java.util.ArrayList;
import java.util.List;

public class ThemePackActivity extends K9Activity implements AdapterView.OnItemClickListener {

    public static void actionEditSettings(Context context) {
        Intent i = new Intent(context, ThemePackActivity.class);
        context.startActivity(i);
    }


    private ThemePackAdapter mAdapter;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.theme_pack_list);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        initializeListView();
    }

    private void initializeListView() {
        ListView listView = (ListView) findViewById(R.id.theme_pack_list);

        List<ThemePack> themePacks = new ThemePackLoader(this).loadPacks();
        List<ThemePack> myThemePacks = new ArrayList<ThemePack>(themePacks.size() + 1);

        myThemePacks.add(createDefaultThemePack());
        myThemePacks.addAll(themePacks);

        mAdapter = new ThemePackAdapter(this, myThemePacks);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            K9.setThemePackInfo(null, null);
        } else {
            ThemePack themePack = mAdapter.getItem(position);
            ComponentName componentName = themePack.componentName;
            K9.setThemePackInfo(componentName.getPackageName(), componentName.getClassName());
        }

        SharedPreferences preferences = Preferences.getPreferences(this).getPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        K9.save(editor);
        editor.commit();

        finish();
    }

    private ThemePack createDefaultThemePack() {
        ThemePack themePack = new ThemePack();
        themePack.name = getString(R.string.default_theme_pack_name);
        themePack.author = getString(R.string.default_theme_pack_author);
        themePack.componentName = getComponentName();
        themePack.iconIdentifier = R.drawable.icon;

        return themePack;
    }


    static class ThemePackAdapter extends ArrayAdapter<ThemePack> {
        private final LayoutInflater mLayoutInflater;
        private final PackageManager mPackageManager;
        private final ComponentName mCurrentThemePack;

        public ThemePackAdapter(Context context, List<ThemePack> themePacks) {
            super(context, R.layout.theme_pack_list_item, R.id.theme_pack_name, themePacks);
            mLayoutInflater = LayoutInflater.from(context);
            mPackageManager = context.getPackageManager();
            mCurrentThemePack = K9.getThemePackComponentName();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            ThemePackHolder holder;

            if (convertView == null) {
                view = mLayoutInflater.inflate(R.layout.theme_pack_list_item, parent, false);

                holder = new ThemePackHolder();
                holder.name = (TextView) view.findViewById(R.id.theme_pack_name);
                holder.author = (TextView) view.findViewById(R.id.theme_pack_author);
                holder.icon = (ImageView) view.findViewById(R.id.theme_pack_icon);

                view.setTag(holder);
            } else {
                view = convertView;
                holder = (ThemePackHolder) view.getTag();
            }

            ThemePack themePack = getItem(position);

            holder.name.setText(themePack.name);
            holder.author.setText(themePack.author);

            try {
                //TODO: optimize
                Resources resources = mPackageManager.getResourcesForActivity(themePack.componentName);
                Drawable drawable = resources.getDrawable(themePack.iconIdentifier);
                holder.icon.setImageDrawable(drawable);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            if (mCurrentThemePack == null && position == 0 ||
                    themePack.componentName.equals(mCurrentThemePack)) {
                //view.setBackgroundColor(color);
            }

            return view;
        }
    }

    static class ThemePackHolder {
        public TextView name;
        public TextView author;
        public ImageView icon;
    }
}
