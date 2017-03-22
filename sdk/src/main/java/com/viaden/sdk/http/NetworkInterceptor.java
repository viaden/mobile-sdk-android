package com.viaden.sdk.http;

import android.support.annotation.NonNull;

import java.io.IOException;

/**
 * {@code NetworkInterceptor} is used to observe requests going out and the corresponding responses coming back in.
 */
public interface NetworkInterceptor {
    /**
     * Intercepts a {@link HttpRequest} with the help of
     * {@link NetworkInterceptor.Chain} and returns the intercepted {@link HttpResponse}.
     *
     * @param chain The helper chain we use to get the request, proceed the request and receive the response.
     * @return The intercepted response.
     * @throws IOException if an I/O error occurs
     */
    @NonNull
    HttpResponse intercept(@NonNull Chain chain) throws IOException;

    /**
     * {@code Chain} is used to chain the interceptors. It can get the request from the previous
     * interceptor, proceed the request to the next interceptor and get the response from the next
     * interceptor. In most of the cases, you don't need to implement this interface.
     */
    interface Chain {

        /**
         * Gets the {@link HttpRequest} from this chain.
         *
         * @return The {@link HttpRequest} of this chain.
         */
        @NonNull
        HttpRequest getRequest();

        @NonNull
        ResponseContentFactory<?> getFactory();

        /**
         * Proceeds the intercepted {@link HttpRequest} in this chain to next
         * {@code NetworkInterceptor} or network and gets the {@link HttpResponse}.
         *
         * @param request The intercepted {@link HttpRequest}.
         * @return The {@link HttpResponse} from next {@code NetworkInterceptor} or network.
         * @throws IOException if an I/O error occurs
         */
        @NonNull
        HttpResponse proceed(@NonNull HttpRequest request, @NonNull ResponseContentFactory<?> factory) throws IOException;
    }
}
