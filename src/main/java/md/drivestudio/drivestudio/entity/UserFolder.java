package md.drivestudio.drivestudio.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import md.drivestudio.drivestudio.model.User;

@Entity
@Getter
@Setter
public class UserFolder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name; // Numele mapei (ex: "poze", "video")

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;
}
