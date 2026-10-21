package com.gregguns.compat.questbook;

/** The shipped questbook resources are missing, unreadable or inconsistent. Nothing is installed. */
public final class QuestbookException extends Exception {

    private static final long serialVersionUID = 1L;

    public QuestbookException(String message) {
        super(message);
    }

    public QuestbookException(String message, Throwable cause) {
        super(message, cause);
    }
}
