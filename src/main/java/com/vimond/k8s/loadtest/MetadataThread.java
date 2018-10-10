package com.vimond.k8s.loadtest;

import com.amazonaws.util.EC2MetadataUtils;
import com.vimond.k8s.loadtest.Main;
import io.prometheus.client.Summary;

import java.util.Map;

public class MetadataThread extends Thread {
    private final int sleep;

    public MetadataThread(int sleep) {
        this.sleep = sleep;
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void run() {
        super.run();
        try {
            while (true) {
                doThings();
                System.out.println("ec2Metadata: Sleeping " + this.sleep + " ms");

                Thread.sleep(this.sleep);

            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void doThings() {
        Map<String, EC2MetadataUtils.IAMSecurityCredential> info = null;

        Summary.Timer timer = Main.rpcDurationSeconds.labels("get-instance-identity-document").startTimer();
        try {
            System.out.println("ec2Metadata: starting call...");
            EC2MetadataUtils.getIAMSecurityCredentials();
            timer.observeDuration();
            System.out.println("ec2Metadata: success");

        } catch (Exception e) {
            timer.observeDuration();
            Main.errorCount.labels("get-instance-identity-document", e.getClass().getName()).inc();
            System.out.println("ec2Metadata: error: " + e.getMessage());
        }
    }
}
