package org.synyx.urlaubsverwaltung.calendarintegration.absence;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.synyx.urlaubsverwaltung.application.domain.Application;
import org.synyx.urlaubsverwaltung.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.settings.CalendarSettings;
import org.synyx.urlaubsverwaltung.settings.SettingsService;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.TEMPORARY_ALLOWED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.WAITING;
import static org.synyx.urlaubsverwaltung.calendarintegration.absence.Absence.of;


@Service
public class AbsenceServiceImpl implements AbsenceService {

    private final ApplicationService applicationService;
    private final SettingsService settingsService;

    @Autowired
    public AbsenceServiceImpl(ApplicationService applicationService, SettingsService settingsService) {

        this.applicationService = applicationService;
        this.settingsService = settingsService;
    }

    @Override
    public List<Absence> getOpenAbsences() {

        CalendarSettings calendarSettings = settingsService.getSettings().getCalendarSettings();
        AbsenceTimeConfiguration config = new AbsenceTimeConfiguration(calendarSettings);

        List<Application> applications = applicationService.getForStates(asList(ALLOWED, WAITING, TEMPORARY_ALLOWED));
        List<Absence> absences = new ArrayList<>();
        absences.addAll(applications.stream().map(application -> of(application, config)).collect(toList()));

        return absences;
    }
}
