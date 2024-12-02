package ch.admin.bit.jeap.postgresql.aws.infrastructure.persistence;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PACKAGE) // for Builder
@NoArgsConstructor // for JPA
@ToString
@Entity
public class Person {

    @Id
    @NonNull
    private Integer id;

    private String firstName;
    private String lastName;

}
