package com.viaden.sdk;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSystemClock;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 23, shadows = {ShadowSystemClock.class})
public class InstanceDataTest {
    private static final String PREF_PREFIX = "qwerty";

    private InstanceData subject;

    @Before
    public void setUp() throws Exception {
        final List<InstanceData.Token> tokens = Arrays.asList(
                new InstanceData.Token("token_a", "authorized_entity_a", "scope_a"),
                new InstanceData.Token("token_b", "authorized_entity_b", "scope_b"),
                new InstanceData.Token("token_c", "authorized_entity_c", "scope_c")
        );
        subject = new InstanceData("id_a", "application_a", tokens, 7L, 5L);
    }

    @Test
    public void prefs() throws Exception {
        final SharedPreferences prefs = RuntimeEnvironment.application.getSharedPreferences("qwerty", Context.MODE_PRIVATE);
        final SharedPreferences.Editor edit = prefs.edit();
        subject.toPrefs(edit, PREF_PREFIX);
        edit.apply();

        final InstanceData instanceData = new InstanceData.Builder(prefs, PREF_PREFIX).build();
        assertThat(subject).isEqualTo(instanceData);
    }
}
