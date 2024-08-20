package fman.ge.smart_auth

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
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
        pendingResult = result
        if (!canAutoFill()) {
            pendingResult?.success(null)
            return
        }

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

    private fun canAutoFill(): Boolean {
        val telephonyManager: TelephonyManager? =
            mActivity?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val canReadState = ActivityCompat.checkSelfPermission(
            mContext, android.Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
        if (telephonyManager == null || !canReadState) return false
        return telephonyManager.simState != TelephonyManager.SIM_STATE_ABSENT
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
//        val credential = maybeBuildCredential(call, result) ?: return
//
//        val mCredentialsClient = Credentials.getClient(mContext)
//        mCredentialsClient.save(credential).addOnCompleteListener { task ->
//            if (task.isSuccessful) {
//                result.success(true)
//                return@addOnCompleteListener
//            }
//            val exception = task.exception
//            if (exception is ResolvableApiException && exception.statusCode == RESOLUTION_REQUIRED && mActivity != null) {
//                try {
//                    pendingResult = result
//                    exception.startResolutionForResult(
//                        mActivity as Activity, SAVE_CREDENTIAL_REQUEST
//                    )
//                    return@addOnCompleteListener
//                } catch (exception: IntentSender.SendIntentException) {
//                    Log.e(PLUGIN_TAG, "Failed to send resolution.", exception)
//                }
//            }
//            result.success(false)
//        }

    }

    // TODO: Implement the following method
    private fun getCredential(call: MethodCall, result: MethodChannel.Result) {
        result.success(null)
        //        val accountType = call.argument<String?>("accountType")
//        val serverClientId = call.argument<String?>("serverClientId")
//        val idTokenNonce = call.argument<String?>("idTokenNonce")
//        val isIdTokenRequested = call.argument<Boolean?>("isIdTokenRequested")
//        val isPasswordLoginSupported = call.argument<Boolean?>("isPasswordLoginSupported")
//        val showResolveDialog = call.argument<Boolean?>("showResolveDialog") ?: false
//
//
//        val credentialRequest = CredentialRequest.Builder().setAccountTypes(accountType)
//        if (accountType != null) credentialRequest.setAccountTypes(accountType)
//        if (idTokenNonce != null) credentialRequest.setIdTokenNonce(idTokenNonce)
//        if (isIdTokenRequested != null) credentialRequest.setIdTokenRequested(isIdTokenRequested)
//        if (isPasswordLoginSupported != null) credentialRequest.setPasswordLoginSupported(
//            isPasswordLoginSupported
//        )
//        if (serverClientId != null) credentialRequest.setServerClientId(serverClientId)
//
//
//        val credentialsClient: CredentialsClient = Credentials.getClient(mContext)
//        credentialsClient.request(credentialRequest.build())
//            .addOnCompleteListener(OnCompleteListener { task ->
//                if (task.isSuccessful && task.result != null && task.result.credential != null) {
//                    val credential: Credential? = task.result!!.credential
//                    if (credential != null) {
//                        result.success(credentialToMap(credential))
//                        return@OnCompleteListener
//                    }
//                }
//
//                val exception = task.exception
//                if (exception is ResolvableApiException && exception.statusCode == RESOLUTION_REQUIRED && mActivity != null && showResolveDialog) {
//                    try {
//                        pendingResult = result
//                        exception.startResolutionForResult(
//                            mActivity as Activity,
//                            GET_CREDENTIAL_REQUEST,
//                        )
//                        return@OnCompleteListener
//                    } catch (exception: IntentSender.SendIntentException) {
//                        Log.e(PLUGIN_TAG, "Failed to send resolution.", exception)
//                    }
//                }
//
//                result.success(null)
//                return@OnCompleteListener
//            })
    }

    // TODO: Implement the following method
    private fun deleteCredential(call: MethodCall, result: MethodChannel.Result) {
//        val credential = maybeBuildCredential(call, result) ?: return
//        val mCredentialsClient: CredentialsClient = Credentials.getClient(mContext)
//        mCredentialsClient.delete(credential).addOnCompleteListener { task ->
//            result.success(task.isSuccessful)
//        }
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
//        if (resultCode == RESULT_OK && data != null) {
//            val credential: Credential? = data.getParcelableExtra(Credential.EXTRA_KEY)
//            if (credential != null) {
//                ignoreIllegalState { pendingResult?.success(credentialToMap(credential)) }
//                return
//            }
//        }

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
//        if (resultCode == RESULT_OK && data != null) {
//            val credential: Credential? = data.getParcelableExtra(Credential.EXTRA_KEY)
//            if (credential != null) {
//                ignoreIllegalState { pendingResult?.success(credentialToMap(credential)) }
//                return
//            }
//
//        }
        ignoreIllegalState { pendingResult?.success(null) }
    }

//    private fun credentialToMap(credential: Credential): HashMap<String, String?> {
//        val r: HashMap<String, String?> = HashMap()
//        r["accountType"] = credential.accountType
//        r["familyName"] = credential.familyName
//        r["givenName"] = credential.givenName
//        r["id"] = credential.id
//        r["name"] = credential.name
//        r["password"] = credential.password
//        r["profilePictureUri"] = credential.profilePictureUri.toString()
//        return r
//    }

//    private fun maybeBuildCredential(call: MethodCall, result: MethodChannel.Result): Credential? {
//        val accountType: String? = call.argument<String?>("accountType")
//        val id: String? = call.argument<String?>("id")
//        val name: String? = call.argument<String?>("name")
//        val password: String? = call.argument<String?>("password")
//        val profilePictureUri: String? = call.argument<String?>("profilePictureUri")
//
//        if (id == null) {
//            result.success(false)
//            return null
//        }
//
//        val credential = Credential.Builder(id)
//        if (accountType != null) credential.setAccountType(accountType)
//        if (name != null) credential.setName(name)
//        if (password != null) credential.setPassword(password)
//        if (profilePictureUri != null) credential.setProfilePictureUri(Uri.parse(profilePictureUri))
//
//        return credential.build()
//    }

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




