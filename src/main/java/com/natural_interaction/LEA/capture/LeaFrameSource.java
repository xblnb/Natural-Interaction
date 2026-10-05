package com.natural_interaction.LEA.capture;

import java.awt.image.BufferedImage;

public interface LeaFrameSource {

    boolean isOpen();

    int getFrameRate();

    BufferedImage grab();

    String getName();

    String getLastError();

    void close();
}
