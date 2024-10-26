import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

part 'sms_code_result.dart';

const _defaultCodeMatcher = '\\d{4,8}';

class Methods {
  static const getAppSignature = 'getAppSignature';
  static const startSmsRetriever = 'startSmsRetriever';
  static const stopSmsRetriever = 'stopSmsRetriever';
  static const startSmsUserConsent = 'startSmsUserConsent';
  static const stopSmsUserConsent = 'stopSmsUserConsent';
  static const requestHint = 'requestHint';
}

/// Flutter package for listening SMS code on Android, suggesting phone number, email, saving a credential.
///
/// If you need pin code input like shown below, take a look at [Pinput](https:///github.com/Tkko/Flutter_Pinput) package, SmartAuth is already integrated into it and you can build highly customizable input, that your designers can't even draw in Figma 🤭
/// `Note that only Android is supported, I faked other operating systems because other package is depended on this one and that package works on every system`
///
/// <img src="https:///user-images.githubusercontent.com/26390946/155599527-fe934f2c-5124-4754-bbf6-bb97d55a77c0.gif" height="600"/>
///
/// ## Features:
/// - Android Autofill
///   - SMS Retriever [API](https:///developers.google.com/identity/sms-retriever/overview?hl=en)
///   - SMS User Consent [API](https:///developers.google.com/identity/sms-retriever/user-consent/overview)
/// - Showing Hint Dialog
/// - Getting Saved Credential
/// - Saving Credential
/// - Deleting Credential
class SmartAuth {
  static const MethodChannel _channel = MethodChannel('fman.smart_auth');

  /// This method outputs hash that is required for SMS Retriever API https://developers.google.com/identity/sms-retriever/overview?hl=en
  /// SMS must contain this hash at the end of the text
  /// Note that hash for debug and release if different
  Future<String?> getAppSignature() async {
    try {
      if (!_isAndroid(Methods.getAppSignature)) return null;
      return _channel.invokeMethod<String?>(Methods.getAppSignature);
    } catch (error) {
      debugPrint('SmartAuth: getAppSignature failed: $error');
      return null;
    }
  }

  /// Starts listening to SMS that contains the
  /// App signature [getAppSignature] in the text
  /// returns code if it matches with matcher
  /// More about SMS Retriever
  /// API https://developers.google.com/identity/sms-retriever/overview?hl=en
  ///
  /// If useUserConsentApi is true SMS User Consent API will be used
  /// https://developers.google.com/identity/sms-retriever/user-consent/overview
  /// Which shows confirmations dialog to user to confirm reading the SMS content
  Future<SmsCodeResult> getSmsCode({
    // used to extract code from SMS
    String matcher = _defaultCodeMatcher,
    // Optional parameter for User Consent API
    String? senderPhoneNumber,
    // if true SMS User Consent API will be used otherwise plugin will use SMS Retriever API
    bool useUserConsentApi = false,
  }) async {
    if (senderPhoneNumber != null) {
      assert(
        useUserConsentApi == true,
        'senderPhoneNumber is only supported if useUserConsentApi is true',
      );
    }
    try {
      if (_isAndroid('getSmsCode')) {
        final String? sms;
        if (useUserConsentApi) {
          sms = await _channel.invokeMethod(
            Methods.startSmsUserConsent,
            <String, String?>{'senderPhoneNumber': senderPhoneNumber},
          );
        } else {
          sms = await _channel.invokeMethod(Methods.startSmsRetriever);
        }
        return SmsCodeResult.fromSms(sms, matcher);
      }
    } catch (error) {
      debugPrint('Pinput/SmartAuth: getSmsCode failed: $error');
      return SmsCodeResult.fromSms(null, matcher);
    }

    return SmsCodeResult.fromSms(null, matcher);
  }

  /// Removes listener for [getSmsCode]
  Future<void> removeSmsListener() async {
    if (_isAndroid('removeSmsListener')) {
      try {
        Future.wait([
          removeSmsRetrieverListener(),
          removeSmsUserConsentListener(),
        ]);
      } catch (error) {
        debugPrint('Pinput/SmartAuth: removeSmsListener failed: $error');
      }
    }
  }

  /// Disposes [getSmsCode] if useUserConsentApi is false listener
  Future<bool> removeSmsRetrieverListener() async {
    try {
      if (_isAndroid('removeSmsRetrieverListener')) {
        final res = await _channel.invokeMethod(Methods.stopSmsRetriever);
        return res == true;
      }
    } catch (error) {
      debugPrint('Pinput/SmartAuth: removeSmsRetrieverListener failed: $error');
    }
    return false;
  }

  /// Disposes [getSmsCode] if useUserConsentApi is true listener
  Future<bool> removeSmsUserConsentListener() async {
    try {
      if (_isAndroid('removeSmsUserConsentListener')) {
        final res = await _channel.invokeMethod(Methods.stopSmsUserConsent);
        return res == true;
      }
    } catch (error) {
      debugPrint(
        'Pinput/SmartAuth: removeSmsUserConsentListener failed: $error',
      );
    }
    return false;
  }

  /// Shows hint dialog to user to select phone number or email
  /// Opens dialog of user emails and/or phone numbers
  /// More about hint request https://developers.google.com/identity/smartlock-passwords/android/retrieve-hints
  /// More about parameters https://developers.google.com/android/reference/com/google/android/gms/auth/api/credentials/HintRequest.Builder
  Future<String?> requestHint({
    // Enables returning credential hints where the identifier is an email address,
    // intended for use with a password chosen by the user.
    bool? isEmailAddressIdentifierSupported,
    // Enables returning credential hints where the identifier is a phone number,
    // intended for use with a password chosen by the user or SMS verification.
    bool? isPhoneNumberIdentifierSupported,
    // The list of account types (identity providers) supported by the app.
    // typically in the form of the associated login domain for each identity provider.
    String? accountTypes,
    // Enables button to add account
    bool? showAddAccountButton,
    // Enables button to cancel request
    bool? showCancelButton,
    // Specify whether an ID token should be acquired for hints, if available for the selected credential identifier.This is enabled by default;
    // disable this if your app does not use ID tokens as part of authentication to decrease latency in retrieving credentials and credential hints.
    bool? isIdTokenRequested,
    // Specify a nonce value that should be included in any generated ID token for this request.
    String? idTokenNonce,
    // Specify the server client ID for the backend associated with this app.
    // If a Google ID token can be generated for a retrieved credential or hint,
    // and the specified server client ID is correctly configured to be associated with the app,
    // then it will be used as the audience of the generated token. If a null value is specified,
    // the default audience will be used for the generated ID token.
    String? serverClientId,
    // For formatting the phone number based on countryCode. Default: 'IN'
    String? countryCode,
  }) async {
    if (!_isAndroid(Methods.requestHint)) return null;
    try {
      final String? result = await _channel.invokeMethod<String?>(
        Methods.requestHint,
        <String, String?>{'countryCode': countryCode},
      );
      return result;
    } catch (error) {
      debugPrint('Pinput/SmartAuth: requestHint failed: $error');
      return null;
    }
  }

  /// Checks for current platform and returns true if it is Android
  bool _isAndroid(String method) {
    if (defaultTargetPlatform == TargetPlatform.android) return true;
    debugPrint('SmartAuth $method is not supported on $defaultTargetPlatform');
    return false;
  }
}
