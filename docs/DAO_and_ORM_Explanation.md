# DAO Pattern and ORM Implementation in Shakwa Project

## Table of Contents
1. [DAO Pattern Overview](#dao-pattern-overview)
2. [ORM Overview](#orm-overview)
3. [DAO Implementation in This Project](#dao-implementation-in-this-project)
4. [ORM Implementation in This Project](#orm-implementation-in-this-project)
5. [How DAO and ORM Work Together](#how-dao-and-orm-work-together)
6. [Examples from Codebase](#examples-from-codebase)

---

## DAO Pattern Overview

### What is DAO?

**DAO (Data Access Object)** is a design pattern that provides an abstract interface to a database or persistence mechanism. It separates the business logic from the data access logic.

### Key Benefits:
- **Separation of Concerns**: Business logic is separated from data access logic
- **Abstraction**: Hides database implementation details from business layer
- **Testability**: Easy to mock data access layer for unit testing
- **Flexibility**: Easy to switch between different data sources without changing business logic

### Traditional DAO Pattern:
```java
public interface UserDAO {
    User findById(Long id);
    List<User> findAll();
    void save(User user);
    void update(User user);
    void delete(Long id);
}
```

---

## ORM Overview

### What is ORM?

**ORM (Object-Relational Mapping)** is a programming technique that converts data between incompatible type systems using object-oriented programming languages. It creates a "virtual object database" that can be used from within the programming language.

### Key Concept:
Instead of writing SQL queries manually, you work with **objects** in your code, and the ORM framework automatically converts these objects to database records and vice versa.

```
Java Object (Entity)  ←→  ORM Framework (Hibernate/JPA)  ←→  Database Table (SQL)
```

### Benefits:
- **Productivity**: No need to write repetitive SQL code
- **Type Safety**: Work with Java objects instead of raw SQL
- **Maintainability**: Changes to database structure are easier to manage
- **Relationships**: Easy to define and navigate entity relationships
- **Caching**: Built-in caching mechanisms for better performance

### How ORM Works:

1. **Entity Classes**: Java classes that represent database tables
   ```java
   @Entity
   @Table(name = "users")
   public class User {
       @Id
       @GeneratedValue
       private Long id;
       private String name;
   }
   ```

2. **Annotations**: Metadata that tells ORM how to map objects to tables
   - `@Entity`: Marks class as a database entity
   - `@Table`: Maps to database table name
   - `@Column`: Maps field to database column
   - `@Id`: Primary key
   - `@ManyToOne`, `@OneToMany`: Relationships

3. **Automatic SQL Generation**: ORM generates SQL queries automatically based on operations

---

## DAO Implementation in This Project

### Technology Stack

This project uses **Spring Data JPA**, which is a higher-level abstraction that combines:
- **DAO Pattern**: Through Repository interfaces
- **ORM**: Through JPA/Hibernate
- **Automatic Implementation**: Spring generates DAO implementation automatically

### Repository Interfaces (DAO Layer)

All repositories are located in:
- `com.Shakwa.user.repository.*`
- `com.Shakwa.complaint.repository.*`

### Repository Structure

#### 1. **User Management Repositories**

**UserRepository.java**
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

**EmployeeRepository.java**
```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    List<Employee> findByGovernmentAgency(GovernmentAgencyType governmentAgency);
    
    @Lock(value = PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Employee e WHERE e.email = :email")
    Optional<Employee> findByEmailWithGovernmentAgency(@Param("email") String email);
}
```

**CitizenRepo.java**
```java
@Repository
public interface CitizenRepo extends JpaRepository<Citizen, Long> {
    Optional<Citizen> findByEmail(String email);
    
    @Query("SELECT c FROM Citizen c WHERE CONCAT(c.firstName, ' ', c.lastName) = :name")
    Optional<Citizen> findByName(@Param("name") String name);
}
```

#### 2. **Complaint Management Repositories**

**ComplaintRepository.java** - Most comprehensive DAO example
```java
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long>, 
                                           JpaSpecificationExecutor<Complaint> {
    
    // Derived Query Methods (Spring generates SQL automatically)
    Page<Complaint> findByCitizenId(Long citizenId, Pageable pageable);
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);
    
    // Custom JPQL Queries
    @Query("SELECT c FROM Complaint c WHERE c.id = :id")
    Optional<Complaint> findByIdForUpdate(@Param("id") Long id);
    
    // Pessimistic Locking for Concurrency Control
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Complaint c WHERE c.id = :id")
    Optional<Complaint> findByIdForUpdate(@Param("id") Long id);
    
    // Caching
    @Cacheable(value = "complaintLists", key = "'complaint:id:' + #id")
    Optional<Complaint> findById(Long id);
    
    @CacheEvict(value = "complaintLists", allEntries = true)
    <S extends Complaint> S save(S entity);
}
```

### Key Features of DAO Implementation:

1. **Inheritance from JpaRepository**: Provides CRUD operations automatically
   - `save()`, `findById()`, `findAll()`, `delete()`, `count()`, etc.

2. **Method Naming Conventions**: Spring generates queries from method names
   - `findByCitizenId()` → `SELECT * FROM complaints WHERE citizen_id = ?`
   - `findByStatusAndCitizenId()` → `SELECT * FROM complaints WHERE status = ? AND citizen_id = ?`

3. **Custom Queries**: Using `@Query` annotation for complex queries
   ```java
   @Query("SELECT c FROM Complaint c WHERE c.id = :id")
   Optional<Complaint> findById(@Param("id") Long id);
   ```

4. **Pagination Support**: Built-in pagination with `Pageable`
   ```java
   Page<Complaint> findByCitizenId(Long citizenId, Pageable pageable);
   ```

5. **Caching**: Repository-level caching with `@Cacheable` and `@CacheEvict`

6. **Locking**: Pessimistic locking for concurrent access control

---

## ORM Implementation in This Project

### Technology Stack

- **JPA (Jakarta Persistence API)**: The specification/standard
- **Hibernate**: The actual ORM implementation (default in Spring Boot)
- **Database**: PostgreSQL

### Configuration (application.properties)

```properties
# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true
```

### Entity Classes (ORM Mapping)

Entities are located in:
- `com.Shakwa.user.entity.*`
- `com.Shakwa.complaint.entity.*`

### Entity Hierarchy

The project uses inheritance in ORM mapping:

```
BaseEntity (abstract)
    └── BaseIdEntity
    └── AuditedEntity
        └── BaseUser (@MappedSuperclass)
            └── User (@Entity, @Inheritance JOINED)
                ├── Citizen (@Entity)
                └── Employee (@Entity)
        └── Complaint (@Entity)
```

### Example Entity: Complaint.java

```java
@Entity
@Table(name = "complaints")
public class Complaint extends AuditedEntity {
    
    // Primary Key with Sequence
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "complaint_seq")
    @SequenceGenerator(name = "complaint_seq", sequenceName = "complaint_id_seq")
    private Long id;
    
    // Enum Mapping
    @Enumerated(EnumType.STRING)
    @Column(name = "complaint_type", nullable = false)
    private ComplaintType complaintType;
    
    // Text Column
    @Column(name = "description", columnDefinition = "TEXT", nullable = false)
    private String description;
    
    // Optimistic Locking (Version Control)
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    
    // Many-to-One Relationship (Lazy Loading)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;
    
    // One-to-Many Relationship (Cascade Operations)
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComplaintAttachment> attachments = new ArrayList<>();
    
    // Many-to-One Relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by")
    private Employee respondedBy;
}
```

### Example Entity: User.java (Inheritance Example)

```java
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends BaseUser {
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_permissions",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> additionalPermissions = new HashSet<>();
}
```

### Key ORM Annotations Used:

1. **@Entity**: Marks class as a persistent entity (database table)
2. **@Table**: Specifies the database table name
3. **@Id**: Marks field as primary key
4. **@GeneratedValue**: Auto-generates primary key values
5. **@Column**: Maps field to database column (optional naming, constraints)
6. **@Enumerated**: Maps Java enum to database column
7. **@Version**: Enables optimistic locking
8. **@ManyToOne**: Many-to-one relationship mapping
9. **@OneToMany**: One-to-many relationship mapping
10. **@ManyToMany**: Many-to-many relationship mapping
11. **@JoinColumn**: Specifies foreign key column
12. **@JoinTable**: Specifies join table for many-to-many
13. **@MappedSuperclass**: Base class that is not an entity itself
14. **@Inheritance**: Defines inheritance strategy (JOINED, SINGLE_TABLE, TABLE_PER_CLASS)
15. **@FetchType**: Controls loading strategy (LAZY, EAGER)

### Relationship Types:

1. **Many-to-One** (`@ManyToOne`):
   ```java
   // Many Complaints belong to One Citizen
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "citizen_id")
   private Citizen citizen;
   ```

2. **One-to-Many** (`@OneToMany`):
   ```java
   // One Complaint has Many Attachments
   @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL)
   private List<ComplaintAttachment> attachments;
   ```

3. **Many-to-Many** (`@ManyToMany`):
   ```java
   // Many Users have Many Permissions
   @ManyToMany
   @JoinTable(name = "user_permissions",
              joinColumns = @JoinColumn(name = "user_id"),
              inverseJoinColumns = @JoinColumn(name = "permission_id"))
   private Set<Permission> additionalPermissions;
   ```

### Fetch Strategies:

1. **EAGER Loading** (`FetchType.EAGER`):
   - Related entities are loaded immediately with the parent
   - Used for frequently accessed relationships
   - Example: User's Role (always needed for authorization)

2. **LAZY Loading** (`FetchType.LAZY`):
   - Related entities are loaded on-demand (when accessed)
   - Better performance, avoids unnecessary data loading
   - Example: Complaint's Citizen (not always needed)

### Advanced ORM Features Used:

1. **Auditing** (Automatic tracking of creation/modification):
   ```java
   @EntityListeners(AuditingEntityListener.class)
   public class AuditedEntity {
       @CreatedDate
       private LocalDateTime createdAt;
       
       @LastModifiedDate
       private LocalDateTime updatedAt;
       
       @CreatedBy
       private Long createdBy;
   }
   ```

2. **Optimistic Locking** (Version control):
   ```java
   @Version
   private Long version;
   ```

3. **Pessimistic Locking** (Repository level):
   ```java
   @Lock(LockModeType.PESSIMISTIC_WRITE)
   Optional<Complaint> findByIdForUpdate(Long id);
   ```

---

## How DAO and ORM Work Together

### Architecture Flow:

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller Layer                         │
│                   (REST Endpoints)                          │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│                 (Business Logic)                            │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│               DAO Layer (Repositories)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  UserRepository                                       │  │
│  │  ComplaintRepository                                  │  │
│  │  EmployeeRepository                                   │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              ORM Layer (JPA/Hibernate)                      │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Entity Manager                                       │  │
│  │  - Manages Entity Lifecycle                          │  │
│  │  - Generates SQL Queries                             │  │
│  │  - Handles Relationships                             │  │
│  │  - Caching                                            │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Entity Classes (Java Objects)                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  @Entity User                                         │  │
│  │  @Entity Complaint                                    │  │
│  │  @Entity Citizen                                      │  │
│  └──────────────────────────────────────────────────────┘  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Database (PostgreSQL)                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Tables: users, complaints, citizens, etc.           │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### Step-by-Step Example:

**1. Controller receives request:**
```java
@PostMapping("/complaints")
public ComplaintDTOResponse createComplaint(@RequestBody ComplaintDTORequest dto) {
    return complaintService.createComplaint(dto);
}
```

**2. Service uses DAO (Repository):**
```java
@Service
public class ComplaintService {
    private final ComplaintRepository complaintRepository; // DAO injected
    
    public ComplaintDTOResponse createComplaint(ComplaintDTORequest dto) {
        // Create entity object (ORM entity)
        Complaint complaint = complaintMapper.toEntity(dto);
        
        // Use DAO to save (ORM converts object to SQL)
        complaint = complaintRepository.save(complaint);
        
        return complaintMapper.toResponse(complaint);
    }
}
```

**3. ORM (Hibernate) automatically:**
- Converts `Complaint` Java object to SQL INSERT statement
- Executes SQL against PostgreSQL database
- Maps database result back to Java object
- Handles relationships, foreign keys, sequences, etc.

**4. What actually happens:**

```java
// Developer writes this:
complaintRepository.save(complaint);

// Hibernate generates and executes this SQL:
INSERT INTO complaints (
    complaint_type, governorate, government_agency, 
    location, description, status, citizen_id, 
    created_at, version
) VALUES (
    'COMPLAINT', 'DAMASCUS', 'MINISTRY_OF_HEALTH',
    'Location text', 'Description text', 'PENDING',
    123, NOW(), 1
);
```

---

## Examples from Codebase

### Example 1: Simple DAO Operation

**Repository (DAO):**
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
```

**Service Usage:**
```java
@Service
public class UserService {
    private final UserRepository userRepository; // DAO
    
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
```

**ORM Mapping:**
```java
@Entity
@Table(name = "users")
public class User extends BaseUser {
    @Column(unique = true, nullable = false)
    private String email;
}
```

### Example 2: Complex DAO with Relationships

**Repository:**
```java
@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    @Cacheable(value = "complaintLists", key = "'complaints:citizen:' + #citizenId")
    Page<Complaint> findByCitizenId(Long citizenId, Pageable pageable);
}
```

**Entity with Relationships:**
```java
@Entity
public class Complaint extends AuditedEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;
    
    @OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL)
    private List<ComplaintAttachment> attachments;
}
```

**Service Usage:**
```java
public Page<ComplaintDTOResponse> getComplaintsByCitizen(Long citizenId, Pageable pageable) {
    Page<Complaint> complaints = complaintRepository.findByCitizenId(citizenId, pageable);
    return complaints.map(complaintMapper::toResponse);
}
```

### Example 3: ORM Inheritance

**Base Entity:**
```java
@MappedSuperclass
public abstract class BaseUser {
    @Column(nullable = false)
    protected String firstName;
    
    @Column(nullable = false)
    protected String lastName;
    
    @Column(unique = true, nullable = false)
    protected String email;
}
```

**Child Entity:**
```java
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends BaseUser {
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;
}
```

**Further Inheritance:**
```java
@Entity
@Table(name = "citizens")
public class Citizen extends User {
    // Citizen-specific fields
}
```

**Database Schema:**
- `users` table: Base User fields
- `citizens` table: Citizen-specific fields + foreign key to `users`

---

## Summary

### DAO Pattern in This Project:
- ✅ **Implemented through Spring Data JPA Repositories**
- ✅ **Location**: `repository` packages
- ✅ **Features**: CRUD operations, custom queries, pagination, caching, locking
- ✅ **Abstraction**: Hides database access details from service layer

### ORM in This Project:
- ✅ **Technology**: JPA (Jakarta Persistence API) with Hibernate
- ✅ **Entity Classes**: Located in `entity` packages
- ✅ **Features**: Object-to-table mapping, relationships, inheritance, auditing, locking
- ✅ **Configuration**: Hibernate with PostgreSQL dialect

### Benefits Achieved:
1. **Separation of Concerns**: Business logic separated from data access
2. **Productivity**: No manual SQL writing for common operations
3. **Type Safety**: Work with Java objects instead of raw SQL
4. **Maintainability**: Centralized data access logic
5. **Performance**: Built-in caching and optimized queries
6. **Relationships**: Easy navigation between related entities
7. **Concurrency**: Optimistic and pessimistic locking support

---

## Key Takeaways

1. **DAO = Repository Interfaces**: Spring Data JPA repositories act as DAOs
2. **ORM = Entity Classes**: JPA/Hibernate maps Java objects to database tables
3. **Together**: Repositories (DAO) use ORM to interact with database through entities
4. **No Manual SQL**: Most database operations are handled automatically
5. **Annotations**: Metadata that tells ORM how to map objects to tables

---

*Document created: 2025*
*Project: Shakwa Complaint Management System*

