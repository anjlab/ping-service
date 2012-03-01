package com.anjlab.ping.services.location;

import java.util.TimeZone;

public interface TimeZoneResolver {

    public TimeZone resolveTimeZone(double latitude, double longitude);
    
}
