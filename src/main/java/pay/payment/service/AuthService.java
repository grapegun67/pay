package pay.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pay.payment.domain.ClientAuthData;
import pay.payment.repository.HandleAuthRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final HandleAuthRepository handleAuthRepository;

    public void saveAuth(ClientAuthData clientAuthData) {
        handleAuthRepository.saveToken(clientAuthData);
    }

    public ClientAuthData findClientAuth(String user_id) {
        return handleAuthRepository.findToken(user_id);
    }

}
