package pay.payment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pay.payment.domain.AuthData;

import javax.persistence.EntityManager;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthRepository {

    private final EntityManager em;

    @Transactional
    public void saveToken(AuthData authData) {
        em.persist(authData);
    }

    public AuthData findUser(String userId) {
        return em.find(AuthData.class, userId);
    }
}
