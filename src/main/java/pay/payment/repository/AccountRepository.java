package pay.payment.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pay.payment.domain.AccountList;

import javax.persistence.EntityManager;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountRepository {

    private final EntityManager em;

    @Transactional
    public void saveAccount(AccountList accountList){
        em.persist(accountList);
    }

    public List<AccountList> findAccount(String userId){
        return em.createQuery("select a from AccountList a", AccountList.class)
                .getResultList();
    }

}
