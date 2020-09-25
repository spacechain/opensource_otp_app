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

import android.os.Parcel;
import android.os.Parcelable;
import com.google.android.apps.authenticator.spc.SpcAccountDb.AccountIndex;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;

/**
 * A tuple of user, OTP value, and type, that represents a particular user.
 *
 * <p>It Implements {@link Parcelable} so that this class can be conveyed through {@link
 * android.os.Bundle}.
 */
public class SpcAccountInfo implements Parcelable {

  /** Calculated OTP, or a placeholder if not calculated */
  @Nullable
  private String mPin = null;
  @Nullable
  private String mPubkey = null;
  @Nullable
  private String mPrivkey = null;

  /** {@link AccountIndex} that owns the pin */
  private final AccountIndex mIndex;

  /**
   * Used to represent whether the OTP type is TOTP or HOTP.
   *
   * <ul>
   *   <li>true: HOTP (counter based)
   *   <li>false: TOTP (time based)
   * </ul>
   **/
  private final boolean mIsHotp;

  /** HOTP only: Whether code generation is allowed for this account. */
  private boolean mHotpCodeGenerationAllowed = false;

  /**
   * Constructor of {@link PinInfo}. The default value for mIsHotp is false.
   *
   * @param index the {@link AccountIndex} that owns the pin
   */
  public SpcAccountInfo(AccountIndex index) {
    this(index, false);
  }

  /**
   * Constructor of {@link PinInfo}.
   *
   * @param index {@link AccountIndex} that owns the pin
   * @param isHotp represents whether the OTP type is TOTP or HOTP
   */
  public SpcAccountInfo(AccountIndex index, boolean isHotp) {
    mIndex = Preconditions.checkNotNull(index);
    mIsHotp = isHotp;
  }

  public SpcAccountInfo(Parcel pc) {
    // Using readValue instead of readString since mPin can be null.
    mPin = (String) pc.readValue(SpcAccountInfo.class.getClassLoader());
    mIndex = (AccountIndex) pc.readSerializable();
    boolean[] booleanArray = new boolean[2];
    pc.readBooleanArray(booleanArray);
    mIsHotp = booleanArray[0];
    mHotpCodeGenerationAllowed = booleanArray[1];
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel pc, int flags) {
    // Using writeValue instead of writeString since mPin can be null.
    pc.writeValue(mPin);
    pc.writeSerializable(mIndex);
    pc.writeBooleanArray(new boolean[] { mIsHotp, mHotpCodeGenerationAllowed });
  }

  public static final Parcelable.Creator<SpcAccountInfo> CREATOR = new Parcelable.Creator<SpcAccountInfo>() {
    @Override
    public SpcAccountInfo createFromParcel(Parcel in) {
      return new SpcAccountInfo(in);
    }

    @Override
    public SpcAccountInfo[] newArray(int size) {
      return new SpcAccountInfo[size];
    }
  };

  @Nullable
  public String getPin() {
    return mPin;
  }
  @Nullable
  public String getPubkey() {
    return mPubkey;
  }
  @Nullable
  public String getPrivkey() {
    return mPrivkey;
  }

  public SpcAccountInfo setPin(String pin) {
    mPin = pin;
    return this;
  }
  public SpcAccountInfo setPubkey(String pubkey) {
    mPubkey = pubkey;
    return this;
  }
  public SpcAccountInfo setPrivkey(String privkey) {
    mPrivkey = privkey;
    return this;
  }

  public AccountIndex getIndex() {
    return mIndex;
  }

  public boolean isHotp() {
    return mIsHotp;
  }

  public boolean isHotpCodeGenerationAllowed() {
    return mHotpCodeGenerationAllowed;
  }

  public SpcAccountInfo setIsHotpCodeGenerationAllowed(boolean hotpCodeGenerationAllowed) {
    mHotpCodeGenerationAllowed = hotpCodeGenerationAllowed;
    return this;
  }

  public static void swapIndex(SpcAccountInfo[] pinInfoArray, int i, int j) {
    SpcAccountInfo pinInfo = pinInfoArray[i];
    pinInfoArray[i] = pinInfoArray[j];
    pinInfoArray[j] = pinInfo;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(mPin, mIndex, mIsHotp, mHotpCodeGenerationAllowed);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof SpcAccountInfo)) {
      return false;
    }
    SpcAccountInfo other = (SpcAccountInfo) obj;
    return Objects.equal(other.mIndex, mIndex) && Objects.equal(other.mIsHotp, mIsHotp)
            && Objects.equal(other.mPin, mPin)
            && Objects.equal(other.mHotpCodeGenerationAllowed, mHotpCodeGenerationAllowed);
  }

  @Override
  public String toString() {
    return String.format("PinInfo {mPin=%s, mIndex=%s, mIsHotp=%s, mHotpCodeGenerationAllowed=%s}",
            mPin, mIndex, mIsHotp, mHotpCodeGenerationAllowed);
  }
}

