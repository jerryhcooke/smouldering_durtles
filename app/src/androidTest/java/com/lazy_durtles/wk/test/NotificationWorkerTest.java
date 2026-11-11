package com.lazy_durtles.wk.test;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.lazy_durtles.wk.services.NotificationWorker;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class
NotificationWorkerTest {

    @Test
    public void testNotification() {
        NotificationWorker.triggerTestNotification();
    }
}