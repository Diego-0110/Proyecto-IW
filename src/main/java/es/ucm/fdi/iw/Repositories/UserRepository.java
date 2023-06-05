package es.ucm.fdi.iw.Repositories;

import java.util.ArrayList;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import es.ucm.fdi.iw.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query(value = "SELECT * FROM IWUser " +
    "WHERE IWUser.username = :username AND IWUser.status != 'SUSPENDED'", nativeQuery = true)  
    User byUsername(@Param("username") String username);

    @Query(value = "SELECT count(*) FROM IWUser " +
    "WHERE IWUser.username = :username", nativeQuery = true) 
    int hasUsername(@Param("username") String username);

    @Query(value = "SELECT count(*) FROM IWUser " +
    "WHERE IWUser.email = :email", nativeQuery = true) 
    int hasEmail(@Param("email") String email);
    
    // List of user joined
    @Query(value = "SELECT * FROM User_Event ue " +
    "INNER JOIN IWUser u ON ue.user_id = u.id " +
    "WHERE ue.event_id = :eventId AND ue.joined = TRUE AND ue.user_id <> :userId", nativeQuery = true)  
    ArrayList<User> getJoinedUsers(@Param("eventId") long eventId, @Param("userId") long userId);

    @Query(value = "SELECT * FROM IWUser "+
        "WHERE IWUser.status = :status", nativeQuery = true)
    Page<User> getUsersByStatus(@Param("status") String status, final Pageable pageable);

}
