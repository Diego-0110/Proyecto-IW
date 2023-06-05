package es.ucm.fdi.iw.model;

import java.time.LocalDateTime;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report implements Transferable<Report.Transfer>{

    public enum Cause {
        EVENT_PAGE,
        MESSAGE,
        DURING_EVENT,
        OTHER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "rep")
    @SequenceGenerator(name = "rep", sequenceName = "rep", initialValue = 6, allocationSize = 1)
    private Long id;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_source", referencedColumnName = "id")
    private User userSource;
    
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_target", referencedColumnName = "id")
    private User userTarget;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "event", referencedColumnName = "id")
    private Event event;
    
    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDateTime dateSent;

    @Enumerated(EnumType.STRING)
    private Cause cause;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
	public static class Transfer {
        private long id;
        private long userSource;
        private long userTarget;
        private long event;
        private String description;
        private String dateSent;
        private String cause;
        public Transfer(Report r) {
            this.id = r.getId();
            this.userSource = r.getUserSource().getId();
            this.userTarget = r.getUserTarget().getId();
            this.event = r.getEvent().getId();
            this.description = r.getDescription();
            this.dateSent = r.getDateSent().toString();
            this.cause = r.getCause().toString();
        }
    }

    @Override
	public Transfer toTransfer() {
		return new Transfer(this);
    }
}
