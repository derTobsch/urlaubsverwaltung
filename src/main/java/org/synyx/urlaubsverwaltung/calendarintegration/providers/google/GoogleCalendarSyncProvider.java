package org.synyx.urlaubsverwaltung.calendarintegration.providers.google;

import com.google.api.client.http.HttpResponse;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.synyx.urlaubsverwaltung.calendarintegration.absence.Absence;
import org.synyx.urlaubsverwaltung.calendarintegration.providers.CalendarProvider;
import org.synyx.urlaubsverwaltung.mail.MailService;
import org.synyx.urlaubsverwaltung.settings.CalendarSettings;
import org.synyx.urlaubsverwaltung.settings.GoogleCalendarSettings;
import org.synyx.urlaubsverwaltung.settings.SettingsService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.Collections.singletonList;
import static org.apache.http.HttpStatus.SC_OK;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class GoogleCalendarSyncProvider implements CalendarProvider {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    static final String APPLICATION_NAME = "Urlaubsverwaltung";

    private Calendar googleCalendarClient;
    private final MailService mailService;
    private final SettingsService settingsService;
    private final GoogleCalendarFactory googleCalendarFactory;

    @Autowired
    public GoogleCalendarSyncProvider(MailService mailService, SettingsService settingsService, GoogleCalendarFactory googleCalendarFactory) {

        this.settingsService = settingsService;
        this.mailService = mailService;
        this.googleCalendarFactory = googleCalendarFactory;
    }

    @Override
    public Optional<String> add(Absence absence, CalendarSettings calendarSettings) {

        googleCalendarClient = googleCalendarFactory.getOrCreateGoogleCalendarClient();

        if (googleCalendarClient != null) {
            GoogleCalendarSettings googleCalendarSettings =
                settingsService.getSettings().getCalendarSettings().getGoogleCalendarSettings();
            String calendarId = googleCalendarSettings.getCalendarId();

            try {
                final Event eventToCommit = fillEvent(absence, new Event());
                Event eventInCalendar = googleCalendarClient.events().insert(calendarId, eventToCommit).execute();

                LOG.info("Event {} for '{}' added to calendar '{}'.", eventInCalendar.getId(),
                    absence.getPerson().getNiceName(), eventInCalendar.getSummary());
                return Optional.of(eventInCalendar.getId());

            } catch (IOException ex) {
                LOG.warn("An error occurred while trying to add appointment to calendar {}", calendarId, ex);
                mailService.sendCalendarSyncErrorNotification(calendarId, absence, ex.toString());
            }
        }
        return Optional.empty();
    }


    @Override
    public void update(Absence absence, String eventId, CalendarSettings calendarSettings) {

        googleCalendarClient = googleCalendarFactory.getOrCreateGoogleCalendarClient();

        if (googleCalendarClient != null) {

            String calendarId = settingsService.getSettings().getCalendarSettings().getGoogleCalendarSettings().getCalendarId();

            try {
                // gather exiting event
                Event event = googleCalendarClient.events().get(calendarId, eventId).execute();

                // update event with absence
                final Event updatedEvent = fillEvent(absence, event);

                // sync event to calendar
                googleCalendarClient.events().patch(calendarId, eventId, updatedEvent).execute();

                LOG.info("Event {} has been updated in calendar '{}'.", eventId, calendarId);
            } catch (IOException ex) {
                LOG.warn("Could not update event {} in calendar '{}'.", eventId, calendarId, ex);
                mailService.sendCalendarUpdateErrorNotification(calendarId, absence, eventId, ex.getMessage());
            }
        }
    }

    @Override
    public void delete(String eventId, CalendarSettings calendarSettings) {

        googleCalendarClient = googleCalendarFactory.getOrCreateGoogleCalendarClient();

        if (googleCalendarClient != null) {

            String calendarId = settingsService.getSettings().getCalendarSettings().getGoogleCalendarSettings().getCalendarId();

            try {
                googleCalendarClient.events().delete(calendarId, eventId).execute();

                LOG.info("Event {} has been deleted in calendar '{}'.", eventId, calendarId);
            } catch (IOException ex) {
                LOG.warn("Could not delete event {} in calendar '{}'", eventId, calendarId, ex);
                mailService.sendCalendarDeleteErrorNotification(calendarId, eventId, ex.getMessage());
            }
        }
    }

    @Override
    public void checkCalendarSyncSettings(CalendarSettings calendarSettings) {

        googleCalendarClient = googleCalendarFactory.getOrCreateGoogleCalendarClient();

        if (googleCalendarClient != null) {
            String calendarId =
                settingsService.getSettings().getCalendarSettings().getGoogleCalendarSettings().getCalendarId();
            try {
                HttpResponse httpResponse = googleCalendarClient.calendarList().get(calendarId).executeUsingHead();
                if (httpResponse.getStatusCode() == SC_OK) {
                    LOG.info("Calendar sync successfully activated!");
                } else {
                    throw new IOException(httpResponse.getStatusMessage());
                }
            } catch (IOException e) {
                LOG.warn("Could not connect to calendar with calendar id '{}'", calendarId, e);
            }
        }
    }

    private static Event fillEvent(Absence absence, Event event) {

        final Event newEvent = event.clone();

        newEvent.setSummary(absence.getEventSubject());

        EventAttendee eventAttendee = new EventAttendee();
        eventAttendee.setEmail(absence.getPerson().getEmail());
        eventAttendee.setDisplayName(absence.getPerson().getNiceName());
        newEvent.setAttendees(singletonList(eventAttendee));

        EventDateTime startEventDateTime;
        EventDateTime endEventDateTime;

        if (absence.isAllDay()) {
            // To create an all-day event, you must use setDate() having created DateTime objects using a String
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String startDateStr = dateTimeFormatter.format(absence.getStartDate());
            String endDateStr = dateTimeFormatter.format(absence.getEndDate());

            DateTime startDateTime = new DateTime(startDateStr);
            DateTime endDateTime = new DateTime(endDateStr);

            startEventDateTime = new EventDateTime().setDate(startDateTime);
            endEventDateTime = new EventDateTime().setDate(endDateTime);
        } else {
            DateTime dateTimeStart = new DateTime(Date.from(absence.getStartDate().toInstant()));
            DateTime dateTimeEnd = new DateTime(Date.from(absence.getEndDate().toInstant()));

            startEventDateTime = new EventDateTime().setDateTime(dateTimeStart);
            endEventDateTime = new EventDateTime().setDateTime(dateTimeEnd);
        }

        newEvent.setStart(startEventDateTime);
        newEvent.setEnd(endEventDateTime);

        return newEvent;
    }
}
