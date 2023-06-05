package es.ucm.fdi.iw.model;

import java.io.Serializable;

public class RatingUserId implements Serializable{
    
    private long userSource;
    
    private long userTarget;

    private long event;

    public long getUserSource(){
        return userSource;
    }

    public long getUserTarget(){
        return userTarget;
    }

    public long getEvent(){
        return event;
    }

    public void setUserSource(long user){
        this.userSource = user;
    }

    public void setUserTarget(long user){
        this.userTarget = user;
    }

    public void setEvent(long event){
        this.event = event;
    }
}
