package ch.alptbz.mqtttelegramdemo;

import java.util.EventObject;

public class AktivStatusEvent extends EventObject {
    Object source;
    Boolean statusalt;
    Boolean statusneu;

    /**
     * Constructs a prototypical Event.
     *
     * @param source the object on which the Event initially occurred
     * @throws IllegalArgumentException if source is null
     */
    public AktivStatusEvent(Object source, Boolean statusalt, Boolean statusneu) {
        super(source);
        this.statusalt = statusalt;
        this.statusneu = statusneu;
        this.source = source;
    }

    @Override
    public Object getSource() {
        return source;
    }

    public Boolean getStatusalt() {
        return statusalt;
    }

    public Boolean getStatusneu() {
        return statusneu;
    }
}
