package me.oriley.cratesample;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout mDrawerLayout;

    private NavigationView mNavigationView;

    private RecyclerView mRecyclerView;

    private FontRecyclerAdapter mFontAdapter;

    private Crate mCrate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCrate = new Crate(this);

        setContentView(R.layout.activity_main);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        mFontAdapter = new FontRecyclerAdapter(mCrate);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mFontAdapter);
        mRecyclerView.scrollToPosition(Integer.MAX_VALUE / 2);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRecyclerView.setAdapter(null);
        mFontAdapter = null;
        mCrate = null;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_fonts) {
            mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            mRecyclerView.setAdapter(mFontAdapter);
        } else if (id == R.id.nav_images) {
            showToast("Coming soon");
        } else if (id == R.id.nav_svgs) {
            showToast("Coming soon");
        }

        mRecyclerView.scrollToPosition(Integer.MAX_VALUE / 2);
        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private static final class FontViewHolder extends RecyclerView.ViewHolder {

        @NonNull
        TextView textView;

        FontViewHolder(@NonNull View view) {
            super(view);
            textView = (TextView) view.findViewById(R.id.text_view);
        }
    }

    private static final class FontRecyclerAdapter extends RecyclerView.Adapter<FontViewHolder> {

        @NonNull
        private final Crate mCrate;

        private final int mActualSize;

        FontRecyclerAdapter(@NonNull Crate crate) {
            mCrate = crate;
            mActualSize = mCrate.assets.fonts.LIST.size();
        }

        @Override
        public FontViewHolder onCreateViewHolder(ViewGroup viewGroup, int position) {
            View view = LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.font_view_item, viewGroup, false);
            return new FontViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull FontViewHolder holder, int position) {
            Crate.FontAsset fontAsset = mCrate.assets.fonts.LIST.get(position % mActualSize);
            holder.textView.setText(fontAsset.getFontName());
            holder.textView.setTypeface(fontAsset.getTypeface());
        }

        @Override
        public int getItemCount() {
            return Integer.MAX_VALUE;
        }
    }
}
