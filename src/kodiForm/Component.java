package kodiForm;
//http://stackoverflow.com/questions/13362636/a-generic-observer-pattern-in-java

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by mike on 18.11.15.
 */
class Component {
    private static final Logger LOG = LoggerFactory.getLogger(new Throwable().getStackTrace()[0].getClassName());

    @Subscription
    public void onString(String s) {
        LOG.info("String: " + s);
        //System.out.println("String - " + s);
    }

/*
    @Subscription
    public void onDate(Date d) {
        System.out.println("Date - " + d);
    }

    @Subscription
    public void onDouble(Double d) {
        System.out.println("Double - " + d);
    }
*/

}