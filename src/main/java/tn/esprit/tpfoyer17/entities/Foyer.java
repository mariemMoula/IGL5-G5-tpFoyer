package tn.esprit.tpfoyer17.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Foyer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long idFoyer;

    private String nomFoyer;

    private long capaciteFoyer;

    @ToString.Exclude
    @OneToOne(mappedBy = "foyer")
    @JsonIgnore
    private Universite universite;

    @JsonIgnore
    @ToString.Exclude
    @OneToMany(mappedBy = "foyer",cascade = CascadeType.ALL)
    private Set<Bloc> blocs;
}
