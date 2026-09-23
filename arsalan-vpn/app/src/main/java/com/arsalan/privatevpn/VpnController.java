package com.arsalan.privatevpn;

import android.content.Context;

import com.wireguard.android.backend.GoBackend;
import com.wireguard.android.backend.Tunnel;
import com.wireguard.config.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class VpnController {
    interface Callback {
        void onResult(Tunnel.State state, Exception error);
    }

    private static volatile VpnController instance;

    private final GoBackend backend;
    private final AppTunnel tunnel = new AppTunnel();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private VpnController(Context context) {
        backend = new GoBackend(context.getApplicationContext());
    }

    static VpnController get(Context context) {
        if (instance == null) {
            synchronized (VpnController.class) {
                if (instance == null) instance = new VpnController(context);
            }
        }
        return instance;
    }

    void connect(Config config, Callback callback) {
        executor.execute(() -> {
            try {
                Tunnel.State state = backend.setState(tunnel, Tunnel.State.UP, config);
                callback.onResult(state, null);
            } catch (Exception e) {
                callback.onResult(Tunnel.State.DOWN, e);
            }
        });
    }

    void disconnect(Callback callback) {
        executor.execute(() -> {
            try {
                Tunnel.State state = backend.setState(tunnel, Tunnel.State.DOWN, null);
                callback.onResult(state, null);
            } catch (Exception e) {
                callback.onResult(Tunnel.State.DOWN, e);
            }
        });
    }

    void state(Callback callback) {
        executor.execute(() -> {
            try {
                callback.onResult(backend.getState(tunnel), null);
            } catch (Exception e) {
                callback.onResult(Tunnel.State.DOWN, e);
            }
        });
    }

    private static final class AppTunnel implements Tunnel {
        private volatile State state = State.DOWN;

        @Override
        public String getName() {
            return "arsalanvpn";
        }

        @Override
        public void onStateChange(State newState) {
            state = newState;
        }

        State getCachedState() {
            return state;
        }
    }
}
