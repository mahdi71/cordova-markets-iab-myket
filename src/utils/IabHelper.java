package ir.miladesign.utils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import ir.miladesign.MdMarketsIAB;

import com.android.vending.billing.IInAppBillingService;
import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.LOG;
import org.json.JSONException;

public class IabHelper {
    boolean mDebugLog = false;
    String mDebugTag = "IabHelper";
    boolean mSetupDone = false;
    boolean mDisposed = false;
    boolean mSubscriptionsSupported = false;
    boolean mAsyncInProgress = false;
    String mAsyncOperation = "";
    Context mContext;
    IInAppBillingService mService;
    ServiceConnection mServiceConn;
    int mRequestCode;
    String mPurchasingItemType;
    String mSignatureBase64 = null;
    String mServiceName = null;
    String mPackageName = null;
    public static final int BILLING_RESPONSE_RESULT_OK = 0;
    public static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
    public static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
    public static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 4;
    public static final int BILLING_RESPONSE_RESULT_DEVELOPER_ERROR = 5;
    public static final int BILLING_RESPONSE_RESULT_ERROR = 6;
    public static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
    public static final int BILLING_RESPONSE_RESULT_ITEM_NOT_OWNED = 8;
    public static final int IABHELPER_ERROR_BASE = -1000;
    public static final int IABHELPER_REMOTE_EXCEPTION = -1001;
    public static final int IABHELPER_BAD_RESPONSE = -1002;
    public static final int IABHELPER_VERIFICATION_FAILED = -1003;
    public static final int IABHELPER_SEND_INTENT_FAILED = -1004;
    public static final int IABHELPER_USER_CANCELLED = -1005;
    public static final int IABHELPER_UNKNOWN_PURCHASE_RESPONSE = -1006;
    public static final int IABHELPER_MISSING_TOKEN = -1007;
    public static final int IABHELPER_UNKNOWN_ERROR = -1008;
    public static final int IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE = -1009;
    public static final int IABHELPER_INVALID_CONSUMPTION = -1010;
    public static final String RESPONSE_CODE = "RESPONSE_CODE";
    public static final String RESPONSE_GET_SKU_DETAILS_LIST = "DETAILS_LIST";
    public static final String RESPONSE_BUY_INTENT = "BUY_INTENT";
    public static final String RESPONSE_INAPP_PURCHASE_DATA = "INAPP_PURCHASE_DATA";
    public static final String RESPONSE_INAPP_SIGNATURE = "INAPP_DATA_SIGNATURE";
    public static final String RESPONSE_INAPP_ITEM_LIST = "INAPP_PURCHASE_ITEM_LIST";
    public static final String RESPONSE_INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    public static final String RESPONSE_INAPP_SIGNATURE_LIST = "INAPP_DATA_SIGNATURE_LIST";
    public static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";
    public static final String ITEM_TYPE_INAPP = "inapp";
    public static final String ITEM_TYPE_SUBS = "subs";
    public static final String GET_SKU_DETAILS_ITEM_LIST = "ITEM_ID_LIST";
    public static final String GET_SKU_DETAILS_ITEM_TYPE_LIST = "ITEM_TYPE_LIST";
    OnIabPurchaseFinishedListener mPurchaseListener;

    public IabHelper(Context ctx, String base64PublicKey, int Market) {
        mContext = ctx.getApplicationContext();
        mSignatureBase64 = base64PublicKey;
        switch(Market) {
            case 0:
                mServiceName = "ir.cafebazaar.pardakht.InAppBillingService.BIND";
                mPackageName = "com.farsitel.bazaar";
                break;
            case 1:
                mServiceName = "ir.mservices.market.InAppBillingService.BIND";
                mPackageName = "ir.mservices.market";
                break;
            case 2:
                mServiceName = "ir.tgbs.iranapps.billing.InAppBillingService.BIND";
                mPackageName = "ir.tgbs.android.iranapp";
                break;
            case 3:
                mServiceName = "net.jhoobin.jhub.InAppBillingService.BIND";
                mPackageName = "net.jhoobin.jhub";
                break;
            case 4:
                mServiceName = "com.ada.market.service.payment.BIND";
                mPackageName = "com.ada.market";
                break;
            case 5:
                mServiceName = "com.hrm.android.market.billing.InAppBillingService.BIND";
                mPackageName = "com.hrm.android.market";
                break;
            case 6:
                mServiceName = "com.android.vending.billing.InAppBillingService.BIND";
                mPackageName = "com.android.vending";
                break;
            default:
                mServiceName = "ir.cafebazaar.pardakht.InAppBillingService.BIND";
                mPackageName = "com.farsitel.bazaar";
                break;
        }
        logDebug("IAB helper created.");
    }

    public void enableDebugLogging(boolean enable, String tag) {
        checkNotDisposed();
        mDebugLog = enable;
        mDebugTag = tag;
    }

    public void enableDebugLogging(boolean enable) {
        checkNotDisposed();
        mDebugLog = enable;
    }

    public void startSetup(final OnIabSetupFinishedListener listener) {
        checkNotDisposed();
        if (mSetupDone) {
            throw new IllegalStateException("IAB helper is already set up.");
        }
        logDebug("Starting in-app billing setup.");
        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceDisconnected(ComponentName name) {
                logDebug("Billing service disconnected.");
                mService = null;
            }

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                if (mDisposed) return;
                logDebug("Billing service connected.");
                mService = IInAppBillingService.Stub.asInterface(service);
                String packageName = mContext.getPackageName();
                try {
                    logDebug("Checking for in-app billing 3 support.");

                    int response = mService.isBillingSupported(3, packageName, ITEM_TYPE_INAPP);
                    if (response != BILLING_RESPONSE_RESULT_OK) {
                        if (listener != null) {
                            listener.onIabSetupFinished(new IabResult(response,
                                "Error checking for billing v3 support."));
                        }
                        mSubscriptionsSupported = false;
                        return;
                    }
                    logDebug("In-app billing version 3 supported for " + packageName);

                    response = mService.isBillingSupported(3, packageName, ITEM_TYPE_SUBS);
                    if (response == BILLING_RESPONSE_RESULT_OK) {
                        logDebug("Subscriptions AVAILABLE.");
                        mSubscriptionsSupported = true;
                    } else {
                        logDebug("Subscriptions NOT AVAILABLE. Response: " + response);
                    }
                    mSetupDone = true;
                } catch (RemoteException e) {
                    if (listener != null) {
                        listener.onIabSetupFinished(new IabResult(IABHELPER_REMOTE_EXCEPTION,
                            "RemoteException while setting up in-app billing."));
                    }
                    e.printStackTrace();
                    return;
                }
                if (listener != null)
                    listener.onIabSetupFinished(new IabResult(BILLING_RESPONSE_RESULT_OK, "Setup successful."));
            }
        };
        Intent serviceIntent = new Intent(mServiceName);
        serviceIntent.setPackage(mPackageName);
        List<android.content.pm.ResolveInfo> list = mContext.getPackageManager().queryIntentServices(serviceIntent, 0);
        if (list != null && !list.isEmpty()) {
            mContext.bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
        } else if (listener != null) {
            listener.onIabSetupFinished(new IabResult(BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE, "Billing service unavailable on device."));
        }
    }

    public void dispose() {
        logDebug("Disposing.");
        mSetupDone = false;
        if (mServiceConn != null) {
            logDebug("Unbinding from service.");
            if (mContext != null && mService!=null) {
                mContext.unbindService(mServiceConn);
            }
            mDisposed = true;       
            mContext = null;
            mServiceConn = null;
            mService = null;
            mPurchaseListener = null;
        }
    }

    private void checkNotDisposed() {       
        if (mDisposed) throw new IllegalStateException("IabHelper was disposed of, so it cannot be used.");     
    }       
    /** Returns whether subscriptions are supported. */     
    public boolean subscriptionsSupported() {
        checkNotDisposed();     
        return mSubscriptionsSupported;
    }

    public void launchPurchaseFlow(Activity act, String sku, String itemType, int requestCode, OnIabPurchaseFinishedListener listener, String extraData) {
        checkNotDisposed();
        checkSetupDone("launchPurchaseFlow");
        flagStartAsync("launchPurchaseFlow");
        if ((itemType.equals(ITEM_TYPE_SUBS)) && (!mSubscriptionsSupported)) {
            IabResult r = new IabResult(IABHELPER_SUBSCRIPTIONS_NOT_AVAILABLE, "Subscriptions are not available.");
            flagEndAsync();
            if (listener != null) {
                listener.onIabPurchaseFinished(r, null);
            }
            return;
        }
        try {
            logDebug("Constructing buy intent for " + sku + ", item type: " + itemType);
            Bundle buyIntentBundle = mService.getBuyIntent(3, mContext.getPackageName(), sku,
                itemType, extraData);
            int response = getResponseCodeFromBundle(buyIntentBundle);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logError("Unable to buy item, Error response: " + getResponseDesc(response));
                flagEndAsync();
                IabResult result = new IabResult(response, "Unable to buy item");
                if (listener != null) {
                    listener.onIabPurchaseFinished(result, null);
                }
                return;
            }
            PendingIntent pendingIntent = buyIntentBundle.getParcelable(RESPONSE_BUY_INTENT);       
            logDebug("Launching buy intent for " + sku + ". Request code: " + requestCode);
            mRequestCode = requestCode;
            mPurchaseListener = listener;
            mPurchasingItemType = itemType;
            act.startIntentSenderForResult(pendingIntent.getIntentSender(), mRequestCode, new Intent(),
                Integer.valueOf(0).intValue(), Integer.valueOf(0).intValue(), Integer.valueOf(0)
                .intValue());
        } catch (IntentSender.SendIntentException e) {
            logError("SendIntentException while launching purchase flow for sku " + sku);
            e.printStackTrace();
            flagEndAsync();
            IabResult result = new IabResult(-1004, "Failed to send intent.");
            if (listener != null) {
                listener.onIabPurchaseFinished(result, null);
            }
        } catch (RemoteException e) {
            logError("RemoteException while launching purchase flow for sku " + sku);
            e.printStackTrace();
            flagEndAsync();
            IabResult result = new IabResult(IABHELPER_REMOTE_EXCEPTION, "Remote exception while starting purchase flow");
            if (listener != null) {
                listener.onIabPurchaseFinished(result, null);
            }
        }
    }

    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        IabResult result;
        Log.i(mDebugTag, "Arrived: " + requestCode + ", " + mRequestCode);
        if (requestCode != mRequestCode) {
            return false;
        }
        checkNotDisposed();
        checkSetupDone("handleActivityResult");

        flagEndAsync();
        if (data == null) {
            logError("Null data in IAB activity result.");
            result = new IabResult(IABHELPER_BAD_RESPONSE, "Null data in IAB result");
            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }
            return true;
        }
        int responseCode = getResponseCodeFromIntent(data);
        String purchaseData = data.getStringExtra(RESPONSE_INAPP_PURCHASE_DATA);
        String dataSignature = data.getStringExtra(RESPONSE_INAPP_SIGNATURE);
        if ((resultCode == Activity.RESULT_OK) && (responseCode == BILLING_RESPONSE_RESULT_OK)) {
            logDebug("Successful resultcode from purchase activity.");
            logDebug("Purchase data: " + purchaseData);
            logDebug("Data signature: " + dataSignature);
            logDebug("Extras: " + data.getExtras());
            logDebug("Expected item type: " + mPurchasingItemType);
            if ((purchaseData == null) || (dataSignature == null)) {
                logError("BUG: either purchaseData or dataSignature is null.");
                logDebug("Extras: " + data.getExtras().toString());
                result = new IabResult(IABHELPER_UNKNOWN_ERROR, "IAB returned null purchaseData or dataSignature");
                if (mPurchaseListener != null) {
                    mPurchaseListener.onIabPurchaseFinished(result, null);
                }
                return true;
            }
            MdMarketsIAB.Prchase purchase = null;
            try {
                purchase = new MdMarketsIAB.Prchase(mPurchasingItemType, purchaseData, dataSignature);
                String sku = purchase.getProductId();
                if (!Security.verifyPurchase(mSignatureBase64, purchaseData, dataSignature)) {
                    logError("Purchase signature verification FAILED for sku " + sku);
                    result = new IabResult(IABHELPER_VERIFICATION_FAILED, "Signature verification failed for Product ID " +
                        sku);
                    if (mPurchaseListener != null) {
                        mPurchaseListener.onIabPurchaseFinished(result, purchase);
                    }
                    return true;
                }
                logDebug("Purchase signature successfully verified.");
            } catch (JSONException e) {
                logError("Failed to parse purchase data.");
                e.printStackTrace();
                result = new IabResult(IABHELPER_BAD_RESPONSE, "Failed to parse purchase data.");
                if (mPurchaseListener != null) {
                    mPurchaseListener.onIabPurchaseFinished(result, null);
                }
                return true;
            }
            if (mPurchaseListener != null)
                mPurchaseListener.onIabPurchaseFinished(new IabResult(BILLING_RESPONSE_RESULT_OK, "Success"), purchase);
        } else if (resultCode == Activity.RESULT_OK) {
            logDebug("Result code was OK but in-app billing response was not OK: " +
                getResponseDesc(responseCode));
            if (mPurchaseListener != null) {
                result = new IabResult(responseCode, "Problem purchashing item.");
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            logDebug("Purchase canceled - Response: " + getResponseDesc(responseCode));
            result = new IabResult(IABHELPER_USER_CANCELLED, "User canceled.");
            if (mPurchaseListener != null)
                mPurchaseListener.onIabPurchaseFinished(result, null);
        } else {
            logError("Purchase failed. Result code: " + Integer.toString(resultCode) + ". Response: " +
                getResponseDesc(responseCode));
            result = new IabResult(IABHELPER_UNKNOWN_PURCHASE_RESPONSE, "Unknown purchase response.");
            if (mPurchaseListener != null) {
                mPurchaseListener.onIabPurchaseFinished(result, null);
            }
        }
        return true;
    }

    public Inventory queryInventory(boolean querySkuDetails, List <String> moreSkus) throws IabException {
        return queryInventory(querySkuDetails, moreSkus, null);
    }

    public Inventory queryInventory(boolean querySkuDetails, List <String> moreItemSkus, List < String > moreSubsSkus) throws IabException {
        checkNotDisposed();
        checkSetupDone("queryInventory");
        try {
            Inventory inv = new Inventory();
            int r = queryPurchases(inv, ITEM_TYPE_INAPP);
            if (r != BILLING_RESPONSE_RESULT_OK) {
                throw new IabException(r, "Error refreshing inventory (querying owned items).");
            }
            if (querySkuDetails) {
                r = querySkuDetails(ITEM_TYPE_INAPP, inv, moreItemSkus);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new IabException(r, "Error refreshing inventory (querying prices of items).");
                }
            }
            if (mSubscriptionsSupported) {
                r = queryPurchases(inv, ITEM_TYPE_SUBS);
                if (r != BILLING_RESPONSE_RESULT_OK) {
                    throw new IabException(r, "Error refreshing inventory (querying owned subscriptions).");
                }
                if (querySkuDetails) {
                    r = querySkuDetails(ITEM_TYPE_SUBS, inv, moreItemSkus);
                    if (r != BILLING_RESPONSE_RESULT_OK) {
                        throw new IabException(r,
                            "Error refreshing inventory (querying prices of subscriptions).");
                    }
                }
            }
            return inv;
        } catch (RemoteException e) {
            throw new IabException(IABHELPER_REMOTE_EXCEPTION, "Remote exception while refreshing inventory.", e);
        } catch (JSONException e) {
            throw new IabException(IABHELPER_BAD_RESPONSE, "Error parsing JSON response while refreshing inventory.", e);
        }
    }

    public void queryInventoryAsync(final boolean querySkuDetails, final List <String> moreSkus, final QueryInventoryFinishedListener listener) {
        final Handler handler = new Handler();
        checkNotDisposed();
        checkSetupDone("queryInventory");
        flagStartAsync("refresh inventory");
        new Thread(new Runnable() {
            public void run() {
                IabResult result = new IabResult(BILLING_RESPONSE_RESULT_OK, "Inventory refresh successful.");
                Inventory inv = null;
                try {
                    inv = queryInventory(querySkuDetails, moreSkus);
                } catch (IabException ex) {
                    result = ex.getResult();
                }
                flagEndAsync();

                final IabResult result_f = result;
                final Inventory inv_f = inv;
                if (!mDisposed && listener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            listener.onQueryInventoryFinished(result_f, inv_f);
                        }
                    });
                }
            }
        }).start();
    }

    public void queryInventoryAsync(QueryInventoryFinishedListener listener) {
        queryInventoryAsync(true, null, listener);
    }

    public void queryInventoryAsync(boolean querySkuDetails, QueryInventoryFinishedListener listener) {
        queryInventoryAsync(querySkuDetails, null, listener);
    }

    void consume(MdMarketsIAB.Prchase itemInfo) throws IabException {
        checkSetupDone("consume");
        itemInfo.mItemType.equals(ITEM_TYPE_INAPP);
        try {
            String token = itemInfo.getPurchaseToken();
            String sku = itemInfo.getProductId();
            if ((token == null) || (token.equals(""))) {
                logError("Can't consume " + sku + ". No token.");
                throw new IabException(IABHELPER_MISSING_TOKEN, "PurchaseInfo is missing token for Product ID: " + sku + " " +
                    itemInfo);
            }
            logDebug("Consuming sku: " + sku + ", token: " + token);
            int response = mService.consumePurchase(3, mContext.getPackageName(), token);
            if (response == BILLING_RESPONSE_RESULT_OK) {
                logDebug("Successfully consumed sku: " + sku);
            } else {
                logDebug("Error consuming consuming sku " + sku + ". " + getResponseDesc(response));
                throw new IabException(response, "Error consuming sku " + sku);
            }
        } catch (RemoteException e) {
            throw new IabException(IABHELPER_REMOTE_EXCEPTION, "Remote exception while consuming. PurchaseInfo: " + itemInfo, e);
        }
    }

    public void consumeAsync(MdMarketsIAB.Prchase purchase, OnConsumeFinishedListener listener) {
        checkSetupDone("consume");
        List < MdMarketsIAB.Prchase > purchases = new ArrayList < MdMarketsIAB.Prchase > ();
        purchases.add(purchase);
        consumeAsyncInternal(purchases, listener, null);
    }

    public void consumeAsync(List < MdMarketsIAB.Prchase > purchases, OnConsumeMultiFinishedListener listener) {
        checkSetupDone("consume");
        consumeAsyncInternal(purchases, null, listener);
    }

    public static String getResponseDesc(int code) {
        String[] iab_msgs = "0:OK/1:User Canceled/2:Unknown/3:Billing Unavailable/4:Item unavailable/5:Developer Error/6:Error/7:Item Already Owned/8:Item not owned/9:User not logged in"
            .split("/");
        String[] iabhelper_msgs = "0:OK/-1001:Remote exception during initialization/-1002:Bad response received/-1003:Purchase signature verification failed/-1004:Send intent failed/-1005:User cancelled/-1006:Unknown purchase response/-1007:Missing token/-1008:Unknown error/-1009:Subscriptions not available/-1010:Invalid consumption attempt"
            .split("/");
        if (code <= -1000) {
            int index = -1000 - code;
            if ((index >= 0) && (index < iabhelper_msgs.length)) {
                return iabhelper_msgs[index];
            }
            return String.valueOf(code) + ":Unknown IAB Helper Error";
        }
        if ((code < 0) || (code >= iab_msgs.length)) {
            return String.valueOf(code) + ":Unknown";
        }
        return iab_msgs[code];
    }

    void checkSetupDone(String operation) {
        if (!mSetupDone) {
            logError("Illegal state for operation (" + operation + "): IAB helper is not set up.");
            throw new IllegalStateException("IAB helper is not set up. Can't perform operation: " + operation);
        }
    }

    int getResponseCodeFromBundle(Bundle b) {
        Object o = b.get(RESPONSE_CODE);
        if (o == null) {
            logDebug("Bundle with null response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        }
        if ((o instanceof Integer)) {
            return ((Integer) o).intValue();
        }
        if ((o instanceof Long)) {
            return (int)((Long) o).longValue();
        }
        logError("Unexpected type for bundle response code.");
        logError(o.getClass().getName());
        throw new RuntimeException("Unexpected type for bundle response code: " + o.getClass().getName());
    }

    int getResponseCodeFromIntent(Intent i) {
        Object o = i.getExtras().get(RESPONSE_CODE);
        if (o == null) {
            logError("Intent with no response code, assuming OK (known issue)");
            return BILLING_RESPONSE_RESULT_OK;
        } else if (o instanceof Integer) return ((Integer)o).intValue();
        else if (o instanceof Long) return (int)((Long)o).longValue();
        else {
            logError("Unexpected type for intent response code.");
            logError(o.getClass().getName());
            throw new RuntimeException("Unexpected type for intent response code: " + o.getClass().getName());
        }
    }

    void flagStartAsync(String operation) {
        if (mAsyncInProgress) throw new IllegalStateException("Can't start async operation (" +
            operation + ") because another async operation(" + mAsyncOperation + ") is in progress.");
        mAsyncOperation = operation;
        mAsyncInProgress = true;
        logDebug("Starting async operation: " + operation);
    }

    void flagEndAsync() {
        logDebug("Ending async operation: " + mAsyncOperation);
        mAsyncOperation = "";
        mAsyncInProgress = false;
    }

    int queryPurchases(Inventory inv, String itemType) throws JSONException, RemoteException, IabException {
        logDebug("Querying owned items, item type: " + itemType);
        logDebug("Package name: " + mContext.getPackageName());
        boolean verificationFailed = false;
        String continueToken = null;
        do {
            logDebug("Calling getPurchases with continuation token: " + continueToken);
            Bundle ownedItems = mService.getPurchases(3, mContext.getPackageName(), itemType,
                continueToken);

            int response = getResponseCodeFromBundle(ownedItems);
            logDebug("Owned items response: " + String.valueOf(response));
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logDebug("getPurchases() failed: " + getResponseDesc(response));
                return response;
            }
            if ((!ownedItems.containsKey(RESPONSE_INAPP_ITEM_LIST)) ||
                (!ownedItems.containsKey(RESPONSE_INAPP_PURCHASE_DATA_LIST)) ||
                (!ownedItems.containsKey(RESPONSE_INAPP_SIGNATURE_LIST))) {
                logError("Bundle returned from getPurchases() doesn't contain required fields.");
                return IABHELPER_BAD_RESPONSE;
            }
            ArrayList <String> ownedSkus = ownedItems.getStringArrayList(RESPONSE_INAPP_ITEM_LIST);
            ArrayList <String> purchaseDataList = ownedItems.getStringArrayList(RESPONSE_INAPP_PURCHASE_DATA_LIST);
            ArrayList <String> signatureList = ownedItems.getStringArrayList(RESPONSE_INAPP_SIGNATURE_LIST);
            for (int i = 0; i < purchaseDataList.size(); i++) {
                String purchaseData = (String) purchaseDataList.get(i);
                String signature = (String) signatureList.get(i);
                String sku = (String) ownedSkus.get(i);
                if (Security.verifyPurchase(mSignatureBase64, purchaseData, signature)) {
                    logDebug("Sku is owned: " + sku);
                    MdMarketsIAB.Prchase purchase = new MdMarketsIAB.Prchase(itemType, purchaseData,
                        signature);
                    if (TextUtils.isEmpty(purchase.getPurchaseToken())) {
                        logWarn("BUG: empty/null token!");
                        logDebug("Purchase data: " + purchaseData);
                    }
                    inv.addPurchase(purchase);
                } else {
                    logWarn("Purchase signature verification **FAILED**. Not adding item.");
                    logDebug("   Purchase data: " + purchaseData);
                    logDebug("   Signature: " + signature);
                    if (itemType.equals(ITEM_TYPE_INAPP)) {
                        MdMarketsIAB.Prchase purchase = new MdMarketsIAB.Prchase(itemType,
                            purchaseData, signature);
                        consume(purchase);
                    }
                    verificationFailed = true;
                }
            }
            continueToken = ownedItems.getString(INAPP_CONTINUATION_TOKEN);
            logDebug("Continuation token: " + continueToken);
        }

        while (!TextUtils.isEmpty(continueToken));
        return verificationFailed ? IABHELPER_VERIFICATION_FAILED : BILLING_RESPONSE_RESULT_OK;
    }

    int querySkuDetails(String itemType, Inventory inv, List < String > moreSkus) throws RemoteException, JSONException {
        logDebug("Querying SKU details.");
        ArrayList < String > skuList = new ArrayList < String > ();
        skuList.addAll(inv.getAllOwnedSkus(itemType));
        if (moreSkus != null) {
            skuList.addAll(moreSkus);
        }


        if (skuList.size() == 0) {
            logDebug("queryPrices: nothing to do because there are no SKUs.");
            return BILLING_RESPONSE_RESULT_OK;
        }
        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(GET_SKU_DETAILS_ITEM_LIST, skuList);
        Bundle skuDetails = mService.getSkuDetails(3, mContext.getPackageName(), itemType,
            querySkus);
        if (!skuDetails.containsKey(RESPONSE_GET_SKU_DETAILS_LIST)) {
            int response = getResponseCodeFromBundle(skuDetails);
            if (response != BILLING_RESPONSE_RESULT_OK) {
                logDebug("getSkuDetails() failed: " + getResponseDesc(response));
                return response;
            }
            logError("getSkuDetails() returned a bundle with neither an error nor a detail list.");
            return IABHELPER_BAD_RESPONSE;
        }
        ArrayList <String> responseList = skuDetails.getStringArrayList(
            RESPONSE_GET_SKU_DETAILS_LIST);

        for (String thisResponse: responseList) {
            SkDetails d = new SkDetails(itemType, thisResponse);
            logDebug("Got sku details: " + d);
            inv.addSkuDetails(d);
        }
        return BILLING_RESPONSE_RESULT_OK;
    }

    void consumeAsyncInternal(final List < MdMarketsIAB.Prchase > purchases, final OnConsumeFinishedListener singleListener, final OnConsumeMultiFinishedListener multiListener) {
        final Handler handler = new Handler();
        flagStartAsync("consume");
        new Thread(new Runnable() {
            public void run() {
                final List < IabResult > results = new ArrayList < IabResult > ();
                for (MdMarketsIAB.Prchase purchase: purchases) {
                    try {
                        consume(purchase);
                        results.add(new IabResult(BILLING_RESPONSE_RESULT_OK, "Successful consume of Product Id " +
                            purchase.getProductId()));
                    } catch (IabException ex) {
                        results.add(ex.getResult());
                    }
                }
                flagEndAsync();
                if (singleListener != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            singleListener.onConsumeFinished((MdMarketsIAB.Prchase) purchases.get(0),
                                (IabResult) results.get(0));
                        }
                    });
                }
                if (multiListener != null)
                    handler.post(new Runnable() {
                        public void run() {
                            multiListener.onConsumeMultiFinished(purchases, results);
                        }
                    });
            }
        }).start();
    }

    void logDebug(String msg) {
        if (mDebugLog)
            LOG.i(mDebugTag, msg);
    }

    void logError(String msg) {
        if (mDebugLog)
            Log.e(mDebugTag, msg);
    }

    void logWarn(String msg) {
        if (mDebugLog)
            Log.i(mDebugTag, msg);
    }

    public static abstract interface OnConsumeFinishedListener {
        public abstract void onConsumeFinished(MdMarketsIAB.Prchase paramPrchase, IabResult paramIabResult);
    }

    public static abstract interface OnConsumeMultiFinishedListener {
        public abstract void onConsumeMultiFinished(List < MdMarketsIAB.Prchase > paramList, List < IabResult > paramList1);
    }

    public static abstract interface OnIabPurchaseFinishedListener {
        public abstract void onIabPurchaseFinished(IabResult paramIabResult, MdMarketsIAB.Prchase paramPrchase);
    }

    public static abstract interface OnIabSetupFinishedListener {
        public abstract void onIabSetupFinished(IabResult paramIabResult);
    }

    public static abstract interface QueryInventoryFinishedListener {
        public abstract void onQueryInventoryFinished(IabResult paramIabResult, Inventory paramInventory);
    }
}