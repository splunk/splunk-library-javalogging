/*
 Copyright © 2019 Splunk Inc.
 SPLUNK CONFIDENTIAL – Use or disclosure of this material in whole or in part
 without a valid written license from Splunk Inc. is PROHIBITED.
 */
package com.splunk.logging.hec;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetadataTags {
    public static final String TIME = "time";
    public static final String HOST = "host";
    public static final String INDEX = "index";
    public static final String SOURCE = "source";
    public static final String SOURCETYPE = "sourcetype";
    public static final String MESSAGEFORMAT = "messageFormat";
    public static final String AWAITTERMINATIONTIMEOUT = "awaitTerminationTimeout";
    public static final String AWAITTERMINATIONTIMEUNIT = "awaitTerminationTimeUnit";
    public static final Set<String> HEC_TAGS = Stream.of(TIME, HOST, INDEX, SOURCE, SOURCETYPE)
            .collect(Collectors.toSet());
    public static final Set<String> INTERNAL_TAGS = Stream.of(MESSAGEFORMAT, AWAITTERMINATIONTIMEOUT, AWAITTERMINATIONTIMEUNIT)
            .collect(Collectors.toSet());
}
