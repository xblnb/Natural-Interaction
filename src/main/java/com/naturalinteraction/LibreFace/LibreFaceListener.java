package com.naturalinteraction.LibreFace;

public interface LibreFaceListener {

    void onResult(LibreFaceResult result);

    default void onError(String message) {
    }
}
