package pay.payment.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class SendAuth {

    private String CLIENT_ID = "****";
    private String CLIENT_SECRET = "****";
    private String SEQN = "***U";

    AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(WebClientConfig.class);
    WebClient webClient = (WebClient) ac.getBean("webClient");

    private final AuthRepository authRepository;
    private final AccountRepository accountRepository;

    @GetMapping("/auth/gate")
    public Mono<ResponseEntity<String>> authGate() {

        MultiValueMap<String, String > params = new LinkedMultiValueMap<>();
        params.add("response_type", "code");
        params.add("client_id", CLIENT_ID);
        params.add("redirect_uri", "http://localhost:8080/auth/gate/new");
        params.add("scope", "login inquiry transfer");
        params.add("state", "9876543245671234utag986fff235245");
        params.add("auth_type", "0");

        // toEntity?????? body??? ????????? ??????????????? ?????? response ??????????????????
        Mono<ResponseEntity<String>> responseEntityMono = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/oauth/2.0/authorize")
                        .queryParams(params).build()).retrieve().toEntity(String.class);

       return responseEntityMono;
    }

    @GetMapping("/auth/gate/new")
    public String getMain(@ModelAttribute AuthClass authClass) {
        log.info("ktfc_result1: {}, {}, {}", authClass.getCode(), authClass.getScope(), authClass.getState());

        AuthData user = new AuthData();
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        /* ?????? ?????? */
        log.info("debug_new register");
        formData.add("code", authClass.getCode());
        formData.add("client_id", CLIENT_ID);
        formData.add("client_secret", CLIENT_SECRET);
        formData.add("redirect_uri", "http://localhost:8080/auth/gate/new");
        formData.add("grant_type", "authorization_code");

        TokenClass tokenClass = webClient.post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve().bodyToMono(TokenClass.class).block();

        //?????? ??????
        user.setId(CLIENT_ID);
        user.setAccess_token(tokenClass.access_token);
        user.setRefresh_token(tokenClass.refresh_token);
        user.setUser_seq_no(tokenClass.user_seq_no);

        if (tokenClass.access_token != null) {
            log.info("debug no null");
            authRepository.saveToken(user);
        }
        else {
            return "no okay";
        }

        log.info("ktfc_result2: {}, {}, {}, {}, {}, {}", tokenClass.access_token, tokenClass.refresh_token, tokenClass.token_type, tokenClass.expires_in, tokenClass.user_seq_no, tokenClass.scope);

        return "ok";
    }

    @GetMapping("/auth/gate/old")
    public String AuthGateOld() {
        log.info("debug_update register");

        AuthData user = authRepository.findUser(CLIENT_ID);
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();

        /* ?????? ?????? ??????                         */
        formData.add("client_id", CLIENT_ID);
        formData.add("client_secret", CLIENT_SECRET);
        formData.add("refresh_token", user.getRefresh_token());
        formData.add("scope", "login inquiry transfer");
        formData.add("grant_type", "refresh_token");

        //webclient ???????????? ????????? ????????? ??? ???????????????
        TokenClass tokenClass = webClient.post()
                .uri("/oauth/2.0/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve().bodyToMono(TokenClass.class).block();

        //?????? ??????
        user.setId(CLIENT_ID);
        user.setAccess_token(tokenClass.access_token);
        user.setRefresh_token(tokenClass.refresh_token);
        user.setUser_seq_no(tokenClass.user_seq_no);

        if (tokenClass.access_token != null) {
            log.info("debug no null");
            authRepository.saveToken(user);
        }
        else {
            return "no okay";
        }

        log.info("ktfc_result2: {}, {}, {}, {}, {}, {}", tokenClass.access_token, tokenClass.refresh_token, tokenClass.token_type, tokenClass.expires_in, tokenClass.user_seq_no, tokenClass.scope);
        return "okay";
    }

    @GetMapping("/account/list")
    public String GetAccountList () {
        AuthData user = authRepository.findUser(CLIENT_ID);
        if (user == null)
            return "no okay";

        String bear = "Bearer " + user.getAccess_token();

        //?????? ??????
        UserAccountInfo accountInfo = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("v2.0/user/me")
                        .queryParam("user_seq_no", user.getUser_seq_no()).build()).header("Authorization", bear).retrieve().bodyToMono(UserAccountInfo.class).block();

        log.info("ktfc_result3 {}, {}, {}", accountInfo.res_cnt, accountInfo.user_name, accountInfo.api_tran_id);

        List<UserAccountInfoList> res_list = accountInfo.getRes_list();
        AccountList accountList = new AccountList();

        /* ????????? fintech ????????? ?????? ????????? ??????????????? */
        for (UserAccountInfoList list : res_list) {
            accountList.setUser_id(CLIENT_ID);
            accountList.setFintech_use_num(list.fintech_use_num);
            log.info("ktfc_result4 {}, {}, {} {} {} {}", list.fintech_use_num, list.account_alias, list.bank_name, list.account_holder_name, list.account_num_masked, list.account_holder_name);
        }

        //???????????? ????????? ????????? ?????? ??? ??????????????????
        //??? ???????????? ??????????????? ?????????????????? ???????????? ??? ????????? ?????????
        accountRepository.saveAccount(accountList);

        return "okay";
    }

    @GetMapping("/account/balance")
    public String getBalance() {

        AuthData user = authRepository.findUser(CLIENT_ID);
        if (user == null)
            return "no okay1";

        List<AccountList> accountList = accountRepository.findAccount(CLIENT_ID);
        if (accountList == null)
            return "no okay2";

        String bear = "Bearer " + user.getAccess_token();

        //?????? ??????????????? ????????? ??? LocalDateTime??? ????????? ?????? ????????? ??? ????????????
        //?????? ??????????????? ??????????????? ????????????. ??????????????? ????????? ??????????????????... ?????? ???????????????
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tran_id = SEQN + getRandomStr(9);
        log.info("tran_debug: {}", tran_id);

        /* ?????? ???????????? ??? ???????????? ?????? ?????? */
        MultiValueMap<String, String> params2 = new LinkedMultiValueMap<>();
        params2.add("bank_tran_id", tran_id);
        params2.add("fintech_use_num", accountList.get(0).getFintech_use_num());
        params2.add("tran_dtime", dateTime);

        // ?????? ?????? ??????
        AccountBalance balance = webClient.get()
                .uri(uriBuilder -> uriBuilder.path("v2.0/account/balance/fin_num")
                        .queryParams(params2).build()).header("Authorization", bear).retrieve().bodyToMono(AccountBalance.class).block();

        log.info("ktfc_result6 {}, {}, {}, {}", balance.bank_name, balance.balance_amt, balance.product_name, balance.bank_rsp_message);

        return "okay";
    }

    @GetMapping("/account/withdraw")
    public String AccountWithdraw() throws JsonProcessingException {

        AuthData user = authRepository.findUser(CLIENT_ID);
        if (user == null)
            return "no okay1";

        List<AccountList> accountList = accountRepository.findAccount(CLIENT_ID);
        if (accountList == null)
            return "no okay2";

        String bear = "Bearer " + user.getAccess_token();

        //?????? ??????
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String tran_id = SEQN + getRandomStr(9);
        log.info("tran_debug: {}", tran_id);

        //??????????????? ????????????, ???????????? ????????? ???????????? ???????????? ???????????? ?????? JSON??? ???????????? ????????? ???????????? ?????? ??? ??????
        Map<String, String> params2 = new HashMap<>();

        /**
         * ???????????? ???????????? ??????
         * ??????????????? ??????????????? ???????????? ???
         * ??????????????? ???????????? ??????????????? ?????????, ??? ??? ??????????????? ??????????????? ???????????? ????????? ??????
         */
        params2.put("bank_tran_id", tran_id);
        params2.put("cntr_account_type", "N");
        params2.put("cntr_account_num", "100000000001");

        /* ?????? ?????? ?????? ??????*/
        params2.put("dps_print_content", "???????????????");
        params2.put("fintech_use_num", accountList.get(0).getFintech_use_num());
        params2.put("wd_print_content", "??????????????????");
        params2.put("tran_amt", "10001");
        params2.put("tran_dtime", dateTime);
        params2.put("req_client_name", "?????????");
        params2.put("req_client_bank_code", "020");
        params2.put("req_client_account_num", "987654321");
        params2.put("req_client_num", "1101015137");
        params2.put("transfer_purpose", "TR");

        /**
         * ????????? ?????? ??????????????? ??????????????? ?????????
         * ?????? ??? ???????????? ?????? ??? ?????????????????? ?????? ?????????????????? ???????????? ??? ????????? ????????? ?????? ?????? ??????
         * ???????????? ???????????? ??????(???TR???) ??? ??????(???ST???)??? ?????? ?????? ?????? ?????? ??????
         * (???, ?????? ?????????????????? ?????? ?????? ????????? ??????????????? ????????? ??? ?????? ????????????????????? ??? ????????? ???????????? ??????)
         */
        params2.put("recv_client_name", "????????????");
        params2.put("recv_client_bank_code", "022");
        params2.put("recv_client_account_num", "300000000001");

        //exception??? ???????????? ???????????? ???????????? ????????????
        ObjectMapper objectMapper = new ObjectMapper();
        String value = objectMapper.writeValueAsString(params2);
        log.info("json request: {}", value);

        //post json?????? I/O?????? string ?????????????????????. ?????? ??? ????????????
        //x-www-form-urlencoded ???????????? .bodyValue()??? ?????? ????????? ?????????????????????, json?????? ????????? string?????? ??????????????????
        String s1 = webClient.post()
                .uri("/v2.0/transfer/withdraw/fin_num")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", bear)
                .bodyValue(value)
                .retrieve().bodyToMono(String.class).block();

        /* json??? webclient??? ???????????? ??? ??? ?????? ?????? or ?????? ????????? ?????? ????????? ?????? */
        Map<String, String> returnJson = objectMapper.readValue(s1, new TypeReference<Map<String, String>>() {});

        log.info("bear {}", bear);

        log.info("{} {} -> $ {} $ -> {} {} ??????????????????", returnJson.get("account_num_masked"), returnJson.get("bank_name"), returnJson.get("tran_amt"), returnJson.get("dps_bank_name"), returnJson.get("dps_account_num_masked"));

        return "ok";
    }

    @GetMapping("/account/deposit")
    public String AccountDeposit() throws JsonProcessingException {

        ObjectMapper objectMapper = new ObjectMapper();
        MultiValueMap<String, String> bodyValue = new LinkedMultiValueMap<>();

        //token ?????? ??????
        AuthData user = authRepository.findUser(CLIENT_ID);
        if (user.getOob_access_token() == null) {
            bodyValue.add("client_id", CLIENT_ID);
            bodyValue.add("client_secret", CLIENT_SECRET);
            bodyValue.add("scope", "oob");
            bodyValue.add("grant_type", "client_credentials");

            String json = webClient.post()
                    .uri("/oauth/2.0/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(bodyValue)
                    .retrieve().bodyToMono(String.class).block();

            log.info("oob token {}", json);
            //???????????? ????????? ?????????
            Map<String, String> returnJson = objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});

            //oob token ??????
            user.setOob_access_token(returnJson.get("access_token"));
            authRepository.saveToken(user);
        }

        List<AccountList> account = accountRepository.findAccount(CLIENT_ID);

        //???????????? ?????? (withdraw??? ?????????????????? ?????????????????????)
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        WithdrawRequestSubList withdrawRequestSubList = new WithdrawRequestSubList();
        withdrawRequestSubList.setTran_no("1");
        withdrawRequestSubList.setBank_tran_id(SEQN + getRandomStr(9));
        withdrawRequestSubList.setFintech_use_num(account.get(0).getFintech_use_num());
        withdrawRequestSubList.setPrint_content("???????????????");
        withdrawRequestSubList.setTran_amt("10000");
        withdrawRequestSubList.setReq_client_name("?????????");
        withdrawRequestSubList.setReq_client_bank_code("020");
        withdrawRequestSubList.setReq_client_account_num("987654321");
        withdrawRequestSubList.setReq_client_num("1101015137");
        withdrawRequestSubList.setTransfer_purpose("TR");

        List<WithdrawRequestSubList> withdrawRequestSubListList = new ArrayList<>();
        withdrawRequestSubListList.add(withdrawRequestSubList);

        WithdrawRequestClass withdrawRequestClass = new WithdrawRequestClass();
        withdrawRequestClass.setCntr_account_type("N");
        withdrawRequestClass.setCntr_account_num("200000000001");
        withdrawRequestClass.setWd_pass_phrase("NONE");
        withdrawRequestClass.setWd_print_content("????????????");
        //??????????????? ?????? ??? ???????????? ????????? ???????????? ???????????? ?????????. ?????????????????? ????????? ?????????
        withdrawRequestClass.setName_check_option("off");
        withdrawRequestClass.setTran_dtime(dateTime);
        withdrawRequestClass.setReq_cnt("1");
        withdrawRequestClass.setReq_list(withdrawRequestSubListList);

        String value = objectMapper.writeValueAsString(withdrawRequestClass);
        log.info("json request: {}", value);

        String oob_bear = "Bearer " + user.getOob_access_token();

        String returnJson = webClient.post()
                .uri("/v2.0/transfer/deposit/fin_num")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", oob_bear)
                .bodyValue(value)
                .retrieve().bodyToMono(String.class).block();

        log.info("ret json: {}", returnJson);

        WithdrawResponseClass readValue = objectMapper.readValue(returnJson, WithdrawResponseClass.class);

        //json?????? ?????? ????????? ????????? ????????? ????????????. ????????? ??????????????? ?????????????????????...
        log.info("{} {} -> $ {} $ -> {}??? ?????? {} {} ?????? ??????", readValue.wd_bank_name, readValue.wd_account_holder_name, readValue.res_list.get(0).tran_amt, readValue.res_list.get(0).print_content, readValue.res_list.get(0).bank_name, readValue.res_list.get(0).account_holder_name);
        return "okay";
    }

    @Getter @Setter
    static class WithdrawRequestClass {
            private String cntr_account_type;
            private String cntr_account_num;
            private String wd_pass_phrase;
            private String wd_print_content;
            private String name_check_option;
            private String tran_dtime;
            private String req_cnt;
            private List<WithdrawRequestSubList> req_list;
    }

    @Getter @Setter
    static class WithdrawRequestSubList {
        private String tran_no;
        private String bank_tran_id;
        private String fintech_use_num;
        private String print_content;
        private String tran_amt;
        private String req_client_name;
        private String req_client_bank_code;
        private String req_client_account_num;
        private String req_client_num;
        private String transfer_purpose;
    }

    @Getter @Setter
    static class WithdrawResponseClass {
        private String api_tran_id;
        private String rsp_code;
        private String rsp_message;
        private String api_tran_dtm;
        private String wd_bank_code_std;
        private String wd_bank_code_sub;
        private String wd_bank_name;
        private String wd_account_num_masked;
        private String wd_print_content;
        private String wd_account_holder_name;
        private String res_cnt;
        private List<WithdrawResponseSubList> res_list;
    }

    @Getter @Setter
    static class WithdrawResponseSubList {
        private String tran_no;
        private String bank_tran_id;
        private String bank_tran_date;
        private String bank_code_tran;
        private String bank_rsp_code;
        private String bank_rsp_message;
        private String fintech_use_num;
        private String account_alias;
        private String bank_code_std;
        private String bank_code_sub;
        private String account_num_masked;
        private String bank_name;
        private String print_content;
        private String account_holder_name;
        private String tran_amt;
        private String cms_num;
        private String savings_bank_name;
        private String withdraw_bank_tran_id;
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

    static class WithDrawResponse {
        private String api_tran_id;
        private String rsp_code;
        private String rsp_message;
        private String api_tran_dtm;
        private String dps_bank_code_std;
        private String dps_bank_code_sub;
        private String dps_bank_name;
        private String dps_account_num_masked;
        private String dps_print_content;
        private String dps_account_holder_name;
        private String bank_tran_id;
        private String bank_tran_date;
        private String bank_code_tran;
        private String bank_rsp_code;
        private String bank_rsp_message;
        private String fintech_use_num;
        private String account_alias;
        private String bank_code_std;
        private String bank_code_sub;
        private String bank_name;
        private String account_num_masked;
        private String print_content;
        private String tran_amt;
        private String account_holder_name;
        private String wd_limit_remain_amt;
        private String savings_bank_name;
    }

    public static String getRandomStr(int size) {
        if(size > 0) {
            char[] tmp = new char[size];
            for(int i=0; i<tmp.length; i++) {
                int div = (int) Math.floor( Math.random() * 2 );

                if(div == 0) { // 0?????? ?????????
                    tmp[i] = (char) (Math.random() * 10 + '0') ;
                }else { //1?????? ?????????
                    tmp[i] = (char) (Math.random() * 26 + 'A') ;
                }
            }
            return new String(tmp);
        }
        return "ERROR : Size is required.";
    }
}