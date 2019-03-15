# basquiat-spring-batch

## Step 1

스프링 배치를 사용하기 전에 내장 디비인 h2를 이용해서 간단하게 job과 step를 만들어서 실행해 본다.


## Step2

기존의 방식이였던 hikari이 좀 독특한 부분이 있어서 이 부분은 따로 기술하고자 한다.

```
spring:
  profiles: local
  datasource:
    hikari:
      cachePrepStmts: true
      connectionTestQuery: SELECT 1
      maximumPoolSize: 4
      minimumIdle: 7
      prepStmtCacheSize: 250 
      prepStmtCacheSqlLimit: 2048 
      pool-name: basquiat-server
    url: jdbc:mysql://127.0.0.1:3306/basquiat?autoReconnect=true
    username: basquiat
    password: basquiat
    dataSourceClassName: com.mysql.jdbc.jdbc2.optional.MysqlDataSource
    type: com.zaxxer.hikari.HikariDataSource

```

위와 같은 방식에서 spring boot2.x가 되면서 hikari가 spring boot2안으로 아예 들어왔다.
따라서 세팅 방식이 다음과 같이 바뀌게 된다.

```
spring:
  profiles: mysql
  datasource:
    hikari:
      jdbc-url: jdbc:mysql://127.0.0.1:3306/basquiat?autoReconnect=true
      username: basquiat
      password: basquiat
      driver-class-name: com.mysql.cj.jdbc.Driver

```

하지만 이렇게 했더니 에러가 난다.
찾아 보니  spring.datasource 커넥션 풀 정보를 알려주면 Spring Boot가 자동 인식해서 데이타소스를 생성해준다고 한다. 그런데 이럴 경우에는 2개의 데이타소스를 생성할 경우 확장할 수 없기 때문에 위 설정을 다음과 같이 하면 된다고 한다.

```
spring:
  profiles: mysql
  datasource:
    basquiat:
      jdbc-url: jdbc:mysql://127.0.0.1:3306/basquiat?autoReconnect=true
      username: basquiat
      password: basquiat
      driver-class-name: com.mysql.cj.jdbc.Driver

```
hikari 대신에 사용할 데이터베이스 명인 basquiat로 세팅하면 된다.

신기한 것은 pom.xml에 h2관련 dependency를 제거하면 실행이 되지 않는다.
이유를 아직 찾지 못함.

또한 mysql버전을 타기 때문이라는데 현재 이 프로젝트를 작성할 때 깐 mysql버전은 SELECT VERSION();날려봤더니 8.0.15이다.

그래서 시간도 남아서 버전을 5.6대로 낮춰서 해봤더니 저렇게 하지 않고 처음 방식으로 해도 잘 된다.

일단은 최신 버전으로 다시 깔아서 저런 방식으로 설정해서 사용한다는 것을 기록해 둔다.


## scheme

h2의 경우에는 관련 테이블 스키마가 생성되는데 반해서 다른 RDBMS의 경우에는 생성을 해줘야 한다.

스키마는 spring-batch-core쪽에 따로 올라와 있으며 폴더 역시 함께 올린다.