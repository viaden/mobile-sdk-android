package com.viaden.sdk;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 23)
public class InstanceDataStorageTest {

    @Test
    public void load_withEmptySharedPreferences() throws Exception {
        assertThat(InstanceDataStorage.load(RuntimeEnvironment.application)).isNull();
    }

    @Test
    public void store() throws Exception {
        final List<InstanceData.Token> tokens = Arrays.asList(
                new InstanceData.Token("token_a", "authorized_entity_a", "scope_a"),
                new InstanceData.Token("token_b", "authorized_entity_b", "scope_b"),
                new InstanceData.Token("token_c", "authorized_entity_c", "scope_c")
        );
        final InstanceData instanceData = new InstanceData("id_a", "application_a", tokens, 7L, 5L);
        InstanceDataStorage.save(RuntimeEnvironment.application, instanceData);
        assertThat(InstanceDataStorage.load(RuntimeEnvironment.application)).isEqualTo(instanceData);
    }
}
