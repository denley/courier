package me.denley.courier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that a List of Nodes representing the connected devices should be provided to a field or method.
 * This will be done when {@link Courier#startReceiving} is called (asynchronously), and whenever a
 * device connects or disconnects from the wearable API.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface RemoteNodes {

}
