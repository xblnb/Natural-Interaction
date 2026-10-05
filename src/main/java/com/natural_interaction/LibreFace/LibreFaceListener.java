package com.natural_interaction.LibreFace;

public interface LibreFaceListener {

    void onResult(LibreFaceResult result);

    default void onError(String message) {
    }
}
