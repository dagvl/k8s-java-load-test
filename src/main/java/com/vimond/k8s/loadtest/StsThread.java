package com.vimond.k8s.loadtest;

import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;
import com.vimond.k8s.loadtest.Main;
import io.prometheus.client.Summary;

public class StsThread extends Thread {
    private final int sleep;

    public StsThread(int sleep) {
        this.sleep = sleep;
    }
    @SuppressWarnings("Duplicates")
    @Override
    public void run() {
        super.run();
        System.out.println("Running sts:Assume role every " + sleep + "ms.");
        try {
            while (true) {
                doThings();
                System.out.println("sts:AssumeRole: Sleeping " + this.sleep + " ms");
                Thread.sleep(this.sleep);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

    }

    private void doThings() {

        Summary.Timer timer = Main.rpcDurationSeconds.labels("get-caller-identity").startTimer();
        try {
            System.out.println("sts:AssumeRole: starting call...");

            AWSSecurityTokenService client = AWSSecurityTokenServiceClientBuilder.standard().build();
            GetCallerIdentityRequest request = new GetCallerIdentityRequest();
            GetCallerIdentityResult response = client.getCallerIdentity(request);
            timer.observeDuration();
            System.out.println("sts:AssumeRole: success");

        } catch (Exception e) {
            timer.observeDuration();
            Main.errorCount.labels("get-caller-identity", e.getClass().getName()).inc();
            System.out.println("sts:AssumeRole: error: " + e.getMessage());
        }
    }
}
