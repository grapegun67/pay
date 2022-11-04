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

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SendAuth {

    private String CLIENT_ID = "7d97bf1d-fafd-407a-8967-678b88898e9f";
    private String CLIENT_SECRET = "8a81c797-b6d1-4138-aeb9-3de495fa66ae";

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(WebClientConfig.class);
    WebClient webClient = (WebClient) ac.getBean("webClient");

    private final HandleAuthRepository handleAuthRepository;

    @GetMapping("/auth-function")
    public Mono<ResponseEntity<String>> authFunction() {

        MultiValueMap<String, String > params = new LinkedMultiValueMap<>();
        params.add("response_type", "code");
        params.add("client_id", CLIENT_ID);
        params.add("redirect_uri", "http://localhost:8080/auth-return");
        params.add("scope", "login inquiry");
        params.add("state", "9876543245671234utrg986fff235245");
        params.add("auth_type", "0");

        // toEntity에서 body로 바꾸면 헤더까지는 굳이 response 안해도될지도
        Mono<ResponseEntity<String>> responseEntityMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/oauth/2.0/authorize")
                        .queryParams(params).build()).retrieve().toEntity(String.class);

       return responseEntityMono;
    }

    @GetMapping("/auth-return")
    public String getAuth(@ModelAttribute AuthClass authClass){

        log.info("ktfc_result1: {}, {}, {}", authClass.getCode(), authClass.getScope(), authClass.getState());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("code", authClass.getCode());
        formData.add("client_id", CLIENT_ID);
        formData.add("client_secret", CLIENT_SECRET);
        formData.add("redirect_uri", "http://localhost:8080/auth-return");
        formData.add("grant_type", "authorization_code");

        TokenClass tokenClass = webClient.post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve().bodyToMono(TokenClass.class).block();

        //토큰 저장
        ClientAuthData clientAuthData = new ClientAuthData();
        clientAuthData.setId(CLIENT_ID);
        clientAuthData.setAccess_token(tokenClass.access_token);
        clientAuthData.setRefresh_token(tokenClass.refresh_token);
        clientAuthData.setUser_seq_no(tokenClass.user_seq_no);

        handleAuthRepository.saveToken(clientAuthData);

        log.info("ktfc_result2: {}, {}, {}, {}, {}, {}", tokenClass.access_token, tokenClass.refresh_token, tokenClass.token_type, tokenClass.expires_in, tokenClass.user_seq_no, tokenClass.scope);

        String bear = "Bearer " + tokenClass.access_token;

        //계좌 조회
        UserAccountInfo accountInfo = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("v2.0/user/me")
                        .queryParam("user_seq_no", tokenClass.user_seq_no).build()).header("Authorization", bear).retrieve().bodyToMono(UserAccountInfo.class).block();

        log.info("ktfc_result3 {}, {}, {}", accountInfo.res_cnt, accountInfo.user_name, accountInfo.api_tran_id);

        List<UserAccountInfoList> res_list = accountInfo.getRes_list();
        for (UserAccountInfoList list : res_list) {
            log.info("ktfc_result4 {}, {}, {}", list.fintech_use_num, list.account_alias, list.bank_name);
        }

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

    @Getter @Setter
    static class UserAccountInfo {
        private String api_tran_id;
        private String rsp_code;
        private String rsp_message;
        private String api_tran_dtm;
        private String user_seq_no;
        private String user_ci;
        private String user_name;
        private String res_cnt;
        private List<UserAccountInfoList> res_list;
    }

    @Getter @Setter
    static class UserAccountInfoList {
        private String  fintech_use_num;
        private String  account_alias;
        private String  bank_code_std;
        private String  bank_code_sub;
        private String  bank_name;
        private String  account_num_masked;
        private String  account_holder_name;
        private String  account_holder_type;
        private String  inquiry_agree_yn;
        private String  inquiry_agree_dtime;
        private String  transfer_agree_yn;
        private String  transfer_agree_dtime;
        private String  payer_num;
        private String  savings_bank_name;
        private String  account_seq;
        private String  account_type;
    }

}