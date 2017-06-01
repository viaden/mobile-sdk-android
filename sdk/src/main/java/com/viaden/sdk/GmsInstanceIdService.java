package com.viaden.sdk;

import com.google.android.gms.iid.InstanceIDListenerService;

public class GmsInstanceIdService extends InstanceIDListenerService {

    @Override
    public void onTokenRefresh() {
        startService(RegistrationService.buildIntent(this));
    }
}
