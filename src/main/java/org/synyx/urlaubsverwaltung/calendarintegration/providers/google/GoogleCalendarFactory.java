package org.synyx.urlaubsverwaltung.calendarintegration.providers.google;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.synyx.urlaubsverwaltung.settings.SettingsService;

import java.io.IOException;
import java.security.GeneralSecurityException;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.synyx.urlaubsverwaltung.calendarintegration.providers.google.GoogleCalendarSyncProvider.APPLICATION_NAME;

@Service
public class GoogleCalendarFactory {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private static final String GOOGLEAPIS_OAUTH2_V4_TOKEN = "https://www.googleapis.com/oauth2/v4/token";

    private Calendar googleCalendarClient;
    private int refreshTokenHashCode;

    private final SettingsService settingsService;

    @Autowired
    public GoogleCalendarFactory(SettingsService settingsService) {

        this.settingsService = settingsService;
    }

    /**
     * Build and return an authorized google calendar client.
     *
     * @return an authorized calendar client service
     */
    Calendar getOrCreateGoogleCalendarClient() {

        String refreshToken = settingsService.getSettings().getCalendarSettings().getGoogleCalendarSettings().getRefreshToken();

        if (googleCalendarClient != null && refreshToken != null && refreshTokenHashCode == refreshToken.hashCode()) {
            LOG.debug("use cached googleCalendarClient");
            return googleCalendarClient;
        }

        try {
            LOG.info("create new googleCalendarClient");

            if (refreshToken != null) {
                refreshTokenHashCode = refreshToken.hashCode();
            }

            NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            TokenResponse tokenResponse = new TokenResponse();
            tokenResponse.setRefreshToken(refreshToken);

            Credential credential = createCredentialWithRefreshToken(httpTransport, JacksonFactory.getDefaultInstance(), tokenResponse);

            googleCalendarClient = new Calendar.Builder(httpTransport, JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

            return googleCalendarClient;

        } catch (GeneralSecurityException | IOException e) {
            LOG.error("Could not create or get a google calendar instance!", e);
        }

        return null;
    }

    private Credential createCredentialWithRefreshToken(HttpTransport transport, JsonFactory jsonFactory, TokenResponse tokenResponse) {

        String clientId = settingsService.getSettings().getCalendarSettings().getGoogleCalendarSettings().getClientId();
        String clientSecret = settingsService.getSettings().getCalendarSettings().getGoogleCalendarSettings().getClientSecret();

        return new Credential.Builder(BearerToken.authorizationHeaderAccessMethod())
            .setTransport(transport)
            .setJsonFactory(jsonFactory)
            .setTokenServerUrl(new GenericUrl(GOOGLEAPIS_OAUTH2_V4_TOKEN))
            .setClientAuthentication(new BasicAuthentication(clientId, clientSecret))
            .build()
            .setFromTokenResponse(tokenResponse);
    }
}
