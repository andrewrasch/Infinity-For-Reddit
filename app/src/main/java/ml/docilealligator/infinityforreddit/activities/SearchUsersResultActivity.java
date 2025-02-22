package ml.docilealligator.infinityforreddit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.r0adkll.slidr.Slidr;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import ml.docilealligator.infinityforreddit.ActivityToolbarInterface;
import ml.docilealligator.infinityforreddit.Infinity;
import ml.docilealligator.infinityforreddit.R;
import ml.docilealligator.infinityforreddit.customtheme.CustomThemeWrapper;
import ml.docilealligator.infinityforreddit.events.SwitchAccountEvent;
import ml.docilealligator.infinityforreddit.fragments.UserListingFragment;
import ml.docilealligator.infinityforreddit.utils.SharedPreferencesUtils;

public class SearchUsersResultActivity extends BaseActivity implements ActivityToolbarInterface {

    static final String EXTRA_QUERY = "EQ";
    static final String EXTRA_IS_MULTI_SELECTION = "EIMS";
    static final String EXTRA_RETURN_USER_NAME = "ERUN";
    static final String EXTRA_RETURN_USER_ICON_URL = "ERUIU";
    static final String RETURN_EXTRA_SELECTED_USERNAMES = "RESU";

    private static final String FRAGMENT_OUT_STATE = "FOS";

    @BindView(R.id.coordinator_layout_search_users_result_activity)
    CoordinatorLayout coordinatorLayout;
    @BindView(R.id.appbar_layout_search_users_result_activity)
    AppBarLayout appBarLayout;
    @BindView(R.id.toolbar_search_users_result_activity)
    Toolbar toolbar;
    Fragment mFragment;
    @Inject
    @Named("default")
    SharedPreferences mSharedPreferences;
    @Inject
    @Named("current_account")
    SharedPreferences mCurrentAccountSharedPreferences;
    @Inject
    CustomThemeWrapper mCustomThemeWrapper;
    private String mAccessToken;
    private String mAccountName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((Infinity) getApplication()).getAppComponent().inject(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_users_result);

        ButterKnife.bind(this);

        EventBus.getDefault().register(this);

        applyCustomTheme();

        if (mSharedPreferences.getBoolean(SharedPreferencesUtils.SWIPE_RIGHT_TO_GO_BACK, true)) {
            Slidr.attach(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();

            if (isChangeStatusBarIconColor()) {
                addOnOffsetChangedListener(appBarLayout);
            }

            if (isImmersiveInterface()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    coordinatorLayout.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
                } else {
                    window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
                }
                adjustToolbar(toolbar);
            }
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setToolbarGoToTop(toolbar);

        String query = getIntent().getExtras().getString(EXTRA_QUERY);

        mAccessToken = mCurrentAccountSharedPreferences.getString(SharedPreferencesUtils.ACCESS_TOKEN, null);
        mAccountName = mCurrentAccountSharedPreferences.getString(SharedPreferencesUtils.ACCOUNT_NAME, null);

        if (savedInstanceState == null) {
            mFragment = new UserListingFragment();
            Bundle bundle = new Bundle();
            bundle.putString(UserListingFragment.EXTRA_QUERY, query);
            bundle.putBoolean(UserListingFragment.EXTRA_IS_GETTING_USER_INFO, true);
            bundle.putString(UserListingFragment.EXTRA_ACCESS_TOKEN, mAccessToken);
            bundle.putString(UserListingFragment.EXTRA_ACCOUNT_NAME, mAccountName);
            bundle.putBoolean(UserListingFragment.EXTRA_IS_MULTI_SELECTION, getIntent().getBooleanExtra(EXTRA_IS_MULTI_SELECTION, false));
            mFragment.setArguments(bundle);
        } else {
            mFragment = getSupportFragmentManager().getFragment(savedInstanceState, FRAGMENT_OUT_STATE);
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout_search_users_result_activity, mFragment)
                .commit();
    }

    @Override
    protected SharedPreferences getDefaultSharedPreferences() {
        return mSharedPreferences;
    }

    @Override
    protected CustomThemeWrapper getCustomThemeWrapper() {
        return mCustomThemeWrapper;
    }

    @Override
    protected void applyCustomTheme() {
        coordinatorLayout.setBackgroundColor(mCustomThemeWrapper.getBackgroundColor());
        applyAppBarLayoutAndToolbarTheme(appBarLayout, toolbar);
    }

    public void getSelectedUser(String name, String iconUrl) {
        Intent returnIntent = new Intent();
        returnIntent.putExtra(EXTRA_RETURN_USER_NAME, name);
        returnIntent.putExtra(EXTRA_RETURN_USER_ICON_URL, iconUrl);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (getIntent().getBooleanExtra(EXTRA_IS_MULTI_SELECTION, false)) {
            getMenuInflater().inflate(R.menu.search_users_result_activity, menu);
            applyMenuItemTheme(menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_save_search_users_result_activity) {
            if (mFragment != null) {
                ArrayList<String> selectedUsernames = ((UserListingFragment) mFragment).getSelectedUsernames();
                Intent returnIntent = new Intent();
                returnIntent.putStringArrayListExtra(RETURN_EXTRA_SELECTED_USERNAMES, selectedUsernames);
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }
            return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        getSupportFragmentManager().putFragment(outState, FRAGMENT_OUT_STATE, mFragment);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onAccountSwitchEvent(SwitchAccountEvent event) {
        finish();
    }

    @Override
    public void onLongPress() {
        if (mFragment != null) {
            ((UserListingFragment) mFragment).goBackToTop();
        }
    }
}