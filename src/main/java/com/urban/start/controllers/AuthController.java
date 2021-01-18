package com.urban.start.controllers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urban.start.models.ERole;
import com.urban.start.models.Movie;
import com.urban.start.models.Role;
import com.urban.start.models.User;
import com.urban.start.payload.request.LoginRequest;
import com.urban.start.payload.request.MovieRequest;
import com.urban.start.payload.request.SignupRequest;
import com.urban.start.payload.response.JwtResponse;
import com.urban.start.payload.response.MessageResponse;
import com.urban.start.repository.MovieRepository;
import com.urban.start.repository.RoleRepository;
import com.urban.start.repository.UserRepository;
import com.urban.start.security.jwt.JwtUtils;
import com.urban.start.security.services.UserDetailsImpl;




@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
	@Autowired
	AuthenticationManager authenticationManager;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	MovieRepository movieRepository;

	@Autowired
	PasswordEncoder encoder;

	@Autowired
	JwtUtils jwtUtils;

	@PostMapping("/signin")
	public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();		
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());

		return ResponseEntity.ok(new JwtResponse(jwt, 
												 userDetails.getId(), 
												 userDetails.getName(),
												 userDetails.getUsername(), 
												 userDetails.getEmail(), 
												 userDetails.getMobileno(),
												 roles));
	}

	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
		if (userRepository.existsByUsername(signUpRequest.getUsername())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Username is already taken!"));
		}

		if (userRepository.existsByEmail(signUpRequest.getEmail())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Email is already in use!"));
		}
		
		// Create new user's account
		User user = new User(signUpRequest.getName(),signUpRequest.getUsername(), 
							 signUpRequest.getEmail(),signUpRequest.getMobileno(),
							 encoder.encode(signUpRequest.getPassword()));

		Set<String> strRoles = signUpRequest.getRole();
		
		Set<Role> roles = new HashSet<>();

		if (strRoles == null) {
			
			Role userRole = roleRepository.findByName(ERole.ROLE_USER)
					.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
			roles.add(userRole);
		} else {
			strRoles.forEach(role -> {
				switch (role) {
				case "admin":
					Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(adminRole);

					break;
				case "mod":
					Role modRole = roleRepository.findByName(ERole.ROLE_MANAGER)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(modRole);

					break;
				default:
					Role userRole = roleRepository.findByName(ERole.ROLE_USER)
							.orElseThrow(() -> new RuntimeException("Error: Role is not found."));
					roles.add(userRole);
				}
			});
		}

		user.setRoles(roles);
		userRepository.save(user);

		return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
	}

	@PostMapping("/addmovies")
	public ResponseEntity<?> addMovies(@Valid @RequestBody MovieRequest movieRequest) {
		Movie movie = new Movie(movieRequest.getName(),movieRequest.getLanguage(),movieRequest.getGenre(),
			movieRequest.getDescription(),movieRequest.getDate(),movieRequest.getTime(),
			movieRequest.getImage());
	
		String strUser = movieRequest.getUser();
	
		Set<User> users = new HashSet<>();
	
		User movieUser = userRepository.findByUsername(strUser)
			.orElseThrow(() -> new RuntimeException("Error: User Not Found."));
	
		users.add(movieUser);
	
		movie.setUser(users);
		movieRepository.save(movie);
	
		return ResponseEntity.ok(new MessageResponse("Movie added successfully!"));
	}

	@GetMapping("/getmovies")
	public List<Movie> getAllMovies(){
		return movieRepository.findAll();
	}


	@GetMapping("/getmovies/{id}")
	public ResponseEntity<List<Movie>> getMovieById(@PathVariable Long id) {
		List<Movie> movies = movieRepository.findAll();
		System.out.println(movies);
		// return ResponseEntity.ok(movies);
		// Set<User> usersid = new HashSet<User>();
		
		List<Movie> selectedmovies = new ArrayList<Movie>();
		
		movies.forEach(data -> {
			Set<User> usersid = data.getUser();
			usersid.forEach(userdata -> {
				if(id == userdata.getId()) {
					selectedmovies.add(data);
				}
			});	
		});
		
		return ResponseEntity.ok(selectedmovies);
	}
	
	@GetMapping("/getuser/{id}")
	public ResponseEntity<List<User>> getUserById(@PathVariable Long id) {
		List<User> users = userRepository.findAll();
		return ResponseEntity.ok(users);
}
}
