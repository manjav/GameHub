package com.farsitel.bazaar.game.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.farsitel.bazaar.game.GameHubBridge;
import com.farsitel.bazaar.game.callbacks.IConnectionCallback;
import com.farsitel.bazaar.game.callbacks.ITournamentMatchCallback;
import com.farsitel.bazaar.game.utils.GHStatus;

public class MainActivity extends Activity {

    private GameHubBridge gameHubBridge;
    private String reservedSessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gameHubBridge = new GameHubBridge();
    }

    public void connect(View view) {
        gameHubBridge.connect(this, (status, message, stackTrace) -> {
            Log.i("TAG", String.format("Connect => Status: %d, Message: %s, StackTrace: %s", status, message, stackTrace));
        });
    }

    public void startTournamentMatch(View view) {
        ITournamentMatchCallback callback = (status, sessionId, matchId, metaData) -> {
            Log.i("TAG", String.format("Start => Status: %d, SessionId: %s, MatchId: %s, MetaData: %s", status, sessionId, matchId, metaData));
            if (status == GHStatus.SUCCESS.getLevelCode()) {
                reservedSessionId = sessionId;
            }
        };
        gameHubBridge.startTournamentMatch(this, callback, "-1", "extra");
    }

    public void endTournamentMatch(View view) {
        if (reservedSessionId == null) {
            Log.e("TAG", "Call startTournamentMatch before!");
            return;
        }
        ITournamentMatchCallback callback = (status, sessionId, matchId, metaData) -> {
            Log.i("TAG", String.format("End => Status: %d, SessionId: %s, MatchId: %s, MetaData: %s", status, sessionId, matchId, metaData));
            reservedSessionId = null;
        };
        gameHubBridge.endTournamentMatch(callback, reservedSessionId, 0.5f);
    }

    public void showLastTournamentLeaderboard(View view) {
        gameHubBridge.showLastTournamentLeaderboard(this, (status, message, stackTrace) -> {
            Log.i("TAG", String.format("showLeaderboard => Status: %d, Message: %s, StackTrace: %s", status, message, stackTrace));
        });
    }
}