package tr.com.huseyinaydin.admin.service;

import java.util.Set;

public record AdminUserData(Long id, String firstName, String lastName, String email, boolean active, Set<String> roles) {
}
