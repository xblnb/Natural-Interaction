package com.natural_interaction.LEA.capture;

import com.natural_interaction.LEA.LeaConfig;

import java.awt.image.BufferedImage;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

public final class LeaReplayFrameSource implements LeaFrameSource {

    private final LeaConfig config;
    private final List<Path> frames = new ArrayList<>();

    private int index;
    private boolean open;
    private volatile String lastError = "";

    public static boolean isAvailable() {
        return true;
    }

    public LeaReplayFrameSource(LeaConfig config) {
        this.config = config;
        try {
            Path directory = Paths.get(config.replayDirectory);
            if (!Files.isDirectory(directory)) {
                lastError = "replay directory not found: " + directory.toAbsolutePath();
                return;
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path path : stream) {
                    if (!Files.isRegularFile(path)) {
                        continue;
                    }
                    String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (name.endsWith(".png") || name.endsWith(".jpg")
                            || name.endsWith(".jpeg") || name.endsWith(".bmp")) {
                        frames.add(path);
                    }
                }
            }
            frames.sort(Comparator.comparing(path -> path.getFileName().toString()));
            if (frames.isEmpty()) {
                lastError = "no image frames in " + directory.toAbsolutePath();
                return;
            }
            this.open = true;
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            this.open = false;
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public int getFrameRate() {
        return config.targetFps;
    }

    @Override
    public BufferedImage grab() {
        if (!open) {
            return null;
        }
        if (index >= frames.size()) {
            if (!config.replayLoop) {
                return null;
            }
            index = 0;
        }
        Path path = frames.get(index++);
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                lastError = "unreadable frame: " + path;
            }
            return image;
        } catch (Throwable t) {
            lastError = LeaReflect.describe(t);
            return null;
        }
    }

    @Override
    public String getName() {
        return "image-replay";
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public void close() {
        open = false;
        index = 0;
    }
}
