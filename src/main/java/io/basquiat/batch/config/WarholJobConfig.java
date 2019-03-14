package io.basquiat.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * created by basquiat
 * 
 * simple andy warhol step 
 * 
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class WarholJobConfig {
	
	/**
	 * Job And Step Relation
	 * 
	 * Job
	 * ----------------------------------
	 * |								|
	 * | Step | Step | Step | ...		|
	 * |								|
	 * ----------------------------------
	 * 
	 * 단순하게 표현하자면 하나의 Job은 step이라는 것을 가지게 된다.
	 * 
	 * 
	 */
	private final JobBuilderFactory jobBuilderFactory;
	
	private final StepBuilderFactory stepBuilderFactory;

	/**
	 * job builder factory
	 * 
	 * job qualifier 설정 ("warholJob")
	 * 해당 job이 실행되면 최초 시작하는 andyStep이 실앻되고 -> warholStep 순으로 job이 스텝을 호출한다.
	 * 
	 * @return Job
	 */
	@Bean
	public Job warholJob() {
	return jobBuilderFactory.get("warholJob")
	            			.start(andyStep())
	            			.next(warholStep())
	            			.build();
	}

	/**
	 * step setup
	 * 
	 * <pre>
	 * Andy Warhol
	 * 
	 * andyStep
	 *     ↓
	 * warholStep
	 * </pre>
	 * 
	 * @return Step
	 */
	@Bean
	public Step andyStep() {
		return stepBuilderFactory.get("andyStep")
				                  .tasklet((contribution, chunkContext) -> 
				                  			{
							                    log.info(">>>>> This is Andy");
							                    return RepeatStatus.FINISHED;
				                  			})
				                  .build();
    }

    @Bean
    public Step warholStep() {
		return stepBuilderFactory.get("warholStep")
				                 .tasklet((contribution, chunkContext) -> 
				                		   {
				                			   log.info(">>>>> This is Andy Warhol");
							                   return RepeatStatus.FINISHED;
				                		   })
				                 .build();
    }

}
