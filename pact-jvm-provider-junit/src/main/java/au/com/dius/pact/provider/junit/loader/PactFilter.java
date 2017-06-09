package com.cisco.lockhart.pact;

/*
 * Copyright (c) Cisco Systems 2017. All rights reserved.
 *
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PactFilter {
    String[] value();
}
