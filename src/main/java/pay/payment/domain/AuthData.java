package pay.payment.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter @Setter
public class AuthData {

    @Id @Column(name = "user_id")
    private String id;

    @Lob
    private String access_token;
    @Lob
    private String refresh_token;
    private String user_seq_no;

    @Lob
    private String oob_access_token;
}
