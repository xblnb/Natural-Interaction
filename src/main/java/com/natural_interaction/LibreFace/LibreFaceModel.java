package com.natural_interaction.LibreFace;

public enum LibreFaceModel {
    AU_ENCODER("LibreFace_AU_Encoder.onnx", "image", true),
    AU_INTENSITY("LibreFace_AU_Intensity.onnx", "feature", false),
    AU_PRESENCE("LibreFace_AU_Presence.onnx", "feature", false),
    FACIAL_EXPRESSION("LibreFace_FE.onnx", "image", true);

    public static final int IMAGE_INPUT_SIZE = 224;
    public static final int FEATURE_SIZE = 512;
    public static final int AU_COUNT = 12;
    public static final int EXPRESSION_COUNT = 8;

    private final String fileName;
    private final String inputName;
    private final boolean imageInput;

    LibreFaceModel(String fileName, String inputName, boolean imageInput) {
        this.fileName = fileName;
        this.inputName = inputName;
        this.imageInput = imageInput;
    }

    public String getFileName() {
        return fileName;
    }

    public String getInputName() {
        return inputName;
    }

    public boolean isImageInput() {
        return imageInput;
    }
}
