package com.viaden.sdk;

import com.google.android.gms.iid.InstanceIDListenerService;

public class InstanceIdService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        startService(RegistrationService.buildIntent(this));
    }
}
