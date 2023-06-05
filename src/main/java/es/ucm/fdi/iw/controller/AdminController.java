package es.ucm.fdi.iw.controller;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.Repositories.EventRepository;
import es.ucm.fdi.iw.Repositories.RatingUserRepository;
import es.ucm.fdi.iw.Repositories.ReportRepository;
import es.ucm.fdi.iw.Repositories.UserRepository;
import es.ucm.fdi.iw.model.Event;
import es.ucm.fdi.iw.model.Report;
import es.ucm.fdi.iw.model.User;

/**
 * Site administration.
 *
 * Access to this end-point is authenticated - see SecurityConfig
 */
@Controller
@RequestMapping("admin")
public class AdminController {

    private static final Logger log = LogManager.getLogger(AdminController.class);

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private RatingUserRepository ratingUserRepository;
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LocalData localData;

    @GetMapping("/")
    public String index(Model model) {
        return "redirect:/admin/usersList";
    }

    private void paginationModelAttrs(Model model, String name, Page<?> objectPage, int page, int size, String filter){
        model.addAttribute(name, objectPage.getContent());
        model.addAttribute("size", size);
        model.addAttribute("showedElems", objectPage.getContent().size());
        model.addAttribute("currentPage", page);
        model.addAttribute("numPages", objectPage.getTotalPages());
        model.addAttribute("numPagesArray", new int[objectPage.getTotalPages()]);
        model.addAttribute("filter", filter);
    }

    @GetMapping("/usersList")
    public String usersList(Model model,
            @RequestParam(name = "filter", defaultValue = "none") String filter,
            @RequestParam(name = "name", defaultValue = "") String name,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<User> usersPage;
        if (filter.equalsIgnoreCase("active")){
            usersPage = userRepository.getUsersByStatus(User.Status.ACTIVE.name(), pageRequest);
        }
        else if (filter.equalsIgnoreCase("suspended")) {
            usersPage = userRepository.getUsersByStatus(User.Status.SUSPENDED.name(), pageRequest);
        }
        else if (filter.equalsIgnoreCase("black_listed")) {
            usersPage = userRepository.getUsersByStatus(User.Status.BLACK_LISTED.name(), pageRequest);
        }
        else {
            usersPage = userRepository.findAll(pageRequest);
        }
        Map<Long, Integer> userNumReports = new HashMap<>();
        for(var u: usersPage.getContent()) {
            userNumReports.put(u.getId(), reportRepository.numReportsByUserId(u.getId()));
        }

        paginationModelAttrs(model, "users", usersPage, page, size, filter);
        model.addAttribute("userNumReports", userNumReports);
        return "admin";
    }

    @GetMapping("/eventsList")
    public String eventsList(Model model,
            @RequestParam(name = "filter", defaultValue = "none") String filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<Event> eventsPage = eventRepository.findAll(PageRequest.of(page, size));
        HashMap<Long, Integer> numImgs = new HashMap<>();
        HashMap<Long, Integer> numJoinedUsers = new HashMap<>();
        HashMap<Long, Integer> numOwnerRatingsMap = new HashMap<>();
        HashMap<Long, Integer> avgOwnerRatingMap = new HashMap<>();
        for (var ev : eventsPage.getContent()) {
            File files = localData.getFile("event", "" + ev.getId());
            numImgs.put(ev.getId(), files.listFiles() != null ? files.listFiles().length : 0);
            int numOccupied = eventRepository.getNumJoinedUsers(ev.getId());
            numJoinedUsers.put(ev.getId(), numOccupied);

            int numOwnerRatings = ratingUserRepository.getNumRatings(ev.getUserOwner().getId());
            numOwnerRatingsMap.put(ev.getId(), numOwnerRatings);
            float avgOwnerRating = ratingUserRepository.getAverageRating(ev.getUserOwner().getId());
            avgOwnerRatingMap.put(ev.getId(), (int) Math.ceil(avgOwnerRating));
        }
        model.addAttribute("numImgs", numImgs);
        model.addAttribute("numJoinedUsers", numJoinedUsers);
        model.addAttribute("numOwnerRatings", numOwnerRatingsMap);
        model.addAttribute("avgOwnerRating", avgOwnerRatingMap);
        paginationModelAttrs(model, "events", eventsPage, page, size, filter);
        return "admin";
    }

    @GetMapping("/reportsList")
    public String reportsList(Model model,
            @RequestParam(name = "filter", defaultValue = "none") String filter,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Page<Report> reportsPage = reportRepository.findAll(PageRequest.of(page, size));

        paginationModelAttrs(model, "reports", reportsPage, page, size, filter);
        return "admin";
    }


    // Method to delete a report.
    @PostMapping("/deleteReport/{id}")
    public String deleteReport(@PathVariable long id, Model model, HttpSession session) {
        Report report = entityManager.find(Report.class, id);
        User u = (User) session.getAttribute("u");
        if (report != null && u != null && u.hasRole(User.Role.ADMIN)){
            reportRepository.delete(report);
        }
        return "redirect:/admin/reportsList";
    }

}
