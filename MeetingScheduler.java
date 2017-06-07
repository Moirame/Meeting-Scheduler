/**
 * Created by Moira on 6/7/2017.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Scanner;

public class MyMeetingScheduler {

    private class CalendarEvent implements Comparable<CalendarEvent> {
        private Calendar start;
        private Calendar end;
        private long length;
        private String userId;

        protected Calendar getStart() {
            return this.start;
        }

        protected Calendar getEnd() {
            return this.end;
        }

        protected String getUserId() {
            return this.userId;
        }

        protected Long getLength() {
            return this.length;
        }

        CalendarEvent(Calendar start, Calendar end, String userId) {
            this.start = start;
            this.end = end;
            this.userId = userId;
            this.length = end.getTime().getTime() - start.getTime().getTime();
        }

        @Override
        public int compareTo(final CalendarEvent b) {
            // sort by start date time
            return this.getStart().compareTo(b.getStart());
        }

    }

    // this function parse CSV to CalendarEvents object
    public ArrayList<CalendarEvent> getCalendarFromCSV(File file) throws FileNotFoundException, ParseException {
        ArrayList<CalendarEvent> calendarEvents = new ArrayList<>();
        Scanner scanner = new Scanner(file);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss");
        // each new line is a new entry by user
        scanner.useDelimiter("\n");

        while (scanner.hasNext()) {
            String[] values = scanner.next().split(",");
            //  only care about event start before 10pm and end after 8am
            String startReg = "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] ([0-1][0-9]|2[0-1]):([0-9][0-9]):([0-9][0-9])$";
            String endReg = "[0-9][0-9][0-9][0-9]-[0-1][0-9]-[0-3][0-9] ([1-2][0-9]|0[8-9]):([0-9][0-9]):([0-9][0-9])$";
            if (values[1].matches(startReg) && values[2].matches(endReg)) {
                Calendar start = Calendar.getInstance();
                Calendar end = Calendar.getInstance();
                Calendar currentDate = Calendar.getInstance();

                currentDate = getBeginingOfDay(currentDate);
                start.setTime(sdf.parse(values[1]));
                end.setTime(sdf.parse(values[2]));

                // only search for time slots on days up to one week from the date the program is run
                if (start.after(currentDate)) {
                    calendarEvents.add(new CalendarEvent(start, end, values[0]));
                }
            }
        }

        scanner.close();

        // sort by start date
        Collections.sort(calendarEvents);
        return calendarEvents;
    }

    private Calendar getBeginingOfDay(Calendar c) {
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    // returns the longest available time slot
    public CalendarEvent findLongestAvailableSlot(File file) throws FileNotFoundException, ParseException {
        // get all events, sorted
        ArrayList<CalendarEvent> calendarEvents = getCalendarFromCSV(file);

        Calendar currentDate = Calendar.getInstance();
        currentDate = getBeginingOfDay(currentDate);

        Calendar oneWeekAfter = Calendar.getInstance();
        oneWeekAfter.add(Calendar.DATE, 7);
        oneWeekAfter = getBeginingOfDay(oneWeekAfter);

        CalendarEvent timeSlot = null;
        CalendarEvent current;

        Calendar timeSlotStart = currentDate;
        timeSlotStart.set(Calendar.HOUR_OF_DAY, 8);

        // only search for time slots on days up to one week from the date the program is run
        for (int i = 0; i < calendarEvents.size() && calendarEvents.get(i).end.before(oneWeekAfter); i++) {
            current = calendarEvents.get(i);
            if (current.start.before(timeSlotStart)) {
                // this could only happen on the first element
                timeSlotStart = current.end;
            } else {
                if (timeSlotStart.before(current.start)) {
                    // available slot
                    if (timeSlot == null) {
                        timeSlot = new CalendarEvent(timeSlotStart, current.start, null);
                    } else {
                        // update time slot
                        Long length = current.start.getTime().getTime() - timeSlotStart.getTime().getTime();
                        timeSlot = timeSlot.getLength().compareTo(length) > 0 ? timeSlot
                                : new CalendarEvent(timeSlotStart, current.start, null);
                    }
                }
            }
        }

        return timeSlot;
    }

}