package org.synyx.urlaubsverwaltung.calendarintegration.providers.ical;

import net.fortuna.ical4j.model.Calendar;
import org.junit.Before;
import org.junit.Test;
import org.synyx.urlaubsverwaltung.calendarintegration.absence.Absence;
import org.synyx.urlaubsverwaltung.calendarintegration.absence.AbsenceTimeConfiguration;
import org.synyx.urlaubsverwaltung.calendarintegration.absence.EventType;
import org.synyx.urlaubsverwaltung.absence.service.AbsenceService;
import org.synyx.urlaubsverwaltung.period.DayLength;
import org.synyx.urlaubsverwaltung.period.Period;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.settings.CalendarSettings;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.calendarintegration.absence.EventType.ALLOWED_APPLICATION;
import static org.synyx.urlaubsverwaltung.calendarintegration.absence.EventType.SICKNOTE;
import static org.synyx.urlaubsverwaltung.calendarintegration.absence.EventType.WAITING_APPLICATION;
import static org.synyx.urlaubsverwaltung.period.DayLength.FULL;
import static org.synyx.urlaubsverwaltung.period.DayLength.MORNING;


public class ICalServiceImplTest {

    private ICalServiceImpl sut;
    private AbsenceService absenceServiceMock;
    private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private String currentDTSTAMP;
    private String currentDTSTAMPPlusOneSecond;

    @Before
    public void setUp() {

        absenceServiceMock = mock(AbsenceService.class);
        sut = new ICalServiceImpl(absenceServiceMock);

        ZonedDateTime dateTime = ZonedDateTime.now(ZoneOffset.UTC);
        currentDTSTAMP = dateTimeFormatter.format(dateTime); // This is not very nice. But there is no way to manipulate the ical lib to print a prepared stamp
        currentDTSTAMPPlusOneSecond = dateTimeFormatter.format(dateTime.plusSeconds(1)); // This is not very nice. But there is no way to manipulate the ical lib to print a prepared stamp
    }


    @Test
    public void getICal() {

        Absence absenceA = absence("APPLICATION_1", "Peter", "Sagan", toDateTime("2019-03-26"),
                toDateTime("2019-03-27"), FULL, ALLOWED_APPLICATION);
        Absence absenceB = absence("APPLICATION_2", "Foo", "Bar", toDateTime("2019-04-26"), toDateTime("2019-04-26"),
                MORNING, WAITING_APPLICATION);
        Absence absenceC = absence("APPLICATION_2", "Sick", "Paul", toDateTime("2019-05-26"), toDateTime("2019-05-30"),
                FULL, SICKNOTE);
        List<Absence> absences = asList(absenceA, absenceB, absenceC);
        when(absenceServiceMock.getOpenAbsences()).thenReturn(absences);

        String result = sut.getICal();

        try {
            assertEquals(expectedICalString(currentDTSTAMP), result);
        } catch (AssertionError e) {
            assertEquals(expectedICalString(currentDTSTAMPPlusOneSecond), result);
        }
    }


    private String expectedICalString(String currentDTSTAMP) {

        return "BEGIN:VCALENDAR\r\n"
            + "PRODID:-//Urlaubsverwaltung//iCal4j 1.0//DE\r\n"
            + "X-WR-CALNAME:Urlaube\r\n"
            + "VERSION:2.0\r\n"
            + "BEGIN:VEVENT\r\n"
            + "DTSTAMP:" + currentDTSTAMP + "\r\n"
            + "DTSTART;VALUE=DATE:20190326\r\n"
            + "SUMMARY:Urlaub Sagan Peter\r\n"
            + "UID:APPLICATION_1\r\n"
            + "END:VEVENT\r\n"
            + "BEGIN:VEVENT\r\n"
            + "DTSTAMP:" + currentDTSTAMP + "\r\n"
            + "DTSTART:20190426T080000\r\n"
            + "DTEND:20190426T120000\r\n"
            + "SUMMARY:Antrag auf Urlaub Bar Foo\r\n"
            + "UID:APPLICATION_2\r\n"
            + "END:VEVENT\r\n"
            + "BEGIN:VEVENT\r\n"
            + "DTSTAMP:" + currentDTSTAMP + "\r\n"
            + "DTSTART;VALUE=DATE:20190526\r\n"
            + "SUMMARY:Paul Sick krank\r\n"
            + "UID:APPLICATION_2\r\n"
            + "END:VEVENT\r\n"
            + "END:VCALENDAR\r\n";
    }


    @Test
    public void validateICal() {

        Absence absence = absence("APPLICATION_1", "Peter", "Sagan", toDateTime("2019-03-26"),
                toDateTime("2019-03-27"), FULL, ALLOWED_APPLICATION);
        List<Absence> absences = asList(absence);
        when(absenceServiceMock.getOpenAbsences()).thenReturn(absences);

        Calendar iCalObject = sut.getICalObject();
        iCalObject.validate();
    }


    private Absence absence(String id, String firstName, String lastName, LocalDate start, LocalDate end,
                            DayLength length, EventType eventType) {

        Person person = new Person("", firstName, lastName, "doesntmatter");
        Period period = new Period(start, end, length);
        AbsenceTimeConfiguration timConfig = new AbsenceTimeConfiguration(new CalendarSettings());

        return new Absence(id, person, period, eventType, timConfig);
    }


    private static LocalDate toDateTime(String input) {

        String pattern = "yyyy-MM-dd";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        return LocalDate.parse(input, formatter);
    }
}
