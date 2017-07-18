package ir.miladesign;

import ir.miladesign.utils.Inventory;
import ir.miladesign.utils.IabHelper;
import ir.miladesign.utils.IabResult;

import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MdMarketsIAB extends CordovaPlugin {
    private IabHelper helper;
    private static final String TAG = "MilaDesign";
    Inventory mInventory;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.e(TAG, "initialize");
        super.initialize(cordova, webView);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.e(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
        if (!helper.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        } else {
            Log.e(TAG, "onActivityResult handled by IABUtil.");
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();

        // Very important:
        if (helper != null) {
            Log.e(TAG, "Destroying helper.");
            helper.dispose();
            helper = null;
        }
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equalsIgnoreCase("Initialize")) {
            Initialize(args.getString(0), args.getInt(1), callbackContext);
            return true;
        }
        if (action.equalsIgnoreCase("GetOwnedProducts")) {
            GetOwnedProducts(callbackContext);
            return true;
        }
        if (action.equalsIgnoreCase("RequestPayment")) {
            RequestPayment(args.getString(0), args.getString(1), args.getString(2), callbackContext);
            return true;
        }
        if (action.equalsIgnoreCase("ConsumeProduct")) {
            ConsumeProduct(args.getString(0), callbackContext);
            return true;
        }
        return false;
    }
    
    public void Initialize(String PublicKey, int Market, final CallbackContext callbackContext) {
        helper = new IabHelper(cordova.getActivity(), PublicKey, Market);
        mInventory = new Inventory();
        helper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            public void onIabSetupFinished(IabResult result) {
                JSONArray jo = new JSONArray();
                jo.put(result.isSuccess());
                jo.put(result.getMessage() == null ? "" : result.getMessage());
                callbackContext.success(jo);
            }
        });
    }

    public void setDebugLogging(boolean debug) {
        helper.enableDebugLogging(debug);
    }

    public boolean getSubscriptionsSupported() {
        return helper.subscriptionsSupported();
    }

    public void GetOwnedProducts(final CallbackContext callbackContext) {
        IabHelper.QueryInventoryFinishedListener lis = new IabHelper.QueryInventoryFinishedListener() {
            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                Map<String, MdMarketsIAB.Prchase> map = new HashMap<String, MdMarketsIAB.Prchase>();
                if ((inv != null) && (inv.mPurchaseMap != null)) {
                    mInventory = inv;
                    map.putAll(inv.mPurchaseMap);
                }
                for(Map.Entry<String, MdMarketsIAB.Prchase> entry : map.entrySet()) {
                    Prchase Product = entry.getValue();
                    if (Product.getItemType().equals("inapp")) {
                        ConsumeProduct(Product.getProductId(), callbackContext);
                    }
                }
                JSONObject purchaseResult = new JSONObject();
                try {
                    purchaseResult.put("data", map);
                    purchaseResult.put("result", result.isSuccess());
                    callbackContext.success(purchaseResult);
                } catch (JSONException e) {Log.e(TAG, "Purchase.");}
            }
        };
        helper.queryInventoryAsync(lis);
    }

    public void RequestPayment(final String ProductId, final String ProductType, final String DeveloperPayload, final CallbackContext callbackContext) {
        cordova.setActivityResultCallback(this);
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                helper.launchPurchaseFlow(cordova.getActivity(), ProductId,
                        ProductType, 1, new IabHelper.OnIabPurchaseFinishedListener() {
                            public void onIabPurchaseFinished(IabResult result, MdMarketsIAB.Prchase purchase) {
                                Log.e(TAG, "Purchase finished: " + result + ", purchase: " + purchase);
                                if (callbackContext!=null) {
                                    if (result.isFailure()) {
                                        callbackContext.error(result.getMessage());
                                        return;
                                    }
                                    Log.e(TAG, "Purchase successful.");
                                    mInventory.addPurchase(purchase);
                                    Log.e(TAG, purchase.getItemType());
                                    if (purchase.getItemType().equals("inapp")) {
                                        Log.e(TAG, "**** Consume after pay");
                                        ConsumeProduct(ProductId, callbackContext);
                                    } else {
                                        JSONObject purchaseResult = new JSONObject();
                                        try {
                                            purchaseResult.put("orderId", purchase.getOrderId());
                                            purchaseResult.put("developerPayload", purchase.getDeveloperPayload());
                                            purchaseResult.put("token", purchase.getPurchaseToken());
                                            purchaseResult.put("productId", purchase.getProductId());
                                            purchaseResult.put("type", purchase.getItemType());
                                            callbackContext.success(purchaseResult);
                                        } catch (JSONException e) {Log.e(TAG, "Purchase.");}
                                    }
                                } else {
                                    Log.e(TAG, "Callback Null");
                                }
                            }
                        }, DeveloperPayload);
            }
        });
    }

    public void ConsumeProduct(final String ProductId, final CallbackContext callbackContext) {
        Prchase Product = mInventory.getPurchase(ProductId);
        cordova.setActivityResultCallback(this);
        Log.e(TAG, "**** Consume function");
        helper.consumeAsync(Product, new IabHelper.OnConsumeFinishedListener() {
            public void onConsumeFinished(MdMarketsIAB.Prchase purchase, IabResult result) {
                if (result.isFailure()) {
                    callbackContext.error(result.getMessage());
                    Log.e(TAG, result.getMessage());
                }
                mInventory.erasePurchase(ProductId);
                JSONObject purchaseResult = new JSONObject();
                try {
                    purchaseResult.put("orderId", purchase.getOrderId());
                    purchaseResult.put("type", purchase.getItemType());
                    purchaseResult.put("developerPayload", purchase.getDeveloperPayload());
                    purchaseResult.put("token", purchase.getPurchaseToken());
                    purchaseResult.put("productId", purchase.getProductId());
                    callbackContext.success(purchaseResult);
                } catch (JSONException e) {Log.e(TAG, "Purchase.");}
            }
        });
    }

    public static class Prchase {
        public static final int STATE_PURCHASED = 0;
        public static final int STATE_CANCELED = 1;
        public static final int STATE_REFUNDED = 2;

        public String mItemType;
        String mOrderId;
        String mPackageName;
        String mSku;
        long mPurchaseTime;
        int mPurchaseState;
        String mDeveloperPayload;
        String mToken;
        String mOriginalJson;
        String mSignature;

        public Prchase() {}
        public Prchase(String itemType, String jsonPurchaseInfo, String signature) throws JSONException {
            mItemType = itemType;
            mOriginalJson = jsonPurchaseInfo;
            JSONObject o = new JSONObject(mOriginalJson);
            mOrderId = o.optString("orderId");
            mPackageName = o.optString("packageName");
            mSku = o.optString("productId");
            mPurchaseTime = o.optLong("purchaseTime");
            mPurchaseState = o.optInt("purchaseState");
            mDeveloperPayload = o.optString("developerPayload");
            mToken = o.optString("token", o.optString("purchaseToken"));
            mSignature = signature;
        }

        public String getItemType() {
            return mItemType;
        }

        public String getOrderId() {
            return mOrderId;
        }

        public String getPackageName() {
            return mPackageName;
        }

        public String getProductId() {
            return mSku;
        }

        public long getPurchaseTime() {
            return mPurchaseTime;
        }

        public int getPurchaseState() {
            return mPurchaseState;
        }

        public String getDeveloperPayload() {
            return mDeveloperPayload;
        }

        public String getPurchaseToken() {
            return mToken;
        }

        public String getOriginalJson() {
            return mOriginalJson;
        }

        public String getSignature() {
            return mSignature;
        }

        public String toString() {
            return "Purchase(type:" + mItemType + "): " + mSku + ", state = " + mPurchaseState;
        }
    }
}