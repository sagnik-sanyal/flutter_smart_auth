package fman.ge.smart_auth

import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import io.flutter.plugin.common.MethodChannel

/**
 * SMS User Consent API
 * [https://developers.google.com/identity/sms-retriever/user-consent/overview]
 */
class ConsentBroadcastReceiver(
    private val mContext: Context,
    private val pendingResult: MethodChannel.Result?,
    private var mActivity: SmartAuthPlugin?,
) : BroadcastReceiver() {

    companion object {
        private const val PLUGIN_TAG = "Pinput/SmartAuth"
        private const val USER_CONSENT_REQUEST = 11101
    }


    private fun unregisterReceiver(receiver: BroadcastReceiver?) {
        try {
            receiver?.let { mContext.unregisterReceiver(it) }
        } catch (exception: Exception) {
            Log.e(PLUGIN_TAG, "Unregistering receiver failed.", exception)
        }
    }

    internal fun removeSmsUserConsentListener() {
        unregisterReceiver(this)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            removeSmsUserConsentListener()
            if (intent.extras != null && intent.extras!!.containsKey(SmsRetriever.EXTRA_STATUS)) {
                val extras = intent.extras!!
                val smsRetrieverStatus = extras.get(SmsRetriever.EXTRA_STATUS) as Status
                when (smsRetrieverStatus.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        try {
                            val consentIntent: Intent? =
                                extras.getParcelable(SmsRetriever.EXTRA_CONSENT_INTENT)

                            if (consentIntent != null && mActivity != null) {
                                mActivity?.startActivityForResult(
                                    consentIntent,
                                    USER_CONSENT_REQUEST
                                )
                            } else {
                                Log.e(
                                    PLUGIN_TAG,
                                    "ConsentBroadcastReceiver error: Can't start consent intent. consentIntent or mActivity is null"
                                )
                                ignoreIllegalState { pendingResult?.success(null) }
                            }
                        } catch (e: ActivityNotFoundException) {
                            Log.e(PLUGIN_TAG, "ConsentBroadcastReceiver error: $e")
                            ignoreIllegalState { pendingResult?.success(null) }
                        }
                    }

                    CommonStatusCodes.TIMEOUT -> {
                        Log.e(PLUGIN_TAG, "ConsentBroadcastReceiver Timeout")
                        ignoreIllegalState { pendingResult?.success(null) }
                    }

                    else -> {
                        Log.e(
                            PLUGIN_TAG,
                            "ConsentBroadcastReceiver failed with status code: ${smsRetrieverStatus.statusCode}"
                        )
                        ignoreIllegalState { pendingResult?.success(null) }
                    }
                }

            } else {
                Log.e(PLUGIN_TAG, "ConsentBroadcastReceiver failed with no status code")
                ignoreIllegalState { pendingResult?.success(null) }
            }

        }
    }

    private fun ignoreIllegalState(fn: () -> Unit) {
        try {
            fn()
        } catch (e: IllegalStateException) {
            Log.e(PLUGIN_TAG, "ignoring exception: $e")
        }
    }
}
