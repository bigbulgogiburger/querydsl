package study.querydsl.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestBody;

//@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Data
public class MemberSearchCondition {

//    회원명, 팀명, 나이(ageGoe,ageLoe)
    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
