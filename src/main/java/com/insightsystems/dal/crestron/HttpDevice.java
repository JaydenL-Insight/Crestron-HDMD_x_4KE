package com.insightsystems.dal.crestron;

import com.avispl.symphony.dal.communicator.HttpCommunicator;

public class HttpDevice extends HttpCommunicator {
    @Override
    protected void authenticate() throws Exception {

    }

    public String get(String uri) throws Exception {
        return this.doGet(uri);
    }
}
