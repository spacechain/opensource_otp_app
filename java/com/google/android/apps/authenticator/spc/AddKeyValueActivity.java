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

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;

import com.google.android.apps.authenticator.AuthenticatorActivity;
import com.google.android.apps.authenticator.testability.TestableActivity;
import com.google.android.apps.authenticator.util.Base32String;
import com.google.android.apps.authenticator.util.Base32String.DecodingException;
import com.google.android.apps.authenticator2.R;
import com.google.common.annotations.VisibleForTesting;

import android.widget.Toast;

import com.google.android.apps.authenticator.spc.SpcMainActivity;

import com.google.android.apps.authenticator.spc.SpcAccountDb.AccountIndex;
import com.google.android.apps.authenticator.spc.RSAEncrypt;

/**
 * The activity that lets the user manually add an account by entering its name, key, and type
 * (TOTP/HOTP).
 */
public class AddKeyValueActivity extends TestableActivity implements TextWatcher {

    @VisibleForTesting
    static final int DIALOG_ID_INVALID_DEVICE = 1;

    private static final int MIN_KEY_BYTES = 10;

    private EditText keyEntryField;
    private EditText accountName;
    private TextInputLayout keyEntryFieldInputLayout;
    private RadioButton typeTotp;
    private RadioButton typeHotp;

    /**
     * Called when the activity is first created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 一个轻量级的存储类
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        // 判断是否使用夜晚模式界面
        if (preferences.getBoolean(AuthenticatorActivity.KEY_DARK_MODE_ENABLED, false)) {
            setTheme(R.style.AuthenticatorTheme_NoActionBar_Dark);
        } else {
            setTheme(R.style.AuthenticatorTheme_NoActionBar);
        }

        // 加载XML界面
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spc_addkeyvalue);

        setSupportActionBar((Toolbar) findViewById(R.id.enter_key_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        accountName = (EditText) findViewById(R.id.account_name);
        findViewById(R.id.add_account_button_enter_key)
                .setOnClickListener(addButtonEnterKeyOnClickListener);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.onBackPressed();
                break;
        }
        return true;
    }


    @SuppressWarnings("deprecation") // TODO: refactor to use DialogFrament
    private final View.OnClickListener addButtonEnterKeyOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String accountName = AddKeyValueActivity.this.accountName.getText().toString();
                    // Note that this never overwrites an existing account, and instead a counter will be
                    // appended to the account name if there is a collision.


                    RSAEncrypt rsac = new RSAEncrypt();
                    rsac.genKeyPair();



                    String privkey = rsac.getPrivateKey();
                    String pubkey = rsac.getPublicKey();
                    SpcMainActivity.saveKeyValue(
                            AddKeyValueActivity.this,
                            new AccountIndex(accountName, null,pubkey,privkey), // Manually entered keys have no issuer
                            privkey,
                            pubkey,
                            1);
                    // 返回上级界面
                    Intent intent = new Intent(AddKeyValueActivity.this, SpcMainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                }
            };

    @SuppressWarnings("deprecation") // TODO: refactor to use DialogFrament
    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_ID_INVALID_DEVICE:
                return new AlertDialog.Builder(this)
                        .setIcon(R.drawable.quantum_ic_report_problem_grey600_24)
                        .setTitle(R.string.error_title)
                        .setMessage(R.string.error_invalid_device)
                        .setPositiveButton(R.string.ok, null)
                        .create();
            default:
                return super.onCreateDialog(id, args);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable userEnteredValue) {
      // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Do nothing
    }
}
