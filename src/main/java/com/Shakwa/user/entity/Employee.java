package com.Shakwa.user.entity;

import com.Shakwa.user.Enum.GovernmentAgencyType;
import com.Shakwa.user.Enum.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "employees")
public class Employee extends BaseUser {

    @Override
    protected String getSequenceName() {
        return "employees_id_seq";
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "employee_permissions",
            joinColumns = @JoinColumn(name = "employee_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> additionalPermissions = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "government_agency", nullable = false)
    private GovernmentAgencyType governmentAgency;

    private String phoneNumber;

    private LocalDate dateOfHire;

    @Override
    public List<GrantedAuthority> getAuthorities() {
        Set<GrantedAuthority> authorities = new HashSet<>();

        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName()));

        authorities.addAll(role.getPermissions().stream()
                .map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toSet()));

        authorities.addAll(additionalPermissions.stream()
                .map(p -> new SimpleGrantedAuthority(p.getName()))
                .collect(Collectors.toSet()));

        return new ArrayList<>(authorities);
    }

    @Override
    public boolean isEnabled() {
        return status != null && status == UserStatus.ACTIVE;
    }
}
