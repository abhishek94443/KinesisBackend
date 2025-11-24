package com.myapp.kinesis.modules.vendor.dto;

import lombok.Data;

import java.time.LocalTime;
import java.util.Map;

@Data
public class BusinessHoursConfig {
    private Map<String, DayHours> businessHours;
    private String timezone;
    private int slotIncrement; // Minutes

    @Data
    public static class DayHours {
        private LocalTime open;
        private LocalTime close;
        private boolean closed;
    }

    public static BusinessHoursConfig getDefault() {
        BusinessHoursConfig config = new BusinessHoursConfig();
        config.setTimezone("Asia/Kolkata");
        config.setSlotIncrement(15);

        // Default: 9 AM - 5 PM, Monday-Friday
        Map<String, DayHours> hours = new java.util.HashMap<>();

        for (String day : new String[]{"MON", "TUE", "WED", "THU", "FRI"}) {
            DayHours dayHours = new DayHours();
            dayHours.setOpen(LocalTime.of(9, 0));
            dayHours.setClose(LocalTime.of(17, 0));
            dayHours.setClosed(false);
            hours.put(day, dayHours);
        }

        DayHours saturday = new DayHours();
        saturday.setOpen(LocalTime.of(10, 0));
        saturday.setClose(LocalTime.of(16, 0));
        saturday.setClosed(false);
        hours.put("SAT", saturday);

        DayHours sunday = new DayHours();
        sunday.setClosed(true);
        hours.put("SUN", sunday);

        config.setBusinessHours(hours);
        return config;

    }
}