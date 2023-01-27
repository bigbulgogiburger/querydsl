package study.querydsl.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.repository.support.Querydsl4RepositorySupport;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

@Repository
public class MemberTestRepository extends Querydsl4RepositorySupport {

    public MemberTestRepository(){
        super(Member.class);
    }


    public List<Member> basicSelect(){
        List<Member> result = select(member)
                .from(member)
                .fetch();
        return result;
    }

    public List<Member> basicSelectFrom(){
        return selectFrom(member)
                .fetch();
    }

    public Page<Member> searchPageByApplyPage(MemberSearchCondition condition, Pageable pageable){
        JPAQuery<Member> query = selectFrom(member)
                .where(usernameEq(condition.getUsername())
                        , teamNameEq(condition.getTeamName())
                        , ageGoeEq(condition.getAgeGoe())
                        , ageLoeEq(condition.getAgeLoe())
                );

        List<Member> content = getQuerydsl().applyPagination(pageable, query).fetch();

        return PageableExecutionUtils.getPage(content,pageable,query::fetchCount);
    }
    
    public Page<Member> applyPagination(MemberSearchCondition condition,Pageable pageable){
        return applyPagination(pageable, query ->
                query.selectFrom(member)
                        .where(usernameEq(condition.getUsername())
                                , teamNameEq(condition.getTeamName())
                                , ageGoeEq(condition.getAgeGoe())
                                , ageLoeEq(condition.getAgeLoe())
                        )
                ,countQuery -> countQuery
                        .select(member.id)
                        .from(member)
                        .leftJoin(member.team,team)
                        .where(usernameEq(condition.getUsername())
                                , teamNameEq(condition.getTeamName())
                                , ageGoeEq(condition.getAgeGoe())
                                , ageLoeEq(condition.getAgeLoe())
                        )

        );
    }


    public BooleanExpression ageBetween(int ageLoe, int ageGoe){
        return ageGoeEq(ageGoe).and(ageLoeEq(ageLoe));
    }



    private BooleanExpression usernameEq(String username) {
        return hasText(username)?member.username.eq(username):null;
    }

    private BooleanExpression teamNameEq(String teamName) {
        return hasText(teamName)?team.name.eq(teamName):null;
    }

    private BooleanExpression ageGoeEq(Integer ageGoe) {
        return ageGoe!=null ? member.age.goe(ageGoe):null;
    }


    private BooleanExpression ageLoeEq(Integer ageLoe) {
        return ageLoe!=null ? member.age.loe(ageLoe):null;
    }
}
