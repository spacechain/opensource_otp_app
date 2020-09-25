
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

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;
import android.content.ClipboardManager;

import com.google.android.apps.authenticator.testability.TestableActivity;
import com.google.android.apps.authenticator2.R;
import com.google.android.apps.authenticator.spc.RSAEncrypt;

import android.util.Base64;

/**
 * The activity that lets the user manually add an account by entering its name, key, and type
 * (TOTP/HOTP).
 */
public class SpcCryptoActivity extends TestableActivity implements TextWatcher {


    /**
     * Called when the activity is first created
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spc_crypto);
        Intent intent = getIntent();
        String name = intent.getStringExtra("name");
        String pubkey = intent.getStringExtra("pubkey");
        String privkey = intent.getStringExtra("privkey");
        setTitle(name);
        EditText pubkeyEdit = (EditText) findViewById(R.id.key_value);
        pubkeyEdit.setText(pubkey);
        pubkeyEdit.setCursorVisible(false);
        pubkeyEdit.setFocusable(false);
        pubkeyEdit.setFocusableInTouchMode(false);
        pubkeyEdit.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(pubkey);
                        Toast.makeText(SpcCryptoActivity.this, "Public key copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                });
        EditText resultEdit = (EditText) findViewById(R.id.result);
        resultEdit.setCursorVisible(false);
        resultEdit.setFocusable(false);
        resultEdit.setFocusableInTouchMode(false);
//        resultEdit.setText(privkey);
        resultEdit.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(resultEdit.getText().toString());
                        Toast.makeText(SpcCryptoActivity.this, "copied to clipboard", Toast.LENGTH_SHORT).show();
                    }
                });
        EditText message_value = (EditText) findViewById(R.id.message_value);
        RSAEncrypt rsac = new RSAEncrypt();
        //解密
        findViewById(R.id.decrypt_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = message_value.getText().toString();
                        if (message == null || "".equals(message)) {
                            Toast.makeText(SpcCryptoActivity.this, "The message cannot be empty", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                rsac.loadPrivateKey(privkey);
                            } catch (Exception e) {
                                Toast.makeText(SpcCryptoActivity.this, "The private key is wrong", Toast.LENGTH_SHORT).show();
                            }
                            try {
                                byte[] decodeMessage = Base64.decode(message.getBytes(), android.util.Base64.DEFAULT);
                                byte[] cipher = rsac.decrypt(rsac.getPrivateKey2(), decodeMessage);
                                resultEdit.setText(new String(cipher));
                            } catch (Exception e) {
                                Toast.makeText(SpcCryptoActivity.this, "Decryption failure", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        //加密
        findViewById(R.id.encrypt_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = message_value.getText().toString();
                        if (message == null || "".equals(message)) {
                            Toast.makeText(SpcCryptoActivity.this, "The message cannot be empty", Toast.LENGTH_SHORT).show();
                        } else {
                            try {
                                rsac.loadPublicKey(pubkey);
                            } catch (Exception e) {
                                Toast.makeText(SpcCryptoActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                            }

                            try {
                                byte[] cipher = rsac.encrypt(rsac.getPublicKey2(), message.getBytes());
                                String res = Base64.encodeToString(cipher, android.util.Base64.DEFAULT);
                                resultEdit.setText(res);
                            } catch (Exception e) {
                                Toast.makeText(SpcCryptoActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterTextChanged(Editable userEnteredValue) {

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

