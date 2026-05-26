package com.craftstudio.launcher.utils.file;

public class InvalidFilenameException extends RuntimeException {
    private final FilenameErrorType type;
    private String illegalCharacters = null;
    private int invalidLength = -1;

    public InvalidFilenameException(String message, String illegalCharacters) {
        super(message);
        this.type = FilenameErrorType.CONTAINS_ILLEGAL_CHARACTERS;
        this.illegalCharacters = illegalCharacters;
    }

    public InvalidFilenameException(String message, int invalidLength) {
        super(message);
        this.type = FilenameErrorType.INVALID_LENGTH;
        this.invalidLength = invalidLength;
    }

    public boolean containsIllegalCharacters() {
        return type == FilenameErrorType.CONTAINS_ILLEGAL_CHARACTERS;
    }

    public String getIllegalCharacters() {
        return illegalCharacters;
    }

    public boolean isInvalidLength() {
        return type == FilenameErrorType.INVALID_LENGTH;
    }

    public int getInvalidLength() {
        return invalidLength;
    }

    private enum FilenameErrorType {
        CONTAINS_ILLEGAL_CHARACTERS,
        INVALID_LENGTH
    }
}
