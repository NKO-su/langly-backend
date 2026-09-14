# Spring Data JPA

## Repository

`UserRepository` được khai báo bằng interface:

```java
public interface UserRepository extends JpaRepository<User, Long>