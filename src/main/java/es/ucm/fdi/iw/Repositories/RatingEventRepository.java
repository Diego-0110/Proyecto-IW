package es.ucm.fdi.iw.Repositories;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.ucm.fdi.iw.model.RatingEvent;

public interface RatingEventRepository extends JpaRepository<RatingEvent, Long> {

    @Query(value = "SELECT COALESCE(SUM(re.rating) / COUNT(*), 0) FROM RATING_EVENT AS re " +
    "WHERE re.EVENT_ID = :eventId", nativeQuery = true)
    float getAverageRating(@Param("eventId") long eventId);

    @Query(value = "SELECT * FROM RATING_EVENT AS re " +
    "WHERE re.EVENT_ID = :eventId", nativeQuery = true)
    ArrayList<RatingEvent> getRatings(@Param("eventId") long eventId);

    @Query(value = "SELECT count(*) FROM RATING_EVENT AS re " +
    "WHERE re.event_id = :eventId", nativeQuery = true)
    int getNumRatings(@Param("eventId") long eventId);
}
