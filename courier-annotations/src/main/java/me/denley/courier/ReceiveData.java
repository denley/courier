package me.denley.courier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that the field should be set or method should be called with data items from the Wearable API for a specific path.
 *
 * This will occur in any instance of the following three cases:
 * 1) Immediately after {@link Courier#startReceiving} is called (but asynchronously).
 * 2) Whenever a device connects to the Wearable API.
 * 3) Whenever a connected device updates the data on the path.
 *
 * Having multiple devices put data items onto the same path is discouraged, but in this case you may include a Node parameter
 * for methods with this annotation, to determine the source of the data.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface ReceiveData {

    /** The path for which to receive data items. */
    String value();

}
