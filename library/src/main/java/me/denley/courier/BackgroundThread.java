package me.denley.courier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When used with {@link me.denley.courier.LocalNode}, {@link me.denley.courier.RemoteNodes}, {@link me.denley.courier.ReceiveMessages},
 * or {@link me.denley.courier.ReceiveData}, this annotation denotes that a method call should not be made on the main thread.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface BackgroundThread {
}
