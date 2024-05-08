package org.apache.cordova.gpay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wallet.AutoResolveHelper;
import com.google.android.gms.wallet.CardInfo;
import com.google.android.gms.wallet.CardRequirements;
import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentsClient;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.IsReadyToPayRequest;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class GooglePay extends CordovaPlugin {
    private static final String IS_READY_TO_PAY = "is_ready_to_pay";
    private static final String REQUEST_PAYMENT = "request_payment";

    private static final int LOAD_PAYMENT_DATA_REQUEST_CODE = 991;

    private PaymentsClient paymentsClient = null;
    private CallbackContext callback;

    @Override
    protected void pluginInitialize() {
        this.paymentsClient = Wallet.getPaymentsClient(
            this.cordova.getActivity().getApplicationContext(),
            new Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_PRODUCTION)
            .build()
        );
    }

    @Override
    public boolean execute(final String action, JSONArray data, CallbackContext callbackContext) throws JSONException {
        this.callback = callbackContext;

        // These actions require the key to be already set
        if (this.isInitialised()) {
            this.callback.error("GPay not initialised.");
        }

        if (action.equals(IS_READY_TO_PAY)) {
            this.isReadyToPay();
        } else if (action.equals(REQUEST_PAYMENT)) {
            this.requestPayment(data.getString(0), data.getString(1), data.getString(2));
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case LOAD_PAYMENT_DATA_REQUEST_CODE:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                       // try {
                            PaymentData paymentData = PaymentData.getFromIntent(data);

                            //String paymentInformation = paymentData.toJson();

                            // Token will be null if PaymentDataRequest was not constructed using fromJson(String).
                            //if (paymentInformation == null) {
                            //    this.callback.error("An error occurred in processing payment");
                            //    return;
                            //}
                            //JSONObject paymentMethodData;


                            //paymentMethodData = new JSONObject(paymentInformation).getJSONObject("paymentMethodData");

                            // You can get some data on the user's card, such as the brand and last 4 digits
                            CardInfo info = paymentData.getCardInfo();
                            // You can also pull the user address from the PaymentData object.
                            UserAddress address = paymentData.getShippingAddress();
                            // This is the raw JSON string version of your G Pay token.
                            String paymentToken = paymentData.getPaymentMethodToken().getToken();

                            //Token mpgsToken = Token.fromString(rawToken);

                            Log.d("GooglePaymentToken", paymentData.getPaymentMethodToken().getToken());

                            //String paymentToken = data.getStringExtra(CollectCardInfoActivity.EXTRA_PAYMENT_TOKEN);
                            //String paymentToken = paymentMethodData.getJSONObject("tokenizationData").getString("token");

                            if (paymentToken != null) {
                                // This chargeToken function is a call to your own server, which should then connect
                                // to Master Card's API to finish the charge.
                                this.callback.success(paymentToken);
                            } else {
                                this.callback.error("An error occurred in processing payment");
                            }
                            //} catch (JSONException e) {
                            //Log.d("GooglePaymentToken", "Error: " + e.toString());
                            //return;
                            //}
                            break;
                            case Activity.RESULT_CANCELED:
                                this.callback.error("Payment cancelled");
                                break;
                            case AutoResolveHelper.RESULT_ERROR:
                                Status status = AutoResolveHelper.getStatusFromIntent(data);
                                this.callback.error("Error in processing your payment");
                                // Log the status for debugging
                                // Generally there is no need to show an error to
                                // the user as the Google Payment API will do that
                                break;
                            default:
                                // Do nothing.
                        }
                        break; // Breaks the case LOAD_PAYMENT_DATA_REQUEST_CODE
                        // Handle any other startActivityForResult calls you may have made.
                    default:
                        // Do nothing.
                }
        }

        private boolean isInitialised() {
            return this.paymentsClient == null;
        }

        private void isReadyToPay() {
            IsReadyToPayRequest request = IsReadyToPayRequest.newBuilder()
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .build();

            Task < Boolean > task = this.paymentsClient.isReadyToPay(request);
            final CallbackContext callbackContext = this.callback;
            task.addOnCompleteListener(
                new OnCompleteListener < Boolean > () {
                    public void onComplete(Task < Boolean > task) {
                        try {
                            boolean result = task.getResult(ApiException.class);
                            if (!result) callbackContext.error("Not supported");
                            else callbackContext.success();

                        } catch (ApiException exception) {
                            callbackContext.error(exception.getMessage());
                        }
                    }
                });
        }

        private void requestPayment(String merchantidentifier, String totalPrice, String currency) {
            //Optional < JSONObject > paymentDataRequestJson = this.getPaymentDataRequest(merchantidentifier, totalPrice, currency);
            //if (!paymentDataRequestJson.isPresent()) {
            //    return;
            //}
            //PaymentDataRequest request =
            //    PaymentDataRequest.fromJson(paymentDataRequestJson.get().toString());

            PaymentDataRequest request = this.createPaymentDataRequest(merchantidentifier, totalPrice, currency);
            Activity activity = this.cordova.getActivity();
            if (request != null) {
                cordova.setActivityResultCallback(this);
                AutoResolveHelper.resolveTask(
                    this.paymentsClient.loadPaymentData(request),
                    activity,
                    LOAD_PAYMENT_DATA_REQUEST_CODE);
            }
        }

        private PaymentMethodTokenizationParameters createTokenisationParameters(String merchantidentifier) {
            return PaymentMethodTokenizationParameters.newBuilder()
                .setPaymentMethodTokenizationType(WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY)
                .addParameter("gateway", "mpgs")
                .addParameter("gatewayMerchantId", merchantidentifier)
                .build();
        }

        private PaymentDataRequest createPaymentDataRequest(String merchantidentifier, String totalPrice, String currency) {
            PaymentDataRequest.Builder request =
                PaymentDataRequest.newBuilder()
                .setTransactionInfo(
                    TransactionInfo.newBuilder()
                    .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                    .setTotalPrice(totalPrice)
                    .setCurrencyCode(currency)
                    .build())
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_CARD)
                .addAllowedPaymentMethod(WalletConstants.PAYMENT_METHOD_TOKENIZED_CARD)
                .setCardRequirements(
                    CardRequirements.newBuilder()
                    .addAllowedCardNetworks(Arrays.asList(
                        WalletConstants.CARD_NETWORK_AMEX,
                        WalletConstants.CARD_NETWORK_DISCOVER,
                        WalletConstants.CARD_NETWORK_VISA,
                        WalletConstants.CARD_NETWORK_MASTERCARD))
                    .build());

            request.setPaymentMethodTokenizationParameters(this.createTokenisationParameters(merchantidentifier));
            return request.build();
        }

        //}
        private Optional < JSONObject > getPaymentDataRequest(String merchantidentifier, String totalPrice, String currency) {
            try {
                JSONObject paymentDataRequest = this.getBaseRequest();
                paymentDataRequest.put(
                    "allowedPaymentMethods", new JSONArray().put(this.getCardPaymentMethod(merchantidentifier)));
                paymentDataRequest.put("transactionInfo", this.getTransactionInfo(totalPrice, currency));
                paymentDataRequest.put("merchantInfo", this.getMerchantInfo(merchantidentifier));

                /* An optional shipping address requirement is a top-level property of the PaymentDataRequest
                JSON object. */
                paymentDataRequest.put("shippingAddressRequired", false);

                return Optional.of(paymentDataRequest);
            } catch (JSONException e) {
                return Optional.empty();
            }
        }

        private JSONObject getMerchantInfo(String merchantidentifier) throws JSONException {
            return new JSONObject().put("merchantName", merchantidentifier);
        }

        private JSONObject getTransactionInfo(String price, String currency) throws JSONException {
            JSONObject transactionInfo = new JSONObject();
            transactionInfo.put("totalPrice", price);
            transactionInfo.put("totalPriceStatus", "FINAL");
            transactionInfo.put("countryCode", "AE");
            transactionInfo.put("currencyCode", currency);

            return transactionInfo;
        }

        private JSONObject getBaseRequest() throws JSONException {
            return new JSONObject().put("apiVersion", 2).put("apiVersionMinor", 0);
        }

        private JSONObject getCardPaymentMethod(String merchantidentifier) throws JSONException {
            JSONObject cardPaymentMethod = getBaseCardPaymentMethod();
            cardPaymentMethod.put("tokenizationSpecification", getGatewayTokenizationSpecification(merchantidentifier));

            return cardPaymentMethod;
        }

        private JSONObject getBaseCardPaymentMethod() throws JSONException {
            JSONObject cardPaymentMethod = new JSONObject();
            cardPaymentMethod.put("type", "CARD");

            JSONObject parameters = new JSONObject();
            parameters.put("allowedAuthMethods", getAllowedCardAuthMethods());
            parameters.put("allowedCardNetworks", getAllowedCardNetworks());
            // Optionally, you can add billing address/phone number associated with a CARD payment method.
            parameters.put("billingAddressRequired", true);

            JSONObject billingAddressParameters = new JSONObject();
            billingAddressParameters.put("format", "FULL");

            parameters.put("billingAddressParameters", billingAddressParameters);

            cardPaymentMethod.put("parameters", parameters);

            return cardPaymentMethod;
        }

        private JSONObject getGatewayTokenizationSpecification(String merchantidentifier) throws JSONException {
            return new JSONObject() {
                {
                    put("type", "PAYMENT_GATEWAY");
                    put("parameters", new JSONObject() {
                        {
                            put("gateway", "mpgs");
                            put("gatewayMerchantId", merchantidentifier);
                        }
                    });
                }
            };
        }

        private JSONArray getAllowedCardNetworks() {
            return new JSONArray(this.SUPPORTED_NETWORKS);
        }
        private JSONArray getAllowedCardAuthMethods() {
            return new JSONArray(this.SUPPORTED_METHODS);
        }
        public final List < String > SUPPORTED_NETWORKS = Arrays.asList(
            "AMEX",
            "DISCOVER",
            "JCB",
            "MASTERCARD",
            "VISA");

        public final List < String > SUPPORTED_METHODS =
            Arrays.asList(
                //"PAN_ONLY",
                "CRYPTOGRAM_3DS");
    }
