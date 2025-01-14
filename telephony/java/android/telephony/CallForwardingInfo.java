/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.telephony;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.telephony.Annotation.CallForwardingReason;
import android.telephony.Annotation.CallForwardingStatus;

import com.android.telephony.Rlog;

import java.util.Objects;

/**
 * Defines the call forwarding information.
 * @hide
 */
@SystemApi
public final class CallForwardingInfo implements Parcelable {
    private static final String TAG = "CallForwardingInfo";

    /**
     * Indicates the call forwarding status is inactive.
     *
     * @hide
     */
    @SystemApi
    public static final int STATUS_INACTIVE = 0;

    /**
     * Indicates the call forwarding status is active.
     *
     * @hide
     */
    @SystemApi
    public static final int STATUS_ACTIVE = 1;

    /**
     * Indicates the call forwarding could not be enabled because the recipient is not on
     * Fixed Dialing Number (FDN) list.
     *
     * @hide
     */
    @SystemApi
    public static final int STATUS_FDN_CHECK_FAILURE = 2;

    /**
     * Indicates the call forwarding status is with an unknown error.
     *
     * @hide
     */
    @SystemApi
    public static final int STATUS_UNKNOWN_ERROR = 3;

    /**
     * Indicates the call forwarding is not supported (e.g. called via CDMA).
     *
     * @hide
     */
    @SystemApi
    public static final int STATUS_NOT_SUPPORTED = 4;

    /**
     * Indicates the call forwarding reason is "unconditional".
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     * @hide
     */
    @SystemApi
    public static final int REASON_UNCONDITIONAL = 0;

    /**
     * Indicates the call forwarding status is "busy".
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     * @hide
     */
    @SystemApi
    public static final int REASON_BUSY = 1;

    /**
     * Indicates the call forwarding reason is "no reply".
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     * @hide
     */
    @SystemApi
    public static final int REASON_NO_REPLY = 2;

    /**
     * Indicates the call forwarding reason is "not reachable".
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     * @hide
     */
    @SystemApi
    public static final int REASON_NOT_REACHABLE = 3;

    /**
     * Indicates the call forwarding reason is "all", for setting all call forwarding reasons
     * simultaneously (unconditional, busy, no reply, and not reachable).
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     * @hide
     */
    @SystemApi
    public static final int REASON_ALL = 4;

    /**
     * Indicates the call forwarding reason is "all_conditional", for setting all conditional call
     * forwarding reasons simultaneously (busy, no reply, and not reachable).
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     * @hide
     */
    @SystemApi
    public static final int REASON_ALL_CONDITIONAL = 5;

    /**
     * The call forwarding status.
     */
    private @CallForwardingStatus int mStatus;

    /**
     * The call forwarding reason indicates the condition under which calls will be forwarded.
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     */
    private @CallForwardingReason int mReason;

    /**
     * The phone number to which calls will be forwarded.
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10 - 7.11 Call forwarding number
     *            and conditions +CCFC
     */
    private String mNumber;

    /**
     * Gets the timeout (in seconds) before the forwarding is attempted.
     */
    private int mTimeSeconds;

    /**
     * Construct a CallForwardingInfo.
     *
     * @param status the call forwarding status
     * @param reason the call forwarding reason
     * @param number the phone number to which calls will be forwarded
     * @param timeSeconds the timeout (in seconds) before the forwarding is attempted
     * @hide
     */
    @SystemApi
    public CallForwardingInfo(@CallForwardingStatus int status, @CallForwardingReason int reason,
            @Nullable String number, int timeSeconds) {
        mStatus = status;
        mReason = reason;
        mNumber = number;
        mTimeSeconds = timeSeconds;
    }

    /**
     * Returns the call forwarding status.
     *
     * @return the call forwarding status.
     *
     * @hide
     */
    @SystemApi
    public @CallForwardingStatus int getStatus() {
        return mStatus;
    }

    /**
     * Returns the call forwarding reason. The call forwarding reason indicates the condition
     * under which calls will be forwarded.  For example, {@link #REASON_NO_REPLY} indicates
     * that calls will be forward to {@link #getNumber()} when the user fails to answer the call.
     *
     * @return the call forwarding reason.
     *
     * @hide
     */
    @SystemApi
    public @CallForwardingReason int getReason() {
        return mReason;
    }

    /**
     * Returns the phone number to which calls will be forwarded.
     *
     * @return the number calls will be forwarded to, or {@code null} if call forwarding
     * is being disabled.
     *
     * @hide
     */
    @SystemApi
    @Nullable
    public String getNumber() {
        return mNumber;
    }

    /**
     * Gets the timeout (in seconds) before the forwarding is attempted. For example,
     * if {@link #REASON_NO_REPLY} is the call forwarding reason, the device will wait this
     * duration of time before forwarding the call to {@link #getNumber()}.
     *
     * Reference: 3GPP TS 27.007 version 10.3.0 Release 10
     *            7.11 Call forwarding number and conditions +CCFC
     *
     * @return the timeout (in seconds) before the forwarding is attempted.
     *
     * @hide
     */
    @SystemApi
    @SuppressLint("MethodNameUnits")
    public int getTimeoutSeconds() {
        return mTimeSeconds;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * @hide
     */
    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mNumber);
        out.writeInt(mStatus);
        out.writeInt(mReason);
        out.writeInt(mTimeSeconds);
    }

    private CallForwardingInfo(Parcel in) {
        mNumber = in.readString();
        mStatus = in.readInt();
        mReason = in.readInt();
        mTimeSeconds = in.readInt();
    }

    /**
     * @hide
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof CallForwardingInfo)) {
            return false;
        }

        CallForwardingInfo other = (CallForwardingInfo) o;
        return mStatus == other.mStatus
                && mNumber == other.mNumber
                && mReason == other.mReason
                && mTimeSeconds == other.mTimeSeconds;
    }

    /**
     * @hide
     */
    @Override
    public int hashCode() {
        return Objects.hash(mStatus, mNumber, mReason, mTimeSeconds);
    }

    public static final @NonNull Parcelable.Creator<CallForwardingInfo> CREATOR =
            new Parcelable.Creator<CallForwardingInfo>() {
                @Override
                public CallForwardingInfo createFromParcel(Parcel in) {
                    return new CallForwardingInfo(in);
                }

                @Override
                public CallForwardingInfo[] newArray(int size) {
                    return new CallForwardingInfo[size];
                }
            };

    /**
     * @hide
     */
    @Override
    public String toString() {
        return "[CallForwardingInfo: status=" + mStatus
                + ", reason= " + mReason
                + ", timeSec= " + mTimeSeconds + " seconds"
                + ", number=" + Rlog.pii(TAG, mNumber) + "]";
    }
}
