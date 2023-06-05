package es.ucm.fdi.iw.Repositories;

import java.time.LocalDate;
import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.ucm.fdi.iw.model.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
        @Query(value = "SELECT * FROM Event " +
                        "WHERE LOWER(title) LIKE LOWER(:title) AND init_date >= :ini " +
                        "AND finish_date <= :fin", nativeQuery = true)
        Page<Event> getEventsByTitle(@Param("title") String title, @Param("ini") LocalDate ini,
                @Param("fin") LocalDate fin, final Pageable pageable);

        @Query(value = "SELECT count(*) FROM User_Event AS ue " +
                        "WHERE ue.event_id = :eventId AND ue.fav = TRUE", nativeQuery = true)
        int getNumFavsEvent(@Param("eventId") long eventId);

        @Query(value = "SELECT Event.* FROM Event " +
                        "INNER JOIN User_Event ON Event.id = User_Event.event_id " +
                        "WHERE User_Event.user_id = :userId AND User_Event.fav = TRUE", nativeQuery = true)
        Page<Event> getFavEvents(@Param("userId") long userId, final Pageable pageable);

        @Query(value = "SELECT count(*) FROM User_Event ue " +
        "INNER JOIN IWUser u ON ue.user_id = u.id " +
        "WHERE ue.event_id = :eventId AND ue.joined = TRUE", nativeQuery = true)  
        int getNumJoinedUsers(@Param("eventId") long eventId);

        @Query(value = "SELECT Event.* FROM Event " +
                        "INNER JOIN User_Event ON Event.id = User_Event.event_id " +
                        "WHERE User_Event.user_id = :userId AND User_Event.Joined = TRUE", nativeQuery = true)
        ArrayList<Event> getAllEventsJoined(@Param("userId") long userId);

        @Query(value = "SELECT Event.* FROM Event " +
                        "INNER JOIN User_Event ON Event.id = User_Event.event_id " +
                        "WHERE User_Event.user_id = :userId AND User_Event.Joined = TRUE", nativeQuery = true)
        Page<Event> getEventsJoined(@Param("userId") long userId, final Pageable pageable);

        // To Filter by Status
        @Query(value = "SELECT Event.* FROM Event " +
                        "INNER JOIN User_Event ON Event.id = User_Event.event_id " +
                        "WHERE User_Event.user_id = :userId AND Event.status = :status AND User_Event.Joined = TRUE", nativeQuery = true)
        Page<Event> getEventsJoinedByStatus(@Param("userId") long userId, @Param("status") String status, final Pageable pageable);

        @Query(value = "SELECT * FROM Event " +
                        "WHERE Event.user_owner_id = :userId", nativeQuery = true)
        Page<Event> getUserEvents(@Param("userId") long userId, final Pageable pageable);

        // To Filter by Status
        @Query(value = "SELECT * FROM Event " +
                        "WHERE Event.user_owner_id = :userId AND Event.status = :status", nativeQuery = true)
        Page<Event> getUserEventsByStatus(@Param("userId") long userId, @Param("status") String status, final Pageable pageable);

}
