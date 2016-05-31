package com.stampery.api;

import android.app.Application;
import android.test.ApplicationTestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class StamperyApplicationTest extends ApplicationTestCase<Application> {

    public StamperyApplicationTest() {
        super(Application.class);

        Stampery stampery = new Stampery();
        stampery.authenticate("foo@bar.com","password");
        stampery.stamp("test data","some test data");


    }

}