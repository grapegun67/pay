package pay.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pay.payment.domain.AuthData;
import pay.payment.repository.AuthRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;

    public void saveAuth(AuthData authData) {
        authRepository.saveToken(authData);
    }

    public AuthData findClientAuth(String user_id) {
        return authRepository.findUser(user_id);
    }

}
