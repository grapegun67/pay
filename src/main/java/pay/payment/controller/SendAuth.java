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
import pay.payment.domain.AuthData;
import pay.payment.domain.AccountList;
import pay.payment.repository.AccountRepository;
import pay.payment.repository.AuthRepository;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SendAuth {

    private String CLIENT_ID = "****";
    private String CLIENT_SECRET = "****";

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(WebClientConfig.class);
    WebClient webClient = (WebClient) ac.getBean("webClient");

    private final AuthRepository authRepository;
    private final AccountRepository accountRepository;

    @GetMapping("/auth/gate")
    public Mono<ResponseEntity<String>> authGate() {

        MultiValueMap<String, String > params = new LinkedMultiValueMap<>();
        params.add("response_type", "code");
        params.add("client_id", CLIENT_ID);
        params.add("redirect_uri", "http://localhost:8080/auth/gate/main");
        params.add("scope", "login inquiry transfer");
        params.add("state", "9876543245671234utag986fff235245");
        params.add("auth_type", "0");

        // toEntity에서 body로 바꾸면 헤더까지는 굳이 response 안해도될지도
        Mono<ResponseEntity<String>> responseEntityMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/oauth/2.0/authorize")
                        .queryParams(params).build()).retrieve().toEntity(String.class);

       return responseEntityMono;
    }

    @GetMapping("/auth/gate/main")
    public String getMain(@ModelAttribute AuthClass authClass) {

        log.info("ktfc_result1: {}, {}, {}", authClass.getCode(), authClass.getScope(), authClass.getState());

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        AuthData user = authRepository.findUser(CLIENT_ID);

        /* 신규 요청 */
        if (user == null) {
            log.info("debug_new register");
            user = new AuthData();
            formData.add("code", authClass.getCode());
            formData.add("client_id", CLIENT_ID);
            formData.add("client_secret", CLIENT_SECRET);
            formData.add("redirect_uri", "http://localhost:8080/auth/gate/main");
            formData.add("grant_type", "authorization_code");
        }
        /* 토큰 갱신 요청                         */
        /* 사실 반드시 갱신할 필요는 없는데          */
        else {
            log.info("debug_update register");
            formData.add("client_id", CLIENT_ID);
            formData.add("client_secret", CLIENT_SECRET);
            formData.add("refresh_token", user.getRefresh_token());
            formData.add("scope", "login inquiry transfer");
            formData.add("grant_type", "refresh_token");
        }

        TokenClass tokenClass = webClient.post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve().bodyToMono(TokenClass.class).block();

        //토큰 저장
        user.setId(CLIENT_ID);
        user.setAccess_token(tokenClass.access_token);
        user.setRefresh_token(tokenClass.refresh_token);
        user.setUser_seq_no(tokenClass.user_seq_no);

        if (tokenClass != null) {
            log.info("debug no null");
            authRepository.saveToken(user);
        }
        else {
            return "no okay";
        }

        log.info("ktfc_result2: {}, {}, {}, {}, {}, {}", tokenClass.access_token, tokenClass.refresh_token, tokenClass.token_type, tokenClass.expires_in, tokenClass.user_seq_no, tokenClass.scope);

        return "ok";
    }

    @GetMapping("/account/list")
    public String GetAccountList () {
        AuthData user = authRepository.findUser(CLIENT_ID);
        if (user == null)
            return "no okay";

        String bear = "Bearer " + user.getAccess_token();

        //계좌 조회
        UserAccountInfo accountInfo = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("v2.0/user/me")
                        .queryParam("user_seq_no", user.getUser_seq_no()).build()).header("Authorization", bear).retrieve().bodyToMono(UserAccountInfo.class).block();

        log.info("ktfc_result3 {}, {}, {}", accountInfo.res_cnt, accountInfo.user_name, accountInfo.api_tran_id);

        List<UserAccountInfoList> res_list = accountInfo.getRes_list();
        AccountList accountList = new AccountList();

        /* 새로운 fintech 번호만 넣는 것으로 수정해야함 */
        for (UserAccountInfoList list : res_list) {
            accountList.setUser_id(CLIENT_ID);
            accountList.setFintech_use_num(list.fintech_use_num);
            log.info("ktfc_result4 {}, {}, {}", list.fintech_use_num, list.account_alias, list.bank_name);
        }

        accountRepository.saveAccount(accountList);

        return "okay";
    }

    @GetMapping("/balance")
    public String getBalance() {

        AuthData user = authRepository.findUser(CLIENT_ID);
        if (user == null)
            return "no okay1";

        List<AccountList> accountList = accountRepository.findAccount(CLIENT_ID);
        if (accountList == null)
            return "no okay2";

        String bear = "Bearer " + user.getAccess_token();

        // 계좌 잔액 조회
        // 이런 날짜까지도 에러처리를 해야하네. 에러처리가 굉장히 세심해야되네... 그냥 막실행하네
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tran_id = "M202202182U" + getRandomStr(9);
        log.info("tran_debug: {}", tran_id);

        MultiValueMap<String, String> params2 = new LinkedMultiValueMap<>();
        params2.add("bank_tran_id", tran_id);
        params2.add("fintech_use_num", accountList.get(0).getFintech_use_num());
        params2.add("tran_dtime", dateTime);

        AccountBalance balance = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("v2.0/account/balance/fin_num")
                        .queryParams(params2).build()).header("Authorization", bear).retrieve().bodyToMono(AccountBalance.class).block();

        log.info("ktfc_result6 {}, {}, {}, {}", balance.bank_name, balance.balance_amt, balance.product_name, balance.bank_rsp_message);

        return "okay";
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
        private String expires_in;
        private String refresh_token;

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

    @Getter @Setter
    static class AccountBalance {
        private String api_tran_id;
        private String api_tran_dtm;
        private String rsp_code;
        private String rsp_message;
        private String bank_tran_id;
        private String bank_tran_date;
        private String bank_code_tran;
        private String bank_rsp_code;
        private String bank_rsp_message;
        private String bank_name;
        private String savings_bank_name;
        private String fintech_use_num;
        private String balance_amt;
        private String available_amt;
        private String account_type;
        private String product_name;
        private String account_issue_date;
        private String maturity_date;
        private String last_tran_date;
    }


    public static String getRandomStr(int size) {
        if(size > 0) {
            char[] tmp = new char[size];
            for(int i=0; i<tmp.length; i++) {
                int div = (int) Math.floor( Math.random() * 2 );

                if(div == 0) { // 0이면 숫자로
                    tmp[i] = (char) (Math.random() * 10 + '0') ;
                }else { //1이면 알파벳
                    tmp[i] = (char) (Math.random() * 26 + 'A') ;
                }
            }
            return new String(tmp);
        }
        return "ERROR : Size is required.";
    }

}