package com.natural_interaction.TAL;

public interface TouchSource {

    void attach(TouchDispatcher dispatcher);

    void detach();

    String getName();
}
