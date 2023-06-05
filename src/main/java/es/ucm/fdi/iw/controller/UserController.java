package es.ucm.fdi.iw.controller;

import es.ucm.fdi.iw.LocalData;
import es.ucm.fdi.iw.Repositories.EventRepository;
import es.ucm.fdi.iw.Repositories.RatingUserRepository;
import es.ucm.fdi.iw.Repositories.ReportRepository;
import es.ucm.fdi.iw.Repositories.UserRepository;
import es.ucm.fdi.iw.model.Event;
import es.ucm.fdi.iw.model.Message;
import es.ucm.fdi.iw.model.RatingUser;
import es.ucm.fdi.iw.model.Report;
import es.ucm.fdi.iw.model.Transferable;
import es.ucm.fdi.iw.model.User;
import es.ucm.fdi.iw.model.User.Role;
import es.ucm.fdi.iw.model.User.Status;
import es.ucm.fdi.iw.model.User.Level;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.persistence.EntityManager;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.JSONPObject;

import java.io.*;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.Year;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * User management.
 *
 * Access to this end-point is authenticated.
 */
@Controller()
@RequestMapping("user")
public class UserController {

	private static final Logger log = LogManager.getLogger(UserController.class);

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private LocalData localData;

	@Autowired
	private SimpMessagingTemplate messagingTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private ReportRepository reportRepository;

	@Autowired
	private EventRepository eventRepository;

	@Autowired
	private RatingUserRepository ratingUserRepository;

	@Autowired
	private UserRepository userRepository;

	/**
	 * Exception to use when denying access to unauthorized users.
	 * 
	 * In general, admins are always authorized, but users cannot modify
	 * each other's profiles.
	 */
	@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "No eres administrador, y éste no es tu perfil") // 403
	public static class NoEsTuPerfilException extends RuntimeException {
	}

	/**
	 * Encodes a password, so that it can be saved for future checking. Notice
	 * that encoding the same password multiple times will yield different
	 * encodings, since encodings contain a randomly-generated salt.
	 * 
	 * @param rawPassword to encode
	 * @return the encoded password (typically a 60-character string)
	 *         for example, a possible encoding of "test" is
	 *         {bcrypt}$2y$12$XCKz0zjXAP6hsFyVc8MucOzx6ER6IsC1qo5zQbclxhddR1t6SfrHm
	 */
	public String encodePassword(String rawPassword) {
		return passwordEncoder.encode(rawPassword);
	}

	/**
	 * Generates random tokens. From https://stackoverflow.com/a/44227131/15472
	 * 
	 * @param byteLength
	 * @return
	 */
	public static String generateRandomBase64Token(int byteLength) {
		SecureRandom secureRandom = new SecureRandom();
		byte[] token = new byte[byteLength];
		secureRandom.nextBytes(token);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(token); // base64 encoding
	}

	/**
	 * Landing page for a user profile
	 */
	@GetMapping("{id}")
	public String index(@PathVariable long id, Model model, HttpSession session,
		@RequestParam(name = "filter", defaultValue = "none") String filter,
		@RequestParam(name = "page", defaultValue = "0") int page,
		@RequestParam(name = "size", defaultValue = "10") int size) {
		User target = entityManager.find(User.class, id);
		User u = (User) session.getAttribute("u");

		// TODO check use
		if (u != null && u.getStatus().equals(User.Status.SUSPENDED)) {
			return "redirect:/error";
		}
		Page<Event> events;
		if (filter.equalsIgnoreCase("my")) {
			events = eventRepository.getUserEvents(id, PageRequest.of(page, size));
		}
		else if (filter.equalsIgnoreCase("finished")){
			events = eventRepository.getEventsJoinedByStatus(id, Event.Status.FINISH.toString(),
				PageRequest.of(page, size));
		}
		else if (filter.equalsIgnoreCase("open")){
			events = eventRepository.getEventsJoinedByStatus(id, Event.Status.OPEN.toString(),
				PageRequest.of(page, size));
		}
		else if (filter.equalsIgnoreCase("closed")) {
			events = eventRepository.getEventsJoinedByStatus(id, Event.Status.CLOSED.toString(),
				PageRequest.of(page, size));
		}
		else if (filter.equalsIgnoreCase("fav")){
			events = eventRepository.getFavEvents(id, PageRequest.of(page, size));
		}
		else {
			events = eventRepository.getEventsJoined(id, PageRequest.of(page, size));
		}

		ArrayList<Report> userReports = reportRepository.findReportsByUserId(target.getId());
		int numReports = 0;
		if (!userReports.isEmpty()) {
			numReports = userReports.size();
		}

		model.addAttribute("isLogged", u != null);
		model.addAttribute("isOwner", u != null && (target.getId() == u.getId()));
		
		paginationModelAttrs(model, "events", events, page, size, filter);
		
		model.addAttribute("user", target);
		model.addAttribute("age", target.getAge());
		model.addAttribute("numReports", numReports);
		int numOwnerRatings = ratingUserRepository.getNumRatings(id);
        model.addAttribute("numOwnerRatings", numOwnerRatings);
        float avgOwnerRating = ratingUserRepository.getAverageRating(id);
        model.addAttribute("avgOwnerRating", (int) Math.ceil(avgOwnerRating));
		ArrayList<RatingUser> ownerRatings = ratingUserRepository.getRatings(id);
		model.addAttribute("ownerRatings", ownerRatings);
		return "user";
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

	public Page<Event> pageImplement(ArrayList<Event> ev) {
		int total = ev.size();
		int page = 0, size = 5;
		int fromIndex = (page) * size;
		int toIndex = Math.min(fromIndex + size, total);
		Page<Event> pageEvents;

		List<Event> pageList = ev.subList(fromIndex, toIndex);
		PageRequest pageRequest = PageRequest.of(page, size);
		pageEvents = new PageImpl<>(pageList, pageRequest, total);
		return pageEvents;
	}

	/**
	 * Returns the default profile pic
	 * 
	 * @return
	 */
	private static InputStream defaultPic() {
		return new BufferedInputStream(Objects.requireNonNull(
				UserController.class.getClassLoader().getResourceAsStream(
						"static/img/default-pic.jpg")));
	}

	/**
	 * Downloads a profile pic for a user id
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	@GetMapping("{id}/pic")
	public StreamingResponseBody getPic(@PathVariable long id) throws IOException {
		File f = localData.getFile("user", "" + id + ".jpg");
		InputStream in = new BufferedInputStream(f.exists() ? new FileInputStream(f) : UserController.defaultPic());
		return os -> FileCopyUtils.copy(in, os);
	}

	/**
	 * Uploads a profile pic for a user id
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 */
	@PostMapping("{id}/pic")
	public String setPic(@RequestParam("photo") MultipartFile photo, @PathVariable long id,
			HttpServletResponse response, HttpSession session, Model model) throws IOException {

		User target = entityManager.find(User.class, id);
		model.addAttribute("user", target);

		// check permissions
		User requester = (User) session.getAttribute("u");
		if (requester.getId() != target.getId() &&
				!requester.hasRole(Role.ADMIN)) {
			throw new NoEsTuPerfilException();
		}

		log.info("Updating photo for user {}", id);
		File f = localData.getFile("user", "" + id + ".jpg");
		if (photo.isEmpty()) {
			log.info("failed to upload photo: emtpy file?");
		} else {
			try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(f))) {
				byte[] bytes = photo.getBytes();
				stream.write(bytes);
				log.info("Uploaded photo for {} into {}!", id, f.getAbsolutePath());
			} catch (Exception e) {
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				log.warn("Error uploading " + id + " ", e);
			}
		}
		return "redirect:/user/" + id;
	}

	@PostMapping("{id}/report")
	@ResponseBody
	@Transactional
	public String reportUser(@PathVariable long id, Model model, HttpSession session,
			@RequestBody Report.Transfer reportT) throws JsonMappingException, JsonProcessingException {
		Report report = new Report();
		User userSource = (User) session.getAttribute("u");
		User userTarget = entityManager.find(User.class, id);
		Event event = entityManager.find(Event.class, reportT.getEvent());
		if (userSource == null || userTarget == null 
			|| userSource.getId() == userTarget.getId()) {
			// Users should exists and can't be the same.
			throw new IllegalArgumentException();
		}
		report.setUserSource(userSource);
		report.setUserTarget(userTarget);
		report.setDescription(reportT.getDescription());
		report.setCause(Report.Cause.valueOf(reportT.getCause()));
		report.setEvent(event);
		report.setDateSent(LocalDateTime.now());
		reportRepository.save(report);

		return "ok";
	}

	// Register a new user.
	@Transactional
	@PostMapping("/register")
	public String register(@ModelAttribute User user, Model model, HttpSession session) {
		if (userRepository.hasUsername(user.getUsername()) > 0 || userRepository.hasEmail(user.getEmail()) > 0){
			return "redirect:/signUp";
		}
		user.setPassword(encodePassword(user.getPassword()));
		user.setStatus(Status.ACTIVE);
		user.setRoles(Role.USER.name());
		user.setLevel(Level.NONE);
		user = userRepository.saveAndFlush(user);
		return "redirect:/user/" + user.getId();

	}

	@Transactional
	@PostMapping("{id}/changeStatus/{status}")
	public String changeStatus(@PathVariable long id, @PathVariable User.Status status,
		Model model, HttpSession session){
		User user = entityManager.find(User.class, id);
		User u = (User) session.getAttribute("u");	
		if (user != null && status != null && u != null && u.hasRole(Role.ADMIN)) {
			user.setStatus(status);
			userRepository.save(user);
			return "redirect:/admin/";
		}
		return "redirect:/";
	}

	@Transactional
	@PostMapping("{id}/deleteUser")
	public String deleteUser(@PathVariable long id, Model model, HttpSession session) {
		User u = (User) session.getAttribute("u");	
		User user = entityManager.find(User.class, id);

		if (user != null && u != null && u.hasRole(Role.ADMIN)) {
			userRepository.delete(user);
			return "redirect:/admin/";
		}
		return "redirect:/";
	}
}
