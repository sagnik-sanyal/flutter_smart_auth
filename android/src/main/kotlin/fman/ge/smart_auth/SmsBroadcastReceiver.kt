package fman.ge.smart_auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import io.flutter.plugin.common.MethodChannel


/**
 * SMS Retriever API
 * [https://developers.google.com/identity/sms-retriever/overview]
 */
class SmsBroadcastReceiver(
    private val mContext: Context,
    private val pendingResult: MethodChannel.Result?
) : BroadcastReceiver() {

    companion object {
        private const val PLUGIN_TAG = "Pinput/SmartAuth"
    }

    private fun unregisterReceiver(receiver: BroadcastReceiver?) {
        try {
            receiver?.let { mContext.unregisterReceiver(it) }
        } catch (exception: Exception) {
            Log.e(PLUGIN_TAG, "Unregistering receiver failed.", exception)
        }
    }

    internal fun removeSmsRetrieverListener() {
        unregisterReceiver(this)
    }

    // OnReceive method is called when the BroadcastReceiver is receiving an Intent broadcast.
    override fun onReceive(context: Context, intent: Intent) {
        if (SmsRetriever.SMS_RETRIEVED_ACTION == intent.action) {
            removeSmsRetrieverListener()
            if (intent.extras != null && intent.extras!!.containsKey(SmsRetriever.EXTRA_STATUS)) {
                val extras = intent.extras!!
                val smsRetrieverStatus = extras.get(SmsRetriever.EXTRA_STATUS) as Status
                when (smsRetrieverStatus.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        val smsContent = extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE)
                        if (smsContent != null) {
                            ignoreIllegalState { pendingResult?.success(smsContent) }
                        } else {
                            Log.e(
                                PLUGIN_TAG,
                                "Retrieved SMS is null, check if SMS contains correct app signature"
                            )
                            ignoreIllegalState { pendingResult?.success(null) }
                        }
                    }

                    CommonStatusCodes.TIMEOUT -> {
                        Log.e(
                            PLUGIN_TAG,
                            "SMS Retriever API timed out, check if SMS contains correct app signature"
                        )
                        ignoreIllegalState { pendingResult?.success(null) }
                    }

                    else -> {
                        Log.e(
                            PLUGIN_TAG,
                            "SMS Retriever API failed with status code: ${smsRetrieverStatus.statusCode}, check if SMS contains correct app signature"
                        )
                        ignoreIllegalState { pendingResult?.success(null) }
                    }
                }
            } else {
                Log.e(
                    PLUGIN_TAG,
                    "SMS Retriever API failed with no status code, check if SMS contains correct app signature"
                )
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
