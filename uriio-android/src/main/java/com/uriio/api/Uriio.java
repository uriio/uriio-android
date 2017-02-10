package com.uriio.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import com.uriio.api.beacons.TransientBeaconAdapter;
import com.uriio.api.model.AccessTokenModel;
import com.uriio.api.model.BeaconModel;
import com.uriio.api.model.ClockModel;
import com.uriio.api.model.NodeModel;
import com.uriio.api.model.PageModel;
import com.uriio.api.model.RegBeaconModel;
import com.uriio.api.model.RegParamsModel;
import com.uriio.api.model.TokenInfoModel;
import com.uriio.beacons.Callback;
import com.uriio.beacons.Util;
import com.uriio.beacons.model.TransientBeacon;

import org.whispersystems.curve25519.Curve25519KeyPair;

import java.lang.ref.WeakReference;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class Uriio {
    private static final String PREFS_FILENAME = "uriio_v2";

    private WeakReference<Context> mAppContext = null;
    private AccessTokenModel mAccessToken = null;

    public Uriio(Context context) {
        mAppContext = new WeakReference<>(context.getApplicationContext());
    }

    private boolean checkAccessToken() {
        if (null == mAccessToken) {
            mAccessToken = new AccessTokenModel();
            Context context = mAppContext.get();
            SharedPreferences preferences = context.getSharedPreferences(PREFS_FILENAME, 0);
            mAccessToken.token = preferences.getString("t", null);
            mAccessToken.expires = preferences.getLong("e", 0);
        }

        // if token expires in less than a few seconds don't use it. Since the client clock can be
        // a few minutes off, the token might still be expired anyway.
        if (null == mAccessToken.token) {
            return false;
        }

        ApiClient.setAccessToken(mAccessToken.token);

        return mAccessToken.expires > System.currentTimeMillis() / 1000 + 5;
    }

    public void clearAccessToken() {
        Context context = mAppContext.get();
        if (null != context) {
            context.getSharedPreferences(PREFS_FILENAME, 0).edit().clear().apply();
        }
        mAccessToken = null;
        ApiClient.setAccessToken(null);
    }

    public void authenticate(String firebaseIdToken, final Callback<AccessTokenModel> callback) {
        ApiClient.authenticate(firebaseIdToken, new Callback<AccessTokenModel>() {
            @Override
            public void onResult(AccessTokenModel result, Throwable error) {
                if (null != result) {
                    mAccessToken = result;
                    mAccessToken.expires = System.currentTimeMillis() + 1000 * mAccessToken.expires;
                    ApiClient.setAccessToken(mAccessToken.token);

                    Context context = mAppContext.get();
                    if (null != context) {
                        SharedPreferences preferences = context.getSharedPreferences(PREFS_FILENAME, 0);
                        preferences.edit()
                                .putString("t", result.token)
                                .putLong("e", result.expires)
                                .apply();
                    }
                }
                callback.onResult(result, error);
            }
        });
    }

    public void registerBeacon(final int rotationExponent, final Callback<TransientBeacon> callback) {
        if (!checkAccessToken()) {
            callback.onResult(null, new ApiException(401, "Access token expired"));
            return;
        }

        ApiClient.getRegistrationParams(new Callback<RegParamsModel>() {
            @Override
            public void onResult(RegParamsModel result, Throwable error) {
                if (null != error) {
                    callback.onResult(null, error);
                    return;
                }

                handleRegistrationParamsResult((byte) rotationExponent, result, callback);
            }
        });
    }

    private void handleRegistrationParamsResult(byte rotationExponent, RegParamsModel result, final Callback<TransientBeacon> callback) {
        byte[] servicePublicKey = Base64.decode(result.serviceEcdhPublicKey, Base64.DEFAULT);

        byte[] sharedSecret;
        Curve25519KeyPair beaconKeyPair;
        for(;;) {
            beaconKeyPair = Crypto.generateKeyPair();
            sharedSecret = Crypto.computeSharedSecret(servicePublicKey, beaconKeyPair.getPrivateKey());
            if (!Util.isZeroBuffer(sharedSecret)) {
                break;
            }
        }

        byte[] identityKey;
        try {
            identityKey = Crypto.computeIdentityKey(sharedSecret, servicePublicKey, beaconKeyPair.getPublicKey());
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            callback.onResult(null, e);
            return;
        }

        final TransientBeacon beacon = TransientBeaconAdapter.createBeacon(identityKey, rotationExponent, 0, null);
        beacon.save(false);

        int initialClockValue = 0;
        RegBeaconModel regBeaconModel = new RegBeaconModel();
        regBeaconModel.beaconEcdhPublicKey = Base64.encodeToString(beaconKeyPair.getPublicKey(), Base64.NO_PADDING | Base64.NO_WRAP);
        regBeaconModel.serviceEcdhPublicKey = result.serviceEcdhPublicKey;
        regBeaconModel.initialClock = initialClockValue;
        regBeaconModel.rotationPeriodExponent = rotationExponent;
        regBeaconModel.initialEid = beacon.computeTransientToken(initialClockValue);

        ApiClient.registerBeacon(regBeaconModel, new Callback<BeaconModel>() {
            @Override
            public void onResult(BeaconModel result, Throwable error) {
                if (null != result) {
                    beacon.edit().setEpochAndId(result.epoch / 1000, result.id).apply();
                    beacon.start();
                    callback.onResult(beacon, null);
                }
                else {
                    beacon.delete();
                    callback.onResult(null, error);
                }
            }
        });
    }

    public void getServerTime(final Callback<ClockModel> callback) {
        ApiClient.getServerTime(new Callback<ClockModel>() {
            @Override
            public void onResult(ClockModel result, Throwable error) {
                if (null != result) {
                    TransientBeacon.applyServerTime(result.currentTimeMs);
                }

                if (null != callback) {
                    callback.onResult(result, error);
                }
            }
        });
    }

    public void checkBeaconToken(String token, Callback<TokenInfoModel> callback) {
        if (!checkAccessToken()) {
            callback.onResult(null, new ApiException(401, "Access token expired"));
            return;
        }

        ApiClient.checkToken(token, callback);
    }

    public void getBeacon(String id, Callback<BeaconModel> callback) {
        if (!checkAccessToken()) {
            callback.onResult(null, new ApiException(401, "Access token expired"));
            return;
        }

        ApiClient.getBeacon(id, callback);
    }

    public void getNode(String id, Callback<NodeModel> callback) {
        if (!checkAccessToken()) {
            callback.onResult(null, new ApiException(401, "Access token expired"));
            return;
        }

        ApiClient.getNode(id, callback);
    }

    public void listNodes(int limit, String pageToken, Callback<PageModel<NodeModel>> callback) {
        if (!checkAccessToken()) {
            callback.onResult(null, new ApiException(401, "Access token expired"));
            return;
        }

        ApiClient.listNodes(limit, pageToken, callback);
    }

    public void insertNode(NodeModel node, Callback<NodeModel> callback) {
        if (!checkAccessToken()) {
            callback.onResult(null, new ApiException(401, "Access token expired"));
            return;
        }

        ApiClient.insertNode(node, callback);
    }

    public void updateBeacon(String id, Map<String, Object> params, Callback<BeaconModel> callback) {
        if (!checkAccessToken()) {
            callback.onResult(null, new ApiException(401, "Access token expired"));
            return;
        }

        ApiClient.updateBeacon(id, params, callback);
    }
}
