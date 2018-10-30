package com.splunk.logging;

import org.json.simple.JSONValue;

/**
 * 
 * Represents format of a message. It is used to apply necessary parsing and formatting to the message, if required for
 * the message format.
 * <p>
 * If format is not set, then it is assumed to be "text"
 * </p>
 * <p>
 * Note: Access modifier is intentionally left default which allows this enum to be accessed only within the package.
 * </p>
 *
 */
enum MessageFormat {

    TEXT("text"),
    JSON("json");

    private final String format;

    MessageFormat(final String format) {
        this.format = format;
    }

    /**
     * Parses the message on the basis of message format.
     * 
     * 
     * @param message the message string
     * 
     * @return parsed message object based on format
     */
    Object parse(final String message) {
        // if message is null or blank then return without parsing
        if (message == null || message.trim().length() == 0) {
            return message;
        }

        switch (this) {
            case JSON:
                return parseJsonEventMessage(message);

            case TEXT:
            default:
                // Treat message type as text if format is not defined or defined as text
                return message;
        }
    }

    /**
     * Parses the message JSON string into JSON object. If parsing fails then the input message is returned as is.
     *
     * @param message the message string
     * @return the parsed message JSON object or input message if parsing fails
     */
    private Object parseJsonEventMessage(final String message) {
        final Object jsonObject = JSONValue.parse(message);
        if (jsonObject == null) {
            // If JSON parsing failed then it is likely a text message or a malformed JSON message.
            // Return input message string in such an event.
            return message;
        } else {
            return jsonObject;
        }
    }

    /**
     * Gets MessageFormat instance from format string.
     *
     * @param format the message format
     * @return the MessageFormat enum
     */
    static MessageFormat fromFormat(String format) {
        if (format != null && format.trim().length() > 0) {
            format = format.toLowerCase();
            for (final MessageFormat formatEnum : values()) {
                if (formatEnum.format.equals(format)) {
                    return formatEnum;
                }
            }
        }
        return TEXT;
    }
}