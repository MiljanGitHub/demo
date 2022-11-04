package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.jsmpp.extra.SessionState;
import org.jsmpp.session.Session;
import org.jsmpp.session.SessionStateListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SessionStateListenerImpl implements SessionStateListener {

    public void onStateChange(SessionState newState, SessionState oldState, Session source) {
        log.info("Session {} changed from {} to {}", source.getSessionId(), oldState, newState);
    }
}
