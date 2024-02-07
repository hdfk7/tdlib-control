package com.fansmore.api.execute;

import com.fansmore.api.common.Constant;
import com.fansmore.api.common.exception.RepeatPhoneException;

import java.util.concurrent.ConcurrentHashMap;

public class ActuatorManager {

    private final ConcurrentHashMap<String, Actuator> actuatorMap = new ConcurrentHashMap<>();

    private ActuatorManager() {
    }

    private static final class ActuatorManagerHolder {
        static final ActuatorManager mInstance = new ActuatorManager();
    }

    public static ActuatorManager getInstance() {
        return ActuatorManagerHolder.mInstance;
    }

    public synchronized void pushPhone(String phone) {
        if (actuatorMap.containsKey(phone)) {
            throw new RepeatPhoneException();
        }
        Constant.CACHED_THREAD_POOL.execute(() -> {
            try {
                final Actuator actuator = new Actuator(phone);
                actuatorMap.put(phone, actuator);
                actuator.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void pushCode(String phone, String code) {
        final Actuator actuator = actuatorMap.get(phone);
        if (actuator != null) {
            actuator.code(code);
        }
    }

    public void close(String phone) {
        final Actuator actuator = actuatorMap.remove(phone);
        if (actuator != null) {
            System.out.println("关闭客户端 " + phone);
            actuator.close();
        }
    }

    public void closeAll() {
        final ConcurrentHashMap.KeySetView<String, Actuator> keySet = actuatorMap.keySet();
        for (String key : keySet) {
            close(key);
        }
    }

    public void pushCommand(String phone, String command) {
        final Actuator actuator = actuatorMap.get(phone);
        if (actuator != null) {
            actuator.command(command);
        }
    }
}
