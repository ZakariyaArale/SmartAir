package com.example.smartairsetup;

public interface DateCalculations {

    int[] MONTH_DAYS = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0));
    }

    default int daysInMonth(int month, int year) {
        if (month == 2 && isLeapYear(year)) {
            return 29;
        }
        return MONTH_DAYS[month - 1]; // month count starts at 1
    }

    default int daysDifference(int startDay, int startMonth, int startYear,
                               int endDay, int endMonth, int endYear) {

        int total = 0;

        //add months difference (including years).
        int monthDiff = 0;
        int m = startMonth;
        int y = startYear;

        if(endYear < startYear || (endMonth < startMonth && endYear == startYear) ||
                (endDay < startDay && endMonth == startMonth && endYear == startYear)){
            return -1; // means expired
        }

        while (y < endYear || (m < endMonth && y == endYear)){

            monthDiff += daysInMonth(m, y);

            m++;
            if (m > 12) {
                m = 1;
                y++;
            }
        }

        total += monthDiff;

        // Step 3: add day difference, could return negative value
        return total += (endDay - startDay);
    }
}
