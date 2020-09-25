
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

import com.google.android.apps.authenticator.testability.TestableActivity;
import com.google.android.apps.authenticator2.R;
import com.google.android.apps.authenticator.spc.RSAEncrypt;
import android.util.Base64;

/**
 * The activity that lets the user manually add an account by entering its name, key, and type
 * (TOTP/HOTP).
 */
public class SpcEncryptActivity extends TestableActivity implements TextWatcher {




    /** Called when the activity is first created */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spc_encrypt);

        findViewById(R.id.encrypt_btn)
                .setOnClickListener(encryptButtonOnClickListener);


    }





    @SuppressWarnings("deprecation") // 点击加密按钮
    private final View.OnClickListener encryptButtonOnClickListener =
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    EditText message_value = (EditText) findViewById(R.id.message_value);
                    String message=message_value.getText().toString();

                    EditText key_value = (EditText) findViewById(R.id.key_value);
                    String pubkeye=key_value.getText().toString();

                    if(message == null || "".equals(message) || pubkeye == null ||"".equals(pubkeye)  ){

                        Toast.makeText(
                                SpcEncryptActivity.this, "The message cannot be empty", Toast.LENGTH_SHORT)
                                .show();
                    }else{

                        RSAEncrypt rsac = new RSAEncrypt();

                        try {
                            rsac.loadPublicKey(pubkeye);
                        } catch (Exception e) {
                            Toast.makeText(
                                    SpcEncryptActivity.this, "The public key is wrong", Toast.LENGTH_SHORT)
                                    .show();
                        }

                        try {

                            byte[] cipher = rsac.encrypt(rsac.getPublicKey2(), message.getBytes());
                            String res=Base64.encodeToString(cipher, android.util.Base64.URL_SAFE |android.util.Base64.NO_WRAP |  android.util.Base64.NO_PADDING);


                            EditText encrypt_result = (EditText) findViewById(R.id.encrypt_result);
                            encrypt_result.setText(res);


                        }catch (Exception e) {

                            Toast.makeText(
                                    SpcEncryptActivity.this, "Encryption failed", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    }

                }
            };


    /** {@inheritDoc} */
    @Override
    public void afterTextChanged(Editable userEnteredValue) {

    }

    /** {@inheritDoc} */
    @Override
    public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override
    public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
        // Do nothing
    }
}

