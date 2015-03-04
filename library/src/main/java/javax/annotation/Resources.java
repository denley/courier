/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package javax.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This class is used to allow multiple resources declarations.
 *
 * @see javax.annotation.Resource
 * @since Common Annotations 1.0
 */

@Documented
@Retention(RUNTIME)
@Target(TYPE)
public @interface Resources {
   /**
    * Array used for multiple resource declarations.
    */
   Resource[] value();
}
