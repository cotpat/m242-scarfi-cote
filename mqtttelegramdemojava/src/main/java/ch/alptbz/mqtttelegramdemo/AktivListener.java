package ch.alptbz.mqtttelegramdemo;

import java.util.EventListener;

public interface AktivListener extends EventListener {
    void aktivGeaendert(AktivStatusEvent event);
}
