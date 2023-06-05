package es.ucm.fdi.iw.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.ucm.fdi.iw.Repositories.EventRepository;
import es.ucm.fdi.iw.Repositories.RatingUserRepository;
import es.ucm.fdi.iw.model.Event;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserEvent;
import es.ucm.fdi.iw.model.UserEventId;

@Controller
public class SearchController {
	
    @Autowired
    private EntityManager entityManager;
    
    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RatingUserRepository ratingUserRepository; 


    public  List<Integer> splitdate(String date) {
   
        List<Integer> aux = new ArrayList<>();
    
        // Split the input string using space as the delimiter
        String[] words = date.split("-");
        for(int i=0; i<words.length; i++){
            aux.add(Integer.parseInt(words[i]));
        }
        
        return aux;
    }
 
    @GetMapping("/search")
    public String search(Model model, HttpSession session,
        @RequestParam(name="page",defaultValue="0") int page,
        @RequestParam(name="size",defaultValue="10") int size,
        @RequestParam(name="search",defaultValue = "") String search,
        @RequestParam(name = "init", defaultValue = "") String init,
        @RequestParam(name = "fin", defaultValue = "") String fin) {
        Page<Event> PageEvents;
        
        LocalDate initDate = LocalDate.of(1999,1,1);
        LocalDate finDate = LocalDate.of(3000,1,1);
        if(!init.isEmpty()){
            initDate = LocalDate.of(splitdate(init).get(0),splitdate(init).get(1),splitdate(init).get(2));
            finDate = LocalDate.of(splitdate(fin).get(0),splitdate(fin).get(1),splitdate(fin).get(2));
        }

        if(search.isEmpty()){
            PageEvents = eventRepository.findAll(PageRequest.of(page, size));
        }
        else {
            PageEvents = eventRepository.getEventsByTitle(search, initDate, finDate, PageRequest.of(page, size));
        }

        User u = (User)session.getAttribute("u");
        Boolean isLogged = u != null;
        model.addAttribute("isLogged", isLogged);
        model.addAttribute("allEvents", PageEvents.getContent());

        List<Boolean> favPageEvents = new ArrayList<>();
        HashMap<Long, Integer> numJoinedUsers = new HashMap<>();
        HashMap<Long, Integer> numOwnerRatingsMap = new HashMap<>();
        HashMap<Long, Integer> avgOwnerRatingMap = new HashMap<>();
        for (Event event : PageEvents) {
            if (isLogged) {
                UserEventId ueId = new UserEventId();
                ueId.setEvent(event.getId());
                ueId.setUser(u.getId());
                UserEvent ue = entityManager.find(UserEvent.class, ueId);
                favPageEvents.add(ue == null? false: ue.getFav());
            }
            else {
                favPageEvents.add(false);
            }
            int numOccupied = eventRepository.getNumJoinedUsers(event.getId());
            numJoinedUsers.put(event.getId(), numOccupied);

            int numOwnerRatings = ratingUserRepository.getNumRatings(event.getUserOwner().getId());
            numOwnerRatingsMap.put(event.getId(), numOwnerRatings);
            float avgOwnerRating = ratingUserRepository.getAverageRating(event.getUserOwner().getId());
            avgOwnerRatingMap.put(event.getId(), (int) Math.ceil(avgOwnerRating));
        }
        model.addAttribute("favPageEvents", favPageEvents);
        model.addAttribute("numJoinedUsers", numJoinedUsers);
        model.addAttribute("numOwnerRatings", numOwnerRatingsMap);
        model.addAttribute("avgOwnerRating", avgOwnerRatingMap);

        model.addAttribute("numpages", new int[PageEvents.getTotalPages()]);
        model.addAttribute("numResults", PageEvents.getTotalPages() * size);
        model.addAttribute("numFirstRes", (page * size) + 1);
        model.addAttribute("size", size);
        model.addAttribute("currentPage", page);
        model.addAttribute("numPages", PageEvents.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("init", init);
        model.addAttribute("fin", fin);
        return "search";
    }

}
