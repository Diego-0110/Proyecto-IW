package es.ucm.fdi.iw.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Random;
import java.util.List;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;

import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.Repositories.EventRepository;
import es.ucm.fdi.iw.Repositories.RatingEventRepository;
import es.ucm.fdi.iw.Repositories.RatingUserRepository;
import es.ucm.fdi.iw.Repositories.UserEventRepository;
import es.ucm.fdi.iw.Repositories.UserRepository;
import es.ucm.fdi.iw.model.Event;
import es.ucm.fdi.iw.model.RatingUser;
import es.ucm.fdi.iw.model.RatingUserId;
import es.ucm.fdi.iw.model.RatingEvent;
import es.ucm.fdi.iw.model.RatingEventId;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.UserEvent;
import es.ucm.fdi.iw.model.UserEventId;
import es.ucm.fdi.iw.model.Event.Status;

@Controller()
@RequestMapping("event")
public class EventController {
    private static final Logger log = LogManager.getLogger(EventController.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private LocalData localData;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserEventRepository userEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RatingEventRepository ratingEventRepository;

    @Autowired
    private RatingUserRepository ratingUserRepository;

    // Return event/{id} page
    @GetMapping("{id}")
    public String index(@PathVariable long id, Model model, HttpSession session) {
        Event target = entityManager.find(Event.class, id);
        if (target == null) {
            return "redirect:/";
        }
        // Get UserEvent row from user logged and event id.
        User u = (User) session.getAttribute("u");
        UserEvent ue = null;
        if (u != null) {
            UserEventId ueId = new UserEventId();
            ueId.setEvent(id);
            ueId.setUser(u.getId());
            ue = entityManager.find(UserEvent.class, ueId);
        }

        
        File files = localData.getFile("event", "" + id);
        ArrayList<String> photosNames = new ArrayList<>();
        if (files.listFiles() != null) {
            for (var f : files.listFiles()) {
                String fileName = f.getName();
                if (fileName.indexOf(".") > 0) {
                    fileName = fileName.substring(0, fileName.lastIndexOf("."));
                }
                photosNames.add(fileName);
            }
        }
        // ATTRIBUTES FOR USER
        model.addAttribute("isLogged", u != null);
        model.addAttribute("fav", ue == null ? false : ue.getFav());
        model.addAttribute("joined", ue == null ? false : ue.getJoined());
        model.addAttribute("isOwner", u != null && (target.getUserOwner().getId() == u.getId()));
        // Check if user is in the event and the event has finished
        if (target.getStatus().equals(Event.Status.FINISH) && ue != null && ue.getJoined()) {
            ArrayList<User> joinedUsers = userRepository.getJoinedUsers(id, u.getId());
            model.addAttribute("joinedUsers", joinedUsers);
            model.addAttribute("canRate", true);

            RatingEventId reId = new RatingEventId();
            reId.setEvent(id);
            reId.setUserSource(u.getId());
            RatingEvent re = entityManager.find(RatingEvent.class, reId);
            model.addAttribute("ratingEvent", re);
            HashMap<Long, RatingUser> ru = new HashMap<>();
            for (var ju : joinedUsers) {
                RatingUserId ruId = new RatingUserId();
                ruId.setEvent(id);
                ruId.setUserSource(u.getId());
                ruId.setUserTarget(ju.getId());
                ru.put(ju.getId(), entityManager.find(RatingUser.class, ruId));
            }
            model.addAttribute("ratingUsers", ru);
        }

        // ATTRIBUTES FOR EVENT'S OWNER
        int numOwnerRatings = ratingUserRepository.getNumRatings(target.getUserOwner().getId());
        model.addAttribute("numOwnerRatings", numOwnerRatings);
        float avgOwnerRating = ratingUserRepository.getAverageRating(target.getUserOwner().getId());
        model.addAttribute("avgOwnerRating", (int) Math.ceil(avgOwnerRating));

        // ATTRIBUTES FOR EVENT
        int numFavs = eventRepository.getNumFavsEvent(id);
        model.addAttribute("numFavs", numFavs);
        int numJoinedUsers = eventRepository.getNumJoinedUsers(id);
        model.addAttribute("numJoinedUsers", numJoinedUsers);
        model.addAttribute("avgRating", (int) Math.ceil(ratingEventRepository.getAverageRating(id)));
        model.addAttribute("numRatings", ratingEventRepository.getNumRatings(id));
        model.addAttribute("eventRatings", ratingEventRepository.getRatings(id));
        model.addAttribute("photos", photosNames);
        model.addAttribute("event", target);
        return "event";
    }

    // Used to modify an existent event.
    // TODO add more attributes to change
    @PostMapping("{id}/change")
    public String changeEvent(@PathVariable long id, HttpServletResponse response,
            HttpSession session, Model model, @RequestParam(required = false) Event.Status status,
            @RequestParam(required = false) String description) {
        User u = (User) session.getAttribute("u");
        Event e = entityManager.find(Event.class, id);
        if (u != null && u.getId() == e.getUserOwner().getId()) {
            // It's not posible to open and event when it has no vacancies.
            int numJoinedUsers = eventRepository.getNumJoinedUsers(id);
            if (status != null && !(status == Event.Status.OPEN && numJoinedUsers >= e.getCapacity())) {
                e.setStatus(status);
            }
            e.setDescription(description == null ? e.getDescription() : description);
            eventRepository.save(e);
        }
        return "redirect:/event/" + id;
    }

    // Update userEvent table
    @PostMapping("{id}/userEvent")
    @ResponseBody
    @Transactional
    public String updateUserEvent(@PathVariable long id, Model model, HttpSession session,
            @RequestBody UserEvent body) {
        Boolean fav;
        Boolean joined;
        String rol;
        int additionJoin = 0;

        // Search UserEvent row with event=id and user=u.getId()
        User u = (User) session.getAttribute("u");
        Event e = entityManager.find(Event.class, id);
        UserEventId ueId = new UserEventId();
        ueId.setEvent(id);
        ueId.setUser(u.getId());
        UserEvent ue = entityManager.find(UserEvent.class, ueId);
        // There is no row, so we set the default value in attributes that
        // were no sent.
        if (ue == null) {
            fav = body.getFav() == null ? false : body.getFav();
            joined = body.getJoined() == null ? false : body.getJoined();
            rol = body.getRol() == null ? "" : body.getRol();
            // Update event occcupied atribute.
            additionJoin = joined ? 1 : 0;
        }
        // There is a row, so we update it with sent values.
        else {
            fav = body.getFav() == null ? ue.getFav() : body.getFav();
            joined = body.getJoined() == null ? ue.getJoined() : body.getJoined();
            rol = body.getRol() == null ? ue.getRol() : body.getRol();
            // Update event occcupied atribute.
            if (ue.getJoined() != joined) { // Value has changed
                additionJoin = joined ? 1 : -1;
            }
        }
        // If an event is not open you can't join.
        if (additionJoin == 1 && e.getStatus() != Status.OPEN) {
            throw new IllegalArgumentException();
        }

        // Update event status
        if (e.getStatus() == Status.OPEN) {
            int numJoinedUsers = eventRepository.getNumJoinedUsers(id);
            // Change event's status to closed or open.
            e.setStatus(numJoinedUsers == e.getCapacity() ? Status.CLOSED : Status.OPEN);
        }

        // Update UserEvent and Event tables.
        ue = new UserEvent(u, e, fav, joined, rol);
        userEventRepository.save(ue);
        eventRepository.save(e);
        return "ok";
    }

    // Add new event
    @Transactional
    @PostMapping("/saveEvent")
    public String saveUser(@ModelAttribute Event event, Model model, HttpSession session) {

        User u = (User) session.getAttribute("u");
        event.setStatus(Event.Status.OPEN);
        event.setUserOwner(u);

        event = eventRepository.saveAndFlush(event);
        UserEvent ue = new UserEvent();
        ue.setUser(u);
        ue.setEvent(event);
        ue.setFav(true);
        ue.setJoined(true);
        ue.setRol("ORGANIZER");
        userEventRepository.save(ue);
        return "redirect:/event/" + event.getId();
    }

    // Method to delete a event.
    @PostMapping("{id}/delete")
    public String deleteEvent(@PathVariable long id, Model model, HttpSession session) {
        Event event = entityManager.find(Event.class, id);
        User u = (User) session.getAttribute("u");
        if (event != null && u != null && 
            (u.hasRole(User.Role.ADMIN) || event.getUserOwner().getId() == u.getId())){
            eventRepository.delete(event);
            if (u.hasRole(User.Role.ADMIN)) {
                return "redirect:/admin/eventsList";
            }
            else {
                return "redirect:/user/" + u.getId();
            }
        }
        return "redirect:/";
    }

    // || IMAGES METHODS
    /**
     * Get a pic from event with name n
     * 
     * @param id
     * @param n
     * @return
     * @throws IOException
     */
    @GetMapping("{id}/pic/{n}")
    public StreamingResponseBody getPic(@PathVariable long id, @PathVariable String n) throws IOException {
        File f = localData.getFile("event/" + id, "" + n + ".jpg");
        InputStream in = new BufferedInputStream(f.exists() ? new FileInputStream(f) : EventController.defaultPic());
        return os -> FileCopyUtils.copy(in, os);
    }

    /**
     * Get first pic from an event
     * 
     * @param id
     * @return
     * @throws IOException
     */
    @GetMapping("{id}/firstPic")
    public StreamingResponseBody getFirstPic(@PathVariable long id) throws IOException {
        File fileDir = localData.getFile("event", "" + id);
        if (fileDir == null) {
            return os -> FileCopyUtils.copy(new BufferedInputStream(EventController.defaultPic()), os);
        }
        File[] files = fileDir.listFiles();
        InputStream in = new BufferedInputStream(
                files != null && files.length != 0 && files[0].exists() ? new FileInputStream(files[0])
                        : EventController.defaultPic());
        return os -> FileCopyUtils.copy(in, os);
    }

    /**
     * Uploads a new pic
     * 
     * @param id
     * @return
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    @PostMapping("{id}/addPic")
    public String addPic(@RequestParam("photo") MultipartFile photo,
            @PathVariable long id,
            HttpServletResponse response, HttpSession session, Model model)
            throws IOException, NoSuchAlgorithmException {
        // Check if user that do the POST is joined in the event.
        User u = (User) session.getAttribute("u");
        Event ev = entityManager.find(Event.class, id);
        UserEventId ueId = new UserEventId();
        ueId.setEvent(id);
        ueId.setUser(u != null ? u.getId() : null);
        UserEvent ue = entityManager.find(UserEvent.class, ueId);
        if (ue == null || !ue.getJoined()) {
            log.info("Failed to add photo in event#{}: user is not joined to the event.", id);
        }
        else if (ev == null || (u.getId() != ev.getUserOwner().getId() && 
            ev.getStatus() == Event.Status.OPEN)){
                log.info("Failed to add photo in event#{}: forbidden.", id);
        }
        else if (photo.isEmpty()) {
            log.info("failed to add photo in event#{}: emtpy file?", id);
        } else {
            byte[] bytes = photo.getBytes();
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.reset();
            m.update(bytes);
            byte[] digest = m.digest();
            BigInteger bigInt = new BigInteger(1, digest);
            String hashtext = bigInt.toString(16);
            File f = localData.getFile("event/" + id, "" + hashtext + ".jpg");
            try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f))) {

                stream.write(bytes);
                log.info("Uploaded photo for event#{} into {}!", id, f.getAbsolutePath());
            } catch (Exception e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                log.warn("Error uploading " + id + " ", e);
            }
        }
        return "redirect:/event/" + id;

    }

    @PostMapping("{id}/rmPic/{n}")
    public String removePic(@PathVariable long id, @PathVariable String n,
            HttpServletResponse response, HttpSession session, Model model)
            throws IOException, NoSuchAlgorithmException {
        File f = localData.getFile("event/" + id, "" + n + ".jpg");
        // Check if user is the event's owner.
        User u = (User) session.getAttribute("u");
        Event ev = entityManager.find(Event.class, id);

        if (u == null || u.getId() != ev.getUserOwner().getId()) {
            log.info("Failed to remove photo in event#{}: user is not the owner.", id);
        } else if (f == null) {
            log.info("Failed to remove photo in event#{}: empty file?", id);
        } else {
            f.delete();
            log.info("Deleted photo in event#{}: {}", id, n);
        }
        return "redirect:/event/" + id;
    }

    @PostMapping("{id}/coverPic/{n}")
    public String setCoverPic(@PathVariable long id, @PathVariable String n,
            HttpServletResponse response, HttpSession session, Model model)
            throws IOException, NoSuchAlgorithmException {
        if (n.compareTo("cover") == 0) {
            // This picture is already the cover.
            return "redirect:/event/" + id;
        }
        File f = localData.getFile("event/" + id, "" + n + ".jpg");
        User u = (User) session.getAttribute("u");
        Event ev = entityManager.find(Event.class, id);
        if (f == null) {
            log.info("Failed to set cover in event#{}: empty file?", id);
        }
        else if (u == null || u.getId() != ev.getUserOwner().getId()) {
            log.info("Failed to set cover in event#{}: user is not the owner.", id);
        }
        else {
            // Change actual cover.jpg to <hash>.jpg
            File fCover = localData.getFile("event/" + id, "cover.jpg");
            if (fCover.exists()) {
                byte[] bytes = Files.readAllBytes(fCover.toPath());
                MessageDigest m = MessageDigest.getInstance("MD5");
                m.reset();
                m.update(bytes);
                byte[] digest = m.digest();
                BigInteger bigInt = new BigInteger(1, digest);
                String hashtext = bigInt.toString(16);
                Path source = Paths.get(fCover.getAbsolutePath());
                Files.move(source, source.resolveSibling(hashtext + ".jpg"));
            }
            // Change name <hash n>.jpg to cover.jpg
            Path source = Paths.get(f.getAbsolutePath());
            Files.move(source, source.resolveSibling("cover.jpg"));
            log.info("New cover in event#{}", id);
        }
        return "redirect:/event/" + id;
    }

    @GetMapping("{id}/coverPic")
    public StreamingResponseBody getCoverPic(@PathVariable long id) throws IOException {
        File file = localData.getFile("event/" + id, "cover.jpg");
        InputStream in = new BufferedInputStream(
                file.exists() ? new FileInputStream(file) : EventController.defaultPic());
        return os -> FileCopyUtils.copy(in, os);
    }

    /**
     * Returns the default event pic
     * 
     * @return
     */
    private static InputStream defaultPic() {
        return new BufferedInputStream(Objects.requireNonNull(
                UserController.class.getClassLoader().getResourceAsStream(
                        "static/img/default-event-pic.jpg")));
    }

    // || RATING METHODS

    @PostMapping("{id}/rate")
	@ResponseBody
	@Transactional
	public String rateEvent(@PathVariable long id, Model model, HttpSession session,
			@RequestBody RatingEvent.Transfer ratingET) {
		RatingEvent ratingE = new RatingEvent();
		User userSource = (User) session.getAttribute("u");
		Event event = entityManager.find(Event.class, id);
        UserEventId ueId = new UserEventId();
        ueId.setEvent(id);
        ueId.setUser(userSource != null ? userSource.getId() : null);
        UserEvent ue = entityManager.find(UserEvent.class, ueId);
		if (event == null || event.getStatus() != Event.Status.FINISH
            || ue == null || !ue.getJoined()) {
            // The event doesn't exist, it's not finished or user is not joined in it.
			throw new IllegalArgumentException();
		}
        
		ratingE.setUserSource(userSource);
		ratingE.setEvent(event);
        ratingE.setRating(Math.min(Math.max(ratingET.getRating(), 0), 10));
		ratingE.setDescription(ratingET.getDescription());
		ratingEventRepository.save(ratingE);
		return "ok";
	}

    @PostMapping("{id}/rateUser/{uid}")
	@ResponseBody
	@Transactional
	public String rateUser(@PathVariable long id, @PathVariable long uid,
            Model model, HttpSession session,
			@RequestBody RatingUser.Transfer ratingUT) {
		RatingUser ratingU = new RatingUser();
		User userSource = (User) session.getAttribute("u");
        User userTarget = (User) entityManager.find(User.class, uid);
		Event event = entityManager.find(Event.class, id);

        UserEventId ueId = new UserEventId();
        ueId.setEvent(id);
        ueId.setUser(userSource != null ? userSource.getId() : null);
        UserEvent ue = entityManager.find(UserEvent.class, ueId);
        UserEventId ueId2 = new UserEventId();
        ueId2.setEvent(id);
        ueId2.setUser(userTarget != null ? userTarget.getId() : null);
        UserEvent ue2 = entityManager.find(UserEvent.class, ueId2);
		if (userSource == null || userTarget == null || userSource.getId() == userTarget.getId() ||
            event == null || event.getStatus() != Event.Status.FINISH 
            || ue == null || !ue.getJoined() || ue2 == null || !ue.getJoined()) {
            // userSource and userTarget are the same, the event doesn't exist,
            // the event is not finished or one of the users is not joined in it.
			throw new IllegalArgumentException();
		}
        
		ratingU.setUserSource(userSource);
        ratingU.setUserTarget(userTarget);
		ratingU.setEvent(event);
        ratingU.setRating(Math.min(Math.max(ratingUT.getRating(), 0), 10));
		ratingU.setDescription(ratingUT.getDescription());
		ratingUserRepository.save(ratingU);
		return "ok";
	}
}
