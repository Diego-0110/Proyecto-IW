package es.ucm.fdi.iw;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


import es.ucm.fdi.iw.Repositories.EventRepository;
import es.ucm.fdi.iw.Repositories.UserRepository;
import es.ucm.fdi.iw.model.Event;
import es.ucm.fdi.iw.model.User;

@SpringBootApplication
public class IwApplication implements CommandLineRunner {

 @Autowired
	private EventRepository eventRepository;
	@Autowired
	private UserRepository userRepository;

	public static void main(String[] args) {
		SpringApplication.run(IwApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		//Aqui es para probar a insertar en la base de datos
	
    
				
	}

}
