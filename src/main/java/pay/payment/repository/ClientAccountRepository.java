package pay.payment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;

@Repository
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClientAccountRepository {

    private final EntityManager em;

    public void saveAccount(){

    }


}
