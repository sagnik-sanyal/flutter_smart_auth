package fman.ge.smart_auth

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.identity.GetPhoneNumberHintIntentRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.phone.SmsRetriever
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry


/** SmartAuthPlugin */

class SmartAuthPlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
    PluginRegistry.ActivityResultListener {
    private lateinit var mContext: Context
    private var mActivity: Activity? = null
    private var mBinding: ActivityPluginBinding? = null
    private var mChannel: MethodChannel? = null
    private var pendingResult: MethodChannel.Result? = null
    private var smsReceiver: SmsBroadcastReceiver? = null
    private var consentReceiver: ConsentBroadcastReceiver? = null

    companion object {
        private const val PLUGIN_TAG = "Pinput/SmartAuth"
        private const val HINT_REQUEST = 11100
        private const val USER_CONSENT_REQUEST = 11101
        private const val SAVE_CREDENTIAL_REQUEST = 11102
        private const val GET_CREDENTIAL_REQUEST = 11103
    }

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mChannel = MethodChannel(flutterPluginBinding.binaryMessenger, "fman.smart_auth")
        mContext = flutterPluginBinding.applicationContext
        mChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        dispose()
        mChannel?.setMethodCallHandler(null)
        mChannel = null
    }

    override fun onDetachedFromActivity() = dispose()

    override fun onDetachedFromActivityForConfigChanges() = dispose()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        mActivity = binding.activity
        mBinding = binding
        binding.addActivityResultListener(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        mActivity = binding.activity
        mBinding = binding
        binding.addActivityResultListener(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getAppSignature" -> getSignature(result)
            "startSmsRetriever" -> startSmsRetriever(result)
            "startSmsUserConsent" -> startSmsUserConsent(call, result)
            "stopSmsRetriever" -> stopSmsRetriever(result)
            "stopSmsUserConsent" -> stopSmsUserConsent(result)
            "requestHint" -> requestHint(result)
            "saveCredential" -> saveCredential(call, result)
            "deleteCredential" -> deleteCredential(call, result)
            "getCredential" -> getCredential(call, result)
            else -> result.notImplemented()
        }
    }

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ): Boolean {
        when (requestCode) {
            HINT_REQUEST -> onHintRequest(resultCode, data)
            USER_CONSENT_REQUEST -> onSmsConsentRequest(resultCode, data)
            SAVE_CREDENTIAL_REQUEST -> onSaveCredentialRequest(resultCode)
            GET_CREDENTIAL_REQUEST -> onGetCredentialRequest(resultCode, data)
        }
        return true
    }

    private fun getSignature(result: MethodChannel.Result) {
        val signatures = AppSignatureHelper(mContext).getAppSignatures()
        result.success(signatures.getOrNull(0))
    }

    private fun requestHint(result: MethodChannel.Result) {
        val request: GetPhoneNumberHintIntentRequest =
            GetPhoneNumberHintIntentRequest.builder().build()

        mActivity?.let {

            Identity.getSignInClient(it).getPhoneNumberHintIntent(request)
                .addOnSuccessListener { pendingIntent ->
                    try {
                        startIntentSenderForResult(
                            mActivity!!,
                            IntentSenderRequest.Builder(pendingIntent).build().intentSender,
                            HINT_REQUEST,
                            null,
                            0,
                            0,
                            0,
                            null
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pendingResult?.error("ERROR", e.message, e)
                    }
                }.addOnFailureListener { e ->
                    e.printStackTrace()
                    pendingResult?.error("ERROR", e.message, e)
                }
        }

    }

    fun startActivityForResult(consentIntent: Intent, userConsentRequest: Int) {
        if (mActivity != null) {
            this@SmartAuthPlugin.mActivity?.startActivityForResult(
                consentIntent, userConsentRequest
            )
        }
    }

    // TODO: Implement the following method
    private fun saveCredential(call: MethodCall, result: MethodChannel.Result) {
        result.success(null)
    }

    // TODO: Implement the following method
    private fun getCredential(call: MethodCall, result: MethodChannel.Result) {
        result.success(null)
    }

    // TODO: Implement the following method
    private fun deleteCredential(call: MethodCall, result: MethodChannel.Result) {
        result.success(null)
    }

    private fun startSmsRetriever(result: MethodChannel.Result) {
        unregisterAllReceivers()
        pendingResult = result
        smsReceiver = SmsBroadcastReceiver(mContext, pendingResult)
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(
            mContext,
            smsReceiver,
            intentFilter,
            SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )
        SmsRetriever.getClient(mContext).startSmsRetriever()
    }

    private fun stopSmsRetriever(result: MethodChannel.Result) {
        if (smsReceiver == null) {
            result.success(false)
        } else {
            removeSmsRetrieverListener()
            result.success(true)
        }
    }

    private fun removeSmsRetrieverListener() {
        smsReceiver?.removeSmsRetrieverListener()
        smsReceiver = null
    }

    private fun startSmsUserConsent(call: MethodCall, result: MethodChannel.Result) {
        unregisterAllReceivers()
        pendingResult = result
        consentReceiver = ConsentBroadcastReceiver(mContext, pendingResult, this)
        val intentFilter = IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION)
        ContextCompat.registerReceiver(
            mContext,
            consentReceiver,
            intentFilter,
            SmsRetriever.SEND_PERMISSION,
            null,
            ContextCompat.RECEIVER_EXPORTED
        )
        SmsRetriever.getClient(mContext).startSmsUserConsent(call.argument("senderPhoneNumber"))
    }

    private fun stopSmsUserConsent(result: MethodChannel.Result) {
        if (consentReceiver == null) {
            result.success(false)
        } else {
            removeSmsUserConsentListener()
            result.success(true)
        }
    }

    private fun removeSmsUserConsentListener() {
        consentReceiver?.removeSmsUserConsentListener()
        consentReceiver = null
    }

    private fun onHintRequest(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            if (data.hasExtra(SmsRetriever.EXTRA_SMS_MESSAGE)) {
                val phoneNumber: String =
                    Identity.getSignInClient(mContext).getPhoneNumberFromIntent(data)
                ignoreIllegalState { pendingResult?.success(phoneNumber) }
                return
            }
        }

        ignoreIllegalState { pendingResult?.success(null) }
    }

    private fun onSmsConsentRequest(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && data != null) {
            val message = data.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)
            ignoreIllegalState { pendingResult?.success(message) }
        } else {
            ignoreIllegalState { pendingResult?.success(null) }
        }
    }

    private fun onSaveCredentialRequest(resultCode: Int) {
        ignoreIllegalState { pendingResult?.success(resultCode == RESULT_OK) }
    }

    // TODO: Implement the following method
    private fun onGetCredentialRequest(resultCode: Int, data: Intent?) {
        ignoreIllegalState { pendingResult?.success(null) }
    }

    private fun dispose() {
        unregisterAllReceivers()
        ignoreIllegalState { pendingResult?.success(null) }
        mActivity = null
        mBinding?.removeActivityResultListener(this)
        mBinding = null
    }

    private fun unregisterAllReceivers() {
        removeSmsRetrieverListener()
        removeSmsUserConsentListener()
    }

    private fun ignoreIllegalState(fn: () -> Unit) {
        try {
            fn()
        } catch (e: IllegalStateException) {
            Log.e(PLUGIN_TAG, "ignoring exception: $e")
        }
    }

}




