package kz.home.RelaySmartSystems.service;

import kz.home.RelaySmartSystems.model.entity.User;
import kz.home.RelaySmartSystems.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User addUser(UUID uuid, String username, String firstname, String lastname, String email) {
        User user = new User();
        user.setUuid(uuid);
        user.setUsername(username);
        user.setFirstName(firstname);
        user.setLastName(lastname);
        user.setEmail(email);
        return userRepository.save(user);
    }

    public void saveUser(User user) {
        userRepository.save(user);
    }

    public User findByUuid(UUID uuid) {
        return userRepository.findById(uuid).orElse(null);
    }
}
