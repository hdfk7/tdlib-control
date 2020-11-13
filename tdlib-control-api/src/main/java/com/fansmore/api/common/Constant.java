package com.fansmore.api.common;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Constant {
    public static final ExecutorService CACHED_THREAD_POOL = Executors.newCachedThreadPool();
}
