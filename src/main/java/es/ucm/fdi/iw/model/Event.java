package es.ucm.fdi.iw.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event implements Transferable<Event.Transfer>{

    public enum Status {
        OPEN,
        CLOSED,
        FINISH
    }

    public enum Type {
        TRIP,
        EVENT,
        CONCERT,
        CAMPING,
        MUSEUM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "genevent")
    @SequenceGenerator(name = "genevent", sequenceName = "genevent", allocationSize = 1)
    private long id;
    private String title;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate initDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate finishDate;
    private String destination;
    private String reunionPoint;

    @Column(columnDefinition = "TEXT")
    private String description;
    private int price;
    private int capacity; // num of joined user -> UserEvent
    private String transport; // split by ',' to separate notes
    // PLANE, BUS, CAR, SHIP, ...
    private String notes; // split by ',' to separate notes
    // NO_KIDS, NO_ANIMALS, ...

    @Enumerated(EnumType.STRING)
    private Type type;
    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_owner_id", referencedColumnName = "id")
    private User userOwner;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
	public static class Transfer {
        private long id;
        private String title;
        private String initDate;
        private String finishDate;
        private String destination;
        private String reunionPoint;
        private String description;
        private int price;
        private int capacity;
        private String transport;
        private String notes;
        private String type;
        private String status;
        public Transfer(Event e) {
            this.id = e.getId();
            this.title = e.getTitle();
            this.initDate = e.getInitDate().toString();
            this.finishDate = e.getFinishDate().toString();
            this.destination = e.getDestination();
            this.reunionPoint = e.getReunionPoint();
            this.description = e.getDescription();
            this.price = e.getPrice();
            this.capacity = e.getCapacity();
            this.transport = e.getTransport();
            this.notes = e.getNotes();
            this.type = e.getType().toString();
            this.status = e.getStatus().toString();
        }
    }

    @Override
	public Transfer toTransfer() {
		return new Transfer(this);
    }
}
