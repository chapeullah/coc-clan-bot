package com.chapeullah.cocclanbot.player.exception;

public class PlayerNotFoundException extends RuntimeException {
    public PlayerNotFoundException(String tag, Throwable cause) {
        super("Player not found: playerTag=\"" + tag + "\"", cause);
    }
}
