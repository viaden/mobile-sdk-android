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
import org.robolectric.annotation.Config;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, manifest = Config.NONE, sdk = 23)
public class ProcessorTest {
    @Captor
    private ArgumentCaptor<HttpRequest> captor;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpResponse httpResponse;
    @Mock
    private Processor.Dispatcher dispatcher;

    private Processor subject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        subject = new Processor(httpClient, dispatcher, placeholder);
        when(httpClient.execute(any(HttpRequest.class))).thenReturn(httpResponse);
    }

    @Test
    public void process() throws Exception {
        when(httpResponse.getStatusCode()).thenReturn(200);
        final Command command = new Command.Builder(new JSONObject(Resources.get("command.json"))).build();

        assertThat(command).isNotNull();
        subject.process(command);
        verify(httpClient).execute(captor.capture());

        final HttpRequest httpRequest = captor.getValue();
        assertThat(httpRequest).isNotNull();
        assertThat(httpRequest.getUrl()).isEqualTo("https://api.ampiri.com/v4/handshake?adPlaceId={adPlaceId}&deviceId={deviceId}");
    }
}
