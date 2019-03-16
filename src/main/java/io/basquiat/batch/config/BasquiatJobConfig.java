package io.basquiat.batch.config;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * created by basquiat
 * 
 * simple jean-michel basquiat step
 *
 */
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
	            			.start(jeanStep(null))
	            			.next(michelStep(null))
	            			.next(basquiatStep(null))
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

}
