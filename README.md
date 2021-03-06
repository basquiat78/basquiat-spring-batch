# basquiat-spring-batch

[About Spring Batch Introduction](https://docs.spring.io/spring-batch/4.1.x/reference/html/index-single.html)

### Project Enviroments

OS: Window10    
IDE: STS    
Framework: Spring Boot 2.1.3    
Module: Spring Batch 4.1.1    
DB: mySql 8.0.15    

버전은 이번에 하나씩 작업하면서 느끼는 건데 버전이 중요한거 같다.

특히 mySql의 경우에는 버전에 따라서 application.yml에 설정하는 방법이 살짝 변경되는 부분도 있고 Spring Batch의 경우에는 추가된 부분들이 좀 있기 때문이다.

이것은 Step별로 브랜치를 생성해서 튜토리얼을 작성한 프로젝트이다.

각 단계별로 브랜치를 확인하면서 진행하면 된다.


## Step 1

스프링 배치를 사용하기 전에 내장 디비인 h2를 이용해서 간단하게 job과 step를 만들어서 실행해 본다.

언제나 "Hello World"를 찎는 걸로 시작하니깐 간단하게 형식적인 job과 step를 작성해서 로그를 찍으면 시작할 준비가 된것이 아니겠는가?


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

### scheme

h2의 경우에는 관련 테이블 스키마가 생성되는데 반해서 다른 RDBMS의 경우에는 생성을 해줘야 한다.

스키마는 spring-batch-core쪽에 따로 올라와 있으며 폴더 역시 함께 올린다.


## Step3

이번 스텝은 Job Parameter와 관련된 내용이다.

지금 프로젝트는 두개의 잡 그러니깐 basquiatJob과 warholJob를 작성하고 해당 프로젝트를 실행하게 되면 이 두개의 잡이 동시에 도는 것을 확인할 수 있다.

두 개 또는 그 이상의 잡이 존재할 경우 원하는 잡만 실행시킬 수 없을까에 대한 방법이다.

또한 Spring Batch를 실행할때 특별히 Job Parameter를 외부에서 입력하지 않으면 parameters={-spring.output.ansi.enabled=always}가 자동으로 입력되서 수행된다.

어찌 되었든 이 parameter는 batch_job_execution_params라는 테이블에 인서트가 된다.

spring batch는 이 파라미터를 통해서 동일한 파라미터가 들어오게 되면  

```
Caused by: org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException: A job instance already exists..

```
와 같은 에러를 발생하며 해당 잡이 수행되지 않는다. 물론 이것은 생각해 보면 당연한 결과일수도 있다.

예를 들어 매일 하루에 도는 배치를 작성했다고 하자.

A라는 사람이 수행을 했는데 B라는 사람이 그 사실을 모르고 수행을 했다면 중복 수행이 될 수 있는데 이것을 통해서 중복 실행을 방지하는 것이다.

Job Parameter라는 것은 결국 외부로부터 어떤 특정 Flag처럼 주입이 되서 배치를 돌때 그 flag에 따른 비지니스 로직을 심을 수 있다는 의미가 되겠다.

예를 들면 오늘 날짜로 job parameter값을 집어넣으면 오늘 날짜의 매출/매입 정보를 돌린다든가 하는 식으로....

## 원하는 Job만 실행을 해보자

어떠한 옵션도 설정하지 않으면 실행시 모든 job이 실행된다.
하지만 원하는 job만을 실행하기 위해서는 application.yml에 옵션을 추가해야한다.

```
spring:
  batch:
    job:
      names: ${job.name:NONE}
  profiles:
    active: mysql

```

기존의 설정에서 batch.job.names의 프로퍼티 값을 ${job.name:NONE}으로 설정하게 되면 어떤 job도 실행되지 않는다.
저 옵션의 의미는 실행시 job name이 존재하지 않으면 실행하지 않는다는 의미다.

저 옵션을 설정하고 이클립스에서 실행시 다음과 같이 옵션을 주어서 실행한다.

Run Configuration에서 arguments탭에 다음과 같이 job name을 주고 실행해 보자

```
--job.name=basquiatJob
```

저 명령어를 입력하고 실행하면

```
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.1.3.RELEASE)

2019-03-16 15:12:21 - Starting BasquiatSpringBatchApplication on Jean-Michel-Basquiat with PID 15240 (C:\Users\basquiat\git\basquiat-spring-batch\target\classes started by basquiat in C:\Users\basquiat\git\basquiat-spring-batch)
2019-03-16 15:12:21 - The following profiles are active: mysql
2019-03-16 15:12:21 - Bootstrapping Spring Data repositories in DEFAULT mode.
2019-03-16 15:12:21 - Finished Spring Data repository scanning in 9ms. Found 0 repository interfaces.
2019-03-16 15:12:21 - HikariPool-1 - Starting...
2019-03-16 15:12:22 - HikariPool-1 - Start completed.
2019-03-16 15:12:22 - HHH000204: Processing PersistenceUnitInfo [
	name: default
	...]
2019-03-16 15:12:22 - HHH000412: Hibernate Core {5.3.7.Final}
2019-03-16 15:12:22 - HHH000206: hibernate.properties not found
2019-03-16 15:12:22 - HCANN000001: Hibernate Commons Annotations {5.0.4.Final}
2019-03-16 15:12:22 - HHH000400: Using dialect: org.hibernate.dialect.MySQL5InnoDBDialect
2019-03-16 15:12:22 - Initialized JPA EntityManagerFactory for persistence unit 'default'
2019-03-16 15:12:22 - JPA does not support custom isolation levels, so locks may not be taken when launching Jobs
2019-03-16 15:12:22 - No database type set, using meta data indicating: MYSQL
2019-03-16 15:12:22 - No TaskExecutor has been set, defaulting to synchronous executor.
2019-03-16 15:12:22 - Started BasquiatSpringBatchApplication in 1.965 seconds (JVM running for 2.44)
2019-03-16 15:12:22 - Running default command line with: [--spring.output.ansi.enabled=always, --job.name=basquiatJob, version=5]
2019-03-16 15:12:22 - Initializing lazy target object
2019-03-16 15:12:22 - Initializing lazy target object
2019-03-16 15:12:22 - Job: [SimpleJob: [name=basquiatJob]] launched with the following parameters: [{version=5, -job.name=basquiatJob, -spring.output.ansi.enabled=always}]
2019-03-16 15:12:22 - Job execution starting: JobExecution: id=10, version=0, startTime=null, endTime=null, lastUpdated=Sat Mar 16 15:12:22 GMT+09:00 2019, status=STARTING, exitStatus=exitCode=UNKNOWN;exitDescription=, job=[JobInstance: id=10, version=0, Job=[basquiatJob]], jobParameters=[{version=5, -job.name=basquiatJob, -spring.output.ansi.enabled=always}]
2019-03-16 15:12:22 - Executing step: [jeanStep]
2019-03-16 15:12:23 - Executing: id=24
2019-03-16 15:12:23 - Starting repeat context.
2019-03-16 15:12:23 - Repeat operation about to start at count=1
2019-03-16 15:12:23 - Preparing chunk execution for StepContext: org.springframework.batch.core.scope.context.StepContext@f1f7db2
2019-03-16 15:12:23 - Chunk execution starting: queue size=0
2019-03-16 15:12:23 - Initializing lazy target object
2019-03-16 15:12:23 - >>>>> This is Jean
2019-03-16 15:12:23 - Applying contribution: [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING]
2019-03-16 15:12:23 - Saving step execution before commit: StepExecution: id=24, version=1, name=jeanStep, status=STARTED, exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:12:23 - Repeat is complete according to policy and result value.
2019-03-16 15:12:23 - Step execution success: id=24
2019-03-16 15:12:23 - Step execution complete: StepExecution: id=24, version=3, name=jeanStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0
2019-03-16 15:12:23 - Executing step: [michelStep]
2019-03-16 15:12:23 - Executing: id=25
2019-03-16 15:12:23 - Starting repeat context.
2019-03-16 15:12:23 - Repeat operation about to start at count=1
2019-03-16 15:12:23 - Preparing chunk execution for StepContext: org.springframework.batch.core.scope.context.StepContext@d2291de
2019-03-16 15:12:23 - Chunk execution starting: queue size=0
2019-03-16 15:12:23 - >>>>> This is Michel
2019-03-16 15:12:23 - Applying contribution: [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING]
2019-03-16 15:12:23 - Saving step execution before commit: StepExecution: id=25, version=1, name=michelStep, status=STARTED, exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:12:23 - Repeat is complete according to policy and result value.
2019-03-16 15:12:23 - Step execution success: id=25
2019-03-16 15:12:23 - Step execution complete: StepExecution: id=25, version=3, name=michelStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0
2019-03-16 15:12:23 - Executing step: [basquiatStep]
2019-03-16 15:12:23 - Executing: id=26
2019-03-16 15:12:23 - Starting repeat context.
2019-03-16 15:12:23 - Repeat operation about to start at count=1
2019-03-16 15:12:23 - Preparing chunk execution for StepContext: org.springframework.batch.core.scope.context.StepContext@6dc9da2d
2019-03-16 15:12:23 - Chunk execution starting: queue size=0
2019-03-16 15:12:23 - >>>>> This is Jean-Michel Basquiat
2019-03-16 15:12:23 - Applying contribution: [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING]
2019-03-16 15:12:23 - Saving step execution before commit: StepExecution: id=26, version=1, name=basquiatStep, status=STARTED, exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:12:23 - Repeat is complete according to policy and result value.
2019-03-16 15:12:23 - Step execution success: id=26
2019-03-16 15:12:23 - Step execution complete: StepExecution: id=26, version=3, name=basquiatStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0
2019-03-16 15:12:23 - Upgrading JobExecution status: StepExecution: id=26, version=3, name=basquiatStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:12:23 - Job execution complete: JobExecution: id=10, version=1, startTime=Sat Mar 16 15:12:22 GMT+09:00 2019, endTime=null, lastUpdated=Sat Mar 16 15:12:22 GMT+09:00 2019, status=COMPLETED, exitStatus=exitCode=COMPLETED;exitDescription=, job=[JobInstance: id=10, version=0, Job=[basquiatJob]], jobParameters=[{version=5, -job.name=basquiatJob, -spring.output.ansi.enabled=always}]
2019-03-16 15:12:23 - Job: [SimpleJob: [name=basquiatJob]] completed with the following parameters: [{version=5, -job.name=basquiatJob, -spring.output.ansi.enabled=always}] and the following status: [COMPLETED]
2019-03-16 15:12:23 - Initializing lazy target object
2019-03-16 15:12:23 - Closing JPA EntityManagerFactory for persistence unit 'default'
2019-03-16 15:12:23 - HikariPool-1 - Shutdown initiated...
2019-03-16 15:12:23 - HikariPool-1 - Shutdown completed.
```

basquiatJob만 실행된 것을 확인할 수 있다.

## 원하는 Job에 JobParameter 주입하기

이미지를 올릴 수 없으니...귀찮지만 Job, Step, Tasklet를 xml형식으로 표현을 해보겠다.

예를 들면 Job이 하나 있고 이름이 basquiatJob이고 Step이 존재할 것이다.?

Jean-Michel Basquiat의 이름을 따서 다음과 같이 xml형식으로 표현할 수 있겠다.

```
<Job name="basquiatJob">
	<Step name="jeanStep">
		<Tasklet name="1">
		</Tasklet>
	</Step>
	<Step name="michelStep">
		<Tasklet name="2">
		</Tasklet>
	</Step>
	<Step name="basquiatStep">
		<Tasklet name="3">
		</Tasklet>
	</Step>
</Job>
```

이런 xml형식을 본다면 step의 scope영역은 Job의 영역이라고 볼 수 있다.
따라서 Batch를 실행시에 JobParameter를 주입할 때 각 Step에 @JobScope 어노테이션을 붙여서 사용하며 예제 코드는 다음과 같이 코딩한다.

예상: step의 tasklet은 step의 영역이나 tasklet관련 메소드에는 @StepScope가 붙지 않을까??

```
	@Bean
	public Job basquiatJob() {
	return jobBuilderFactory.get("basquiatJob")
	            			.start(jeanStep(null))
	            			.next(michelStep(null))
	            			.next(basquiatStep(null))
	            			.build();
	}

	@Bean
	@JobScope
	public Step jeanStep(@Value("#{jobParameters[favoriteMusician]}") String favoriteMusician) {
		return stepBuilderFactory.get("jeanStep")
				                  .tasklet((contribution, chunkContext) -> 
				                  			{
							                    log.info(">>>>> This is Jean");
							                    log.info("Jean-Michel Basquiat Loves {}", favoriteMusician);
							                    return RepeatStatus.FINISHED;
				                  			})
				                  .build();
    }
    
    @Bean
    @JobScope
    public Step michelStep(@Value("#{jobParameters[favoriteMusician]}") String favoriteMusician) {
		return stepBuilderFactory.get("michelStep")
				                 .tasklet((contribution, chunkContext) -> 
				                		   {
				                			   log.info(">>>>> This is Michel");
				                			   log.info("Jean-Michel Basquiat Loves {}", favoriteMusician);
							                   return RepeatStatus.FINISHED;
				                		   })
				                 .build();
    }
    
    @Bean
    @JobScope
    public Step basquiatStep(@Value("#{jobParameters[favoriteMusician]}") String favoriteMusician) {
		return stepBuilderFactory.get("basquiatStep")
                				 .tasklet((contribution, chunkContext) -> 
				                		   {
				                			   log.info(">>>>> This is Jean-Michel Basquiat");
				                			   log.info("Jean-Michel Basquiat Loves {}", favoriteMusician);
				                			   return RepeatStatus.FINISHED;
				                		   })
                				 .build();
    }
```

basquiatJob내부에 flow에 파라미터가 null인 이유는 runtime시에 lazy 또눈  late data binding방식으로 주입이 되기 때문이다.


다음과 같이 실행을 해보자

```
--job.name=basquiatJob favoriteMusician=CharlieParker
```

log

```

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::        (v2.1.3.RELEASE)

2019-03-16 15:29:43 - Starting BasquiatSpringBatchApplication on Jean-Michel-Basquiat with PID 12176 (C:\Users\basquiat\git\basquiat-spring-batch\target\classes started by basquiat in C:\Users\basquiat\git\basquiat-spring-batch)
2019-03-16 15:29:43 - The following profiles are active: mysql
2019-03-16 15:29:43 - Bootstrapping Spring Data repositories in DEFAULT mode.
2019-03-16 15:29:43 - Finished Spring Data repository scanning in 9ms. Found 0 repository interfaces.
2019-03-16 15:29:43 - HikariPool-1 - Starting...
2019-03-16 15:29:44 - HikariPool-1 - Start completed.
2019-03-16 15:29:44 - HHH000204: Processing PersistenceUnitInfo [
	name: default
	...]
2019-03-16 15:29:44 - HHH000412: Hibernate Core {5.3.7.Final}
2019-03-16 15:29:44 - HHH000206: hibernate.properties not found
2019-03-16 15:29:44 - HCANN000001: Hibernate Commons Annotations {5.0.4.Final}
2019-03-16 15:29:44 - HHH000400: Using dialect: org.hibernate.dialect.MySQL5InnoDBDialect
2019-03-16 15:29:44 - Initialized JPA EntityManagerFactory for persistence unit 'default'
2019-03-16 15:29:44 - JPA does not support custom isolation levels, so locks may not be taken when launching Jobs
2019-03-16 15:29:44 - No database type set, using meta data indicating: MYSQL
2019-03-16 15:29:44 - No TaskExecutor has been set, defaulting to synchronous executor.
2019-03-16 15:29:44 - Started BasquiatSpringBatchApplication in 2.009 seconds (JVM running for 2.491)
2019-03-16 15:29:44 - Running default command line with: [--spring.output.ansi.enabled=always, --job.name=basquiatJob, favoriteMusician=CharlieParker]
2019-03-16 15:29:44 - Initializing lazy target object
2019-03-16 15:29:45 - Initializing lazy target object
2019-03-16 15:29:45 - Job: [SimpleJob: [name=basquiatJob]] launched with the following parameters: [{favoriteMusician=CharlieParker, -job.name=basquiatJob, -spring.output.ansi.enabled=always}]
2019-03-16 15:29:45 - Job execution starting: JobExecution: id=11, version=0, startTime=null, endTime=null, lastUpdated=Sat Mar 16 15:29:45 GMT+09:00 2019, status=STARTING, exitStatus=exitCode=UNKNOWN;exitDescription=, job=[JobInstance: id=11, version=0, Job=[basquiatJob]], jobParameters=[{favoriteMusician=CharlieParker, -job.name=basquiatJob, -spring.output.ansi.enabled=always}]
2019-03-16 15:29:45 - Creating object in scope=job, name=scopedTarget.jeanStep
2019-03-16 15:29:45 - Executing step: [jeanStep]
2019-03-16 15:29:45 - Executing: id=27
2019-03-16 15:29:45 - Starting repeat context.
2019-03-16 15:29:45 - Repeat operation about to start at count=1
2019-03-16 15:29:45 - Preparing chunk execution for StepContext: org.springframework.batch.core.scope.context.StepContext@11eed657
2019-03-16 15:29:45 - Chunk execution starting: queue size=0
2019-03-16 15:29:45 - Initializing lazy target object
2019-03-16 15:29:45 - >>>>> This is Jean
2019-03-16 15:29:45 - Jean-Michel Basquiat Loves CharlieParker
2019-03-16 15:29:45 - Applying contribution: [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING]
2019-03-16 15:29:45 - Saving step execution before commit: StepExecution: id=27, version=1, name=jeanStep, status=STARTED, exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:29:45 - Repeat is complete according to policy and result value.
2019-03-16 15:29:45 - Step execution success: id=27
2019-03-16 15:29:45 - Step execution complete: StepExecution: id=27, version=3, name=jeanStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0
2019-03-16 15:29:45 - Creating object in scope=job, name=scopedTarget.michelStep
2019-03-16 15:29:45 - Executing step: [michelStep]
2019-03-16 15:29:45 - Executing: id=28
2019-03-16 15:29:45 - Starting repeat context.
2019-03-16 15:29:45 - Repeat operation about to start at count=1
2019-03-16 15:29:45 - Preparing chunk execution for StepContext: org.springframework.batch.core.scope.context.StepContext@1806bc4c
2019-03-16 15:29:45 - Chunk execution starting: queue size=0
2019-03-16 15:29:45 - >>>>> This is Michel
2019-03-16 15:29:45 - Jean-Michel Basquiat Loves CharlieParker
2019-03-16 15:29:45 - Applying contribution: [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING]
2019-03-16 15:29:45 - Saving step execution before commit: StepExecution: id=28, version=1, name=michelStep, status=STARTED, exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:29:45 - Repeat is complete according to policy and result value.
2019-03-16 15:29:45 - Step execution success: id=28
2019-03-16 15:29:45 - Step execution complete: StepExecution: id=28, version=3, name=michelStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0
2019-03-16 15:29:45 - Creating object in scope=job, name=scopedTarget.basquiatStep
2019-03-16 15:29:45 - Executing step: [basquiatStep]
2019-03-16 15:29:45 - Executing: id=29
2019-03-16 15:29:45 - Starting repeat context.
2019-03-16 15:29:45 - Repeat operation about to start at count=1
2019-03-16 15:29:45 - Preparing chunk execution for StepContext: org.springframework.batch.core.scope.context.StepContext@5115f590
2019-03-16 15:29:45 - Chunk execution starting: queue size=0
2019-03-16 15:29:45 - >>>>> This is Jean-Michel Basquiat
2019-03-16 15:29:45 - Jean-Michel Basquiat Loves CharlieParker
2019-03-16 15:29:45 - Applying contribution: [StepContribution: read=0, written=0, filtered=0, readSkips=0, writeSkips=0, processSkips=0, exitStatus=EXECUTING]
2019-03-16 15:29:45 - Saving step execution before commit: StepExecution: id=29, version=1, name=basquiatStep, status=STARTED, exitStatus=EXECUTING, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:29:45 - Repeat is complete according to policy and result value.
2019-03-16 15:29:45 - Step execution success: id=29
2019-03-16 15:29:45 - Step execution complete: StepExecution: id=29, version=3, name=basquiatStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0
2019-03-16 15:29:45 - Upgrading JobExecution status: StepExecution: id=29, version=3, name=basquiatStep, status=COMPLETED, exitStatus=COMPLETED, readCount=0, filterCount=0, writeCount=0 readSkipCount=0, writeSkipCount=0, processSkipCount=0, commitCount=1, rollbackCount=0, exitDescription=
2019-03-16 15:29:45 - Job execution complete: JobExecution: id=11, version=1, startTime=Sat Mar 16 15:29:45 GMT+09:00 2019, endTime=null, lastUpdated=Sat Mar 16 15:29:45 GMT+09:00 2019, status=COMPLETED, exitStatus=exitCode=COMPLETED;exitDescription=, job=[JobInstance: id=11, version=0, Job=[basquiatJob]], jobParameters=[{favoriteMusician=CharlieParker, -job.name=basquiatJob, -spring.output.ansi.enabled=always}]
2019-03-16 15:29:45 - Job: [SimpleJob: [name=basquiatJob]] completed with the following parameters: [{favoriteMusician=CharlieParker, -job.name=basquiatJob, -spring.output.ansi.enabled=always}] and the following status: [COMPLETED]
2019-03-16 15:29:45 - Initializing lazy target object
2019-03-16 15:29:45 - Closing JPA EntityManagerFactory for persistence unit 'default'
2019-03-16 15:29:45 - HikariPool-1 - Shutdown initiated...
2019-03-16 15:29:45 - HikariPool-1 - Shutdown completed.
```


## At A Glance

하다보면 원하지 않는 파라미터 -spring.output.ansi.enabled=always가 항상 따라 붙는다.

이거 없애는 방법이 뭔지를 모르겠다. forum, stackoverflow를 아무리 찾아봐도 방법이 없다...

Log의 ANSI와 관련이 있다고 하는데 아무래도 기본으로 붙어서 날아가는 듯하다.

Next Level : Tasklet