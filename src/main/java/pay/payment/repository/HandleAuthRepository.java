package pay.payment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pay.payment.domain.ClientAuthData;

import javax.persistence.EntityManager;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HandleAuthRepository {

    private final EntityManager em;

    @Transactional
    public void saveToken(ClientAuthData clientAuthData) {
        em.persist(clientAuthData);
    }

    public ClientAuthData findUser(String userId) {
        return em.find(ClientAuthData.class, userId);
    }
}
