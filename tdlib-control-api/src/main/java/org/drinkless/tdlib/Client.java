package org.drinkless.tdlib;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class Client {
    private final int nativeClientId;
    private static final ConcurrentHashMap<Integer, ExceptionHandler> defaultExceptionHandlers;
    private static final ConcurrentHashMap<Integer, Handler> updateHandlers;
    private static final ConcurrentHashMap<Long, Handler> handlers;
    private static final AtomicLong currentQueryId;
    private static final AtomicLong clientCount;
    private static final ResponseReceiver responseReceiver;

    public void send(TdApi.Function var1, ResultHandler var2, ExceptionHandler var3) {
        long var4 = currentQueryId.incrementAndGet();
        if (var2 != null) {
            handlers.put(var4, new Handler(var2, var3));
        }

        nativeClientSend(this.nativeClientId, var4, var1);
    }

    public void send(TdApi.Function var1, ResultHandler var2) {
        this.send(var1, var2, (ExceptionHandler) null);
    }

    public static <T extends TdApi.Object> T execute(TdApi.Function<T> var0) throws ExecutionException {
        TdApi.Object var1 = nativeClientExecute(var0);
        if (var1 instanceof TdApi.Error) {
            throw new ExecutionException((TdApi.Error) var1);
        } else {
            return (T) var1;
        }
    }

    public static Client create(ResultHandler var0, ExceptionHandler var1, ExceptionHandler var2, TdApi.AddProxy proxy) {
        Client var3 = new Client(var0, var1, var2);
        if (proxy != null) {
            var3.send(proxy, null);
        }
        synchronized (responseReceiver) {
            if (!responseReceiver.isRun) {
                responseReceiver.isRun = true;
                Thread var5 = new Thread(responseReceiver, "TDLib thread");
                var5.setDaemon(true);
                var5.start();
            }

            return var3;
        }
    }

    public static void setLogMessageHandler(int var0, LogMessageHandler var1) {
        nativeClientSetLogMessageHandler(var0, var1);
    }

    private Client(ResultHandler var1, ExceptionHandler var2, ExceptionHandler var3) {
        clientCount.incrementAndGet();
        this.nativeClientId = createNativeClient();
        if (var1 != null) {
            updateHandlers.put(this.nativeClientId, new Handler(var1, var2));
        }

        if (var3 != null) {
            defaultExceptionHandlers.put(this.nativeClientId, var3);
        }

        this.send(new TdApi.GetOption("version"), (ResultHandler) null, (ExceptionHandler) null);
    }

    private static native int createNativeClient();

    private static native void nativeClientSend(int var0, long var1, TdApi.Function var3);

    private static native int nativeClientReceive(int[] var0, long[] var1, TdApi.Object[] var2, double var3);

    private static native TdApi.Object nativeClientExecute(TdApi.Function var0);

    private static native void nativeClientSetLogMessageHandler(int var0, LogMessageHandler var1);

    static {
        try {
            System.loadLibrary("tdjni");
        } catch (UnsatisfiedLinkError var1) {
            var1.printStackTrace();
        }

        defaultExceptionHandlers = new ConcurrentHashMap();
        updateHandlers = new ConcurrentHashMap();
        handlers = new ConcurrentHashMap();
        currentQueryId = new AtomicLong();
        clientCount = new AtomicLong();
        responseReceiver = new ResponseReceiver();
    }

    private static class Handler {
        final ResultHandler resultHandler;
        final ExceptionHandler exceptionHandler;

        Handler(ResultHandler var1, ExceptionHandler var2) {
            this.resultHandler = var1;
            this.exceptionHandler = var2;
        }
    }

    public interface ResultHandler {
        void onResult(TdApi.Object var1);
    }

    public interface ExceptionHandler {
        void onException(Throwable var1);
    }

    public static class ExecutionException extends Exception {
        public final TdApi.Error error;

        ExecutionException(TdApi.Error var1) {
            super(var1.code + ": " + var1.message);
            this.error = var1;
        }
    }

    private static class ResponseReceiver implements Runnable {
        public boolean isRun = false;
        private static final int MAX_EVENTS = 1000;
        private final int[] clientIds = new int[1000];
        private final long[] eventIds = new long[1000];
        private final TdApi.Object[] events = new TdApi.Object[1000];

        private ResponseReceiver() {
        }

        public void run() {
            while (true) {
                int var1 = Client.nativeClientReceive(this.clientIds, this.eventIds, this.events, 100000.0);

                for (int var2 = 0; var2 < var1; ++var2) {
                    this.processResult(this.clientIds[var2], this.eventIds[var2], this.events[var2]);
                    this.events[var2] = null;
                }
            }
        }

        private void processResult(int var1, long var2, TdApi.Object var4) {
            boolean var5 = false;
            if (var2 == 0L && var4 instanceof TdApi.UpdateAuthorizationState) {
                TdApi.AuthorizationState var6 = ((TdApi.UpdateAuthorizationState) var4).authorizationState;
                if (var6 instanceof TdApi.AuthorizationStateClosed) {
                    var5 = true;
                }
            }

            Handler var12 = var2 == 0L ? (Handler) Client.updateHandlers.get(var1) : (Handler) Client.handlers.remove(var2);
            if (var12 != null) {
                try {
                    var12.resultHandler.onResult(var4);
                } catch (Throwable var11) {
                    Throwable var7 = var11;
                    ExceptionHandler var8 = var12.exceptionHandler;
                    if (var8 == null) {
                        var8 = Client.defaultExceptionHandlers.get(var1);
                    }

                    if (var8 != null) {
                        try {
                            var8.onException(var7);
                        } catch (Throwable var10) {
                        }
                    }
                }
            }

            if (var5) {
                Client.updateHandlers.remove(var1);
                Client.defaultExceptionHandlers.remove(var1);
                Client.clientCount.decrementAndGet();
            }

        }
    }

    public interface LogMessageHandler {
        void onLogMessage(int var1, String var2);
    }
}
