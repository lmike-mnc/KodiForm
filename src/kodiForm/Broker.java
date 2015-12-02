package kodiForm;
//http://stackoverflow.com/questions/13362636/a-generic-observer-pattern-in-java

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class Broker {
    private final Map<Class, List<SubscriberInfo>> map = new LinkedHashMap<Class, List<SubscriberInfo>>();
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());

    public void add(Object o) {
        for (Method method : o.getClass().getMethods()) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (method.getAnnotation(Subscription.class) == null) continue;// || parameterTypes.length != 1) continue;
            Class subscribeTo = parameterTypes[0];
            List<SubscriberInfo> subscriberInfos = map.get(subscribeTo);
            if (subscriberInfos == null)
                map.put(subscribeTo, subscriberInfos = new ArrayList<SubscriberInfo>());
            subscriberInfos.add(new SubscriberInfo(method, o));
        }
    }

    public void remove(Object o) {
        for (List<SubscriberInfo> subscriberInfos : map.values()) {
            for (int i = subscriberInfos.size() - 1; i >= 0; i--)
                if (subscriberInfos.get(i).object == o)
                    subscriberInfos.remove(i);
        }
    }

    public int publish(Object o, boolean avail) {
        List<SubscriberInfo> subscriberInfos = map.get(o.getClass());
        if (subscriberInfos == null) return 0;
        int count = 0;
        for (SubscriberInfo subscriberInfo : subscriberInfos) {
            subscriberInfo.invoke(o, avail);
            count++;
        }
        return count;
    }

    public int publish(Object o) {
        List<SubscriberInfo> subscriberInfos = map.get(o.getClass());
        if (subscriberInfos == null) return 0;
        int count = 0;
        for (SubscriberInfo subscriberInfo : subscriberInfos) {
            subscriberInfo.invoke(o);
            count++;
        }
        return count;
    }

    static class SubscriberInfo {
        final Method method;
        final Object object;

        SubscriberInfo(Method method, Object o) {
            this.method = method;
            this.object = o;
        }

        void invoke(Object o) {
            try {
                method.invoke(object, o);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        void invoke(Object o, boolean avail) {
            try {
                method.invoke(object, o, avail);
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }
    }
}