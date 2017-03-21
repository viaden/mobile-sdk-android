package com.viaden.sdk.http;

import android.support.annotation.NonNull;

public enum HttpMethod {
    GET, POST, PUT, DELETE;

    /**
     * Creates a {@code Method} from the given string. Valid stings are {@code GET}, {@code POST},
     * {@code PUT} and {@code DELETE}.
     *
     * @param string The string value of this {@code Method}.
     * @return A {@code Method} based on the given string.
     */
    @NonNull
    public static HttpMethod parse(@NonNull final String string) {
        switch (string) {
            case "GET":
                return GET;
            case "POST":
                return POST;
            case "PUT":
                return PUT;
            case "DELETE":
                return DELETE;
            default:
                throw new IllegalArgumentException("Invalid http method: <" + string + ">");
        }
    }

    @NonNull
    @Override
    public String toString() {
        switch (this) {
            case GET:
                return "GET";
            case POST:
                return "POST";
            case PUT:
                return "PUT";
            case DELETE:
                return "DELETE";
            default:
                throw new IllegalArgumentException("Invalid http method: <" + this + ">");
        }
    }
}
