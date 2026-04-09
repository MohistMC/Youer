package com.mohistmc.youer.util;

public class TimeUtils {

    public static String formatDuration(long seconds) {
        if (seconds < 60) {
            return I18n.as("time.unit.second", String.valueOf(seconds));
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long remainingSeconds = seconds % 60;
            if (remainingSeconds > 0) {
                return I18n.as("time.unit.minute", String.valueOf(minutes)) + " " +
                        I18n.as("time.unit.second", String.valueOf(remainingSeconds));
            } else {
                return I18n.as("time.unit.minute", String.valueOf(minutes));
            }
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            long remainingSeconds = seconds % 60;

            String hourPart = I18n.as("time.unit.hour", String.valueOf(hours));
            String minutePart = minutes > 0 ? I18n.as("time.unit.minute", String.valueOf(minutes)) : "";
            String secondPart = remainingSeconds > 0 ? I18n.as("time.unit.second", String.valueOf(remainingSeconds)) : "";

            StringBuilder sb = new StringBuilder();
            sb.append(hourPart);
            if (!minutePart.isEmpty()) {
                sb.append(" ").append(minutePart);
            }
            if (!secondPart.isEmpty()) {
                sb.append(" ").append(secondPart);
            }
            return sb.toString();
        }
    }
}
