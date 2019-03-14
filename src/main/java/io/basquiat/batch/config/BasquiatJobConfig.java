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

@Slf4j
@RequiredArgsConstructor
@Configuration
public class BasquiatJobConfig {
	
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
	 * job qualifier 설정 ("basquiatJob")
	 * 해당 job이 실행되면 최초 시작하는 jeanStep이 실앻되고 -> michelStep -> basquiatStep 순으로 job이 스텝을 호출한다.
	 * 
	 * @return Job
	 */
	@Bean
	public Job basquiatJob() {
	return jobBuilderFactory.get("basquiatJob")
	            			.start(jeanStep())
	            			.next(michelStep())
	            			.next(basquiatStep())
	            			.build();
	}

	/**
	 * step setup
	 * 
	 * <pre>
	 * Jean-Michel Basquiat
	 * 
	 * jeanStep
	 *     ↓
	 * michelStep
	 *     ↓ 
	 * basquiatStep
	 * </pre>
	 * 
	 * @return Step
	 */
	@Bean
	public Step jeanStep() {
		return stepBuilderFactory.get("jeanStep")
				                  .tasklet((contribution, chunkContext) -> 
				                  			{
							                    log.info(">>>>> This is Jean");
							                    return RepeatStatus.FINISHED;
				                  			})
				                  .build();
    }

    @Bean
    public Step michelStep() {
		return stepBuilderFactory.get("michelStep")
				                 .tasklet((contribution, chunkContext) -> 
				                		   {
				                			   log.info(">>>>> This is Michel");
							                   return RepeatStatus.FINISHED;
				                		   })
				                 .build();
    }

    
    @Bean
    public Step basquiatStep() {
		return stepBuilderFactory.get("basquiatStep")
                				 .tasklet((contribution, chunkContext) -> 
				                		   {
				                			   log.info(">>>>> This is Jean-Michel Basquiat");
				                			   return RepeatStatus.FINISHED;
				                		   })
                				 .build();
    }

}
