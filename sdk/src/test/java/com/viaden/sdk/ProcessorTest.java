package com.viaden.sdk;

import com.viaden.sdk.http.HttpClient;
import com.viaden.sdk.http.HttpRequest;
import com.viaden.sdk.http.HttpResponse;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 23, shadows = {
        ShadowDeviceIdTypeRetriever.class,
        ShadowDeviceIdValueRetriever.class
})
public class ProcessorTest {
    @Captor
    private ArgumentCaptor<HttpRequest> httpRequestCaptor;
    @Captor
    private ArgumentCaptor<Map<String, String>> mapCaptor;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private Processor.Dispatcher dispatcher;

    private Processor subject;
    private Placeholder placeholder;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        placeholder = spy(new Placeholder(RuntimeEnvironment.application));
        subject = new Processor(httpClient, dispatcher, placeholder);
        when(httpClient.execute(any(HttpRequest.class))).thenReturn(httpResponse);
    }

    @Test
    public void process() throws Exception {
        ShadowDeviceIdTypeRetriever.value = "ADVERTISING_ID";
        when(httpResponse.getStatusCode()).thenReturn(200);
        when(httpResponse.getContent()).thenReturn(Resources.asString("response.json"));
        final Command command = new Command.Builder(new JSONObject(Resources.asString("command.json"))).build();

        assertThat(command).isNotNull();
        subject.process(command);
        verify(httpClient).execute(httpRequestCaptor.capture());
        verify(placeholder, times(3)).setPlaceholders(mapCaptor.capture());

        final HttpRequest httpRequest = httpRequestCaptor.getValue();
        assertThat(httpRequest).isNotNull();
        assertThat(httpRequest.getUrl()).isEqualTo("https://api.ampiri.com/v4/handshake?deviceId=fake_device_id_value");
        assertThat(httpRequest.getBody()).isNotNull();
        assertThat(new JSONObject(Resources.toString(httpRequest.getBody().getContent())).toString()).isEqualTo(new JSONObject(Resources.asString("request.json")).toString());

        assertThat(mapCaptor.getValue()).isNotNull();
        assertThat(mapCaptor.getValue().get("handshake.key")).isEqualTo("6Y1z7Be5gxL3anMwqec9AEcobLGh7L");

    }
}
