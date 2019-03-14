package io.basquiat;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 
 * created by basquiat
 * 
 * Spring batch를 사용하기 위해선 다음 어노테이션을 설정해준다.
 *
 */
@EnableBatchProcessing
@SpringBootApplication
public class BasquiatSpringBatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(BasquiatSpringBatchApplication.class, args);
	}

}
