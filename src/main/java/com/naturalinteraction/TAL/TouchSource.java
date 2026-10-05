package com.naturalinteraction.TAL;

public interface TouchSource {

    void attach(TouchDispatcher dispatcher);

    void detach();

    String getName();
}
