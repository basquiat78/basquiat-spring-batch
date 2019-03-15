# basquiat-spring-batch

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

찾아보니 이유는 mysql의 버전때문이다. 버전을 낮추면 될 문제지만 최신 버전을 사용한다면 그것에 맞춰서 수정을 해줘야 하니 삽질이 시작됨

일단 timezone 문제라는 것을 구글신을 통해서 찾게 되었다.

그중에 간단한 방식은 다음과 같이 수정하는 것이다.

```
spring:
  profiles: mysql
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate.dialect: org.hibernate.dialect.MySQL5InnoDBDialect
      hibernate.hbm2ddl.import_files_sql_extractor: org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor
      hibernate.default_batch_fetch_size: ${chunkSize:1000}
    open-in-view: false
  datasource:
    hikari:
      jdbc-url: jdbc:mysql://localhost:3306/basquiat?useUnicode=yes&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
      username: basquiat
      password: basquiat
      driver-class-name: com.mysql.cj.jdbc.Driver

```
jdbc-url에 설정한 값에 옵션을 추가해서 serverTimezone=Asia/Seoul을 붙여서 실행하면 에러없이 잘 된다. 물론 UTC를 붙여서 해도 문제 없이 돈다.

하지만 여기는 한국이니깐... KST는 에러가 나고 저렇게 지역을 명시해야 에러없이 작동한다.

문제는 이것은 임시방편이 아닌가 싶다.

왜냐하면 시스템간의 시간과 세션의 시간이 다르다고 한다. 

일단은 모르겠다. 저렇게 하면 문제없이 돌아가고 select now()를 날렸을 때 시간도 한국 시간이니깐 그대로....

## scheme

h2의 경우에는 관련 테이블 스키마가 생성되는데 반해서 다른 RDBMS의 경우에는 생성을 해줘야 한다.

스키마는 spring-batch-core쪽에 따로 올라와 있으며 폴더 역시 함께 올린다.