package me.denley.courier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the field should be set or method should be called with Wearable API messages for a specific path.
 * This will occur only when the message is received. If this device is disconnected when the message is sent, it will not receive it.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface ReceiveMessages {

    /** The path for which to receive messages. */
    String value();

}
