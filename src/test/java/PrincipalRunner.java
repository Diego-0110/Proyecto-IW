
import com.intuit.karate.junit5.Karate;

class PrincipalRunner {

    @Karate.Test
    Karate testPrincipal() {
        return Karate.run("principal").relativeTo(getClass());
    }

    @Karate.Test
    Karate testAddEvent() {
        return Karate.run("addEvent").relativeTo(getClass());
    }

}
