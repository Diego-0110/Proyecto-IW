package es.ucm.fdi.iw.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@IdClass(RatingEventId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RatingEvent implements Transferable<RatingEvent.Transfer>{

    @Id
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "event_id", referencedColumnName = "id")
    private Event event;

    @Id
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_src_id", referencedColumnName = "id")
    private User userSource;

    private int rating;
    
    @Column(columnDefinition = "TEXT")
    private String description;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
	public static class Transfer {
        private long event;
        private long userSource;
        private int rating;
        private String description;
        public Transfer(RatingEvent r) {
            this.userSource = r.getUserSource().getId();
            this.event = r.getEvent().getId();
            this.rating = r.getRating();
            this.description = r.getDescription();
        }
    }

    @Override
	public Transfer toTransfer() {
		return new Transfer(this);
    }
}
