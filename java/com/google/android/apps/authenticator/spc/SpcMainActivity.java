/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.authenticator.spc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.apps.authenticator.barcode.BarcodeCaptureActivity;
import com.google.android.apps.authenticator.barcode.BarcodeConditionChecker;
import com.google.android.apps.authenticator.enroll2sv.wizard.AddAccountActivity;
import com.google.android.apps.authenticator.howitworks.HowItWorksActivity;
import com.google.android.apps.authenticator.otp.CheckCodeActivity;
//import com.google.android.apps.authenticator.otp.OtpSource;
import com.google.android.apps.authenticator.otp.OtpSourceException;
import com.google.android.apps.authenticator.otp.TotpClock;
import com.google.android.apps.authenticator.otp.TotpCountdownTask;
import com.google.android.apps.authenticator.otp.TotpCounter;
import com.google.android.apps.authenticator.settings.SettingsActivity;
import com.google.android.apps.authenticator.testability.DaggerInjector;
import com.google.android.apps.authenticator.testability.DependencyInjector;
import com.google.android.apps.authenticator.testability.TestableActivity;
import com.google.android.apps.authenticator.util.EmptySpaceClickableDragSortListView;
import com.google.android.apps.authenticator.util.Utilities;
import com.google.android.apps.authenticator.util.annotations.FixWhenMinSdkVersion;
import com.google.android.apps.authenticator2.R;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortItemView;
import com.mobeta.android.dslv.DragSortListView.DragListener;
import com.mobeta.android.dslv.DragSortListView.DropListener;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import com.google.android.apps.authenticator.spc.SpcDecryptActivity;
import com.google.android.apps.authenticator.spc.SpcEncryptActivity;
import com.google.android.apps.authenticator.spc.AddKeyValueActivity;

import com.google.android.apps.authenticator.spc.SpcAccountInfo;

import com.google.android.apps.authenticator.spc.SpcAccountDb;
import com.google.android.apps.authenticator.spc.SpcAccountDb.AccountDbIdUpdateFailureException;
import com.google.android.apps.authenticator.spc.SpcAccountDb.AccountIndex;
import com.google.android.apps.authenticator.spc.SpcCryptoActivity;


/**
 * The main activity that displays usernames and codes
 */
@FixWhenMinSdkVersion(11) // Will be able to remove the context menu
public class SpcMainActivity extends TestableActivity {

    private static final long VIBRATE_DURATION = 200L;
    public static final String KEY_DARK_MODE_ENABLED = "darkModeEnabled";
    private static final String KEY_FIRST_ACCOUNT_ADDED_NOTICE_DISPLAY_REQUIRED ="firstAccountAddedNoticeDisplayRequired";
    private boolean firstAccountAddedNoticeDisplayRequired;
    protected EmptySpaceClickableDragSortListView userList;
    protected SpcAccountInfo[] users = {};

    @VisibleForTesting
    boolean darkModeEnabled;
    protected SpcAccountDb accountDb;
    @VisibleForTesting
    protected SharedPreferences preferences;

    @VisibleForTesting
    ActionMode actionMode;

    private static final String LOCAL_TAG = "SpcMainActivity";

    private PinListAdapter userAdapter;
    private static final float PIN_TEXT_SCALEX_NORMAL = 1.0f;
    private static final float PIN_TEXT_SCALEX_UNDERSCORE = 0.87f;

    private View contentAccountsPresent;
    private View contentNoAccounts;
    private boolean saveKeyIntentConfirmationInProgress;

    @VisibleForTesting
    static final int DIALOG_ID_INVALID_QR_CODE = 15;
    @VisibleForTesting
    static final int SCAN_REQUEST = 31337;
    @VisibleForTesting
    static final int DIALOG_ID_BARCODE_SCANNER_NOT_AVAILABLE = 18;
    @VisibleForTesting
    static final int DIALOG_ID_LOW_STORAGE_FOR_BARCODE_SCANNER = 19;

    @VisibleForTesting
    static final int DIALOG_ID_CAMERA_NOT_AVAILABLE = 20;
    @VisibleForTesting
    static final String PREF_KEY_LAST_LAUNCH_ACCOUNT_COUNT = "accountCountDuringLastMainPageLaunch";



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        accountDb = DependencyInjector.getSpcAccountDb();

        setContentView(R.layout.spc_main);


        if (savedInstanceState != null) {
            firstAccountAddedNoticeDisplayRequired =
                    savedInstanceState.getBoolean(KEY_FIRST_ACCOUNT_ADDED_NOTICE_DISPLAY_REQUIRED, false);
        }

        userList = (EmptySpaceClickableDragSortListView) findViewById(R.id.user_list);

        registerContextualActionBarForUserList();
//        userList.setOnItemClickListener(
//                new OnItemClickListener() {
//                    @Override
//                    public void onItemClick(AdapterView<?> parent, View row, int position, long itemId) {
//                        // Each item in DragSortListView is wrapped with DragSortItemView.
//                        // Iterating the children to find the enclosed SpcUserRowView
////                        DragSortItemView dragSortItemView = (DragSortItemView) row;
////                        View userRowView = null;
////                        for (int i = 0; i < dragSortItemView.getChildCount(); i++) {
////                            if (dragSortItemView.getChildAt(i) instanceof UserRowView) {
////                                userRowView = dragSortItemView.getChildAt(i);
////                            }
////                        }
////                        if (userRowView == null) {
////                            return;
////                        }
//
//                        Crypto clickListener = (Crypto) row.getTag();
////                        clickListener.onClick(row);
////                        View nextOtp = userRowView.findViewById(R.id.next_otp);
//                        if (clickListener != null) {
//                            clickListener.onClick(row);
//                        }
//                        userList.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
//
//                    }
//                });
        userList.setOnItemClickListener(
                new OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View row, int position, long itemId) {
                        // Each item in DragSortListView is wrapped with DragSortItemView.
                        // Iterating the children to find the enclosed UserRowView
                        DragSortItemView dragSortItemView = (DragSortItemView) row;
                        View userRowView = null;
                        for (int i = 0; i < dragSortItemView.getChildCount(); i++) {
                            if (dragSortItemView.getChildAt(i) instanceof SpcUserRowView) {
                                userRowView = dragSortItemView.getChildAt(i);
                            }
                        }
                        if (userRowView == null) {
                            return;
                        }
                        Crypto clickListener = (Crypto) userRowView.getTag();
                        if (clickListener != null) {
                            clickListener.onClick(userRowView);
                        }
                        userList.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_SELECTED);
                    }
                });
        userList.setOnEmptySpaceClickListener(() -> unselectItemOnList());


        contentNoAccounts = findViewById(R.id.content_no_accounts);
        contentAccountsPresent = findViewById(R.id.content_accounts_present);

        refreshLayoutByUserNumber();
        refreshOrientationState();

        findViewById(R.id.add_account_button)
                .setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(new Intent(SpcMainActivity.this, AddKeyValueActivity.class));
                            }
                        });

        FloatingActionButton addAccountFab = (FloatingActionButton) findViewById(R.id.add_account_fab);
        addAccountFab.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(SpcMainActivity.this, AddKeyValueActivity.class));
                    }
                });


        addAccountFab.bringToFront();
        contentAccountsPresent.invalidate();

        findViewById(R.id.first_account_message_button_done)
                .setOnClickListener(
                        new OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dismissFirstAccountAddedNoticeDisplay();
                            }
                        });

        userAdapter = new PinListAdapter(this, R.layout.spc_user_row, users);

        userList.setAdapter(userAdapter);

        userList.setDropListener(
                new DropListener() {
                    @Override
                    public void drop(int from, int to) {
                        userAdapter.notifyDataSetChanged();
                    }
                });

        userList.setDragListener(
                new DragListener() {
                    @Override
                    public void drag(int from, int to) {
                        if (from == to) {
                            return;
                        }
                        List<AccountIndex> accounts = accountDb.getAccounts();
                        AccountIndex firstIndex = accounts.get(from);
                        AccountIndex secondIndex = accounts.get(to);
                        try {
                            // drag callback is fired in a worker thread, swapping the Ids doesn't affect the UI
                            // thread.
                            accountDb.swapId(firstIndex, secondIndex);
                        } catch (AccountDbIdUpdateFailureException e) {
                            Toast.makeText(
                                    getApplicationContext(), R.string.accounts_reorder_failed, Toast.LENGTH_SHORT)
                                    .show();
                        }
                        SpcAccountInfo.swapIndex(users, from, to);
                    }
                });
        DragItemController dragItemController = new DragItemController(userList, this);
        dragItemController.setStartDraggingListener(
                new DragItemController.StartDraggingListener() {
                    @Override
                    public void startDragging() {
                        unselectItemOnList();
                    }
                });


        userList.setFloatViewManager(dragItemController);
        userList.setOnTouchListener(dragItemController);


        if (savedInstanceState == null) {
            // This is the first time this Activity is starting (i.e., not restoring previous state which
            // was saved, for example, due to orientation change)
            handleIntent(getIntent());
        }

        refreshView();

    }


    private void dismissFirstAccountAddedNoticeDisplay() {
        setLastLaunchAccountCount(getAccountCount());
        firstAccountAddedNoticeDisplayRequired = false;
        refreshFirstAccountAddedNoticeDisplay();
        refreshOrientationState();
    }


    private void refreshOrientationState() {
        boolean isTablet = getResources().getBoolean(R.bool.isTablet);
        if (!isTablet && (getAccountCount() == 0 || firstAccountAddedNoticeDisplayRequired)) {
            // Lock to portrait mode.
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
        }
    }


    private int getLastLaunchAccountCount() {
        return preferences.getInt(PREF_KEY_LAST_LAUNCH_ACCOUNT_COUNT, -1);
    }


    private void refreshView() {
        refreshView(false);
    }


    private int getAccountCount() {
        return accountDb.getAccounts().size();
    }


    @VisibleForTesting
    public void refreshView(boolean isAccountModified) {
        List<AccountIndex> accounts = accountDb.getAccounts();
        int userCount = accounts.size();
        if (userCount > 0) {
            boolean newListRequired = isAccountModified || users.length != userCount;
            if (newListRequired) {
                users = new SpcAccountInfo[userCount];
            }

            for (int i = 0; i < userCount; ++i) {
                AccountIndex user = accounts.get(i);
                try {
                    computeAndDisplayPin(user, i, false);
                } catch (OtpSourceException ignored) {
                    // Ignore
                }
            }

            if (newListRequired) {
                if (actionMode != null) {
                    actionMode.finish();
                    actionMode = null;
                }
                // Make the list display the data from the newly created array of accounts
                // This forces the list to scroll to top.

                userAdapter = new PinListAdapter(this, R.layout.spc_user_row, users);
                userList.setAdapter(userAdapter);
            }

            userAdapter.notifyDataSetChanged();
        } else {
            users = new SpcAccountInfo[0]; // clear any existing user PIN state
        }

        refreshLayoutByUserNumber();
        refreshFirstAccountAddedNoticeDisplay();
        refreshOrientationState();
    }


    private void refreshFirstAccountAddedNoticeDisplay() {
        int[] viewIdArrayForFirstAccountAddedNotice = {
                R.id.first_account_message_header,
                R.id.first_account_message_detail,
                R.id.first_account_message_button_done
        };

        int userListPaddingTop;
        int userListPaddingBottom;
        LayoutParams layoutParams = userList.getLayoutParams();

        if (firstAccountAddedNoticeDisplayRequired) {
            // Update view visibility
            for (int viewId : viewIdArrayForFirstAccountAddedNotice) {
                findViewById(viewId).setVisibility(View.VISIBLE);
            }
            findViewById(R.id.add_account_fab).setVisibility(View.GONE);

            // Figure out layout parameters
            userListPaddingTop = getResources().getDimensionPixelSize(R.dimen.pincode_list_no_paddingTop);
            userListPaddingBottom =
                    getResources().getDimensionPixelSize(R.dimen.pincode_list_no_paddingBottom);
            layoutParams.height = LayoutParams.WRAP_CONTENT;
        } else {
            // Update view visibility
            for (int viewId : viewIdArrayForFirstAccountAddedNotice) {
                findViewById(viewId).setVisibility(View.GONE);
            }
            findViewById(R.id.add_account_fab).setVisibility(View.VISIBLE);

            // Figure out layout parameters
            userListPaddingTop = getResources().getDimensionPixelSize(R.dimen.pincode_list_paddingTop);
            userListPaddingBottom =
                    getResources().getDimensionPixelSize(R.dimen.pincode_list_paddingBottom);
            layoutParams.height = LayoutParams.MATCH_PARENT;
        }

        userList.setPadding(0, userListPaddingTop, 0, userListPaddingBottom);
        userList.setLayoutParams(layoutParams);
    }

    public static boolean saveKeyValue(
            Context context, AccountIndex index, String privkey, String pubkey, Integer counter) {
        if (privkey != null && pubkey != null) {
            SpcAccountDb spcAccountDb = DependencyInjector.getSpcAccountDb();
            spcAccountDb.add(index.getName(), privkey, pubkey, counter, null, index.getIssuer());

            ((Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(VIBRATE_DURATION);
            return true;
        } else {
            return false;
        }
    }


    @TargetApi(11)
    private void registerContextualActionBarForUserList() {
        // TODO: Consider switching to a single choice list when its action mode state starts to
        // automatically survive configuration (e.g., orientation) changes.
        //
        // Since action mode does not currently survive orientation changes in single choice mode, we
        // use multiple choice mode instead while still enforcing that only one item is checked at any
        // time.
        userList.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        final AbsListView.MultiChoiceModeListener multiChoiceModeListener =
                new AbsListView.MultiChoiceModeListener() {
                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                        actionMode = null;
                    }

                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        actionMode = mode;
                        getMenuInflater().inflate(R.menu.spc_account_list_context, menu);

                        // This method is invoked both when the user starts the CAB and when the CAB is
                        // recreated
                        // after an orientation change. In the former case, no list items are checked. In the
                        // latter, one of the items is checked.
                        // Unfortunately, the Android framework does not preserve the state of the menu after
                        // orientation changes. Thus, we need to update the menu.
                        if (userList.getCheckedItemCount() > 0) {
                            // Assume only one item can be checked, and blow up otherwise
                            int position = getMultiSelectListSingleCheckedItemPosition(userList);
                            updateCabForAccount(mode, menu, users[position]);
                        }

                        return true;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        // This function can be triggered twice as user can double click the item while it is on
                        // the
                        // dismissing animation, we ignore the second click by checking if the item is
                        // unselected.

                        if (userList.getCheckedItemCount() == 0) {
                            return false;
                        }
                        int position = getMultiSelectListSingleCheckedItemPosition(userList);
                        if (onContextItemSelected(item, position)) {
                            mode.finish();
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onItemCheckedStateChanged(
                            ActionMode mode, int position, long id, boolean checked) {
                        if (checked) {
                            mode.setTitle(users[position].getIndex().getStrippedName());

                            // Ensure that only one item is checked, by unchecking all other checked items.
                            SparseBooleanArray checkedItemPositions = userList.getCheckedItemPositions();
                            for (int i = 0, len = userList.getCount(); i < len; i++) {
                                if (i == position) {
                                    continue;
                                }
                                boolean itemChecked = checkedItemPositions.get(i);
                                if (itemChecked) {
                                    userList.setItemChecked(i, false);
                                }
                            }

                            updateCabForAccount(mode, mode.getMenu(), users[position]);
                            userAdapter.notifyDataSetChanged();
                        }
                    }

                    private void updateCabForAccount(ActionMode mode, Menu menu, SpcAccountInfo account) {
                        mode.setTitle(account.getIndex().getStrippedName());
                    }


                };
        userList.setMultiChoiceModeListener(multiChoiceModeListener);
    }


    @TargetApi(11)
    private static int getMultiSelectListSingleCheckedItemPosition(AbsListView list) {
        Preconditions.checkState(list.getCheckedItemCount() == 1);
        SparseBooleanArray checkedItemPositions = list.getCheckedItemPositions();
        Preconditions.checkState(checkedItemPositions != null);
        for (int i = 0, len = list.getCount(); i < len; i++) {
            boolean itemChecked = checkedItemPositions.get(i);
            if (itemChecked) {
                return i;
            }
        }

        throw new IllegalStateException("No items checked");
    }


    private class PinListAdapter extends ArrayAdapter<SpcAccountInfo> {

        public PinListAdapter(Context context, int userRowId, SpcAccountInfo[] items) {
            super(context, userRowId, items);
        }



        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            SpcAccountInfo currentPin = getItem(position);


            View row;
            if (convertView != null) {
                row = convertView;
            } else {
                row = inflater.inflate(R.layout.spc_user_row, null);

                row.setLayoutParams(
                        new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            }

            TextView pinView = row.findViewById(R.id.pin_value);
            // We only show drag handle on selected item when the number of items is larger than 1.
            boolean showDragHandle = false;
            try {
                if (getMultiSelectListSingleCheckedItemPosition(userList) == position && getCount() >= 2) {
                    showDragHandle = true;
                }
            } catch (IllegalStateException ignored) {
                // No pin code is selected.
            }
            row.findViewById(R.id.user_row_drag_handle_image)
                    .setVisibility(showDragHandle ? View.VISIBLE : View.GONE);

            if (getString(R.string.empty_pin).equals(currentPin.getPin())) {
                pinView.setTextScaleX(PIN_TEXT_SCALEX_UNDERSCORE); // smaller gap between underscores
            } else {
                pinView.setTextScaleX(PIN_TEXT_SCALEX_NORMAL);
            }
            pinView.setText(Utilities.getStyledPincode(currentPin.getPin()));


            Crypto clickListener = new Crypto(currentPin);
            row.setTag(clickListener);

            return row;
        }
    }


    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        return onContextItemSelected(item, info.id);
    }


    @SuppressWarnings("deprecation")
    @FixWhenMinSdkVersion(11) // Switch to android.content.ClipboardManager.setPrimaryClip
    @TargetApi(11)
    private boolean onContextItemSelected(MenuItem item, long itemId) {
        Intent intent;
        final AccountIndex index = users[(int) itemId].getIndex(); // final so listener can see value

        if (item.getItemId() == R.id.rename) {
            final Context context = this; // final so listener can see value
            final View frame =
                    getLayoutInflater().inflate(R.layout.rename, (ViewGroup) findViewById(R.id.rename_root));
            final EditText nameEdit = frame.findViewById(R.id.rename_edittext);
            nameEdit.setText(index.getStrippedName()); // User can only edit the stripped name
            new AlertDialog.Builder(this)
                    .setTitle(R.string.rename)
                    .setView(frame)
                    .setPositiveButton(R.string.submit, this.getRenameClickListener(context, index, nameEdit))
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        } else if (item.getItemId() == R.id.delete) {
            AlertDialog.Builder alertDialogBuilder =
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.remove_account_dialog_title, index))
                            .setMessage(
                                    Utilities.getStyledTextFromHtml(
                                            getString(R.string.remove_account_dialog_message)))
                            .setPositiveButton(
                                    R.string.remove_account_dialog_button_remove,
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            accountDb.delete(index);
                                            refreshView(true);
                                        }
                                    })
                            .setNegativeButton(R.string.cancel, null)
                            .setIcon(R.drawable.quantum_ic_report_problem_grey600_24);
            alertDialogBuilder.show();
            return true;
        }
        return super.onContextItemSelected(item);
    }


    /**
     * 判断是否首次生成公私钥对
     * **/
    private void refreshLayoutByUserNumber() {
        int numUsers = users.length;
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(numUsers > 0);
        }
        contentNoAccounts.setVisibility(numUsers == 0 ? View.VISIBLE : View.GONE);
        contentAccountsPresent.setVisibility(numUsers > 0 ? View.VISIBLE : View.GONE);
    }


    public void computeAndDisplayPin(AccountIndex user, int position, boolean computeHotp)
            throws OtpSourceException {

        SpcAccountInfo currentPin;
        if (users[position] != null) {
            currentPin = users[position]; // existing PinInfo, so we'll update it
        } else {
            currentPin = new SpcAccountInfo(user, false);
            currentPin.setPin(getString(R.string.empty_pin));
            currentPin.setIsHotpCodeGenerationAllowed(true);
        }
        currentPin.setPin(currentPin.getIndex().getName());
        currentPin.setPubkey(currentPin.getIndex().getPubkey());
        currentPin.setPrivkey(currentPin.getIndex().getPrivkey());
        users[position] = currentPin;
    }


    @TargetApi(11)
    private void unselectItemOnList() {
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
            try {
                int selected = getMultiSelectListSingleCheckedItemPosition(userList);
                userList.setItemChecked(selected, false);
            } catch (IllegalStateException e) {
                // No item is selected.
                Log.e(getString(R.string.app_name), LOCAL_TAG, e);
            }
        }
    }


    protected void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
    }


    private void setLastLaunchAccountCount(int accountCount) {
        preferences.edit().putInt(PREF_KEY_LAST_LAUNCH_ACCOUNT_COUNT, accountCount).commit();
    }


    private static class DragItemController extends DragSortController {

        /**
         * Handle event when user start dragging.
         */
        public interface StartDraggingListener {

            /**
             * Event when user start dragging the item.
             */
            void startDragging();
        }

        private final Activity activity;
        private final EmptySpaceClickableDragSortListView dragSortListView;
        private StartDraggingListener startDraggingListener;
        private Bitmap floatBitmap;
        private View floatView;

        public DragItemController(
                EmptySpaceClickableDragSortListView dragSortListView, Activity activity) {
            super(dragSortListView, R.id.user_row_drag_handle, DragSortController.ON_DOWN, 0);
            this.dragSortListView = dragSortListView;
            this.activity = activity;
            setRemoveEnabled(false);
        }

        public void setStartDraggingListener(StartDraggingListener startDraggingListener) {
            this.startDraggingListener = startDraggingListener;
        }

        @Override
        public int startDragPosition(MotionEvent ev) {
            ListAdapter adapter = dragSortListView.getAdapter();

            if (adapter == null || adapter.getCount() <= 1) {
                return DragSortController.MISS;
            }


            int position = super.startDragPosition(ev);
            boolean allowDragging = false;

            try {
                if (getMultiSelectListSingleCheckedItemPosition(dragSortListView) == position) {
                    allowDragging = true;
                }
            } catch (IllegalStateException ignored) {
            }

            if (!allowDragging) {
                position = DragSortController.MISS;
            }

            if (position != DragSortController.MISS && startDraggingListener != null) {
                startDraggingListener.startDragging();
            }
            return position;
        }


        @Override
        public View onCreateFloatView(int position) {
            View view =
                    dragSortListView.getChildAt(
                            position
                                    + dragSortListView.getHeaderViewsCount()
                                    - dragSortListView.getFirstVisiblePosition());
            if (view == null) {
                return null;
            }
            View userRowLayout = view.findViewById(R.id.user_row_layout);
            if (userRowLayout != null) {
                userRowLayout.setPressed(false);
                userRowLayout.setSelected(false);
                userRowLayout.setActivated(false);
            }

            view.setDrawingCacheEnabled(true);
            floatBitmap = Bitmap.createBitmap(view.getDrawingCache());
            view.setDrawingCacheEnabled(false);

            if (floatView == null) {
                LayoutInflater inflater = activity.getLayoutInflater();
                floatView = inflater.inflate(R.layout.user_row_dragged, null);
            }
            ImageView imageView = floatView.findViewById(R.id.user_row_dragged_image);
            imageView.setImageBitmap(floatBitmap);
            imageView.setLayoutParams(new RelativeLayout.LayoutParams(view.getWidth(), view.getHeight()));

            return floatView;
        }


        @Override
        public void onDestroyFloatView(View floatView) {
            ImageView imageView = floatView.findViewById(R.id.user_row_dragged_image);
            if (imageView != null) {
                imageView.setImageDrawable(null);
            }
            floatBitmap.recycle();
            floatBitmap = null;
        }
    }


    private DialogInterface.OnClickListener getRenameClickListener(
            final Context context, final AccountIndex user, final EditText nameEdit) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                String newName = nameEdit.getText().toString().trim();
                AccountIndex newIndex = new AccountIndex(newName, user.getIssuer(),user.getPubkey(),user.getPrivkey());
                if (!newIndex.getStrippedName().equals(user.getStrippedName())) {
                    if (accountDb.findSimilarExistingIndex(newIndex) != null) {
                        Toast.makeText(context, R.string.error_exists, Toast.LENGTH_LONG).show();
                    } else {
                        accountDb.rename(user, newName);
                        refreshView(true);
                    }
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onStop() {


        super.onStop();
    }
    private class Crypto implements OnClickListener {
        private final Handler handler = new Handler();
        private final SpcAccountInfo account;

        private Crypto(SpcAccountInfo account) {
            this.account = account;
        }

        @SuppressWarnings("deprecation") // TODO: refactor to use DialogFrament
        @Override
        public void onClick(View v) {
            int position = findAccountPositionInList();
            if (position == -1) {
                throw new RuntimeException("Account not in list: " + account);
            }

            try {
                computeAndDisplayPin(account.getIndex(), position, true);
            } catch (OtpSourceException e) {
                throw new RuntimeException("Failed to generate OTP for account", e);
            }

            final String pin = account.getPin();

            Intent intent = new Intent(SpcMainActivity.this, SpcCryptoActivity.class);
            intent.putExtra("name",pin);
            intent.putExtra("pubkey",account.getPubkey());
            intent.putExtra("privkey",account.getPrivkey());
            startActivity(intent);

        }

        /**
         * Gets the position in the account list of the account this listener is associated with.
         *
         * @return {@code 0}-based position or {@code -1} if the account is not in the list.
         */
        private int findAccountPositionInList() {
            for (int i = 0, len = users.length; i < len; i++) {
                if (users[i].equals(account)) {
                    return i;
                }
            }

            return -1;
        }
    }
}




