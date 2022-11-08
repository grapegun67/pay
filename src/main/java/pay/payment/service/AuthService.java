package pay.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pay.payment.domain.ClientAuthData;
import pay.payment.repository.ClientAuthRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientAuthRepository clientAuthRepository;

    public void saveAuth(ClientAuthData clientAuthData) {
        clientAuthRepository.saveToken(clientAuthData);
    }

    public ClientAuthData findClientAuth(String user_id) {
        return clientAuthRepository.findUser(user_id);
    }

}
