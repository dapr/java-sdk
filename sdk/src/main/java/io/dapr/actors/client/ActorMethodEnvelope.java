/*
 * Copyright (c) Microsoft Corporation.
 * Licensed under the MIT License.
 */

package io.dapr.actors.client;

/**
 * Request and Response object used to talk to Actors.
 */
public class ActorMethodEnvelope {

    /**
     * Data serialized for input/output of Actor methods.
     */
    private byte[] data;

    /**
     * Gets the data serialized for input/output of Actor methods.
     *
     * @return Data serialized for input/output of Actor methods.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Sets the data serialized for input/output of Actor methods.
     *
     * @param data Data serialized for input/output of Actor methods.
     */
    public void setData(byte[] data) {
        this.data = data;
    }
}
