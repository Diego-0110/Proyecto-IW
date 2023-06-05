package es.ucm.fdi.iw.model;

import java.io.Serializable;

public class RatingEventId implements Serializable{
    private long userSource;

    private long event;

    public long getUserSource(){
        return userSource;
    }

    public long getEvent(){
        return event;
    }

    public void setUserSource(long user){
        this.userSource = user;
    }

    public void setEvent(long event){
        this.event = event;
    }
}
