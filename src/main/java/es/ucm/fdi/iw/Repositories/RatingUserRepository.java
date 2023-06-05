package es.ucm.fdi.iw.Repositories;

import java.util.ArrayList;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import es.ucm.fdi.iw.model.RatingUser;

public interface RatingUserRepository extends JpaRepository<RatingUser, Long> {
    @Query(value = "SELECT COALESCE(SUM(ru.rating) / COUNT(*), 0) FROM RATING_USER AS ru " +
    "WHERE ru.USER_TARG_ID = :userId", nativeQuery = true)
    float getAverageRating(@Param("userId") long userId);

    @Query(value = "SELECT * FROM RATING_USER AS ru " +
    "WHERE ru.USER_TARG_ID = :userId", nativeQuery = true)
    ArrayList<RatingUser> getRatings(@Param("userId") long userId);

    @Query(value = "SELECT count(*) FROM RATING_USER AS ru " +
    "WHERE ru.USER_TARG_ID = :userId", nativeQuery = true)
    int getNumRatings(@Param("userId") long userId);
}
