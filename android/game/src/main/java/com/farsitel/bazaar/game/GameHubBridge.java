package com.farsitel.bazaar.game;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;

import com.farsitel.bazaar.game.callbacks.IConnectionCallback;
import com.farsitel.bazaar.game.callbacks.ITournamentMatchCallback;
import com.farsitel.bazaar.game.utils.GHLogger;
import com.farsitel.bazaar.game.utils.GHResult;
import com.farsitel.bazaar.game.utils.GHStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class GameHubBridge extends AbstractGameHub {
    private static GameHubBridge instance;

    private ServiceConnection gameHubConnection;
    private IGameHub gameHubService;

    public GameHubBridge() {
        super(new GHLogger());
    }

    public static GameHubBridge getInstance() {
        if (instance == null) {
            instance = new GameHubBridge();
        }
        return instance;
    }

    @Override
    public Result isLogin(Context context, boolean showPrompts) {
        try {
            if (gameHubService.isLogin()) {
                return result;
            }
            result.message = "Login to Cafebazaar before!";
        } catch (Exception e) {
            e.printStackTrace();
            result.message = e.getMessage();
            result.stackTrace = Arrays.toString(e.getStackTrace());
        }
        result.status = Status.LOGIN_CAFEBAZAAR;
        if (showPrompts) {
        startActionViewIntent(context, "bazaar://login", "com.farsitel.bazaar");
        }
        return result;
    }

    @Override
    public void connect(Context context, boolean showPrompts, IConnectionCallback callback) {
        connectionState = isCafebazaarInstalled(context, showPrompts);
                connectionState.call(callback);
                return;
            }
            logger.logDebug("GameHub service started.");
            gameHubConnection = new ServiceConnection() {
                @Override
                public void onServiceDisconnected(ComponentName name) {
                    logger.logDebug("GameHub service disconnected.");
                    gameHubService = null;
                    connectionState = new GHResult(GHStatus.DISCONNECTED, "GameHub service disconnected.", "");
                    connectionState.call(callback);
                }

                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    if (disposed()) return;
                    logger.logDebug("GameHub service connected.");
                    gameHubService = IGameHub.Stub.asInterface(service);
                    connectionState.status = GHStatus.SUCCESS;
                    connectionState.message = "GameHub service connected.";
                    connectionState.call(callback);
                }
            };

            // Bind to bazaar game hub
            Intent serviceIntent = new Intent("com.farsitel.bazaar.Game.BIND");
            serviceIntent.setPackage("com.farsitel.bazaar");

            PackageManager pm = context.getPackageManager();
            List<ResolveInfo> intentServices = pm.queryIntentServices(serviceIntent, 0);
            if (!intentServices.isEmpty()) {
                // service available to handle that Intent
                context.bindService(serviceIntent, gameHubConnection, Context.BIND_AUTO_CREATE);
            }
        });

    }

    public void startTournamentMatch(Activity activity, ITournamentMatchCallback callback, String matchId, String metaData) {
        Bundle resultBundle = null;
        logger.logDebug("startTournamentMatch");
        try {
            resultBundle = gameHubService.startTournamentMatch(activity.getPackageName(), matchId, metaData);
        } catch (RemoteException e) {
            callback.onFinish(GHStatus.FAILURE.getLevelCode(), e.getMessage(), Arrays.toString(e.getStackTrace()), "");
            e.printStackTrace();
        }
//        for (String key : Objects.requireNonNull(resultBundle).keySet()) {
//            logger.logInfo("start  " + key + " : " + (resultBundle.get(key) != null ? resultBundle.get(key) : "NULL"));
//        }

        int statusCode = Objects.requireNonNull(resultBundle).getInt("statusCode");
        if (statusCode != GHStatus.SUCCESS.getLevelCode()) {
            callback.onFinish(statusCode, "Error on startTournamentMatch", "", "");
            return;
        }
        String sessionId = resultBundle.containsKey("sessionId") ? resultBundle.getString("sessionId") : "sessionId";
        callback.onFinish(statusCode, sessionId, matchId, metaData);
    }

    public void endTournamentMatch(ITournamentMatchCallback callback, String sessionId, float coefficient) {
        logger.logDebug("endTournamentMatch");
        Bundle resultBundle = null;
        try {
            resultBundle = gameHubService.endTournamentMatch(sessionId, coefficient);
        } catch (RemoteException e) {
            callback.onFinish(GHStatus.FAILURE.getLevelCode(), e.getMessage(), Arrays.toString(e.getStackTrace()), "");
            e.printStackTrace();
        }
//        for (String key : Objects.requireNonNull(resultBundle).keySet()) {
//            logger.logInfo("end  " + key + " : " + (resultBundle.get(key) != null ? resultBundle.get(key) : "NULL"));
//        }

        int statusCode = Objects.requireNonNull(resultBundle).getInt("statusCode");
        if (statusCode != GHStatus.SUCCESS.getLevelCode()) {
            callback.onFinish(statusCode, "Error on endTournamentMatch", "", "");
            return;
        }
        String matchId = resultBundle.containsKey("matchId") ? resultBundle.getString("matchId") : "matchId";
        String metaData = resultBundle.containsKey("metadata") ? resultBundle.getString("metadata") : "metadata";
        callback.onFinish(statusCode, sessionId, matchId, metaData);
    }

    public void showLastTournamentLeaderboard(Context context, IConnectionCallback callback) {
        logger.logDebug("showLastTournamentLeaderboard");
        new Handler(Looper.getMainLooper()).post(() -> {

            connectionState = isAvailable(context);
            if (connectionState.status != GHStatus.SUCCESS) {
                connectionState.call(callback);
                return;
            }

            String data = "bazaar://tournament_leaderboard?id=-1";
            startActionViewIntent(context, data, "com.farsitel.bazaar");
            connectionState.call(callback);
        });
    }
}