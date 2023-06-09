
package es.ucm.fdi.iw.model;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * An authorized user of the system.
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name="IWUser")
public class User {

    public enum Role {
        USER,			// normal users 
        ADMIN,          // admin users
    }
    public enum Level {
        NONE,
        BRONZE,
        SILVER,
        GOLD
    }
    public enum Status {
        ACTIVE,
        SUSPENDED,
        BLACK_LISTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "gen")
    @SequenceGenerator(name = "gen", sequenceName = "gen")
	private long id;

    @Column(nullable = false, unique = true)
    private String username;
    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;

    private String location;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthdate;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    private String  languages;// split by ',' to separate languages
 
    @Enumerated(EnumType.STRING)
    private Level level;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(unique = true)
    private String email;


    @OneToMany(mappedBy = "event")
    private List<UserEvent> userEvent = new ArrayList<>();

    private String roles; // split by ',' to separate roles User or Admin	

    /**
     * Checks whether this user has a given role.
     * @param role to check
     * @return true iff this user has that role.
     */
    public boolean hasRole(Role role) {
        String roleName = role.name();
        return Arrays.asList(roles.split(",")).contains(roleName);
    }

    public int getAge(){
        LocalDate now = LocalDate.now();
        return Period.between(birthdate, now).getYears();
    }
	
	@Override
	public String toString() {
		// return toTransfer().toString();
        return "user-" + id + "-" + username;
	}
}

