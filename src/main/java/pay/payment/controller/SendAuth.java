package pay.payment.controller;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import pay.payment.WebClientConfig;
import pay.payment.domain.ClientAuthData;
import pay.payment.repository.HandleAuthRepository;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SendAuth {

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(WebClientConfig.class);
    WebClient webClient = (WebClient) ac.getBean("webClient");

    private final HandleAuthRepository handleAuthRepository;

    @GetMapping("/auth-function")
    public Mono<ResponseEntity<String>> authFunction(String tmp)    {

        Mono<ResponseEntity<String>> responseEntityMono = webClient.get()
                .uri("/oauth/2.0/authorize?response_type=code&client_id=7d97bf1d-fafd-407a-8967-678b88898e9f&redirect_uri=http://localhost:8080/auth-return&scope=login inquiry&state=9876543245671234utrg986fff235245&auth_type=0")
                .retrieve().toEntity(String.class);
        //toEntity에서 body로 바꾸면 헤더까지는 굳이 response 안해도될지도

       return responseEntityMono;
    }

    @GetMapping("/auth-return")
    public String getAuth(@ModelAttribute AuthClass authClass){

        log.info("gun_result1: {}, {}, {}", authClass.getCode(), authClass.getScope(), authClass.getState());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", authClass.getCode());
        formData.add("client_id", "7d97bf1d-fafd-407a-8967-678b88898e9f");
        formData.add("client_secret", "8a81c797-b6d1-4138-aeb9-3de495fa66ae");
        formData.add("redirect_uri", "http://localhost:8080/auth-return");
        formData.add("grant_type", "authorization_code");

        TokenClass tokenClass = webClient.post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve().bodyToMono(TokenClass.class).block();

        //토큰 저장
        ClientAuthData clientAuthData = new ClientAuthData();
        clientAuthData.setId("7d97bf1d-fafd-407a-8967-678b88898e9f");
        clientAuthData.setAccess_token(tokenClass.access_token);
        clientAuthData.setRefresh_token(tokenClass.refresh_token);
        clientAuthData.setUser_seq_no(tokenClass.user_seq_no);

        handleAuthRepository.saveToken(clientAuthData);

        log.info("gun_result2: {}, {}, {}, {}, {}, {}", tokenClass.access_token, tokenClass.refresh_token, tokenClass.token_type, tokenClass.expires_in, tokenClass.user_seq_no, tokenClass.scope);

        //계좌 조회



        return "ok";
    }

    @Getter @Setter
    static class AuthClass {
        private String code;
        private String scope;
        private String state;
    }

    @Getter @Setter
    static class TokenClass {
        private String access_token;
        private String token_type;
        private String refresh_token;
        private String expires_in;
        private String scope;
        private String user_seq_no;
    }

}