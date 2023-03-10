package study.querydsl;


import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.persistence.TypedQuery;

import java.util.List;
import java.util.stream.Collectors;

import static com.querydsl.jpa.JPAExpressions.*;
import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {


    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        em.persist(teamA);
        em.persist(teamB);


        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);
        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        em.flush();
        em.clear();

    }


    @Test
    public void startJPQL(){
        //find member1
        Member findMember = em.createQuery("select m from Member m where m.username =:username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");

    }

    @Test
    public void startQuerydsl(){
//        ?????? ???????????? ???????????? ????????? ???????????? ??????(?????????)
//        QMember m = new QMember("m");
//        QMember m = QMember.member;

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1")) //???????????? ????????? ??????
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory.selectFrom(member)
                .where(member.username.eq("member1"),// .and()
                        member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetchTest(){
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory.selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory.selectFrom(member)
//                .fetchFirst();

        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();

        long total = queryFactory.selectFrom(member)
                .fetchCount();

//        long total = results.getTotal();
//        List<Member> resultList = results.getResults();
    }

    /**
     * ?????? ?????? ??????
     * 1. ?????? ?????? ????????????
     * 2. ?????? ?????? ????????????
     * ??? 2?????? ?????? ????????? ????????? ???????????? ??????(null last)
     */

    @Test
    public void sort(){
        em.persist(new Member(null,100));
        em.persist(new Member("member5",100));
        em.persist(new Member("member6",100));

        List<Member> result = queryFactory.selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isEqualTo(null);

    }

    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2(){
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);
    }

    @Test
    public void aggregation(){
        List<Tuple> result = queryFactory
                .select(member.count()
                        , member.age.sum()
                        , member.age.avg()
                        , member.age.max()
                        , member.age.min()
                ).from(member).fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(40);
        assertThat(tuple.get(member.age.avg())).isEqualTo(10);
        assertThat(tuple.get(member.age.max())).isEqualTo(10);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????
     *
     */
    @Test
    public void groupby(){
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }


    /**
     * teamA??? ????????? ?????? ??????
     */
    @Test
    public void join(){

        List<Member> result = queryFactory.selectFrom(member)
                .leftJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");

    }


    /**
     * ????????????
     * ????????? ????????? ??? ????????? ?????? ?????? ??????
     */
    @Test
    public void theta_join(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result).extracting("username").containsExactly("teamA","teamB");
    }

    /**
     * ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * jpql : select m, t from Member m left join m.team t on t.name = 'teamA'
     */
    @Test
    public void join_on_filtering(){
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                .on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * ??????????????? ?????? ????????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
     */
    @Test
    public void join_on_no_relation(){
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));


        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo(){
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("???????????? ?????????").isTrue();
    }

    /**
     * ????????? ?????? ?????? ????????? ??????
     *
     */
    @Test
    public void subQuery(){

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age").containsExactly(40);

    }

    @Test
    public void joinInline(){
        QMember memberSub = new QMember("memberSub");

        queryFactory.select(member).from(member).join(team).on(member.team.eq(team)).fetch();
    }

    /**
     * ????????? ??????????????? ????????? ??????
     *
     */
    @Test
    public void subQueryGoe(){

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                )).fetch();

        assertThat(result).extracting("age").containsExactly(30,40);

    }

    @Test
    public void subQueryIn(){

        QMember memberSub = new QMember("memberSub");
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                )).fetch();

        assertThat(result).extracting("age").containsExactly(20,30,40);

    }
    
    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(member.username,
                        select(memberSub.age.avg()).from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
            
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(member.age.when(10).then("10???")
                        .when(20).then("20???").otherwise("etc")).from(member).fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }


    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder().when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("20~30").otherwise("etc")).from(member)
                .fetch();

        for (String s : result) {

            System.out.println("s = " + s);
        }

    }

    @Test
    public void constant(){
        List<Tuple> result = queryFactory.select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    @Test
    public void concat(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        List<String> concatResult = result.stream().map(x -> x.get(member.username) + "_" + x.get(member.age)).collect(Collectors.toList());

        for (String s : concatResult) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();


        for (Tuple tuple : result) {

            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);


        }

    }

    @Test
    public void findDtoByJpql(){
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username,m.age) from Member m", MemberDto.class)
                .getResultList();

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                //setter??? ????????? ?????? ?????????.
                .select(Projections.bean(MemberDto.class,
                        member.username, member.age)
                )
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
//              java Reflection?????? getter setter ????????? ??? ??? ??????.
                .select(Projections.fields(MemberDto.class,
                        member.username, member.age)
                )
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByField(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
//              java Reflection?????? getter setter ????????? ??? ??? ??????.
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                         //sub-query??? ????????? alias???
                         ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub),"age"))
                )
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("memberDto = " + userDto);
        }
    }


    @Test
    public void findDtoByConstructor(){
        List<UserDto> result = queryFactory
                .select(Projections.constructor(UserDto.class,
                        member.username, member.age)
                )
                .from(member)
                .fetch();
        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                //constructor??? ?????? ?????????????????? ?????????, ?????????????????? ???????????? ?????????????????? ?????? ??? ??????
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void dynamicQueryBooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember1(usernameParam,ageParam);

        assertThat(result.size()).isEqualTo(1);
    }


    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

//        ????????? ????????? ??????
//        BooleanBuilder builder = new BooleanBuilder(member.username.eq(usernameCond));
        BooleanBuilder builder = new BooleanBuilder();
        if(usernameCond!=null){
            builder.and(member.username.eq(usernameCond));
        }
        if(ageCond!=null){
            builder.and(member.age.eq(ageCond));
        }
        return queryFactory.selectFrom(member)
                .where(builder)
                .fetch();

    }

    @Test
    public void dynamicQuery_Where_Param(){
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam,ageParam);

        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
//                where ?????? ????????? null?????? ????????????.
//                .where(usernameEq(usernameCond),ageEq(ageCond))
                .where(allEq(usernameCond,ageCond))
                .fetch();

    }



    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond!=null?member.username.eq(usernameCond):null;

    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond!=null?member.age.eq(ageCond):null;
    }

    //?????? ?????? isValid, ????????? IN -> ????????? ??????????????? ????????? ????????? ????????? ????????????
    //composition??? ????????? ??? ??????.

    private BooleanExpression allEq(String usernameCond,Integer ageCond){
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }


    @Test
//    @Commit
    public void bulkUpdate(){
//        member1 =10 -> ?????????
//        member2 =20 -> ?????????
        queryFactory
                .update(member)
                .set(member.username,"?????????")
//                .set(member.age,member.age.add(2))
                .set(member.age,member.age.multiply(2))
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        //????????? ????????????????????? member1 ??????db????????? ?????????
        //update ????????? 1??? ????????? ???????????? ?????? ????????????
        //????????? ??????????????? ???????????? ?????????.
        List<Member> result = queryFactory.select(member).from(member).fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    @Test
    public void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.lt(20))
                .execute();
    }

    @Test
    public void sqlFunction(){
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace',{0},{1},{2})"
                        , member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2(){
        queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username
//                        .eq(Expressions.stringTemplate(
//                                "function('lower',{0})"
//                                ,member.username))).fetch();
                .where(member.username.eq(member.username.lower())).fetch();
    }


}
