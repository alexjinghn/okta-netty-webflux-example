package com.test.netty.common;

import com.test.netty.client.Client;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FutureListener implements GenericFutureListener {
    private static Logger logger = LoggerFactory.getLogger(Client.class);

    @Override
    public void operationComplete(Future future) throws Exception {
        logger.info("future operation complete " + future);
    }
}
